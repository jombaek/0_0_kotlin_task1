package com.example.screenshotmaker

import javafx.event.ActionEvent
import javafx.fxml.FXML
import javafx.scene.SnapshotParameters
import javafx.scene.canvas.Canvas
import javafx.scene.control.*
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.image.WritableImage
import javafx.scene.input.MouseButton
import javafx.scene.input.MouseEvent
import javafx.scene.layout.HBox
import javafx.scene.layout.StackPane
import javafx.scene.paint.Color
import javafx.scene.transform.Scale
import java.awt.Rectangle
import java.awt.Robot
import java.awt.Toolkit
import java.awt.image.BufferedImage
import java.io.IOException
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.system.exitProcess


class ScreenshotMakerController {

    @FXML
    private lateinit var defaultToolPanel: HBox
    @FXML
    private lateinit var cutToolPanel: HBox
    @FXML
    private lateinit var hideWindowCheck: CheckBox
    @FXML
    private lateinit var timerSlider: Slider
    @FXML
    private lateinit var canvasPane: StackPane
    @FXML
    private lateinit var screenshotCanvas: Canvas
    @FXML
    private lateinit var drawingCanvas: Canvas
    @FXML
    private lateinit var cutCanvas: Canvas
    @FXML
    private lateinit var takeScreenshotButton: Button
    @FXML
    private lateinit var brushColorPicker: ColorPicker
    @FXML
    private lateinit var brushWidthSpinner: Spinner<Int>
    @FXML
    private lateinit var cutButton: Button

    private enum class AppMode {
        CUTTING,
        DRAWING
    }

    private var timer = 0.0
    private var hasImage = false
    private var appMode = AppMode.DRAWING
    private var x_start = -1.0
    private var y_start = -1.0

    private fun convertToFxImage(image: BufferedImage?): Image? {
        var wr: WritableImage? = null
        if (image != null) {
            wr = WritableImage(image.width, image.height)
            val pw = wr.pixelWriter
            for (x in 0 until image.width) {
                for (y in 0 until image.height) {
                    pw.setArgb(x, y, image.getRGB(x, y))
                }
            }
        }
        return ImageView(wr).image
    }

    private fun getScreenshot(): Image? {
        try {
            var robot = Robot()
            var screenRect = Rectangle(Toolkit.getDefaultToolkit().screenSize)
            var screenFullImage = robot.createScreenCapture(screenRect)
            return convertToFxImage(screenFullImage)
        } catch (ex: IOException) {
            print(ex)
            return null
        }
    }

    private fun changeCanvasImage(stackPane: StackPane, screenshotCanvas: Canvas, drawingCanvas: Canvas, cutCanvas:Canvas, image: Image?) {
        if (image != null) {
            drawingCanvas.graphicsContext2D.clearRect(0.0, 0.0, drawingCanvas.width, drawingCanvas.height)
            screenshotCanvas.height = image.height
            screenshotCanvas.width = image.width
            drawingCanvas.height = image.height
            drawingCanvas.width = image.width
            cutCanvas.height = image.height
            cutCanvas.width = image.width
            var screenRect = Rectangle(Toolkit.getDefaultToolkit().screenSize)
            var scaleMult = min(1.0, 0.5 * min(screenRect.getHeight() / image.height, screenRect.getWidth() / image.width))
            var scale = Scale(scaleMult, scaleMult)
            scale.pivotX = 0.0
            scale.pivotY = 0.0
            stackPane.transforms.setAll(scale)
            screenshotCanvas.graphicsContext2D.drawImage(image, 0.0, 0.0)

            stackPane.scene.window.width = image.width * scaleMult + 15
            stackPane.scene.window.height = image.height * scaleMult + defaultToolPanel.height + 70
        }
    }

    @FXML
    private fun onTakeScreenshotButtonClick(event: ActionEvent) {
        timer = timerSlider.value
        val scene = takeScreenshotButton.scene
        val defaultOpacity = scene.window.opacity

        if (hideWindowCheck.isSelected)
        {
            scene.window.opacity = 0.0
            Thread.sleep(100)
        }
        Thread.sleep((timer * 1000).toLong())
        var image = getScreenshot()
        scene.window.opacity = defaultOpacity
        changeCanvasImage(canvasPane, screenshotCanvas, drawingCanvas, cutCanvas, image)
        hasImage = true
        cutButton.isVisible = true
    }

    @FXML
    private fun onCutButtonClick() {
        appMode = AppMode.CUTTING
        defaultToolPanel.isVisible = false
        cutToolPanel.isVisible = true
    }

    @FXML
    private fun onReturnButtonClick() {
        appMode = AppMode.DRAWING
        defaultToolPanel.isVisible = true
        cutToolPanel.isVisible = false
    }

    @FXML
    private fun onCanvasMousePressed(event: MouseEvent) {
        if (!hasImage)
            return
        if (appMode == AppMode.CUTTING && event.button != MouseButton.SECONDARY)
        {
            x_start = event.x
            y_start = event.y
            val cutCanvasCtx = cutCanvas.graphicsContext2D
            cutCanvasCtx.fill = Color.rgb(0, 0, 0, 0.5)
            cutCanvasCtx.fillRect(0.0, 0.0, cutCanvas.width, cutCanvas.height)
        }
        else if (appMode == AppMode.DRAWING)
        {
            val drawingCanvasCtx = drawingCanvas.graphicsContext2D
            if (event.button != MouseButton.SECONDARY) {
                drawingCanvasCtx.beginPath()
                drawingCanvasCtx.moveTo(event.x, event.y)
                drawingCanvasCtx.stroke()
            }
        }
    }

    @FXML
    private fun onCanvasMouseDragged(event: MouseEvent) {
        if (!hasImage)
            return
        if (appMode == AppMode.CUTTING && event.button != MouseButton.SECONDARY)
        {
            val cutCanvasCtx = cutCanvas.graphicsContext2D
            cutCanvasCtx.clearRect(0.0, 0.0, drawingCanvas.width, drawingCanvas.height)
            cutCanvasCtx.fill = Color.rgb(0, 0, 0, 0.5)
            cutCanvasCtx.fillRect(0.0, 0.0, drawingCanvas.width, drawingCanvas.height)
            cutCanvasCtx.clearRect(min(x_start, event.x), min(y_start, event.y), abs(event.x - x_start), abs(event.y - y_start))
        }
        else if (appMode == AppMode.DRAWING)
        {
            val drawingCanvasCtx = drawingCanvas.graphicsContext2D
            val brushSize = brushWidthSpinner.value.toDouble()
            val x = event.x - brushSize / 2.0
            val y = event.y - brushSize / 2.0

            if (event.button == MouseButton.SECONDARY)
            {
                drawingCanvasCtx.clearRect(x, y, brushSize + 10.0, brushSize + 10.0)
            }
            else
            {
                drawingCanvasCtx.lineWidth = brushSize
                drawingCanvasCtx.stroke = brushColorPicker.value
                drawingCanvasCtx.fill = brushColorPicker.value
                drawingCanvasCtx.lineTo(event.x, event.y)
                drawingCanvasCtx.stroke()
                drawingCanvasCtx.closePath()
                drawingCanvasCtx.beginPath()
                drawingCanvasCtx.moveTo(event.x, event.y)
            }
        }
    }

    @FXML
    private fun onCanvasMouseReleased(event: MouseEvent) {
        if (!hasImage)
            return
        val drawingCanvasCtx = drawingCanvas.graphicsContext2D
        if (appMode == AppMode.CUTTING && event.button != MouseButton.SECONDARY)
        {
            val cutCanvasCtx = cutCanvas.graphicsContext2D
            cutCanvasCtx.clearRect(0.0, 0.0, drawingCanvas.width, drawingCanvas.height)
            var params = SnapshotParameters()
            params.fill = Color.TRANSPARENT
            var screenshotSnapshot = screenshotCanvas.snapshot(params, null)
            var drawingSnapshot = drawingCanvas.snapshot(params, null)
            var x = max(min(x_start, event.x), 0.0).toInt()
            var y = max(min(y_start, event.y), 0.0).toInt()
            var width = min(abs(event.x - x_start), screenshotCanvas.width - x).toInt()
            var height = min(abs(event.y - y_start), screenshotCanvas.height - y).toInt()
            var screenshotImage = WritableImage(screenshotSnapshot.pixelReader, x, y, width, height)
            var drawingImage = WritableImage(drawingSnapshot.pixelReader, x, y, width, height)

            changeCanvasImage(canvasPane, screenshotCanvas, drawingCanvas, cutCanvas, screenshotImage)
            drawingCanvasCtx.drawImage(drawingImage, 0.0, 0.0)
        }
        else if (appMode == AppMode.DRAWING)
        {
            if (event.button != MouseButton.SECONDARY && !defaultToolPanel.isHover)
            {
                drawingCanvasCtx.lineTo(event.x, event.y)
                drawingCanvasCtx.stroke()
                drawingCanvasCtx.closePath()
            }
        }
    }

    @FXML
    private fun onOpenMenuClicked() {

    }

    @FXML
    private fun onSaveMenuClicked() {

    }

    @FXML
    private fun onSaveAsMenuClicked() {

    }

    @FXML
    private fun onExitMenuClicked() {
        exitProcess(0)
    }
}
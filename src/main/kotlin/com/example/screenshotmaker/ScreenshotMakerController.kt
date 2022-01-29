package com.example.screenshotmaker

import javafx.event.ActionEvent
import javafx.fxml.FXML
import javafx.scene.SnapshotParameters
import javafx.scene.canvas.Canvas
import javafx.scene.control.*
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.image.WritableImage
import javafx.scene.input.*
import javafx.scene.layout.HBox
import javafx.scene.layout.StackPane
import javafx.scene.paint.Color
import javafx.scene.transform.Scale
import javafx.stage.FileChooser
import java.awt.Rectangle
import java.awt.Robot
import java.awt.Toolkit
import java.awt.image.BufferedImage
import java.awt.image.BufferedImage.TYPE_4BYTE_ABGR
import java.io.*
import javax.imageio.ImageIO
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
    @FXML
    private lateinit var saveMenuItem: MenuItem
    @FXML
    private lateinit var saveAsMenuItem: MenuItem


    private enum class AppMode {
        CUTTING,
        DRAWING
    }

    private var timer = 0.0
    private var hasImage = false
    private var appMode = AppMode.DRAWING
    private var x_start = -1.0
    private var y_start = -1.0
    private var defaultSaveLocation = "\\Documents\\ScreenshotMaker\\screenshots\\"
    private var quickSaveLocation = "\\Documents\\ScreenshotMaker\\quicksave\\"
    private var quickScreenshotName = "quicksave"
    private var saveLocationConfigFilePath = "\\Documents\\ScreenshotMaker\\config\\"
    private var saveLocationConfigFileName = "save"

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

    private fun convertToBufferedImage(image: Image?): BufferedImage? {
        var wr: BufferedImage? = null
        if (image != null) {
            wr = BufferedImage(image.width.toInt(), image.height.toInt(), TYPE_4BYTE_ABGR)
            var pw = image.pixelReader
            for (x in 0 until image.width.toInt()) {
                for (y in 0 until image.height.toInt()) {
                    wr.setRGB(x, y, pw.getArgb(x, y))
                }
            }
        }
        return wr
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

    private fun getPathFromConfig(): String {
        try {
            var file = File(System.getenv("USERPROFILE") + saveLocationConfigFilePath + saveLocationConfigFileName + ".txt")
            if (!file.exists())
            {
                file.parentFile.mkdirs()
                return System.getenv("USERPROFILE") + defaultSaveLocation
            }
            BufferedReader(FileReader(System.getenv("USERPROFILE") + saveLocationConfigFilePath + saveLocationConfigFileName + ".txt")).use { reader ->
                return reader.readLine()
            }
        } catch (e: IOException) {
            e.printStackTrace()
            var file = File(System.getenv("USERPROFILE") + defaultSaveLocation + "screenshot.png")
            if (!file.exists())
                file.parentFile.mkdirs()
            return System.getenv("USERPROFILE") + defaultSaveLocation
        }
    }

    private fun rememberLastSavePath(path: String) {
        try {
            BufferedWriter(PrintWriter(System.getenv("USERPROFILE") + saveLocationConfigFilePath + saveLocationConfigFileName + ".txt")).use { bw ->
                var file = File(path);
                if (file.isFile)
                    bw.write(file.parent)
                else
                    bw.write(path)
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun changeCanvasImage(image: Image?) {
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
            canvasPane.transforms.setAll(scale)
            screenshotCanvas.graphicsContext2D.drawImage(image, 0.0, 0.0)

            canvasPane.scene.window.width = image.width * scaleMult + 15
            canvasPane.scene.window.height = image.height * scaleMult + defaultToolPanel.height + 70
        }
    }

    private fun takeAScreenshot() {
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
        changeCanvasImage(image)
        hasImage = true
        cutButton.isVisible = true
        saveMenuItem.isDisable = false
        saveAsMenuItem.isDisable = false
    }

    private fun getImageFromCanvases(): Image? {
        var params = SnapshotParameters()
        params.fill = Color.TRANSPARENT
        var screenshotImage = screenshotCanvas.snapshot(params, null)
        var drawingImage = drawingCanvas.snapshot(params, null)
        cutCanvas.graphicsContext2D.drawImage(screenshotImage, 0.0, 0.0)
        cutCanvas.graphicsContext2D.drawImage(drawingImage, 0.0, 0.0)
        var resultImage = cutCanvas.snapshot(params, null)
        cutCanvas.graphicsContext2D.clearRect(0.0, 0.0, resultImage.width, resultImage.height)
        return resultImage
    }

    @FXML
    private fun onTakeScreenshotButtonClick(event: ActionEvent) {
        takeAScreenshot()
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

            changeCanvasImage(screenshotImage)
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
        var fileChooser = FileChooser()
        var imageFilter = FileChooser.ExtensionFilter("Image Files", "*.jpg", "*.png")
        fileChooser.extensionFilters.add(imageFilter)
        fileChooser.initialDirectory = File(getPathFromConfig())

        try {
            var file = fileChooser.showOpenDialog(canvasPane.scene.window)
            rememberLastSavePath(file.parent)
            var image = Image(file.toURI().toString());
            changeCanvasImage(image)
            saveMenuItem.isDisable = false
            saveAsMenuItem.isDisable = false
            hasImage = true
            cutButton.isVisible = true
        } catch(e: NullPointerException){
        }
    }

    @FXML
    private fun onSaveMenuClicked() {
        var file: File
        file = File(System.getenv("USERPROFILE") + quickSaveLocation + quickScreenshotName + ".png")

        var resultImage = getImageFromCanvases()
        if (!file.exists())
            file.parentFile.mkdirs()
        ImageIO.write(convertToBufferedImage(resultImage), "png", file)
    }

    @FXML
    private fun onSaveAsMenuClicked() {
        var file: File
        var fileChooser = FileChooser()
        var imageFilter = FileChooser.ExtensionFilter("Image Files", "*.png")
        fileChooser.extensionFilters.add(imageFilter)
        val path = getPathFromConfig()
        fileChooser.initialDirectory = File(path)

        try {
            file = fileChooser.showSaveDialog(canvasPane.scene.window)
            rememberLastSavePath(file.parent)

            var resultImage = getImageFromCanvases()
            ImageIO.write(convertToBufferedImage(resultImage), "png", file)
        } catch (ex: NullPointerException) {
        }
    }

    @FXML
    private fun onExitMenuClicked() {
        exitProcess(0)
    }

    @FXML
    private fun onKeyPressed(e: KeyEvent) {
        when (e.code) {
            KeyCode.T -> {
                if (e.isControlDown) {
                    takeAScreenshot()
                }
            }
            KeyCode.C -> {
                if (e.isControlDown) {
                    val clipboard: Clipboard = Clipboard.getSystemClipboard()
                    val content = ClipboardContent()
                    var resultImage = getImageFromCanvases()
                    content.putImage(resultImage)
                    clipboard.setContent(content)
                }
            }
        }
    }
}
package com.example.screenshotmaker

import javafx.application.Application
import javafx.fxml.FXMLLoader
import javafx.scene.Scene
import javafx.stage.Stage
import java.io.File

class ScreenshotMaker : Application() {

    private var defaultSaveLocation = "\\Documents\\ScreenshotMaker\\screenshots\\"
    private var saveLocationConfigFilePath = "\\Documents\\ScreenshotMaker\\config\\"
    private var quickScreenshotName = "quicksave"
    private var saveLocationConfigFileName = "save"

    private fun checkAllDirectories() {
        var file = File(System.getenv("USERPROFILE") + saveLocationConfigFilePath + saveLocationConfigFileName + ".txt")
        if (!file.exists())
            file.parentFile.mkdirs()
        file = File(System.getenv("USERPROFILE") + defaultSaveLocation + quickScreenshotName + ".png")
        if (!file.exists())
            file.parentFile.mkdirs()
    }

    override fun start(stage: Stage) {
        val fxmlLoader = FXMLLoader(ScreenshotMaker::class.java.getResource("screenshot-styles.fxml"))
        val scene = Scene(fxmlLoader.load(), 1000.0, 100.0)
        stage.title = "ScreenshotMaker"
        stage.scene = scene
        stage.show()
        stage.sizeToScene()
        stage.isResizable = false
        checkAllDirectories()
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            Application.launch(ScreenshotMaker::class.java)
        }
    }
}
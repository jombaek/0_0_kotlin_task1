package com.example.screenshotmaker

import javafx.application.Application
import javafx.fxml.FXMLLoader
import javafx.scene.Scene
import javafx.stage.Stage

class ScreenshotMaker : Application() {

    override fun start(stage: Stage) {
        val fxmlLoader = FXMLLoader(ScreenshotMaker::class.java.getResource("screenshot-styles.fxml"))
        val scene = Scene(fxmlLoader.load(), 1000.0, 100.0)
        stage.title = "ScreenshotMaker"
        stage.scene = scene
        stage.show()
        stage.sizeToScene()
        stage.isResizable = false
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            Application.launch(ScreenshotMaker::class.java)
        }
    }
}
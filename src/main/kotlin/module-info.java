module com.example.screenshotmaker {
    requires javafx.controls;
    requires javafx.fxml;
    requires kotlin.stdlib;
    requires java.desktop;


    opens com.example.screenshotmaker to javafx.fxml;
    exports com.example.screenshotmaker;
}
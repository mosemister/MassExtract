package org.massextract.page;

import javafx.scene.Scene;
import javafx.stage.Stage;

public interface IPage {

    Scene scene();

    void generateScene(Stage stage);
}

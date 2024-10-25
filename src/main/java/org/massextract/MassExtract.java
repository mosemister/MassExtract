package org.massextract;

import javafx.application.Application;
import javafx.stage.Stage;
import org.massextract.page.Pages;

public class MassExtract extends Application {

    private static Stage stageShowing;

    public static void main(String[] args) {
        launch(args);
    }

    public static Stage stage() {
        if (stageShowing == null) {
            throw new RuntimeException("Stage requested but start has not run");
        }
        return stageShowing;
    }

    @Override
    public void start(Stage stage) throws Exception {
        stageShowing = stage;
        Pages.SELECT_INPUT.generateScene(stage);
        stage.setScene(Pages.SELECT_INPUT.scene());
        stage.setHeight(600);
        stage.setWidth(400);
        stage.setTitle("Mass Extract");
        stage.show();
    }
}

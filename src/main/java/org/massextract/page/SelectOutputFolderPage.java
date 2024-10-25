package org.massextract.page;

import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import org.massextract.MassExtract;

import java.io.File;
import java.util.Optional;

public class SelectOutputFolderPage implements IPage {

    private final TextField outputFolder;
    private final Button openDialog;
    private final DirectoryChooser fileChooser = new DirectoryChooser();
    private final CheckBox consolidateFolders;
    private final TextArea exampleArea;
    private final Button nextButton;
    private Scene scene;

    public SelectOutputFolderPage() {
        this.outputFolder = new TextField(Pages.SELECT_INPUT.directory().map(File::getAbsolutePath).orElse(""));
        consolidateFolders = new CheckBox("Consolidate Folders");
        exampleArea = new TextArea();
        openDialog = new Button("...");
        nextButton = new Button("Filter Options");
        init();
    }

    public Optional<File> output() {
        String path = outputFolder.textProperty().get();
        if (path == null || path.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(new File(path));
    }

    private CheckBox consolidateFolders() {
        return this.consolidateFolders;
    }

    public boolean isConsolidatingFolders() {
        return this.consolidateFolders.isSelected();
    }

    private void init() {
        nextButton.setOnMouseClicked(mouseEvent -> {
            Pages.FILTER.generateScene(MassExtract.stage());
            MassExtract.stage().setScene(Pages.FILTER.scene());
        });
        openDialog.setOnMouseClicked((event) -> {
            var folder = fileChooser.showDialog(scene.getWindow());
            if (folder == null) {
                return;
            }
            outputFolder.setText(folder.getAbsolutePath());
        });
        nextButton.disableProperty().bind(outputFolder.textProperty().map(String::isBlank));
        exampleArea.setDisable(true);
        exampleArea.textProperty().bind(consolidateFolders.selectedProperty().map(isSelected -> isSelected ? "FileFromZip1.txt\nFileFromZip2.txt\nFileFromZip3.txt" : "Zip1/FileFromZip1.txt\nZip2/FileFromZip2.txt\nZip3/FileFromZip3.txt"));
    }

    @Override
    public Scene scene() {
        if (this.scene == null) {
            throw new IllegalStateException("Scene must be generated first");
        }
        return this.scene;
    }

    @Override
    public void generateScene(Stage stage) {
        HBox folder = new HBox(this.outputFolder, openDialog);
        HBox.setHgrow(this.outputFolder, Priority.ALWAYS);
        VBox box = new VBox(folder, consolidateFolders, exampleArea, nextButton);
        VBox.setVgrow(exampleArea, Priority.ALWAYS);
        nextButton.prefWidthProperty().bind(stage.widthProperty());

        this.scene = new Scene(box);
        stage.setScene(this.scene);

    }
}

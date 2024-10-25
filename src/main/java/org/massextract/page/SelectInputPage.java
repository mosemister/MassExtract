package org.massextract.page;

import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import org.massextract.MassExtract;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;
import java.util.zip.ZipFile;

public class SelectInputPage implements IPage {

    private final DirectoryChooser fileChooser;
    private final TextField file;
    private final VBox checkBoxes;
    private final LinkedTransferQueue<String> foundFiles = new LinkedTransferQueue<>();
    private final Button selectAllBox;
    private final Button deselectAllBox;
    private final HBox allSelectBox;
    private final Button openDialog;
    private final Button nextButton;
    private Scene scene;

    public SelectInputPage() {
        this.fileChooser = new DirectoryChooser();
        this.file = new TextField();
        this.selectAllBox = new Button("Select all");
        this.deselectAllBox = new Button("Deselect all");
        this.openDialog = new Button("...");
        this.allSelectBox = new HBox(this.selectAllBox, this.deselectAllBox);
        this.selectAllBox.prefWidthProperty().bind(this.allSelectBox.widthProperty().divide(2));
        this.deselectAllBox.prefWidthProperty().bind(this.allSelectBox.widthProperty().divide(2));
        this.checkBoxes = new VBox(allSelectBox);
        this.nextButton = new Button("Select output folder");
        this.nextButton.setDisable(true);
        init();
    }

    public Stream<File> selectedZipFiles() {
        var opDirectory = directory();
        if (opDirectory.isEmpty()) {
            return Stream.empty();
        }
        var directory = opDirectory.get();
        return checkBoxes
                .getChildrenUnmodifiable()
                .stream()
                .filter(node -> node instanceof CheckBox)
                .map(node -> (CheckBox) node)
                .filter(CheckBox::isSelected)
                .map(checkbox -> new File(directory, checkbox.getText()));
    }

    public Optional<File> directory() {
        var text = this.file.getText();
        if (text == null || text.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(new File(text));
    }

    private Stream<String> requiresUpdating() {
        return this
                .foundFiles
                .parallelStream()
                .filter(found -> checkBoxes
                        .getChildrenUnmodifiable()
                        .parallelStream()
                        .filter(node -> node instanceof CheckBox)
                        .map(node -> (CheckBox) node)
                        .noneMatch(node -> node.getText().equals(found)));
    }

    private void init() {
        this.fileChooser.setTitle("Select folder");
        this.selectAllBox.setOnMouseClicked(mouseEvent -> allCheckboxSelectedState(true));
        this.deselectAllBox.setOnMouseClicked(mouseEvent -> allCheckboxSelectedState(false));
        this.file.textProperty().addListener((observableValue, before, after) -> filePathChanged(after));
        this.openDialog.setOnMouseClicked(mouseEvent -> openFileChooser());

        nextButton.setOnMouseClicked(mouseEvent -> {
            Pages.SELECT_OUTPUT_FOLDER.generateScene(MassExtract.stage());
            MassExtract.stage().setScene(Pages.SELECT_OUTPUT_FOLDER.scene());
        });
    }

    private void filePathChanged(String newPath) {
        this.foundFiles.clear();
        File folder = new File(newPath);
        var checkboxesToRemove = this.checkBoxes.getChildrenUnmodifiable().stream().filter(node -> node instanceof CheckBox).toArray(Node[]::new);
        this.checkBoxes.getChildren().removeAll(checkboxesToRemove);
        if (!folder.exists() || folder.isFile()) {
            return;
        }

        var files = folder.listFiles();
        if (files == null || files.length == 0) {
            return;
        }

        var atomicFinished = new AtomicBoolean();
        var thread = new Thread(() -> {
            Stream.of(files).parallel().filter(file -> {
                try {
                    ZipFile zip = new ZipFile(file);
                    zip.close();
                    return true;
                } catch (IOException e) {
                    return false;
                }
            }).forEach(file -> this.foundFiles.add(file.getName()));
            atomicFinished.set(true);
        });
        thread.start();
        while (!atomicFinished.get() || this.foundFiles.hasWaitingConsumer() || thread.isAlive() || !requiresUpdating().toList().isEmpty()) {
            try {
                Thread.sleep(5);
            } catch (InterruptedException e) {
            }

            var toAdd = requiresUpdating().toList();
            if (toAdd.isEmpty()) {
                continue;
            }
            toAdd.forEach(this::registerCheckbox);
        }
    }

    private void registerCheckbox(String name) {
        CheckBox box = new CheckBox(name);
        box.selectedProperty().addListener((observableValue, aBoolean, t1) -> {
            if (t1) {
                nextButton.setDisable(false);
                deselectAllBox.setDisable(false);
                if (isAllCheckboxesInState(true, box)) {
                    selectAllBox.setDisable(true);
                }
            } else {
                selectAllBox.setDisable(false);
                if (isAllCheckboxesInState(false, box)) {
                    deselectAllBox.setDisable(true);
                    nextButton.setDisable(true);
                }
            }
        });
        this.checkBoxes.getChildren().add(box);
    }

    private boolean isAnyCheckboxesInState(boolean state, CheckBox... exceptions) {
        return this.checkBoxes.getChildrenUnmodifiable().stream()
                .filter(node -> node instanceof CheckBox)
                .map(node -> (CheckBox) node)
                .filter(node -> Arrays.asList(exceptions).contains(node))
                .anyMatch(checkbox -> checkbox.isSelected() == state);
    }

    private boolean isAllCheckboxesInState(boolean state, CheckBox... exceptions) {
        return this.checkBoxes.getChildrenUnmodifiable().stream()
                .filter(node -> node instanceof CheckBox)
                .map(node -> (CheckBox) node)
                .filter(node -> Arrays.asList(exceptions).contains(node))
                .allMatch(checkbox -> checkbox.isSelected() == state);
    }

    private void allCheckboxSelectedState(boolean state) {
        checkBoxes
                .getChildrenUnmodifiable()
                .stream()
                .filter(node -> node instanceof CheckBox)
                .map(node -> (CheckBox) node)
                .forEach(checkbox -> checkbox.setSelected(state));

    }

    private void openFileChooser() {
        File file = this.fileChooser.showDialog(scene().getWindow());
        if (file == null) {
            return;
        }
        this.file.setText(file.getAbsolutePath());
    }

    @Override
    public Scene scene() {
        if (this.scene == null) {
            throw new IllegalStateException("Generate scene first");
        }
        return this.scene;
    }

    @Override
    public void generateScene(Stage stage) {
        var box = new HBox(file, this.openDialog);
        VBox root = new VBox(box, checkBoxes, nextButton);
        checkBoxes.setFillWidth(true);

        VBox.setVgrow(checkBoxes, Priority.ALWAYS);
        HBox.setHgrow(this.file, Priority.ALWAYS);
        scene = new Scene(root);
        stage.setScene(scene);

        root.prefWidthProperty().bind(scene.widthProperty());
        checkBoxes.prefWidthProperty().bind(scene.widthProperty());
        box.prefWidthProperty().bind(scene.widthProperty());
        nextButton.prefWidthProperty().bind(scene.widthProperty());

    }

}

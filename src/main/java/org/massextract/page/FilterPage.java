package org.massextract.page;

import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.massextract.MassExtract;

import java.util.Arrays;
import java.util.function.Predicate;
import java.util.zip.ZipEntry;

public class FilterPage implements IPage {

    private static final String COMBO_NO_FILTERS = "No Filters";
    private static final String COMBO_FILE_TYPE = "File Type";
    private final ComboBox<String> filterSelect;
    private final TextField fileTypes;
    private final Label infoLabel;
    private final Button nextButton;
    private final Pane spacer;
    private Scene scene;

    public FilterPage() {
        spacer = new Pane();
        filterSelect = new ComboBox<>();
        fileTypes = new TextField();
        this.infoLabel = new Label();
        this.nextButton = new Button("Start");
        init();
    }

    private void init() {
        fileTypes.setVisible(true);
        filterSelect.getItems().add(COMBO_NO_FILTERS);
        filterSelect.getItems().add(COMBO_FILE_TYPE);
        filterSelect.getSelectionModel().select(COMBO_NO_FILTERS);

        var filteringByFileType = filterSelect.getSelectionModel().selectedItemProperty().map(string -> string.equals(COMBO_FILE_TYPE));
        fileTypes.visibleProperty().bind(filteringByFileType);

        infoLabel.textProperty().bind(filterSelect.getSelectionModel().selectedItemProperty().map(string -> switch (string) {
            case COMBO_NO_FILTERS -> "All files will be extracted";
            case COMBO_FILE_TYPE -> "Filter by the file type. Use comma (,) to add multiple. Example: \n .png, .jpeg";
            default -> "";
        }));

        nextButton.setOnMouseClicked(mouseEvent -> {
            Pages.START.generateScene(MassExtract.stage());
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            Pages.START.startGenerating();
        });
    }

    public Predicate<ZipEntry> filter() {
        return switch (filterSelect.getSelectionModel().getSelectedItem()) {
            case COMBO_FILE_TYPE ->
                    (file -> Arrays.stream(fileTypes.getText().split(",")).anyMatch(fileType -> file.getName().toLowerCase().endsWith(fileType.toLowerCase().trim())));
            case COMBO_NO_FILTERS -> (file -> true);
            default ->
                    throw new IllegalStateException("Unexpected value: " + filterSelect.getSelectionModel().getSelectedItem());
        };
    }

    @Override
    public Scene scene() {
        if (scene == null) {
            throw new IllegalStateException("Scene must be generated first");
        }
        return this.scene;
    }

    @Override
    public void generateScene(Stage stage) {
        VBox box = new VBox(this.filterSelect, this.fileTypes, this.infoLabel, this.spacer, this.nextButton);
        VBox.setVgrow(this.spacer, Priority.ALWAYS);
        this.filterSelect.prefWidthProperty().bind(box.widthProperty());
        this.fileTypes.prefWidthProperty().bind(box.widthProperty());
        this.nextButton.prefWidthProperty().bind(box.widthProperty());
        this.scene = new Scene(box);
        stage.setScene(this.scene);
    }
}

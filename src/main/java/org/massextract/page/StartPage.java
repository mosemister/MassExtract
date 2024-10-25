package org.massextract.page;

import javafx.concurrent.Task;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.massextract.page.component.ExtractionProgress;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.function.Predicate;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class StartPage implements IPage {

    private static final String TITLE_GENERATING_STRUCTURE = "Extracting";
    private static final String TITLE_WAITING = "Waiting for ready";
    private final Label title;
    private final VBox progress = new VBox();
    private final ScrollPane scroll;
    private Scene scene;

    public StartPage() {
        title = new Label(TITLE_WAITING);
        scroll = new ScrollPane(this.progress);
        init();
    }

    private void init() {
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
    }

    public void startGenerating() {
        this.title.textProperty().set(TITLE_GENERATING_STRUCTURE);
        var output = Pages.SELECT_OUTPUT_FOLDER.output().orElseThrow();
        var selectedFiles = Pages.SELECT_INPUT.selectedZipFiles().toList();

        for (var selectedZip : selectedFiles) {
            ExtractionProgress progress = new ExtractionProgress();
            progress.maxHeightProperty().bind(this.progress.heightProperty().divide(2));
            progress.prefWidthProperty().bind(scroll.widthProperty());
            progress.label().textProperty().set(selectedZip.getName());

            var task = new Task<Void>() {
                @Override
                protected Void call() throws Exception {
                    ZipFile zip = new ZipFile(selectedZip);
                    Predicate<ZipEntry> filter = Pages.FILTER.filter();
                    boolean isConsolidatingFolders = Pages.SELECT_OUTPUT_FOLDER.isConsolidatingFolders();
                    var entries = zip.stream().filter(filter).toList();
                    updateProgress(0, entries.size());
                    for (int i = 0; i < entries.size(); i++) {
                        var entry = entries.get(i);
                        if (entry.isDirectory()) {
                            updateProgress(i + 1, entries.size());
                            continue;
                        }
                        File placing = new File(output, (isConsolidatingFolders ? "" : selectedZip.getName() + "/") + entry.getName());
                        placing.getParentFile().mkdirs();
                        placing.createNewFile();
                        var is = zip.getInputStream(entry);
                        var fileWriter = new FileOutputStream(placing);
                        is.transferTo(fileWriter);
                        updateProgress(i + 1, entries.size());
                    }
                    updateProgress(entries.size(), entries.size());
                    zip.close();
                    return null;
                }
            };

            progress.progressProperty().bind(task.progressProperty());
            progress.label().textProperty().bind(task.progressProperty().map(percent -> selectedZip.getName() + " (" + (percent.doubleValue() * 100) + "%)"));

            Thread currentTask = new Thread(task);
            currentTask.start();

            this.progress.getChildren().add(progress);
        }

    }

    @Override
    public Scene scene() {
        if (this.scene == null) {
            throw new RuntimeException("Must generate before");
        }
        return this.scene;
    }

    @Override
    public void generateScene(Stage stage) {
        VBox box = new VBox(this.title, scroll);
        VBox.setVgrow(scroll, Priority.ALWAYS);
        scene = new Scene(box);
        stage.setScene(scene);
    }
}

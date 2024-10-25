package org.massextract.page.component;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;


public class ExtractionProgress extends VBox {

    private final Label label;
    private final ProgressBar progressBar;
    private final ObjectProperty<Color> progressColourProperty = new SimpleObjectProperty<>(Color.CYAN);

    public ExtractionProgress() {
        this.label = new Label();
        this.progressBar = new ProgressBar();
        init();
    }

    public DoubleProperty progressProperty() {
        return this.progressBar.progressProperty();
    }

    public ObjectProperty<Color> paintProperty() {
        return this.progressColourProperty;
    }

    private void init() {
        //this.progressBar.styleProperty().bind(progressColourProperty.map(paint -> "fx-accent: rgb(" + paint.getRed() + ", " + paint.getGreen() + ", " + paint.getBlue() + ")"));

        this.label.prefWidthProperty().bind(this.widthProperty());
        this.progressBar.prefWidthProperty().bind(this.widthProperty());
        this.label.prefHeightProperty().bind(this.heightProperty().divide(2));
        this.progressBar.prefHeightProperty().bind(this.heightProperty().divide(2));

        var children = this.getChildren();
        children.addAll(label, progressBar);
    }

    public Label label() {
        return this.label;
    }
}

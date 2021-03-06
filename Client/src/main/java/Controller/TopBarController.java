package Controller;

import Controller.Library.RAMTClientHelper;
import application.Launcher;
import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXProgressBar;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.IOException;

/**
 * The Top bar controller. Handles all tasks relating to the Top bar.
 */
public class TopBarController extends AnchorPane {
    private final Stage stage;
    private final Stage helpStage = new Stage();
    private final RAMTClientHelper helper = new RAMTClientHelper(helpStage);

    private double xOffset = 0;
    private double yOffset = 0;

    private Boolean isMaximised = false;
    private double prevWidth;
    private double prevHeight;
    private double prevX;
    private double prevY;

    @FXML JFXButton btnHelp, btnMin, btnMax, btnExit;
    @FXML JFXProgressBar progressBarState;
    @FXML Label lblState;


    /**
     * Constructs the ButtonBar and loads its FXML file. Does a lot of the undecorated
     * window processing.
     * @param progressProperty The property of what to be bind the progress value to for the progress bar & state.
     */
    public TopBarController(ReadOnlyDoubleProperty progressProperty, ReadOnlyStringProperty stateProperty) {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/View/TopBar.fxml"));
        this.getStylesheets().add(getClass().getResource("/CSS/Launcher.css").toExternalForm());
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);

        try {
            fxmlLoader.load();
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }

        this.stage = Launcher.MainStart.getStage();

        applyEventHandlers();
        progressBarState.progressProperty().bind(progressProperty);
        lblState.textProperty().bind(stateProperty);
    }

    /**
     * Applies this panes event handlers included those related to the stage.
     */
    public void applyEventHandlers() {
        this.setOnMousePressed(event -> { // These next two control window movement
            xOffset = event.getSceneX();
            yOffset = event.getSceneY();
        });

        this.setOnMouseDragged(event -> {
            stage.setX(event.getScreenX() - xOffset);
            stage.setY(event.getScreenY() - yOffset);
            isMaximised = false;
        });

        btnExit.setOnMouseClicked(event -> RootController.exitApp(0));

        btnMax.setOnMouseClicked(event -> {
            ObservableList<Screen> screens = Screen.getScreensForRectangle(new Rectangle2D(stage.getX(), stage.getY(), stage.getWidth(), stage.getHeight()));

            if (!isMaximised) {
                Rectangle2D bounds = screens.get(0).getVisualBounds();
                prevX = stage.getX();
                prevY = stage.getY();

                stage.setX(bounds.getMinX());
                stage.setY(bounds.getMinY());

                prevWidth = stage.getWidth();
                prevHeight = stage.getHeight();

                stage.setWidth(bounds.getWidth());
                stage.setHeight(bounds.getHeight());

                isMaximised = true;
            } else {
                stage.setWidth(prevWidth);
                stage.setHeight(prevHeight);

                stage.setX(prevX);
                stage.setY(prevY);

                isMaximised = false;
            }
        });

        btnMin.setOnMouseClicked(event -> stage.setIconified(true));

        btnHelp.setOnMouseClicked(event -> {
            if (!helpStage.isShowing()) {
                try {
                    helpStage.initStyle(StageStyle.UNDECORATED);
                    helpStage.setScene(new Scene(helper));
                    helpStage.getIcons().add(new Image("file:src/main/resources/openramt.png"));
                    helpStage.setTitle("OpenRAMT Helper");
                    helpStage.setAlwaysOnTop(true);
                } catch (IllegalStateException ignored) {}
                helpStage.show();
            } else {
                helpStage.requestFocus();
            }
        });
    }
}



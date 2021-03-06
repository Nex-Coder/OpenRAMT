package Controller;

import Controller.Content.*;

import Controller.Library.Services.MonitoringService;
import Controller.Library.Services.TaskProgressiveService;
import Controller.Library.SideButton;
import Model.General.AppPermission;
import Model.Task.Task;
import Model.Task.TaskRequest;
import Model.User.UserData;
import application.Launcher;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import java.io.IOException;

/**
 * The root pains controller.
 * Handles most of the post-initial tasks as well as the many generic (non-specific pane) tasks.
 */
public class RootController extends AnchorPane {
	@FXML Pane resizeHelperR;
	@FXML Pane resizeHelperB;

	private final MainContentController mcc;
	private final SideBarController sbc;

	private static UserData loggedInUser;

	private static MonitorController mc;

	private static final TaskProgressiveService taskService = new TaskProgressiveService(null);


	/**
	 * Constructs the AnchorPane and does a bulk of the post-initial tasks (First tasks after completing launch).
	 * @param user The user defined as logged in by the LoginProgressiveService preferably. Check will not be
	 *                     done by this class so it is expected you test the UserData before hand using the previous
	 *                     class.
	 */
	public RootController(UserData user) {
		loggedInUser = user;

		taskService.setRequest(new TaskRequest<>(Task.TESTING, loggedInUser));

		System.out.println(user.getHost());

		FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/View/Root.fxml"));
		this.getStylesheets().add(getClass().getResource("/CSS/Launcher.css").toExternalForm());
		fxmlLoader.setRoot(this);
		fxmlLoader.setController(this);

		try {
			fxmlLoader.load();
		} catch (IOException exception) {
			throw new RuntimeException(exception);
		}

		mcc = new MainContentController(new WelcomeController());
		sbc = new SideBarController(mcc);
		mc = new MonitorController();

		addInitialChildren();
		applyEventHandlers();
	}

	private void addInitialChildren() {
		// Top Bar
		TopBarController tb = new TopBarController(taskService.progressProperty(),taskService.messageProperty());
		this.getChildren().add(tb);

		// Content Wizard
		this.getChildren().add(mcc);

		// Side Bar
		ScrollPane sbScroll = new ScrollPane(sbc);

		sbScroll.setStyle("-fx-background-color: #474343;");
		AnchorPane.setTopAnchor(sbScroll, 26.0);
		AnchorPane.setBottomAnchor(sbScroll, 0.0);
		AnchorPane.setLeftAnchor(sbScroll, 0.0);
		sbScroll.setMinWidth(100);
		sbScroll.setPrefWidth(100);
		sbScroll.setMaxWidth(100);
		sbScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
		sbScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);



		this.getChildren().add(sbScroll);
		if (loggedInUser.getObjGroup().isAdmin() || loggedInUser.getObjGroup().isGeneral()) {
			addPaneButton(new GeneralController(), "General", AppPermission.GENERAL, sbc.getLastIndex()+1);
		}
		if (loggedInUser.getObjGroup().isAdmin()) {
			addPaneButton(new AdminController(), "Admin", AppPermission.ADMINISTRATOR, sbc.getLastIndex()+1);
		}
		if (loggedInUser.getObjGroup().isAdmin() || loggedInUser.getObjGroup().isPower()) {
			addPaneButton(new PowerController(), "Power", AppPermission.POWER, sbc.getLastIndex()+1);
		}
		if (loggedInUser.getObjGroup().isAdmin() || loggedInUser.getObjGroup().isProcess()) {
			addPaneButton(new ProcessController(), "Process", AppPermission.PROCESS, sbc.getLastIndex()+1);
		}
		if (loggedInUser.getObjGroup().isAdmin() || loggedInUser.getObjGroup().isMonitoring()) {
			addPaneButton(mc, "Monitoring", AppPermission.MONITORING, sbc.getLastIndex()+1);
		}
		if (loggedInUser.getObjGroup().isAdmin()) {
			addPaneButton(new SettingsController(), "Server Settings", AppPermission.ADMINISTRATOR, sbc.getLastIndex()+1);
		}



		resizeHelperR.toFront();
		resizeHelperB.toFront();
		tb.toFront(); // This one should be unneeded. But avoids some unexpected behavior found 'only' in testing.
	}

	private void addPaneButton(Node child, String btnName, AppPermission buttonPerm, int index) {
		mcc.getChildren().add(child);
		sbc.addButton(new SideButton(btnName, buttonPerm), index);
	}

	private void applyEventHandlers() {
		UndecoratedResizable.addResizeListener(Launcher.MainStart.getStage(), this, true);
	}

	public static TaskProgressiveService getTaskService() {
		return taskService;
	}

	public static UserData getLoggedInUser() {
		return loggedInUser;
	}

	/**
	 * A static method that helps cut down on duplicate code by providing an easy way to use an instance of TaskService.
	 * As long as this RootController specifies a way to get an instance of the task service then this method will
	 * handle most aspects of starting a task request, including detection of client currently processing a request.
	 * @param taskRequest The request to be made to the current server via the running service.
	 * @return The request ID for convenience. Can be safely ignored.
	 */
	public static String requestStart(TaskRequest<?> taskRequest) {
		if (RootController.getTaskService().isRunning()) {
			Alert alert = RAMTAlert.getAlertMidTask();
			alert.showAndWait();

			if (alert.getResult() == ButtonType.OK) {
				return RootController.getTaskService().updateAndRestart(taskRequest);
			}
		} else {
			return RootController.getTaskService().updateAndRestart(taskRequest);
		}
		return null;
	}

	/**
	 * Does the same gestured work of requestStart but is designed for automated work where the user didn't request at
	 * least part of the task directly. Thus this version will skip any confirmation checks for the user.
	 *
	 * A static method that helps cut down on duplicate code by providing an easy way to use an instance of TaskService.
	 * As long as this RootController specifies a way to get an instance of the task service then this method will
	 * handle most aspects of starting a task request, including detection of client currently processing a request.
	 * @param taskRequest The request to be made to the current server via the running service.
	 * @return The request ID for convenience. Can be safely ignored.
	 */
	public static String requestAutomatedStart(TaskRequest<?> taskRequest) {
		return RootController.getTaskService().updateAndRestart(taskRequest);
	}

	/**
	 * This method is intended to be used as the de facto way for the application to be shut down. It will shutdown
	 * all threads via utilising the System.exit method. Before that it will try to close any sockets still open.
	 * If they take too long the application will be forced to close. The JavaFX thread will be the first to close.
	 *
	 * @param exitCode The exit code to pass to System.exit(int). 0 is recommended especially if nothing went wrong.
	 */
	public static void exitApp(int exitCode) {
		Platform.exit();
		mc.stopMonitoringService();
		System.exit(exitCode);
	}

	@FXML
	public void exitApplication(ActionEvent event) {
		exitApp(0);
	}
}

package bp.roadnetworkpartitioning;

import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.Group;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.util.Map;


/**
 * Controller of main window of the app.
 * @author Lucie Roy
 * @version 27-03-2023
 */
public class MainController {
    /** Instance of main stage. */
    private static Stage stage = null;
    /** Graph to show. */
    private Graph graph = null;
    /** Instance of ScrollPane containing graph. */
    @FXML
    private ScrollPane scrollPane;
    /** TextField with zoom value. */
    @FXML
    private TextField textZoom;
    /** VBox containing radio buttons of all available
     * algorithms and buttons with their setting. */
    @FXML
    private VBox vboxRadioBtn;
    /** Label with zoom value. */
    @FXML
    private Label labelZoom;
    /** Default zoom value. */
    private static int zoom = 10;
    /** Default size of vertex. */
    private static final int size = 5;
    /** Toggle group containing radio buttons of all available algorithms. */
    @FXML
    private ToggleGroup group;
    /** Map with all available graph partitioning algorithms. */
    private static Map<String, IPartitioning> algorithms;

    /**
     * Displays all available graph partitioning algorithms.
     */
    public void setAlgorithms(){
        AlgorithmsLoader.findAlgorithms();
        algorithms = AlgorithmsLoader.getAlgorithms();
        for (String key : algorithms.keySet()) {
            HBox hBox = new HBox(20);
            RadioButton radioButton = new RadioButton();
            radioButton.setText(key);
            radioButton.setId(key);
            radioButton.setToggleGroup(group);
            Button btnSetting = new Button("Setting");
            btnSetting.getStyleClass().setAll("btn","btn-primary");
            btnSetting.setOnAction(e -> showSettingDialog(key));
            hBox.getChildren().addAll(radioButton, btnSetting);
            vboxRadioBtn.setPadding(new Insets(5, 5, 5, 50));
            vboxRadioBtn.getChildren().add(hBox);
        }
    }

    /**
     * Shows setting dialog of given algorithm.
     * @param key   name of the algorithm.
     */
    private void showSettingDialog(String key) {
        IPartitioning algorithm = algorithms.get(key);
        try {
            SettingDialog dialog = new SettingDialog(stage, algorithm);
            dialog.showAndWait().ifPresent(graph -> {

            });
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    /**
     * Method called when MenuItem "Insert Graph" is clicked.
     * This method opens a dialog for choosing a file with vertices coordinates.
     */
    @FXML
    protected void onInsertGraphJSONMenuClick(){
        FileChooser fileChooser = new FileChooser();
        File selectedFile = fileChooser.showOpenDialog(stage);
        this.graph = JSONParser.readFile(selectedFile);
        visualizeGraph(zoom);
    }

    /**
     * Method called on +5 button click.
     * Increasing zoom by 5.
     */
    @FXML
    protected  void onIncreaseButtonClick(){
        zoom += 5;
        labelZoom.setText(""+zoom);
        visualizeGraph(zoom);
    }

    /**
     * Method called on -5 button click.
     * Decreasing zoom by 5.
     */
    @FXML
    protected  void onDecreaseButtonClick(){
        zoom -= 5;
        labelZoom.setText(""+zoom);
        visualizeGraph(zoom);
    }

    /**
     * Method called when zoom button clicked.
     * This method zooms graph.
     */
    @FXML
    protected  void onZoomButtonClick(){
        zoom = getNumberFromString(textZoom.getText());
        labelZoom.setText(""+zoom);
        visualizeGraph(zoom);
    }

    /**
     * Method called when button "Create graph!" is clicked.
     * This method opens a dialog for choosing a file with vertices coordinates.
     */
    @FXML
    protected void onCreateGraphMenuClick(){
        try {
            CreateGraphDialog dialog = new CreateGraphDialog(stage);
            dialog.showAndWait().ifPresent(graph -> {
                this.graph = graph;
                visualizeGraph(zoom);
            });
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    /**
     * Visualizes attribute graph.
     * @param zoom  how much bigger the graph should be.
     */
    private void visualizeGraph(int zoom){
        Group group = new Group();
        for(Vertex vertex: this.graph.getVertices().values()){
            group.getChildren().add(new Circle(vertex.getX()*zoom, vertex.getY()*zoom, size));
            for(int j = 0; j < vertex.getStartingEdges().size(); j++){
                Edge edge = vertex.getStartingEdges().get(j);
                group.getChildren().add(new Line(vertex.getX()*zoom, vertex.getY()*zoom,
                        edge.getEndpoint().getX()*zoom, edge.getEndpoint().getY()*zoom));
            }
        }
        scrollPane.setPrefSize(1000, 1000);
        scrollPane.setContent(group);
    }


    /**
     * Method called when button "Create JSON File!" is clicked.
     * This method uses method createJSONFile from JSONParser class for creating JSON file.
     * And gets details from textFields for correct parsing of files.
     */
    @FXML
    protected void onCreateJSONMenuClick(){
        try {
            CreateJSONDialog dialog = new CreateJSONDialog(stage);
            dialog.showAndWait().ifPresent(System.out::println);
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    /**
     * Gets positive integer from string.
     * @param text      String to convert to integer.
     * @return  integer made from string.
     */
    public static int getNumberFromString(String text){
        if(text.trim().matches("\\d+")){
            return Integer.parseInt(text.trim());
        }else {
            return -1;
        }
    }

    /**
     * Sets primary stage.
     * @param primaryStage  Instance of Stage.
     */
    public static void setStage(Stage primaryStage){
        if(stage == null) {
            stage = primaryStage;
        }
    }
}
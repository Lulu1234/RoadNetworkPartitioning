package bp.roadnetworkpartitioning;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;


/**
 * Controller of main window of the app.
 * @author Lucie Roy
 * @version 28-12-2023
 */
public class MainController {
    /** Instance of main stage. */
    private static Stage stage = null;
    @FXML
    private TextArea progressMessages;
    /** Default zoom value. */
    private double zoom = 10;
    /** Default zoom value. */
    private int moveHorizontally = 0;
    /** Default zoom value. */
    private int moveVertically = 0;
    /** Stores color of each part. */
    private Color[] colors = {Color.BLUE, Color.RED};
    /** Sets number of parts. */
    @FXML
    private Spinner<Integer> spinnerPartCount;
    /** Instance of ScrollPane containing graph. */
    @FXML
    private ScrollPane scrollPane;
    /** TextField with zoom value. */
    @FXML
    private TextField textZoom;
    /**
     * VBox containing radio buttons of all available
     * algorithms and buttons with their setting.
     */
    @FXML
    private VBox vboxRadioBtn;
    /** Label with zoom value. */
    @FXML
    private Label labelZoom;
    /** Toggle group containing radio buttons of all available algorithms. */
    @FXML
    private ToggleGroup group;
    /** Graph to show. */
    private Graph graph = null;
    /** Computed partition of the graph. */
    private GraphPartition graphPartition = null;
    /**  */
    private Map<String, APartitionAlgorithm> algorithms = new HashMap<>();

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

    /**
     * Displays all available graph partitioning algorithms.
     */
    public void setAlgorithms(){
        algorithms = AlgorithmsLoader.findAlgorithms();
        for (Map.Entry<String, APartitionAlgorithm> algorithm: algorithms.entrySet()) {
            HBox hBox = new HBox(10);
            RadioButton radioButton = new RadioButton();
            radioButton.setText(algorithm.getKey());
            radioButton.setUserData(algorithm.getValue());
            radioButton.setId(algorithm.getKey());
            radioButton.setToggleGroup(group);
            Button btnSetting = new Button("Setting");
            btnSetting.getStyleClass().setAll("btn","btn-primary");
            btnSetting.setOnAction(e -> showSettingDialog(algorithm.getValue()));
            Button btnCalculate = new Button("Calculate");
            btnCalculate.getStyleClass().setAll("btn","btn-success");
            btnCalculate.setOnAction(e -> {
                progressMessages.appendText("Partitioning Graph by " + algorithm.getKey() + "...\n");
                Task<Void> graphPartitioningTask = new Task<>() {
                    @Override
                    protected Void call() {
                        algorithm.getValue().getGraphPartition(graph, spinnerPartCount.getValue());
                        progressMessages.appendText("Partitioning is done.\n");
                        return null;
                    }
                };
                Thread graphPartitioningThread = new Thread(graphPartitioningTask);
                graphPartitioningThread.setDaemon(true);
                graphPartitioningThread.start();
            });
            hBox.getChildren().addAll(radioButton, btnSetting, btnCalculate);
            hBox.setAlignment(Pos.CENTER);
            vboxRadioBtn.getChildren().add(hBox);
        }
        group.selectedToggleProperty().addListener((ov, old_toggle, new_toggle) -> {

            if (group.getSelectedToggle() != null) {
                APartitionAlgorithm algorithm = (APartitionAlgorithm) group.getSelectedToggle().getUserData();
                progressMessages.appendText("Getting Graph Partition by "+ algorithm.getName() +"...\n");
                graphPartition = algorithm.getGraphPartition(graph);
                if (graphPartition != null) {
                    progressMessages.appendText("Graph Partition was displayed.\n");
                }
                else {
                    progressMessages.appendText("Graph Partition could not be displayed.\n");
                }
                visualizeGraph();
            }

        });
    }

    /**
     * Method called when MenuItem "Insert Graph" is clicked.
     * This method opens a dialog for choosing a json file with graph parameters.
     */
    @FXML
    protected void onInsertGraphJSONMenuClick(){
        FileChooser fileChooser = new FileChooser();
        File selectedFile = fileChooser.showOpenDialog(stage);
        progressMessages.appendText("Reading selected file...\n");
        Task<Void> insertGraphTask = new Task<>() {
            @Override
            protected Void call() {
                MainController.this.graph = JSONParser.readFile(selectedFile);
                MainController.this.graphPartition = null;
                progressMessages.appendText("Reading is done, visualizing graph...\n");
                return null;
            }

            @Override
            protected void succeeded() {
                super.succeeded();
                visualizeGraph();
            }

            @Override
            protected void failed() {
                super.failed();
                progressMessages.appendText("Something went wrong...\n");
            }
        };
        Thread insertGraphThread = new Thread(insertGraphTask);
        insertGraphThread.setDaemon(true);
        insertGraphThread.start();
    }

    /**
     * Method called on +5 button click.
     * Increasing zoom by 5.
     */
    @FXML
    protected void onIncreaseButtonClick(){
        zoom += 5;
        labelZoom.setText(""+zoom);
        progressMessages.appendText("Zoomed.\n");
        visualizeGraph();
    }

    /**
     * Method called on -5 button click.
     * Decreasing zoom by 5.
     */
    @FXML
    protected void onDecreaseButtonClick(){
        zoom -= 5;
        labelZoom.setText(""+zoom);
        progressMessages.appendText("Zoomed.\n");
        visualizeGraph();
    }

    /**
     * Method called when zoom button clicked.
     * This method zooms graph.
     */
    @FXML
    protected void onZoomButtonClick(){
        try {
            zoom = Double.parseDouble(textZoom.getText());
            labelZoom.setText("" + zoom);
            progressMessages.appendText("Zoomed.\n");
            visualizeGraph();
        } catch (NumberFormatException e) {
            progressMessages.appendText("Zoom value must be number (double).\n");
        }
    }

    /**
     * Called when button "Pick colors" is clicked.
     * It opens dialog with color picker for each part.
     */
    @FXML
    protected void onPickColorsButtonClick() {
        try {
            PartColorsDialogController dialog = new PartColorsDialogController(stage, spinnerPartCount.getValue(), colors);
            dialog.showAndWait().ifPresent(pickedColors -> {
                colors = pickedColors;
                progressMessages.appendText("You picked these colors: " + Arrays.toString(colors) + ". Repainting...\n");
                visualizeGraph();
            });
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    /**
     *
     */
    @FXML
    protected void onShowStatisticsButtonClick() {
        try {
            StatisticsDialogController dialog = new StatisticsDialogController(stage, algorithms);
            dialog.showAndWait();
        } catch (Exception e){
            progressMessages.appendText("Statistics could not be displayed.\n");
            e.printStackTrace();
        }
    }

    /**
     * Method called when button "Create graph!" is clicked.
     * This method opens a dialog for inserting graph parameters.
     */
    @FXML
    protected void onCreateGraphMenuClick(){
        try {
            GraphDialogController dialog = new GraphDialogController(stage);
            dialog.showAndWait().ifPresent(graph -> {
                this.graph = graph;
                this.graphPartition = null;
                visualizeGraph();
            });
            progressMessages.appendText("Graph was successfully generated.\n");
        } catch (Exception e){
            progressMessages.appendText("Something went wrong when generating graph.\n");
            e.printStackTrace();
        }
    }

    /**
     * Method called when button "Create JSON File!" is clicked.
     * It opens dialog for entering details and files for requested JSON file.
     */
    @FXML
    protected void onCreateJSONMenuClick(){
        try {
            JSONDialogController dialog = new JSONDialogController(stage);
            dialog.showAndWait().ifPresent(isCreated -> {
                if (isCreated) {
                    progressMessages.appendText("JSON file was created.\n");
                }
                else {
                    progressMessages.appendText("JSON file could not be created.\n");

                }
            });
        } catch (Exception e){
            progressMessages.appendText("Something went wrong when creating JSON file.\n");
            e.printStackTrace();
        }
    }

    @FXML
    protected void onTestMenuClick() {
        try {
            TestDialogController dialog = new TestDialogController(stage, algorithms, graph, spinnerPartCount.getValue());
            dialog.showAndWait();
        } catch (Exception e){
            progressMessages.appendText("Something went wrong when testing.\n");
            e.printStackTrace();
        }
    }

    /**
     * Shows setting dialog of given algorithm.
     * @param algorithm   name of the algorithm.
     */
    private void showSettingDialog(APartitionAlgorithm algorithm) {
        try {
            SettingDialogController dialog = new SettingDialogController(stage, algorithm);
            dialog.showAndWait().ifPresent(apply -> {
                if (group.getSelectedToggle() != null) {

                    graphPartition = algorithm.getGraphPartition(graph, spinnerPartCount.getValue());
                    visualizeGraph();
                }
            });
        } catch (Exception e){
            progressMessages.appendText("Something went wrong when setting things up.\n");
            e.printStackTrace();
        }
    }

    /**
     * Visualizes graph.
     */
    private void visualizeGraph(){
        if (this.graph == null) {
            progressMessages.appendText("No graph to visualize.\n");
            return;
        }
        Group group = new Group();
        if (graphPartition == null) {
            progressMessages.appendText("No Graph Partition available, visualizing plain graph with "
                    + this.graph.getVertices().size() + " vertices and " + this.graph.getEdges().size() + " edges.\n");
            Task<Void> drawGraphTask = new Task<>() {
                @Override
                protected Void call() {
                    drawGraph(group, MainController.this.graph, Color.BLACK);
                    return null;
                }

                @Override
                protected void succeeded() {
                    super.succeeded();
                    setGraphDrawing(group);
                }

                @Override
                protected void failed() {
                    super.failed();
                    progressMessages.appendText("Something went wrong.\n");
                }
            };
            Thread drawGraphThread = new Thread(drawGraphTask);
            drawGraphThread.setDaemon(true);
            drawGraphThread.start();

        }
        else {
            progressMessages.appendText("Visualizing Graph Partition with " +
                     this.graph.getVertices().size() + " vertices and " + this.graph.getEdges().size() + " edges...\n");
            Task<Void> drawGraphTask = new Task<>() {
                @Override
                protected Void call() {
                    for (int i = 0; i < spinnerPartCount.getValue(); i++) {
                        Color color = colors.length > i ? colors[i] : Color.BLACK;
                        drawGraph(group, graphPartition.getGraphComponents().get(i), color);
                    }
                    return null;
                }

                @Override
                protected void succeeded() {
                    super.succeeded();
                    setGraphDrawing(group);
                }

                @Override
                protected void failed() {
                    super.failed();
                    progressMessages.appendText("Something went wrong.\n");
                }
            };
            Thread drawGraphThread = new Thread(drawGraphTask);
            drawGraphThread.setDaemon(true);
            drawGraphThread.start();
        }

    }

    private void setGraphDrawing(Group group) {
        scrollPane.setPrefSize(1000, 1000);
        scrollPane.setContent(group);
        progressMessages.appendText("Visualizing is done.\n");
    }

    /**
     * Draws graph or one graph component/part.
     * @param group group where graph belongs.
     * @param graph the graph.
     * @param color color of the graph.
     */
    private void drawGraph(Group group, Graph graph, Color color){
        int size = 5;
        for(Vertex vertex: graph.getVertices().values()){
            Circle circle = new Circle((vertex.getX()*zoom) + moveHorizontally, (vertex.getY()*zoom) + moveVertically, size);
            circle.setStroke(color);
            circle.setFill(color);
            group.getChildren().add(circle);
            for(Edge edge: vertex.getStartingEdges()){
                Line line = new Line((vertex.getX()*zoom) + moveHorizontally, (vertex.getY()*zoom) + moveVertically,
                        (edge.getEndpoint().getX()*zoom) + moveHorizontally, (edge.getEndpoint().getY()*zoom) + moveVertically);
                if (graph.getVertices().containsValue(edge.getEndpoint())){
                    line.setStroke(color);
                }
                group.getChildren().add(line);
            }
        }
    }

    @FXML
    protected void onAddGraphJSONMenuClick() {
        FileChooser fileChooser = new FileChooser();
        File selectedFile = fileChooser.showOpenDialog(stage);
        Graph graphComponent = JSONParser.readFile(selectedFile);
        if (this.graph == null) {
            progressMessages.appendText("No initial graph. Setting added graph as initial.\n");
            this.graph = graphComponent;
            this.graphPartition = null;

        }
        else {
            progressMessages.appendText("Merging Graphs...\n");

            Graph mergedGraph = Graph.mergeGraphs(this.graph, graphComponent);
            if (mergedGraph != null) {
                progressMessages.appendText("Merging completed.\n");
                this.graph = mergedGraph;
            }
            else {
                progressMessages.appendText("Graphs could not be merged.\n");
            }
        }
        progressMessages.appendText("Visualizing merged graphs...\n");
        visualizeGraph();
    }

    @FXML
    protected void onExportToGeoJSONMenuClick() {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("ddMMyyyyHHmmss");
        LocalDateTime now = LocalDateTime.now();
        String jsonName = "graph_" + dtf.format(now);
        progressMessages.appendText("Exporting to JSON file...\n");
        JSONParser.writeJSONFile(jsonName, this.graph);
        progressMessages.appendText("Export completed.\n");

    }

    @FXML
    protected void onExportPartitionToGeoJSONMenuClick() {
        progressMessages.appendText("Exporting partitions...\n");

        for (APartitionAlgorithm algorithm : algorithms.values()) {
            progressMessages.appendText("Exporting to JSON file...\n");
            JSONParser.exportResultingPartition(algorithm, algorithm.getGraphPartition(graph), 0);
            progressMessages.appendText("Exported resulted partition of " + algorithm.getName() + "\n");
        }
    }

    @FXML
    protected void onRecalculateButtonClick() {
        Platform.runLater(() -> progressMessages.appendText("Recalculating Partitions...\n"));
        for (Map.Entry<String, APartitionAlgorithm> algorithm: algorithms.entrySet()) {
            Platform.runLater(() -> progressMessages.appendText("Partitioning Graph by " + algorithm.getKey() + "...\n"));
            algorithm.getValue().getGraphPartition(graph, spinnerPartCount.getValue());
            Platform.runLater(() -> progressMessages.appendText("Partitioning is done.\n"));

        }
        visualizeGraph();
    }

    @FXML
    protected void onExitMenuClick() {
        stage.close();
    }
}
package io.gupshup.tc.ui;

import io.gupshup.tc.WhatsAppSender;
import javafx.animation.ScaleTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.chart.PieChart;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.File;
import java.io.IOException;
import java.util.Map;

public class WhatsAppUI extends Application {

    private File csvFile;
    private File attachmentFile;
    private final WhatsAppSender sender = new WhatsAppSender();

    private final PieChart.Data successData = new PieChart.Data("Success", 0);
    private final PieChart.Data errorData = new PieChart.Data("Error", 0);
    private final PieChart.Data pendingData = new PieChart.Data("Pending", 1); // Start with 1 to show chart

    private final Label chartCenterLabel = new Label("0%");
    private final Label chartSubLabel = new Label("OVERALL");
    private final ProgressBar progressBar = new ProgressBar(0);
    private final Label statusLabel = new Label("System Ready");

    @Override
    public void start(Stage stage) {
        // --- Sidebar (Controls) ---
        VBox sidebar = new VBox(20);
        sidebar.setPadding(new Insets(25));
        sidebar.setPrefWidth(340);
        sidebar.setStyle("-fx-background-color: #0f172a; -fx-border-color: #1e293b; -fx-border-width: 0 1 0 0;");

        Label title = new Label("Gupshup Pro");
        title.setStyle("-fx-text-fill: white; -fx-font-size: 22px; -fx-font-weight: bold;");

        VBox csvBox = createField("RECIPIENTS (CSV)", "Upload CSV");
        VBox mediaBox = createField("ATTACHMENT", "Select File");

        TextArea messageInput = new TextArea();
        messageInput.setPromptText("Message Template...");
        messageInput.setPrefHeight(150);
        messageInput.setWrapText(true);
        messageInput.setStyle("-fx-control-inner-background: #020617; -fx-text-fill: white; -fx-border-color: #334155; -fx-background-radius: 8;");

        Button startBtn = new Button("EXECUTE BROADCAST");
        startBtn.setMaxWidth(Double.MAX_VALUE);
        startBtn.setPrefHeight(45);
        startBtn.setStyle("-fx-background-color: #3b82f6; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 8; -fx-cursor: hand;");

        sidebar.getChildren().addAll(title, csvBox, mediaBox, new Label("MESSAGE"), messageInput, startBtn);

        // --- Main Dashboard ---
        VBox main = new VBox(40);
        main.setPadding(new Insets(40));
        main.setAlignment(Pos.CENTER);
        main.setStyle("-fx-background-color: #020617;");
        HBox.setHgrow(main, Priority.ALWAYS);

        // Interactive Doughnut Card
        StackPane chartStack = new StackPane();
        chartStack.setPrefSize(400, 400);
        chartStack.setStyle("-fx-background-color: #0f172a; -fx-background-radius: 200; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.4), 20, 0, 0, 10);");

        PieChart pieChart = new PieChart();
        pieChart.getData().addAll(successData, errorData, pendingData);
        pieChart.setLabelsVisible(false);
        pieChart.setLegendVisible(false);
        pieChart.setStartAngle(90);

        Circle hole = new Circle(115, Color.web("#020617"));

        VBox centerText = new VBox(-5);
        centerText.setAlignment(Pos.CENTER);
        chartCenterLabel.setStyle("-fx-text-fill: white; -fx-font-size: 48px; -fx-font-weight: 800;");
        chartSubLabel.setStyle("-fx-text-fill: #64748b; -fx-font-size: 14px; -fx-font-weight: bold; -fx-letter-spacing: 1px;");
        centerText.getChildren().addAll(chartCenterLabel, chartSubLabel);

        chartStack.getChildren().addAll(pieChart, hole, centerText);

        // Sub-Stats Row
        HBox legend = new HBox(30,
                createLegendItem("Success", "#10b981"),
                createLegendItem("Error", "#ef4444"),
                createLegendItem("Pending", "#475569")
        );
        legend.setAlignment(Pos.CENTER);

        // Thick Progress Bar Section
        VBox progressSection = new VBox(15);
        progressSection.setMaxWidth(600);
        statusLabel.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 14px;");

        progressBar.setMaxWidth(Double.MAX_VALUE);
        progressBar.setPrefHeight(20); // Much thicker
        progressBar.setStyle("-fx-accent: #3b82f6; -fx-background-insets: 0; -fx-padding: 2; -fx-background-radius: 10;");

        progressSection.getChildren().addAll(statusLabel, progressBar);

        main.getChildren().addAll(chartStack, legend, progressSection);

        // --- Setup Logic ---
        setupChartInteractions(pieChart);

        ((Button)csvBox.getChildren().get(1)).setOnAction(e -> {
            csvFile = new FileChooser().showOpenDialog(stage);
            if(csvFile != null) ((Button)csvBox.getChildren().get(1)).setText("✔ " + csvFile.getName());
        });

        ((Button)mediaBox.getChildren().get(1)).setOnAction(e -> {
            attachmentFile = new FileChooser().showOpenDialog(stage);
            if(attachmentFile != null) ((Button)mediaBox.getChildren().get(1)).setText("✔ " + attachmentFile.getName());
        });

        startBtn.setOnAction(e -> startSending(messageInput.getText()));

        Scene scene = new Scene(new HBox(sidebar, main), 1200, 800);
        stage.setTitle("Gupshup Analytics Dashboard");
        stage.setScene(scene);
        stage.show();

        // Colors must be applied AFTER stage is shown
        applyColors();
    }

    private void setupChartInteractions(PieChart chart) {
        for (PieChart.Data data : chart.getData()) {
            data.getNode().setCursor(Cursor.HAND);

            data.getNode().setOnMouseEntered(e -> {
                ScaleTransition st = new ScaleTransition(Duration.millis(150), data.getNode());
                st.setToX(1.08);
                st.setToY(1.08);
                st.play();

                chartCenterLabel.setText(String.valueOf((int)data.getPieValue()));
                chartSubLabel.setText(data.getName().toUpperCase());
                chartSubLabel.setStyle("-fx-text-fill: " + getHexColor(data.getName()) + "; -fx-font-size: 14px; -fx-font-weight: bold;");
            });

            data.getNode().setOnMouseExited(e -> {
                ScaleTransition st = new ScaleTransition(Duration.millis(150), data.getNode());
                st.setToX(1.0);
                st.setToY(1.0);
                st.play();

                chartCenterLabel.setText((int)(progressBar.getProgress() * 100) + "%");
                chartSubLabel.setText("OVERALL");
                chartSubLabel.setStyle("-fx-text-fill: #64748b; -fx-font-size: 14px; -fx-font-weight: bold;");
            });
        }
    }

    private String getHexColor(String name) {
        return switch (name) {
            case "Success" -> "#10b981";
            case "Error" -> "#ef4444";
            default -> "#94a3b8";
        };
    }

    private void applyColors() {
        successData.getNode().setStyle("-fx-pie-color: #10b981; -fx-border-color: #020617; -fx-border-width: 3;");
        errorData.getNode().setStyle("-fx-pie-color: #ef4444; -fx-border-color: #020617; -fx-border-width: 3;");
        pendingData.getNode().setStyle("-fx-pie-color: #334155; -fx-border-color: #020617; -fx-border-width: 3;");
    }

    private VBox createField(String title, String btnTxt) {
        VBox v = new VBox(8);
        Label l = new Label(title);
        l.setStyle("-fx-text-fill: #64748b; -fx-font-size: 11px; -fx-font-weight: bold;");
        Button b = new Button(btnTxt);
        b.setMaxWidth(Double.MAX_VALUE);
        b.setStyle("-fx-background-color: #1e293b; -fx-text-fill: #cbd5e1; -fx-cursor: hand; -fx-background-radius: 5;");
        v.getChildren().addAll(l, b);
        return v;
    }

    private HBox createLegendItem(String name, String color) {
        Circle c = new Circle(6, Color.web(color));
        Label l = new Label(name);
        l.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 14px;");
        HBox h = new HBox(10, c, l);
        h.setAlignment(Pos.CENTER);
        return h;
    }

    private void startSending(String msg) {
        new Thread(() -> {
            try {
                sender.sendMessageToCustomers(msg, csvFile.getAbsolutePath(),
                        attachmentFile != null ? attachmentFile.getAbsolutePath() : null);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }).start();

        new Thread(() -> {
            while (true) {
                try { Thread.sleep(800); } catch (Exception ignored) {}
                Map<String, Integer> s = sender.getStats();
                int total = s.getOrDefault("total", 1);
                int done = s.getOrDefault("completed", 0);
                double p = (double) done / total;

                Platform.runLater(() -> {
                    successData.setPieValue(s.getOrDefault("success", 0));
                    errorData.setPieValue(s.getOrDefault("error", 0));
                    pendingData.setPieValue(total - done);
                    progressBar.setProgress(p);
                    chartCenterLabel.setText((int)(p * 100) + "%");
                    statusLabel.setText("Broadcasting: " + done + " / " + total);
                });
                if (done >= total) break;
            }
        }).start();
    }

    public static void main(String[] args) { launch(); }
}
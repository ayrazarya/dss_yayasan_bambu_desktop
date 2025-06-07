package javafx.com.yasbu_dekstop.controllers;

import javafx.com.yasbu_dekstop.models.UserJson;
import javafx.com.yasbu_dekstop.models.ProductJson;
import javafx.com.yasbu_dekstop.models.VikorJson;
import javafx.com.yasbu_dekstop.services.ProductService;
import javafx.com.yasbu_dekstop.services.VikorService;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.Button;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.Alert;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.layout.StackPane;
import javafx.event.ActionEvent;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Pos;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Comparator;
import java.util.stream.Collectors;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.Logger;
import java.util.logging.Level;

public class HomeController implements Initializable {

    private static final Logger LOGGER = Logger.getLogger(HomeController.class.getName());
    private static final int MAX_CHART_ITEMS = 5;
    private static final String DEFAULT_GUEST_NAME = "Guest User";
    private static final String DEFAULT_GUEST_INITIALS = "GU";

    // Header elements
    @FXML private Label welcomeLabel;
    @FXML private Label userInitials;
    @FXML private Label mainWelcomeLabel;

    // Navigation buttons
    @FXML private Button dashboardButton;
    @FXML private Button profileButton;
    @FXML private Button settingsButton;

    // Action buttons
    @FXML private Button newAnalysisButton;
    @FXML private Button loadRankingButton;

    // Dashboard card elements
    @FXML private Label productTitle;
    @FXML private Label productId;
    @FXML private Label qValueLabel;
    @FXML private Label sValueLabel;
    @FXML private Label rValueLabel;

    // Loading elements
    @FXML private ProgressIndicator loadingIndicator;
    @FXML private Label loadingLabel;

    // Status elements
    @FXML private Label statusLabel;
    @FXML private Label lastUpdatedLabel;

    // Chart container for dynamic product list
    @FXML private VBox chartData;

    // Detail Table Elements (NEW)
    @FXML private HBox vikorDetailHeader;
    @FXML private VBox vikorDetailTable;

    private UserJson user;
    private ProductService productService;
    private VikorService vikorService;
    private List<ProductJson> products;
    private List<VikorJson> vikorRankings;
    private Map<Integer, ProductJson> productMap;
    private volatile boolean isLoading = false;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        LOGGER.info("HomeController initialized");
        productService = new ProductService();
        vikorService = new VikorService();
        productMap = new HashMap<>();

        setDefaultValues();
        setupButtonHandlers();
        setupDetailTableHeader(); // Call new method to setup table header
        loadDataAsync();
    }

    private void setDefaultValues() {
        updateLabel(welcomeLabel, DEFAULT_GUEST_NAME);
        updateLabel(mainWelcomeLabel, "Halo, Guest!");
        updateLabel(userInitials, DEFAULT_GUEST_INITIALS);
        updateLabel(productTitle, "Memuat produk...");
        updateLabel(productId, "ID Produk: -");
        updateLabel(statusLabel, "Memuat data...");
    }

    private void updateLabel(Label label, String text) {
        if (label != null) {
            label.setText(text);
        }
    }

    private void loadDataAsync() {
        if (isLoading) {
            LOGGER.warning("Data loading already in progress");
            return;
        }

        showLoading(true);
        isLoading = true;
        Task<Void> loadDataTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                try {
                    updateMessage("Memuat data produk...");
                    products = productService.getAllProducts();

                    if (products != null && !products.isEmpty()) {
                        productMap.clear();
                        for (ProductJson product : products) {
                            if (product != null && product.getProductId() != 0) {
                                productMap.put(product.getProductId(), product);
                            }
                        }
                        updateMessage("Menghitung nilai VIKOR...");
                        vikorRankings = vikorService.calculateVikor(products);
                        LOGGER.info("Successfully loaded " + products.size() + " products and calculated VIKOR rankings");
                    } else {
                        throw new Exception("Tidak ada data produk yang ditemukan");
                    }
                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE, "Error loading data", e);
                    throw e;
                }
                return null;
            }

            @Override
            protected void succeeded() {
                Platform.runLater(() -> {
                    isLoading = false;
                    showLoading(false);
                    updateDashboardWithVikorData();
                    updateChartWithVikorData();
                    updateDetailTableWithVikorData(); // UPDATE
                    updateStatus("Siap", LocalDateTime.now());
                    LOGGER.info("Data loading completed successfully");
                });
            }

            @Override
            protected void failed() {
                Platform.runLater(() -> {
                    isLoading = false;
                    showLoading(false);
                    String errorMessage = getException() != null ? getException().getMessage() : "Unknown error";
                    showError("Gagal memuat data: " + errorMessage);
                    setFallbackData();
                    updateStatus("Error", LocalDateTime.now());
                    LOGGER.log(Level.SEVERE, "Data loading failed", getException());
                });
            }

            @Override
            protected void updateMessage(String message) {
                Platform.runLater(() -> {
                    updateLabel(loadingLabel, message);
                    updateLabel(statusLabel, message);
                });
            }
        };

        Thread loadThread = new Thread(loadDataTask);
        loadThread.setDaemon(true);
        loadThread.start();
    }

    private void loadExistingRankings() {
        if (isLoading) {
            showInfo("Operasi sedang berjalan, mohon tunggu...");
            return;
        }

        showLoading(true);
        isLoading = true;
        Task<List<VikorJson>> loadRankingsTask = new Task<>() {
            @Override
            protected List<VikorJson> call() throws Exception {
                updateMessage("Memuat peringkat tersimpan...");
                if (products != null && !products.isEmpty()) {
                    return vikorService.calculateVikor(products);
                } else {
                    return vikorService.getVikorRankings();
                }
            }

            @Override
            protected void succeeded() {
                vikorRankings = getValue();
                Platform.runLater(() -> {
                    isLoading = false;
                    showLoading(false);
                    updateDashboardWithVikorData();
                    // INI YANG DIPERBAIKI: 'Vkor' menjadi 'Vikor'
                    updateChartWithVikorData();
                    updateDetailTableWithVikorData();
                    updateStatus("Data dimuat", LocalDateTime.now());
                    LOGGER.info("Rankings loaded successfully");
                });
            }

            @Override
            protected void failed() {
                Platform.runLater(() -> {
                    isLoading = false;
                    showLoading(false);
                    String errorMessage = getException() != null ? getException().getMessage() : "Unknown error";
                    showError("Gagal memuat peringkat: " + errorMessage);
                    updateStatus("Error", LocalDateTime.now());
                });
            }

            @Override
            protected void updateMessage(String message) {
                Platform.runLater(() -> {
                    updateLabel(loadingLabel, message);
                    updateLabel(statusLabel, message);
                });
            }
        };

        Thread loadThread = new Thread(loadRankingsTask);
        loadThread.setDaemon(true);
        loadThread.start();
    }

    // GANTI metode ini di HomeController.java
    private void setupDetailTableHeader() {
        if (vikorDetailHeader == null) return;
        vikorDetailHeader.getChildren().clear();

        // Kolom Peringkat
        Label rankHeader = new Label("PERINGKAT");
        rankHeader.getStyleClass().add("detail-header-label");
        VBox rankContainer = new VBox(rankHeader);
        rankContainer.setPrefWidth(90); // Lebar kolom peringkat
        rankContainer.setAlignment(Pos.CENTER_LEFT);

        // Kolom Nama Produk (Fleksibel)
        Label nameHeader = new Label("NAMA PRODUK");
        nameHeader.getStyleClass().add("detail-header-label");
        HBox.setHgrow(nameHeader, javafx.scene.layout.Priority.ALWAYS); // Biarkan tetap fleksibel

        // -- Perbaikan Dimulai Di Sini --

        // Kolom Q Value
        Label qHeader = new Label("Q VALUE");
        qHeader.getStyleClass().add("detail-header-label");
        VBox qContainer = new VBox(qHeader);
        qContainer.setPrefWidth(110); // Lebar kolom Q
        qContainer.setAlignment(Pos.CENTER_RIGHT);

        // Kolom S Value
        Label sHeader = new Label("S VALUE");
        sHeader.getStyleClass().add("detail-header-label");
        VBox sContainer = new VBox(sHeader);
        sContainer.setPrefWidth(110); // Lebar kolom S
        sContainer.setAlignment(Pos.CENTER_RIGHT);

        // Kolom R Value
        Label rHeader = new Label("R VALUE");
        rHeader.getStyleClass().add("detail-header-label");
        VBox rContainer = new VBox(rHeader);
        rContainer.setPrefWidth(110); // Lebar kolom R
        rContainer.setAlignment(Pos.CENTER_RIGHT);

        // Kolom ID Produk
        Label idHeader = new Label("ID PRODUK");
        idHeader.getStyleClass().add("detail-header-label");
        VBox idContainer = new VBox(idHeader);
        idContainer.setPrefWidth(90); // Lebar kolom ID
        idContainer.setAlignment(Pos.CENTER_RIGHT);

        vikorDetailHeader.getChildren().addAll(rankContainer, nameHeader, qContainer, sContainer, rContainer, idContainer);
    }

    private void updateDetailTableWithVikorData() {
        if (vikorDetailTable == null) return;
        vikorDetailTable.getChildren().clear();

        if (vikorRankings == null || vikorRankings.isEmpty()) {
            Label noDataLabel = new Label("Tidak ada data peringkat detail tersedia.");
            noDataLabel.getStyleClass().add("no-data-text");
            vikorDetailTable.getChildren().add(noDataLabel);
            return;
        }

        List<VikorJson> sortedRankings = vikorRankings.stream()
                .filter(v -> v != null)
                .sorted(Comparator.comparingInt(VikorJson::getRank))
                .collect(Collectors.toList());

        for (VikorJson vikorResult : sortedRankings) {
            HBox row = createDetailTableRow(vikorResult);
            if (row != null) {
                vikorDetailTable.getChildren().add(row);
            }
        }
        LOGGER.info("Detail table updated with " + sortedRankings.size() + " items");
    }

    /**
     * Creates a single HBox row for the detail table.
     * @param vikorResult The VIKOR data for the row.
     * @return A styled HBox representing the table row.
     */
    private HBox createDetailTableRow(VikorJson vikorResult) {
        try {
            HBox row = new HBox();
            row.getStyleClass().add("detail-table-row");
            row.setAlignment(Pos.CENTER_LEFT);
            row.setSpacing(15);

            if (vikorResult.getRank() == 1) {
                row.getStyleClass().add("top-rank-row");
            }

            // Kolom Peringkat
            Label rankLabel = new Label(String.valueOf(vikorResult.getRank()));
            rankLabel.getStyleClass().add("rank-cell-text");
            StackPane rankCircle = new StackPane(rankLabel);
            rankCircle.getStyleClass().add("rank-cell");
            VBox rankContainer = new VBox(rankCircle);
            rankContainer.setPrefWidth(90);
            rankContainer.setAlignment(Pos.CENTER_LEFT);

            // Kolom Nama Produk (Fleksibel)
            ProductJson product = productMap.get(vikorResult.getProductId());
            String productName = getProductDisplayName(product, vikorResult);
            Label nameLabel = new Label(productName);
            nameLabel.getStyleClass().add("detail-cell");
            HBox.setHgrow(nameLabel, javafx.scene.layout.Priority.ALWAYS);

            // -- Perbaikan Dimulai Di Sini --

            // Kolom Q Value
            Label qLabel = new Label(vikorResult.getFormattedQ());
            qLabel.getStyleClass().addAll("detail-cell", "numeric-cell");
            VBox qContainer = new VBox(qLabel);
            qContainer.setPrefWidth(110);
            qContainer.setAlignment(Pos.CENTER_RIGHT);

            // Kolom S Value
            Label sLabel = new Label(vikorResult.getFormattedS());
            sLabel.getStyleClass().addAll("detail-cell", "numeric-cell");
            VBox sContainer = new VBox(sLabel);
            sContainer.setPrefWidth(110);
            sContainer.setAlignment(Pos.CENTER_RIGHT);

            // Kolom R Value
            Label rLabel = new Label(vikorResult.getFormattedR());
            rLabel.getStyleClass().addAll("detail-cell", "numeric-cell");
            VBox rContainer = new VBox(rLabel);
            rContainer.setPrefWidth(110);
            rContainer.setAlignment(Pos.CENTER_RIGHT);

            // Kolom ID Produk
            Label idLabel = new Label(String.valueOf(vikorResult.getProductId()));
            idLabel.getStyleClass().addAll("detail-cell", "numeric-cell");
            VBox idContainer = new VBox(idLabel);
            idContainer.setPrefWidth(90);
            idContainer.setAlignment(Pos.CENTER_RIGHT);

            row.getChildren().addAll(rankContainer, nameLabel, qContainer, sContainer, rContainer, idContainer);
            return row;

        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error creating detail table row for product " + vikorResult.getProductId(), e);
            return null;
        }
    }



    private void runNewAnalysis() {
        if (products == null || products.isEmpty()) {
            showError("Tidak ada data produk untuk dianalisis");
            return;
        }

        if (isLoading) {
            showInfo("Analisis sedang berjalan, mohon tunggu...");
            return;
        }

        showLoading(true);
        isLoading = true;

        Task<Void> analysisTask = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                updateMessage("Menjalankan analisis VIKOR baru...");
                // Only calculate VIKOR, don't call getVikorRankings() again
                vikorRankings = vikorService.calculateVikor(products);
                LOGGER.info("New VIKOR analysis completed");
                return null;
            }

            @Override
            protected void succeeded() {
                Platform.runLater(() -> {
                    isLoading = false;
                    showLoading(false);
                    updateDashboardWithVikorData();
                    updateChartWithVikorData();
                    updateStatus("Analisis selesai", LocalDateTime.now());
                    showInfo("Analisis VIKOR berhasil dijalankan!");
                });
            }

            @Override
            protected void failed() {
                Platform.runLater(() -> {
                    isLoading = false;
                    showLoading(false);
                    String errorMessage = getException() != null ? getException().getMessage() : "Unknown error";
                    showError("Gagal menjalankan analisis: " + errorMessage);
                    updateStatus("Error", LocalDateTime.now());
                });
            }

            @Override
            protected void updateMessage(String message) {
                Platform.runLater(() -> {
                    updateLabel(loadingLabel, message);
                    updateLabel(statusLabel, message);
                });
            }
        };

        Thread analysisThread = new Thread(analysisTask);
        analysisThread.setDaemon(true);
        analysisThread.start();
    }


    private void showLoading(boolean show) {
        if (loadingIndicator != null) {
            loadingIndicator.setVisible(show);
        }
        if (loadingLabel != null) {
            loadingLabel.setVisible(show);
        }
    }

    private void updateDashboardWithVikorData() {
        if (vikorRankings == null || vikorRankings.isEmpty()) {
            setFallbackData();
            return;
        }

        try {
            // Get the best product (rank 1)
            VikorJson bestVikorResult = vikorRankings.stream()
                    .filter(v -> v != null)
                    .min(Comparator.comparingInt(VikorJson::getRank))
                    .orElse(vikorRankings.get(0));

            if (bestVikorResult == null) {
                setFallbackData();
                return;
            }

            // Try to get product details
            ProductJson bestProduct = null;
            if (bestVikorResult.getProductId() != 0) {
                bestProduct = productMap.get(bestVikorResult.getProductId());
            }

            // Update product information
            if (bestProduct != null) {
                updateLabel(productTitle, bestProduct.getName());
                updateLabel(productId, "ID Produk: " + bestProduct.getProductId());
            } else {
                String productName = bestVikorResult.getName() != null ?
                        bestVikorResult.getName() : "Produk Terbaik";
                updateLabel(productTitle, productName);
                updateLabel(productId, "ID Produk: " + bestVikorResult.getProductId());
            }

            // Update VIKOR values with null safety
            updateLabel(qValueLabel, bestVikorResult.getFormattedQ());
            updateLabel(sValueLabel, bestVikorResult.getFormattedS());
            updateLabel(rValueLabel, bestVikorResult.getFormattedR());

            LOGGER.info("Dashboard updated with best product: " + bestVikorResult.getProductId());

        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error updating dashboard", e);
            setFallbackData();
        }
    }

    private void updateChartWithVikorData() {
        if (chartData == null || vikorRankings == null || vikorRankings.isEmpty()) {
            return;
        }

        try {
            // Clear existing chart data
            chartData.getChildren().clear();

            // Filter and sort valid rankings
            List<VikorJson> validRankings = vikorRankings.stream()
                    .filter(v -> v != null)
                    .sorted(Comparator.comparingInt(VikorJson::getRank))
                    .limit(MAX_CHART_ITEMS)
                    .toList();

            // Add products to the chart
            for (VikorJson vikorResult : validRankings) {
                HBox chartItem = createChartItem(vikorResult);
                if (chartItem != null) {
                    chartData.getChildren().add(chartItem);
                }
            }

            LOGGER.info("Chart updated with " + validRankings.size() + " items");

        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error updating chart", e);
            setNoDataMessage();
        }
    }

    private HBox createChartItem(VikorJson vikorResult) {
        try {
            ProductJson product = productMap.get(vikorResult.getProductId());

            // Create chart item container
            HBox chartItem = new HBox();
            chartItem.getStyleClass().add("chart-item");
            chartItem.setSpacing(10);

            // Product information
            VBox productInfo = new VBox();
            productInfo.setSpacing(2);

            String productName = getProductDisplayName(product, vikorResult);
            Label productLabel = new Label( + vikorResult.getRank() + " - " + productName);
            productLabel.getStyleClass().add("chart-label");

            Label scoreLabel = new Label("Q-Value: " + vikorResult.getFormattedQ());
            scoreLabel.getStyleClass().add("chart-score");

            productInfo.getChildren().addAll(productLabel, scoreLabel);

            // Progress bar
            double qValue = vikorResult.getQ(); // Assume getQ() returns primitive double
            double normalizedScore = Math.max(0, Math.min(1.0, 1.0 - qValue));

            ProgressBar progressBar = new ProgressBar(normalizedScore);
            progressBar.setPrefWidth(200);
            progressBar.getStyleClass().addAll("chart-bar", getBarStyleClass(qValue));

            // Spacer
            javafx.scene.layout.Region spacer = new javafx.scene.layout.Region();
            HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

            chartItem.getChildren().addAll(productInfo, spacer, progressBar);
            return chartItem;

        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error creating chart item for product " + vikorResult.getProductId(), e);
            return null;
        }
    }

    private String getProductDisplayName(ProductJson product, VikorJson vikorResult) {
        if (product != null && product.getName() != null && !product.getName().trim().isEmpty()) {
            return product.getName();
        }
        if (vikorResult.getName() != null && !vikorResult.getName().trim().isEmpty()) {
            return vikorResult.getName();
        }
        return "Produk " + vikorResult.getProductId();
    }

    private String getBarStyleClass(double qValue) {
        // Lower Q-value is better in VIKOR
        if (qValue <= 0.2) return "bar-excellent";
        else if (qValue <= 0.4) return "bar-good";
        else if (qValue <= 0.6) return "bar-average";
        else if (qValue <= 0.8) return "bar-below-average";
        else return "bar-poor";
    }

    private void updateStatus(String status, LocalDateTime time) {
        updateLabel(statusLabel, status);
        if (lastUpdatedLabel != null) {
            try {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
                lastUpdatedLabel.setText("Terakhir diperbarui: " + time.format(formatter));
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error formatting date", e);
                lastUpdatedLabel.setText("Terakhir diperbarui: " + time.toString());
            }
        }
    }

    private void setFallbackData() {
        updateLabel(productTitle, "Data tidak tersedia");
        updateLabel(productId, "ID Produk: -");
        updateLabel(qValueLabel, "N/A");
        updateLabel(sValueLabel, "N/A");
        updateLabel(rValueLabel, "N/A");
        setNoDataMessage();

        // UPDATE: Also clear the detail table on fallback
        if (vikorDetailTable != null) {
            vikorDetailTable.getChildren().clear();
            Label noDataLabel = new Label("Tidak ada data peringkat detail tersedia.");
            noDataLabel.getStyleClass().add("no-data-text");
            vikorDetailTable.getChildren().add(noDataLabel);
        }
    }

    private void setNoDataMessage() {
        if (chartData != null) {
            chartData.getChildren().clear();
            Label noDataLabel = new Label("Tidak ada data peringkat tersedia");
            noDataLabel.getStyleClass().add("no-data-text");
            chartData.getChildren().add(noDataLabel);
        }
    }

    private void setupButtonHandlers() {
        // Navigation button handlers
        if (dashboardButton != null) {
            dashboardButton.setOnAction(this::handleDashboardClick);
        }
        if (profileButton != null) {
            profileButton.setOnAction(this::handleProfileClick);
        }
        if (settingsButton != null) {
            settingsButton.setOnAction(this::handleSettingsClick);
        }

        // Action button handlers
        if (newAnalysisButton != null) {
            newAnalysisButton.setOnAction(this::handleNewAnalysisClick);
        }
        if (loadRankingButton != null) {
            loadRankingButton.setOnAction(this::handleLoadRankingClick);
        }
    }

    public void setUserData(UserJson user) {
        this.user = user;
        LOGGER.info("Setting user data: " + (user != null ? user.getFullName() : "null"));
        updateUI();
    }

    private void updateUI() {
        if (user != null) {
            updateLabel(welcomeLabel, user.getFullName());
            updateLabel(mainWelcomeLabel, "Halo, " + user.getFullName() + "!");

            if (userInitials != null) {
                String initials = getUserInitials(user.getFullName());
                userInitials.setText(initials);
            }

            LOGGER.info("UI updated for user: " + user.getFullName());
        } else {
            LOGGER.warning("Cannot update UI - user is null");
            setDefaultValues();
        }
    }

    private String getUserInitials(String fullName) {
        if (fullName == null || fullName.trim().isEmpty()) {
            return "U";
        }

        try {
            String[] nameParts = fullName.trim().split("\\s+");
            StringBuilder initials = new StringBuilder();

            for (int i = 0; i < Math.min(2, nameParts.length); i++) {
                if (!nameParts[i].isEmpty()) {
                    initials.append(Character.toUpperCase(nameParts[i].charAt(0)));
                }
            }

            return initials.length() > 0 ? initials.toString() : "U";
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error generating initials for: " + fullName, e);
            return "U";
        }
    }

    // Navigation handlers
    @FXML
    private void handleDashboardClick(ActionEvent event) {
        LOGGER.info("Dashboard clicked - refreshing data");
        loadDataAsync();
    }

    @FXML
    private void handleProfileClick(ActionEvent event) {
        LOGGER.info("Profile clicked");
        // Navigate to profile page
    }

    @FXML
    private void handleSettingsClick(ActionEvent event) {
        LOGGER.info("Settings clicked");
        // Navigate to settings page
    }

    // Action handlers
    @FXML
    private void handleNewAnalysisClick(ActionEvent event) {
        LOGGER.info("New Analysis clicked");
        runNewAnalysis();
    }

    @FXML
    private void handleLoadRankingClick(ActionEvent event) {
        LOGGER.info("Load Ranking clicked");
        loadExistingRankings();
    }

    // Public methods
    public void refreshData() {
        loadDataAsync();
    }

    // Utility methods
    private void showError(String message) {
        LOGGER.severe("Error: " + message);
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("Terjadi Kesalahan");
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    private void showInfo(String message) {
        LOGGER.info("Info: " + message);
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Informasi");
            alert.setHeaderText("Sukses");
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    // Getters
    public Label getWelcomeLabel() { return welcomeLabel; }
    public Label getUserInitials() { return userInitials; }
    public Label getMainWelcomeLabel() { return mainWelcomeLabel; }
    public List<ProductJson> getProducts() { return products; }
    public List<VikorJson> getVikorRankings() { return vikorRankings; }

    public void cleanup() {
        LOGGER.info("HomeController cleanup");
        isLoading = false;
    }
}
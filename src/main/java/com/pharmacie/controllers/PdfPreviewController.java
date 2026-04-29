package com.pharmacie.controllers;

import com.pharmacie.utils.AlertUtils;
import com.pharmacie.utils.ToastService;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.print.PrinterJob;
import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import org.apache.pdfbox.printing.PDFPageable;

public class PdfPreviewController {

    private static final Logger logger = LoggerFactory.getLogger(PdfPreviewController.class);

    @FXML private ImageView pdfImageView;
    @FXML private ProgressIndicator progressLoading;
    @FXML private Label lblLoading;
    @FXML private Label lblFileName;
    @FXML private Label lblFileSize;

    private File pdfFile;
    private Stage dialogStage;

    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }

    public void loadPdf(File file, String documentName) {
        this.pdfFile = file;
        lblFileName.setText(documentName);
        
        long sizeKb = file.length() / 1024;
        lblFileSize.setText("Taille : " + sizeKb + " KB");

        // Utilisation d'un Task en arrière-plan pour ne pas bloquer l'UI (le rendu PDFBox est lourd)
        Task<Image> renderTask = new Task<Image>() {
            @Override
            protected Image call() throws Exception {
                // Utilisation de Loader (PDFBox 3.0.x)
                try (PDDocument document = Loader.loadPDF(file)) {
                    PDFRenderer renderer = new PDFRenderer(document);
                    // Rendu de la première page avec 150 DPI pour la qualité visuelle (300DPI peut être trop lourd)
                    BufferedImage awtImage = renderer.renderImageWithDPI(0, 150);
                    return SwingFXUtils.toFXImage(awtImage, null);
                }
            }
        };

        renderTask.setOnSucceeded(e -> {
            pdfImageView.setImage(renderTask.getValue());
            // Ajustement de la largeur pour prendre l'espace
            pdfImageView.fitWidthProperty().bind(
                javafx.beans.binding.Bindings.createDoubleBinding(
                    () -> pdfImageView.getParent().getLayoutBounds().getWidth() - 60,
                    pdfImageView.getParent().layoutBoundsProperty()
                )
            );
            
            progressLoading.setVisible(false);
            progressLoading.setManaged(false);
            lblLoading.setVisible(false);
            lblLoading.setManaged(false);
            
            pdfImageView.setVisible(true);
        });

        renderTask.setOnFailed(e -> {
            logger.error("Erreur lors du rendu du PDF", renderTask.getException());
            lblLoading.setText("❌ Erreur de rendu de l'aperçu");
            lblLoading.setStyle("-fx-text-fill: #E11D48;");
            progressLoading.setVisible(false);
        });

        Thread thread = new Thread(renderTask);
        thread.setDaemon(true);
        thread.start();
    }

    @FXML
    public void downloadPdf() {
        if (pdfFile == null || !pdfFile.exists()) return;

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Enregistrer le Document PDF");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Document PDF", "*.pdf"));
        fileChooser.setInitialFileName(lblFileName.getText());

        File destFile = fileChooser.showSaveDialog(dialogStage);
        if (destFile != null) {
            try {
                Files.copy(pdfFile.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                ToastService.showSuccess(dialogStage, "Document enregistré", "Le PDF a été sauvegardé avec succès.");
                
                // Ouvrir optionnellement le fichier téléchargé
                if (java.awt.Desktop.isDesktopSupported()) {
                    java.awt.Desktop.getDesktop().open(destFile);
                }
            } catch (IOException e) {
                logger.error("Erreur lors de la copie du PDF", e);
                AlertUtils.showPremiumAlert(javafx.scene.control.Alert.AlertType.ERROR, "Erreur", "Sauvegarde impossible", e.getMessage());
            }
        }
    }

    @FXML
    public void printPdf() {
        if (pdfFile == null || !pdfFile.exists()) return;

        Task<Void> printTask = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                try (PDDocument document = Loader.loadPDF(pdfFile)) {
                    PrinterJob job = PrinterJob.getPrinterJob();
                    job.setPageable(new PDFPageable(document));

                    // Impression silencieuse vers l'imprimante par défaut
                    Platform.runLater(() -> ToastService.showSuccess(dialogStage, "Impression en cours", "Le document a été envoyé à l'imprimante par défaut."));
                    job.print();
                }
                return null;
            }
        };

        printTask.setOnFailed(e -> {
            logger.error("Erreur d'impression", printTask.getException());
            AlertUtils.showPremiumAlert(javafx.scene.control.Alert.AlertType.ERROR, "Erreur d'impression", "L'impression a échoué", printTask.getException().getMessage());
        });

        Thread thread = new Thread(printTask);
        thread.setDaemon(true);
        thread.start();
    }

    @FXML
    public void closeWindow() {
        if (dialogStage != null) {
            dialogStage.close();
        }
    }
}

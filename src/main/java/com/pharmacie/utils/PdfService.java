package com.pharmacie.utils;

import com.pharmacie.models.Achat;
import com.pharmacie.models.LigneAchat;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PdfService {

    private static final Logger logger = LoggerFactory.getLogger(PdfService.class);
    private static final float MARGIN = 50;
    private static final float PAGE_WIDTH = PDRectangle.A4.getWidth();
    private static final float PAGE_HEIGHT = PDRectangle.A4.getHeight();

    private static float drawProfessionalHeader(PDDocument document, PDPageContentStream cs, String documentTitle, String subtitle, float startY) throws IOException {
        com.pharmacie.models.PharmacieInfo info = new com.pharmacie.dao.PharmacieInfoDAO().getInfo();
        String nomPharma = (info != null && info.getNom() != null && !info.getNom().isEmpty()) ? info.getNom() : "PHARMACIE VETERINAIRE";
        String adrPharma = (info != null && info.getAdresse() != null) ? info.getAdresse() : "Adresse non définie";
        String telPharma = (info != null && info.getTelephone() != null) ? info.getTelephone() : "Tel non défini";

        org.apache.pdfbox.pdmodel.font.PDFont fontBold;
        org.apache.pdfbox.pdmodel.font.PDFont fontNormal;
        
        try {
            File reg = new File("src/main/resources/fonts/Inter-Regular.ttf");
            File bold = new File("src/main/resources/fonts/Inter-Bold.ttf");
            if (reg.exists() && bold.exists()) {
                fontNormal = org.apache.pdfbox.pdmodel.font.PDType0Font.load(document, reg);
                fontBold = org.apache.pdfbox.pdmodel.font.PDType0Font.load(document, bold);
            } else {
                fontBold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
                fontNormal = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            }
        } catch (Exception e) {
            fontBold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            fontNormal = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
        }
        
        float y = startY;

        // Logo
        float currentX = MARGIN;
        try {
            File logoFile = new File("src/main/resources/images/logo1.jpeg");
            if (logoFile.exists()) {
                org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject logo = org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject.createFromFile(logoFile.getAbsolutePath(), document);
                float scale = 50f / logo.getHeight();
                float width = logo.getWidth() * scale;
                cs.drawImage(logo, currentX, y - 50, width, 50);
                currentX += width + 15;
            }
        } catch (Exception e) {
            logger.warn("Impossible de charger le logo", e);
        }

        // Info Pharmacie
        cs.setNonStrokingColor(Color.decode("#1E293B"));
        if (nomPharma.length() > 25) {
            cs.setFont(fontBold, 12);
        } else {
            cs.setFont(fontBold, 14);
        }
        drawText(cs, nomPharma.toUpperCase(), currentX, y - 15);
        cs.setFont(fontNormal, 9);
        cs.setNonStrokingColor(Color.decode("#64748B"));
        drawText(cs, adrPharma, currentX, y - 30);
        drawText(cs, "Tel: " + telPharma, currentX, y - 45);

        y -= 90; // Espace après logo et infos

        // Titre Centré
        cs.setFont(fontBold, 18);
        cs.setNonStrokingColor(Color.decode("#059669")); // Emerald 600
        String titre = documentTitle.toUpperCase();
        float titreWidth = fontBold.getStringWidth(titre) / 1000 * 18;
        float titreX = (PAGE_WIDTH - titreWidth) / 2;
        drawText(cs, titre, titreX, y);
        y -= 20;
        
        if (subtitle != null && !subtitle.isEmpty()) {
            cs.setFont(fontNormal, 10);
            cs.setNonStrokingColor(Color.decode("#475569"));
            float subWidth = fontNormal.getStringWidth(subtitle) / 1000 * 10;
            float subX = (PAGE_WIDTH - subWidth) / 2;
            drawText(cs, subtitle, subX, y);
            y -= 20;
        }

        y -= 10;

        // Ligne séparatrice fine
        cs.setLineWidth(1f);
        cs.setStrokingColor(Color.decode("#E2E8F0"));
        cs.moveTo(MARGIN, y);
        cs.lineTo(PAGE_WIDTH - MARGIN, y);
        cs.stroke();
        
        return y - 30; // Retourne le nouveau Y
    }

    public static void genererBonDeCommande(Achat achat, Stage ownerStage) {
        String nomFichier = "BonCommande_" + (achat.getReferenceFacture() != null ? achat.getReferenceFacture() : achat.getId()) + ".pdf";
        File fichier = new File(System.getProperty("java.io.tmpdir"), nomFichier);

        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            org.apache.pdfbox.pdmodel.font.PDFont fontBold;
            org.apache.pdfbox.pdmodel.font.PDFont fontNormal;
            org.apache.pdfbox.pdmodel.font.PDFont fontOblique;
            
            try {
                File reg = new File("src/main/resources/fonts/Inter-Regular.ttf");
                File bold = new File("src/main/resources/fonts/Inter-Bold.ttf");
                if (reg.exists() && bold.exists()) {
                    fontNormal = org.apache.pdfbox.pdmodel.font.PDType0Font.load(document, reg);
                    fontBold = org.apache.pdfbox.pdmodel.font.PDType0Font.load(document, bold);
                    fontOblique = fontNormal; // Inter n'a pas d'oblique natif facile à charger, on utilise normal pour TVA
                } else {
                    fontBold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
                    fontNormal = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
                    fontOblique = new PDType1Font(Standard14Fonts.FontName.HELVETICA_OBLIQUE);
                }
            } catch (Exception e) {
                fontBold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
                fontNormal = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
                fontOblique = new PDType1Font(Standard14Fonts.FontName.HELVETICA_OBLIQUE);
            }

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

            try (PDPageContentStream cs = new PDPageContentStream(document, page)) {
                float y = PAGE_HEIGHT - MARGIN;

                // --- HEADER PREMIUM ---
                com.pharmacie.models.PharmacieInfo info = new com.pharmacie.dao.PharmacieInfoDAO().getInfo();
                String nomPharma = (info != null && info.getNom() != null && !info.getNom().isEmpty()) ? info.getNom() : "PHARMACIE VETERINAIRE";
                String adrPharma = (info != null && info.getAdresse() != null) ? info.getAdresse() : "Adresse non définie";
                String telPharma = (info != null && info.getTelephone() != null) ? info.getTelephone() : "Tel non défini";

                // Logo
                float currentX = MARGIN;
                try {
                    File logoFile = new File("src/main/resources/images/logo1.jpeg");
                    if (logoFile.exists()) {
                        org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject logo = org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject.createFromFile(logoFile.getAbsolutePath(), document);
                        // Scale logo to fit height of 50
                        float scale = 50f / logo.getHeight();
                        float width = logo.getWidth() * scale;
                        cs.drawImage(logo, currentX, y - 50, width, 50);
                        currentX += width + 15;
                    }
                } catch (Exception e) {
                    logger.warn("Impossible de charger le logo", e);
                }

                // Info Pharmacie
                cs.setNonStrokingColor(Color.decode("#1E293B"));
                if (nomPharma.length() > 25) {
                    cs.setFont(fontBold, 12); // Réduction si trop long
                } else {
                    cs.setFont(fontBold, 14);
                }
                drawText(cs, nomPharma.toUpperCase(), currentX, y - 15);
                cs.setFont(fontNormal, 9); // Taille 9 pour éviter les dépassements
                cs.setNonStrokingColor(Color.decode("#64748B"));
                drawText(cs, adrPharma, currentX, y - 30);
                drawText(cs, "Tel: " + telPharma, currentX, y - 45);

                // Titre BON DE COMMANDE à droite
                cs.setFont(fontBold, 20);
                cs.setNonStrokingColor(Color.decode("#059669")); // Emerald 600
                String titre = "BON DE COMMANDE";
                // On s'assure qu'il est aligné par rapport à la bordure droite
                float titreWidth = fontBold.getStringWidth(titre) / 1000 * 20;
                float titreX = PAGE_WIDTH - MARGIN - titreWidth;
                drawText(cs, titre, titreX, y - 15);
                
                cs.setFont(fontBold, 9);
                cs.setNonStrokingColor(Color.decode("#334155"));
                String ref = achat.getReferenceFacture() != null ? achat.getReferenceFacture() : String.valueOf(achat.getId());
                drawText(cs, "RÉFÉRENCE : " + ref, titreX, y - 35);
                String date = achat.getDateAchat() != null ? achat.getDateAchat().toLocalDate().format(formatter) : "---";
                drawText(cs, "DATE : " + date, titreX, y - 50);

                y -= 90;

                // Ligne séparatrice fine
                cs.setLineWidth(1f);
                cs.setStrokingColor(Color.decode("#E2E8F0"));
                cs.moveTo(MARGIN, y);
                cs.lineTo(PAGE_WIDTH - MARGIN, y);
                cs.stroke();
                
                y -= 30;

                // --- BLOC FOURNISSEUR ---
                drawFilledRect(cs, MARGIN, y - 40, 250, 50, Color.decode("#F8FAFC"));
                cs.setLineWidth(1f);
                cs.setStrokingColor(Color.decode("#CBD5E1"));
                cs.addRect(MARGIN, y - 40, 250, 50);
                cs.stroke();

                cs.setFont(fontBold, 10);
                cs.setNonStrokingColor(Color.decode("#64748B"));
                drawText(cs, "DESTINATAIRE (FOURNISSEUR)", MARGIN + 10, y);
                
                cs.setFont(fontBold, 12);
                cs.setNonStrokingColor(Color.decode("#1E293B"));
                drawText(cs, achat.getFournisseur().getNom(), MARGIN + 10, y - 18);
                
                cs.setFont(fontNormal, 10);
                String contact = achat.getFournisseur().getTelephone() != null ? achat.getFournisseur().getTelephone() : "Non renseigné";
                drawText(cs, "Contact: " + contact, MARGIN + 10, y - 32);

                y -= 70;

                // --- TABLEAU PREMIUM ---
                // Header du tableau
                drawFilledRect(cs, MARGIN, y - 5, PAGE_WIDTH - 2 * MARGIN, 22, Color.decode("#059669"));
                cs.setFont(fontBold, 9);
                cs.setNonStrokingColor(Color.WHITE);
                
                drawText(cs, "DÉSIGNATION PRODUIT", MARGIN + 10, y);
                drawText(cs, "N° LOT", MARGIN + 230, y);
                drawText(cs, "QTE", MARGIN + 310, y);
                drawText(cs, "PRIX U.", MARGIN + 360, y);
                drawText(cs, "SOUS-TOTAL", MARGIN + 430, y);

                y -= 25;

                // Lignes de données
                double grandTotal = 0;
                boolean pair = false;

                for (LigneAchat l : achat.getLignesAchat()) {
                    // RÉINITIALISATION DU STYLE POUR CHAQUE LIGNE (Correction du Bug "ALPHA")
                    cs.setFont(fontNormal, 9);
                    cs.setNonStrokingColor(Color.decode("#334155"));

                    if (pair) {
                        drawFilledRect(cs, MARGIN, y - 5, PAGE_WIDTH - 2 * MARGIN, 20, Color.decode("#F8FAFC"));
                    }
                    String nomProd = l.getProduit().getNom();
                    if (nomProd.length() > 38) nomProd = nomProd.substring(0, 38) + "...";
                    double sousTotal = l.getQuantiteAchetee() * l.getPrixUnitaire();
                    grandTotal += sousTotal;

                    cs.setNonStrokingColor(Color.decode("#334155"));
                    drawText(cs, nomProd, MARGIN + 10, y);
                    drawText(cs, l.getLot().getNumeroLot(), MARGIN + 230, y);
                    
                    cs.setFont(fontBold, 9);
                    drawText(cs, String.valueOf(l.getQuantiteAchetee()), MARGIN + 310, y);
                    cs.setFont(fontNormal, 9);
                    
                    drawText(cs, String.format("%.0f", l.getPrixUnitaire()), MARGIN + 360, y);
                    
                    cs.setFont(fontBold, 9);
                    cs.setNonStrokingColor(Color.decode("#0F172A"));
                    drawText(cs, String.format("%.0f FCFA", sousTotal), MARGIN + 430, y);
                    
                    // Séparateur de ligne subtil
                    cs.setLineWidth(0.5f);
                    cs.setStrokingColor(Color.decode("#E2E8F0"));
                    cs.moveTo(MARGIN, y - 5);
                    cs.lineTo(PAGE_WIDTH - MARGIN, y - 5);
                    cs.stroke();

                    y -= 20;
                    pair = !pair;
                }

                // --- TOTAL BLOCK ---
                y -= 20;
                drawFilledRect(cs, PAGE_WIDTH - MARGIN - 240, y - 10, 240, 30, Color.decode("#F1F5F9"));
                cs.setLineWidth(1f);
                cs.setStrokingColor(Color.decode("#059669"));
                cs.moveTo(PAGE_WIDTH - MARGIN - 240, y - 10);
                cs.lineTo(PAGE_WIDTH - MARGIN - 240, y + 20);
                cs.stroke();

                cs.setFont(fontBold, 11);
                cs.setNonStrokingColor(Color.decode("#059669"));
                drawText(cs, "MONTANT TOTAL :", PAGE_WIDTH - MARGIN - 230, y - 1);
                
                cs.setFont(fontBold, 13);
                cs.setNonStrokingColor(Color.decode("#0F172A"));
                String strTotal = String.format("%.0f FCFA", grandTotal);
                float totalW = fontBold.getStringWidth(strTotal) / 1000 * 13;
                // Alignement mathématique parfait à droite
                drawText(cs, strTotal, PAGE_WIDTH - MARGIN - totalW - 10, y - 1);

                y -= 40;

                // --- MENTION LÉGALE ---
                cs.setFont(fontOblique, 9);
                cs.setNonStrokingColor(Color.decode("#64748B"));
                drawText(cs, "TVA non applicable, ou mention exonérée selon l'article en vigueur.", MARGIN, y);

                y -= 60;

                // --- SIGNATURES ---
                cs.setFont(fontBold, 10);
                cs.setNonStrokingColor(Color.decode("#1E293B"));
                drawText(cs, "Signature Fournisseur :", MARGIN, y);
                drawText(cs, "Cachet et Signature Pharmacie :", MARGIN + 300, y);
                y -= 40;
                
                // Lignes de pointillé ou de signature
                cs.setLineWidth(1f);
                cs.setStrokingColor(Color.decode("#CBD5E1"));
                cs.moveTo(MARGIN, y);
                cs.lineTo(MARGIN + 150, y);
                cs.stroke();
                
                cs.moveTo(MARGIN + 300, y);
                cs.lineTo(MARGIN + 480, y);
                cs.stroke();
            }

            document.save(fichier);
            logger.info("Bon de commande premium généré en temp : {}", fichier.getAbsolutePath());

            javafx.application.Platform.runLater(() -> {
                try {
                    javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(PdfService.class.getResource("/fxml/pdf_preview_modal.fxml"));
                    javafx.scene.Parent root = loader.load();
                    com.pharmacie.controllers.PdfPreviewController controller = loader.getController();
                    
                    Stage dialogStage = new Stage();
                    dialogStage.setTitle("Aperçu du Bon de Commande");
                    dialogStage.initModality(javafx.stage.Modality.WINDOW_MODAL);
                    dialogStage.initOwner(ownerStage);
                    dialogStage.setScene(new javafx.scene.Scene(root));
                    
                    controller.setDialogStage(dialogStage);
                    controller.loadPdf(fichier, nomFichier);
                    
                    dialogStage.showAndWait();
                } catch (Exception ex) {
                    logger.error("Erreur d'ouverture du modal PDF", ex);
                    com.pharmacie.utils.AlertUtils.showPremiumAlert(javafx.scene.control.Alert.AlertType.ERROR, 
                        "Erreur Aperçu", "Impossible d'afficher le document", ex.getMessage());
                }
            });

        } catch (Exception e) {
            logger.error("Erreur lors de la génération du PDF Premium", e);
        }
    }

    private static void drawText(PDPageContentStream cs, String text, float x, float y) throws IOException {
        cs.beginText();
        cs.newLineAtOffset(x, y);
        cs.showText(sanitize(text));
        cs.endText();
    }

    /**
     * Filtre les caractères hors WinAnsiEncoding (emoji, symboles Unicode > U+00FF)
     * pour éviter les crashes quand Helvetica est utilisé en fallback (sans fichier Inter.ttf).
     */
    private static String sanitize(String text) {
        if (text == null) return "";
        return text
            .replace("\u26A0", "[!]")   // ⚠ WARNING SIGN
            .replace("\u2713", "OK")    // ✓ CHECK MARK
            .replace("\u2714", "OK")    // ✔ HEAVY CHECK MARK
            .replace("\u2718", "X")     // ✘ HEAVY BALLOT X
            .replace("\u2022", "-")     // • BULLET
            .replace("\u2019", "'")     // ' RIGHT SINGLE QUOTATION MARK
            .replace("\u2018", "'")     // ' LEFT SINGLE QUOTATION MARK
            .replace("\u201C", "\"")    // " LEFT DOUBLE QUOTATION MARK
            .replace("\u201D", "\"")    // " RIGHT DOUBLE QUOTATION MARK
            .replaceAll("[^\\x00-\\xFF]", "?"); // Fallback universel
    }


    private static void drawFilledRect(PDPageContentStream cs, float x, float y, float w, float h, Color color) throws IOException {
        cs.setNonStrokingColor(color);
        cs.addRect(x, y, w, h);
        cs.fill();
        cs.setNonStrokingColor(Color.BLACK);
    }

    public static void genererRecapitulatifVentes(java.util.List<com.pharmacie.models.Vente> ventes, String periode, Stage ownerStage) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Enregistrer le Récapitulatif des Ventes");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Fichier PDF", "*.pdf"));
        fileChooser.setInitialFileName("RecapVentes_" + (periode.isEmpty() ? "tout" : periode) + ".pdf");

        File fichier = fileChooser.showSaveDialog(ownerStage);
        if (fichier == null) return;

        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            org.apache.pdfbox.pdmodel.font.PDFont fontBold;
            org.apache.pdfbox.pdmodel.font.PDFont fontNormal;
            try {
                File reg = new File("src/main/resources/fonts/Inter-Regular.ttf");
                File bold = new File("src/main/resources/fonts/Inter-Bold.ttf");
                if (reg.exists() && bold.exists()) {
                    fontNormal = org.apache.pdfbox.pdmodel.font.PDType0Font.load(document, reg);
                    fontBold = org.apache.pdfbox.pdmodel.font.PDType0Font.load(document, bold);
                } else {
                    fontBold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
                    fontNormal = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
                }
            } catch (Exception e) {
                fontBold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
                fontNormal = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            }
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

            try (PDPageContentStream cs = new PDPageContentStream(document, page)) {
                float y = PAGE_HEIGHT - MARGIN;

                // En-tête
                y = drawProfessionalHeader(document, cs, "HISTORIQUE DES VENTES", "Récapitulatif" + (periode.isEmpty() ? "" : " — Période : " + periode), y);

                // En-tête tableau Premium (Sans ID)
                drawFilledRect(cs, MARGIN, y - 5, PAGE_WIDTH - 2 * MARGIN, 22, Color.decode("#059669"));
                cs.setFont(fontBold, 9);
                cs.setNonStrokingColor(Color.WHITE);
                drawText(cs, "DATE & HEURE", MARGIN + 10, y);
                drawText(cs, "AGENT", MARGIN + 120, y);
                drawText(cs, "NB PROD.", MARGIN + 230, y);
                drawText(cs, "MODE PAIEMENT", MARGIN + 300, y);
                drawText(cs, "TOTAL (FCFA)", MARGIN + 430, y);

                y -= 25;

                // Lignes
                cs.setFont(fontNormal, 9);
                double grandTotal = 0;
                boolean pair = false;

                for (com.pharmacie.models.Vente v : ventes) {
                    if (y < MARGIN + 40) break; // Sécurité — page overflow simplifiée
                    
                    cs.setFont(fontNormal, 9);
                    cs.setNonStrokingColor(Color.decode("#334155"));

                    if (pair) {
                        drawFilledRect(cs, MARGIN, y - 5, PAGE_WIDTH - 2 * MARGIN, 20, Color.decode("#F8FAFC"));
                    }
                    
                    String dateStr = v.getDateVente() != null ? v.getDateVente().format(formatter) : "---";
                    drawText(cs, dateStr, MARGIN + 10, y);
                    String agent = v.getUser() != null ? v.getUser().getNom() : "---";
                    if (agent.length() > 13) agent = agent.substring(0, 13) + "...";
                    drawText(cs, agent, MARGIN + 120, y);
                    int nb = v.getLignesVente() != null ? v.getLignesVente().size() : 0;
                    drawText(cs, String.valueOf(nb), MARGIN + 240, y);
                    drawText(cs, v.getModePaiement() != null ? v.getModePaiement().name() : "", MARGIN + 300, y);
                    
                    cs.setFont(fontBold, 9);
                    cs.setNonStrokingColor(Color.decode("#0F172A"));
                    drawText(cs, String.format("%.0f", v.getTotal()), MARGIN + 430, y);
                    grandTotal += v.getTotal();
                    
                    // Séparateur fin
                    cs.setLineWidth(0.5f);
                    cs.setStrokingColor(Color.decode("#E2E8F0"));
                    cs.moveTo(MARGIN, y - 5);
                    cs.lineTo(PAGE_WIDTH - MARGIN, y - 5);
                    cs.stroke();

                    y -= 20;
                    pair = !pair;
                }

                // --- TOTAL BLOCK ---
                y -= 20;
                drawFilledRect(cs, PAGE_WIDTH - MARGIN - 260, y - 10, 260, 30, Color.decode("#F1F5F9"));
                cs.setLineWidth(1f);
                cs.setStrokingColor(Color.decode("#059669"));
                cs.moveTo(PAGE_WIDTH - MARGIN - 260, y - 10);
                cs.lineTo(PAGE_WIDTH - MARGIN - 260, y + 20);
                cs.stroke();

                cs.setFont(fontBold, 11);
                cs.setNonStrokingColor(Color.decode("#059669"));
                drawText(cs, "C.A TOTAL :", PAGE_WIDTH - MARGIN - 250, y - 1);
                
                cs.setFont(fontBold, 13);
                cs.setNonStrokingColor(Color.decode("#0F172A"));
                String strTotal = String.format("%.0f FCFA", grandTotal);
                float totalW = fontBold.getStringWidth(strTotal) / 1000 * 13;
                drawText(cs, strTotal, PAGE_WIDTH - MARGIN - totalW - 10, y - 1);
            }

            document.save(fichier);
            logger.info("Récap ventes sauvegardé : {}", fichier.getAbsolutePath());
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(fichier);
            }
        } catch (IOException e) {
            logger.error("Erreur PDF lors de la génération du récapitulatif ventes", e);
        }
    }

    public static void genererRecapitulatifAchats(java.util.List<com.pharmacie.models.Achat> achats, String periode, Stage ownerStage) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Enregistrer le Récapitulatif des Achats");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Fichier PDF", "*.pdf"));
        fileChooser.setInitialFileName("RecapAchats_" + (periode.isEmpty() ? "tout" : periode) + ".pdf");

        File fichier = fileChooser.showSaveDialog(ownerStage);
        if (fichier == null) return;

        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            org.apache.pdfbox.pdmodel.font.PDFont fontBold;
            org.apache.pdfbox.pdmodel.font.PDFont fontNormal;
            try {
                File reg = new File("src/main/resources/fonts/Inter-Regular.ttf");
                File bold = new File("src/main/resources/fonts/Inter-Bold.ttf");
                if (reg.exists() && bold.exists()) {
                    fontNormal = org.apache.pdfbox.pdmodel.font.PDType0Font.load(document, reg);
                    fontBold = org.apache.pdfbox.pdmodel.font.PDType0Font.load(document, bold);
                } else {
                    fontBold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
                    fontNormal = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
                }
            } catch (Exception e) {
                fontBold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
                fontNormal = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            }
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

            try (PDPageContentStream cs = new PDPageContentStream(document, page)) {
                float y = PAGE_HEIGHT - MARGIN;

                // En-tête
                y = drawProfessionalHeader(document, cs, "HISTORIQUE DES APPROVISIONNEMENTS", "Récapitulatif" + (periode.isEmpty() ? "" : " — Période : " + periode), y);

                // En-tête tableau Premium (sans ID)
                drawFilledRect(cs, MARGIN, y - 5, PAGE_WIDTH - 2 * MARGIN, 22, Color.decode("#059669"));
                cs.setFont(fontBold, 9);
                cs.setNonStrokingColor(Color.WHITE);
                drawText(cs, "DATE", MARGIN + 10, y);
                drawText(cs, "FOURNISSEUR", MARGIN + 80, y);
                drawText(cs, "REF FACTURE", MARGIN + 230, y);
                drawText(cs, "NB LIGNES", MARGIN + 340, y);
                drawText(cs, "MONTANT (FCFA)", MARGIN + 420, y);

                y -= 25;

                // Lignes
                cs.setFont(fontNormal, 9);
                double grandTotal = 0;
                boolean pair = false;

                for (com.pharmacie.models.Achat a : achats) {
                    if (y < MARGIN + 40) break;
                    
                    cs.setFont(fontNormal, 9);
                    cs.setNonStrokingColor(Color.decode("#334155"));

                    if (pair) {
                        drawFilledRect(cs, MARGIN, y - 5, PAGE_WIDTH - 2 * MARGIN, 20, Color.decode("#F8FAFC"));
                    }
                    
                    String dateStr = a.getDateAchat() != null ? a.getDateAchat().toLocalDate().format(formatter) : "---";
                    drawText(cs, dateStr, MARGIN + 10, y);
                    String fournisseur = a.getFournisseur() != null ? a.getFournisseur().getNom() : "---";
                    if (fournisseur.length() > 18) fournisseur = fournisseur.substring(0, 18) + "...";
                    drawText(cs, fournisseur, MARGIN + 80, y);
                    String ref = a.getReferenceFacture() != null ? a.getReferenceFacture() : "---";
                    drawText(cs, ref, MARGIN + 230, y);
                    int nbLignes = a.getLignesAchat() != null ? a.getLignesAchat().size() : 0;
                    drawText(cs, String.valueOf(nbLignes), MARGIN + 350, y);
                    
                    double montant = a.getLignesAchat() != null
                        ? a.getLignesAchat().stream().mapToDouble(la -> la.getQuantiteAchetee() * la.getPrixUnitaire()).sum()
                        : 0;
                        
                    cs.setFont(fontBold, 9);
                    cs.setNonStrokingColor(Color.decode("#0F172A"));
                    drawText(cs, String.format("%.0f", montant), MARGIN + 420, y);
                    grandTotal += montant;
                    
                    // Séparateur fin
                    cs.setLineWidth(0.5f);
                    cs.setStrokingColor(Color.decode("#E2E8F0"));
                    cs.moveTo(MARGIN, y - 5);
                    cs.lineTo(PAGE_WIDTH - MARGIN, y - 5);
                    cs.stroke();

                    y -= 20;
                    pair = !pair;
                }

                // --- TOTAL BLOCK ---
                y -= 20;
                drawFilledRect(cs, PAGE_WIDTH - MARGIN - 260, y - 10, 260, 30, Color.decode("#F1F5F9"));
                cs.setLineWidth(1f);
                cs.setStrokingColor(Color.decode("#059669"));
                cs.moveTo(PAGE_WIDTH - MARGIN - 260, y - 10);
                cs.lineTo(PAGE_WIDTH - MARGIN - 260, y + 20);
                cs.stroke();

                cs.setFont(fontBold, 11);
                cs.setNonStrokingColor(Color.decode("#059669"));
                drawText(cs, "ACHATS TOTAUX :", PAGE_WIDTH - MARGIN - 250, y - 1);
                
                cs.setFont(fontBold, 13);
                cs.setNonStrokingColor(Color.decode("#0F172A"));
                String strTotal = String.format("%.0f FCFA", grandTotal);
                float totalW = fontBold.getStringWidth(strTotal) / 1000 * 13;
                drawText(cs, strTotal, PAGE_WIDTH - MARGIN - totalW - 10, y - 1);
            }

            document.save(fichier);
            logger.info("Récap achats sauvegardé : {}", fichier.getAbsolutePath());
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(fichier);
            }
        } catch (IOException e) {
            logger.error("Erreur PDF lors de la génération du récapitulatif achats", e);
        }
    }

    public static void genererEtatStockPdf(java.util.List<com.pharmacie.controllers.ProduitController.EtatStockDTO> stockList, Stage ownerStage) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Enregistrer l'état du stock");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Fichier PDF", "*.pdf"));
        fileChooser.setInitialFileName("Etat_Stock_Inventaire.pdf");

        File fichier = fileChooser.showSaveDialog(ownerStage);
        if (fichier == null) return;

        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            org.apache.pdfbox.pdmodel.font.PDFont fontBold;
            org.apache.pdfbox.pdmodel.font.PDFont fontNormal;
            try {
                File reg = new File("src/main/resources/fonts/Inter-Regular.ttf");
                File bold = new File("src/main/resources/fonts/Inter-Bold.ttf");
                if (reg.exists() && bold.exists()) {
                    fontNormal = org.apache.pdfbox.pdmodel.font.PDType0Font.load(document, reg);
                    fontBold = org.apache.pdfbox.pdmodel.font.PDType0Font.load(document, bold);
                } else {
                    fontBold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
                    fontNormal = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
                }
            } catch (Exception e) {
                fontBold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
                fontNormal = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            }

            try (PDPageContentStream cs = new PDPageContentStream(document, page)) {
                float y = PAGE_HEIGHT - MARGIN;

                y = drawProfessionalHeader(document, cs, "ETAT DU STOCK PAR LOTS", "Inventaire Valorisé", y);

                // En-tête tableau Premium
                drawFilledRect(cs, MARGIN, y - 5, PAGE_WIDTH - 2 * MARGIN, 22, Color.decode("#059669"));
                cs.setFont(fontBold, 9);
                cs.setNonStrokingColor(Color.WHITE);
                drawText(cs, "PRODUIT", MARGIN + 10, y);
                drawText(cs, "LOT", MARGIN + 95, y);
                drawText(cs, "EXPIR.", MARGIN + 155, y);
                drawText(cs, "JOURS", MARGIN + 210, y);
                drawText(cs, "STOCK", MARGIN + 250, y);
                drawText(cs, "SEUIL", MARGIN + 315, y);
                drawText(cs, "VENDU", MARGIN + 355, y);
                drawText(cs, "PRIX U.", MARGIN + 400, y);
                drawText(cs, "VALEUR", MARGIN + 450, y);

                y -= 25;

                cs.setFont(fontNormal, 8); // Plus petit pour tout faire rentrer
                double valeurTotale = 0;
                boolean pair = false;

                for (com.pharmacie.controllers.ProduitController.EtatStockDTO s : stockList) {
                    if (y < MARGIN + 40) break;
                    
                    cs.setFont(fontNormal, 8);
                    cs.setNonStrokingColor(Color.decode("#334155"));

                    if (pair) {
                        drawFilledRect(cs, MARGIN, y - 5, PAGE_WIDTH - 2 * MARGIN, 18, Color.decode("#F8FAFC"));
                    }
                    
                    String prod = s.getProduitNom();
                    if (prod.length() > 18) prod = prod.substring(0, 18) + ".";
                    drawText(cs, prod, MARGIN + 10, y);
                    
                    String lot = s.getLotNumero();
                    if (lot.length() > 10) lot = lot.substring(0, 10);
                    drawText(cs, lot, MARGIN + 95, y);
                    
                    drawText(cs, s.getDateExpiration(), MARGIN + 155, y);
                    
                    String jours = s.getJoursRestantsFormate().replace(" Expiré", " Exp.");
                    drawText(cs, jours, MARGIN + 210, y);

                    String qte = s.getQuantiteFormatee()
                        .replace(" Bte(s) et ", "B/").replace(" Unité(s)", "U")
                        .replace(" Bte(s)", "B").replace(" Unités", "U").replace(" Unité", "U");
                    drawText(cs, qte, MARGIN + 250, y);

                    drawText(cs, String.valueOf(s.getSeuilAlerte()), MARGIN + 320, y);
                    drawText(cs, String.valueOf(s.getQuantiteVendue()), MARGIN + 360, y);
                    
                    double prix = s.getPrixUnitaire() != null ? s.getPrixUnitaire() : 0;
                    drawText(cs, String.format("%.0f", prix), MARGIN + 400, y);
                    
                    double valeurLigne = s.getValeurFinanciere() != null ? s.getValeurFinanciere() : 0;
                    
                    cs.setFont(fontBold, 8);
                    cs.setNonStrokingColor(Color.decode("#0F172A"));
                    drawText(cs, String.format("%.0f", valeurLigne), MARGIN + 450, y);
                    
                    valeurTotale += valeurLigne;
                    
                    // Séparateur fin
                    cs.setLineWidth(0.5f);
                    cs.setStrokingColor(Color.decode("#E2E8F0"));
                    cs.moveTo(MARGIN, y - 5);
                    cs.lineTo(PAGE_WIDTH - MARGIN, y - 5);
                    cs.stroke();

                    y -= 18;
                    pair = !pair;
                }

                // --- TOTAL BLOCK ---
                y -= 20;
                drawFilledRect(cs, PAGE_WIDTH - MARGIN - 260, y - 10, 260, 30, Color.decode("#F1F5F9"));
                cs.setLineWidth(1f);
                cs.setStrokingColor(Color.decode("#059669"));
                cs.moveTo(PAGE_WIDTH - MARGIN - 260, y - 10);
                cs.lineTo(PAGE_WIDTH - MARGIN - 260, y + 20);
                cs.stroke();

                cs.setFont(fontBold, 11);
                cs.setNonStrokingColor(Color.decode("#059669"));
                drawText(cs, "VALEUR TOTALE :", PAGE_WIDTH - MARGIN - 250, y - 1);
                
                cs.setFont(fontBold, 13);
                cs.setNonStrokingColor(Color.decode("#0F172A"));
                String strTotal = String.format("%.0f FCFA", valeurTotale);
                float totalW = fontBold.getStringWidth(strTotal) / 1000 * 13;
                drawText(cs, strTotal, PAGE_WIDTH - MARGIN - totalW - 10, y - 1);
            }

            document.save(fichier);
            logger.info("Etat du stock sauvegardé : {}", fichier.getAbsolutePath());
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(fichier);
            }
        } catch (IOException e) {
            logger.error("Erreur PDF lors de la génération de l'état du stock", e);
        }
    }

    public static void genererRapportAjustements(
            java.util.List<com.pharmacie.models.AjustementStock> ajustements,
            String periode,
            com.pharmacie.models.MouvementStock.TypeMouvement operation,
            Stage ownerStage) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Enregistrer le rapport des ajustements");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Fichier PDF", "*.pdf"));
        fileChooser.setInitialFileName("Rapport_Ajustements_" + (periode.isEmpty() ? "tout" : periode.replace(" au ", "_").replace("/", "-")) + ".pdf");

        File fichier = fileChooser.showSaveDialog(ownerStage);
        if (fichier == null) return;

        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            org.apache.pdfbox.pdmodel.font.PDFont fontBold;
            org.apache.pdfbox.pdmodel.font.PDFont fontNormal;
            try {
                File reg = new File("src/main/resources/fonts/Inter-Regular.ttf");
                File bold = new File("src/main/resources/fonts/Inter-Bold.ttf");
                if (reg.exists() && bold.exists()) {
                    fontNormal = org.apache.pdfbox.pdmodel.font.PDType0Font.load(document, reg);
                    fontBold = org.apache.pdfbox.pdmodel.font.PDType0Font.load(document, bold);
                } else {
                    fontBold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
                    fontNormal = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
                }
            } catch (Exception e) {
                fontBold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
                fontNormal = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            }
            java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

            try (PDPageContentStream cs = new PDPageContentStream(document, page)) {
                float y = PAGE_HEIGHT - MARGIN;

                // Construire le label de l'opération pour le sous-titre
                String opLabel;
                if (operation == null) {
                    opLabel = "Toutes les opérations";
                } else switch (operation) {
                    case AJUSTEMENT_POSITIF: opLabel = "Ajouts au stock"; break;
                    case AJUSTEMENT_NEGATIF: opLabel = "Retraits du stock"; break;
                    default: opLabel = operation.name(); break;
                }

                y = drawProfessionalHeader(document, cs, "HISTORIQUE DES AJUSTEMENTS",
                    "Période : " + periode + "   |   Opération : " + opLabel, y);

                // En-tête tableau Premium
                // Colonnes réorganisées : DATE | PRODUIT | LOT | OPÉRATION | MOTIF | QTE | AGENT
                drawFilledRect(cs, MARGIN, y - 5, PAGE_WIDTH - 2 * MARGIN, 22, Color.decode("#059669"));
                cs.setFont(fontBold, 9);
                cs.setNonStrokingColor(Color.WHITE);
                drawText(cs, "DATE", MARGIN + 10, y);
                drawText(cs, "PRODUIT", MARGIN + 100, y);
                drawText(cs, "LOT", MARGIN + 215, y);
                drawText(cs, "MOTIF", MARGIN + 268, y);
                drawText(cs, "QTE", MARGIN + 400, y);
                drawText(cs, "AGENT", MARGIN + 435, y);

                y -= 25;

                cs.setFont(fontNormal, 9);
                int totalPerte = 0;
                boolean pair = false;

                for (com.pharmacie.models.AjustementStock a : ajustements) {
                    if (y < MARGIN + 40) break;
                    
                    cs.setFont(fontNormal, 9);
                    cs.setNonStrokingColor(Color.decode("#334155"));

                    if (pair) {
                        drawFilledRect(cs, MARGIN, y - 5, PAGE_WIDTH - 2 * MARGIN, 20, Color.decode("#F8FAFC"));
                    }
                    
                    String dateStr = a.getDateAjustement() != null ? a.getDateAjustement().format(formatter) : "---";
                    drawText(cs, dateStr, MARGIN + 10, y);
                    
                    String prod = a.getLot().getProduit().getNom();
                    if (prod.length() > 16) prod = prod.substring(0, 16) + "...";
                    drawText(cs, prod, MARGIN + 100, y);
                    
                    String lot = a.getLot().getNumeroLot();
                    if (lot.length() > 8) lot = lot.substring(0, 8);
                    drawText(cs, lot, MARGIN + 215, y);
                    
                    // Motif avec label lisible
                    String motifLabel = a.getMotif().getLabel();
                    if (motifLabel.length() > 18) motifLabel = motifLabel.substring(0, 18) + ".";
                    drawText(cs, motifLabel, MARGIN + 268, y);
                    
                    // Coloriser la quantité selon le type d'opération
                    com.pharmacie.models.MouvementStock.TypeMouvement typeOp = a.getTypeAjustement() != null
                        ? a.getTypeAjustement()
                        : com.pharmacie.models.MouvementStock.TypeMouvement.AJUSTEMENT_NEGATIF;
                    
                    cs.setFont(fontBold, 9);
                    if (typeOp == com.pharmacie.models.MouvementStock.TypeMouvement.AJUSTEMENT_POSITIF) {
                        cs.setNonStrokingColor(Color.decode("#059669")); // Vert
                        drawText(cs, "+ " + a.getQuantite(), MARGIN + 400, y);
                    } else {
                        cs.setNonStrokingColor(Color.decode("#E11D48")); // Rouge
                        drawText(cs, "- " + a.getQuantite(), MARGIN + 400, y);
                    }
                    
                    cs.setFont(fontNormal, 9);
                    cs.setNonStrokingColor(Color.decode("#334155"));
                    String user = a.getUser().getNom();
                    if (user.length() > 15) user = user.substring(0, 15) + "...";
                    drawText(cs, user, MARGIN + 435, y);
                    
                    totalPerte += a.getQuantite();
                    
                    // Séparateur fin
                    cs.setLineWidth(0.5f);
                    cs.setStrokingColor(Color.decode("#E2E8F0"));
                    cs.moveTo(MARGIN, y - 5);
                    cs.lineTo(PAGE_WIDTH - MARGIN, y - 5);
                    cs.stroke();

                    y -= 20;
                    pair = !pair;
                }

                // --- TOTAL BLOCK ---
                y -= 20;
                drawFilledRect(cs, PAGE_WIDTH - MARGIN - 260, y - 10, 260, 30, Color.decode("#F1F5F9"));
                cs.setLineWidth(1f);
                cs.setStrokingColor(Color.decode("#059669"));
                cs.moveTo(PAGE_WIDTH - MARGIN - 260, y - 10);
                cs.lineTo(PAGE_WIDTH - MARGIN - 260, y + 20);
                cs.stroke();

                cs.setFont(fontBold, 11);
                cs.setNonStrokingColor(Color.decode("#059669"));
                drawText(cs, "UNITÉS PERDUES :", PAGE_WIDTH - MARGIN - 250, y - 1);
                
                cs.setFont(fontBold, 13);
                cs.setNonStrokingColor(Color.decode("#0F172A"));
                String strTotal = String.valueOf(totalPerte);
                float totalW = fontBold.getStringWidth(strTotal) / 1000 * 13;
                drawText(cs, strTotal, PAGE_WIDTH - MARGIN - totalW - 10, y - 1);
            }

            document.save(fichier);
            logger.info("Rapport ajustements sauvegardé : {}", fichier.getAbsolutePath());
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(fichier);
            }
        } catch (IOException e) {
            logger.error("Erreur PDF lors de la génération des ajustements", e);
        }
    }

    public static void genererJournalVentes(java.util.List<com.pharmacie.models.LigneVente> lignes, String periode, Stage ownerStage) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Enregistrer le Journal détaillé des Ventes");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Fichier PDF", "*.pdf"));
        fileChooser.setInitialFileName("Journal_Ventes_" + (periode.isEmpty() ? "tout" : periode.replace(" au ", "_").replace("/", "-")) + ".pdf");

        File fichier = fileChooser.showSaveDialog(ownerStage);
        if (fichier == null) return;

        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            org.apache.pdfbox.pdmodel.font.PDFont fontBold;
            org.apache.pdfbox.pdmodel.font.PDFont fontNormal;
            try {
                File reg = new File("src/main/resources/fonts/Inter-Regular.ttf");
                File bold = new File("src/main/resources/fonts/Inter-Bold.ttf");
                if (reg.exists() && bold.exists()) {
                    fontNormal = org.apache.pdfbox.pdmodel.font.PDType0Font.load(document, reg);
                    fontBold = org.apache.pdfbox.pdmodel.font.PDType0Font.load(document, bold);
                } else {
                    fontBold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
                    fontNormal = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
                }
            } catch (Exception e) {
                fontBold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
                fontNormal = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            }
            java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

            try (PDPageContentStream cs = new PDPageContentStream(document, page)) {
                float y = PAGE_HEIGHT - MARGIN;

                y = drawProfessionalHeader(document, cs, "LIGNES DE VENTE", "Journal Détaillé — Période : " + periode, y);

                // En-tête tableau Premium
                drawFilledRect(cs, MARGIN, y - 5, PAGE_WIDTH - 2 * MARGIN, 22, Color.decode("#059669"));
                cs.setFont(fontBold, 9);
                cs.setNonStrokingColor(Color.WHITE);
                drawText(cs, "DATE", MARGIN + 10, y);
                drawText(cs, "TICKET", MARGIN + 90, y);
                drawText(cs, "PRODUIT", MARGIN + 150, y);
                drawText(cs, "CAISSIER", MARGIN + 290, y);
                drawText(cs, "QTE", MARGIN + 370, y);
                drawText(cs, "TOTAL", MARGIN + 420, y);

                y -= 25;

                cs.setFont(fontNormal, 9);
                double grandTotal = 0;
                boolean pair = false;

                for (com.pharmacie.models.LigneVente lv : lignes) {
                    if (y < MARGIN + 40) break;
                    
                    cs.setFont(fontNormal, 9);
                    cs.setNonStrokingColor(Color.decode("#334155"));

                    if (pair) {
                        drawFilledRect(cs, MARGIN, y - 5, PAGE_WIDTH - 2 * MARGIN, 20, Color.decode("#F8FAFC"));
                    }
                    
                    String dateStr = lv.getVente().getDateVente() != null ? lv.getVente().getDateVente().format(formatter) : "---";
                    drawText(cs, dateStr, MARGIN + 10, y);
                    
                    drawText(cs, "TCK-" + lv.getVente().getId(), MARGIN + 90, y);
                    
                    String prod = lv.getProduit().getNom();
                    if (prod.length() > 25) prod = prod.substring(0, 25) + "...";
                    drawText(cs, prod, MARGIN + 150, y);
                    
                    String agent = lv.getVente().getUser().getNom();
                    if (agent.length() > 18) agent = agent.substring(0, 18) + "...";
                    drawText(cs, agent, MARGIN + 290, y);
                    
                    cs.setFont(fontBold, 9);
                    drawText(cs, String.valueOf(lv.getQuantiteVendue()), MARGIN + 375, y);
                    
                    cs.setNonStrokingColor(Color.decode("#0F172A"));
                    drawText(cs, String.format("%.0f", lv.getSousTotal()), MARGIN + 420, y);
                    
                    grandTotal += lv.getSousTotal();
                    
                    // Séparateur fin
                    cs.setLineWidth(0.5f);
                    cs.setStrokingColor(Color.decode("#E2E8F0"));
                    cs.moveTo(MARGIN, y - 5);
                    cs.lineTo(PAGE_WIDTH - MARGIN, y - 5);
                    cs.stroke();

                    y -= 20;
                    pair = !pair;
                }

                // --- TOTAL BLOCK ---
                y -= 20;
                drawFilledRect(cs, PAGE_WIDTH - MARGIN - 260, y - 10, 260, 30, Color.decode("#F1F5F9"));
                cs.setLineWidth(1f);
                cs.setStrokingColor(Color.decode("#059669"));
                cs.moveTo(PAGE_WIDTH - MARGIN - 260, y - 10);
                cs.lineTo(PAGE_WIDTH - MARGIN - 260, y + 20);
                cs.stroke();

                cs.setFont(fontBold, 11);
                cs.setNonStrokingColor(Color.decode("#059669"));
                drawText(cs, "C.A SÉLECTION :", PAGE_WIDTH - MARGIN - 250, y - 1);
                
                cs.setFont(fontBold, 13);
                cs.setNonStrokingColor(Color.decode("#0F172A"));
                String strTotal = String.format("%.0f FCFA", grandTotal);
                float totalW = fontBold.getStringWidth(strTotal) / 1000 * 13;
                drawText(cs, strTotal, PAGE_WIDTH - MARGIN - totalW - 10, y - 1);
            }

            document.save(fichier);
            logger.info("Journal détaillé ventes sauvegardé : {}", fichier.getAbsolutePath());
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(fichier);
            }
        } catch (IOException e) {
            logger.error("Erreur PDF lors de la génération du journal des ventes", e);
        }
    }

    public static void genererRapportAudit(java.util.List<com.pharmacie.models.MouvementStock> mouvements, String periode, Stage ownerStage) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Enregistrer le rapport d'Audit des Stocks");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Fichier PDF", "*.pdf"));
        fileChooser.setInitialFileName("Rapport_Audit_" + (periode.isEmpty() ? "tout" : periode.replace(" au ", "_").replace("/", "-")) + ".pdf");

        File fichier = fileChooser.showSaveDialog(ownerStage);
        if (fichier == null) return;

        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            org.apache.pdfbox.pdmodel.font.PDFont fontBold;
            org.apache.pdfbox.pdmodel.font.PDFont fontNormal;
            try {
                File reg = new File("src/main/resources/fonts/Inter-Regular.ttf");
                File bold = new File("src/main/resources/fonts/Inter-Bold.ttf");
                if (reg.exists() && bold.exists()) {
                    fontNormal = org.apache.pdfbox.pdmodel.font.PDType0Font.load(document, reg);
                    fontBold = org.apache.pdfbox.pdmodel.font.PDType0Font.load(document, bold);
                } else {
                    fontBold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
                    fontNormal = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
                }
            } catch (Exception e) {
                fontBold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
                fontNormal = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            }
            java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

            try (PDPageContentStream cs = new PDPageContentStream(document, page)) {
                float y = PAGE_HEIGHT - MARGIN;

                y = drawProfessionalHeader(document, cs, "AUDIT DES STOCKS", "Période : " + periode, y);

                // En-tête tableau Premium — 6 colonnes en 8pt sur 495 pts disponibles
                // DATE(70) | TYPE(55) | PRODUIT/LOT(145) | QTE(45) | REFERENCE(110) | AGENT(60)
                drawFilledRect(cs, MARGIN, y - 5, PAGE_WIDTH - 2 * MARGIN, 22, Color.decode("#059669"));
                cs.setFont(fontBold, 8);
                cs.setNonStrokingColor(Color.WHITE);
                drawText(cs, "DATE",         MARGIN + 10,  y);
                drawText(cs, "TYPE",         MARGIN + 80,  y);
                drawText(cs, "PRODUIT / LOT",MARGIN + 135, y);
                drawText(cs, "QTE",          MARGIN + 280, y);
                drawText(cs, "REFERENCE",    MARGIN + 325, y);
                drawText(cs, "AGENT",        MARGIN + 435, y);

                y -= 25;

                cs.setFont(fontNormal, 8);
                boolean pair = false;

                for (com.pharmacie.models.MouvementStock m : mouvements) {
                    if (y < MARGIN + 40) break;
                    
                    cs.setFont(fontNormal, 8);
                    cs.setNonStrokingColor(Color.decode("#334155"));

                    if (pair) {
                        drawFilledRect(cs, MARGIN, y - 5, PAGE_WIDTH - 2 * MARGIN, 18, Color.decode("#F8FAFC"));
                    }
                    
                    drawText(cs, m.getDateMouvement().format(formatter), MARGIN + 10, y);
                    
                    // Label lisible pour le type de mouvement
                    String typeLabel;
                    switch (m.getTypeMouvement()) {
                        case VENTE:               typeLabel = "Vente";   cs.setNonStrokingColor(Color.decode("#059669")); break;
                        case ACHAT:               typeLabel = "Achat";   cs.setNonStrokingColor(Color.decode("#2563EB")); break;
                        case AJUSTEMENT_POSITIF:  typeLabel = "Ajout";   cs.setNonStrokingColor(Color.decode("#059669")); break;
                        case AJUSTEMENT_NEGATIF:  typeLabel = "Retrait"; cs.setNonStrokingColor(Color.decode("#E11D48")); break;
                        default:                  typeLabel = m.getTypeMouvement().name(); cs.setNonStrokingColor(Color.decode("#334155")); break;
                    }
                    drawText(cs, typeLabel, MARGIN + 80, y);
                    
                    cs.setNonStrokingColor(Color.decode("#334155"));
                    String prod = m.getProduit().getNom();
                    if (m.getLot() != null) prod += " (L: " + m.getLot().getNumeroLot() + ")";
                    if (prod.length() > 26) prod = prod.substring(0, 23) + "...";
                    drawText(cs, prod, MARGIN + 135, y);
                    
                    String prefix = m.getQuantite() > 0 ? "+ " : "- ";
                    cs.setFont(fontBold, 8);
                    drawText(cs, prefix + Math.abs(m.getQuantite()), MARGIN + 280, y);
                    
                    cs.setFont(fontNormal, 8);
                    String ref;
                    switch (m.getTypeMouvement()) {
                        case AJUSTEMENT_POSITIF:
                        case AJUSTEMENT_NEGATIF:
                            ref = "\u2014"; // Tiret long — : ajustement sans référence externe
                            break;
                        default:
                            ref = m.getReference() != null ? m.getReference() : "N/A";
                            if (ref.length() > 17) ref = ref.substring(0, 14) + "...";
                            break;
                    }
                    drawText(cs, ref, MARGIN + 325, y);
                    
                    String user = m.getUser().getNom();
                    if (user.length() > 14) user = user.substring(0, 14) + ".";
                    drawText(cs, user, MARGIN + 435, y);
                    
                    // Séparateur fin
                    cs.setLineWidth(0.5f);
                    cs.setStrokingColor(Color.decode("#E2E8F0"));
                    cs.moveTo(MARGIN, y - 5);
                    cs.lineTo(PAGE_WIDTH - MARGIN, y - 5);
                    cs.stroke();
                    
                    y -= 18;
                    pair = !pair;
                }

            }
            document.save(fichier);
            logger.info("Rapport Audit sauvegardé : {}", fichier.getAbsolutePath());
            if (Desktop.isDesktopSupported()) Desktop.getDesktop().open(fichier);
        } catch (IOException e) {
            logger.error("Erreur PDF lors de l'audit", e);
        }
    }

    public static void genererRapportClotures(java.util.List<com.pharmacie.models.SessionCaisse> sessions, String periode, Stage ownerStage) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Enregistrer le rapport des Sessions de Caisse");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Fichier PDF", "*.pdf"));
        fileChooser.setInitialFileName("Rapport_Clotures_" + (periode.isEmpty() ? "tout" : periode.replace(" au ", "_").replace("/", "-")) + ".pdf");

        File fichier = fileChooser.showSaveDialog(ownerStage);
        if (fichier == null) return;

        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            org.apache.pdfbox.pdmodel.font.PDFont fontBold;
            org.apache.pdfbox.pdmodel.font.PDFont fontNormal;
            try {
                File reg = new File("src/main/resources/fonts/Inter-Regular.ttf");
                File bold = new File("src/main/resources/fonts/Inter-Bold.ttf");
                if (reg.exists() && bold.exists()) {
                    fontNormal = org.apache.pdfbox.pdmodel.font.PDType0Font.load(document, reg);
                    fontBold = org.apache.pdfbox.pdmodel.font.PDType0Font.load(document, bold);
                } else {
                    fontBold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
                    fontNormal = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
                }
            } catch (Exception e) {
                fontBold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
                fontNormal = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            }
            java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yy HH:mm");

            try (PDPageContentStream cs = new PDPageContentStream(document, page)) {
                float y = PAGE_HEIGHT - MARGIN;

                y = drawProfessionalHeader(document, cs, "SESSIONS DE CAISSE (CLOTURES Z)", "Période : " + periode, y);

                // En-tête tableau Premium
                drawFilledRect(cs, MARGIN, y - 5, PAGE_WIDTH - 2 * MARGIN, 22, Color.decode("#059669"));
                cs.setFont(fontBold, 9);
                cs.setNonStrokingColor(Color.WHITE);
                drawText(cs, "OUVERTURE", MARGIN + 10, y);
                drawText(cs, "CLOTURE", MARGIN + 90, y);
                drawText(cs, "AGENT", MARGIN + 170, y);
                drawText(cs, "STATUT", MARGIN + 255, y);
                drawText(cs, "ECART ESPECES", MARGIN + 325, y);
                drawText(cs, "ECART MOBILE", MARGIN + 435, y);

                y -= 25;

                cs.setFont(fontNormal, 9);
                boolean pair = false;

                for (com.pharmacie.models.SessionCaisse s : sessions) {
                    if (y < MARGIN + 40) break;
                    
                    cs.setFont(fontNormal, 9);
                    cs.setNonStrokingColor(Color.decode("#334155"));

                    if (pair) {
                        drawFilledRect(cs, MARGIN, y - 5, PAGE_WIDTH - 2 * MARGIN, 20, Color.decode("#F8FAFC"));
                    }
                    
                    drawText(cs, s.getDateOuverture().format(formatter), MARGIN + 10, y);
                    drawText(cs, s.getDateCloture() != null ? s.getDateCloture().format(formatter) : "En cours", MARGIN + 90, y);
                    
                    String user = s.getUser().getNom();
                    if (user.length() > 15) user = user.substring(0, 12) + "...";
                    drawText(cs, user, MARGIN + 170, y);
                    
                    drawText(cs, s.getStatut().name(), MARGIN + 255, y);
                    
                    // Couleur des écarts
                    cs.setFont(fontBold, 9);
                    double ecartEsp = s.getEcartEspeces() != null ? s.getEcartEspeces() : 0.0;
                    if (ecartEsp < 0) cs.setNonStrokingColor(Color.decode("#E11D48")); // Rouge 600
                    else if (ecartEsp > 0) cs.setNonStrokingColor(Color.decode("#059669")); // Emerald 600
                    drawText(cs, String.format("%.0f F", ecartEsp), MARGIN + 325, y);
                    
                    cs.setNonStrokingColor(Color.decode("#334155"));
                    double ecartMob = s.getEcartMobile() != null ? s.getEcartMobile() : 0.0;
                    if (ecartMob < 0) cs.setNonStrokingColor(Color.decode("#E11D48")); // Rouge 600
                    else if (ecartMob > 0) cs.setNonStrokingColor(Color.decode("#059669")); // Emerald 600
                    drawText(cs, String.format("%.0f F", ecartMob), MARGIN + 435, y);
                    
                    // Séparateur fin
                    cs.setLineWidth(0.5f);
                    cs.setStrokingColor(Color.decode("#E2E8F0"));
                    cs.moveTo(MARGIN, y - 5);
                    cs.lineTo(PAGE_WIDTH - MARGIN, y - 5);
                    cs.stroke();
                    
                    y -= 20;
                    pair = !pair;
                }
            }
            document.save(fichier);
            logger.info("Rapport Clôtures sauvegardé : {}", fichier.getAbsolutePath());
            if (Desktop.isDesktopSupported()) Desktop.getDesktop().open(fichier);
        } catch (IOException e) {
            logger.error("Erreur PDF lors de la génération du rapport de clôture", e);
        }
    }
}

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

    private static float drawProfessionalHeader(PDPageContentStream cs, String documentTitle, String subtitle, float startY) throws IOException {
        com.pharmacie.models.PharmacieInfo info = new com.pharmacie.dao.PharmacieInfoDAO().getInfo();
        String nom = (info != null && info.getNom() != null && !info.getNom().isEmpty()) ? info.getNom() : "PHARMACIE VETERINAIRE";
        String adresse = (info != null && info.getAdresse() != null) ? info.getAdresse() : "";
        String telephone = (info != null && info.getTelephone() != null) ? info.getTelephone() : "";
        String email = (info != null && info.getEmail() != null) ? info.getEmail() : "";

        PDType1Font fontBold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
        PDType1Font fontNormal = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
        
        float y = startY;

        // 1. Nom Pharmacie
        cs.setFont(fontBold, 20);
        cs.setNonStrokingColor(Color.decode("#2C3E50"));
        drawText(cs, nom.toUpperCase(), MARGIN, y);
        y -= 15;
        
        // 2. Coordonnées
        cs.setFont(fontNormal, 10);
        cs.setNonStrokingColor(Color.DARK_GRAY);
        if (!adresse.trim().isEmpty()) {
            drawText(cs, adresse, MARGIN, y);
            y -= 14;
        }
        
        String contactLine = "";
        if (!telephone.trim().isEmpty()) contactLine += "Tel: " + telephone;
        if (!email.trim().isEmpty()) {
            if (!contactLine.isEmpty()) contactLine += "  |  ";
            contactLine += "Email/NINEA: " + email;
        }
        
        if (!contactLine.isEmpty()) {
            drawText(cs, contactLine, MARGIN, y);
            y -= 15;
        }

        y -= 8;
        
        // Séparateur principal
        cs.setLineWidth(1.5f);
        cs.setStrokingColor(Color.decode("#BDC3C7"));
        cs.moveTo(MARGIN, y);
        cs.lineTo(PAGE_WIDTH - MARGIN, y);
        cs.stroke();
        cs.setStrokingColor(Color.BLACK);
        
        y -= 25;
        
        // 3. Titre du Document
        cs.setNonStrokingColor(Color.decode("#2980B9"));
        cs.setFont(fontBold, 16);
        drawText(cs, documentTitle.toUpperCase(), MARGIN, y);
        y -= 18;
        
        if (subtitle != null && !subtitle.isEmpty()) {
            cs.setNonStrokingColor(Color.BLACK);
            cs.setFont(fontNormal, 11);
            drawText(cs, subtitle, MARGIN, y);
            y -= 18;
        }
        
        cs.setNonStrokingColor(Color.BLACK);
        return y - 10;
    }

    public static void genererBonDeCommande(Achat achat, Stage ownerStage) {
        // Boite de dialogue pour choisir l'emplacement de sauvegarde
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Enregistrer le Bon de Commande");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Fichier PDF", "*.pdf"));
        String nomFichier = "BonCommande_" + (achat.getReferenceFacture() != null ? achat.getReferenceFacture() : achat.getId()) + ".pdf";
        fileChooser.setInitialFileName(nomFichier);

        File fichier = fileChooser.showSaveDialog(ownerStage);
        if (fichier == null) return; // L'utilisateur a annulé

        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            PDType1Font fontBold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            PDType1Font fontNormal = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

            try (PDPageContentStream cs = new PDPageContentStream(document, page)) {

                float y = PAGE_HEIGHT - MARGIN;

                // -------- EN-TETE PROFESSIONNEL --------
                y = drawProfessionalHeader(cs, "BON DE COMMANDE", "Document d'approvisionnement", y);

                // -------- INFOS --------
                cs.setFont(fontNormal, 11);
                drawText(cs, "Fournisseur : " + achat.getFournisseur().getNom(), MARGIN, y);
                y -= 16;

                String contact = achat.getFournisseur().getTelephone() != null ? achat.getFournisseur().getTelephone() : "N/A";
                drawText(cs, "Contact      : " + contact, MARGIN, y);
                y -= 16;

                String ref = achat.getReferenceFacture() != null ? achat.getReferenceFacture() : "---";
                drawText(cs, "Ref Facture  : " + ref, MARGIN, y);
                y -= 16;

                String date = achat.getDateAchat() != null ? achat.getDateAchat().toLocalDate().format(formatter) : "---";
                drawText(cs, "Date         : " + date, MARGIN, y);
                y -= 30;

                // -------- EN-TETE TABLEAU --------
                cs.setFont(fontBold, 10);
                cs.setNonStrokingColor(Color.decode("#2C3E50"));
                drawFilledRect(cs, MARGIN, y - 5, PAGE_WIDTH - 2 * MARGIN, 18, Color.decode("#ECF0F1"));
                cs.setNonStrokingColor(Color.BLACK);

                drawText(cs, "PRODUIT", MARGIN + 5, y);
                drawText(cs, "N° LOT", MARGIN + 220, y);
                drawText(cs, "QTE", MARGIN + 310, y);
                drawText(cs, "PRIX/U", MARGIN + 360, y);
                drawText(cs, "SOUS-TOTAL", MARGIN + 420, y);

                // Ligne sous le header — dessinée AVANT le décrément pour rester entre header et données
                cs.setLineWidth(0.5f);
                cs.moveTo(MARGIN, y - 7);
                cs.lineTo(PAGE_WIDTH - MARGIN, y - 7);
                cs.stroke();

                y -= 22; // Espacement suffisant pour que les données commencent bien en dessous

                // -------- LIGNES --------
                cs.setFont(fontNormal, 10);
                double grandTotal = 0;
                boolean pair = false;

                for (LigneAchat l : achat.getLignesAchat()) {
                    if (pair) {
                        drawFilledRect(cs, MARGIN, y - 5, PAGE_WIDTH - 2 * MARGIN, 16, Color.decode("#F8F9FA"));
                    }
                    String nomProd = l.getProduit().getNom();
                    if (nomProd.length() > 30) nomProd = nomProd.substring(0, 30) + "...";
                    double sousTotal = l.getQuantiteAchetee() * l.getPrixUnitaire();
                    grandTotal += sousTotal;

                    cs.setNonStrokingColor(Color.BLACK);
                    drawText(cs, nomProd, MARGIN + 5, y);
                    drawText(cs, l.getLot().getNumeroLot(), MARGIN + 220, y);
                    drawText(cs, String.valueOf(l.getQuantiteAchetee()), MARGIN + 310, y);
                    drawText(cs, String.format("%.2f", l.getPrixUnitaire()), MARGIN + 360, y);
                    drawText(cs, String.format("%.2f FCFA", sousTotal), MARGIN + 420, y);
                    y -= 18;
                    pair = !pair;
                }

                // -------- TOTAL --------
                y -= 10;
                cs.setLineWidth(1f);
                cs.moveTo(MARGIN, y + 8);
                cs.lineTo(PAGE_WIDTH - MARGIN, y + 8);
                cs.stroke();
                y -= 5;

                cs.setFont(fontBold, 12);
                drawText(cs, "MONTANT TOTAL : " + String.format("%.2f FCFA", grandTotal), MARGIN + 280, y);
                y -= 40;

                // -------- SIGNATURE --------
                cs.setFont(fontNormal, 10);
                drawText(cs, "Signature Fournisseur :", MARGIN, y);
                drawText(cs, "Cachet Pharmacie :", MARGIN + 270, y);
                y -= 50;
                cs.setLineWidth(0.5f);
                cs.moveTo(MARGIN, y);
                cs.lineTo(MARGIN + 200, y);
                cs.stroke();
                cs.moveTo(MARGIN + 270, y);
                cs.lineTo(MARGIN + 470, y);
                cs.stroke();
            }

            document.save(fichier);
            logger.info("Bon de commande sauvegardé : {}", fichier.getAbsolutePath());

            // Ouvrir le PDF automatiquement
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(fichier);
            }

        } catch (IOException e) {
            logger.error("Erreur lors de la génération du PDF", e);
        }
    }

    private static void drawText(PDPageContentStream cs, String text, float x, float y) throws IOException {
        cs.beginText();
        cs.newLineAtOffset(x, y);
        cs.showText(text);
        cs.endText();
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

            PDType1Font fontBold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            PDType1Font fontNormal = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

            try (PDPageContentStream cs = new PDPageContentStream(document, page)) {
                float y = PAGE_HEIGHT - MARGIN;

                // En-tête
                y = drawProfessionalHeader(cs, "HISTORIQUE DES VENTES", "Récapitulatif" + (periode.isEmpty() ? "" : " — Période : " + periode), y);

                // En-tête tableau
                cs.setFont(fontBold, 9);
                drawFilledRect(cs, MARGIN, y - 5, PAGE_WIDTH - 2 * MARGIN, 18, Color.decode("#ECF0F1"));
                cs.setNonStrokingColor(Color.BLACK);
                drawText(cs, "ID", MARGIN + 5, y);
                drawText(cs, "DATE & HEURE", MARGIN + 35, y);
                drawText(cs, "AGENT", MARGIN + 145, y);
                drawText(cs, "NB PROD.", MARGIN + 250, y);
                drawText(cs, "MODE PAIEMENT", MARGIN + 310, y);
                drawText(cs, "TOTAL (FCFA)", MARGIN + 420, y);

                cs.setLineWidth(0.5f);
                cs.moveTo(MARGIN, y - 7);
                cs.lineTo(PAGE_WIDTH - MARGIN, y - 7);
                cs.stroke();
                y -= 22;

                // Lignes
                cs.setFont(fontNormal, 9);
                double grandTotal = 0;
                boolean pair = false;

                for (com.pharmacie.models.Vente v : ventes) {
                    if (y < MARGIN + 40) break; // Sécurité — page overflow simplifiée
                    if (pair) {
                        drawFilledRect(cs, MARGIN, y - 5, PAGE_WIDTH - 2 * MARGIN, 16, Color.decode("#F8F9FA"));
                    }
                    cs.setNonStrokingColor(Color.BLACK);
                    drawText(cs, String.valueOf(v.getId()), MARGIN + 5, y);
                    String dateStr = v.getDateVente() != null ? v.getDateVente().format(formatter) : "---";
                    drawText(cs, dateStr, MARGIN + 35, y);
                    String agent = v.getUser() != null ? v.getUser().getNom() : "---";
                    if (agent.length() > 13) agent = agent.substring(0, 13) + "...";
                    drawText(cs, agent, MARGIN + 145, y);
                    int nb = v.getLignesVente() != null ? v.getLignesVente().size() : 0;
                    drawText(cs, String.valueOf(nb), MARGIN + 270, y);
                    drawText(cs, v.getModePaiement() != null ? v.getModePaiement().name() : "", MARGIN + 310, y);
                    drawText(cs, String.format("%.0f", v.getTotal()), MARGIN + 430, y);
                    grandTotal += v.getTotal();
                    y -= 18;
                    pair = !pair;
                }

                // Grand Total
                y -= 10;
                cs.setLineWidth(1f);
                cs.moveTo(MARGIN, y + 8);
                cs.lineTo(PAGE_WIDTH - MARGIN, y + 8);
                cs.stroke();
                cs.setFont(fontBold, 12);
                drawText(cs, "CHIFFRE D'AFFAIRES TOTAL : " + String.format("%.0f FCFA", grandTotal), MARGIN + 220, y - 5);
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

            PDType1Font fontBold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            PDType1Font fontNormal = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

            try (PDPageContentStream cs = new PDPageContentStream(document, page)) {
                float y = PAGE_HEIGHT - MARGIN;

                // En-tête
                y = drawProfessionalHeader(cs, "HISTORIQUE DES APPROVISIONNEMENTS", "Récapitulatif" + (periode.isEmpty() ? "" : " — Période : " + periode), y);

                // En-tête tableau
                cs.setFont(fontBold, 9);
                drawFilledRect(cs, MARGIN, y - 5, PAGE_WIDTH - 2 * MARGIN, 18, Color.decode("#ECF0F1"));
                cs.setNonStrokingColor(Color.BLACK);
                drawText(cs, "ID", MARGIN + 5, y);
                drawText(cs, "DATE", MARGIN + 35, y);
                drawText(cs, "FOURNISSEUR", MARGIN + 110, y);
                drawText(cs, "REF FACTURE", MARGIN + 260, y);
                drawText(cs, "NB LIGNES", MARGIN + 370, y);
                drawText(cs, "MONTANT (FCFA)", MARGIN + 440, y);

                cs.setLineWidth(0.5f);
                cs.moveTo(MARGIN, y - 7);
                cs.lineTo(PAGE_WIDTH - MARGIN, y - 7);
                cs.stroke();
                y -= 22;

                // Lignes
                cs.setFont(fontNormal, 9);
                double grandTotal = 0;
                boolean pair = false;

                for (com.pharmacie.models.Achat a : achats) {
                    if (y < MARGIN + 40) break;
                    if (pair) {
                        drawFilledRect(cs, MARGIN, y - 5, PAGE_WIDTH - 2 * MARGIN, 16, Color.decode("#F8F9FA"));
                    }
                    cs.setNonStrokingColor(Color.BLACK);
                    drawText(cs, String.valueOf(a.getId()), MARGIN + 5, y);
                    String dateStr = a.getDateAchat() != null ? a.getDateAchat().toLocalDate().format(formatter) : "---";
                    drawText(cs, dateStr, MARGIN + 35, y);
                    String fournisseur = a.getFournisseur() != null ? a.getFournisseur().getNom() : "---";
                    if (fournisseur.length() > 18) fournisseur = fournisseur.substring(0, 18) + "...";
                    drawText(cs, fournisseur, MARGIN + 110, y);
                    String ref = a.getReferenceFacture() != null ? a.getReferenceFacture() : "---";
                    drawText(cs, ref, MARGIN + 260, y);
                    int nbLignes = a.getLignesAchat() != null ? a.getLignesAchat().size() : 0;
                    drawText(cs, String.valueOf(nbLignes), MARGIN + 390, y);
                    double montant = a.getLignesAchat() != null
                        ? a.getLignesAchat().stream().mapToDouble(la -> la.getQuantiteAchetee() * la.getPrixUnitaire()).sum()
                        : 0;
                    drawText(cs, String.format("%.0f", montant), MARGIN + 450, y);
                    grandTotal += montant;
                    y -= 18;
                    pair = !pair;
                }

                // Grand Total
                y -= 10;
                cs.setLineWidth(1f);
                cs.moveTo(MARGIN, y + 8);
                cs.lineTo(PAGE_WIDTH - MARGIN, y + 8);
                cs.stroke();
                cs.setFont(fontBold, 12);
                drawText(cs, "TOTAL DES ACHATS : " + String.format("%.0f FCFA", grandTotal), MARGIN + 250, y - 5);
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

            PDType1Font fontBold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            PDType1Font fontNormal = new PDType1Font(Standard14Fonts.FontName.HELVETICA);

            try (PDPageContentStream cs = new PDPageContentStream(document, page)) {
                float y = PAGE_HEIGHT - MARGIN;

                y = drawProfessionalHeader(cs, "ETAT DU STOCK PAR LOTS", "Inventaire Valorisé", y);

                cs.setFont(fontBold, 9);
                drawFilledRect(cs, MARGIN, y - 5, PAGE_WIDTH - 2 * MARGIN, 18, Color.decode("#ECF0F1"));
                cs.setNonStrokingColor(Color.BLACK);
                drawText(cs, "PRODUIT", MARGIN + 5, y);
                drawText(cs, "LOT", MARGIN + 90, y);
                drawText(cs, "EXPIR.", MARGIN + 150, y);
                drawText(cs, "JOURS", MARGIN + 205, y);
                drawText(cs, "STOCK", MARGIN + 245, y);
                drawText(cs, "SEUIL", MARGIN + 310, y);
                drawText(cs, "VENDU", MARGIN + 350, y);
                drawText(cs, "PRIX U.", MARGIN + 395, y);
                drawText(cs, "VALEUR", MARGIN + 445, y);

                cs.setLineWidth(0.5f);
                cs.moveTo(MARGIN, y - 7);
                cs.lineTo(PAGE_WIDTH - MARGIN, y - 7);
                cs.stroke();
                y -= 22;

                cs.setFont(fontNormal, 8); // Plus petit pour tout faire rentrer
                double valeurTotale = 0;
                boolean pair = false;

                for (com.pharmacie.controllers.ProduitController.EtatStockDTO s : stockList) {
                    if (y < MARGIN + 40) break;
                    if (pair) {
                        drawFilledRect(cs, MARGIN, y - 5, PAGE_WIDTH - 2 * MARGIN, 16, Color.decode("#F8F9FA"));
                    }
                    cs.setNonStrokingColor(Color.BLACK);
                    
                    String prod = s.getProduitNom();
                    if (prod.length() > 18) prod = prod.substring(0, 18) + ".";
                    drawText(cs, prod, MARGIN + 5, y);
                    
                    String lot = s.getLotNumero();
                    if (lot.length() > 10) lot = lot.substring(0, 10);
                    drawText(cs, lot, MARGIN + 90, y);
                    
                    drawText(cs, s.getDateExpiration(), MARGIN + 150, y);
                    
                    String jours = s.getJoursRestantsFormate().replace(" Expiré", " Exp.");
                    drawText(cs, jours, MARGIN + 205, y);

                    // Conversion intelligente "Bte(s) et Unités" pour le PDF
                    String qte = s.getQuantiteFormatee()
                        .replace(" Bte(s) et ", "B/").replace(" Unité(s)", "U")
                        .replace(" Bte(s)", "B").replace(" Unités", "U").replace(" Unité", "U");
                    drawText(cs, qte, MARGIN + 245, y);

                    drawText(cs, String.valueOf(s.getSeuilAlerte()), MARGIN + 315, y);
                    drawText(cs, String.valueOf(s.getQuantiteVendue()), MARGIN + 355, y);
                    
                    double prix = s.getPrixUnitaire() != null ? s.getPrixUnitaire() : 0;
                    drawText(cs, String.format("%.0f", prix), MARGIN + 395, y);
                    
                    double valeurLigne = s.getValeurFinanciere() != null ? s.getValeurFinanciere() : 0;
                    drawText(cs, String.format("%.0f", valeurLigne), MARGIN + 445, y);
                    
                    valeurTotale += valeurLigne;
                    y -= 14; // Lignes plus serrées pour police de 8
                    pair = !pair;
                }

                y -= 10;
                cs.setLineWidth(1f);
                cs.moveTo(MARGIN, y + 8);
                cs.lineTo(PAGE_WIDTH - MARGIN, y + 8);
                cs.stroke();
                cs.setFont(fontBold, 12);
                drawText(cs, "VALEUR TOTALE : " + String.format("%.0f FCFA", valeurTotale), MARGIN + 220, y - 5);
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

    public static void genererRapportAjustements(java.util.List<com.pharmacie.models.AjustementStock> ajustements, String periode, Stage ownerStage) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Enregistrer le rapport des pertes/ajustements");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Fichier PDF", "*.pdf"));
        fileChooser.setInitialFileName("Rapport_Ajustements_" + (periode.isEmpty() ? "tout" : periode.replace(" au ", "_").replace("/", "-")) + ".pdf");

        File fichier = fileChooser.showSaveDialog(ownerStage);
        if (fichier == null) return;

        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            PDType1Font fontBold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            PDType1Font fontNormal = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

            try (PDPageContentStream cs = new PDPageContentStream(document, page)) {
                float y = PAGE_HEIGHT - MARGIN;

                y = drawProfessionalHeader(cs, "HISTORIQUE DES RETRAITS EXCEPTIONNELS", "Période : " + periode, y);

                cs.setFont(fontBold, 9);
                drawFilledRect(cs, MARGIN, y - 5, PAGE_WIDTH - 2 * MARGIN, 18, Color.decode("#ECF0F1"));
                cs.setNonStrokingColor(Color.BLACK);
                drawText(cs, "DATE", MARGIN + 5, y);
                drawText(cs, "PRODUIT", MARGIN + 90, y);
                drawText(cs, "LOT", MARGIN + 220, y);
                drawText(cs, "MOTIF", MARGIN + 290, y);
                drawText(cs, "QTE", MARGIN + 400, y);
                drawText(cs, "AGENT", MARGIN + 440, y);

                cs.setLineWidth(0.5f);
                cs.moveTo(MARGIN, y - 7);
                cs.lineTo(PAGE_WIDTH - MARGIN, y - 7);
                cs.stroke();
                y -= 22;

                cs.setFont(fontNormal, 9);
                int totalPerte = 0;
                boolean pair = false;

                for (com.pharmacie.models.AjustementStock a : ajustements) {
                    if (y < MARGIN + 40) break;
                    if (pair) {
                        drawFilledRect(cs, MARGIN, y - 5, PAGE_WIDTH - 2 * MARGIN, 16, Color.decode("#F8F9FA"));
                    }
                    cs.setNonStrokingColor(Color.BLACK);
                    
                    String dateStr = a.getDateAjustement() != null ? a.getDateAjustement().format(formatter) : "---";
                    drawText(cs, dateStr, MARGIN + 5, y);
                    
                    String prod = a.getLot().getProduit().getNom();
                    if (prod.length() > 22) prod = prod.substring(0, 22) + "...";
                    drawText(cs, prod, MARGIN + 90, y);
                    
                    drawText(cs, a.getLot().getNumeroLot(), MARGIN + 220, y);
                    
                    String motif = a.getMotif().name();
                    if (motif.length() > 20) motif = motif.substring(0, 20);
                    drawText(cs, motif, MARGIN + 290, y);
                    
                    drawText(cs, String.valueOf(a.getQuantite()), MARGIN + 405, y);
                    
                    String user = a.getUser().getNom();
                    if (user.length() > 10) user = user.substring(0, 10) + "...";
                    drawText(cs, user, MARGIN + 440, y);
                    
                    totalPerte += a.getQuantite();
                    y -= 18;
                    pair = !pair;
                }

                y -= 10;
                cs.setLineWidth(1f);
                cs.moveTo(MARGIN, y + 8);
                cs.lineTo(PAGE_WIDTH - MARGIN, y + 8);
                cs.stroke();
                cs.setFont(fontBold, 12);
                drawText(cs, "TOTAL DES UNITES RETIREES / PERDUES : " + totalPerte, MARGIN + 220, y - 5);
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

            PDType1Font fontBold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            PDType1Font fontNormal = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

            try (PDPageContentStream cs = new PDPageContentStream(document, page)) {
                float y = PAGE_HEIGHT - MARGIN;

                y = drawProfessionalHeader(cs, "LIGNES DE VENTE", "Journal Détaillé — Période : " + periode, y);

                cs.setFont(fontBold, 9);
                drawFilledRect(cs, MARGIN, y - 5, PAGE_WIDTH - 2 * MARGIN, 18, Color.decode("#ECF0F1"));
                cs.setNonStrokingColor(Color.BLACK);
                drawText(cs, "DATE", MARGIN + 5, y);
                drawText(cs, "TICKET", MARGIN + 85, y);
                drawText(cs, "PRODUIT", MARGIN + 140, y);
                drawText(cs, "CAISSIER", MARGIN + 280, y);
                drawText(cs, "QTE", MARGIN + 360, y);
                drawText(cs, "TOTAL", MARGIN + 410, y);

                cs.setLineWidth(0.5f);
                cs.moveTo(MARGIN, y - 7);
                cs.lineTo(PAGE_WIDTH - MARGIN, y - 7);
                cs.stroke();
                y -= 22;

                cs.setFont(fontNormal, 9);
                double grandTotal = 0;
                boolean pair = false;

                for (com.pharmacie.models.LigneVente lv : lignes) {
                    if (y < MARGIN + 40) break;
                    if (pair) {
                        drawFilledRect(cs, MARGIN, y - 5, PAGE_WIDTH - 2 * MARGIN, 16, Color.decode("#F8F9FA"));
                    }
                    cs.setNonStrokingColor(Color.BLACK);
                    
                    String dateStr = lv.getVente().getDateVente() != null ? lv.getVente().getDateVente().format(formatter) : "---";
                    drawText(cs, dateStr, MARGIN + 5, y);
                    
                    drawText(cs, "TCK-" + lv.getVente().getId(), MARGIN + 85, y);
                    
                    String prod = lv.getProduit().getNom();
                    if (prod.length() > 25) prod = prod.substring(0, 25) + "...";
                    drawText(cs, prod, MARGIN + 140, y);
                    
                    String agent = lv.getVente().getUser().getNom();
                    if (agent.length() > 12) agent = agent.substring(0, 12) + "...";
                    drawText(cs, agent, MARGIN + 280, y);
                    
                    drawText(cs, String.valueOf(lv.getQuantiteVendue()), MARGIN + 365, y);
                    drawText(cs, String.format("%.0f", lv.getSousTotal()), MARGIN + 410, y);
                    
                    grandTotal += lv.getSousTotal();
                    y -= 18;
                    pair = !pair;
                }

                y -= 10;
                cs.setLineWidth(1f);
                cs.moveTo(MARGIN, y + 8);
                cs.lineTo(PAGE_WIDTH - MARGIN, y + 8);
                cs.stroke();
                cs.setFont(fontBold, 12);
                drawText(cs, "C.A. DE LA SELECTION : " + String.format("%.0f FCFA", grandTotal), MARGIN + 220, y - 5);
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

            PDType1Font fontBold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            PDType1Font fontNormal = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

            try (PDPageContentStream cs = new PDPageContentStream(document, page)) {
                float y = PAGE_HEIGHT - MARGIN;

                y = drawProfessionalHeader(cs, "AUDIT DES STOCKS", "Période : " + periode, y);

                cs.setFont(fontBold, 9);
                drawFilledRect(cs, MARGIN, y - 5, PAGE_WIDTH - 2 * MARGIN, 18, Color.decode("#ECF0F1"));
                cs.setNonStrokingColor(Color.BLACK);
                drawText(cs, "DATE", MARGIN + 5, y);
                drawText(cs, "TYPE", MARGIN + 80, y);
                drawText(cs, "PRODUIT / LOT", MARGIN + 140, y);
                drawText(cs, "QTE", MARGIN + 300, y);
                drawText(cs, "REFERENCE", MARGIN + 350, y);
                drawText(cs, "AGENT", MARGIN + 450, y);

                cs.setLineWidth(0.5f);
                cs.moveTo(MARGIN, y - 7);
                cs.lineTo(PAGE_WIDTH - MARGIN, y - 7);
                cs.stroke();
                y -= 22;

                cs.setFont(fontNormal, 9);
                boolean pair = false;

                for (com.pharmacie.models.MouvementStock m : mouvements) {
                    if (y < MARGIN + 40) break;
                    if (pair) drawFilledRect(cs, MARGIN, y - 5, PAGE_WIDTH - 2 * MARGIN, 16, Color.decode("#F8F9FA"));
                    cs.setNonStrokingColor(Color.BLACK);
                    
                    drawText(cs, m.getDateMouvement().format(formatter), MARGIN + 5, y);
                    
                    if (m.getTypeMouvement() == com.pharmacie.models.MouvementStock.TypeMouvement.VENTE) {
                        cs.setNonStrokingColor(Color.decode("#27ae60"));
                    } else {
                        cs.setNonStrokingColor(Color.decode("#c0392b"));
                    }
                    drawText(cs, m.getTypeMouvement().name(), MARGIN + 80, y);
                    
                    cs.setNonStrokingColor(Color.BLACK);
                    String prod = m.getProduit().getNom();
                    if (m.getLot() != null) prod += " (L: " + m.getLot().getNumeroLot() + ")";
                    if (prod.length() > 28) prod = prod.substring(0, 25) + "...";
                    drawText(cs, prod, MARGIN + 140, y);
                    
                    String prefix = m.getQuantite() > 0 ? "+ " : "- ";
                    drawText(cs, prefix + Math.abs(m.getQuantite()), MARGIN + 300, y);
                    
                    String ref = m.getReference() != null ? m.getReference() : "N/A";
                    if (ref.length() > 18) ref = ref.substring(0, 15) + "...";
                    drawText(cs, ref, MARGIN + 350, y);
                    
                    String user = m.getUser().getNom();
                    if (user.length() > 10) user = user.substring(0, 10) + "...";
                    drawText(cs, user, MARGIN + 450, y);
                    
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

            PDType1Font fontBold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            PDType1Font fontNormal = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yy HH:mm");

            try (PDPageContentStream cs = new PDPageContentStream(document, page)) {
                float y = PAGE_HEIGHT - MARGIN;

                y = drawProfessionalHeader(cs, "SESSIONS DE CAISSE (CLOTURES Z)", "Période : " + periode, y);

                cs.setFont(fontBold, 9);
                drawFilledRect(cs, MARGIN, y - 5, PAGE_WIDTH - 2 * MARGIN, 18, Color.decode("#ECF0F1"));
                cs.setNonStrokingColor(Color.BLACK);
                drawText(cs, "OUVERTURE", MARGIN + 5, y);
                drawText(cs, "CLOTURE", MARGIN + 85, y);
                drawText(cs, "AGENT", MARGIN + 165, y);
                drawText(cs, "STATUT", MARGIN + 250, y);
                drawText(cs, "ECART ESPECES", MARGIN + 320, y);
                drawText(cs, "ECART MOBILE", MARGIN + 430, y);

                cs.setLineWidth(0.5f);
                cs.moveTo(MARGIN, y - 7);
                cs.lineTo(PAGE_WIDTH - MARGIN, y - 7);
                cs.stroke();
                y -= 22;

                cs.setFont(fontNormal, 9);
                boolean pair = false;

                for (com.pharmacie.models.SessionCaisse s : sessions) {
                    if (y < MARGIN + 40) break;
                    if (pair) drawFilledRect(cs, MARGIN, y - 5, PAGE_WIDTH - 2 * MARGIN, 16, Color.decode("#F8F9FA"));
                    cs.setNonStrokingColor(Color.BLACK);
                    
                    drawText(cs, s.getDateOuverture().format(formatter), MARGIN + 5, y);
                    drawText(cs, s.getDateCloture() != null ? s.getDateCloture().format(formatter) : "En cours", MARGIN + 85, y);
                    
                    String user = s.getUser().getNom();
                    if (user.length() > 15) user = user.substring(0, 12) + "...";
                    drawText(cs, user, MARGIN + 165, y);
                    
                    drawText(cs, s.getStatut().name(), MARGIN + 250, y);
                    
                    // Couleur des écarts
                    double ecartEsp = s.getEcartEspeces() != null ? s.getEcartEspeces() : 0.0;
                    if (ecartEsp < 0) cs.setNonStrokingColor(Color.decode("#c0392b")); // Rouge
                    else if (ecartEsp > 0) cs.setNonStrokingColor(Color.decode("#27ae60")); // Vert
                    drawText(cs, String.format("%.0f F", ecartEsp), MARGIN + 320, y);
                    
                    cs.setNonStrokingColor(Color.BLACK);
                    double ecartMob = s.getEcartMobile() != null ? s.getEcartMobile() : 0.0;
                    if (ecartMob < 0) cs.setNonStrokingColor(Color.decode("#c0392b")); // Rouge
                    else if (ecartMob > 0) cs.setNonStrokingColor(Color.decode("#27ae60")); // Vert
                    drawText(cs, String.format("%.0f F", ecartMob), MARGIN + 430, y);
                    
                    y -= 18;
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

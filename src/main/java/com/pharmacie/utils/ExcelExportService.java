package com.pharmacie.utils;

import com.pharmacie.controllers.ProduitController.EtatStockDTO;
import com.pharmacie.models.Achat;
import com.pharmacie.models.LigneVente;
import com.pharmacie.models.MouvementStock;
import com.pharmacie.models.SessionCaisse;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Desktop;
import java.io.File;
import java.io.FileOutputStream;
import java.time.LocalDate;
import java.util.List;

public class ExcelExportService {

    private static final Logger logger = LoggerFactory.getLogger(ExcelExportService.class);

    public static void genererEtatStockExcel(List<EtatStockDTO> donnees, Stage ownerStage) {
        if (donnees == null || donnees.isEmpty()) return;

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Enregistrer l'État des Stocks (Excel Premium)");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Classeur Excel", "*.xlsx"));
        fileChooser.setInitialFileName("Etat_Stock_Premium_" + LocalDate.now() + ".xlsx");

        File fichier = fileChooser.showSaveDialog(ownerStage);
        if (fichier == null) return;

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("État des Stocks");

            // 1. Définition des Styles Premium
            
            // Style En-tête : Fond Vert Émeraude (#059669), Texte Blanc, Gras
            CellStyle headerStyle = workbook.createCellStyle();
            byte[] rgb = new byte[]{(byte) 5, (byte) 150, (byte) 105}; // #059669
            XSSFColor emeraldColor = new XSSFColor(rgb, null);
            ((org.apache.poi.xssf.usermodel.XSSFCellStyle) headerStyle).setFillForegroundColor(emeraldColor);
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            
            Font headerFont = workbook.createFont();
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerFont.setBold(true);
            headerFont.setFontName("Arial");
            headerFont.setFontHeightInPoints((short) 11);
            headerStyle.setFont(headerFont);
            headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);

            // Style Numérique (Séparateur de milliers)
            CellStyle numberStyle = workbook.createCellStyle();
            DataFormat format = workbook.createDataFormat();
            numberStyle.setDataFormat(format.getFormat("#,##0"));
            
            // Style Date
            CellStyle dateStyle = workbook.createCellStyle();
            dateStyle.setDataFormat(format.getFormat("dd/mm/yyyy"));

            // 2. Création de l'En-tête
            String[] colonnes = {
                "Produit", "Catégorie", "N° Lot", "Expiration", 
                "Jours Restants", "En Stock", "Seuil Alerte", "Prix Unitaire", "Valeur Nette"
            };

            Row headerRow = sheet.createRow(0);
            headerRow.setHeightInPoints(25); // Hauteur aérée pour l'en-tête
            
            for (int i = 0; i < colonnes.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(colonnes[i]);
                cell.setCellStyle(headerStyle);
            }

            // Figer la ligne d'en-tête
            sheet.createFreezePane(0, 1);

            // 3. Remplissage des données
            int rowNum = 1;
            for (EtatStockDTO d : donnees) {
                Row row = sheet.createRow(rowNum++);
                
                row.createCell(0).setCellValue(d.getProduitNom() != null ? d.getProduitNom() : "");
                row.createCell(1).setCellValue(d.getCategorieNom() != null ? d.getCategorieNom() : "");
                row.createCell(2).setCellValue(d.getLotNumero() != null ? d.getLotNumero() : "");
                
                // Expiration (en texte car formaté dans DTO)
                row.createCell(3).setCellValue(d.getDateExpiration() != null ? d.getDateExpiration() : "");
                
                // Jours restants
                Cell cellJours = row.createCell(4);
                cellJours.setCellValue(d.getJoursRestants());
                cellJours.setCellStyle(numberStyle);

                // En Stock
                Cell cellStock = row.createCell(5);
                cellStock.setCellValue(d.getQuantiteStock() != null ? d.getQuantiteStock() : 0);
                cellStock.setCellStyle(numberStyle);

                // Seuil
                Cell cellSeuil = row.createCell(6);
                cellSeuil.setCellValue(d.getSeuilAlerte());
                cellSeuil.setCellStyle(numberStyle);

                // Prix U
                Cell cellPrix = row.createCell(7);
                cellPrix.setCellValue(d.getPrixUnitaire() != null ? d.getPrixUnitaire() : 0.0);
                cellPrix.setCellStyle(numberStyle);

                // Valeur Nette
                Cell cellValeur = row.createCell(8);
                cellValeur.setCellValue(d.getValeurFinanciere() != null ? d.getValeurFinanciere() : 0.0);
                cellValeur.setCellStyle(numberStyle);
            }

            // 4. Ligne de Totaux
            Row totalRow = sheet.createRow(rowNum);
            totalRow.setHeightInPoints(20);
            
            CellStyle totalStyle = workbook.createCellStyle();
            totalStyle.cloneStyleFrom(numberStyle);
            Font totalFont = workbook.createFont();
            totalFont.setBold(true);
            totalStyle.setFont(totalFont);
            
            Cell labelTotalCell = totalRow.createCell(7);
            labelTotalCell.setCellValue("TOTAL :");
            labelTotalCell.setCellStyle(totalStyle);

            Cell sumTotalCell = totalRow.createCell(8);
            // Formule Excel native (ex: =SUM(I2:I100))
            sumTotalCell.setCellFormula("SUM(I2:I" + rowNum + ")");
            sumTotalCell.setCellStyle(totalStyle);

            // 5. Ajustement des colonnes
            for (int i = 0; i < colonnes.length; i++) {
                sheet.autoSizeColumn(i);
                // Ajouter un petit padding
                int currentWidth = sheet.getColumnWidth(i);
                sheet.setColumnWidth(i, currentWidth + 1000);
            }

            // 6. Écriture du fichier
            try (FileOutputStream fileOut = new FileOutputStream(fichier)) {
                workbook.write(fileOut);
            }

            logger.info("Export Excel Premium sauvegardé : {}", fichier.getAbsolutePath());
            
            // Notification de succès et ouverture automatique
            ToastService.showSuccess(ownerStage, "Export Premium Réussi", "Fichier Excel généré avec succès !");
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(fichier);
            }

        } catch (Exception e) {
            logger.error("Erreur lors de l'export Excel Premium", e);
            AlertUtils.showPremiumAlert(javafx.scene.control.Alert.AlertType.ERROR, 
                "Erreur Export", "Échec de l'export", e.getMessage());
        }
    }

    public static void genererJournalVentesExcel(List<LigneVente> donnees, Stage ownerStage) {
        if (donnees == null || donnees.isEmpty()) return;

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Enregistrer le Journal des Ventes (Excel Premium)");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Classeur Excel", "*.xlsx"));
        fileChooser.setInitialFileName("Journal_Ventes_Premium_" + LocalDate.now() + ".xlsx");

        File fichier = fileChooser.showSaveDialog(ownerStage);
        if (fichier == null) return;

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Journal des Ventes");

            // 1. Définition des Styles Premium
            CellStyle headerStyle = workbook.createCellStyle();
            byte[] rgb = new byte[]{(byte) 5, (byte) 150, (byte) 105}; // #059669
            XSSFColor emeraldColor = new XSSFColor(rgb, null);
            ((org.apache.poi.xssf.usermodel.XSSFCellStyle) headerStyle).setFillForegroundColor(emeraldColor);
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            
            Font headerFont = workbook.createFont();
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerFont.setBold(true);
            headerFont.setFontName("Arial");
            headerFont.setFontHeightInPoints((short) 11);
            headerStyle.setFont(headerFont);
            headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);

            // Style Numérique
            CellStyle numberStyle = workbook.createCellStyle();
            DataFormat format = workbook.createDataFormat();
            numberStyle.setDataFormat(format.getFormat("#,##0"));
            
            // Style Date
            CellStyle dateStyle = workbook.createCellStyle();
            dateStyle.setDataFormat(format.getFormat("dd/mm/yyyy hh:mm"));

            // 2. Création de l'En-tête
            String[] colonnes = {
                "Date", "Ticket", "Agent (Caissier)", "Produit", 
                "Catégorie", "Espèce", "Qté", "Prix U.", "Sous-Total"
            };

            Row headerRow = sheet.createRow(0);
            headerRow.setHeightInPoints(25); 
            
            for (int i = 0; i < colonnes.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(colonnes[i]);
                cell.setCellStyle(headerStyle);
            }

            sheet.createFreezePane(0, 1);

            // 3. Remplissage des données
            int rowNum = 1;
            for (LigneVente lv : donnees) {
                Row row = sheet.createRow(rowNum++);
                
                // Date
                Cell cellDate = row.createCell(0);
                if (lv.getVente().getDateVente() != null) {
                    cellDate.setCellValue(lv.getVente().getDateVente());
                } else {
                    cellDate.setCellValue("");
                }
                cellDate.setCellStyle(dateStyle);

                row.createCell(1).setCellValue(lv.getVente().getNumeroTicketOfficiel());
                row.createCell(2).setCellValue(lv.getVente().getUser().getNom() != null ? lv.getVente().getUser().getNom() : "");
                row.createCell(3).setCellValue(lv.getProduit().getNom() != null ? lv.getProduit().getNom() : "");
                row.createCell(4).setCellValue(lv.getProduit().getCategorie().getNom() != null ? lv.getProduit().getCategorie().getNom() : "");
                row.createCell(5).setCellValue(lv.getProduit().getEspece().getNom() != null ? lv.getProduit().getEspece().getNom() : "");
                
                Cell cellQte = row.createCell(6);
                cellQte.setCellValue(lv.getQuantiteVendue());
                cellQte.setCellStyle(numberStyle);

                Cell cellPrix = row.createCell(7);
                cellPrix.setCellValue(lv.getPrixUnitaire() != null ? lv.getPrixUnitaire() : 0.0);
                cellPrix.setCellStyle(numberStyle);

                Cell cellTotal = row.createCell(8);
                cellTotal.setCellValue(lv.getSousTotal() != null ? lv.getSousTotal() : 0.0);
                cellTotal.setCellStyle(numberStyle);
            }

            // 4. Ligne de Totaux
            Row totalRow = sheet.createRow(rowNum);
            totalRow.setHeightInPoints(20);
            
            CellStyle totalStyle = workbook.createCellStyle();
            totalStyle.cloneStyleFrom(numberStyle);
            Font totalFont = workbook.createFont();
            totalFont.setBold(true);
            totalStyle.setFont(totalFont);
            
            Cell labelTotalCell = totalRow.createCell(7);
            labelTotalCell.setCellValue("CA TOTAL :");
            labelTotalCell.setCellStyle(totalStyle);

            Cell sumTotalCell = totalRow.createCell(8);
            // Formule Excel native (=SUM(I2:Ixxx))
            sumTotalCell.setCellFormula("SUM(I2:I" + rowNum + ")");
            sumTotalCell.setCellStyle(totalStyle);

            // 5. Ajustement des colonnes
            for (int i = 0; i < colonnes.length; i++) {
                sheet.autoSizeColumn(i);
                int currentWidth = sheet.getColumnWidth(i);
                sheet.setColumnWidth(i, currentWidth + 1000);
            }

            // 6. Écriture
            try (FileOutputStream fileOut = new FileOutputStream(fichier)) {
                workbook.write(fileOut);
            }

            logger.info("Export Journal Ventes Excel sauvegardé : {}", fichier.getAbsolutePath());
            ToastService.showSuccess(ownerStage, "Export Premium Réussi", "Journal des ventes généré avec succès !");
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(fichier);
            }

        } catch (Exception e) {
            logger.error("Erreur lors de l'export Journal Ventes Excel", e);
            AlertUtils.showPremiumAlert(javafx.scene.control.Alert.AlertType.ERROR, 
                "Erreur Export", "Échec de l'export", e.getMessage());
        }
    }

    public static void genererAuditStocksExcel(List<MouvementStock> mouvements, String periodeLabel, Stage ownerStage) {
        if (mouvements == null || mouvements.isEmpty()) return;

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Enregistrer l'Audit des Stocks (Excel Premium)");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Classeur Excel", "*.xlsx"));
        
        // Nom du fichier avec la période demandée
        String nomFichier = "Audit_Stocks_" + (periodeLabel.isEmpty() ? LocalDate.now().toString() : periodeLabel.replace(" ", "_")) + ".xlsx";
        fileChooser.setInitialFileName(nomFichier);

        File fichier = fileChooser.showSaveDialog(ownerStage);
        if (fichier == null) return;

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Audit des Stocks");

            // 1. Définition des Styles Premium
            CellStyle headerStyle = workbook.createCellStyle();
            byte[] rgb = new byte[]{(byte) 5, (byte) 150, (byte) 105}; // #059669
            XSSFColor emeraldColor = new XSSFColor(rgb, null);
            ((org.apache.poi.xssf.usermodel.XSSFCellStyle) headerStyle).setFillForegroundColor(emeraldColor);
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            
            Font headerFont = workbook.createFont();
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerFont.setBold(true);
            headerFont.setFontName("Arial");
            headerFont.setFontHeightInPoints((short) 11);
            headerStyle.setFont(headerFont);
            headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);

            // Style Numérique
            CellStyle numberStyle = workbook.createCellStyle();
            DataFormat format = workbook.createDataFormat();
            numberStyle.setDataFormat(format.getFormat("#,##0"));
            
            // Style Numérique Positif (Ajout)
            CellStyle positiveStyle = workbook.createCellStyle();
            positiveStyle.cloneStyleFrom(numberStyle);
            Font positiveFont = workbook.createFont();
            positiveFont.setColor(IndexedColors.GREEN.getIndex());
            positiveStyle.setFont(positiveFont);

            // Style Numérique Négatif (Retrait)
            CellStyle negativeStyle = workbook.createCellStyle();
            negativeStyle.cloneStyleFrom(numberStyle);
            Font negativeFont = workbook.createFont();
            negativeFont.setColor(IndexedColors.RED.getIndex());
            negativeStyle.setFont(negativeFont);

            // Style Date
            CellStyle dateStyle = workbook.createCellStyle();
            dateStyle.setDataFormat(format.getFormat("dd/mm/yyyy hh:mm"));

            // 2. Création de l'En-tête
            String[] colonnes = {
                "Date", "Type", "Produit", "Lot", "Qté", "Référence", "Agent"
            };

            Row headerRow = sheet.createRow(0);
            headerRow.setHeightInPoints(25); 
            
            for (int i = 0; i < colonnes.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(colonnes[i]);
                cell.setCellStyle(headerStyle);
            }

            sheet.createFreezePane(0, 1);

            // 3. Remplissage des données
            int rowNum = 1;
            for (MouvementStock m : mouvements) {
                Row row = sheet.createRow(rowNum++);
                
                // Date
                Cell cellDate = row.createCell(0);
                if (m.getDateMouvement() != null) {
                    cellDate.setCellValue(m.getDateMouvement());
                } else {
                    cellDate.setCellValue("");
                }
                cellDate.setCellStyle(dateStyle);

                // Type
                String typeLabel;
                switch (m.getTypeMouvement()) {
                    case VENTE:               typeLabel = "Vente"; break;
                    case ACHAT:               typeLabel = "Achat"; break;
                    case AJUSTEMENT_POSITIF:  typeLabel = "Ajout"; break;
                    case AJUSTEMENT_NEGATIF:  typeLabel = "Retrait"; break;
                    default:                  typeLabel = m.getTypeMouvement().name(); break;
                }
                row.createCell(1).setCellValue(typeLabel);

                // Produit
                row.createCell(2).setCellValue(m.getProduit() != null ? m.getProduit().getNom() : "");
                
                // Lot
                row.createCell(3).setCellValue(m.getLot() != null ? m.getLot().getNumeroLot() : "N/A");

                // Quantité
                Cell cellQte = row.createCell(4);
                cellQte.setCellValue(m.getQuantite());
                if (m.getQuantite() > 0) {
                    cellQte.setCellStyle(positiveStyle);
                } else if (m.getQuantite() < 0) {
                    cellQte.setCellStyle(negativeStyle);
                } else {
                    cellQte.setCellStyle(numberStyle);
                }

                // Référence : tiret pour les ajustements
                String ref;
                switch (m.getTypeMouvement()) {
                    case AJUSTEMENT_POSITIF:
                    case AJUSTEMENT_NEGATIF:
                        ref = "\u2014"; // Tiret long
                        break;
                    default:
                        ref = m.getReference() != null ? m.getReference() : "N/A";
                        break;
                }
                row.createCell(5).setCellValue(ref);

                // Agent
                row.createCell(6).setCellValue(m.getUser() != null ? m.getUser().getNom() : "");
            }

            // 4. Ajustement des colonnes
            for (int i = 0; i < colonnes.length; i++) {
                sheet.autoSizeColumn(i);
                int currentWidth = sheet.getColumnWidth(i);
                sheet.setColumnWidth(i, currentWidth + 1000);
            }

            // 5. Écriture
            try (FileOutputStream fileOut = new FileOutputStream(fichier)) {
                workbook.write(fileOut);
            }

            logger.info("Export Audit Stocks Excel sauvegardé : {}", fichier.getAbsolutePath());
            ToastService.showSuccess(ownerStage, "Export Premium Réussi", "Audit des stocks généré avec succès !");
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(fichier);
            }

        } catch (Exception e) {
            logger.error("Erreur lors de l'export Audit Stocks Excel", e);
            AlertUtils.showPremiumAlert(javafx.scene.control.Alert.AlertType.ERROR, 
                "Erreur Export", "Échec de l'export", e.getMessage());
        }
    }

    public static void genererHistoriqueAchatsExcel(List<Achat> achats, String periodeLabel, Stage ownerStage) {
        if (achats == null || achats.isEmpty()) return;

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Enregistrer l'Historique des Achats (Excel Premium)");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Classeur Excel", "*.xlsx"));
        
        String nomFichier = "Historique_Achats_" + (periodeLabel.isEmpty() ? LocalDate.now().toString() : periodeLabel.replace(" ", "_")) + ".xlsx";
        fileChooser.setInitialFileName(nomFichier);

        File fichier = fileChooser.showSaveDialog(ownerStage);
        if (fichier == null) return;

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Historique des Achats");

            // 1. Définition des Styles Premium
            CellStyle headerStyle = workbook.createCellStyle();
            byte[] rgb = new byte[]{(byte) 5, (byte) 150, (byte) 105}; // #059669
            XSSFColor emeraldColor = new XSSFColor(rgb, null);
            ((org.apache.poi.xssf.usermodel.XSSFCellStyle) headerStyle).setFillForegroundColor(emeraldColor);
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            
            Font headerFont = workbook.createFont();
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerFont.setBold(true);
            headerFont.setFontName("Arial");
            headerFont.setFontHeightInPoints((short) 11);
            headerStyle.setFont(headerFont);
            headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);

            CellStyle numberStyle = workbook.createCellStyle();
            DataFormat format = workbook.createDataFormat();
            numberStyle.setDataFormat(format.getFormat("#,##0"));

            CellStyle dateStyle = workbook.createCellStyle();
            dateStyle.setDataFormat(format.getFormat("dd/mm/yyyy hh:mm"));

            // 2. Création de l'En-tête
            String[] colonnes = {
                "Date", "N° Facture", "Fournisseur", "Nb Produits", "Total TTC"
            };

            Row headerRow = sheet.createRow(0);
            headerRow.setHeightInPoints(25); 
            
            for (int i = 0; i < colonnes.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(colonnes[i]);
                cell.setCellStyle(headerStyle);
            }

            sheet.createFreezePane(0, 1);

            // 3. Remplissage des données
            int rowNum = 1;
            for (Achat a : achats) {
                Row row = sheet.createRow(rowNum++);
                
                // Date
                Cell cellDate = row.createCell(0);
                if (a.getDateAchat() != null) {
                    cellDate.setCellValue(a.getDateAchat());
                } else {
                    cellDate.setCellValue("");
                }
                cellDate.setCellStyle(dateStyle);

                // N° Facture
                row.createCell(1).setCellValue(a.getReferenceFacture() != null && !a.getReferenceFacture().isEmpty() ? a.getReferenceFacture() : "—");
                
                // Fournisseur
                row.createCell(2).setCellValue(a.getFournisseur() != null ? a.getFournisseur().getNom() : "");

                // Calculs
                int nbProduits = 0;
                double totalTTC = 0.0;
                if (a.getLignesAchat() != null) {
                    nbProduits = a.getLignesAchat().size();
                    for (com.pharmacie.models.LigneAchat la : a.getLignesAchat()) {
                        totalTTC += (la.getPrixUnitaire() * la.getQuantiteAchetee());
                    }
                }

                // Nb Produits
                Cell cellNb = row.createCell(3);
                cellNb.setCellValue(nbProduits);
                cellNb.setCellStyle(numberStyle);

                // Total TTC
                Cell cellTotal = row.createCell(4);
                cellTotal.setCellValue(totalTTC);
                cellTotal.setCellStyle(numberStyle);
            }

            // 4. Ligne de Totaux
            Row totalRow = sheet.createRow(rowNum);
            totalRow.setHeightInPoints(20);
            
            CellStyle totalStyle = workbook.createCellStyle();
            totalStyle.cloneStyleFrom(numberStyle);
            Font totalFont = workbook.createFont();
            totalFont.setBold(true);
            totalStyle.setFont(totalFont);
            
            Cell labelTotalCell = totalRow.createCell(3);
            labelTotalCell.setCellValue("TOTAL DES ACHATS :");
            labelTotalCell.setCellStyle(totalStyle);

            Cell sumTotalCell = totalRow.createCell(4);
            sumTotalCell.setCellFormula("SUM(E2:E" + rowNum + ")");
            sumTotalCell.setCellStyle(totalStyle);

            // 5. Ajustement des colonnes
            for (int i = 0; i < colonnes.length; i++) {
                sheet.autoSizeColumn(i);
                int currentWidth = sheet.getColumnWidth(i);
                sheet.setColumnWidth(i, currentWidth + 1000);
            }

            // 6. Écriture
            try (FileOutputStream fileOut = new FileOutputStream(fichier)) {
                workbook.write(fileOut);
            }

            logger.info("Export Achats Excel sauvegardé : {}", fichier.getAbsolutePath());
            ToastService.showSuccess(ownerStage, "Export Premium Réussi", "Historique des Achats généré avec succès !");
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(fichier);
            }

        } catch (Exception e) {
            logger.error("Erreur lors de l'export Achats Excel", e);
            AlertUtils.showPremiumAlert(javafx.scene.control.Alert.AlertType.ERROR, 
                "Erreur Export", "Échec de l'export", e.getMessage());
        }
    }

    public static void genererCloturesCaisseExcel(List<SessionCaisse> clotures, String periodeLabel, Stage ownerStage) {
        if (clotures == null || clotures.isEmpty()) return;

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Enregistrer les Clôtures de Caisse (Excel Premium)");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Classeur Excel", "*.xlsx"));
        
        String nomFichier = "Clotures_Caisse_" + (periodeLabel.isEmpty() ? LocalDate.now().toString() : periodeLabel.replace(" ", "_")) + ".xlsx";
        fileChooser.setInitialFileName(nomFichier);

        File fichier = fileChooser.showSaveDialog(ownerStage);
        if (fichier == null) return;

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Clôtures de Caisse");

            // 1. Définition des Styles Premium
            CellStyle headerStyle = workbook.createCellStyle();
            byte[] rgb = new byte[]{(byte) 5, (byte) 150, (byte) 105}; // #059669
            XSSFColor emeraldColor = new XSSFColor(rgb, null);
            ((org.apache.poi.xssf.usermodel.XSSFCellStyle) headerStyle).setFillForegroundColor(emeraldColor);
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            
            Font headerFont = workbook.createFont();
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerFont.setBold(true);
            headerFont.setFontName("Arial");
            headerFont.setFontHeightInPoints((short) 11);
            headerStyle.setFont(headerFont);
            headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);

            CellStyle numberStyle = workbook.createCellStyle();
            DataFormat format = workbook.createDataFormat();
            numberStyle.setDataFormat(format.getFormat("#,##0"));

            CellStyle positiveStyle = workbook.createCellStyle();
            positiveStyle.cloneStyleFrom(numberStyle);
            Font positiveFont = workbook.createFont();
            positiveFont.setColor(IndexedColors.GREEN.getIndex());
            positiveStyle.setFont(positiveFont);

            CellStyle negativeStyle = workbook.createCellStyle();
            negativeStyle.cloneStyleFrom(numberStyle);
            Font negativeFont = workbook.createFont();
            negativeFont.setColor(IndexedColors.RED.getIndex());
            negativeStyle.setFont(negativeFont);

            CellStyle dateStyle = workbook.createCellStyle();
            dateStyle.setDataFormat(format.getFormat("dd/mm/yyyy hh:mm"));

            // 2. Création de l'En-tête
            String[] colonnes = {
                "Agent", "Ouverture", "Clôture", "Fonds Initial", "CA Théorique", "Espèces Déclarées", "Écart Espèces", "Statut"
            };

            Row headerRow = sheet.createRow(0);
            headerRow.setHeightInPoints(25); 
            
            for (int i = 0; i < colonnes.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(colonnes[i]);
                cell.setCellStyle(headerStyle);
            }

            sheet.createFreezePane(0, 1);

            // 3. Remplissage des données
            int rowNum = 1;
            for (SessionCaisse s : clotures) {
                Row row = sheet.createRow(rowNum++);
                
                // Agent
                row.createCell(0).setCellValue(s.getUser() != null ? s.getUser().getNom() : "");

                // Ouverture
                Cell cellOuv = row.createCell(1);
                if (s.getDateOuverture() != null) cellOuv.setCellValue(s.getDateOuverture());
                cellOuv.setCellStyle(dateStyle);

                // Clôture
                Cell cellClo = row.createCell(2);
                if (s.getDateCloture() != null) {
                    cellClo.setCellValue(s.getDateCloture());
                    cellClo.setCellStyle(dateStyle);
                } else {
                    cellClo.setCellValue("En cours...");
                }

                // Fonds Initial
                Cell cellFond = row.createCell(3);
                cellFond.setCellValue(s.getFondDeCaisse() != null ? s.getFondDeCaisse() : 0.0);
                cellFond.setCellStyle(numberStyle);

                // CA Théorique
                Cell cellCA = row.createCell(4);
                cellCA.setCellValue(s.getTotalEspecesAttendu() != null ? s.getTotalEspecesAttendu() : 0.0);
                cellCA.setCellStyle(numberStyle);

                // Espèces Déclarées
                Cell cellDec = row.createCell(5);
                cellDec.setCellValue(s.getEspecesDeclare() != null ? s.getEspecesDeclare() : 0.0);
                cellDec.setCellStyle(numberStyle);

                // Écart Espèces
                Cell cellEcart = row.createCell(6);
                double ecart = s.getEcartEspeces() != null ? s.getEcartEspeces() : 0.0;
                cellEcart.setCellValue(ecart);
                if (ecart > 0) {
                    cellEcart.setCellStyle(positiveStyle);
                } else if (ecart < 0) {
                    cellEcart.setCellStyle(negativeStyle);
                } else {
                    cellEcart.setCellStyle(numberStyle);
                }

                // Statut
                row.createCell(7).setCellValue(s.getStatut() != null ? s.getStatut().name() : "");
            }

            // 4. Ligne de Totaux pour les écarts
            Row totalRow = sheet.createRow(rowNum);
            totalRow.setHeightInPoints(20);
            
            CellStyle totalStyle = workbook.createCellStyle();
            totalStyle.cloneStyleFrom(numberStyle);
            Font totalFont = workbook.createFont();
            totalFont.setBold(true);
            totalStyle.setFont(totalFont);
            
            Cell labelTotalCell = totalRow.createCell(5);
            labelTotalCell.setCellValue("TOTAL ÉCARTS :");
            labelTotalCell.setCellStyle(totalStyle);

            Cell sumTotalCell = totalRow.createCell(6);
            sumTotalCell.setCellFormula("SUM(G2:G" + rowNum + ")");
            sumTotalCell.setCellStyle(totalStyle);

            // 5. Ajustement des colonnes
            for (int i = 0; i < colonnes.length; i++) {
                sheet.autoSizeColumn(i);
                int currentWidth = sheet.getColumnWidth(i);
                sheet.setColumnWidth(i, currentWidth + 1000);
            }

            // 6. Écriture
            try (FileOutputStream fileOut = new FileOutputStream(fichier)) {
                workbook.write(fileOut);
            }

            logger.info("Export Clôtures Excel sauvegardé : {}", fichier.getAbsolutePath());
            ToastService.showSuccess(ownerStage, "Export Premium Réussi", "Rapport de Clôture généré avec succès !");
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(fichier);
            }

        } catch (Exception e) {
            logger.error("Erreur lors de l'export Clôtures Excel", e);
            AlertUtils.showPremiumAlert(javafx.scene.control.Alert.AlertType.ERROR, 
                "Erreur Export", "Échec de l'export", e.getMessage());
        }
    }

    public static void genererRapportAjustementsExcel(List<com.pharmacie.models.AjustementStock> ajustements, String periodeLabel, MouvementStock.TypeMouvement typeOp, Stage ownerStage) {
        if (ajustements == null || ajustements.isEmpty()) return;

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Enregistrer l'Historique des Ajustements (Excel Premium)");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Classeur Excel", "*.xlsx"));
        
        String typeNom = typeOp != null ? typeOp.name() : "Tous";
        String nomFichier = "Ajustements_Stocks_" + typeNom + "_" + (periodeLabel.isEmpty() ? LocalDate.now().toString() : periodeLabel.replace(" ", "_")) + ".xlsx";
        fileChooser.setInitialFileName(nomFichier);

        File fichier = fileChooser.showSaveDialog(ownerStage);
        if (fichier == null) return;

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Ajustements");

            // 1. Définition des Styles Premium
            CellStyle headerStyle = workbook.createCellStyle();
            byte[] rgb = new byte[]{(byte) 5, (byte) 150, (byte) 105}; // #059669
            XSSFColor emeraldColor = new XSSFColor(rgb, null);
            ((org.apache.poi.xssf.usermodel.XSSFCellStyle) headerStyle).setFillForegroundColor(emeraldColor);
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            
            Font headerFont = workbook.createFont();
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerFont.setBold(true);
            headerFont.setFontName("Arial");
            headerFont.setFontHeightInPoints((short) 11);
            headerStyle.setFont(headerFont);
            headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);

            CellStyle numberStyle = workbook.createCellStyle();
            DataFormat format = workbook.createDataFormat();
            numberStyle.setDataFormat(format.getFormat("#,##0"));

            CellStyle positiveStyle = workbook.createCellStyle();
            positiveStyle.cloneStyleFrom(numberStyle);
            Font positiveFont = workbook.createFont();
            positiveFont.setColor(IndexedColors.GREEN.getIndex());
            positiveStyle.setFont(positiveFont);

            CellStyle negativeStyle = workbook.createCellStyle();
            negativeStyle.cloneStyleFrom(numberStyle);
            Font negativeFont = workbook.createFont();
            negativeFont.setColor(IndexedColors.RED.getIndex());
            negativeStyle.setFont(negativeFont);

            CellStyle dateStyle = workbook.createCellStyle();
            dateStyle.setDataFormat(format.getFormat("dd/mm/yyyy hh:mm"));

            // 2. Création de l'En-tête
            String[] colonnes = {
                "Date", "Produit", "Lot", "Type", "Motif", "Qté", "Agent", "Observations"
            };

            Row headerRow = sheet.createRow(0);
            headerRow.setHeightInPoints(25); 
            
            for (int i = 0; i < colonnes.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(colonnes[i]);
                cell.setCellStyle(headerStyle);
            }

            sheet.createFreezePane(0, 1);

            // 3. Remplissage des données
            int rowNum = 1;
            for (com.pharmacie.models.AjustementStock a : ajustements) {
                Row row = sheet.createRow(rowNum++);
                
                // Date
                Cell cellDate = row.createCell(0);
                if (a.getDateAjustement() != null) {
                    cellDate.setCellValue(a.getDateAjustement());
                } else {
                    cellDate.setCellValue("");
                }
                cellDate.setCellStyle(dateStyle);

                // Produit
                row.createCell(1).setCellValue(a.getLot() != null && a.getLot().getProduit() != null ? a.getLot().getProduit().getNom() : "");
                
                // Lot
                row.createCell(2).setCellValue(a.getLot() != null ? a.getLot().getNumeroLot() : "N/A");

                // Type
                String typeLabel = a.getTypeAjustement() != null ? (a.getTypeAjustement() == MouvementStock.TypeMouvement.AJUSTEMENT_POSITIF ? "Ajout (+)" : "Retrait (-)") : "Retrait (-)";
                row.createCell(3).setCellValue(typeLabel);

                // Motif
                row.createCell(4).setCellValue(a.getMotif() != null ? a.getMotif().getLabel() : "");

                // Quantité
                Cell cellQte = row.createCell(5);
                int qte = a.getQuantite();
                cellQte.setCellValue(qte);
                if (a.getTypeAjustement() == MouvementStock.TypeMouvement.AJUSTEMENT_POSITIF) {
                    cellQte.setCellStyle(positiveStyle);
                } else {
                    cellQte.setCellStyle(negativeStyle);
                }

                // Agent
                row.createCell(6).setCellValue(a.getUser() != null ? a.getUser().getNom() : "");

                // Observations
                row.createCell(7).setCellValue(a.getObservation() != null ? a.getObservation() : "");
            }

            // 4. Ajustement des colonnes
            for (int i = 0; i < colonnes.length; i++) {
                sheet.autoSizeColumn(i);
                int currentWidth = sheet.getColumnWidth(i);
                sheet.setColumnWidth(i, currentWidth + 1000);
            }

            // 5. Écriture
            try (FileOutputStream fileOut = new FileOutputStream(fichier)) {
                workbook.write(fileOut);
            }

            logger.info("Export Ajustements Excel sauvegardé : {}", fichier.getAbsolutePath());
            ToastService.showSuccess(ownerStage, "Export Premium Réussi", "Historique des Ajustements généré avec succès !");
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(fichier);
            }

        } catch (Exception e) {
            logger.error("Erreur lors de l'export Ajustements Excel", e);
            AlertUtils.showPremiumAlert(javafx.scene.control.Alert.AlertType.ERROR, 
                "Erreur Export", "Échec de l'export", e.getMessage());
        }
    }

    public static void genererHistoriqueVentesExcel(List<com.pharmacie.models.Vente> ventes, String periodeLabel, Stage ownerStage) {
        if (ventes == null || ventes.isEmpty()) return;

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Enregistrer l'Historique des Ventes (Excel Premium)");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Classeur Excel", "*.xlsx"));
        
        String nomFichier = "Historique_Ventes_" + (periodeLabel.isEmpty() ? LocalDate.now().toString() : periodeLabel.replace(" ", "_")) + ".xlsx";
        fileChooser.setInitialFileName(nomFichier);

        File fichier = fileChooser.showSaveDialog(ownerStage);
        if (fichier == null) return;

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Historique des Ventes");

            // 1. Définition des Styles Premium
            CellStyle headerStyle = workbook.createCellStyle();
            byte[] rgb = new byte[]{(byte) 5, (byte) 150, (byte) 105}; // #059669 (Emerald)
            XSSFColor emeraldColor = new XSSFColor(rgb, null);
            ((org.apache.poi.xssf.usermodel.XSSFCellStyle) headerStyle).setFillForegroundColor(emeraldColor);
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            
            Font headerFont = workbook.createFont();
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerFont.setBold(true);
            headerFont.setFontName("Arial");
            headerFont.setFontHeightInPoints((short) 11);
            headerStyle.setFont(headerFont);
            headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);

            CellStyle numberStyle = workbook.createCellStyle();
            DataFormat format = workbook.createDataFormat();
            numberStyle.setDataFormat(format.getFormat("#,##0"));

            CellStyle dateStyle = workbook.createCellStyle();
            dateStyle.setDataFormat(format.getFormat("dd/mm/yyyy hh:mm"));

            // 2. Création de l'En-tête
            String[] colonnes = {
                "ID", "Date & Heure", "Agent", "Nb Produits", "Mode Paiement", "Total (FCFA)"
            };

            Row headerRow = sheet.createRow(0);
            headerRow.setHeightInPoints(25);
            
            for (int i = 0; i < colonnes.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(colonnes[i]);
                cell.setCellStyle(headerStyle);
            }

            sheet.createFreezePane(0, 1);

            // 3. Remplissage des données
            int rowNum = 1;
            for (com.pharmacie.models.Vente v : ventes) {
                Row row = sheet.createRow(rowNum++);
                
                // ID
                row.createCell(0).setCellValue(v.getNumeroTicketOfficiel());
                
                // Date & Heure
                Cell cellDate = row.createCell(1);
                if (v.getDateVente() != null) {
                    cellDate.setCellValue(v.getDateVente());
                    cellDate.setCellStyle(dateStyle);
                } else {
                    cellDate.setCellValue("");
                }
                
                // Agent
                row.createCell(2).setCellValue(v.getUser() != null ? v.getUser().getNom() : "");
                
                // Nb Produits
                Cell cellQte = row.createCell(3);
                int totalProduits = 0;
                if (v.getLignesVente() != null) {
                    for (com.pharmacie.models.LigneVente lv : v.getLignesVente()) {
                        totalProduits += lv.getQuantiteVendue();
                    }
                }
                cellQte.setCellValue(totalProduits);
                cellQte.setCellStyle(numberStyle);

                // Mode Paiement
                row.createCell(4).setCellValue(v.getModePaiement() != null ? v.getModePaiement().toString() : "");

                // Total TTC
                Cell cellTotal = row.createCell(5);
                cellTotal.setCellValue(v.getTotal() != null ? v.getTotal() : 0.0);
                cellTotal.setCellStyle(numberStyle);
            }

            // 4. Ligne de Totaux
            Row totalRow = sheet.createRow(rowNum);
            totalRow.setHeightInPoints(20);
            
            CellStyle totalStyle = workbook.createCellStyle();
            totalStyle.cloneStyleFrom(numberStyle);
            Font totalFont = workbook.createFont();
            totalFont.setBold(true);
            totalStyle.setFont(totalFont);
            
            Cell labelTotalCell = totalRow.createCell(4);
            labelTotalCell.setCellValue("TOTAL :");
            labelTotalCell.setCellStyle(totalStyle);

            Cell sumTotalCell = totalRow.createCell(5);
            sumTotalCell.setCellFormula("SUM(F2:F" + rowNum + ")");
            sumTotalCell.setCellStyle(totalStyle);

            // 5. Ajustement des colonnes
            for (int i = 0; i < colonnes.length; i++) {
                sheet.autoSizeColumn(i);
                int currentWidth = sheet.getColumnWidth(i);
                sheet.setColumnWidth(i, currentWidth + 1000);
            }

            // 6. Écriture du fichier
            try (FileOutputStream fileOut = new FileOutputStream(fichier)) {
                workbook.write(fileOut);
            }

            logger.info("Export Excel Premium sauvegardé : {}", fichier.getAbsolutePath());
            ToastService.showSuccess(ownerStage, "Export Premium Réussi", "Fichier Excel généré avec succès !");
            
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(fichier);
            }

        } catch (Exception e) {
            logger.error("Erreur lors de l'export Excel Premium", e);
            AlertUtils.showPremiumAlert(javafx.scene.control.Alert.AlertType.ERROR, 
                "Erreur Export", "Échec de l'export", e.getMessage());
        }
    }
}

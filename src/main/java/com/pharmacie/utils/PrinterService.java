package com.pharmacie.utils;

import com.pharmacie.models.LigneVente;
import com.pharmacie.models.Vente;

import javax.print.*;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.format.DateTimeFormatter;

public class PrinterService {

    public static void imprimerTicket(Vente vente) {
        StringBuilder ticket = new StringBuilder();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

        // Commandes ESC/POS standards pour imprimantes thermiques
        final char ESC = 27;
        final String INIT = ESC + "@"; 
        final String CENTER = ESC + "a" + 1;
        final String LEFT = ESC + "a" + 0;
        final String BOLD_ON = ESC + "E" + 1;
        final String BOLD_OFF = ESC + "E" + 0;
        final String CUT = ESC + "m"; // Coupe partielle

        // Récupération globale des paramètres
        com.pharmacie.models.PharmacieInfo info = new com.pharmacie.dao.PharmacieInfoDAO().getInfo();
        String nomPharmacie = (info != null && info.getNom() != null && !info.getNom().isEmpty()) ? info.getNom() : "PHARMACIE VETERINAIRE";
        String msgFin = (info != null && info.getMessageTicket() != null && !info.getMessageTicket().isEmpty()) ? info.getMessageTicket() : "MERCI DE VOTRE VISITE !";

        // Construction du ticket
        ticket.append(INIT);
        ticket.append(CENTER).append(BOLD_ON);
        ticket.append(nomPharmacie).append("\n");
        ticket.append(BOLD_OFF);
        
        if (info != null && info.getAdresse() != null && !info.getAdresse().trim().isEmpty()) {
            ticket.append(info.getAdresse()).append("\n");
        }
        if (info != null && info.getTelephone() != null && !info.getTelephone().trim().isEmpty()) {
            ticket.append("Tel: ").append(info.getTelephone()).append("\n");
        }
        if (info != null && info.getEmail() != null && !info.getEmail().trim().isEmpty()) {
            ticket.append(info.getEmail()).append("\n");
        }
        
        ticket.append("--------------------------------\n");
        ticket.append(LEFT);
        ticket.append("Date: ").append(vente.getDateVente().format(formatter)).append("\n");
        String agentName = vente.getUser() != null ? vente.getUser().getNom() : "Admin";
        ticket.append("Agent: ").append(agentName).append("\n");
        String customRef = vente.getDateVente() != null ? vente.getDateVente().format(DateTimeFormatter.ofPattern("ddMMyy-HHmm")) + "-" + String.format("%03d", vente.getId()) : String.valueOf(vente.getId());
        ticket.append("Ticket N°: ").append(customRef).append("\n");
        ticket.append("--------------------------------\n");
        
        for (LigneVente lv : vente.getLignesVente()) {
            String nom = lv.getProduit().getNom();
            if (nom.length() > 32) nom = nom.substring(0, 32); // Tronquer pour que ça rentre sur 58mm/80mm
            
            ticket.append(nom).append("\n");
            
            String leftPart = "  " + lv.getQuantiteVendue() + " x " + String.format(java.util.Locale.FRANCE, "%.0f", lv.getPrixUnitaire()) + " F ";
            String rightPart = " " + String.format(java.util.Locale.FRANCE, "%.0f", lv.getSousTotal()) + " FCFA";
            
            int dotsCount = 32 - (leftPart.length() + rightPart.length());
            if (dotsCount < 1) dotsCount = 1; // Au moins 1 point
            
            StringBuilder line = new StringBuilder(leftPart);
            for (int i = 0; i < dotsCount; i++) {
                line.append(".");
            }
            line.append(rightPart);
            
            ticket.append(line.toString()).append("\n");
        }
        
        ticket.append("--------------------------------\n");
        ticket.append(CENTER).append(BOLD_ON);
        ticket.append("TOTAL A PAYER : ").append(vente.getTotal()).append(" FCFA\n");
        ticket.append(BOLD_OFF);
        ticket.append("Mode Reglement: ").append(vente.getModePaiement().name()).append("\n");
        if (vente.getModePaiement() == Vente.ModePaiement.MIXTE || vente.getModePaiement() == Vente.ModePaiement.ESPECES) {
            double rec = vente.getMontantRecu() != null ? vente.getMontantRecu() : vente.getTotal();
            double mon = vente.getMonnaieRendue() != null ? vente.getMonnaieRendue() : 0.0;
            ticket.append(String.format(java.util.Locale.FRANCE, "Montant Recu  : %,.0f FCFA\n", rec));
            ticket.append(String.format(java.util.Locale.FRANCE, "Monnaie       : %,.0f FCFA\n", mon));
        }
        ticket.append("\n");
        ticket.append(msgFin).append("\n");
        ticket.append("\n\n\n\n\n").append(CUT);

        // Garder une trace dans la console
        System.out.println(ticket.toString());
        
        // Envoi réel vers le port USB/Imprimante par défaut
        envoyerAImprimante(ticket.toString());
    }

    private static void envoyerAImprimante(String texte) {
        try {
            // Recherche de l'imprimante physique configurée "Par défaut" dans Windows
            PrintService printer = PrintServiceLookup.lookupDefaultPrintService();
            if (printer == null) {
                System.out.println("[Impression] Aucune imprimante physique par défaut n'est configurée sur Windows.");
                return;
            }
            
            System.out.println("[Impression] Envoi du ticket vers l'imprimante : " + printer.getName());
            
            DocPrintJob job = printer.createPrintJob();
            // Beaucoup d'imprimantes thermiques africaines/asiatiques utilisent CP858 ou ISO-8859-1 pour les accents
            InputStream is = new ByteArrayInputStream(texte.getBytes("ISO-8859-1")); 
            Doc doc = new SimpleDoc(is, DocFlavor.INPUT_STREAM.AUTOSENSE, null);
            PrintRequestAttributeSet pras = new HashPrintRequestAttributeSet();
            
            job.print(doc, pras);
            is.close();
            System.out.println("[Impression] Succès ! Ticket imprimé physiquement.");
            
        } catch (Exception e) {
            System.out.println("[Erreur Impression] Le câble est-il bien branché ou l'imprimante allumée ? " + e.getMessage());
            e.printStackTrace();
        }
    }
}

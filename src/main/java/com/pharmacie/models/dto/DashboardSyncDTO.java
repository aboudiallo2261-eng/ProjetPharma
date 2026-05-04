package com.pharmacie.models.dto;

import java.util.List;

/**
 * Data Transfer Object (DTO) Principal (Le Jumeau Numérique).
 * Structure hiérarchique optimisée pour le frontend (React/PWA).
 */
public class DashboardSyncDTO {
    private String dateSynchro; // Format ISO 8601
    private KpisDTO kpis;
    private AlertesDTO alertes;
    
    // Nouvelles listes d'analyse
    private List<TopProduitDTO> topProduits;
    private List<HistoriqueCADTO> historique7Jours;
    private List<HistoriqueCADTO> historique3Mois;
    private List<HistoriqueCADTO> historique3Ans;

    public DashboardSyncDTO() {}

    // Getters & Setters
    public String getDateSynchro() { return dateSynchro; }
    public void setDateSynchro(String dateSynchro) { this.dateSynchro = dateSynchro; }
    
    public KpisDTO getKpis() { return kpis; }
    public void setKpis(KpisDTO kpis) { this.kpis = kpis; }
    
    public AlertesDTO getAlertes() { return alertes; }
    public void setAlertes(AlertesDTO alertes) { this.alertes = alertes; }

    public List<TopProduitDTO> getTopProduits() { return topProduits; }
    public void setTopProduits(List<TopProduitDTO> topProduits) { this.topProduits = topProduits; }

    public List<HistoriqueCADTO> getHistorique7Jours() { return historique7Jours; }
    public void setHistorique7Jours(List<HistoriqueCADTO> historique7Jours) { this.historique7Jours = historique7Jours; }

    public List<HistoriqueCADTO> getHistorique3Mois() { return historique3Mois; }
    public void setHistorique3Mois(List<HistoriqueCADTO> historique3Mois) { this.historique3Mois = historique3Mois; }

    public List<HistoriqueCADTO> getHistorique3Ans() { return historique3Ans; }
    public void setHistorique3Ans(List<HistoriqueCADTO> historique3Ans) { this.historique3Ans = historique3Ans; }

    // --- CLASSES INTERNES ---

    public static class KpisDTO {
        private KpiFinancierDTO jour;
        private KpiFinancierDTO mois;
        private KpiFinancierDTO annee;
        private KpiStockDTO stock;

        public KpiFinancierDTO getJour() { return jour; }
        public void setJour(KpiFinancierDTO jour) { this.jour = jour; }
        
        public KpiFinancierDTO getMois() { return mois; }
        public void setMois(KpiFinancierDTO mois) { this.mois = mois; }

        public KpiFinancierDTO getAnnee() { return annee; }
        public void setAnnee(KpiFinancierDTO annee) { this.annee = annee; }
        
        public KpiStockDTO getStock() { return stock; }
        public void setStock(KpiStockDTO stock) { this.stock = stock; }
    }

    public static class KpiFinancierDTO {
        private long chiffreAffaire;
        private long benefice;
        private int ventesRealisees;
        private double evolutionCA; // Tendance en pourcentage (ex: +15.5)
        private long pertesValeur;  // Valeur financière perdue

        public long getChiffreAffaire() { return chiffreAffaire; }
        public void setChiffreAffaire(long chiffreAffaire) { this.chiffreAffaire = chiffreAffaire; }
        
        public long getBenefice() { return benefice; }
        public void setBenefice(long benefice) { this.benefice = benefice; }
        
        public int getVentesRealisees() { return ventesRealisees; }
        public void setVentesRealisees(int ventesRealisees) { this.ventesRealisees = ventesRealisees; }

        public double getEvolutionCA() { return evolutionCA; }
        public void setEvolutionCA(double evolutionCA) { this.evolutionCA = evolutionCA; }

        public long getPertesValeur() { return pertesValeur; }
        public void setPertesValeur(long pertesValeur) { this.pertesValeur = pertesValeur; }
    }

    public static class KpiStockDTO {
        private long valeurTotale;
        private int nombreRuptures;
        private int nombreAlerteStock; // Nouveau: Produits <= seuil mais > 0
        private int nombrePerimes;
        private long valeurPerimes;
        private int nombreProchePeremption;
        private long valeurARisque;

        public long getValeurTotale() { return valeurTotale; }
        public void setValeurTotale(long valeurTotale) { this.valeurTotale = valeurTotale; }
        
        public int getNombreRuptures() { return nombreRuptures; }
        public void setNombreRuptures(int nombreRuptures) { this.nombreRuptures = nombreRuptures; }

        public int getNombreAlerteStock() { return nombreAlerteStock; }
        public void setNombreAlerteStock(int nombreAlerteStock) { this.nombreAlerteStock = nombreAlerteStock; }
        
        public int getNombrePerimes() { return nombrePerimes; }
        public void setNombrePerimes(int nombrePerimes) { this.nombrePerimes = nombrePerimes; }

        public long getValeurPerimes() { return valeurPerimes; }
        public void setValeurPerimes(long valeurPerimes) { this.valeurPerimes = valeurPerimes; }

        public int getNombreProchePeremption() { return nombreProchePeremption; }
        public void setNombreProchePeremption(int nombreProchePeremption) { this.nombreProchePeremption = nombreProchePeremption; }

        public long getValeurARisque() { return valeurARisque; }
        public void setValeurARisque(long valeurARisque) { this.valeurARisque = valeurARisque; }
    }

    public static class AlertesDTO {
        private List<ProduitRuptureDTO> ruptures; // Uniquement stock = 0
        private List<ProduitRuptureDTO> alertesStock; // Stock <= seuil et > 0
        private List<ProduitPerimeDTO> perimes;
        private List<ProduitPerimeDTO> prochePeremptions;
        private List<PerteDetailDTO> pertes; // Liste détaillée des casses/pertes

        public List<ProduitRuptureDTO> getRuptures() { return ruptures; }
        public void setRuptures(List<ProduitRuptureDTO> ruptures) { this.ruptures = ruptures; }

        public List<ProduitRuptureDTO> getAlertesStock() { return alertesStock; }
        public void setAlertesStock(List<ProduitRuptureDTO> alertesStock) { this.alertesStock = alertesStock; }
        
        public List<ProduitPerimeDTO> getPerimes() { return perimes; }
        public void setPerimes(List<ProduitPerimeDTO> perimes) { this.perimes = perimes; }

        public List<ProduitPerimeDTO> getProchePeremptions() { return prochePeremptions; }
        public void setProchePeremptions(List<ProduitPerimeDTO> prochePeremptions) { this.prochePeremptions = prochePeremptions; }

        public List<PerteDetailDTO> getPertes() { return pertes; }
        public void setPertes(List<PerteDetailDTO> pertes) { this.pertes = pertes; }
    }
}

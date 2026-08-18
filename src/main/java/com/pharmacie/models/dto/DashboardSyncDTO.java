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
    private CaisseDTO caisse;
    
    // Nouvelles listes d'analyse
    private List<TopProduitDTO> topProduitsJour;
    private List<TopProduitDTO> topProduitsMois;
    private List<TopProduitDTO> topProduitsAnnee;
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

    public CaisseDTO getCaisse() { return caisse; }
    public void setCaisse(CaisseDTO caisse) { this.caisse = caisse; }

    public List<TopProduitDTO> getTopProduitsJour() { return topProduitsJour; }
    public void setTopProduitsJour(List<TopProduitDTO> topProduitsJour) { this.topProduitsJour = topProduitsJour; }

    public List<TopProduitDTO> getTopProduitsMois() { return topProduitsMois; }
    public void setTopProduitsMois(List<TopProduitDTO> topProduitsMois) { this.topProduitsMois = topProduitsMois; }

    public List<TopProduitDTO> getTopProduitsAnnee() { return topProduitsAnnee; }
    public void setTopProduitsAnnee(List<TopProduitDTO> topProduitsAnnee) { this.topProduitsAnnee = topProduitsAnnee; }

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

    /**
     * État de la caisse, tel qu'il est lisible à distance.
     *
     * Ces chiffres existaient déjà dans sessions_caisse et n'étaient consultables
     * que sur le poste, dans le Registre des clôtures. Or c'est précisément quand
     * le propriétaire est absent qu'il a besoin de savoir si la caisse a été
     * tenue et clôturée : la clôture déclenche à la fois la sauvegarde de la base
     * et la synchronisation, si bien qu'une journée non clôturée est une journée
     * ni sauvegardée ni remontée.
     *
     * La ventilation des ventes par agent est délibérément absente : elle est
     * comparative, donc sensible, et rien ne prouve encore qu'elle soit utile.
     * Le nom porté ici est celui de la personne à appeler, pas un classement.
     */
    public static class CaisseDTO {
        private SessionDTO derniere;
        private int sessionsTotal;
        private int sessionsCloturees;
        private int sessionsNonCloturees;
        /**
         * Nombre de sessions closes dont le comptage ne tombait pas juste.
         *
         * Le solde ci-dessous est une somme signée : un manque de 5 000 un jour
         * et un excédent de 5 000 un autre s'y annulent, et deux comptages faux
         * y prennent l'apparence d'une caisse irréprochable. Ce compteur est ce
         * qui distingue une caisse juste d'une caisse dont les erreurs se
         * compensent.
         */
        private int sessionsAvecEcart;
        private long ecartEspecesCumule;
        private long ecartMobileCumule;
        private int joursObserves;
        private java.util.List<SessionDTO> historique;

        public SessionDTO getDerniere() { return derniere; }
        public void setDerniere(SessionDTO derniere) { this.derniere = derniere; }

        public int getSessionsTotal() { return sessionsTotal; }
        public void setSessionsTotal(int sessionsTotal) { this.sessionsTotal = sessionsTotal; }

        public int getSessionsCloturees() { return sessionsCloturees; }
        public void setSessionsCloturees(int sessionsCloturees) { this.sessionsCloturees = sessionsCloturees; }

        public int getSessionsNonCloturees() { return sessionsNonCloturees; }
        public void setSessionsNonCloturees(int sessionsNonCloturees) { this.sessionsNonCloturees = sessionsNonCloturees; }

        public int getSessionsAvecEcart() { return sessionsAvecEcart; }
        public void setSessionsAvecEcart(int sessionsAvecEcart) { this.sessionsAvecEcart = sessionsAvecEcart; }

        public long getEcartEspecesCumule() { return ecartEspecesCumule; }
        public void setEcartEspecesCumule(long ecartEspecesCumule) { this.ecartEspecesCumule = ecartEspecesCumule; }

        public long getEcartMobileCumule() { return ecartMobileCumule; }
        public void setEcartMobileCumule(long ecartMobileCumule) { this.ecartMobileCumule = ecartMobileCumule; }

        public int getJoursObserves() { return joursObserves; }
        public void setJoursObserves(int joursObserves) { this.joursObserves = joursObserves; }

        public java.util.List<SessionDTO> getHistorique() { return historique; }
        public void setHistorique(java.util.List<SessionDTO> historique) { this.historique = historique; }
    }

    /** Une session de caisse : qui l'a tenue, quand, et ce qu'elle a laissé comme écart. */
    public static class SessionDTO {
        private String agent;
        private String dateOuverture; // ISO 8601
        private String dateCloture;   // ISO 8601, null si la session est restée ouverte
        private String statut;        // OUVERTE | FERMEE
        private long especesAttendu;
        private long especesDeclare;
        private long ecartEspeces;
        private long mobileAttendu;
        private long mobileDeclare;
        private long ecartMobile;

        public String getAgent() { return agent; }
        public void setAgent(String agent) { this.agent = agent; }

        public String getDateOuverture() { return dateOuverture; }
        public void setDateOuverture(String dateOuverture) { this.dateOuverture = dateOuverture; }

        public String getDateCloture() { return dateCloture; }
        public void setDateCloture(String dateCloture) { this.dateCloture = dateCloture; }

        public String getStatut() { return statut; }
        public void setStatut(String statut) { this.statut = statut; }

        public long getEspecesAttendu() { return especesAttendu; }
        public void setEspecesAttendu(long especesAttendu) { this.especesAttendu = especesAttendu; }

        public long getEspecesDeclare() { return especesDeclare; }
        public void setEspecesDeclare(long especesDeclare) { this.especesDeclare = especesDeclare; }

        public long getEcartEspeces() { return ecartEspeces; }
        public void setEcartEspeces(long ecartEspeces) { this.ecartEspeces = ecartEspeces; }

        public long getMobileAttendu() { return mobileAttendu; }
        public void setMobileAttendu(long mobileAttendu) { this.mobileAttendu = mobileAttendu; }

        public long getMobileDeclare() { return mobileDeclare; }
        public void setMobileDeclare(long mobileDeclare) { this.mobileDeclare = mobileDeclare; }

        public long getEcartMobile() { return ecartMobile; }
        public void setEcartMobile(long ecartMobile) { this.ecartMobile = ecartMobile; }
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

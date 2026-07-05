package com.pharmacie.services;

import com.pharmacie.dao.VenteDAO;
import com.pharmacie.models.SessionCaisse;
import com.pharmacie.models.Vente;

import java.util.List;

/**
 * Service métier dédié aux calculs de Caisse (clôture Z).
 *
 * <p>Règle d'or des paiements MIXTES : la part espèces et la part mobile sont
 * lues depuis les champs {@code montantEspeces} / {@code montantMobile} persistés
 * à l'encaissement. Sans cette décomposition, l'argent des ventes mixtes
 * disparaîtrait des totaux de clôture et créerait des écarts fictifs.</p>
 *
 * <p>Le cœur du calcul ({@link #calculerBilan(double, List)}) est une fonction pure
 * sans accès base de données : elle est testable unitairement avec de simples
 * objets {@link Vente} en mémoire.</p>
 */
public class CaisseService {

    private final VenteDAO venteDAO;

    public CaisseService() {
        this(new VenteDAO());
    }

    /** Constructeur d'injection — permet de mocker le DAO dans les tests. */
    public CaisseService(VenteDAO venteDAO) {
        this.venteDAO = venteDAO;
    }

    /**
     * Résultat complet des calculs de clôture d'une session de caisse.
     *
     * @param ventesPuresEspeces      Total des ventes payées 100% en espèces
     * @param ventesPuresMobile       Total des ventes payées 100% en Mobile Money
     * @param nbVentesMixtes          Nombre de ventes en paiement mixte
     * @param partEspecesMixtes       Somme des parts espèces des ventes mixtes
     * @param partMobileMixtes        Somme des parts mobile des ventes mixtes
     * @param totalEspeces            Espèces encaissées (pures + parts mixtes)
     * @param totalMobile             Mobile encaissé (pur + parts mixtes)
     * @param especesAttenduesTiroir  Fond de caisse + totalEspeces (théorie du tiroir)
     */
    public record BilanCloture(
            double ventesPuresEspeces,
            double ventesPuresMobile,
            long nbVentesMixtes,
            double partEspecesMixtes,
            double partMobileMixtes,
            double totalEspeces,
            double totalMobile,
            double especesAttenduesTiroir) {

        /** Écart entre le comptage physique du tiroir et la théorie. */
        public double ecartEspeces(double especesDeclarees) {
            return especesDeclarees - especesAttenduesTiroir;
        }
    }

    /**
     * Charge les ventes de la session en base puis calcule le bilan de clôture.
     */
    public BilanCloture calculerBilanCloture(SessionCaisse session) {
        List<Vente> ventes = venteDAO.findBySessionCaisse(session.getId());
        double fond = session.getFondDeCaisse() != null ? session.getFondDeCaisse() : 0.0;
        return calculerBilan(fond, ventes);
    }

    /**
     * Calcul pur du bilan de clôture — aucun accès base de données.
     *
     * <ul>
     *   <li>ESPECES : le total de la vente entre au tiroir</li>
     *   <li>MOBILE_MONEY : le total est 100% digital, rien au tiroir</li>
     *   <li>MIXTE : décomposé via montantEspeces / montantMobile persistés</li>
     * </ul>
     */
    public static BilanCloture calculerBilan(double fondDeCaisse, List<Vente> ventes) {
        double ventesPuresEspeces = 0.0;
        double ventesPuresMobile = 0.0;
        long nbMixtes = 0;
        double partEspecesMixtes = 0.0;
        double partMobileMixtes = 0.0;

        for (Vente v : ventes) {
            double total = v.getTotal() != null ? v.getTotal() : 0.0;
            if (v.getModePaiement() == Vente.ModePaiement.ESPECES) {
                ventesPuresEspeces += total;
            } else if (v.getModePaiement() == Vente.ModePaiement.MOBILE_MONEY) {
                ventesPuresMobile += total;
            } else if (v.getModePaiement() == Vente.ModePaiement.MIXTE) {
                nbMixtes++;
                partEspecesMixtes += v.getMontantEspeces() != null ? v.getMontantEspeces() : 0.0;
                partMobileMixtes += v.getMontantMobile() != null ? v.getMontantMobile() : 0.0;
            }
        }

        double totalEspeces = ventesPuresEspeces + partEspecesMixtes;
        double totalMobile = ventesPuresMobile + partMobileMixtes;

        return new BilanCloture(
                ventesPuresEspeces,
                ventesPuresMobile,
                nbMixtes,
                partEspecesMixtes,
                partMobileMixtes,
                totalEspeces,
                totalMobile,
                fondDeCaisse + totalEspeces);
    }
}

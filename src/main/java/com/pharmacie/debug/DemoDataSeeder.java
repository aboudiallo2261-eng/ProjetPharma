package com.pharmacie.debug;

import com.pharmacie.models.*;
import com.pharmacie.services.CaisseService;
import com.pharmacie.utils.HibernateUtil;
import com.pharmacie.utils.SecuritySeeder;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.mindrot.jbcrypt.BCrypt;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Générateur de données de DÉMONSTRATION réalistes (marché vétérinaire ouest-africain).
 *
 * <p>⚠ REMPLACE tout le contenu de la base. À n'exécuter qu'après sauvegarde :
 * {@code java -cp "target/classes;target/libs/*" com.pharmacie.debug.DemoDataSeeder}</p>
 *
 * <p>Génère ~18 mois d'activité cohérente en respectant les invariants du logiciel :
 * chaque vente débite ses lots en FEFO, possède son ticket, ses MouvementStock,
 * sa session de caisse clôturée avec des totaux exacts (via CaisseService) et des
 * écarts occasionnels réalistes. Les alertes (périmés, proches péremption, ruptures,
 * seuils) sont garanties en fin de génération pour les démonstrations.</p>
 */
public class DemoDataSeeder {

    private static final Random RNG = new Random(20260706); // reproductible
    private static final LocalDate DEBUT = LocalDate.of(2025, 1, 6);
    private static final LocalDate AUJOURDHUI = LocalDate.now();

    // ── Suivi mémoire du stock (source de vérité pendant la génération) ──
    private static class LotSim {
        Long id;
        String numero;
        LocalDate expiration;
        int stock;
        double prixAchat;
        LotSim(Long id, String numero, LocalDate exp, int stock, double prixAchat) {
            this.id = id; this.numero = numero; this.expiration = exp;
            this.stock = stock; this.prixAchat = prixAchat;
        }
    }

    private static class ProduitSim {
        Long id;
        String nom;
        double prixAchat, prixVente;
        boolean decond;
        Integer unitesParBoite;
        Double prixVenteUnite;
        int seuil;
        int poids;          // popularité relative pour le tirage des ventes
        Long fournisseurId;
        boolean stopRestock = false; // pour fabriquer ruptures et alertes
        List<LotSim> lots = new ArrayList<>();
        int stockTotal() { return lots.stream().mapToInt(l -> l.stock).sum(); }
    }

    private static final List<ProduitSim> PRODUITS = new ArrayList<>();
    private static Long adminId, awaId, moussaId;
    private static int compteurLot = 1;

    public static void main(String[] args) {
        System.out.println("=== GÉNÉRATEUR DE DONNÉES DE DÉMONSTRATION VETPHARMA ===");
        HibernateUtil.getSessionFactory(); // init schéma + Flyway

        viderBase();
        SecuritySeeder.initializeSecurity(); // recrée profil SUPER-ADMIN + admin/admin
        creerCatalogueEtUtilisateurs();
        genererHistorique();
        fabriquerAlertesDemo();
        ecrireStocksFinaux();
        afficherResume();
        System.out.println("=== TERMINÉ — base de démonstration prête ===");
        System.exit(0);
    }

    // ═════════════════════════════ 1. VIDAGE ═════════════════════════════

    private static void viderBase() {
        System.out.println("[1/5] Vidage de la base...");
        try (Session s = HibernateUtil.getSessionFactory().openSession()) {
            Transaction tx = s.beginTransaction();
            s.createNativeMutationQuery("SET FOREIGN_KEY_CHECKS=0").executeUpdate();
            for (String table : new String[]{"mouvements_stock", "ajustements_stock",
                    "lignes_vente", "ventes", "sessions_caisse", "lignes_achat", "achats",
                    "lots", "produits", "fournisseurs", "categories", "especes",
                    "users", "profils", "pharmacie_info"}) {
                s.createNativeMutationQuery("TRUNCATE TABLE " + table).executeUpdate();
            }
            s.createNativeMutationQuery("SET FOREIGN_KEY_CHECKS=1").executeUpdate();
            tx.commit();
        }
    }

    // ═════════════════ 2. CATALOGUE, ÉQUIPE, PHARMACIE ═══════════════════

    private static void creerCatalogueEtUtilisateurs() {
        System.out.println("[2/5] Catalogue, fournisseurs et équipe...");
        try (Session s = HibernateUtil.getSessionFactory().openSession()) {
            Transaction tx = s.beginTransaction();

            // Pharmacie
            PharmacieInfo info = s.createQuery("FROM PharmacieInfo", PharmacieInfo.class)
                    .setMaxResults(1).uniqueResult();
            if (info == null) { info = new PharmacieInfo(); s.persist(info); }
            info.setNom("KAOURAL CLINIQUE PHARMACIE");
            info.setAdresse("Avenue de l'Élevage, Dakar — Sénégal");
            info.setTelephone("77 123 45 67");
            info.setEmail("contact@kaoural-vet.sn");
            info.setMessageTicket("Merci de votre confiance — La santé animale, notre métier !");

            // Profil AGENT (caisse + stock uniquement)
            Profil superAdmin = s.createQuery("FROM Profil WHERE nom='SUPER-ADMIN'", Profil.class)
                    .uniqueResult();
            Profil agent = new Profil("AGENT-CAISSE", "Ventes, caisse et consultation du stock.");
            agent.setCanAccessDashboard(false);
            agent.setCanAccessVentes(true);
            agent.setCanAccessAchats(false);
            agent.setCanAccessStock(true);
            agent.setCanAccessFournisseurs(false);
            agent.setCanAccessRapports(false);
            agent.setCanAccessParametres(false);
            s.persist(agent);

            // Équipe (mots de passe de démo : demo1234)
            String hash = BCrypt.hashpw("demo1234", BCrypt.gensalt());
            User awa = new User();
            awa.setNom("Awa Ndiaye"); awa.setIdentifiant("awa");
            awa.setMotDePasseHash(hash); awa.setRole("AGENT");
            awa.setEmail("awa@kaoural-vet.sn"); awa.setProfil(agent);
            awa.setMustChangePassword(false);
            s.persist(awa);
            User moussa = new User();
            moussa.setNom("Moussa Diallo"); moussa.setIdentifiant("moussa");
            moussa.setMotDePasseHash(hash); moussa.setRole("AGENT");
            moussa.setEmail("moussa@kaoural-vet.sn"); moussa.setProfil(agent);
            moussa.setMustChangePassword(false);
            s.persist(moussa);

            // Catégories
            Map<String, Categorie> cats = new HashMap<>();
            for (String n : new String[]{"Antibiotiques", "Antiparasitaires", "Vaccins",
                    "Vitamines & Reconstituants", "Antiseptiques & Soins", "Matériel & Consommables"}) {
                Categorie c = new Categorie(); c.setNom(n); s.persist(c); cats.put(n, c);
            }
            // Espèces
            Map<String, Espece> esps = new HashMap<>();
            for (String n : new String[]{"Bovins", "Ovins & Caprins", "Volaille",
                    "Équins & Asins", "Chiens & Chats", "Toutes espèces"}) {
                Espece e = new Espece(); e.setNom(n); s.persist(e); esps.put(n, e);
            }
            // Fournisseurs
            List<Fournisseur> fours = new ArrayList<>();
            String[][] f = {
                {"SENVET Distribution", "M. Abdoulaye Sarr", "77 631 20 45", "commandes@senvet.sn", "Zone industrielle, Dakar", "Paiement à 30 jours"},
                {"LAPROVET Sénégal", "Mme Fatou Bèye", "76 480 11 02", "vente@laprovet.sn", "Km 4.5 Bd du Centenaire, Dakar", "Livraison sous 72h"},
                {"CEVA Santé Animale Afrique", "M. Jean-Marc Diouf", "78 152 90 33", "afrique@ceva.com", "Almadies, Dakar", "Chaîne du froid vaccins"},
                {"AGROPHARM Thiès", "M. Ibrahima Fall", "70 214 55 87", "agropharm@orange.sn", "Route de Mbour, Thiès", "Comptant"},
                {"DISTRIVET Touba", "Serigne Modou Mbacké", "77 905 34 21", "distrivet@gmail.com", "Marché Ocass, Touba", "Paiement à 15 jours"},
            };
            for (String[] row : f) {
                Fournisseur fo = new Fournisseur();
                fo.setNom(row[0]); fo.setContact(row[1]); fo.setTelephone(row[2]);
                fo.setEmail(row[3]); fo.setAdresse(row[4]); fo.setConditions(row[5]);
                s.persist(fo); fours.add(fo);
            }

            // Produits : nom | catégorie | espèce | achat | vente | decond(upb, prixUnité) | seuil | poids | fournisseur
            Object[][] defs = {
                // Antiparasitaires (gros volume élevage)
                {"Ivermectine 1% Injectable 50ml", "Antiparasitaires", "Bovins", 3200, 5000, null, null, 8, 10, 0},
                {"Ivermectine 1% Injectable 100ml", "Antiparasitaires", "Bovins", 5500, 8500, null, null, 5, 6, 0},
                {"Albendazole 2500mg Bolus (x20)", "Antiparasitaires", "Bovins", 6000, 9000, 20, 600, 60, 12, 1},
                {"Albendazole 300mg Comprimés (x50)", "Antiparasitaires", "Ovins & Caprins", 4500, 7000, 50, 200, 100, 11, 1},
                {"Lévamisole 3g Bolus (x25)", "Antiparasitaires", "Bovins", 5000, 7500, 25, 400, 50, 8, 1},
                {"Tétramisole 20% Poudre 100g", "Antiparasitaires", "Ovins & Caprins", 1800, 3000, null, null, 10, 7, 3},
                {"Amitraz 12.5% Solution 100ml", "Antiparasitaires", "Bovins", 2500, 4000, null, null, 6, 5, 3},
                {"Deltaméthrine Pour-On 250ml", "Antiparasitaires", "Bovins", 4800, 7500, null, null, 5, 5, 0},
                {"Praziquantel Comprimés Chien (x10)", "Antiparasitaires", "Chiens & Chats", 3000, 5000, 10, 700, 20, 3, 1},
                // Antibiotiques
                {"Oxytétracycline 20% LA 100ml", "Antibiotiques", "Bovins", 2800, 4500, null, null, 10, 12, 0},
                {"Oxytétracycline 10% 100ml", "Antibiotiques", "Toutes espèces", 1900, 3200, null, null, 8, 8, 1},
                {"Pénicilline-Streptomycine 100ml", "Antibiotiques", "Bovins", 3200, 5000, null, null, 8, 9, 0},
                {"Amoxicilline 15% LA 100ml", "Antibiotiques", "Bovins", 4200, 6500, null, null, 6, 7, 1},
                {"Tylosine 20% Injectable 100ml", "Antibiotiques", "Bovins", 3800, 6000, null, null, 5, 5, 1},
                {"Sulfadimidine 33% 100ml", "Antibiotiques", "Toutes espèces", 2200, 3500, null, null, 6, 5, 3},
                {"Colistine Poudre Orale 100g", "Antibiotiques", "Volaille", 2000, 3200, null, null, 8, 6, 3},
                {"Enrofloxacine 10% Oral 1L", "Antibiotiques", "Volaille", 6500, 10000, null, null, 4, 5, 1},
                {"Gentamicine 10% 100ml", "Antibiotiques", "Chiens & Chats", 3500, 5500, null, null, 4, 3, 1},
                // Vaccins (chaîne du froid — CEVA)
                {"Vaccin Newcastle I-2 (100 doses)", "Vaccins", "Volaille", 2500, 4000, null, null, 10, 9, 2},
                {"Vaccin PPR (100 doses)", "Vaccins", "Ovins & Caprins", 4000, 6500, null, null, 6, 7, 2},
                {"Vaccin Charbon Symptomatique (50 doses)", "Vaccins", "Bovins", 3500, 5500, null, null, 5, 5, 2},
                {"Vaccin Pasteurellose Bovine (50 doses)", "Vaccins", "Bovins", 3800, 6000, null, null, 5, 4, 2},
                {"Vaccin Gumboro (100 doses)", "Vaccins", "Volaille", 2800, 4500, null, null, 8, 6, 2},
                {"Vaccin Rage Canine (monodose)", "Vaccins", "Chiens & Chats", 1500, 3000, null, null, 10, 4, 2},
                // Vitamines & Reconstituants
                {"Multivitamines Injectable 100ml", "Vitamines & Reconstituants", "Toutes espèces", 2200, 3500, null, null, 8, 9, 0},
                {"Fer + B12 Injectable 100ml", "Vitamines & Reconstituants", "Toutes espèces", 2500, 4000, null, null, 6, 5, 0},
                {"Calcium-Magnésium Inj. 500ml", "Vitamines & Reconstituants", "Bovins", 3500, 5500, null, null, 5, 5, 1},
                {"AD3E Injectable 100ml", "Vitamines & Reconstituants", "Toutes espèces", 2400, 3800, null, null, 6, 6, 0},
                {"Vitamine C Antistress Poudre 100g (x25 sachets)", "Vitamines & Reconstituants", "Volaille", 3000, 5000, 25, 300, 50, 10, 3},
                {"Réhydratant Électrolytes 1kg", "Vitamines & Reconstituants", "Toutes espèces", 2800, 4500, null, null, 5, 5, 3},
                {"Amprolium 20% Anticoccidien 100g", "Vitamines & Reconstituants", "Volaille", 1800, 3000, null, null, 8, 7, 3},
                // Antiseptiques & Soins
                {"Povidone Iodée 10% 500ml", "Antiseptiques & Soins", "Toutes espèces", 2000, 3500, null, null, 6, 6, 4},
                {"Alcool 90° 250ml", "Antiseptiques & Soins", "Toutes espèces", 600, 1000, null, null, 12, 7, 4},
                {"Spray Cicatrisant Aluminium 200ml", "Antiseptiques & Soins", "Toutes espèces", 2800, 4500, null, null, 5, 5, 4},
                {"Pommade Oxyde de Zinc 100g", "Antiseptiques & Soins", "Toutes espèces", 1200, 2000, null, null, 6, 4, 4},
                {"Eau Oxygénée 250ml", "Antiseptiques & Soins", "Toutes espèces", 500, 900, null, null, 10, 4, 4},
                {"Shampooing Antiparasitaire Chien 250ml", "Antiseptiques & Soins", "Chiens & Chats", 2500, 4000, null, null, 4, 3, 4},
                // Matériel & Consommables (déconditionnables à l'unité)
                {"Seringues 10ml (boîte x100)", "Matériel & Consommables", "Toutes espèces", 5000, 8000, 100, 100, 200, 9, 4},
                {"Seringues 5ml (boîte x100)", "Matériel & Consommables", "Toutes espèces", 4000, 6500, 100, 85, 200, 7, 4},
                {"Aiguilles 18G (boîte x100)", "Matériel & Consommables", "Toutes espèces", 3000, 5000, 100, 75, 150, 6, 4},
                {"Gants Latex (boîte x100)", "Matériel & Consommables", "Toutes espèces", 3500, 5500, 100, 75, 100, 8, 4},
                {"Trocart Bovin Inox", "Matériel & Consommables", "Bovins", 4500, 7500, null, null, 3, 2, 4},
                {"Thermomètre Vétérinaire Digital", "Matériel & Consommables", "Toutes espèces", 1500, 3000, null, null, 4, 3, 4},
                {"Sonde Œsophagienne Veau", "Matériel & Consommables", "Bovins", 6000, 9500, null, null, 2, 2, 4},
                {"Pince Mouchette Bovin", "Matériel & Consommables", "Bovins", 3500, 6000, null, null, 3, 2, 4},
                {"Fil de Suture Résorbable (x12)", "Matériel & Consommables", "Toutes espèces", 4800, 7500, 12, 750, 24, 3, 4},
            };
            for (Object[] d : defs) {
                Produit p = new Produit();
                p.setNom((String) d[0]);
                p.setCategorie(cats.get((String) d[1]));
                p.setEspece(esps.get((String) d[2]));
                p.setPrixAchat(((Integer) d[3]).doubleValue());
                p.setPrixVente(((Integer) d[4]).doubleValue());
                boolean decond = d[5] != null;
                p.setEstDeconditionnable(decond);
                if (decond) {
                    p.setUnitesParBoite((Integer) d[5]);
                    p.setPrixVenteUnite(((Integer) d[6]).doubleValue());
                }
                p.setSeuilAlerte((Integer) d[7]);
                s.persist(p);

                ProduitSim ps = new ProduitSim();
                ps.nom = p.getNom();
                ps.prixAchat = p.getPrixAchat(); ps.prixVente = p.getPrixVente();
                ps.decond = decond;
                ps.unitesParBoite = decond ? (Integer) d[5] : null;
                ps.prixVenteUnite = decond ? ((Integer) d[6]).doubleValue() : null;
                ps.seuil = (Integer) d[7];
                ps.poids = (Integer) d[8];
                ps.fournisseurId = fours.get((Integer) d[9]).getId();
                PRODUITS.add(ps);
                ps.id = p.getId();
            }
            tx.commit();

            awaId = awa.getId(); moussaId = moussa.getId();
            adminId = s.createQuery("SELECT u.id FROM User u WHERE u.identifiant='admin'", Long.class)
                    .uniqueResult();
        }
    }

    // ═════════════════════ 3. HISTORIQUE 18 MOIS ═════════════════════════

    private static void genererHistorique() {
        System.out.println("[3/5] Génération de l'historique " + DEBUT + " → " + AUJOURDHUI + "...");
        LocalDate jour = DEBUT;
        int totalVentes = 0;
        while (!jour.isAfter(AUJOURDHUI)) {
            LocalDate finMois = jour.withDayOfMonth(jour.lengthOfMonth());
            LocalDate borne = finMois.isAfter(AUJOURDHUI) ? AUJOURDHUI : finMois;
            totalVentes += genererMois(jour, borne);
            jour = borne.plusDays(1);
        }
        System.out.println("      → " + totalVentes + " ventes générées.");
    }

    /** Génère un mois complet (réapprovisionnements + ventes quotidiennes) dans UNE transaction. */
    private static int genererMois(LocalDate du, LocalDate au, Session... unused) {
        int nbVentes = 0;
        try (Session s = HibernateUtil.getSessionFactory().openSession()) {
            Transaction tx = s.beginTransaction();

            // Réapprovisionnement en début de mois (+ stock initial le tout premier mois)
            reapprovisionner(s, du, du.equals(DEBUT));

            for (LocalDate jour = du; !jour.isAfter(au); jour = jour.plusDays(1)) {
                if (jour.getDayOfWeek() == DayOfWeek.SUNDAY) continue;

                // Saisonnalité : vaccination/vermifugation (nov-fév) et hivernage (juin-juil)
                int mois = jour.getMonthValue();
                double saison = (mois >= 11 || mois <= 2) ? 1.35 : (mois == 6 || mois == 7) ? 1.2 : 1.0;
                int ventesJour = (int) Math.round((4 + RNG.nextInt(7)) * saison);
                boolean aujourdHui = jour.equals(AUJOURDHUI);
                if (aujourdHui) ventesJour = Math.min(ventesJour, 4); // journée en cours, partielle

                Long agentId = (jour.getDayOfMonth() % 2 == 0) ? awaId : moussaId;
                User agent = s.getReference(User.class, agentId);

                // Session de caisse du jour
                SessionCaisse caisse = new SessionCaisse();
                caisse.setUser(agent);
                caisse.setDateOuverture(jour.atTime(8, 15 + RNG.nextInt(20)));
                caisse.setFondDeCaisse(25000.0);
                caisse.setStatut(SessionCaisse.StatutSession.OUVERTE);
                s.persist(caisse);

                List<Vente> ventesDuJour = new ArrayList<>();
                for (int v = 0; v < ventesJour; v++) {
                    Vente vente = genererVente(s, jour, agent, caisse, aujourdHui);
                    if (vente != null) { ventesDuJour.add(vente); nbVentes++; }
                }

                // Clôture Z avec les VRAIS calculs du logiciel
                if (!aujourdHui) {
                    CaisseService.BilanCloture bilan = CaisseService.calculerBilan(25000.0, ventesDuJour);
                    caisse.setDateCloture(jour.atTime(19, RNG.nextInt(30)));
                    caisse.setTotalEspecesAttendu(bilan.especesAttenduesTiroir());
                    double ecart = (RNG.nextInt(100) < 12)
                            ? (RNG.nextBoolean() ? -1 : 1) * (100 + RNG.nextInt(15)) * 10.0
                            : 0.0;
                    caisse.setEspecesDeclare(bilan.especesAttenduesTiroir() + ecart);
                    caisse.setEcartEspeces(ecart);
                    caisse.setTotalMobileAttendu(bilan.totalMobile());
                    caisse.setMobileDeclare(bilan.totalMobile());
                    caisse.setEcartMobile(0.0);
                    caisse.setStatut(SessionCaisse.StatutSession.FERMEE);
                }

                if (nbVentes % 120 == 0) { s.flush(); s.clear(); }
            }
            tx.commit();
        }
        return nbVentes;
    }

    /** Ligne candidate préparée en mémoire avant toute persistance. */
    private static class LigneSim {
        ProduitSim produit;
        boolean vendreUnite;
        int qte;
        double prixU, sousTotal;
        LotSim dernierLot;
        List<Object[]> debits = new ArrayList<>(); // [LotSim, unitesPrises]
    }

    /**
     * Tire des produits au sort (pondéré), débite en FEFO, crée vente+lignes+mouvements.
     * IMPORTANT : avec GenerationType.IDENTITY, persist() exécute l'INSERT immédiatement —
     * on construit donc TOUT en mémoire (paiement compris) avant le moindre persist.
     */
    private static Vente genererVente(Session s, LocalDate jour, User agent,
                                      SessionCaisse caisse, boolean aujourdHui) {
        int nbLignes = 1 + (RNG.nextInt(100) < 35 ? 1 : 0) + (RNG.nextInt(100) < 10 ? 1 : 0);
        LocalDateTime heure = jour.atTime(9 + RNG.nextInt(aujourdHui ? 3 : 9), RNG.nextInt(60));

        // ── 1. Construction en mémoire ────────────────────────────────────
        List<LigneSim> lignes = new ArrayList<>();
        double total = 0;
        for (int l = 0; l < nbLignes; l++) {
            ProduitSim p = tirerProduit();
            if (p == null) continue;

            LigneSim ls = new LigneSim();
            ls.produit = p;
            ls.vendreUnite = p.decond && RNG.nextInt(100) < 65;
            ls.qte = ls.vendreUnite ? 1 + RNG.nextInt(8) : (RNG.nextInt(100) < 25 ? 2 : 1);
            int unitesBase = ls.vendreUnite ? ls.qte : ls.qte * (p.decond ? p.unitesParBoite : 1);
            if (p.stockTotal() < unitesBase) continue; // pas de stock → ligne abandonnée

            ls.prixU = ls.vendreUnite ? p.prixVenteUnite : p.prixVente;
            ls.sousTotal = ls.prixU * ls.qte;

            // Débit FEFO en mémoire
            p.lots.sort(Comparator.comparing(lot -> lot.expiration));
            int reste = unitesBase;
            for (LotSim lot : p.lots) {
                if (reste <= 0) break;
                if (lot.stock <= 0 || lot.expiration.isBefore(jour)) continue;
                int pris = Math.min(lot.stock, reste);
                lot.stock -= pris; reste -= pris; ls.dernierLot = lot;
                ls.debits.add(new Object[]{lot, pris});
            }
            if (reste > 0 || ls.dernierLot == null) {
                // Stock non expiré insuffisant : on rend ce qui a été débité et on abandonne
                for (Object[] d : ls.debits) ((LotSim) d[0]).stock += (Integer) d[1];
                continue;
            }
            lignes.add(ls);
            total += ls.sousTotal;
        }
        if (lignes.isEmpty()) return null;

        // ── 2. Vente complète (paiement : 62% espèces, 23% mobile, 15% mixte) ──
        Vente vente = new Vente();
        vente.setUser(agent);
        vente.setDateVente(heure);
        vente.setSessionCaisse(caisse);
        vente.setLignesVente(new ArrayList<>());
        vente.setTotal(total);
        int tirage = RNG.nextInt(100);
        if (tirage < 62) {
            vente.setModePaiement(Vente.ModePaiement.ESPECES);
            double recu = Math.ceil(total / 500.0) * 500 + (RNG.nextInt(100) < 30 ? 1000 : 0);
            vente.setMontantEspeces(total); vente.setMontantMobile(0.0);
            vente.setMontantRecu(recu); vente.setMonnaieRendue(recu - total);
        } else if (tirage < 85) {
            vente.setModePaiement(Vente.ModePaiement.MOBILE_MONEY);
            vente.setMontantEspeces(0.0); vente.setMontantMobile(total);
            vente.setMontantRecu(total); vente.setMonnaieRendue(0.0);
        } else {
            vente.setModePaiement(Vente.ModePaiement.MIXTE);
            double partEsp = Math.round(total * (0.3 + RNG.nextDouble() * 0.4) / 500) * 500.0;
            partEsp = Math.min(partEsp, total);
            vente.setMontantEspeces(partEsp); vente.setMontantMobile(total - partEsp);
            vente.setMontantRecu(total); vente.setMonnaieRendue(0.0);
        }
        s.persist(vente); // INSERT immédiat (IDENTITY) → id disponible

        // ── 3. Lignes + audit trail avec la vraie référence ticket ────────
        String ticketRef = String.format("TK-%02d%02d%04d-%02d%02d-%03d",
                jour.getDayOfMonth(), jour.getMonthValue(), jour.getYear(),
                heure.getHour(), heure.getMinute(), vente.getId());
        for (LigneSim ls : lignes) {
            LigneVente lv = new LigneVente();
            lv.setVente(vente);
            lv.setProduit(s.getReference(Produit.class, ls.produit.id));
            lv.setQuantiteVendue(ls.qte);
            lv.setTypeUnite(ls.vendreUnite ? LigneVente.TypeUnite.DETAIL : LigneVente.TypeUnite.BOITE_ENTIERE);
            lv.setPrixUnitaire(ls.prixU);
            lv.setSousTotal(ls.sousTotal);
            lv.setLot(s.getReference(Lot.class, ls.dernierLot.id));
            s.persist(lv);
            vente.getLignesVente().add(lv);

            String suffix = ls.vendreUnite ? " (Vente au Détail)" : " (Vente en Boîte)";
            for (Object[] d : ls.debits) {
                LotSim lot = (LotSim) d[0];
                int pris = (Integer) d[1];
                MouvementStock mvt = new MouvementStock(
                        s.getReference(Produit.class, ls.produit.id), s.getReference(Lot.class, lot.id),
                        agent, MouvementStock.TypeMouvement.VENTE, -pris, heure, ticketRef + suffix);
                s.persist(mvt);
            }
        }
        return vente;
    }

    private static ProduitSim tirerProduit() {
        int totalPoids = PRODUITS.stream().mapToInt(p -> p.poids).sum();
        int r = RNG.nextInt(totalPoids);
        for (ProduitSim p : PRODUITS) {
            r -= p.poids;
            if (r < 0) return p;
        }
        return null;
    }

    /** Commande fournisseur pour les produits sous leur point de commande. */
    private static void reapprovisionner(Session s, LocalDate date, boolean initial) {
        Map<Long, List<ProduitSim>> parFournisseur = new HashMap<>();
        for (ProduitSim p : PRODUITS) {
            if (p.stopRestock && !initial) continue;
            int pointCommande = p.seuil * 4;
            if (initial || p.stockTotal() < pointCommande) {
                parFournisseur.computeIfAbsent(p.fournisseurId, k -> new ArrayList<>()).add(p);
            }
        }
        for (Map.Entry<Long, List<ProduitSim>> e : parFournisseur.entrySet()) {
            Achat achat = new Achat();
            achat.setFournisseur(s.getReference(Fournisseur.class, e.getKey()));
            achat.setDateAchat(date.atTime(10, RNG.nextInt(50)));
            achat.setReferenceFacture(String.format("FA-%d%02d-%03d",
                    date.getYear(), date.getMonthValue(), 100 + RNG.nextInt(880)));
            achat.setLignesAchat(new ArrayList<>());
            s.persist(achat);

            for (ProduitSim p : e.getValue()) {
                int boites = initial ? 12 + RNG.nextInt(18) : 8 + RNG.nextInt(15);
                int unites = boites * (p.decond ? p.unitesParBoite : 1);
                // légère variation du prix d'achat par lot (négociation, inflation)
                double prixLot = Math.round(p.prixAchat * (0.95 + RNG.nextDouble() * 0.1) / 50) * 50.0;
                LocalDate expiration = date.plusMonths(10 + RNG.nextInt(20));

                Lot lot = new Lot();
                lot.setProduit(s.getReference(Produit.class, p.id));
                lot.setNumeroLot(String.format("LOT-%d%02d-%04d",
                        date.getYear() % 100, date.getMonthValue(), compteurLot++));
                lot.setDateExpiration(expiration);
                lot.setQuantiteStock(unites); // valeur finale réécrite en fin de génération
                lot.setEstArchive(false);
                lot.setPrixAchat(prixLot);
                s.persist(lot);

                LigneAchat la = new LigneAchat();
                la.setAchat(achat); la.setProduit(s.getReference(Produit.class, p.id));
                la.setLot(lot); la.setQuantiteAchetee(boites); la.setPrixUnitaire(prixLot);
                s.persist(la);
                achat.getLignesAchat().add(la);

                MouvementStock mvt = new MouvementStock(
                        s.getReference(Produit.class, p.id), lot,
                        s.getReference(User.class, adminId),
                        MouvementStock.TypeMouvement.ACHAT, unites,
                        achat.getDateAchat(), "Achat " + achat.getReferenceFacture());
                s.persist(mvt);

                s.flush();
                p.lots.add(new LotSim(lot.getId(), lot.getNumeroLot(), expiration, unites, prixLot));
            }
        }
    }

    // ═══════════════ 4. ALERTES GARANTIES POUR LA DÉMO ═══════════════════

    private static void fabriquerAlertesDemo() {
        System.out.println("[4/5] Fabrication des situations d'alerte (périmés, proches, ruptures)...");
        try (Session s = HibernateUtil.getSessionFactory().openSession()) {
            Transaction tx = s.beginTransaction();

            // a) 3 lots PÉRIMÉS avec stock restant (sur-stock acheté il y a ~4 mois)
            int[] perimesIdx = {6, 14, 35}; // Amitraz, Sulfadimidine, Oxyde de Zinc
            for (int idx : perimesIdx) {
                creerLotSpecial(s, PRODUITS.get(idx), AUJOURDHUI.minusDays(120),
                        AUJOURDHUI.minusDays(15 + RNG.nextInt(25)), 4 + RNG.nextInt(8));
            }
            // b) 5 lots PROCHES de la péremption (< 60 jours)
            int[] prochesIdx = {18, 20, 25, 30, 37}; // vaccins, fer, réhydratant, shampooing
            for (int idx : prochesIdx) {
                creerLotSpecial(s, PRODUITS.get(idx), AUJOURDHUI.minusDays(30),
                        AUJOURDHUI.plusDays(20 + RNG.nextInt(35)), 6 + RNG.nextInt(10));
            }
            // c) RUPTURES totales : 4 produits vidés
            int[] rupturesIdx = {7, 17, 41, 43}; // Deltaméthrine, Gentamicine, Trocart, Sonde
            for (int idx : rupturesIdx) {
                PRODUITS.get(idx).stopRestock = true;
                for (LotSim lot : PRODUITS.get(idx).lots) lot.stock = 0;
            }
            // d) Alertes seuil : 4 produits laissés juste sous le seuil
            int[] alerteIdx = {5, 23, 36, 42};
            for (int idx : alerteIdx) {
                ProduitSim p = PRODUITS.get(idx);
                int cible = Math.max(1, p.seuil - 1 - RNG.nextInt(3));
                int aRetirer = p.stockTotal() - cible;
                for (LotSim lot : p.lots) {
                    if (aRetirer <= 0) break;
                    int retrait = Math.min(lot.stock, aRetirer);
                    lot.stock -= retrait; aRetirer -= retrait;
                }
            }
            // e) Quelques pertes tracées (casse / péremption) pour le rapport des pertes
            for (int i = 0; i < 6; i++) {
                ProduitSim p = PRODUITS.get(RNG.nextInt(PRODUITS.size()));
                Optional<LotSim> lotOpt = p.lots.stream().filter(l -> l.stock > 3).findFirst();
                if (lotOpt.isEmpty()) continue;
                LotSim lot = lotOpt.get();
                int qte = 1 + RNG.nextInt(3);
                lot.stock -= qte;
                LocalDateTime quand = AUJOURDHUI.minusDays(RNG.nextInt(25)).atTime(11 + RNG.nextInt(6), RNG.nextInt(60));

                AjustementStock aj = new AjustementStock();
                aj.setLot(s.getReference(Lot.class, lot.id));
                aj.setUser(s.getReference(User.class, adminId));
                aj.setDateAjustement(quand);
                aj.setQuantite(-qte);
                aj.setTypeAjustement(MouvementStock.TypeMouvement.AJUSTEMENT_NEGATIF);
                aj.setMotif(RNG.nextBoolean() ? AjustementStock.MotifAjustement.CASSE
                                              : AjustementStock.MotifAjustement.PEREMPTION);
                aj.setObservation("Constaté lors de l'inventaire hebdomadaire");
                s.persist(aj);

                MouvementStock mvt = new MouvementStock(
                        s.getReference(Produit.class, p.id), s.getReference(Lot.class, lot.id),
                        s.getReference(User.class, adminId),
                        MouvementStock.TypeMouvement.AJUSTEMENT_NEGATIF, -qte, quand,
                        "Ajustement: " + aj.getMotif().getLabel());
                s.persist(mvt);
            }
            tx.commit();
        }
    }

    /** Crée un achat + lot « spécial » daté dans le passé (pour périmés/proches). */
    private static void creerLotSpecial(Session s, ProduitSim p, LocalDate dateAchat,
                                        LocalDate expiration, int stock) {
        Achat achat = new Achat();
        achat.setFournisseur(s.getReference(Fournisseur.class, p.fournisseurId));
        achat.setDateAchat(dateAchat.atTime(10, RNG.nextInt(50)));
        achat.setReferenceFacture(String.format("FA-%d%02d-%03d",
                dateAchat.getYear(), dateAchat.getMonthValue(), 100 + RNG.nextInt(880)));
        achat.setLignesAchat(new ArrayList<>());
        s.persist(achat);

        int boites = p.decond ? Math.max(1, stock / p.unitesParBoite + 1) : stock;
        Lot lot = new Lot();
        lot.setProduit(s.getReference(Produit.class, p.id));
        lot.setNumeroLot(String.format("LOT-%d%02d-%04d",
                dateAchat.getYear() % 100, dateAchat.getMonthValue(), compteurLot++));
        lot.setDateExpiration(expiration);
        lot.setQuantiteStock(stock);
        lot.setEstArchive(false);
        lot.setPrixAchat(p.prixAchat);
        s.persist(lot);

        LigneAchat la = new LigneAchat();
        la.setAchat(achat); la.setProduit(s.getReference(Produit.class, p.id));
        la.setLot(lot); la.setQuantiteAchetee(boites); la.setPrixUnitaire(p.prixAchat);
        s.persist(la);

        MouvementStock mvt = new MouvementStock(
                s.getReference(Produit.class, p.id), lot,
                s.getReference(User.class, adminId),
                MouvementStock.TypeMouvement.ACHAT, stock,
                achat.getDateAchat(), "Achat " + achat.getReferenceFacture());
        s.persist(mvt);

        s.flush();
        p.lots.add(new LotSim(lot.getId(), lot.getNumeroLot(), expiration, stock, p.prixAchat));
    }

    // ═══════════════ 5. ÉCRITURE DES STOCKS FINAUX ═══════════════════════

    private static void ecrireStocksFinaux() {
        System.out.println("[5/5] Écriture des stocks finaux en base...");
        try (Session s = HibernateUtil.getSessionFactory().openSession()) {
            Transaction tx = s.beginTransaction();
            for (ProduitSim p : PRODUITS) {
                for (LotSim lot : p.lots) {
                    s.createMutationQuery(
                            "UPDATE Lot SET quantiteStock = :stock, estArchive = :arch WHERE id = :id")
                        .setParameter("stock", lot.stock)
                        .setParameter("arch", lot.stock == 0)
                        .setParameter("id", lot.id)
                        .executeUpdate();
                }
            }
            tx.commit();
        }
    }

    private static void afficherResume() {
        try (Session s = HibernateUtil.getSessionFactory().openSession()) {
            for (String[] q : new String[][]{
                    {"Produits", "SELECT COUNT(*) FROM Produit"},
                    {"Fournisseurs", "SELECT COUNT(*) FROM Fournisseur"},
                    {"Lots", "SELECT COUNT(*) FROM Lot"},
                    {"Achats", "SELECT COUNT(*) FROM Achat"},
                    {"Ventes", "SELECT COUNT(*) FROM Vente"},
                    {"Lignes de vente", "SELECT COUNT(*) FROM LigneVente"},
                    {"Sessions de caisse", "SELECT COUNT(*) FROM SessionCaisse"},
                    {"Mouvements de stock", "SELECT COUNT(*) FROM MouvementStock"},
                    {"CA total (FCFA)", "SELECT COALESCE(SUM(v.total),0) FROM Vente v"}}) {
                Object val = s.createQuery(q[1], Object.class).uniqueResult();
                System.out.printf("      %-22s : %,.0f%n", q[0], ((Number) val).doubleValue());
            }
        }
    }
}

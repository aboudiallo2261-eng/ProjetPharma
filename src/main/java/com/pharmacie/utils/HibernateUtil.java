package com.pharmacie.utils;

import org.hibernate.SessionFactory;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;
import org.hibernate.service.ServiceRegistry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.pharmacie.models.*;

public class HibernateUtil {
    private static final Logger log = LoggerFactory.getLogger(HibernateUtil.class);
    private static SessionFactory sessionFactory;

    public static SessionFactory getSessionFactory() {
        if (sessionFactory == null) {
            try {
                Configuration configuration = new Configuration();
                configuration.configure("hibernate.cfg.xml");

                // Sécurité : les identifiants BDD ne sont plus dans hibernate.cfg.xml (versionné).
                // Ils sont lus depuis config.properties (gitignoré). Voir config.properties.example.
                String dbUrl  = ConfigService.getDbUrl();
                String dbUser = ConfigService.getDbUsername();
                String dbPass = ConfigService.getDbPassword();
                if (dbUrl == null || dbUser == null) {
                    throw new IllegalStateException(
                        "Identifiants BDD manquants : renseignez db.url, db.username et db.password "
                        + "dans config.properties (modèle fourni : config.properties.example).");
                }
                configuration.setProperty("connection.url", dbUrl);
                configuration.setProperty("hibernate.connection.url", dbUrl);
                configuration.setProperty("connection.username", dbUser);
                configuration.setProperty("hibernate.connection.username", dbUser);
                configuration.setProperty("connection.password", dbPass != null ? dbPass : "");
                configuration.setProperty("hibernate.connection.password", dbPass != null ? dbPass : "");

                // Mappings
                configuration.addAnnotatedClass(PharmacieInfo.class);
                configuration.addAnnotatedClass(Profil.class);
                configuration.addAnnotatedClass(User.class);
                configuration.addAnnotatedClass(Categorie.class);
                configuration.addAnnotatedClass(Espece.class);
                configuration.addAnnotatedClass(Fournisseur.class);
                
                configuration.addAnnotatedClass(Produit.class);
                configuration.addAnnotatedClass(Lot.class);
                configuration.addAnnotatedClass(Achat.class);
                configuration.addAnnotatedClass(LigneAchat.class);
                configuration.addAnnotatedClass(Vente.class);
                configuration.addAnnotatedClass(LigneVente.class);
                configuration.addAnnotatedClass(SessionCaisse.class);
                configuration.addAnnotatedClass(AjustementStock.class);
                configuration.addAnnotatedClass(MouvementStock.class);

                ServiceRegistry serviceRegistry = new StandardServiceRegistryBuilder()
                        .applySettings(configuration.getProperties()).build();

                sessionFactory = configuration.buildSessionFactory(serviceRegistry);

                // Migrations Flyway APRÈS le build de la SessionFactory :
                // hbm2ddl "update" crée d'abord les tables/colonnes manquantes,
                // puis Flyway applique les correctifs versionnés (db/migration/V*.sql).
                executerMigrationsFlyway(dbUrl, dbUser, dbPass);
            } catch (Exception e) {
                log.error("Erreur lors de la création de la SessionFactory: ", e);
                throw new RuntimeException("Erreur critique d'initialisation Hibernate", e);
            }
        }
        return sessionFactory;
    }

    /**
     * Applique les migrations de schéma versionnées (src/main/resources/db/migration).
     *
     * <p>Stratégie hybride : Hibernate "hbm2ddl.auto=update" continue de créer les
     * tables/colonnes manquantes, et Flyway gère tout ce que "update" ne sait pas
     * faire (élargissement de colonnes, backfill de données, index...).</p>
     *
     * <p><b>Règle pour la suite</b> : toute nouvelle évolution de schéma ou de données
     * doit être un fichier V&lt;n&gt;__description.sql — plus jamais de SQL dans le code.</p>
     *
     * <p>{@code baselineOnMigrate} : les bases existantes (créées avant Flyway) sont
     * automatiquement baselinées en V1 au premier démarrage, puis V2+ s'appliquent.</p>
     */
    private static void executerMigrationsFlyway(String url, String user, String password) {
        org.flywaydb.core.api.output.MigrateResult result = org.flywaydb.core.Flyway.configure()
                .dataSource(url, user, password != null ? password : "")
                .locations("classpath:db/migration")
                .baselineOnMigrate(true)
                .baselineVersion("1")
                .load()
                .migrate();
        if (result.migrationsExecuted > 0) {
            log.info("[Flyway] {} migration(s) appliquée(s). Version du schéma : {}",
                    result.migrationsExecuted, result.targetSchemaVersion);
        } else {
            log.info("[Flyway] Schéma à jour (aucune migration en attente).");
        }
    }
}

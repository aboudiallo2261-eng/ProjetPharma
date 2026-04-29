package com.pharmacie.utils;

import org.hibernate.SessionFactory;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;
import org.hibernate.service.ServiceRegistry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.pharmacie.models.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

public class HibernateUtil {
    private static final Logger log = LoggerFactory.getLogger(HibernateUtil.class);
    private static SessionFactory sessionFactory;

    public static SessionFactory getSessionFactory() {
        if (sessionFactory == null) {
            try {
                Configuration configuration = new Configuration();
                configuration.configure("hibernate.cfg.xml");

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
                // Migration corrective : Hibernate "update" n'élargit pas les colonnes existantes.
                // On force l'extension de la colonne motif à VARCHAR(255) si elle est encore trop courte.
                appliquerMigrationsCorrectivese(configuration);
            } catch (Exception e) {
                log.error("Erreur lors de la création de la SessionFactory: ", e);
                throw new RuntimeException("Erreur critique d'initialisation Hibernate", e);
            }
        }
        return sessionFactory;
    }

    /**
     * Migration corrective manuelle : Hibernate "hbm2ddl.auto=update" ne modifie
     * pas la taille des colonnes existantes (uniquement les crée si absentes).
     * Cette méthode élargit la colonne 'motif' à VARCHAR(255) si nécessaire.
     */
    private static void appliquerMigrationsCorrectivese(Configuration configuration) {
        String url      = configuration.getProperty("connection.url");
        String user     = configuration.getProperty("connection.username");
        String password = configuration.getProperty("connection.password");
        if (password == null) password = "";
        try (Connection conn = DriverManager.getConnection(url, user, password);
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(
                "ALTER TABLE ajustements_stock MODIFY COLUMN motif VARCHAR(255) NOT NULL"
            );
            log.info("[Migration] Colonne 'motif' élargie à VARCHAR(255) avec succès.");
        } catch (Exception e) {
            log.warn("[Migration] Colonne 'motif' déjà correcte ou erreur ignorée : {}", e.getMessage());
        }
    }
}

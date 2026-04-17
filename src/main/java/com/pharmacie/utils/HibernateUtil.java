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
            } catch (Exception e) {
                log.error("Erreur lors de la création de la SessionFactory: ", e);
                throw new RuntimeException("Erreur critique d'initialisation Hibernate", e);
            }
        }
        return sessionFactory;
    }
}

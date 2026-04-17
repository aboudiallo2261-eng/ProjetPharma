package com.pharmacie;

import com.pharmacie.dao.GenericDAO;
import com.pharmacie.models.User;
import com.pharmacie.models.Profil;

public class TestInsertUtilisateur {
    public static void main(String[] args) {
        System.out.println("====== TEST D'INSERTION USER ======");
        try {
            GenericDAO<User> userDAO = new GenericDAO<>(User.class);
            GenericDAO<Profil> profilDAO = new GenericDAO<>(Profil.class);

            Profil p = new Profil();
            p.setNom("TestProfil_" + System.currentTimeMillis());
            profilDAO.save(p);
            System.out.println("Profil sauvegardé avec ID: " + p.getId());

            User u = new User();
            u.setNom("TestName");
            u.setIdentifiant("testident_" + System.currentTimeMillis());
            u.setMotDePasseHash("hash");
            u.setEmail("test@test.com");
            u.setRole("AGENT");
            u.setProfil(p);

            System.out.println("Tentative de sauvegarde du User...");
            userDAO.save(u);
            System.out.println("Sauvegarde User terminée sans exception de l'appel DAO.");
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("====== FIN TEST ======");
        System.exit(0);
    }
}

import com.pharmacie.dao.GenericDAO;
import com.pharmacie.models.User;
import com.pharmacie.models.Profil;

public class TestUserInsert {
    public static void main(String[] args) {
        try {
            GenericDAO<User> userDAO = new GenericDAO<>(User.class);
            GenericDAO<Profil> profilDAO = new GenericDAO<>(Profil.class);

            Profil p = new Profil();
            p.setNom("TestProfil");
            profilDAO.save(p);

            User u = new User();
            u.setNom("TestName");
            u.setIdentifiant("testident" + System.currentTimeMillis());
            u.setMotDePasseHash("hash");
            u.setEmail("test@test.com");
            u.setProfil(p);

            System.out.println("Tentative de sauvegarde du User...");
            userDAO.save(u);
            System.out.println("Sauvegarde réussie !");
            System.exit(0);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}

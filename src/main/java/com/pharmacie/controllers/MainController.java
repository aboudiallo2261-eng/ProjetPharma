package com.pharmacie.controllers;

import com.pharmacie.MainApp;
import com.pharmacie.models.User;
import com.pharmacie.utils.SessionManager;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MainController {

    private static final Logger logger = LoggerFactory.getLogger(MainController.class);

    @FXML
    private StackPane contentArea;

    @FXML
    private Button btnDashboard;
    @FXML
    private Button btnVentes;
    @FXML
    private Button btnProduits;
    @FXML
    private Button btnAchats;
    @FXML
    private Button btnFournisseurs;
    @FXML
    private Button btnRapports;
    @FXML
    private Button btnUsers;
    
    @FXML private javafx.scene.control.Label lblUserName;
    @FXML private javafx.scene.control.Label lblUserRole;

    @FXML
    public void initialize() {
        User currentUser = SessionManager.getCurrentUser();
        if (currentUser != null && currentUser.getProfil() != null) {
            com.pharmacie.models.Profil p = currentUser.getProfil();
            
            if(lblUserName != null) lblUserName.setText(currentUser.getNom());
            if(lblUserRole != null) lblUserRole.setText(p.getNom());

            btnDashboard.setVisible(p.isCanAccessDashboard());
            btnDashboard.setManaged(p.isCanAccessDashboard());

            btnVentes.setVisible(p.isCanAccessVentes());
            btnVentes.setManaged(p.isCanAccessVentes());

            btnProduits.setVisible(p.isCanAccessStock());
            btnProduits.setManaged(p.isCanAccessStock());

            btnAchats.setVisible(p.isCanAccessAchats());
            btnAchats.setManaged(p.isCanAccessAchats());

            btnFournisseurs.setVisible(p.isCanAccessFournisseurs());
            btnFournisseurs.setManaged(p.isCanAccessFournisseurs());

            btnRapports.setVisible(p.isCanAccessRapports());
            btnRapports.setManaged(p.isCanAccessRapports());

            btnUsers.setVisible(p.isCanAccessParametres());
            btnUsers.setManaged(p.isCanAccessParametres());

            // Lancer la première vue autorisée
            if (p.isCanAccessDashboard())
                showDashboard();
            else if (p.isCanAccessVentes())
                showSales();
            else if (p.isCanAccessStock())
                showProducts();
            else if (p.isCanAccessAchats())
                showPurchases();
        } else {
            // Sécurité par défaut (empêche un plantage si profil null)
            btnDashboard.setVisible(false);
            btnDashboard.setManaged(false);
            btnUsers.setVisible(false);
            btnUsers.setManaged(false);
            btnRapports.setVisible(false);
            btnRapports.setManaged(false);
            showProducts();
        }
    }

    private void loadView(String fxmlFile) {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource(fxmlFile));
            javafx.scene.Node view = loader.load();
            contentArea.getChildren().setAll(view);
        } catch (java.io.IOException e) {
            logger.error("Erreur chargement d'une Vue (MainController)", e);
        }
    }

    private void setActiveMenu(Button activeBtn) {
        Button[] allButtons = {btnDashboard, btnVentes, btnProduits, btnAchats, btnFournisseurs, btnRapports, btnUsers};
        for (Button btn : allButtons) {
            if (btn != null) {
                btn.getStyleClass().remove("active-menu");
            }
        }
        if (activeBtn != null) {
            if (!activeBtn.getStyleClass().contains("active-menu")) {
                activeBtn.getStyleClass().add("active-menu");
            }
        }
    }

    @FXML
    public void showDashboard() {
        setActiveMenu(btnDashboard);
        loadView("/fxml/dashboard.fxml");
    }

    @FXML
    public void showSales() {
        setActiveMenu(btnVentes);
        loadView("/fxml/ventes.fxml");
    }

    @FXML
    public void showProducts() {
        setActiveMenu(btnProduits);
        loadView("/fxml/produits.fxml");
    }

    @FXML
    public void showPurchases() {
        setActiveMenu(btnAchats);
        loadView("/fxml/achats.fxml");
    }

    @FXML
    public void showFournisseurs() {
        setActiveMenu(btnFournisseurs);
        loadView("/fxml/fournisseurs.fxml");
    }

    @FXML
    public void showReports() {
        setActiveMenu(btnRapports);
        loadView("/fxml/rapports.fxml");
    }

    @FXML
    public void showUsers() {
        setActiveMenu(btnUsers);
        loadView("/fxml/users.fxml");
    }

    @FXML
    public void handleLogout() {
        if (SessionManager.getCurrentUser() != null) {
            com.pharmacie.dao.SessionCaisseDAO sessionDAO = new com.pharmacie.dao.SessionCaisseDAO();
            boolean isCaisseOpen = sessionDAO.findAll().stream()
                    .anyMatch(s -> s.getUser().getId().equals(SessionManager.getCurrentUser().getId())
                            && s.getStatut() == com.pharmacie.models.SessionCaisse.StatutSession.OUVERTE);

            if (isCaisseOpen) {
                javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                        javafx.scene.control.Alert.AlertType.ERROR);
                alert.setTitle("Déconnexion Impossible");
                alert.setHeaderText("🚨 Caisse logicielle toujours ouverte !");
                alert.setContentText(
                        "La norme de sécurité vous interdit de quitter votre poste sans justifier le tiroir-caisse.\n\nVeuillez accéder à l'onglet [Ventes] et cliquer sur [Clôturer (Z)] pour fermer proprement votre caisse.");
                alert.showAndWait();
                return; // Bloque la déconnexion
            }
        }

        SessionManager.clearSession();
        MainApp.showLoginScreen();
    }
}

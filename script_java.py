import re

filepath = r'c:\projetjavasout\src\main\java\com\pharmacie\controllers\VenteController.java'
with open(filepath, 'r', encoding='utf-8') as f:
    content = f.read()

imports = "import javafx.scene.control.TabPane;\nimport javafx.animation.FadeTransition;\nimport javafx.util.Duration;\n"
if "import javafx.scene.control.TabPane;" not in content:
    content = content.replace("import javafx.scene.control.TableView;", imports + "import javafx.scene.control.TableView;")

if "@FXML\n    private TabPane tabPaneMain;" not in content:
    content = content.replace(
        "@FXML\n    private TableView<Produit> tableStock;",
        "@FXML\n    private TabPane tabPaneMain;\n    @FXML\n    private TableView<Produit> tableStock;"
    )

init_anim = """        if (tabPaneMain != null) {
            tabPaneMain.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
                if (newTab != null && newTab.getContent() != null) {
                    FadeTransition ft = new FadeTransition(Duration.millis(250), newTab.getContent());
                    ft.setFromValue(0.0);
                    ft.setToValue(1.0);
                    ft.play();
                }
            });
        }
"""
if "tabPaneMain.getSelectionModel()" not in content:
    content = content.replace(
        "private void initialiserColonnesStock() {",
        init_anim + "\n    private void initialiserColonnesStock() {"
    )

with open(filepath, 'w', encoding='utf-8') as f:
    f.write(content)
print("Done Controller Update")

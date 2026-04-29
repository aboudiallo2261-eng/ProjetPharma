import re

filepath = r'c:\projetjavasout\src\main\resources\fxml\ventes.fxml'
with open(filepath, 'r', encoding='utf-8') as f:
    content = f.read()

content = content.replace(
    '<TabPane VBox.vgrow="ALWAYS" tabClosingPolicy="UNAVAILABLE"',
    '<TabPane fx:id="tabPaneMain" VBox.vgrow="ALWAYS" tabClosingPolicy="UNAVAILABLE"'
)

with open(filepath, 'w', encoding='utf-8') as f:
    f.write(content)
print("Done FXML ID Add")

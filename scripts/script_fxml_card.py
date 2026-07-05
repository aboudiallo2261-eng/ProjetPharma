import re

filepath = r'c:\projetjavasout\src\main\resources\fxml\ventes.fxml'
with open(filepath, 'r', encoding='utf-8') as f:
    content = f.read()

# Replace VBox style with card class
old_style_1 = 'style="-fx-background-color: white; -fx-padding: 20; -fx-background-radius: 8; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 10, 0, 0, 5);"'
new_style_1 = 'styleClass="card" style="-fx-padding: 20;"'
content = content.replace(old_style_1, new_style_1)

old_style_2 = 'style="-fx-background-color: white; -fx-padding: 15; -fx-background-radius: 8; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 10, 0, 0, 5);"'
new_style_2 = 'styleClass="card" style="-fx-padding: 15;"'
content = content.replace(old_style_2, new_style_2)

old_style_3 = 'style="-fx-padding: 10; -fx-background-color: #F8FAFC; -fx-background-radius: 8;"'
new_style_3 = 'style="-fx-padding: 10; -fx-background-color: transparent;"'
content = content.replace(old_style_3, new_style_3)

with open(filepath, 'w', encoding='utf-8') as f:
    f.write(content)
print("Done FXML Script")

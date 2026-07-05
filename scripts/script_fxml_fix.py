import re

filepath = r'c:\projetjavasout\src\main\resources\fxml\ventes.fxml'
with open(filepath, 'r', encoding='utf-8') as f:
    content = f.read()

# Fix commas in styleClass and add minWidth to Stock column
content = content.replace(
    '<TableColumn fx:id="colStkQte" text="Stock (Unités min)" prefWidth="180" />',
    '<TableColumn fx:id="colStkQte" text="Stock (Unités min)" prefWidth="180" minWidth="150" />'
)

# Replace all occurrences of styleClass="icon, icon-xxx, icon-dark" with spaces
content = re.sub(
    r'styleClass="icon,\s*([^",]+),\s*icon-dark"',
    r'styleClass="icon \1 icon-dark"',
    content
)
# And the ones that just had one comma
content = re.sub(
    r'styleClass="icon,\s*([^",]+)"',
    r'styleClass="icon \1"',
    content
)

with open(filepath, 'w', encoding='utf-8') as f:
    f.write(content)
print("Done FXML fix")

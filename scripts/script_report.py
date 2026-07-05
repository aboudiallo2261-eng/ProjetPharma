import re

filepath = r'c:\projetjavasout\src\main\java\com\pharmacie\controllers\ReportController.java'
with open(filepath, 'r', encoding='utf-8') as f:
    content = f.read()

# Replace selected white text in ACHAT/VENTE
content = re.sub(
    r'setStyle\(selected \? "-fx-text-fill: white; -fx-font-weight: bold;"\s*: "-fx-text-fill: #27ae60; -fx-font-weight: bold;"\);',
    'setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");',
    content
)
content = re.sub(
    r'setStyle\(selected \? "-fx-text-fill: white; -fx-font-weight: bold;"\s*: "-fx-text-fill: #2980b9; -fx-font-weight: bold;"\);',
    'setStyle("-fx-text-fill: #2980b9; -fx-font-weight: bold;");',
    content
)
content = re.sub(
    r'setStyle\(selected \? "-fx-text-fill: white; -fx-font-weight: bold;"\s*: "-fx-text-fill: #c0392b; -fx-font-weight: bold;"\);',
    'setStyle("-fx-text-fill: #c0392b; -fx-font-weight: bold;");',
    content
)

# Replace setTextFill(Color.WHITE)
content = re.sub(
    r'if \(selected\) \{\s*setTextFill\(Color\.WHITE\);\s*setStyle\("-fx-font-weight: bold; -fx-alignment: CENTER-RIGHT; -fx-padding: 0 10 0 0;"\);\s*\}',
    'if (selected) {\n                    setTextFill(Color.web("#0F172A"));\n                    setStyle("-fx-font-weight: bold; -fx-alignment: CENTER-RIGHT; -fx-padding: 0 10 0 0;");\n                }',
    content
)

# Also fix setStyle for strings
content = re.sub(
    r'if \(selected\) \{\s*setStyle\("-fx-text-fill: white;"\);\s*\}',
    'if (selected) {\n                    setStyle("-fx-text-fill: #0F172A;");\n                }',
    content
)

# Find any other setTextFill(Color.WHITE)
content = content.replace("setTextFill(Color.WHITE);", "setTextFill(Color.web(\"#0F172A\"));")

with open(filepath, 'w', encoding='utf-8') as f:
    f.write(content)
print("Done ReportController script")

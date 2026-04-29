import re

filepath = r'c:\projetjavasout\src\main\resources\css\styles.css'
with open(filepath, 'r', encoding='utf-8') as f:
    content = f.read()

# 1. Bold borders for ALL inputs
# Find .text-field, .password-field, .combo-box { block and add -fx-border-width: 1.5px; and -fx-border-color: #94A3B8;
content = re.sub(
    r'(\.text-field, \.password-field, \.combo-box \{[^\}]+)-fx-border-color: #CBD5E1;',
    r'\1-fx-border-color: #94A3B8;\n    -fx-border-width: 1.5px;',
    content
)
# Remove the custom combo-box border block added previously so it doesn't duplicate
content = re.sub(r'/\* Contours des ComboBox.*?\*/\s*\.combo-box\s*\{.*?\n\}', '', content, flags=re.DOTALL)

# 2. Bold table headers and dashboard card titles
content = re.sub(
    r'(\.table-view \.column-header \.label \{[^\}]+)-fx-font-weight: bold;',
    r'\1-fx-font-weight: 900;\n    -fx-text-fill: #0F172A;',
    content
)
if '.kpi-title' not in content:
    content += "\n.kpi-title, .section-title, .dashboard-title { -fx-font-weight: 900; -fx-text-fill: #0F172A; }\n"

# 3. Bold text on selected row
content = re.sub(
    r'(\.table-row-cell:selected \.text \{[^\}]+)-fx-fill: #0F172A;',
    r'\1-fx-fill: #0F172A;\n    -fx-font-weight: bold;',
    content
)

# 5. Tab text size 14
content = re.sub(
    r'(\.tab-pane \.tab-label \{[^\}]+)-fx-font-weight: bold;',
    r'\1-fx-font-weight: bold;\n    -fx-font-size: 14px;',
    content
)

with open(filepath, 'w', encoding='utf-8') as f:
    f.write(content)
print("Done CSS Script")

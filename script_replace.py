import re

filepath = r'c:\projetjavasout\src\main\resources\fxml\ventes.fxml'
with open(filepath, 'r', encoding='utf-8') as f:
    content = f.read()

# Dictionary of emoji to (Text, IconClass)
emoji_map = {
    '🔑 Ouvrir': ('Ouvrir', 'icon-key'),
    '🔒 Clôturer (Z)': ('Clôturer (Z)', 'icon-lock'),
    '🔐 Verrouil.': ('Verrouil.', 'icon-lock'),
    '⏸ Attente (0)': ('Attente (0)', 'icon-pause'),
    '🗵 Tout': ('Tout', 'icon-x'),
    '➕ Ajouter': ('Ajouter', 'icon-plus'),
    '⏸ Attente': ('Attente', 'icon-pause'),
    '✖ Retirer': ('Retirer', 'icon-x'),
    '✖ Annuler': ('Annuler', 'icon-x'),
    '✔ Valider': ('Valider', 'icon-check'),
    '🔓 Déverrouiller': ('Déverrouiller', 'icon-unlock'),
    '🔍 Filtrer': ('Filtrer', 'icon-search'),
    '🔄 Réinitialiser': ('Réinitialiser', 'icon-refresh'),
    '🖨 Réimprimer le Reçu': ('Réimprimer le Reçu', 'icon-printer'),
    '👁 Voir Détails': ('Voir Détails', 'icon-eye'),
    '🖨 Imprimer Récapitulatif PDF': ('Imprimer Récapitulatif PDF', 'icon-printer'),
    '👁': ('', 'icon-eye')
}

# Regex to find <Button ... text="[emoji_map_key]" ... />
for emoji_text, (clean_text, icon_class) in emoji_map.items():
    # Find the button that has this text and ends with />
    pattern = r'(<Button[^>]+?text="' + re.escape(emoji_text) + r'"[^>]*?)(/>)'
    
    def repl_func(match):
        start_tag = match.group(1)
        # Replace the text inside the matched start_tag
        start_tag = start_tag.replace('text="' + emoji_text + '"', 'text="' + clean_text + '"')
        
        # Add styleClass="pos-btn" if there is a style attribute
        if 'style="' in start_tag:
            start_tag = start_tag.replace('style="', 'styleClass="pos-btn" style="')
        elif 'styleClass="' in start_tag:
            start_tag = start_tag.replace('styleClass="', 'styleClass="pos-btn, ')
        else:
            start_tag += ' styleClass="pos-btn"'
            
        return f'{start_tag}>\n    <graphic><Region styleClass="icon, {icon_class}"/></graphic>\n</Button>'
        
    content = re.sub(pattern, repl_func, content)

# specific labels
content = content.replace('promptText="🔍 Nom du produit..."', 'promptText="Nom du produit..."')
content = content.replace('<Label text="🔒" style="-fx-font-size: 50px;"/>', '<Region styleClass="icon, icon-lock, icon-dark" style="-fx-min-width:50px; -fx-min-height:50px;"/>')
content = content.replace('<Label text="🔐" style="-fx-font-size: 60px;"/>', '<Region styleClass="icon, icon-lock, icon-dark" style="-fx-min-width:60px; -fx-min-height:60px; -fx-background-color: white;"/>')

content = content.replace(
    '<Label text="🔍 Aucun produit trouvé ou disponible" style="-fx-text-fill: #94A3B8; -fx-font-style: italic; -fx-font-size: 14px;" />',
    '<VBox alignment="CENTER" spacing="10"><Region styleClass="icon, icon-search, icon-dark" style="-fx-min-width:32px; -fx-min-height:32px;"/><Label text="Aucun produit trouvé ou disponible" style="-fx-text-fill: #94A3B8; -fx-font-style: italic; -fx-font-size: 14px;" /></VBox>'
)
content = content.replace(
    '<Label text="🛒 Le panier est vide pour le moment" style="-fx-text-fill: #94A3B8; -fx-font-style: italic; -fx-font-size: 14px;" />',
    '<VBox alignment="CENTER" spacing="10"><Region styleClass="icon, icon-cart, icon-dark" style="-fx-min-width:32px; -fx-min-height:32px;"/><Label text="Le panier est vide pour le moment" style="-fx-text-fill: #94A3B8; -fx-font-style: italic; -fx-font-size: 14px;" /></VBox>'
)
content = content.replace(
    '<Label text="📭 Aucune vente enregistrée pour cette période." style="-fx-text-fill: #94A3B8; -fx-font-style: italic; -fx-font-size: 14px;" />',
    '<VBox alignment="CENTER" spacing="10"><Region styleClass="icon, icon-doc, icon-dark" style="-fx-min-width:32px; -fx-min-height:32px;"/><Label text="Aucune vente enregistrée pour cette période." style="-fx-text-fill: #94A3B8; -fx-font-style: italic; -fx-font-size: 14px;" /></VBox>'
)

with open(filepath, 'w', encoding='utf-8') as f:
    f.write(content)
print("Done Python Script")

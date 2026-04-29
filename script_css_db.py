import re

filepath = r'c:\projetjavasout\src\main\resources\css\styles.css'
with open(filepath, 'r', encoding='utf-8') as f:
    content = f.read()

database_icon = '\n.icon-database { -fx-shape: "M448 80v48c0 44.2-100.3 80-224 80S0 172.2 0 128V80C0 35.8 100.3 0 224 0S448 35.8 448 80zM393.2 214.7c20.8-7.4 39.2-16.9 54.8-28.6V288c0 44.2-100.3 80-224 80S0 332.2 0 288V186.1c15.6 11.8 34 21.2 54.8 28.6C112.7 236.4 165.2 248 224 248s111.3-11.6 169.2-33.3zM0 346.1c15.6 11.8 34 21.2 54.8 28.6C112.7 396.4 165.2 408 224 408s111.3-11.6 169.2-33.3c20.8-7.4 39.2-16.9 54.8-28.6V432c0 44.2-100.3 80-224 80S0 476.2 0 432V346.1z"; }'

if '.icon-database' not in content:
    content = content.replace('.icon-doc { -fx-shape: "M224 136V0H24C10.7 0 0 10.7 0 24v464c0 13.3 10.7 24 24 24h336c13.3 0 24-10.7 24-24V160H248c-13.2 0-24-10.8-24-24zm160-14.1v6.1H256V0h6.1c6.4 0 12.5 2.5 17 7l97.9 98c4.5 4.5 7 10.6 7 16.9z"; }',
        '.icon-doc { -fx-shape: "M224 136V0H24C10.7 0 0 10.7 0 24v464c0 13.3 10.7 24 24 24h336c13.3 0 24-10.7 24-24V160H248c-13.2 0-24-10.8-24-24zm160-14.1v6.1H256V0h6.1c6.4 0 12.5 2.5 17 7l97.9 98c4.5 4.5 7 10.6 7 16.9z"; }' + database_icon)

with open(filepath, 'w', encoding='utf-8') as f:
    f.write(content)
print("Done CSS")

# Procédure de restauration de la base de données

> Testée et validée le 2026-07-05 (backup du 2026-06-21 restauré avec succès :
> 15 tables, 198 ventes, 27 produits, 67 lots, 7 utilisateurs).

## Quand l'utiliser

- Disque ou PC de la pharmacie hors service
- Base de données corrompue
- Retour à un état antérieur après une erreur grave de manipulation

## Prérequis

- Un fichier de sauvegarde `.sql` (dossier `backups/`, clé USB configurée, ou Google Drive)
- MySQL en fonctionnement (WAMP : icône verte)
- Le client `mysql.exe` (fourni avec WAMP : `C:\wamp64\bin\mysql\mysql9.1.0\bin\mysql.exe`)

## Étapes (PowerShell)

```powershell
$MYSQL = "C:\wamp64\bin\mysql\mysql9.1.0\bin\mysql.exe"
$BACKUP = "C:\projetjavasout\backups\pharmacie_backup_AAAA-MM-JJ_HH-MM.sql"   # ← adapter

# 1. TOUJOURS tester d'abord dans une base temporaire
& $MYSQL -u root -e "CREATE DATABASE pharmacie_vet_restore_test;"
Get-Content $BACKUP | & $MYSQL -u root pharmacie_vet_restore_test

# 2. Vérifier le contenu (nombre de ventes, produits, dernier ticket...)
& $MYSQL -u root -e "SELECT COUNT(*) FROM pharmacie_vet_restore_test.ventes;"

# 3. Si le contenu est bon : restaurer en production
#    ATTENTION : ceci ÉCRASE la base actuelle. Faire un dump de l'état courant avant.
& $MYSQL -u root -e "DROP DATABASE pharmacie_vet_db; CREATE DATABASE pharmacie_vet_db;"
Get-Content $BACKUP | & $MYSQL -u root pharmacie_vet_db

# 4. Nettoyer la base de test
& $MYSQL -u root -e "DROP DATABASE pharmacie_vet_restore_test;"
```

## Après restauration

1. Relancer l'application et vérifier : dernier ticket de vente, état du stock, comptes utilisateurs.
2. Si la sauvegarde est antérieure au jour même, **re-saisir les ventes manquantes**.
3. Vérifier que l'utilisateur MySQL `vetpharma_app` a toujours ses droits :
   ```sql
   GRANT ALL PRIVILEGES ON pharmacie_vet_db.* TO 'vetpharma_app'@'localhost';
   ```

## Règles d'or

- **Tester une restauration au moins une fois par trimestre** (dans la base temporaire).
- Une sauvegarde quotidienne en fin de journée (clôture de caisse) : vérifier la date du
  dernier fichier dans `backups/` chaque semaine.
- Garder au moins une copie HORS du PC (USB + Google Drive).

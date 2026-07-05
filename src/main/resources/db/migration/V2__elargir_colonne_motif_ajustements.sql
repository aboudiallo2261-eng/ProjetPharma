-- Migration corrective historique : Hibernate "hbm2ddl.auto=update" n'élargit pas
-- les colonnes existantes. On force motif à VARCHAR(255).
-- (Sans effet si la colonne est déjà à la bonne taille — l'ALTER est idempotent.)
ALTER TABLE ajustements_stock MODIFY COLUMN motif VARCHAR(255) NOT NULL;

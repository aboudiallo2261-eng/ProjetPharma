-- Rétrocompatibilité : peupler lots.prix_achat pour les lots créés avant
-- l'introduction de cette colonne.
-- Source primaire : lignes_achat.prixUnitaire (prix RÉEL payé pour ce lot).
UPDATE lots l
INNER JOIN lignes_achat la ON la.lot_id = l.id
SET l.prix_achat = la.prixUnitaire
WHERE l.prix_achat IS NULL;

-- Fallback : produits.prixAchat (dernier prix connu) pour les lots orphelins.
UPDATE lots l
INNER JOIN produits p ON l.produit_id = p.id
SET l.prix_achat = p.prixAchat
WHERE l.prix_achat IS NULL AND p.prixAchat IS NOT NULL AND p.prixAchat > 0;

/**
 * Formate un nombre en FCFA avec des espaces comme séparateurs de milliers.
 * Ex: 1381228469 => "1 381 228 469 FCFA"
 * Utilise un espace insécable étroit (\u202F) pour garantir la compatibilité mobile.
 */
export const formatFCFA = (val) => {
  const n = isNaN(val) || val === null || val === undefined ? 0 : Number(val);
  // On force l'espace insécable étroit pour tous les navigateurs (mobile inclus)
  const formatted = n
    .toFixed(0)
    .replace(/\B(?=(\d{3})+(?!\d))/g, '\u00A0'); // espace insécable standard
  return formatted + ' FCFA';
};

/**
 * Formate un nombre simple (quantité) avec des séparateurs de milliers.
 * Ex: 12500 => "12 500"
 */
export const formatNumber = (val) => {
  const n = isNaN(val) || val === null || val === undefined ? 0 : Number(val);
  return n
    .toFixed(0)
    .replace(/\B(?=(\d{3})+(?!\d))/g, '\u00A0');
};

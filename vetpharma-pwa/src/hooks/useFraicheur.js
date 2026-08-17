import { useEffect, useState } from 'react';

/**
 * Fraîcheur des données affichées.
 *
 * Un tableau de bord de supervision à distance n'a de valeur que si le lecteur
 * sait à quel moment ce qu'il lit a été constaté. Sans cette information, un
 * poste éteint depuis trois jours produit exactement le même écran qu'une
 * pharmacie calme : chiffres bas, aucune alerte nouvelle, tout paraît en ordre.
 * Le silence d'une panne et le silence d'une journée sereine sont indiscernables.
 *
 * Ce module rend cette distinction visible.
 */

const MINUTE = 60 * 1000;
const HEURE  = 60 * MINUTE;
const JOUR   = 24 * HEURE;

/**
 * Seuils choisis sur le rythme réel d'une officine, pas sur des chiffres ronds.
 *
 * Le poste synchronise à intervalle régulier tant que le logiciel tourne, et à
 * chaque vente encaissée. Deux heures sans le moindre signal restent explicables
 * un jour ouvré — pause, fermeture méridienne, coupure réseau passagère — mais
 * ce n'est plus le fonctionnement nominal : le vert n'est plus mérité.
 *
 * Douze heures ne s'expliquent plus par aucune journée d'ouverture normale. À ce
 * stade le poste est éteint, hors ligne, ou le logiciel est fermé. Continuer à
 * présenter ces chiffres comme l'état courant de la pharmacie serait faux.
 */
export const SEUIL_ATTENTION = 2 * HEURE;
export const SEUIL_PERIME    = 12 * HEURE;

/**
 * Durée en français courant : « il y a 8 minutes », « depuis 3 jours ».
 * Le préfixe est paramétrable car on lit « mis à jour il y a 3 heures » mais
 * « figées depuis 3 jours » — même durée, deux tournures.
 */
export function formaterAge(ms, prefixe = 'il y a') {
  if (ms < MINUTE) return "à l'instant";

  const [valeur, unite] =
    ms < HEURE ? [Math.floor(ms / MINUTE), 'minute'] :
    ms < JOUR  ? [Math.floor(ms / HEURE),  'heure']  :
                 [Math.floor(ms / JOUR),   'jour'];

  return `${prefixe} ${valeur} ${unite}${valeur > 1 ? 's' : ''}`;
}

/**
 * Évalue l'âge de la dernière synchronisation reçue.
 *
 * @param {string|null} lastSync horodatage ISO écrit par le poste de la pharmacie
 * @returns {{niveau: 'inconnu'|'frais'|'attention'|'perime', age: number|null,
 *            texte: string, resume: string}}
 */
export function useFraicheur(lastSync) {
  const [maintenant, setMaintenant] = useState(() => Date.now());

  useEffect(() => {
    // L'âge doit continuer de croître tout seul. Sans cette horloge, un onglet
    // laissé ouvert la nuit afficherait encore au matin l'âge calculé la veille.
    const battement = setInterval(() => setMaintenant(Date.now()), MINUTE);

    // Le rythme des minuteurs est bridé par les navigateurs sur un onglet caché,
    // et suspendu quand le téléphone dort. On recale donc au retour à l'écran :
    // c'est précisément l'instant où l'utilisateur lit la valeur.
    const recaler = () => { if (!document.hidden) setMaintenant(Date.now()); };
    document.addEventListener('visibilitychange', recaler);

    return () => {
      clearInterval(battement);
      document.removeEventListener('visibilitychange', recaler);
    };
  }, []);

  if (!lastSync) {
    return {
      niveau: 'inconnu',
      age: null,
      texte: 'En attente de synchronisation',
      resume: 'Aucune donnée reçue de la pharmacie',
    };
  }

  const horodatage = new Date(lastSync).getTime();
  if (Number.isNaN(horodatage)) {
    return {
      niveau: 'inconnu',
      age: null,
      texte: 'Date de synchronisation illisible',
      resume: 'Date de synchronisation illisible',
    };
  }

  // Une horloge locale en avance sur le serveur donnerait un âge négatif :
  // on le ramène à zéro plutôt que d'afficher une durée absurde.
  const age = Math.max(0, maintenant - horodatage);

  const niveau =
    age >= SEUIL_PERIME    ? 'perime'    :
    age >= SEUIL_ATTENTION ? 'attention' :
                             'frais';

  return { niveau, age, texte: formaterAge(age), resume: formaterAge(age, 'depuis') };
}

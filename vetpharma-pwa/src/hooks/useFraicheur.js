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
 * Au-delà de trois jours, même une fermeture régulière mérite d'être signalée.
 * Une caisse close suivie d'une panne du poste produit un silence parfaitement
 * innocent en apparence ; passé ce délai, l'absence prolongée d'activité vaut
 * qu'on s'assure qu'elle est bien voulue.
 */
export const SEUIL_FERMETURE_LONGUE = 72 * HEURE;

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

/** Heure courte d'un horodatage ISO : « samedi 19:42 ». */
function momentCourt(iso) {
  if (!iso) return null;
  const d = new Date(iso);
  if (Number.isNaN(d.getTime())) return null;
  const jour = d.toLocaleDateString('fr-FR', { weekday: 'long' });
  const heure = d.toLocaleTimeString('fr-FR', { hour: '2-digit', minute: '2-digit' });
  return `${jour} ${heure}`;
}

/**
 * Évalue l'âge de la dernière synchronisation reçue.
 *
 * Trois formulations de la même durée sont exposées, car les écrans ne les
 * emploient pas au même endroit d'une phrase : `texte` est une phrase complète
 * et contextuelle, `resume` s'accroche après un verbe, `ageTexte` reste l'âge
 * nu pour les écrans qui composent leur propre phrase.
 *
 * Le second argument change la lecture du silence. Sans lui, tout silence
 * prolongé est traité comme une anomalie — ce qui déclencherait une alerte
 * chaque lundi matin dans une officine fermée le dimanche, jusqu'à ce que
 * l'alerte ne soit plus lue. Avec lui, le statut de la dernière session tranche :
 * une caisse close signe une fin de journée normale, une caisse restée ouverte
 * signe une clôture qui n'a pas eu lieu.
 *
 * L'argument est facultatif, et son absence rétablit l'ancien comportement :
 * un poste qui n'a pas encore reçu la mise à jour continue d'alimenter un
 * tableau de bord cohérent, simplement moins fin.
 *
 * @param {string|null} lastSync horodatage ISO écrit par le poste de la pharmacie
 * @param {object|null} caisse bloc « caisse » du snapshot, s'il est présent
 * @returns {{niveau: 'inconnu'|'frais'|'fermee'|'attention'|'perime', age: number|null,
 *            texte: string, resume: string, caisseOuverte: boolean, agent: string|null}}
 */
export function useFraicheur(lastSync, caisse = null) {
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

  const derniere = caisse?.derniere || null;
  const caisseOuverte = derniere?.statut === 'OUVERTE';
  const caisseClose = derniere?.statut === 'FERMEE';
  const agent = derniere?.agent || null;

  const inconnu = (texte) => ({
    niveau: 'inconnu', age: null, texte, resume: texte,
    ageTexte: texte, caisseOuverte: false, agent: null,
  });

  if (!lastSync) return inconnu('En attente de synchronisation');

  const horodatage = new Date(lastSync).getTime();
  if (Number.isNaN(horodatage)) return inconnu('Date de synchronisation illisible');

  // Une horloge locale en avance sur le serveur donnerait un âge négatif :
  // on le ramène à zéro plutôt que d'afficher une durée absurde.
  const age = Math.max(0, maintenant - horodatage);
  const base = { age, caisseOuverte, agent, ageTexte: formaterAge(age) };

  // Le poste synchronise toutes les cinq minutes tant qu'il tourne : en deçà de
  // deux heures, rien ne mérite d'être signalé, caisse ouverte ou non.
  if (age < SEUIL_ATTENTION) {
    return { ...base, niveau: 'frais', texte: formaterAge(age), resume: formaterAge(age, 'depuis') };
  }

  if (caisseClose) {
    const fermeeDepuis = momentCourt(derniere.dateCloture) || formaterAge(age);
    if (age < SEUIL_FERMETURE_LONGUE) {
      return {
        ...base,
        niveau: 'fermee',
        texte: `Pharmacie fermée depuis ${fermeeDepuis}`,
        resume: `fermée depuis ${fermeeDepuis}`,
      };
    }
    // La fermeture s'éternise : sans accuser une panne, on cesse de la traiter
    // comme un état parfaitement banal.
    return {
      ...base,
      niveau: 'attention',
      texte: `Aucune activité ${formaterAge(age, 'depuis')} — dernière clôture ${fermeeDepuis}`,
      resume: `sans activité ${formaterAge(age)}`,
    };
  }

  // Caisse restée ouverte, ou statut inconnu (poste pas encore mis à jour) :
  // le silence n'est expliqué par rien, on remonte la gravité avec l'âge.
  const niveau = age >= SEUIL_PERIME ? 'perime' : 'attention';
  const texte = caisseOuverte
    ? `Caisse ouverte, aucune donnée ${formaterAge(age, 'depuis')}`
    : formaterAge(age);

  return { ...base, niveau, texte, resume: formaterAge(age, 'depuis') };
}

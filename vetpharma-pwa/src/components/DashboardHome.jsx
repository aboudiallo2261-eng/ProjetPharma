import React from 'react';
import { DollarSign, Receipt, PackageX, AlertTriangle, Clock, Wallet,
         HeartCrack, BarChart2, Ban, CheckCircle2, ArrowRight, TrendingUp, TrendingDown } from 'lucide-react';
import { formatFCFA } from '../lib/formatters';
import { useFraicheur } from '../hooks/useFraicheur';

/**
 * Écran d'accueil — conçu pour répondre en quelques secondes à trois questions :
 *   1. Est-ce que ma pharmacie va bien aujourd'hui ?
 *   2. Qu'est-ce qui demande une action de ma part, et dans quel ordre ?
 *   3. Combien d'argent est en jeu ?
 *
 * L'ancienne version présentait tous les indicateurs au même niveau visuel : le
 * propriétaire devait lire dix chiffres, faire ses additions et deviner ses
 * priorités. Ici, l'information est hiérarchisée par ce qu'elle implique.
 */

/** Bandeau de synthèse : le verdict global, visible dès l'ouverture. */
function Verdict({ gravite, titre, sousTitre }) {
  const styles = {
    critique:  { fond: 'rgba(220,38,38,0.12)',  bord: 'rgba(220,38,38,0.35)',  couleur: '#f87171', Icone: Ban },
    attention: { fond: 'rgba(245,158,11,0.10)', bord: 'rgba(245,158,11,0.30)', couleur: '#fbbf24', Icone: AlertTriangle },
    ok:        { fond: 'rgba(16,185,129,0.10)', bord: 'rgba(16,185,129,0.30)', couleur: '#34d399', Icone: CheckCircle2 },
  }[gravite];
  const { Icone } = styles;

  return (
    <div className="mx-4 mt-2 mb-5 rounded-2xl p-4 flex items-start gap-3"
      style={{ background: styles.fond, border: `1px solid ${styles.bord}` }}>
      <Icone className="w-6 h-6 shrink-0 mt-0.5" style={{ color: styles.couleur }} />
      <div className="min-w-0">
        <p className="text-base font-bold leading-tight" style={{ color: styles.couleur }}>{titre}</p>
        <p className="text-xs text-slate-300 mt-1 leading-relaxed">{sousTitre}</p>
      </div>
    </div>
  );
}

/** Une action à mener, numérotée par ordre de priorité. */
function Action({ rang, titre, detail, montant, couleur, icon: Icon }) {
  return (
    <div className="flex items-center gap-3 py-3 border-b border-white/5 last:border-0">
      <div className="w-7 h-7 rounded-lg flex items-center justify-center shrink-0 text-xs font-black"
        style={{ background: `${couleur}1f`, color: couleur }}>
        {rang}
      </div>
      <div className="flex-1 min-w-0">
        <p className="text-sm font-semibold text-white leading-tight">{titre}</p>
        {detail && <p className="text-[11px] text-slate-400 mt-0.5">{detail}</p>}
      </div>
      {montant != null && (
        <span className="text-sm font-bold shrink-0" style={{ color: couleur }}>{formatFCFA(montant)}</span>
      )}
      <Icon className="w-4 h-4 shrink-0" style={{ color: couleur }} />
    </div>
  );
}

function KpiCard({ label, value, sousTexte, icon: Icon, color }) {
  return (
    <div className="rounded-2xl p-4 flex flex-col"
      style={{ background: 'rgba(255,255,255,0.03)', border: '1px solid rgba(255,255,255,0.06)' }}>
      <div className="flex justify-between items-start mb-2">
        <p className="text-[10px] sm:text-xs font-semibold uppercase tracking-wider text-slate-400">{label}</p>
        <div className="w-6 h-6 rounded-md flex items-center justify-center shrink-0" style={{ background: `${color}15` }}>
          <Icon className="w-3 h-3" style={{ color }} />
        </div>
      </div>
      <p className="text-lg sm:text-xl font-bold text-white leading-tight">{value}</p>
      {sousTexte && <p className="text-[10px] text-slate-500 mt-1 leading-tight">{sousTexte}</p>}
    </div>
  );
}

function LigneMontant({ label, value, icon: Icon, color, valueColor = 'white', accent }) {
  return (
    <div className="flex items-center justify-between py-2.5 border-b border-white/5 last:border-0">
      <div className="flex items-center gap-3 min-w-0">
        <div className="w-8 h-8 rounded-lg flex items-center justify-center shrink-0" style={{ background: `${color}15` }}>
          <Icon className="w-4 h-4" style={{ color }} />
        </div>
        <span className={`text-sm font-medium ${accent ? 'text-white' : 'text-slate-300'}`}>{label}</span>
      </div>
      <span className={`${accent ? 'text-base' : 'text-base'} font-bold shrink-0`} style={{ color: valueColor }}>{value}</span>
    </div>
  );
}

export default function DashboardHome({ data, lastSync }) {
  const fraicheur = useFraicheur(lastSync);
  const kpis = data?.kpis || {};
  const stock = kpis.stock || {};
  const jour = kpis.jour || {};
  const mois = kpis.mois || {};

  const nbPerimes  = stock.nombrePerimes || 0;
  const nbRuptures = stock.nombreRuptures || 0;
  const nbAlertes  = stock.nombreAlerteStock || 0;
  const nbProches  = stock.nombreProchePeremption || 0;

  const valeurPerimes  = stock.valeurPerimes || 0;
  const valeurARisque  = stock.valeurARisque || 0;
  // Somme calculée POUR le propriétaire : il devait auparavant additionner
  // mentalement une perte déjà subie et une exposition future.
  const expositionTotale = valeurPerimes + valeurARisque;

  const ventesJour = jour.ventesRealisees || 0;
  const caJour     = jour.chiffreAffaire || 0;
  const evolution  = jour.evolutionCA || 0;
  const journeeVide = ventesJour === 0;

  // ── Actions, classées par ce qu'elles coûtent si on les ignore ──────────
  const actions = [];
  if (nbPerimes > 0) {
    actions.push({ titre: `Retirer ${nbPerimes} lot${nbPerimes > 1 ? 's' : ''} périmé${nbPerimes > 1 ? 's' : ''} des rayons`,
      detail: 'Invendables — risque sanitaire et légal', montant: valeurPerimes, couleur: '#f87171', icon: Ban });
  }
  if (nbRuptures > 0) {
    actions.push({ titre: `Commander ${nbRuptures} produit${nbRuptures > 1 ? 's' : ''} en rupture`,
      detail: 'Ventes perdues tant que le stock est vide', couleur: '#f87171', icon: PackageX });
  }
  if (nbProches > 0) {
    actions.push({ titre: `Écouler ${nbProches} lot${nbProches > 1 ? 's' : ''} expirant sous 60 jours`,
      detail: 'Encore vendables — à prioriser', montant: valeurARisque, couleur: '#fbbf24', icon: Clock });
  }
  if (nbAlertes > 0) {
    actions.push({ titre: `Réapprovisionner ${nbAlertes} produit${nbAlertes > 1 ? 's' : ''} sous le seuil`,
      detail: 'Rupture imminente', couleur: '#fbbf24', icon: AlertTriangle });
  }

  // ── Verdict global ──────────────────────────────────────────────────────
  let verdict;
  if (nbPerimes > 0 || nbRuptures > 0) {
    verdict = {
      gravite: 'critique',
      titre: `${actions.length} action${actions.length > 1 ? 's' : ''} à mener aujourd'hui`,
      sousTitre: expositionTotale > 0
        ? `${formatFCFA(expositionTotale)} sont en jeu sur votre stock, dont ${formatFCFA(valeurPerimes)} déjà perdus.`
        : 'Des produits demandent une intervention immédiate.',
    };
  } else if (nbProches > 0 || nbAlertes > 0) {
    verdict = {
      gravite: 'attention',
      titre: 'Situation sous contrôle, points à surveiller',
      sousTitre: `Aucune perte constatée. ${formatFCFA(valeurARisque)} à écouler avant péremption.`,
    };
  } else {
    verdict = {
      gravite: 'ok',
      titre: 'Tout est en ordre',
      sousTitre: 'Aucune rupture, aucun lot périmé, aucune péremption proche.',
    };
  }

  // ── Ce que le verdict peut honnêtement affirmer ─────────────────────────
  // Une pharmacie sans alerte et une pharmacie dont on n'a plus de nouvelles
  // produisent le même écran. La première mérite « tout est en ordre » ; la
  // seconde n'en sait rien, et l'affirmer quand même est le seul mensonge que
  // ce tableau de bord soit capable de commettre.
  if (fraicheur.niveau === 'perime' || fraicheur.niveau === 'inconnu') {
    if (verdict.gravite === 'ok') {
      verdict = {
        gravite: 'attention',
        titre: 'Rien à signaler, mais rien de confirmé',
        sousTitre: fraicheur.niveau === 'inconnu'
          ? "Aucune donnée n'a encore été reçue de la pharmacie : cet écran ne décrit pas son état réel."
          : `Ces chiffres ont été constatés ${fraicheur.texte} et n'ont pas été confirmés depuis. L'absence d'alerte ne prouve pas que tout va bien.`,
      };
    } else {
      // Les alertes constatées restent valables — un lot périmé le reste — mais
      // la liste peut s'être allongée depuis sans que rien ne le laisse voir.
      verdict = {
        ...verdict,
        sousTitre: `${verdict.sousTitre} Constaté ${fraicheur.texte} ; la situation a pu évoluer depuis.`,
      };
    }
  }

  return (
    <div className="pb-24 min-h-screen" style={{ background: 'linear-gradient(180deg, #0f172a 0%, #1e293b 100%)' }}>

      <div className="px-4 pt-6 mb-3">
        <h2 className="text-xl font-bold text-white tracking-tight">Ma pharmacie aujourd'hui</h2>
        <p className="text-xs text-slate-400 mt-0.5">
          {fraicheur.niveau === 'inconnu'
            ? 'En attente des données de la pharmacie'
            : `Données de la pharmacie, mises à jour ${fraicheur.texte}`}
        </p>
      </div>

      {/* 1. LE VERDICT — ce qu'il doit retenir en une phrase */}
      <Verdict {...verdict} />

      {/* 2. CE QU'IL DOIT FAIRE — par ordre de priorité */}
      {actions.length > 0 && (
        <div className="px-4 mb-6">
          <h3 className="text-xs font-bold uppercase tracking-widest text-slate-400 mb-3 ml-1">
            Par où commencer
          </h3>
          <div className="rounded-2xl px-4 py-1"
            style={{ background: 'rgba(255,255,255,0.03)', border: '1px solid rgba(255,255,255,0.05)' }}>
            {actions.map((a, i) => <Action key={i} rang={i + 1} {...a} />)}
          </div>
          <p className="text-[11px] text-slate-500 mt-2 ml-1 flex items-center gap-1">
            Le détail produit par produit se trouve dans l'onglet Urgences
            <ArrowRight className="w-3 h-3" />
          </p>
        </div>
      )}

      {/* 3. SON ACTIVITÉ — avec un repli sur le mois quand la journée est vide */}
      <div className="px-4 mb-6">
        <h3 className="text-xs font-bold uppercase tracking-widest text-slate-400 mb-3 ml-1">Activité</h3>
        {journeeVide ? (
          <div className="rounded-2xl p-4 mb-3"
            style={{ background: 'rgba(255,255,255,0.03)', border: '1px solid rgba(255,255,255,0.06)' }}>
            <p className="text-sm text-slate-300 font-medium">Aucune vente enregistrée aujourd'hui</p>
            <p className="text-[11px] text-slate-500 mt-1">
              Ce mois-ci : <span className="text-slate-300 font-semibold">{formatFCFA(mois.chiffreAffaire || 0)}</span>
              {' '}sur <span className="text-slate-300 font-semibold">{mois.ventesRealisees || 0} vente{(mois.ventesRealisees || 0) > 1 ? 's' : ''}</span>
            </p>
          </div>
        ) : (
          <div className="grid grid-cols-2 md:grid-cols-4 gap-3">
            <KpiCard label="Chiffre d'affaires" value={formatFCFA(caJour)} sousTexte="Aujourd'hui"
              icon={DollarSign} color="#34d399" />
            <KpiCard label="Marge brute" value={formatFCFA(jour.benefice || 0)} sousTexte="Hors charges"
              icon={BarChart2} color="#60a5fa" />
            <KpiCard label="Ventes" value={ventesJour} sousTexte="Tickets du jour"
              icon={Receipt} color="#a78bfa" />
            <KpiCard label="Évolution" value={`${evolution > 0 ? '+' : ''}${evolution.toFixed(1).replace('.', ',')} %`}
              sousTexte="Par rapport à hier"
              icon={evolution >= 0 ? TrendingUp : TrendingDown} color={evolution >= 0 ? '#34d399' : '#f87171'} />
          </div>
        )}
      </div>

      {/* 4. L'ARGENT — du plus immobilisé au plus menacé */}
      <div className="px-4">
        <h3 className="text-xs font-bold uppercase tracking-widest text-slate-400 mb-3 ml-1">Votre argent</h3>
        <div className="rounded-2xl p-4"
          style={{ background: 'rgba(255,255,255,0.03)', border: '1px solid rgba(255,255,255,0.05)' }}>
          <LigneMontant label="Valeur du stock" value={formatFCFA(stock.valeurTotale)} icon={Wallet} color="#60a5fa" />
          <LigneMontant label="Déjà perdu (lots périmés)" value={formatFCFA(valeurPerimes)}
            icon={Ban} color="#dc2626" valueColor={valeurPerimes > 0 ? '#f87171' : '#34d399'} />
          <LigneMontant label="Menacé (expire sous 60 jours)" value={formatFCFA(valeurARisque)}
            icon={Clock} color="#f59e0b" valueColor={valeurARisque > 0 ? '#fbbf24' : '#34d399'} />
          <LigneMontant label="Pertes du jour (casse)" value={formatFCFA(jour.pertesValeur || 0)}
            icon={HeartCrack} color="#f43f5e" valueColor={(jour.pertesValeur || 0) > 0 ? '#fb7185' : '#34d399'} />
        </div>
        {expositionTotale > 0 && (
          <p className="text-[11px] text-slate-500 mt-2 ml-1">
            Total en jeu : <span className="text-slate-300 font-semibold">{formatFCFA(expositionTotale)}</span>
            {stock.valeurTotale > 0 && ` — ${((expositionTotale / stock.valeurTotale) * 100).toFixed(1).replace('.', ',')} % de votre stock`}
          </p>
        )}
      </div>

    </div>
  );
}

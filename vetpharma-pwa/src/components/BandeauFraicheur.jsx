import React from 'react';
import { PlugZap } from 'lucide-react';

/**
 * Avertissement affiché lorsque les données ne décrivent plus le présent.
 *
 * Il est délibérément impossible à masquer. Un avertissement qu'on peut écarter
 * d'un geste est écarté par réflexe, et l'utilisateur se retrouve alors devant
 * des chiffres périmés sans plus aucune marque de leur âge — soit exactement la
 * situation que ce bandeau existe pour empêcher.
 *
 * Il dit aussi quoi faire : le lecteur est à distance, la seule action utile est
 * de faire rallumer le poste, ce qui suppose de savoir que c'est le problème.
 */
export default function BandeauFraicheur({ fraicheur, lastSync }) {
  if (fraicheur.niveau !== 'perime' && fraicheur.niveau !== 'inconnu') return null;

  const jamaisRecu = fraicheur.niveau === 'inconnu';

  const titre = jamaisRecu
    ? 'Aucune donnée reçue de la pharmacie'
    : `Données figées ${fraicheur.resume}`;

  const dateExacte = lastSync
    ? new Date(lastSync).toLocaleString('fr-FR', {
        day: '2-digit', month: 'long', hour: '2-digit', minute: '2-digit',
      })
    : null;

  return (
    <div
      role="alert"
      className="mx-4 mt-3 rounded-2xl p-4 flex items-start gap-3"
      style={{ background: 'rgba(220,38,38,0.12)', border: '1px solid rgba(220,38,38,0.35)' }}
    >
      <PlugZap className="w-6 h-6 shrink-0 mt-0.5" style={{ color: '#f87171' }} />
      <div className="min-w-0">
        <p className="text-sm font-bold leading-tight" style={{ color: '#f87171' }}>
          {titre}
        </p>
        <p className="text-xs text-slate-300 mt-1 leading-relaxed">
          Ces chiffres ne reflètent plus l'état actuel de votre pharmacie.
          Vérifiez que le poste est allumé, connecté à Internet, et que VetPharma y est ouvert.
        </p>
        {dateExacte && (
          <p className="text-[11px] text-slate-400 mt-1.5">
            Dernière donnée reçue le {dateExacte}
          </p>
        )}
      </div>
    </div>
  );
}

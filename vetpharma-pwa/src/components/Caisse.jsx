import React from 'react';
import { Wallet, Smartphone, CheckCircle2, AlertTriangle, Unlock, CalendarDays, User } from 'lucide-react';
import { formatFCFA } from '../lib/formatters';
import { useFraicheur } from '../hooks/useFraicheur';

/**
 * Écran Caisse — ce que le propriétaire ne pouvait vérifier qu'en se déplaçant.
 *
 * Ces chiffres existaient déjà dans le Registre des clôtures du poste. Les
 * amener ici répond à la question qu'on se pose en étant absent : la caisse
 * a-t-elle été tenue, et clôturée. La clôture déclenche la sauvegarde de la
 * base et la synchronisation, si bien qu'une journée non clôturée n'est ni
 * enregistrée ailleurs ni visible ici — l'oubli coûte plus que la procédure.
 *
 * La comparaison des agents entre eux est délibérément absente : elle est
 * lourde de conséquences et son utilité reste à établir par l'usage.
 */

/** Un écart nul est le seul résultat normal ; un excédent est une anomalie au même titre qu'un manque. */
function couleurEcart(ecart) {
  if (!ecart) return '#34d399';
  return ecart < 0 ? '#f87171' : '#fbbf24';
}

function LigneMontant({ label, valeur, accent }) {
  return (
    <div className="flex items-center justify-between py-1.5">
      <span className="text-xs text-slate-400">{label}</span>
      <span className={`text-sm shrink-0 ${accent ? 'font-bold' : 'font-medium text-slate-200'}`}
        style={accent ? { color: accent } : undefined}>
        {formatFCFA(valeur)}
      </span>
    </div>
  );
}

function BlocPaiement({ titre, icone: Icone, attendu, declare, ecart }) {
  return (
    <div className="rounded-xl p-3" style={{ background: 'rgba(255,255,255,0.03)' }}>
      <div className="flex items-center gap-2 mb-1">
        <Icone className="w-4 h-4 text-slate-400" />
        <span className="text-xs font-semibold uppercase tracking-wider text-slate-400">{titre}</span>
      </div>
      <LigneMontant label="Attendu par le logiciel" valeur={attendu} />
      <LigneMontant label="Déclaré par l'agent" valeur={declare} />
      <div className="border-t border-white/5 mt-1 pt-1">
        <LigneMontant label="Écart" valeur={ecart} accent={couleurEcart(ecart)} />
      </div>
    </div>
  );
}

function dateLongue(iso) {
  if (!iso) return null;
  const d = new Date(iso);
  if (Number.isNaN(d.getTime())) return null;
  return d.toLocaleDateString('fr-FR', { weekday: 'long', day: '2-digit', month: 'long' });
}

function heure(iso) {
  if (!iso) return null;
  const d = new Date(iso);
  if (Number.isNaN(d.getTime())) return null;
  return d.toLocaleTimeString('fr-FR', { hour: '2-digit', minute: '2-digit' });
}

export default function Caisse({ data, lastSync }) {
  const caisse = data?.caisse;
  const fraicheur = useFraicheur(lastSync, caisse);

  // Un poste qui n'a pas encore reçu la mise à jour n'envoie pas ce bloc.
  // Mieux vaut l'expliquer que d'afficher un écran de zéros trompeur.
  if (!caisse || !caisse.derniere) {
    return (
      <div className="pb-24 min-h-screen" style={{ background: 'linear-gradient(180deg, #0f172a 0%, #1e293b 100%)' }}>
        <div className="px-4 pt-6 mb-4">
          <h2 className="text-xl font-bold text-white tracking-tight">Caisse</h2>
        </div>
        <div className="mx-4 rounded-2xl p-4"
          style={{ background: 'rgba(255,255,255,0.03)', border: '1px solid rgba(255,255,255,0.06)' }}>
          <p className="text-sm text-slate-300 leading-relaxed">
            Aucune session de caisse n'a encore été transmise par la pharmacie.
          </p>
          <p className="text-xs text-slate-400 mt-2 leading-relaxed">
            Cet écran se remplira à la première clôture effectuée depuis le poste.
          </p>
        </div>
      </div>
    );
  }

  const d = caisse.derniere;
  const ouverte = d.statut === 'OUVERTE';
  const historique = (caisse.historique || []).slice(1); // la première ligne est déjà détaillée ci-dessus
  const ecartTotal = (caisse.ecartEspecesCumule || 0) + (caisse.ecartMobileCumule || 0);

  return (
    <div className="pb-24 min-h-screen" style={{ background: 'linear-gradient(180deg, #0f172a 0%, #1e293b 100%)' }}>

      <div className="px-4 pt-6 mb-3">
        <h2 className="text-xl font-bold text-white tracking-tight">Caisse</h2>
        <p className="text-xs text-slate-400 mt-0.5">
          {fraicheur.niveau === 'fermee' ? fraicheur.texte : 'Tenue et clôture de la caisse'}
        </p>
      </div>

      {/* Dernière session — l'état présent de la caisse */}
      <div className="mx-4 mb-6 rounded-2xl p-4"
        style={{
          background: ouverte ? 'rgba(245,158,11,0.08)' : 'rgba(255,255,255,0.03)',
          border: `1px solid ${ouverte ? 'rgba(245,158,11,0.30)' : 'rgba(255,255,255,0.06)'}`,
        }}>
        <div className="flex items-start gap-3 mb-3">
          {ouverte
            ? <Unlock className="w-5 h-5 shrink-0 mt-0.5" style={{ color: '#fbbf24' }} />
            : <CheckCircle2 className="w-5 h-5 shrink-0 mt-0.5" style={{ color: '#34d399' }} />}
          <div className="min-w-0">
            <p className="text-sm font-bold leading-tight" style={{ color: ouverte ? '#fbbf24' : '#34d399' }}>
              {ouverte
                ? `Caisse ouverte depuis ${heure(d.dateOuverture)}`
                : `Clôturée à ${heure(d.dateCloture)}`}
            </p>
            <p className="text-xs text-slate-400 mt-1 first-letter:uppercase">
              {dateLongue(ouverte ? d.dateOuverture : d.dateCloture)}
            </p>
            {d.agent && (
              <p className="text-[11px] text-slate-400 mt-1 flex items-center gap-1.5">
                <User className="w-3 h-3 shrink-0" />
                {d.agent}
              </p>
            )}
          </div>
        </div>

        {ouverte ? (
          <p className="text-xs text-slate-300 leading-relaxed">
            Les montants ne seront connus qu'à la clôture. Tant qu'elle n'a pas eu lieu,
            la journée n'est ni sauvegardée ni remontée sur ce tableau de bord.
          </p>
        ) : (
          <div className="space-y-2">
            <BlocPaiement titre="Espèces" icone={Wallet}
              attendu={d.especesAttendu} declare={d.especesDeclare} ecart={d.ecartEspeces} />
            <BlocPaiement titre="Mobile Money" icone={Smartphone}
              attendu={d.mobileAttendu} declare={d.mobileDeclare} ecart={d.ecartMobile} />
          </div>
        )}
      </div>

      {/* Tendance sur la période observée */}
      <div className="px-4 mb-6">
        <h3 className="text-xs font-bold uppercase tracking-widest text-slate-400 mb-3 ml-1">
          {caisse.joursObserves} derniers jours
        </h3>
        <div className="rounded-2xl p-4"
          style={{ background: 'rgba(255,255,255,0.03)', border: '1px solid rgba(255,255,255,0.06)' }}>
          <div className="flex items-center justify-between py-2 border-b border-white/5">
            <span className="text-sm text-slate-300">Sessions clôturées</span>
            <span className="text-sm font-bold text-white">
              {caisse.sessionsCloturees} / {caisse.sessionsTotal}
            </span>
          </div>
          {caisse.sessionsNonCloturees > 0 && (
            <div className="flex items-center justify-between py-2 border-b border-white/5">
              <span className="text-sm text-slate-300 flex items-center gap-2">
                <AlertTriangle className="w-4 h-4 shrink-0" style={{ color: '#fbbf24' }} />
                Sessions non clôturées
              </span>
              <span className="text-sm font-bold" style={{ color: '#fbbf24' }}>
                {caisse.sessionsNonCloturees}
              </span>
            </div>
          )}
          {/* Sans ce compteur, le solde ci-dessous se lit à contresens : un manque
              et un excédent équivalents s'y annulent, et deux comptages faux
              prennent l'apparence d'une caisse irréprochable. */}
          {caisse.sessionsAvecEcart > 0 && (
            <div className="flex items-center justify-between py-2 border-b border-white/5">
              <span className="text-sm text-slate-300">Sessions avec écart</span>
              <span className="text-sm font-bold" style={{ color: '#fbbf24' }}>
                {caisse.sessionsAvecEcart} / {caisse.sessionsCloturees}
              </span>
            </div>
          )}
          <div className="flex items-center justify-between py-2">
            <span className="text-sm text-slate-300">Solde des écarts</span>
            <span className="text-sm font-bold shrink-0" style={{ color: couleurEcart(ecartTotal) }}>
              {formatFCFA(ecartTotal)}
            </span>
          </div>
        </div>
      </div>

      {/* Sessions précédentes */}
      {historique.length > 0 && (
        <div className="px-4">
          <h3 className="text-xs font-bold uppercase tracking-widest text-slate-400 mb-3 ml-1">
            Clôtures précédentes
          </h3>
          <div className="rounded-2xl px-4 py-1"
            style={{ background: 'rgba(255,255,255,0.03)', border: '1px solid rgba(255,255,255,0.06)' }}>
            {historique.map((s, i) => {
              const ecart = (s.ecartEspeces || 0) + (s.ecartMobile || 0);
              const nonClose = s.statut === 'OUVERTE';
              return (
                <div key={`${s.dateOuverture}-${i}`}
                  className="flex items-center gap-3 py-3 border-b border-white/5 last:border-0">
                  <CalendarDays className="w-4 h-4 shrink-0 text-slate-500" />
                  <div className="flex-1 min-w-0">
                    <p className="text-sm font-medium text-white leading-tight first-letter:uppercase truncate">
                      {dateLongue(s.dateOuverture)}
                    </p>
                    <p className="text-[11px] text-slate-400 mt-0.5 truncate">
                      {/* « Clôturée à » est implicite sous ce titre, et le rappeler
                          faisait déborder la ligne au point de rogner le nom de l'agent. */}
                      {nonClose
                        ? 'Jamais clôturée'
                        : `${heure(s.dateCloture)}${s.agent ? ` · ${s.agent}` : ''}`}
                    </p>
                  </div>
                  <span className="text-sm font-bold shrink-0"
                    style={{ color: nonClose ? '#fbbf24' : couleurEcart(ecart) }}>
                    {nonClose ? '—' : formatFCFA(ecart)}
                  </span>
                </div>
              );
            })}
          </div>
        </div>
      )}
    </div>
  );
}

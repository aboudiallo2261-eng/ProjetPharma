import React, { useState } from 'react';
import { PackageX, AlertTriangle, Clock, CheckCircle2, HeartCrack, ChevronDown, ChevronUp } from 'lucide-react';

const formatFCFA = (val) => {
  const n = Math.round(val || 0);
  return n.toLocaleString('fr-FR') + ' FCFA';
};

const daysUntil = (dateStr) => {
  if (!dateStr) return null;
  const diff = new Date(dateStr) - new Date();
  return Math.ceil(diff / (1000 * 60 * 60 * 24));
};

function getPeremptionColor(dateStr) {
  const d = daysUntil(dateStr);
  if (d === null) return { bg: 'rgba(255,255,255,0.1)', text: 'white', label: 'Inconnu' };
  if (d < 0) return { bg: 'rgba(239,68,68,0.2)', text: '#f87171', label: 'Périmé' };
  if (d <= 7) return { bg: 'rgba(239,68,68,0.15)', text: '#fca5a5', label: `${d} j` };
  if (d <= 60) return { bg: 'rgba(249,115,22,0.15)', text: '#fb923c', label: `${d} j` };
  return { bg: 'rgba(16,185,129,0.15)', text: '#34d399', label: `${d} j` };
}

function SectionCard({ title, icon: Icon, color, children }) {
  return (
    <div className="mx-4 mb-6 rounded-3xl overflow-hidden" style={{ background: 'rgba(255,255,255,0.02)', border: '1px solid rgba(255,255,255,0.05)' }}>
      <div className="px-4 py-3 flex items-center gap-2" style={{ borderBottom: '1px solid rgba(255,255,255,0.05)', background: `linear-gradient(90deg, ${color}10, transparent)` }}>
        <Icon className="w-4 h-4" style={{ color }} />
        <h3 className="text-sm font-bold text-white uppercase tracking-wider">{title}</h3>
      </div>
      {children}
    </div>
  );
}

export default function Urgences({ data }) {
  const [expandedSection, setExpandedSection] = useState(null);

  const toggleSection = (section) => {
    setExpandedSection(expandedSection === section ? null : section);
  };

  // Sécurisation des données
  const kpis = data?.kpis || {};
  const stockData = kpis.stock || {};
  const alertes = data?.alertes || {};

  // 3.1 Réapprovisionnement
  const nbRuptures = stockData.nombreRuptures || 0;
  const nbAlertesStock = stockData.nombreAlerteStock || 0;
  const rupturesList = alertes.ruptures || [];
  const alertesStockList = alertes.alertesStock || [];
  
  const allReappro = [...rupturesList, ...alertesStockList];
  const reapproTop5 = allReappro.slice(0, 5);
  const showAllReappro = expandedSection === 'reappro';
  const displayedReappro = showAllReappro ? allReappro : reapproTop5;

  // 3.2 Péremption
  const nbPerimesProches = stockData.nombreProchePeremption || 0; 
  const valeurARisque = stockData.valeurARisque || 0;
  const perimesList = alertes.perimes || [];
  const prochePeremptionsList = alertes.prochePeremptions || [];
  
  const allPeremptions = [...perimesList, ...prochePeremptionsList]
    .sort((a, b) => new Date(a.dateExpiration) - new Date(b.dateExpiration));
  const peremptionTop5 = allPeremptions.slice(0, 5);
  const showAllPeremptions = expandedSection === 'peremption';
  const displayedPeremptions = showAllPeremptions ? allPeremptions : peremptionTop5;

  // 3.3 Pertes
  const pertesJourValeur = kpis.jour?.pertesValeur || 0;
  const pertesList = alertes.pertes || [];
  const nbIncidents = pertesList.length;
  const showAllPertes = expandedSection === 'pertes';
  const displayedPertes = showAllPertes ? pertesList : pertesList.slice(0, 5);

  return (
    <div className="pb-24 min-h-screen" style={{ background: 'linear-gradient(180deg, #0f172a 0%, #1e293b 100%)' }}>
      
      {/* Header */}
      <div className="px-4 pt-6 mb-6">
        <h2 className="text-xl font-bold text-white tracking-tight">Décision Opérationnelle</h2>
        <p className="text-xs text-slate-400 mt-0.5">Urgences & Actions immédiates</p>
      </div>

      {/* 3.1 Réapprovisionnement */}
      <SectionCard title="Réapprovisionnement" icon={PackageX} color="#f59e0b">
        <div className="p-4 grid grid-cols-2 gap-3" style={{ borderBottom: '1px solid rgba(255,255,255,0.05)' }}>
          <div>
            <p className="text-[10px] uppercase font-semibold text-slate-400 tracking-wider">Ruptures totales</p>
            <p className="text-2xl font-black text-red-400 mt-1">{nbRuptures}</p>
          </div>
          <div>
            <p className="text-[10px] uppercase font-semibold text-slate-400 tracking-wider">Alertes de stock</p>
            <p className="text-2xl font-black text-amber-400 mt-1">{nbAlertesStock}</p>
          </div>
        </div>

        {allReappro.length > 0 ? (
          <div className="divide-y divide-white/5">
            {displayedReappro.map((prod, idx) => {
              const isRupture = prod.stockPhysique === 0;
              return (
                <div key={idx} className="px-4 py-3 flex items-center justify-between hover:bg-white/5 transition-colors">
                  <div className="flex flex-col min-w-0 pr-4">
                    <p className="text-sm font-semibold text-white truncate">{prod.nom}</p>
                    <p className="text-[11px] text-slate-500 mt-0.5">
                      Seuil: {prod.seuilAlerte || 5}
                    </p>
                  </div>
                  <div className="flex flex-col items-end shrink-0">
                    <span className={`text-lg font-bold ${isRupture ? 'text-red-400' : 'text-amber-400'}`}>
                      {prod.stockPhysique || 0}
                    </span>
                    <span className="text-[10px] text-slate-500 uppercase">En stock</span>
                  </div>
                </div>
              );
            })}
            
            {allReappro.length > 5 && (
              <button 
                onClick={() => toggleSection('reappro')}
                className="w-full py-3 flex items-center justify-center gap-2 text-xs font-semibold text-amber-400 hover:bg-white/5 transition-colors">
                {showAllReappro ? (
                  <>Voir moins <ChevronUp className="w-4 h-4" /></>
                ) : (
                  <>Voir tous les {allReappro.length} produits concernés <ChevronDown className="w-4 h-4" /></>
                )}
              </button>
            )}
          </div>
        ) : (
          <div className="p-4 flex items-center gap-2 text-emerald-400 bg-emerald-400/5">
            <CheckCircle2 className="w-5 h-5"/>
            <span className="text-sm font-medium">Aucun produit en rupture ou en alerte.</span>
          </div>
        )}
      </SectionCard>

      {/* 3.2 Péremption */}
      <SectionCard title="Péremption — Surveillance" icon={Clock} color="#ef4444">
        {nbPerimesProches === 0 ? (
          <div className="p-5 flex flex-col gap-3 bg-emerald-400/5">
            <div className="flex items-center gap-3 text-emerald-400">
              <CheckCircle2 className="w-5 h-5 shrink-0"/>
              <span className="text-sm font-bold">Aucun produit critique en péremption</span>
            </div>
            <div className="flex items-center gap-3 text-emerald-400/80">
              <CheckCircle2 className="w-5 h-5 shrink-0"/>
              <span className="text-sm font-medium">0 FCFA à risque</span>
            </div>
          </div>
        ) : (
          <>
            <div className="p-4 grid grid-cols-2 gap-3" style={{ borderBottom: '1px solid rgba(255,255,255,0.05)' }}>
              <div>
                <p className="text-[10px] uppercase font-semibold text-slate-400 tracking-wider">Lots &lt; 60 jours</p>
                <p className="text-2xl font-black text-orange-400 mt-1">{nbPerimesProches}</p>
              </div>
              <div>
                <p className="text-[10px] uppercase font-semibold text-slate-400 tracking-wider">Valeur à risque</p>
                <p className="text-lg font-black text-red-400 mt-1.5">{formatFCFA(valeurARisque)}</p>
              </div>
            </div>

            <div className="divide-y divide-white/5">
              {displayedPeremptions.map((lot, idx) => {
                const colorInfo = getPeremptionColor(lot.dateExpiration);
                return (
                  <div key={idx} className="px-4 py-3 flex items-start justify-between hover:bg-white/5 transition-colors">
                    <div className="flex flex-col min-w-0 pr-4">
                      <p className="text-sm font-semibold text-white truncate">{lot.nom}</p>
                      <p className="text-[11px] text-slate-400 mt-0.5">
                        Lot {lot.numeroLot} — <span className="text-slate-300 font-medium">{lot.stockRestant} unités</span>
                      </p>
                    </div>
                    <div className="flex flex-col items-end shrink-0 gap-1.5">
                      <span className="text-[10px] font-bold px-2 py-0.5 rounded-md" 
                        style={{ background: colorInfo.bg, color: colorInfo.text }}>
                        {colorInfo.label}
                      </span>
                      <span className="text-[10px] text-slate-500 font-medium">{lot.dateExpiration.split('-').reverse().join('/')}</span>
                    </div>
                  </div>
                );
              })}
              
              {allPeremptions.length > 5 && (
                <button 
                  onClick={() => toggleSection('peremption')}
                  className="w-full py-3 flex items-center justify-center gap-2 text-xs font-semibold text-red-400 hover:bg-white/5 transition-colors">
                  {showAllPeremptions ? (
                    <>Voir moins <ChevronUp className="w-4 h-4" /></>
                  ) : (
                    <>Voir tous les {allPeremptions.length} lots concernés <ChevronDown className="w-4 h-4" /></>
                  )}
                </button>
              )}
            </div>
          </>
        )}
      </SectionCard>

      {/* 3.3 Pertes (Casse) */}
      <SectionCard title="Pertes (Casse / Erreurs)" icon={HeartCrack} color="#f43f5e">
        <div className="p-4 grid grid-cols-2 gap-3" style={{ borderBottom: '1px solid rgba(255,255,255,0.05)' }}>
          <div>
            <p className="text-[10px] uppercase font-semibold text-slate-400 tracking-wider">Valeur perdue (Jour)</p>
            <p className="text-2xl font-black text-rose-400 mt-1">{formatFCFA(pertesJourValeur)}</p>
          </div>
          <div>
            <p className="text-[10px] uppercase font-semibold text-slate-400 tracking-wider">Incidents</p>
            <p className="text-2xl font-black text-white mt-1">{nbIncidents}</p>
          </div>
        </div>

        {pertesList.length > 0 ? (
          <div className="divide-y divide-white/5">
            {displayedPertes.map((perte, idx) => (
              <div key={idx} className="px-4 py-3 flex items-center justify-between hover:bg-white/5 transition-colors">
                <div className="flex flex-col min-w-0 pr-4">
                  <p className="text-sm font-semibold text-white truncate">{perte.produit}</p>
                  <div className="flex items-center gap-2 mt-0.5">
                    <span className="text-[10px] bg-white/10 px-1.5 py-0.5 rounded text-slate-300 font-mono">Lot: {perte.numeroLot || 'N/A'}</span>
                    <span className="text-[11px] text-rose-400/80 font-medium">{perte.motif}</span>
                  </div>
                </div>
                <div className="flex flex-col items-end shrink-0">
                  <span className="text-sm font-bold text-rose-400">{formatFCFA(perte.valeur)}</span>
                  <span className="text-[10px] text-slate-400 mt-0.5">{perte.quantite} unité(s)</span>
                </div>
              </div>
            ))}
            
            {pertesList.length > 5 && (
              <button 
                onClick={() => toggleSection('pertes')}
                className="w-full py-3 flex items-center justify-center gap-2 text-xs font-semibold text-rose-400 hover:bg-white/5 transition-colors">
                {showAllPertes ? (
                  <>Voir moins <ChevronUp className="w-4 h-4" /></>
                ) : (
                  <>Voir tous les détails ({nbIncidents}) <ChevronDown className="w-4 h-4" /></>
                )}
              </button>
            )}
          </div>
        ) : (
          <div className="p-4 flex items-center gap-2 text-emerald-400 bg-emerald-400/5">
            <CheckCircle2 className="w-5 h-5"/>
            <span className="text-sm font-medium">Aucun incident de perte aujourd'hui.</span>
          </div>
        )}
      </SectionCard>

    </div>
  );
}

import React, { useState } from 'react';
import { TrendingUp, TrendingDown, DollarSign, Receipt, PackageX, AlertTriangle, Clock, Wallet, ShieldAlert, HeartCrack } from 'lucide-react';

const formatFCFA = (val) => {
  const n = val || 0;
  if (n >= 1_000_000) return `${(n / 1_000_000).toFixed(1)} M FCFA`;
  if (n >= 1_000) return `${(n / 1_000).toFixed(0)} K FCFA`;
  return `${n.toLocaleString('fr-FR')} FCFA`;
};

function KpiCard({ label, value, icon: Icon, color, sub }) {
  return (
    <div className="rounded-2xl p-4 flex flex-col gap-1 relative overflow-hidden"
      style={{ background: 'rgba(255,255,255,0.04)', border: '1px solid rgba(255,255,255,0.08)' }}>
      <div className="flex justify-between items-start">
        <p className="text-xs font-medium uppercase tracking-wider" style={{ color: 'rgba(148,163,184,0.8)' }}>{label}</p>
        <div className="w-7 h-7 rounded-lg flex items-center justify-center" style={{ background: `${color}20` }}>
          <Icon className="w-3.5 h-3.5" style={{ color }} />
        </div>
      </div>
      <p className="text-xl font-bold text-white mt-1 leading-tight">{value}</p>
      {sub && <p className="text-[11px]" style={{ color: 'rgba(148,163,184,0.6)' }}>{sub}</p>}
    </div>
  );
}

function TrendBadge({ trend }) {
  const isPos = trend >= 0;
  return (
    <div className={`flex items-center gap-1 px-2.5 py-1 rounded-full text-xs font-bold`}
      style={{
        background: isPos ? 'rgba(16,185,129,0.15)' : 'rgba(239,68,68,0.15)',
        color: isPos ? '#10b981' : '#ef4444',
        border: `1px solid ${isPos ? 'rgba(16,185,129,0.3)' : 'rgba(239,68,68,0.3)'}`
      }}>
      {isPos ? <TrendingUp className="w-3 h-3" /> : <TrendingDown className="w-3 h-3" />}
      {isPos ? '+' : ''}{trend.toFixed(1)}%
    </div>
  );
}

function SummaryRow({ label, value, icon: Icon, color, valueColor = 'white' }) {
  return (
    <div className="flex items-center justify-between py-2.5 border-b border-white/5 last:border-0">
      <div className="flex items-center gap-3">
        <div className="w-8 h-8 rounded-lg flex items-center justify-center" style={{ background: `${color}15` }}>
          <Icon className="w-4 h-4" style={{ color }} />
        </div>
        <span className="text-sm text-slate-300 font-medium">{label}</span>
      </div>
      <span className="text-base font-bold" style={{ color: valueColor }}>{value}</span>
    </div>
  );
}

export default function DashboardHome({ data }) {
  const [view, setView] = useState('jour'); // 'jour' | 'mois' | 'annee'

  // Sécurisation de l'accès aux données avec fallback sécurisé
  const kpis = data?.kpis || {};
  const stockData = kpis.stock || {};
  const currentKpi = view === 'jour'
    ? (kpis.jour || {})
    : view === 'mois'
      ? (kpis.mois || {})
      : (kpis.annee || {});

  const ca = currentKpi.chiffreAffaire || 0;
  const marge = currentKpi.benefice || 0;
  const ventes = currentKpi.ventesRealisees || 0;
  const evolution = currentKpi.evolutionCA || 0;

  // Labels dynamiques selon la vue
  const labelVentes = view === 'jour' ? 'Tickets du jour' : view === 'mois' ? 'Tickets du mois' : "Tickets de l'année";
  const labelMarge  = view === 'jour' ? 'Marge du jour'   : view === 'mois' ? 'Marge du mois'   : "Marge annuelle";
  const labelCA     = view === 'annee' ? `1er Jan → Aujourd'hui` : '';

  const margeRate = ca > 0 ? ((marge / ca) * 100).toFixed(1) : '0.0';

  return (
    <div className="pb-24 min-h-screen" style={{ background: 'linear-gradient(180deg, #0f172a 0%, #1e293b 100%)' }}>
      
      {/* 1. KPI Principaux (Hero Card) */}
      <div className="px-4 pt-4">
        <div className="rounded-3xl p-5 relative overflow-hidden"
          style={{
            background: 'linear-gradient(135deg, #065f46 0%, #047857 40%, #059669 100%)',
            boxShadow: '0 20px 60px rgba(5,150,105,0.3)'
          }}>

          {/* Toggle Aujourd'hui / Ce Mois / Cette Année */}
          <div className="flex gap-1 p-1 rounded-xl mb-4 w-fit"
            style={{ background: 'rgba(0,0,0,0.25)' }}>
            {[
              { id: 'jour',  label: "Auj." },
              { id: 'mois',  label: 'Ce Mois' },
              { id: 'annee', label: 'Cette Année' },
            ].map(({ id, label }) => (
              <button key={id} onClick={() => setView(id)}
                className="px-3 py-1.5 rounded-lg text-xs font-semibold transition-all"
                style={{
                  background: view === id ? 'rgba(255,255,255,0.18)' : 'transparent',
                  color: view === id ? 'white' : 'rgba(255,255,255,0.5)',
                  boxShadow: view === id ? '0 1px 4px rgba(0,0,0,0.3)' : 'none',
                }}>
                {label}
              </button>
            ))}
          </div>

          {/* CA Principal */}
          <div className="mb-2">
            <p className="text-emerald-200 text-xs font-medium uppercase tracking-widest mb-1">Chiffre d'Affaires</p>
            <div className="flex items-end gap-3">
              <p className="text-4xl font-black text-white tracking-tight">{formatFCFA(ca)}</p>
              <TrendBadge trend={evolution || 0} />
            </div>
            {labelCA && <p className="text-emerald-200/60 text-[10px] mt-1 font-medium">{labelCA}</p>}
          </div>

          {/* Taux de marge */}
          <div className="mt-4 pt-4" style={{ borderTop: '1px solid rgba(255,255,255,0.15)' }}>
            <p className="text-emerald-200/70 text-[11px] mb-1">Taux de marge brute</p>
            <div className="flex items-center gap-3">
              <div className="flex-1 h-2 rounded-full overflow-hidden" style={{ background: 'rgba(0,0,0,0.25)' }}>
                <div className="h-full rounded-full transition-all"
                  style={{
                    width: `${Math.min(Math.max(parseFloat(margeRate), 0), 100)}%`,
                    background: parseFloat(margeRate) > 20 ? '#6ee7b7' : '#fbbf24'
                  }} />
              </div>
              <span className="text-white font-bold text-sm">{margeRate}%</span>
            </div>
          </div>

          {/* Fond décoratif */}
          <div className="absolute top-0 right-0 w-40 h-40 rounded-full opacity-10"
            style={{ background: 'radial-gradient(circle, white, transparent)', transform: 'translate(40%, -40%)' }} />
        </div>
      </div>

      {/* Cartes Bénéfice & Ventes */}
      <div className="px-4 mt-4 grid grid-cols-2 gap-3">
        <KpiCard
          label="Bénéfice Net"
          value={formatFCFA(marge)}
          icon={DollarSign}
          color="#10b981"
          sub={labelMarge}
        />
        <KpiCard
          label="Ventes"
          value={ventes}
          icon={Receipt}
          color="#60a5fa"
          sub={labelVentes}
        />
      </div>

      {/* 2. État du stock (résumé intelligent) */}
      <div className="px-4 mt-6">
        <h3 className="text-xs font-bold uppercase tracking-widest text-slate-400 mb-3 ml-1">État du stock</h3>
        <div className="rounded-2xl p-4" style={{ background: 'rgba(255,255,255,0.03)', border: '1px solid rgba(255,255,255,0.05)' }}>
          <SummaryRow 
            label="Produits en rupture" 
            value={stockData.nombreRuptures} 
            icon={PackageX} 
            color="#ef4444" 
            valueColor={stockData.nombreRuptures > 0 ? '#ef4444' : '#10b981'} 
          />
          <SummaryRow 
            label="En alerte de stock" 
            value={stockData.nombreAlerteStock} 
            icon={AlertTriangle} 
            color="#f59e0b" 
            valueColor={stockData.nombreAlerteStock > 0 ? '#f59e0b' : '#10b981'} 
          />
          <SummaryRow 
            label="Proches péremption (<60j)" 
            value={stockData.nombrePerimes} 
            icon={Clock} 
            color="#f97316" 
            valueColor={stockData.nombrePerimes > 0 ? '#f97316' : '#10b981'} 
          />
        </div>
      </div>

      {/* 3. Risques financiers */}
      <div className="px-4 mt-6">
        <h3 className="text-xs font-bold uppercase tracking-widest text-slate-400 mb-3 ml-1">Risques financiers</h3>
        <div className="rounded-2xl p-4" style={{ background: 'rgba(255,255,255,0.03)', border: '1px solid rgba(255,255,255,0.05)' }}>
          <SummaryRow 
            label="Valeur totale du stock" 
            value={formatFCFA(stockData.valeurTotale)} 
            icon={Wallet} 
            color="#60a5fa" 
          />
          <SummaryRow 
            label="Valeur stock à risque" 
            value={formatFCFA(stockData.valeurPerimes)} 
            icon={ShieldAlert} 
            color="#f59e0b" 
            valueColor={stockData.valeurPerimes > 0 ? '#f59e0b' : '#10b981'} 
          />
          <SummaryRow 
            label="Pertes du jour (casse)" 
            value={formatFCFA(currentKpi.pertesValeur)} 
            icon={HeartCrack} 
            color="#ef4444" 
            valueColor={currentKpi.pertesValeur > 0 ? '#ef4444' : '#10b981'} 
          />
        </div>
      </div>

    </div>
  );
}

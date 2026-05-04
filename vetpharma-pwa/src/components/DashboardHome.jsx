import React from 'react';
import { TrendingUp, TrendingDown, DollarSign, Receipt, PackageX, AlertTriangle, Clock, Wallet, ShieldAlert, HeartCrack, BarChart2 } from 'lucide-react';

const formatFCFA = (val) => {
  const n = val || 0;
  if (n >= 1_000_000) return `${(n / 1_000_000).toFixed(1)} M FCFA`;
  if (n >= 1_000) return `${(n / 1_000).toFixed(0)} K FCFA`;
  return `${n.toLocaleString('fr-FR')} FCFA`;
};

function PerfKpiCard({ label, value, icon: Icon, color, isTrend, trendVal }) {
  const isPos = trendVal >= 0;
  const finalColor = isTrend ? (isPos ? '#10b981' : '#ef4444') : color;
  const TrendIcon = isTrend ? (isPos ? TrendingUp : TrendingDown) : Icon;

  return (
    <div className="rounded-2xl p-4 flex flex-col relative overflow-hidden"
      style={{ background: 'rgba(255,255,255,0.03)', border: '1px solid rgba(255,255,255,0.06)' }}>
      <div className="flex justify-between items-start mb-2">
        <p className="text-[10px] sm:text-xs font-semibold uppercase tracking-wider text-slate-400">{label}</p>
        <div className="w-6 h-6 rounded-md flex items-center justify-center shrink-0" style={{ background: `${finalColor}15` }}>
          <TrendIcon className="w-3 h-3" style={{ color: finalColor }} />
        </div>
      </div>
      <p className="text-lg sm:text-xl font-bold text-white leading-tight" style={{ color: isTrend ? finalColor : 'white' }}>
        {value}
      </p>
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
  const alertes = data?.alertes || {};
  const kpis = data?.kpis || {};
  const stockData = kpis.stock || {};
  const currentKpi = kpis.jour || {};

  const ca = currentKpi.chiffreAffaire || 0;
  const marge = currentKpi.benefice || 0;
  const ventes = currentKpi.ventesRealisees || 0;
  const evolution = currentKpi.evolutionCA || 0;

  const margeRate = ca > 0 ? ((marge / ca) * 100).toFixed(1) : '0.0';

  return (
    <div className="pb-24 min-h-screen" style={{ background: 'linear-gradient(180deg, #0f172a 0%, #1e293b 100%)' }}>
      
      {/* Header avec Titre */}
      <div className="px-4 pt-6 mb-4">
        <h2 className="text-xl font-bold text-white tracking-tight">Vue d'ensemble d'aujourd'hui</h2>
        <p className="text-xs text-slate-400 mt-0.5">Indicateurs opérationnels en temps réel</p>
      </div>

      {/* Cartes KPI (Style Performances) */}
      <div className="px-4 grid grid-cols-2 gap-3 mb-6">
        <PerfKpiCard label="Chiffre d'Affaires" value={formatFCFA(ca)} icon={DollarSign} color="#34d399" />
        <PerfKpiCard label="Bénéfices (Marge)" value={formatFCFA(marge)} icon={BarChart2} color="#60a5fa" />
        <PerfKpiCard label="Nombre de Ventes" value={ventes} icon={Receipt} color="#a78bfa" />
        <PerfKpiCard label="Croissance" value={`${evolution > 0 ? '+' : ''}${evolution.toFixed(1)}%`} isTrend trendVal={evolution} />
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
            label="Valeur stock à risque (< 60 jrs)" 
            value={formatFCFA(stockData.valeurARisque || 0)} 
            icon={ShieldAlert} 
            color="#f59e0b" 
            valueColor={(stockData.valeurARisque || 0) > 0 ? '#f59e0b' : '#10b981'} 
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

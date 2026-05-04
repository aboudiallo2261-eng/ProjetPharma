import React, { useState, useEffect } from 'react';
import { Package, TrendingUp, TrendingDown, BarChart2, Award, DollarSign, Receipt, Filter } from 'lucide-react';
import { ResponsiveContainer, AreaChart, Area, Tooltip, XAxis } from 'recharts';

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

export default function Performances({ data }) {
  const [view, setView] = useState('jour');
  const [sortTop, setSortTop] = useState('quantite');
  const [mounted, setMounted] = useState(false);

  // Ne rendre le graphique qu'après le montage complet du DOM
  // pour éviter le warning Recharts width(-1)/height(-1)
  useEffect(() => { setMounted(true); }, []);

  const kpis = data?.kpis || {};
  const currentKpi = view === 'jour' 
    ? (kpis.jour || {}) 
    : view === 'mois' 
      ? (kpis.mois || {}) 
      : (kpis.annee || {});

  const ca = currentKpi.chiffreAffaire || 0;
  const marge = currentKpi.benefice || 0;
  const ventes = currentKpi.ventesRealisees || 0;
  const evolution = currentKpi.evolutionCA || 0;

  // Préparation des données pour le graphique
  const raw7 = data?.historique7Jours || [];
  const raw3 = data?.historique3Mois || [];
  const raw3Ans = data?.historique3Ans || [];
  
  let chartData = [];
  if (view === 'jour') {
    const refDate = raw7.length > 0 ? new Date(raw7[raw7.length - 1].date) : new Date();
    for (let i = 6; i >= 0; i--) {
      const d = new Date(refDate);
      d.setDate(d.getDate() - i);
      const dateStr = `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
      const found = raw7.find(item => item.date === dateStr);
      chartData.push({
        name: dateStr.split('-').slice(1).reverse().join('/'),
        ca: found ? found.ca : 0
      });
    }
  } else if (view === 'mois') {
    // raw3 contains dates like '2026-03'
    const refDate = raw3.length > 0 ? new Date(raw3[raw3.length - 1].date + '-01') : new Date();
    for (let i = 2; i >= 0; i--) {
      const d = new Date(refDate.getFullYear(), refDate.getMonth() - i, 1);
      const dateStr = `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}`;
      const found = raw3.find(item => item.date === dateStr);
      chartData.push({
        name: dateStr.split('-').reverse().join('/'),
        ca: found ? found.ca : 0
      });
    }
  } else {
    // raw3Ans contains dates like '2024'
    const refDate = raw3Ans.length > 0 ? parseInt(raw3Ans[raw3Ans.length - 1].date) : new Date().getFullYear();
    for (let i = 2; i >= 0; i--) {
      const yearStr = String(refDate - i);
      const found = raw3Ans.find(item => item.date === yearStr);
      chartData.push({
        name: yearStr,
        ca: found ? found.ca : 0
      });
    }
  }
  
  const hasChart = chartData.some(d => d.ca > 0);

  // Top 5 Produits
  const topProduitsRaw = data?.topProduits || [];
  const topProduits = [...topProduitsRaw].sort((a, b) => (b[sortTop] || 0) - (a[sortTop] || 0)).slice(0, 5);
  const maxBarVal = Math.max(...topProduits.map(p => p[sortTop] || 0), 1);

  return (
    <div className="pb-24 min-h-screen" style={{ background: 'linear-gradient(180deg, #0f172a 0%, #1e293b 100%)' }}>
      
      {/* Header avec Toggle */}
      <div className="px-4 pt-6 mb-6 flex justify-between items-start">
        <div>
          <h2 className="text-xl font-bold text-white tracking-tight">Performances</h2>
          <p className="text-xs text-slate-400 mt-0.5">Analyse et compréhension</p>
        </div>
        <div className="flex gap-1 p-1 rounded-xl" style={{ background: 'rgba(255,255,255,0.05)' }}>
          {[
            { id: 'jour', label: 'Journalière' },
            { id: 'mois', label: 'Mensuelle' },
            { id: 'annee', label: 'Annuelle' }
          ].map(v => (
            <button key={v.id} onClick={() => setView(v.id)}
              className="px-3 py-1.5 rounded-lg text-xs font-semibold transition-all"
              style={{
                background: view === v.id ? 'rgba(255,255,255,0.15)' : 'transparent',
                color: view === v.id ? 'white' : 'rgba(255,255,255,0.5)',
              }}>
              {v.label}
            </button>
          ))}
        </div>
      </div>

      {/* KPIs Évolution */}
      <div className="px-4 grid grid-cols-2 md:grid-cols-4 gap-3 mb-6">
        <PerfKpiCard label="Chiffre d'Affaires" value={formatFCFA(ca)} icon={DollarSign} color="#34d399" />
        <PerfKpiCard label="Bénéfices (Marge)" value={formatFCFA(marge)} icon={BarChart2} color="#60a5fa" />
        <PerfKpiCard label="Nombre de Ventes" value={ventes} icon={Receipt} color="#a78bfa" />
        <PerfKpiCard label="Croissance" value={`${evolution > 0 ? '+' : ''}${evolution.toFixed(1)}%`} isTrend trendVal={evolution} />
      </div>

      {/* Graphique */}
      <div className="px-4 mb-6">
        <div className="rounded-3xl p-4 md:p-6" style={{ background: 'rgba(255,255,255,0.02)', border: '1px solid rgba(255,255,255,0.05)' }}>
          <div className="flex items-center gap-2 mb-6">
            <BarChart2 className="w-4 h-4 text-emerald-400" />
            <h3 className="text-sm font-bold text-white uppercase tracking-wider">
              {view === 'jour' ? 'Évolution des 7 derniers jours' : 'Évolution des 3 derniers mois'}
            </h3>
          </div>
          <div className="h-48 md:h-64 w-full">
            {!mounted ? (
              <div className="h-full flex items-center justify-center">
                <div className="w-6 h-6 rounded-full border-2 border-emerald-500 border-t-transparent animate-spin" />
              </div>
            ) : hasChart ? (
              <ResponsiveContainer width="100%" height="100%" minWidth={0}>
                <AreaChart data={chartData} margin={{ top: 10, right: 15, left: 15, bottom: 0 }} isAnimationActive={false}>
                  <defs>
                    <linearGradient id="colorCa" x1="0" y1="0" x2="0" y2="1">
                      <stop offset="5%" stopColor="#10b981" stopOpacity={0.4} />
                      <stop offset="95%" stopColor="#10b981" stopOpacity={0} />
                    </linearGradient>
                  </defs>
                  <XAxis 
                    dataKey="name" 
                    axisLine={false} 
                    tickLine={false} 
                    tick={{ fill: '#64748b', fontSize: 10 }} 
                    dy={10} 
                    interval="preserveStartEnd" 
                  />
                  <Tooltip
                    formatter={(v) => [formatFCFA(v), 'CA']}
                    contentStyle={{ background: '#1e293b', border: '1px solid rgba(16,185,129,0.2)', borderRadius: '12px', color: 'white', fontSize: '12px' }}
                    labelStyle={{ color: '#94a3b8', marginBottom: '4px' }}
                    cursor={{ stroke: 'rgba(16,185,129,0.3)', strokeWidth: 1, strokeDasharray: '4 4' }}
                  />
                  <Area type="monotone" dataKey="ca" stroke="#10b981" strokeWidth={3} fill="url(#colorCa)" isAnimationActive={false} activeDot={{ r: 5, fill: '#10b981', stroke: '#1e293b', strokeWidth: 2 }} />
                </AreaChart>
              </ResponsiveContainer>
            ) : (
              <div className="h-full flex flex-col items-center justify-center gap-2">
                <BarChart2 className="w-8 h-8 text-slate-600" />
                <p className="text-xs text-slate-500">Données insuffisantes pour l'affichage</p>
              </div>
            )}
          </div>
        </div>
      </div>

      {/* Top Produits */}
      <div className="px-4">
        <div className="rounded-3xl overflow-hidden" style={{ background: 'rgba(255,255,255,0.02)', border: '1px solid rgba(255,255,255,0.05)' }}>
          <div className="px-4 py-4 flex flex-col sm:flex-row sm:items-center justify-between gap-3" style={{ borderBottom: '1px solid rgba(255,255,255,0.05)' }}>
            <div className="flex items-center gap-2">
              <Award className="w-4 h-4 text-amber-400 shrink-0" />
              <h3 className="text-sm font-bold text-white uppercase tracking-wider">Top 5 des produits les plus vendus</h3>
            </div>
            
            {/* Toggle Tri */}
            <div className="flex items-center gap-1 bg-white/5 rounded-lg p-1">
              <button onClick={() => setSortTop('quantite')}
                className={`px-2 py-1 text-[10px] font-bold uppercase rounded-md transition-all ${sortTop === 'quantite' ? 'bg-white/10 text-white' : 'text-slate-400'}`}>
                Quantité
              </button>
              <button onClick={() => setSortTop('marge')}
                className={`px-2 py-1 text-[10px] font-bold uppercase rounded-md transition-all ${sortTop === 'marge' ? 'bg-white/10 text-white' : 'text-slate-400'}`}>
                Marge
              </button>
            </div>
          </div>

          {topProduits.length > 0 ? (
            <div className="divide-y divide-white/5">
              {topProduits.map((prod, idx) => {
                const val = prod[sortTop] || 0;
                const pct = (val / maxBarVal) * 100;
                const medals = ['🥇', '🥈', '🥉', '4.', '5.'];
                const displayVal = sortTop === 'marge' ? formatFCFA(val) : `${val} unités`;
                
                return (
                  <div key={idx} className="px-4 py-3 hover:bg-white/5 transition-colors">
                    <div className="flex justify-between items-center mb-2">
                      <div className="flex items-center gap-3">
                        <span className="text-lg w-6 text-center">{medals[idx]}</span>
                        <p className="text-sm font-semibold text-slate-200 truncate max-w-[140px] sm:max-w-[200px]">{prod.nom}</p>
                      </div>
                      <p className={`text-sm font-bold ${sortTop === 'marge' ? 'text-emerald-400' : 'text-blue-400'}`}>
                        {displayVal}
                      </p>
                    </div>
                    <div className="h-1.5 w-full rounded-full overflow-hidden" style={{ background: 'rgba(255,255,255,0.05)' }}>
                      <div className="h-full rounded-full transition-all duration-700"
                        style={{ width: `${pct}%`, background: sortTop === 'marge' ? '#10b981' : '#60a5fa' }} />
                    </div>
                  </div>
                );
              })}
            </div>
          ) : (
            <div className="py-12 flex flex-col items-center gap-3">
              <Package className="w-8 h-8 text-slate-600" />
              <p className="text-xs text-slate-500">Aucun produit vendu pour le moment.</p>
            </div>
          )}
        </div>
      </div>
      
    </div>
  );
}

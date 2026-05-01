import React from 'react';
import { Home, TrendingUp, Siren } from 'lucide-react';

export default function BottomNav({ currentTab, setCurrentTab, alertCount = 0 }) {
  const tabs = [
    { id: 'home', label: 'Accueil', icon: Home },
    { id: 'perf', label: 'Performances', icon: TrendingUp },
    { id: 'alerts', label: 'Urgences', icon: Siren, badge: alertCount },
  ];

  return (
    <div className="fixed bottom-0 left-0 right-0 z-50 md:top-0 md:bottom-0 md:w-64 md:border-r md:border-white/10 md:bg-[#0f172a]"
      style={{
        paddingBottom: 'max(8px, env(safe-area-inset-bottom))'
      }}>
      
      {/* Background gradient for mobile */}
      <div className="absolute inset-0 md:hidden" style={{ background: 'linear-gradient(to top, rgba(15,23,42,1) 60%, rgba(15,23,42,0))' }} />

      {/* Container principal */}
      <div className="relative flex justify-around items-center px-2 py-2 mx-2 md:mx-0 md:flex-col md:justify-start md:items-stretch md:h-full md:px-4 md:py-8 md:gap-4 md:bg-transparent md:backdrop-blur-none md:border-none md:shadow-none rounded-2xl md:rounded-none"
        style={{
          background: 'rgba(30,41,59,0.95)',
          backdropFilter: 'blur(20px)',
          border: '1px solid rgba(255,255,255,0.08)',
          boxShadow: '0 -4px 30px rgba(0,0,0,0.4)'
        }}>

        {/* Logo version Desktop uniquement */}
        <div className="hidden md:flex flex-col items-center mb-8 gap-3">
          <img src="/logo.jpeg" alt="Logo" className="w-20 h-20 rounded-full object-cover border-2 border-emerald-500 shadow-lg shadow-emerald-500/20" />
          <div className="text-center">
            <h2 className="text-white font-bold tracking-tight">Kaoural</h2>
            <p className="text-emerald-400 text-xs uppercase tracking-widest font-medium">Clinique Pharmacie</p>
          </div>
        </div>

        {tabs.map((tab) => {
          const Icon = tab.icon;
          const isActive = currentTab === tab.id;

          return (
            <button
              key={tab.id}
              id={`nav-${tab.id}`}
              onClick={() => setCurrentTab(tab.id)}
              className="flex flex-col md:flex-row md:justify-start items-center py-2.5 px-5 md:px-4 md:py-3 rounded-xl transition-all relative w-full"
              style={{
                background: isActive ? 'rgba(16,185,129,0.12)' : 'transparent',
              }}
            >
              <div className="relative md:mr-3">
                <Icon
                  className="w-5 h-5 mb-1 md:mb-0 transition-all"
                  style={{
                    color: isActive ? '#10b981' : 'rgba(148,163,184,0.5)',
                    strokeWidth: isActive ? 2.5 : 1.5
                  }}
                />
                {tab.badge > 0 && (
                  <div className="absolute -top-1 -right-1.5 w-4 h-4 rounded-full flex items-center justify-center text-[9px] font-black text-white"
                    style={{ background: '#ef4444' }}>
                    {tab.badge > 9 ? '9+' : tab.badge}
                  </div>
                )}
              </div>
              <span className="text-[10px] md:text-sm font-semibold transition-all"
                style={{ color: isActive ? '#10b981' : 'rgba(148,163,184,0.4)' }}>
                {tab.label}
              </span>
            </button>
          );
        })}
      </div>
    </div>
  );
}

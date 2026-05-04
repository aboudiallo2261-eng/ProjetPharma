import React, { useState, useEffect } from 'react';
import { RefreshCcw, LogOut } from 'lucide-react';

export default function TopBar({ lastSync, loading, onLogout }) {
  const formatTime = (isoString) => {
    if (!isoString) return 'En attente de synchro...';
    const d = new Date(isoString);
    const today = new Date();
    const isToday = d.toDateString() === today.toDateString();
    const time = d.toLocaleTimeString('fr-FR', { hour: '2-digit', minute: '2-digit' });
    const date = isToday ? "Aujourd'hui" : d.toLocaleDateString('fr-FR', { day: '2-digit', month: 'short' });
    return `${date} à ${time}`;
  };

  const [isVisible, setIsVisible] = useState(true);

  useEffect(() => {
    let lastScrollY = window.scrollY;

    const handleScroll = () => {
      const currentScrollY = window.scrollY;

      // Cache la barre si on scrolle vers le bas (et qu'on a dépassé le haut)
      if (currentScrollY > lastScrollY && currentScrollY > 80) {
        setIsVisible(false);
      }
      // Montre la barre si on scrolle vers le haut ou si on est tout en haut
      else if (currentScrollY < lastScrollY || currentScrollY <= 80) {
        setIsVisible(true);
      }

      lastScrollY = currentScrollY;
    };

    window.addEventListener('scroll', handleScroll, { passive: true });
    return () => window.removeEventListener('scroll', handleScroll);
  }, []);

  return (
    <div className={`sticky top-0 z-50 px-4 pt-4 pb-3 transition-transform duration-300 ease-in-out`}
      style={{
        transform: isVisible ? 'translateY(0)' : 'translateY(-100%)',
        background: 'linear-gradient(135deg, #0f172a 0%, #134e4a 100%)',
        backdropFilter: 'blur(20px)',
        WebkitBackdropFilter: 'blur(20px)',
        borderBottom: '1px solid rgba(16,185,129,0.2)',
        boxShadow: '0 4px 30px rgba(0,0,0,0.3)'
      }}>
      <div className="flex justify-between items-center">
        <div className="flex items-center gap-3 md:hidden">
          {/* Logo */}
          <div className="w-10 h-10 rounded-full flex items-center justify-center shrink-0 border border-emerald-500 overflow-hidden"
            style={{ boxShadow: '0 0 12px rgba(16,185,129,0.4)' }}>
            <img src="/logo.jpeg" alt="Logo" className="w-full h-full object-cover" />
          </div>
          <div>
            <h1 className="font-bold text-white text-base leading-tight tracking-tight">Kaoural</h1>
            <p className="text-[10px] text-slate-400 leading-tight">Clinique Pharmacie — Tableau de bord</p>
          </div>
        </div>
        <div className="flex items-center gap-2 ml-auto">
          <div className={`w-8 h-8 rounded-full flex items-center justify-center transition-all ${loading ? 'bg-emerald-500/20' : 'bg-white/5'}`}>
            <RefreshCcw className={`w-4 h-4 text-emerald-400 ${loading ? 'animate-spin' : ''}`} />
          </div>
          {onLogout && (
            <button onClick={onLogout}
              className="w-8 h-8 rounded-full flex items-center justify-center bg-white/5 hover:bg-red-500/20 transition-colors group"
              title="Déconnexion">
              <LogOut className="w-4 h-4 text-slate-400 group-hover:text-red-400 transition-colors" />
            </button>
          )}
        </div>
      </div>

      {/* Barre de sync */}
      <div className="mt-2 flex items-center gap-2">
        <div className={`w-1.5 h-1.5 rounded-full shrink-0 ${loading ? 'bg-amber-400 animate-pulse' : 'bg-emerald-400'}`} />
        <p className="text-[11px] text-slate-400">
          {loading ? 'Synchronisation en cours...' : `Dernière mise à jour : ${formatTime(lastSync)}`}
        </p>
      </div>
    </div>
  );
}

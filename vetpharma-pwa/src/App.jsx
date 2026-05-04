import React, { useState, useEffect } from 'react';
import { supabase } from './lib/supabaseClient';
import { WifiOff } from 'lucide-react';
import { useDashboardData } from './hooks/useDashboardData';
import TopBar from './components/TopBar';
import BottomNav from './components/BottomNav';
import DashboardHome from './components/DashboardHome';
import Performances from './components/Performances';
import Urgences from './components/Urgences';
import Login from './components/Login';

const INACTIVITY_TIMEOUT = 5 * 60 * 60 * 1000; // 5 heures

function App() {
  const [session, setSession] = useState(null);
  const [currentTab, setCurrentTab] = useState('home');
  const [authLoading, setAuthLoading] = useState(true);
  const [deferredPrompt, setDeferredPrompt] = useState(null);

  // Écoute de l'événement d'installation PWA
  useEffect(() => {
    const handleBeforeInstallPrompt = (e) => {
      e.preventDefault(); // Empêche l'affichage de la mini-barre native
      setDeferredPrompt(e); // Sauvegarde l'événement pour l'utiliser plus tard
    };

    window.addEventListener('beforeinstallprompt', handleBeforeInstallPrompt);

    return () => {
      window.removeEventListener('beforeinstallprompt', handleBeforeInstallPrompt);
    };
  }, []);

  const handleInstallClick = async () => {
    if (!deferredPrompt) return;
    deferredPrompt.prompt();
    const { outcome } = await deferredPrompt.userChoice;
    setDeferredPrompt(null);
  };

  // Gestion de la session et de l'inactivité
  useEffect(() => {
    let inactivityTimer;

    const resetInactivityTimer = () => {
      clearTimeout(inactivityTimer);
      if (session) {
        inactivityTimer = setTimeout(async () => {
          await supabase.auth.signOut();
        }, INACTIVITY_TIMEOUT);
      }
    };

    // Obtenir la session initiale
    supabase.auth.getSession().then(({ data: { session } }) => {
      setSession(session);
      setAuthLoading(false);
      if (session) resetInactivityTimer();
    });

    // Écouter les changements d'auth
    const { data: { subscription } } = supabase.auth.onAuthStateChange((_event, session) => {
      setSession(session);
      if (session) {
        resetInactivityTimer();
      } else {
        clearTimeout(inactivityTimer);
      }
    });

    // Écouteurs d'activité utilisateur pour réinitialiser le timer
    const events = ['mousedown', 'mousemove', 'keypress', 'scroll', 'touchstart'];
    events.forEach(event => document.addEventListener(event, resetInactivityTimer));

    return () => {
      subscription.unsubscribe();
      clearTimeout(inactivityTimer);
      events.forEach(event => document.removeEventListener(event, resetInactivityTimer));
    };
  }, [session]);

  // Hook qui va récupérer les données (seulement s'il y a une session grâce aux modifications du hook)
  const { dashboardData, loading, error, lastSync } = useDashboardData(session ? 'MAIN_PHARMACY' : null);

  const handleLogout = async () => {
    await supabase.auth.signOut();
  };

  if (authLoading) {
    return <div className="min-h-screen bg-slate-50 flex justify-center items-center">Chargement...</div>;
  }

  if (!session) {
    return <Login />;
  }

  const raw = dashboardData || {};
  const kpis = raw.kpis || {};
  const stock = kpis.stock || {};
  const alertes = raw.alertes || {};
  
  // Badge = actions urgentes réelles : ruptures + alertes stock + lots proches péremption (<60j)
  // On exclut les lots déjà périmés (alertes.perimes) car moins actionnables en urgence immédiate
  const alertCount = (stock.nombreRuptures || 0)
    + (stock.nombreAlerteStock || 0)
    + (alertes.prochePeremptions?.length || 0);

  return (
    <div className="min-h-screen font-sans antialiased selection:bg-emerald-500/30 selection:text-emerald-100 flex flex-col md:flex-row"
      style={{ background: '#0f172a' }}>
      
      {/* Navigation Responsive (Sidebar sur Desktop, Barre du bas sur Mobile) */}
      <BottomNav currentTab={currentTab} setCurrentTab={setCurrentTab} alertCount={alertCount} />

      {/* Main Content Area */}
      <div className="flex-1 flex flex-col min-w-0 md:ml-64">
        <TopBar lastSync={lastSync} loading={loading} onLogout={handleLogout} />
        
        <main className="max-w-7xl w-full mx-auto relative pb-24 md:pb-8">
          {error && (
            <div className="m-4 md:mx-8 p-3 bg-orange-500/10 text-orange-400 rounded-2xl text-sm font-medium border border-orange-500/20 flex items-start gap-3">
              <WifiOff className="w-5 h-5 shrink-0 mt-0.5" />
              <div>
                <p className="font-bold">Mode Hors Ligne</p>
                <p className="text-xs text-orange-400/80 mt-0.5">Impossible de synchroniser. Affichage des dernières données connues.</p>
              </div>
            </div>
          )}

          {currentTab === 'home' && <DashboardHome data={raw} />}
          {currentTab === 'perf' && <Performances data={raw} />}
          {currentTab === 'alerts' && <Urgences data={raw} />}
        </main>
      </div>

      {/* Bannière d'installation personnalisée PWA */}
      {deferredPrompt && (
        <div className="fixed bottom-20 left-4 right-4 md:bottom-6 md:left-auto md:right-6 md:w-80 rounded-2xl p-4 shadow-2xl z-50 flex items-center justify-between" 
          style={{ background: 'linear-gradient(135deg, #059669 0%, #10b981 100%)', border: '1px solid rgba(255,255,255,0.2)' }}>
          <div className="text-white flex-1 mr-3">
            <p className="text-sm font-bold tracking-tight">Installez Kaoural sur votre téléphone</p>
            <p className="text-[11px] text-emerald-100 leading-tight mt-0.5">Accès plus rapide sans passer par le navigateur.</p>
          </div>
          <button 
            onClick={handleInstallClick}
            className="bg-white text-emerald-700 px-4 py-2 rounded-xl text-xs font-bold shadow-md active:scale-95 transition-transform shrink-0"
          >
            Installer
          </button>
        </div>
      )}
    </div>
  );
}

export default App;

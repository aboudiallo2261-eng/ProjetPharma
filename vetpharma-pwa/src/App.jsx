import React, { useState, useEffect } from 'react';
import { supabase } from './lib/supabaseClient';
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
            <div className="m-4 md:mx-8 p-3 bg-red-500/10 text-red-400 rounded-2xl text-sm font-medium border border-red-500/20 flex items-start gap-2">
              <span className="font-bold">Erreur :</span> {error}
            </div>
          )}

          {currentTab === 'home' && <DashboardHome data={raw} />}
          {currentTab === 'perf' && <Performances data={raw} />}
          {currentTab === 'alerts' && <Urgences data={raw} />}
        </main>
      </div>
    </div>
  );
}

export default App;

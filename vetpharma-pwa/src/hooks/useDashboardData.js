import { useEffect, useState } from 'react'
import { supabase } from '../lib/supabaseClient'

export function useDashboardData(pharmacyId) {
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [lastSync, setLastSync] = useState(null)
  
  const [dashboardData, setDashboardData] = useState(() => {
    const saved = localStorage.getItem('vetpharma_dashboard_cache');
    if (saved) {
      try { return JSON.parse(saved); } catch (e) {}
    }
    return {
      kpis: {
        jour: { chiffreAffaire: 0, benefice: 0, ventesRealisees: 0, evolutionCA: 0, pertesValeur: 0 },
        mois: { chiffreAffaire: 0, benefice: 0, ventesRealisees: 0, evolutionCA: 0, pertesValeur: 0 },
        stock: { valeurTotale: 0, nombreRuptures: 0, nombreAlerteStock: 0, nombrePerimes: 0, valeurPerimes: 0, nombreProchePeremption: 0 }
      },
      alertes: { ruptures: [], alertesStock: [], perimes: [], prochePeremptions: [], pertes: [] },
      topProduitsJour: [],
      topProduitsMois: [],
      topProduitsAnnee: [],
      historique7Jours: [],
      historique3Mois: []
    };
  });

  useEffect(() => {
    let isMounted = true;

    // Si on n'a pas d'identifiant (ex: non authentifié), on ne fait rien
    if (!pharmacyId) {
      setLoading(false);
      return;
    }

    const fetchInitialData = async () => {
      try {
        setLoading(true)
        const { data, error } = await supabase
          .from('pharmacy_dashboard_sync')
          .select('payload, updated_at')
          .eq('pharmacy_id', pharmacyId)
          .maybeSingle()

        if (error) throw error
        
        if (data && data.payload && isMounted) {
          setDashboardData(data.payload)
          setLastSync(data.updated_at)
          localStorage.setItem('vetpharma_dashboard_cache', JSON.stringify(data.payload));
        }
      } catch (err) {
        if (isMounted) setError(err.message)
      } finally {
        if (isMounted) setLoading(false)
      }
    }

    fetchInitialData()

    // Real-time subscription
    const channel = supabase
      .channel('schema-db-changes')
      .on(
        'postgres_changes',
        {
          event: 'UPDATE',
          schema: 'public',
          table: 'pharmacy_dashboard_sync',
          filter: `pharmacy_id=eq.${pharmacyId}`,
        },
        (payload) => {
          if (payload.new && payload.new.payload && isMounted) {
            setDashboardData(payload.new.payload)
            setLastSync(payload.new.updated_at)
          }
        }
      )
      .subscribe()

    return () => {
      isMounted = false;
      supabase.removeChannel(channel)
    }
  }, [pharmacyId])

  return { dashboardData, loading, error, lastSync }
}

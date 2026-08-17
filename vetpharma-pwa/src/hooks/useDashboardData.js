import { useEffect, useState } from 'react'
import { supabase } from '../lib/supabaseClient'

const CLE_CACHE = 'vetpharma_dashboard_cache'
const CLE_CACHE_DATE = 'vetpharma_dashboard_cache_date'

/**
 * Mémorise les données et leur date ensemble.
 * Les dissocier laisserait ressurgir au rechargement des chiffres orphelins,
 * privés de l'horodatage qui permet d'en juger la validité.
 */
function memoriser(payload, date) {
  try {
    localStorage.setItem(CLE_CACHE, JSON.stringify(payload))
    if (date) localStorage.setItem(CLE_CACHE_DATE, date)
  } catch {
    // Quota saturé ou stockage refusé : le cache est un confort, pas une nécessité.
  }
}

export function useDashboardData(pharmacyId) {
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  // Restaurée avec les données qu'elle date : hors ligne, l'écran doit pouvoir
  // dire de quand datent les chiffres qu'il continue d'afficher.
  const [lastSync, setLastSync] = useState(() => localStorage.getItem(CLE_CACHE_DATE))

  const [dashboardData, setDashboardData] = useState(() => {
    const saved = localStorage.getItem(CLE_CACHE);
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
          memoriser(data.payload, data.updated_at)
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
            // Les mises à jour temps réel alimentent le cache au même titre que
            // le chargement initial, sans quoi une session longue rechargerait
            // sur un instantané plus ancien que ce qu'elle affichait déjà.
            memoriser(payload.new.payload, payload.new.updated_at)
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

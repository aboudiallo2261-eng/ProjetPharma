import React, { useState } from 'react';
import { supabase } from '../lib/supabaseClient';
import { Lock, Mail, AlertCircle, Loader2, ShieldCheck, Eye, EyeOff } from 'lucide-react';

export default function Login() {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [showPassword, setShowPassword] = useState(false);

  const handleLogin = async (e) => {
    e.preventDefault();
    setLoading(true);
    setError('');
    const { error } = await supabase.auth.signInWithPassword({ email, password });
    if (error) {
      setError("Email ou mot de passe incorrect. Veuillez réessayer.");
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen flex flex-col justify-center items-center p-4 relative overflow-hidden"
      style={{ background: 'linear-gradient(145deg, #0f172a 0%, #134e4a 50%, #0f172a 100%)' }}>

      <div className="absolute w-96 h-96 rounded-full opacity-10 pointer-events-none"
        style={{ background: 'radial-gradient(circle, #10b981, transparent)', top: '-10%', right: '-10%' }} />
      <div className="absolute w-64 h-64 rounded-full opacity-10 pointer-events-none"
        style={{ background: 'radial-gradient(circle, #10b981, transparent)', bottom: '5%', left: '-8%' }} />

      <div className="mb-8 text-center z-10">
        <div className="w-24 h-24 rounded-full flex items-center justify-center mx-auto mb-5 shadow-2xl border-2 border-emerald-500 overflow-hidden"
          style={{ boxShadow: '0 0 40px rgba(16,185,129,0.4)' }}>
          <img src="/logo.jpeg" alt="Logo Kaoural" className="w-full h-full object-cover" />
        </div>
        <h1 className="text-3xl font-bold text-white tracking-tight">Kaoural</h1>
        <p className="text-emerald-300 font-medium mt-1 text-sm tracking-widest uppercase">Clinique Pharmacie</p>
        <p className="text-slate-400 mt-3 text-sm">Portail de Direction — Espace Sécurisé</p>
      </div>

      <div className="w-full max-w-sm z-10 rounded-3xl p-8"
        style={{
          background: 'rgba(255,255,255,0.05)',
          backdropFilter: 'blur(20px)',
          WebkitBackdropFilter: 'blur(20px)',
          border: '1px solid rgba(255,255,255,0.1)',
          boxShadow: '0 25px 50px rgba(0,0,0,0.5)'
        }}>

        <h2 className="text-lg font-semibold text-white mb-6">Connexion Sécurisée</h2>

        {error && (
          <div className="mb-5 p-3 rounded-xl flex items-start gap-3"
            style={{ background: 'rgba(239,68,68,0.15)', border: '1px solid rgba(239,68,68,0.3)' }}>
            <AlertCircle className="w-4 h-4 text-red-400 shrink-0 mt-0.5" />
            <p className="text-sm text-red-300">{error}</p>
          </div>
        )}

        <form onSubmit={handleLogin} className="space-y-4">
          <div>
            <label className="block text-xs font-medium text-slate-400 mb-1.5 uppercase tracking-wider">Email</label>
            <div className="relative">
              <Mail className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-slate-500" />
              <input
                id="login-email"
                type="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                required
                placeholder="direction@kaoural.com"
                className="w-full pl-10 pr-4 py-3 rounded-xl text-sm text-white placeholder-slate-600 outline-none"
                style={{ background: 'rgba(255,255,255,0.07)', border: '1px solid rgba(255,255,255,0.1)' }}
              />
            </div>
          </div>

          <div>
            <label className="block text-xs font-medium text-slate-400 mb-1.5 uppercase tracking-wider">Mot de passe</label>
            <div className="relative">
              <Lock className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-slate-500" />
              <input
                id="login-password"
                type={showPassword ? 'text' : 'password'}
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                required
                placeholder="••••••••"
                className="w-full pl-10 pr-11 py-3 rounded-xl text-sm text-white placeholder-slate-600 outline-none"
                style={{ background: 'rgba(255,255,255,0.07)', border: '1px solid rgba(255,255,255,0.1)' }}
              />
              <button
                type="button"
                onClick={() => setShowPassword(v => !v)}
                className="absolute right-3 top-1/2 -translate-y-1/2 p-1 rounded-lg transition-colors"
                style={{ color: showPassword ? '#10b981' : 'rgba(148,163,184,0.5)' }}
                tabIndex={-1}
                aria-label={showPassword ? 'Masquer le mot de passe' : 'Afficher le mot de passe'}
              >
                {showPassword ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
              </button>
            </div>
          </div>

          <button
            id="login-submit"
            type="submit"
            disabled={loading}
            className="w-full flex justify-center items-center gap-2 py-3.5 rounded-xl text-sm font-semibold text-white mt-2 disabled:opacity-60 active:scale-95 transition-transform"
            style={{ background: 'linear-gradient(135deg, #059669, #10b981)', boxShadow: '0 8px 24px rgba(16,185,129,0.35)' }}
          >
            {loading ? <><Loader2 className="w-4 h-4 animate-spin" /> Vérification...</> : 'Accéder au Dashboard'}
          </button>
        </form>
      </div>

      <div className="mt-8 flex items-center gap-2 text-slate-600 text-xs z-10">
        <ShieldCheck className="w-3.5 h-3.5" />
        Chiffrement AES-256 — Déconnexion automatique après 5h d'inactivité
      </div>
    </div>
  );
}

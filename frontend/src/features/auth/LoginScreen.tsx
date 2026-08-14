import { useState } from 'react';
import axios from 'axios';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import { authService } from '../../services/auth.service';
import { esGerente } from '../../utils/roles';
import { LockIcon } from '../../components/icons';

export function LoginScreen() {
  const { iniciarSesion } = useAuth();
  const navigate = useNavigate();
  const [cargando, setCargando] = useState(false);
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');

  const handleLogin = async () => {
    setError('');
    setCargando(true);
    try {
      const respuesta = await authService.login({ username, password });
      iniciarSesion(respuesta.token, respuesta.userId, respuesta.username, respuesta.roles);
      navigate(esGerente(respuesta.roles) ? '/dashboard' : '/pos');
    } catch (err) {
      if (axios.isAxiosError(err)) {
        if (err.response) {
          const mensaje = err.response.data?.message;
          setError(
            mensaje && err.response.status !== 401
              ? mensaje
              : 'Credenciales inválidas. Verifique usuario y contraseña.',
          );
        } else {
          setError('No se pudo conectar con el backend. Verifique que esté en ejecución.');
        }
      } else {
        setError('Credenciales inválidas. Verifique usuario y contraseña.');
      }
    } finally {
      setCargando(false);
    }
  };

  return (
    <div className="min-h-screen flex items-center justify-center bg-gradient-to-br from-slate-50 via-slate-100 to-emerald-50 p-4">
      <div className="absolute inset-0 opacity-30" style={{
        backgroundImage: 'radial-gradient(circle at center, #CBD5E1 1px, transparent 1px)',
        backgroundSize: '28px 28px',
      }} />

      <div className="relative w-full max-w-sm">
        <div className="bg-white rounded-2xl shadow-xl shadow-slate-200/60 border border-slate-100 p-8">
          <div className="flex flex-col items-center mb-8">
            <div className="w-16 h-16 rounded-2xl bg-gradient-to-br from-emerald-500 to-emerald-700 flex items-center justify-center shadow-lg shadow-emerald-200 mb-4">
              <svg width="32" height="32" viewBox="0 0 32 32" fill="none">
                <rect x="13" y="4" width="6" height="24" rx="3" fill="white" fillOpacity="0.9" />
                <rect x="4" y="13" width="24" height="6" rx="3" fill="white" fillOpacity="0.9" />
              </svg>
            </div>
            <h1 style={{ fontFamily: 'DM Sans, sans-serif' }} className="text-xl font-700 text-slate-900 text-center">
              Botica Inteligente
            </h1>
            <p className="text-sm text-slate-400 text-center mt-1">Sistema de Gestión Farmacéutica</p>
          </div>

          {error && (
            <div className="mb-4 p-3 rounded-lg bg-red-50 border border-red-200 text-red-700 text-sm">{error}</div>
          )}

          <div className="space-y-4 mb-6">
            <div>
              <label className="block text-xs font-600 text-slate-500 mb-1.5 uppercase tracking-wide">Usuario</label>
              <input
                type="text"
                value={username}
                onChange={(e) => setUsername(e.target.value)}
                className="w-full px-3.5 py-2.5 rounded-lg border border-slate-200 text-sm text-slate-800 bg-slate-50 focus:bg-white focus:border-emerald-500 focus:ring-2 focus:ring-emerald-100 transition-all outline-none"
              />
            </div>
            <div>
              <label className="block text-xs font-600 text-slate-500 mb-1.5 uppercase tracking-wide">Contraseña</label>
              <input
                type="password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                onKeyDown={(e) => e.key === 'Enter' && handleLogin()}
                className="w-full px-3.5 py-2.5 rounded-lg border border-slate-200 text-sm text-slate-800 bg-slate-50 focus:bg-white focus:border-emerald-500 focus:ring-2 focus:ring-emerald-100 transition-all outline-none"
              />
            </div>
          </div>

          <button
            onClick={handleLogin}
            disabled={cargando || !username || !password}
            className="w-full py-3 rounded-xl bg-emerald-600 hover:bg-emerald-700 text-white text-sm font-600 transition-all shadow-sm shadow-emerald-200 disabled:opacity-70 flex items-center justify-center gap-2"
          >
            {cargando ? (
              <>
                <svg className="animate-spin w-4 h-4" fill="none" viewBox="0 0 24 24">
                  <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="3" />
                  <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v8z" />
                </svg>
                Autenticando…
              </>
            ) : (
              <>
                <LockIcon />
                Iniciar Sesión
              </>
            )}
          </button>

          <p className="text-center text-xs text-slate-400 mt-4 flex items-center justify-center gap-1.5">
            <svg width="12" height="12" fill="none" viewBox="0 0 16 16"><path d="M8 1a4 4 0 014 4v2H4V5a4 4 0 014-4zm5 6H3a1 1 0 00-1 1v6a1 1 0 001 1h10a1 1 0 001-1V8a1 1 0 00-1-1z" fill="#94A3B8" /></svg>
            Autenticación segura con JWT
          </p>
        </div>
      </div>
    </div>
  );
}
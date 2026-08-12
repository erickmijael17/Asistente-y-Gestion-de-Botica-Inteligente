import { useState, useRef, useEffect, KeyboardEvent, useCallback } from 'react'
import {
  BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer
} from 'recharts'
import { useAuth } from './context/AuthContext'
import { authService } from './services/auth.service'
import { productoService } from './services/producto.service'
import { categoriaService } from './services/categoria.service'
import { laboratorioService } from './services/laboratorio.service'
import { ventaService } from './services/venta.service'
import type {
  PantallaApp, ProductoResumen, ItemCarrito, MensajeChat, Venta, Categoria, Laboratorio
} from './types/domain.types'

// Respuestas simuladas del chatbot (módulo IA pendiente de implementación)
const RESPUESTAS_ASISTENTE: Record<string, { texto: string; palabrasClave: string[] }> = {
  default: { texto: 'He analizado el catálogo disponible. Aquí tienes opciones relevantes:', palabrasClave: [] },
  fiebre: { texto: 'Para fiebre y dolor recomiendo evaluar estas opciones del catálogo:', palabrasClave: ['fiebre', 'tos', 'dolor'] },
  antibiotico: { texto: 'Para infecciones bacterianas, estos productos están registrados:', palabrasClave: ['antibio', 'infec'] },
  gastro: { texto: 'Para síntomas gastrointestinales:', palabrasClave: ['estomago', 'gastro', 'acidez'] },
  alergia: { texto: 'Para cuadros alérgicos:', palabrasClave: ['alergi', 'rinitis'] },
}

const formatearMoneda = (valor: number) => `S/ ${valor.toFixed(2)}`

const formatearFecha = (fecha: string) =>
  new Date(fecha).toLocaleString('es-PE', { day: '2-digit', month: '2-digit', year: 'numeric', hour: '2-digit', minute: '2-digit' })

const esGerente = (roles: string) => roles.includes('ROLE_OWNER')

// ─── Utility Components ───────────────────────────────────────────────────────

function Badge({ label, variant }: { label: string; variant: 'green' | 'red' | 'yellow' | 'blue' | 'gray' }) {
  const styles = {
    green: 'bg-emerald-50 text-emerald-700 border-emerald-200',
    red: 'bg-red-50 text-red-700 border-red-200',
    yellow: 'bg-amber-50 text-amber-700 border-amber-200',
    blue: 'bg-blue-50 text-blue-700 border-blue-200',
    gray: 'bg-slate-100 text-slate-500 border-slate-200',
  }
  return (
    <span className={`inline-flex items-center px-2 py-0.5 text-xs font-medium border rounded-full ${styles[variant]}`}>
      {label}
    </span>
  )
}

function Sidebar({ pantalla, roles, nombreUsuario, onNav, onLogout }: {
  pantalla: PantallaApp; roles: string; nombreUsuario: string
  onNav: (p: PantallaApp) => void; onLogout: () => void
}) {
  const gerente = esGerente(roles)
  const items = [
    { id: 'panel' as const, label: 'Dashboard', icon: ChartIcon, visible: gerente },
    { id: 'punto-venta' as const, label: 'Punto de Venta', icon: CashIcon, visible: true },
    { id: 'catalogo' as const, label: 'Catálogo', icon: BoxIcon, visible: true },
  ]

  return (
    <aside style={{ width: 220, minWidth: 220 }} className="h-screen flex flex-col bg-white border-r border-slate-200">
      <div className="px-5 py-5 border-b border-slate-100">
        <div className="flex items-center gap-2.5">
          <div className="w-8 h-8 rounded-lg bg-emerald-600 flex items-center justify-center">
            <CrossIcon />
          </div>
          <div>
            <div style={{ fontFamily: 'DM Sans, sans-serif' }} className="text-sm font-700 text-slate-900 leading-tight">Botica</div>
            <div style={{ fontFamily: 'DM Sans, sans-serif' }} className="text-xs font-600 text-emerald-600 leading-tight">Inteligente</div>
          </div>
        </div>
      </div>

      <nav className="flex-1 px-3 py-4 space-y-0.5 overflow-y-auto">
        {items.filter(i => i.visible).map(item => {
          const Icon = item.icon
          const active = pantalla === item.id
          return (
            <button
              key={item.id}
              onClick={() => onNav(item.id)}
              className={`w-full flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm font-medium transition-colors ${
                active
                  ? 'bg-emerald-50 text-emerald-700'
                  : 'text-slate-600 hover:bg-slate-50 hover:text-slate-900'
              }`}
            >
              <Icon active={active} />
              {item.label}
            </button>
          )
        })}
      </nav>

      <div className="px-3 py-4 border-t border-slate-100">
        <div className="px-3 py-2 mb-1">
          <div className="text-xs font-medium text-slate-900">{nombreUsuario}</div>
          <div className="text-xs text-slate-400">{gerente ? 'Gerente' : 'Vendedor'}</div>
        </div>
        <button
          onClick={onLogout}
          className="w-full flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm font-medium text-slate-500 hover:bg-red-50 hover:text-red-600 transition-colors"
        >
          <LogoutIcon />
          Cerrar Sesión
        </button>
      </div>
    </aside>
  )
}

// ─── Screen: Login ────────────────────────────────────────────────────────────

function LoginScreen({ onLogin }: { onLogin: (roles: string) => void }) {
  const { iniciarSesion } = useAuth()
  const [cargando, setCargando] = useState(false)
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')

  const handleLogin = async () => {
    setError('')
    setCargando(true)
    try {
      const respuesta = await authService.login({ username, password })
      iniciarSesion(respuesta.token, respuesta.userId, respuesta.username, respuesta.roles)
      onLogin(respuesta.roles)
    } catch {
      setError('Credenciales inválidas. Verifique usuario y contraseña.')
    } finally {
      setCargando(false)
    }
  }

  return (
    <div className="min-h-screen flex items-center justify-center bg-gradient-to-br from-slate-50 via-slate-100 to-emerald-50 p-4">
      {/* Background pattern */}
      <div className="absolute inset-0 opacity-30" style={{
        backgroundImage: 'radial-gradient(circle at center, #CBD5E1 1px, transparent 1px)',
        backgroundSize: '28px 28px',
      }} />

      <div className="relative w-full max-w-sm">
        {/* Logo card */}
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
                onChange={e => setUsername(e.target.value)}
                className="w-full px-3.5 py-2.5 rounded-lg border border-slate-200 text-sm text-slate-800 bg-slate-50 focus:bg-white focus:border-emerald-500 focus:ring-2 focus:ring-emerald-100 transition-all outline-none"
              />
            </div>
            <div>
              <label className="block text-xs font-600 text-slate-500 mb-1.5 uppercase tracking-wide">Contraseña</label>
              <input
                type="password"
                value={password}
                onChange={e => setPassword(e.target.value)}
                onKeyDown={e => e.key === 'Enter' && handleLogin()}
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
  )
}

// ─── Screen: Dashboard ────────────────────────────────────────────────────────

function DashboardScreen({ ventas, productos, cargando }: {
  ventas: Venta[]; productos: ProductoResumen[]; cargando: boolean
}) {
  const ventasCompletadas = ventas.filter(v => v.estado === 'COMPLETADA')
  const totalDia = ventasCompletadas.reduce((s, v) => s + v.total, 0)
  const productosInactivos = productos.filter(p => !p.estado).length

  const datosGrafico = ['Lun', 'Mar', 'Mié', 'Jue', 'Vie', 'Sáb', 'Dom'].map((dia, i) => ({
    day: dia,
    ventas: ventasCompletadas[i]?.total ?? 0,
  }))

  const kpis = [
    { label: 'Ventas Recientes', value: formatearMoneda(totalDia), sub: `${ventasCompletadas.length} transacciones`, icon: '💰', color: 'emerald' },
    { label: 'Productos Inactivos', value: `${productosInactivos}`, sub: 'Revisar catálogo', icon: '⚠️', color: 'amber' },
    { label: 'Catálogo Activo', value: `${productos.filter(p => p.estado).length}`, sub: 'Productos habilitados', icon: '📦', color: 'blue' },
    { label: 'Total Ventas', value: `${ventas.length}`, sub: 'Registradas en el sistema', icon: '📈', color: 'blue' },
  ]

  const colorMap: Record<string, string> = {
    emerald: 'border-l-4 border-emerald-500',
    amber: 'border-l-4 border-amber-500',
    red: 'border-l-4 border-red-500',
    blue: 'border-l-4 border-blue-500',
  }

  return (
    <div className="flex-1 overflow-y-auto p-6">
      {cargando && <p className="text-sm text-slate-400 mb-4">Cargando datos del panel…</p>}
      <div className="mb-6">
        <h1 style={{ fontFamily: 'DM Sans, sans-serif' }} className="text-2xl font-700 text-slate-900">Dashboard</h1>
        <p className="text-sm text-slate-400 mt-0.5">Lunes, 11 de agosto de 2026 · Turno mañana</p>
      </div>

      {/* KPI Cards */}
      <div className="grid grid-cols-4 gap-4 mb-6">
        {kpis.map(k => (
          <div key={k.label} className={`bg-white rounded-xl p-5 shadow-sm border border-slate-100 ${colorMap[k.color]}`}>
            <div className="flex items-start justify-between mb-3">
              <span className="text-2xl">{k.icon}</span>
            </div>
            <div style={{ fontFamily: 'JetBrains Mono, monospace' }} className="text-xl font-500 text-slate-900">{k.value}</div>
            <div className="text-xs font-600 text-slate-700 mt-1">{k.label}</div>
            <div className="text-xs text-slate-400 mt-0.5">{k.sub}</div>
          </div>
        ))}
      </div>

      <div className="grid grid-cols-5 gap-4">
        {/* Bar Chart */}
        <div className="col-span-3 bg-white rounded-xl border border-slate-100 shadow-sm p-5">
          <div className="flex items-center justify-between mb-4">
            <div>
              <h2 className="text-sm font-600 text-slate-900">Ventas por Día</h2>
              <p className="text-xs text-slate-400">Semana actual · En soles (S/)</p>
            </div>
            <Badge label="Esta semana" variant="green" />
          </div>
          <ResponsiveContainer width="100%" height={200}>
            <BarChart data={datosGrafico} barSize={28}>
              <CartesianGrid strokeDasharray="3 3" stroke="#F1F5F9" vertical={false} />
              <XAxis dataKey="day" tick={{ fontSize: 12, fill: '#94A3B8' }} axisLine={false} tickLine={false} />
              <YAxis tick={{ fontSize: 11, fill: '#94A3B8', fontFamily: 'JetBrains Mono' }} axisLine={false} tickLine={false} tickFormatter={v => `${v/1000}k`} />
              <Tooltip
                formatter={(v: number) => [`S/ ${v.toLocaleString()}`, 'Ventas']}
                contentStyle={{ border: '1px solid #E2E8F0', borderRadius: 8, fontSize: 12 }}
              />
              <Bar dataKey="ventas" fill="#059669" radius={[4, 4, 0, 0]} />
            </BarChart>
          </ResponsiveContainer>
        </div>

        {/* Alerts */}
        <div className="col-span-2 bg-white rounded-xl border border-slate-100 shadow-sm p-5">
          <h2 className="text-sm font-600 text-slate-900 mb-4">Productos Inactivos</h2>
          <div className="space-y-3">
            {productos.filter(p => !p.estado).slice(0, 5).map(p => (
              <div key={p.id} className="flex items-center gap-3 py-2 border-b border-slate-50 last:border-0">
                <div className="w-2 h-2 rounded-full flex-shrink-0 bg-red-500" />
                <div className="flex-1 min-w-0">
                  <div className="text-xs font-500 text-slate-800 truncate">{p.nombreComercial}</div>
                  <div className="text-xs text-slate-400">{p.laboratorioNombre}</div>
                </div>
                <Badge label="Inactivo" variant="red" />
              </div>
            ))}
            {productos.filter(p => !p.estado).length === 0 && (
              <p className="text-xs text-slate-400">Todos los productos están activos.</p>
            )}
          </div>
        </div>
      </div>

      {/* Recent Sales Table */}
      <div className="mt-4 bg-white rounded-xl border border-slate-100 shadow-sm">
        <div className="flex items-center justify-between px-6 py-4 border-b border-slate-100">
          <h2 className="text-sm font-600 text-slate-900">Últimas Ventas Registradas</h2>
          <button className="text-xs text-emerald-600 font-500 hover:text-emerald-700">Ver todas →</button>
        </div>
        <table className="w-full text-sm">
          <thead>
            <tr className="border-b border-slate-100">
              {['ID Venta', 'Fecha y Hora', 'Total', 'Estado', ''].map(h => (
                <th key={h} className="text-left px-6 py-3 text-xs font-600 text-slate-400 uppercase tracking-wider">{h}</th>
              ))}
            </tr>
          </thead>
          <tbody>
            {ventas.slice(0, 6).map((s, i) => (
              <tr key={s.id} className={`border-b border-slate-50 hover:bg-slate-50/60 transition-colors ${i === ventas.length - 1 ? 'border-0' : ''}`}>
                <td className="px-6 py-3.5">
                  <span style={{ fontFamily: 'JetBrains Mono, monospace' }} className="text-xs font-500 text-slate-700">VTA-{String(s.id).padStart(4, '0')}</span>
                </td>
                <td className="px-6 py-3.5 text-xs text-slate-500">{formatearFecha(s.fechaVenta)}</td>
                <td className="px-6 py-3.5">
                  <span style={{ fontFamily: 'JetBrains Mono, monospace' }} className="text-sm font-600 text-slate-800">{formatearMoneda(s.total)}</span>
                </td>
                <td className="px-6 py-3.5">
                  <Badge label={s.estado === 'COMPLETADA' ? 'Completada' : 'Anulada'} variant={s.estado === 'COMPLETADA' ? 'green' : 'red'} />
                </td>
                <td className="px-6 py-3.5">
                  <button className="text-xs text-slate-400 hover:text-slate-600">Ver →</button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
        <div className="px-6 py-3 border-t border-slate-100 flex items-center justify-between">
          <span className="text-xs text-slate-400">Mostrando {Math.min(ventas.length, 6)} de {ventas.length} ventas</span>
          <div className="flex gap-1">
            {[1, 2, 3, '…', 45].map((p, i) => (
              <button key={i} className={`w-7 h-7 text-xs rounded-md ${p === 1 ? 'bg-emerald-600 text-white' : 'text-slate-500 hover:bg-slate-100'}`}>
                {p}
              </button>
            ))}
          </div>
        </div>
      </div>
    </div>
  )
}

// ─── Screen: POS ──────────────────────────────────────────────────────────────

function POSScreen({ productos, usuarioId, onVentaRegistrada }: {
  productos: ProductoResumen[]; usuarioId: number; onVentaRegistrada: () => void
}) {
  const [query, setQuery] = useState('')
  const [carrito, setCarrito] = useState<ItemCarrito[]>([])
  const [chatInput, setChatInput] = useState('')
  const [mensajes, setMensajes] = useState<MensajeChat[]>([
    { rol: 'asistente', texto: '¡Hola! Soy tu asistente IA. Cuéntame los síntomas del paciente o pregúntame por algún medicamento específico.' }
  ])
  const [chatLoading, setChatLoading] = useState(false)
  const [procesandoVenta, setProcesandoVenta] = useState(false)
  const searchRef = useRef<HTMLInputElement>(null)
  const chatEndRef = useRef<HTMLDivElement>(null)

  useEffect(() => { searchRef.current?.focus() }, [])
  useEffect(() => { chatEndRef.current?.scrollIntoView({ behavior: 'smooth' }) }, [mensajes])

  const filtrados = query.length >= 2
    ? productos.filter(p => p.estado && p.nombreComercial.toLowerCase().includes(query.toLowerCase()))
    : []

  const agregarAlCarrito = (producto: ProductoResumen) => {
    setCarrito(prev => {
      const existente = prev.find(i => i.producto.id === producto.id)
      if (existente) return prev.map(i => i.producto.id === producto.id ? { ...i, cantidad: i.cantidad + 1 } : i)
      return [...prev, { producto, cantidad: 1 }]
    })
    setQuery('')
    searchRef.current?.focus()
  }

  const actualizarCantidad = (id: number, cantidad: number) => {
    if (cantidad < 1) setCarrito(prev => prev.filter(i => i.producto.id !== id))
    else setCarrito(prev => prev.map(i => i.producto.id === id ? { ...i, cantidad } : i))
  }

  const total = carrito.reduce((s, i) => s + i.producto.precioVenta * i.cantidad, 0)

  const handleSearchKey = (e: KeyboardEvent<HTMLInputElement>) => {
    if (e.key === 'Enter' && filtrados.length === 1) agregarAlCarrito(filtrados[0])
  }

  const registrarVenta = async () => {
    if (carrito.length === 0) return
    setProcesandoVenta(true)
    try {
      await ventaService.crear({
        usuarioId,
        detalles: carrito.map(item => ({
          productoId: item.producto.id,
          cantidad: item.cantidad,
          precioUnitario: item.producto.precioVenta,
        })),
      })
      setCarrito([])
      onVentaRegistrada()
    } catch {
      alert('No se pudo registrar la venta. Verifique la conexión con el backend.')
    } finally {
      setProcesandoVenta(false)
    }
  }

  const enviarChat = async () => {
    if (!chatInput.trim()) return
    const mensajeUsuario = chatInput.trim()
    setChatInput('')
    setMensajes(prev => [...prev, { rol: 'usuario', texto: mensajeUsuario }])
    setChatLoading(true)
    await new Promise(r => setTimeout(r, 800))

    const lower = mensajeUsuario.toLowerCase()
    let respuesta = RESPUESTAS_ASISTENTE.default
    for (const [, config] of Object.entries(RESPUESTAS_ASISTENTE)) {
      if (config.palabrasClave.some(p => lower.includes(p))) {
        respuesta = config
        break
      }
    }

    const sugerencias = productos.filter(p => p.estado).slice(0, 3)
    setMensajes(prev => [...prev, { rol: 'asistente', texto: respuesta.texto, sugerencias }])
    setChatLoading(false)
  }

  return (
    <div className="flex-1 flex overflow-hidden">
      {/* Left: Search + Cart */}
      <div className="flex-1 flex flex-col p-4 gap-3 min-w-0">
        <div className="flex items-center gap-3">
          <h1 style={{ fontFamily: 'DM Sans, sans-serif' }} className="text-xl font-700 text-slate-900">Punto de Venta</h1>
          <Badge label="Caja #1 · Turno mañana" variant="green" />
        </div>

        {/* Search */}
        <div className="relative">
          <div className="absolute left-4 top-1/2 -translate-y-1/2 text-slate-400">
            <svg width="18" height="18" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
              <path strokeLinecap="round" strokeLinejoin="round" d="M21 21l-4.35-4.35M17 11A6 6 0 1 1 5 11a6 6 0 0 1 12 0z" />
            </svg>
          </div>
          <input
            ref={searchRef}
            value={query}
            onChange={e => setQuery(e.target.value)}
            onKeyDown={handleSearchKey}
            placeholder="Buscar medicamento por nombre… (Enter para agregar si hay 1 resultado)"
            className="w-full pl-11 pr-4 py-3.5 rounded-xl border-2 border-slate-200 focus:border-emerald-500 focus:ring-4 focus:ring-emerald-50 text-sm text-slate-800 outline-none transition-all bg-white shadow-sm"
          />
          {query && (
            <button onClick={() => { setQuery(''); searchRef.current?.focus() }}
              className="absolute right-3 top-1/2 -translate-y-1/2 text-slate-300 hover:text-slate-500">
              ✕
            </button>
          )}
        </div>

        {/* Search results dropdown */}
        {filtrados.length > 0 && (
          <div className="bg-white rounded-xl border border-slate-200 shadow-lg overflow-hidden -mt-1">
            {filtrados.slice(0, 5).map(p => (
              <button key={p.id} onClick={() => agregarAlCarrito(p)}
                className="w-full flex items-center gap-4 px-4 py-3 hover:bg-emerald-50 transition-colors border-b border-slate-50 last:border-0 text-left">
                <div className="flex-1 min-w-0">
                  <div className="text-sm font-500 text-slate-800">{p.nombreComercial}</div>
                  <div className="text-xs text-slate-400">{p.categoriaNombre} · {p.laboratorioNombre}</div>
                </div>
                <div className="text-right flex-shrink-0">
                  <div style={{ fontFamily: 'JetBrains Mono, monospace' }} className="text-sm font-600 text-emerald-700">{formatearMoneda(p.precioVenta)}</div>
                </div>
                <div className="w-8 h-8 rounded-lg bg-emerald-100 text-emerald-700 flex items-center justify-center text-lg font-300">+</div>
              </button>
            ))}
          </div>
        )}

        {/* Cart */}
        <div className="flex-1 bg-white rounded-xl border border-slate-200 shadow-sm overflow-hidden flex flex-col">
          <div className="px-4 py-3 border-b border-slate-100 flex items-center justify-between">
            <span className="text-sm font-600 text-slate-700">Carrito de Venta</span>
            <span className="text-xs text-slate-400">{carrito.length} producto{carrito.length !== 1 ? 's' : ''}</span>
          </div>

          {carrito.length === 0 ? (
            <div className="flex-1 flex flex-col items-center justify-center text-slate-300 gap-2">
              <svg width="40" height="40" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={1}>
                <path strokeLinecap="round" strokeLinejoin="round" d="M3 3h2l.4 2M7 13h10l4-8H5.4M7 13L5.4 5M7 13l-2 9m5-9v9m4-9v9m5-9l2 9" />
              </svg>
              <span className="text-sm">Busca un producto para agregar</span>
            </div>
          ) : (
            <div className="flex-1 overflow-y-auto">
              <table className="w-full text-sm">
                <thead>
                  <tr className="border-b border-slate-100 bg-slate-50">
                    {['Producto', 'Cant.', 'P. Unit.', 'Subtotal', ''].map(h => (
                      <th key={h} className="text-left px-4 py-2.5 text-xs font-600 text-slate-400 uppercase tracking-wider">{h}</th>
                    ))}
                  </tr>
                </thead>
                <tbody>
                  {carrito.map(item => (
                    <tr key={item.producto.id} className="border-b border-slate-50 hover:bg-slate-50/50">
                      <td className="px-4 py-3">
                        <div className="font-500 text-slate-800 text-xs">{item.producto.nombreComercial}</div>
                        <div className="text-xs text-slate-400">{item.producto.categoriaNombre}</div>
                      </td>
                      <td className="px-4 py-3">
                        <div className="flex items-center gap-1">
                          <button onClick={() => actualizarCantidad(item.producto.id, item.cantidad - 1)}
                            className="w-6 h-6 rounded bg-slate-100 hover:bg-slate-200 text-slate-600 text-xs flex items-center justify-center">−</button>
                          <span style={{ fontFamily: 'JetBrains Mono, monospace' }} className="w-6 text-center text-sm font-500">{item.cantidad}</span>
                          <button onClick={() => actualizarCantidad(item.producto.id, item.cantidad + 1)}
                            className="w-6 h-6 rounded bg-slate-100 hover:bg-slate-200 text-slate-600 text-xs flex items-center justify-center">+</button>
                        </div>
                      </td>
                      <td className="px-4 py-3">
                        <span style={{ fontFamily: 'JetBrains Mono, monospace' }} className="text-xs text-slate-600">{formatearMoneda(item.producto.precioVenta)}</span>
                      </td>
                      <td className="px-4 py-3">
                        <span style={{ fontFamily: 'JetBrains Mono, monospace' }} className="text-sm font-600 text-slate-800">{formatearMoneda(item.producto.precioVenta * item.cantidad)}</span>
                      </td>
                      <td className="px-4 py-3">
                        <button onClick={() => actualizarCantidad(item.producto.id, 0)} className="text-slate-300 hover:text-red-400 transition-colors">✕</button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}

          {/* Total & Actions */}
          <div className="border-t border-slate-200 p-4">
            <div className="flex items-center justify-between mb-4">
              <span className="text-sm text-slate-500 font-medium">Total a Pagar</span>
              <span style={{ fontFamily: 'JetBrains Mono, monospace' }} className="text-2xl font-700 text-slate-900">{formatearMoneda(total)}</span>
            </div>
            <div className="grid grid-cols-2 gap-3">
              <button onClick={() => setCarrito([])}
                className="py-2.5 rounded-xl border-2 border-red-200 text-red-600 text-sm font-600 hover:bg-red-50 transition-colors">
                Anular Venta
              </button>
              <button disabled={carrito.length === 0 || procesandoVenta} onClick={registrarVenta}
                className="py-2.5 rounded-xl bg-emerald-600 text-white text-sm font-600 hover:bg-emerald-700 disabled:opacity-40 disabled:cursor-not-allowed transition-colors shadow-sm shadow-emerald-200">
                {procesandoVenta ? 'Procesando…' : `Cobrar ${formatearMoneda(total)}`}
              </button>
            </div>
          </div>
        </div>
      </div>

      {/* Right: AI Chatbot */}
      <div style={{ width: 360, minWidth: 360 }} className="flex flex-col border-l border-slate-200 bg-white">
        {/* Header */}
        <div className="px-4 py-3.5 border-b border-slate-100 flex items-center gap-3 bg-gradient-to-r from-emerald-600 to-emerald-700">
          <div className="w-8 h-8 rounded-full bg-white/20 flex items-center justify-center">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="white" strokeWidth={2}>
              <path strokeLinecap="round" strokeLinejoin="round" d="M9.75 9.75l4.5 4.5m0-4.5l-4.5 4.5M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
              <path strokeLinecap="round" strokeLinejoin="round" d="M12 6v.01M12 12v.01" />
            </svg>
          </div>
          <div>
            <div className="text-sm font-600 text-white">Asistente Farmacéutico IA</div>
            <div className="flex items-center gap-1.5">
              <div className="w-1.5 h-1.5 rounded-full bg-emerald-300 animate-pulse" />
              <span className="text-xs text-emerald-200">En línea · Inventario sincronizado</span>
            </div>
          </div>
        </div>

        {/* Messages */}
        <div className="flex-1 overflow-y-auto p-4 space-y-4">
          {mensajes.map((msg, i) => (
            <div key={i} className={`msg-enter flex ${msg.rol === 'usuario' ? 'justify-end' : 'justify-start'} flex-col gap-2`}>
              {msg.rol === 'asistente' && (
                <div className="flex items-end gap-2">
                  <div className="w-6 h-6 rounded-full bg-emerald-100 flex items-center justify-center flex-shrink-0 text-xs">🤖</div>
                  <div className="bg-slate-100 text-slate-800 text-xs leading-relaxed px-3.5 py-2.5 rounded-2xl rounded-bl-sm max-w-[260px]">
                    {msg.texto}
                  </div>
                </div>
              )}
              {msg.rol === 'usuario' && (
                <div className="flex justify-end">
                  <div className="bg-emerald-600 text-white text-xs leading-relaxed px-3.5 py-2.5 rounded-2xl rounded-br-sm max-w-[220px]">
                    {msg.texto}
                  </div>
                </div>
              )}
              {msg.sugerencias && msg.sugerencias.map(p => (
                <div key={p.id} className="ml-8 bg-white border border-slate-200 rounded-xl p-3 shadow-sm">
                  <div className="flex items-start justify-between gap-2 mb-2">
                    <div>
                      <div className="text-xs font-600 text-slate-800 leading-tight">{p.nombreComercial}</div>
                      <div className="text-xs text-slate-400 mt-0.5">{p.laboratorioNombre}</div>
                    </div>
                    <Badge label="Disponible" variant="green" />
                  </div>
                  <div className="flex items-center justify-between">
                    <span style={{ fontFamily: 'JetBrains Mono, monospace' }} className="text-sm font-700 text-emerald-700">{formatearMoneda(p.precioVenta)}</span>
                    <button onClick={() => agregarAlCarrito(p)}
                      className="flex items-center gap-1 px-3 py-1.5 bg-emerald-600 text-white text-xs font-600 rounded-lg hover:bg-emerald-700 transition-colors">
                      + Agregar
                    </button>
                  </div>
                </div>
              ))}
            </div>
          ))}
          {chatLoading && (
            <div className="flex items-end gap-2 msg-enter">
              <div className="w-6 h-6 rounded-full bg-emerald-100 flex items-center justify-center text-xs">🤖</div>
              <div className="bg-slate-100 px-4 py-3 rounded-2xl rounded-bl-sm">
                <div className="flex gap-1">
                  {[0, 1, 2].map(d => (
                    <div key={d} className="w-1.5 h-1.5 bg-slate-400 rounded-full animate-bounce" style={{ animationDelay: `${d * 0.15}s` }} />
                  ))}
                </div>
              </div>
            </div>
          )}
          <div ref={chatEndRef} />
        </div>

        {/* Chat Input */}
        <div className="p-3 border-t border-slate-100">
          <div className="flex gap-2">
            <input
              value={chatInput}
              onChange={e => setChatInput(e.target.value)}
              onKeyDown={e => e.key === 'Enter' && enviarChat()}
              placeholder="Ej: fiebre y tos, ¿qué hay en stock?"
              className="flex-1 text-xs px-3.5 py-2.5 rounded-xl border border-slate-200 focus:border-emerald-500 focus:ring-2 focus:ring-emerald-50 outline-none bg-slate-50 focus:bg-white transition-all"
            />
            <button onClick={enviarChat} disabled={!chatInput.trim() || chatLoading}
              className="w-10 h-10 rounded-xl bg-emerald-600 text-white flex items-center justify-center hover:bg-emerald-700 disabled:opacity-40 transition-colors flex-shrink-0">
              <svg width="16" height="16" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2.5}>
                <path strokeLinecap="round" strokeLinejoin="round" d="M6 12L3.269 3.126A59.768 59.768 0 0121.485 12 59.77 59.77 0 013.27 20.876L5.999 12zm0 0h7.5" />
              </svg>
            </button>
          </div>
          <p className="text-xs text-slate-400 text-center mt-2">Presiona Enter para enviar · Responde en &lt; 2 segundos</p>
        </div>
      </div>
    </div>
  )
}

// ─── Screen: Inventory ────────────────────────────────────────────────────────

function CatalogoScreen({ productos, categorias, laboratorios, cargando, esGerente }: {
  productos: ProductoResumen[]; categorias: Categoria[]; laboratorios: Laboratorio[]
  cargando: boolean; esGerente: boolean
}) {
  const [tab, setTab] = useState<'productos' | 'categorias' | 'laboratorios'>('productos')
  const [busqueda, setBusqueda] = useState('')
  const [pagina, setPagina] = useState(1)
  const [mostrarModal, setMostrarModal] = useState(false)
  const [productoEditando, setProductoEditando] = useState<Partial<ProductoResumen>>({})
  const POR_PAGINA = 8

  const filtrados = productos.filter(p =>
    p.nombreComercial.toLowerCase().includes(busqueda.toLowerCase()) ||
    p.categoriaNombre.toLowerCase().includes(busqueda.toLowerCase()) ||
    p.laboratorioNombre.toLowerCase().includes(busqueda.toLowerCase())
  )
  const paginados = filtrados.slice((pagina - 1) * POR_PAGINA, pagina * POR_PAGINA)
  const totalPaginas = Math.max(1, Math.ceil(filtrados.length / POR_PAGINA))

  return (
    <div className="flex-1 overflow-y-auto p-6">
      <div className="flex items-center justify-between mb-6">
        <div>
          <h1 style={{ fontFamily: 'DM Sans, sans-serif' }} className="text-2xl font-700 text-slate-900">Catálogo e Inventario</h1>
          <p className="text-sm text-slate-400 mt-0.5">{productos.length} productos registrados</p>
        </div>
        {esGerente && (
        <button onClick={() => { setProductoEditando({}); setMostrarModal(true) }}
          className="flex items-center gap-2 px-4 py-2.5 bg-emerald-600 text-white text-sm font-600 rounded-xl hover:bg-emerald-700 transition-colors shadow-sm shadow-emerald-200">
          <span className="text-lg leading-none">+</span> Nuevo Producto
        </button>
        )}
      </div>

      {cargando && <p className="text-sm text-slate-400 mb-4">Cargando catálogo…</p>}

      {/* Tabs */}
      <div className="flex gap-1 mb-4 bg-slate-100 p-1 rounded-xl w-fit">
        {(['productos', 'categorias', 'laboratorios'] as const).map(t => (
          <button key={t} onClick={() => setTab(t)}
            className={`px-4 py-2 rounded-lg text-sm font-500 capitalize transition-all ${
              tab === t ? 'bg-white text-slate-900 shadow-sm' : 'text-slate-500 hover:text-slate-700'
            }`}>
            {t === 'productos' ? `Productos (${productos.length})` : t === 'categorias' ? `Categorías (${categorias.length})` : `Laboratorios (${laboratorios.length})`}
          </button>
        ))}
      </div>

      {tab === 'productos' && (
        <div className="bg-white rounded-xl border border-slate-200 shadow-sm">
          {/* Search + Filter bar */}
          <div className="flex items-center gap-3 px-5 py-4 border-b border-slate-100">
            <div className="relative flex-1 max-w-xs">
              <svg className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" width="14" height="14" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
                <path strokeLinecap="round" strokeLinejoin="round" d="M21 21l-4.35-4.35M17 11A6 6 0 115 11a6 6 0 0112 0z" />
              </svg>
              <input value={busqueda} onChange={e => { setBusqueda(e.target.value); setPagina(1) }}
                placeholder="Buscar productos…"
                className="w-full pl-9 pr-3 py-2 text-sm border border-slate-200 rounded-lg focus:border-emerald-500 focus:ring-2 focus:ring-emerald-50 outline-none" />
            </div>
            <select className="px-3 py-2 text-sm border border-slate-200 rounded-lg text-slate-600 outline-none focus:border-emerald-500">
              <option>Todas las categorías</option>
              {categorias.map(c => <option key={c.id}>{c.nombre}</option>)}
            </select>
            <select className="px-3 py-2 text-sm border border-slate-200 rounded-lg text-slate-600 outline-none focus:border-emerald-500">
              <option>Todos los estados</option>
              <option>Activo</option>
              <option>Inactivo</option>
            </select>
            <span className="ml-auto text-xs text-slate-400">{filtrados.length} resultados</span>
          </div>

          <table className="w-full text-sm">
            <thead>
              <tr className="border-b border-slate-100 bg-slate-50/80">
                {['Nombre / Laboratorio', 'Categoría', 'Precio', 'Estado', ''].map(h => (
                  <th key={h} className="text-left px-5 py-3 text-xs font-600 text-slate-400 uppercase tracking-wider">{h}</th>
                ))}
              </tr>
            </thead>
            <tbody>
              {paginados.map(p => (
                <tr key={p.id} className="border-b border-slate-50 hover:bg-slate-50/60 transition-colors">
                  <td className="px-5 py-3.5">
                    <div className="font-500 text-slate-800 text-sm">{p.nombreComercial}</div>
                    <div className="text-xs text-slate-400">{p.laboratorioNombre}</div>
                  </td>
                  <td className="px-5 py-3.5 text-xs text-slate-500">{p.categoriaNombre}</td>
                  <td className="px-5 py-3.5">
                    <span style={{ fontFamily: 'JetBrains Mono, monospace' }} className="text-sm font-600 text-slate-800">{formatearMoneda(p.precioVenta)}</span>
                  </td>
                  <td className="px-5 py-3.5">
                    <Badge label={p.estado ? 'Activo' : 'Inactivo'} variant={p.estado ? 'green' : 'gray'} />
                  </td>
                  <td className="px-5 py-3.5">
                    {esGerente && (
                    <button onClick={() => { setProductoEditando(p); setMostrarModal(true) }}
                      className="text-xs text-slate-400 hover:text-emerald-600 font-500 transition-colors">Editar</button>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>

          {/* Pagination */}
          <div className="flex items-center justify-between px-5 py-3.5 border-t border-slate-100">
            <span className="text-xs text-slate-400">
              Página {pagina} de {totalPaginas} · {filtrados.length} productos
            </span>
            <div className="flex gap-1">
              <button onClick={() => setPagina(p => Math.max(1, p - 1))} disabled={pagina === 1}
                className="px-3 py-1.5 text-xs rounded-lg border border-slate-200 text-slate-500 hover:bg-slate-50 disabled:opacity-40 disabled:cursor-not-allowed">
                ← Anterior
              </button>
              {Array.from({ length: Math.min(totalPaginas, 5) }, (_, i) => i + 1).map(n => (
                <button key={n} onClick={() => setPagina(n)}
                  className={`w-8 h-8 text-xs rounded-lg ${pagina === n ? 'bg-emerald-600 text-white' : 'border border-slate-200 text-slate-500 hover:bg-slate-50'}`}>
                  {n}
                </button>
              ))}
              <button onClick={() => setPagina(p => Math.min(totalPaginas, p + 1))} disabled={pagina === totalPaginas}
                className="px-3 py-1.5 text-xs rounded-lg border border-slate-200 text-slate-500 hover:bg-slate-50 disabled:opacity-40 disabled:cursor-not-allowed">
                Siguiente →
              </button>
            </div>
          </div>
        </div>
      )}

      {tab === 'categorias' && (
        <div className="bg-white rounded-xl border border-slate-200 shadow-sm">
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b border-slate-100 bg-slate-50/80">
                {['Categoría', 'N° Productos', 'Estado', ''].map(h => (
                  <th key={h} className="text-left px-5 py-3 text-xs font-600 text-slate-400 uppercase tracking-wider">{h}</th>
                ))}
              </tr>
            </thead>
            <tbody>
              {categorias.map(c => {
                const prods = productos.filter(p => p.categoriaId === c.id)
                return (
                  <tr key={c.id} className="border-b border-slate-50 hover:bg-slate-50/60">
                    <td className="px-5 py-3.5 font-500 text-slate-800">{c.nombre}</td>
                    <td className="px-5 py-3.5 text-slate-500">{prods.length}</td>
                    <td className="px-5 py-3.5">
                      <Badge label={c.estado ? 'Activa' : 'Inactiva'} variant={c.estado ? 'green' : 'gray'} />
                    </td>
                    <td className="px-5 py-3.5">
                      <button className="text-xs text-slate-400 hover:text-emerald-600">Ver productos →</button>
                    </td>
                  </tr>
                )
              })}
            </tbody>
          </table>
        </div>
      )}

      {tab === 'laboratorios' && (
        <div className="bg-white rounded-xl border border-slate-200 shadow-sm">
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b border-slate-100 bg-slate-50/80">
                {['Laboratorio', 'N° Productos', 'Valor en Stock', ''].map(h => (
                  <th key={h} className="text-left px-5 py-3 text-xs font-600 text-slate-400 uppercase tracking-wider">{h}</th>
                ))}
              </tr>
            </thead>
            <tbody>
              {laboratorios.map(l => {
                const prods = productos.filter(p => p.laboratorioId === l.id)
                const valor = prods.reduce((s, p) => s + p.precioVenta, 0)
                return (
                  <tr key={l.id} className="border-b border-slate-50 hover:bg-slate-50/60">
                    <td className="px-5 py-3.5 font-500 text-slate-800">{l.nombre}</td>
                    <td className="px-5 py-3.5 text-slate-500">{prods.length}</td>
                    <td className="px-5 py-3.5">
                      <span style={{ fontFamily: 'JetBrains Mono, monospace' }} className="text-sm font-600 text-emerald-700">
                        {formatearMoneda(valor)}
                      </span>
                    </td>
                    <td className="px-5 py-3.5">
                      <button className="text-xs text-slate-400 hover:text-emerald-600">Ver productos →</button>
                    </td>
                  </tr>
                )
              })}
            </tbody>
          </table>
        </div>
      )}

      {/* New/Edit Product Modal */}
      {mostrarModal && (
        <div className="fixed inset-0 bg-black/40 backdrop-blur-sm flex items-center justify-center z-50 p-4">
          <div className="bg-white rounded-2xl shadow-2xl w-full max-w-lg overflow-hidden">
            <div className="flex items-center justify-between px-6 py-5 border-b border-slate-100">
              <div>
                <h2 style={{ fontFamily: 'DM Sans, sans-serif' }} className="text-lg font-700 text-slate-900">
                  {productoEditando.id ? 'Editar Producto' : 'Nuevo Producto'}
                </h2>
                <p className="text-xs text-slate-400 mt-0.5">La creación/edición vía API estará disponible en la siguiente iteración</p>
              </div>
              <button onClick={() => setMostrarModal(false)} className="w-8 h-8 rounded-lg hover:bg-slate-100 flex items-center justify-center text-slate-400 hover:text-slate-600 transition-colors">✕</button>
            </div>
            <div className="p-6">
              <p className="text-sm text-slate-600">Use Swagger o el rol Gerente en la API REST para administrar productos: <code className="text-xs bg-slate-100 px-1 rounded">POST /api/v1/productos</code></p>
            </div>
            <div className="flex gap-3 px-6 py-4 border-t border-slate-100 bg-slate-50">
              <button onClick={() => setMostrarModal(false)}
                className="flex-1 py-2.5 rounded-xl border border-slate-200 text-sm font-500 text-slate-600 hover:bg-white transition-colors">
                Cerrar
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}

// ─── SVG Icons ────────────────────────────────────────────────────────────────

function CrossIcon() {
  return (
    <svg width="16" height="16" viewBox="0 0 16 16" fill="none">
      <rect x="6.5" y="2" width="3" height="12" rx="1.5" fill="white" fillOpacity="0.9" />
      <rect x="2" y="6.5" width="12" height="3" rx="1.5" fill="white" fillOpacity="0.9" />
    </svg>
  )
}

function ChartIcon({ active }: { active: boolean }) {
  return (
    <svg width="16" height="16" fill="none" viewBox="0 0 24 24" stroke={active ? '#059669' : '#94A3B8'} strokeWidth={2}>
      <path strokeLinecap="round" strokeLinejoin="round" d="M3 3v18h18M7 16l4-4 4 4 4-6" />
    </svg>
  )
}

function CashIcon({ active }: { active: boolean }) {
  return (
    <svg width="16" height="16" fill="none" viewBox="0 0 24 24" stroke={active ? '#059669' : '#94A3B8'} strokeWidth={2}>
      <path strokeLinecap="round" strokeLinejoin="round" d="M3 7h18v13H3zM3 7l9-4 9 4M12 12v3" />
    </svg>
  )
}

function BoxIcon({ active }: { active: boolean }) {
  return (
    <svg width="16" height="16" fill="none" viewBox="0 0 24 24" stroke={active ? '#059669' : '#94A3B8'} strokeWidth={2}>
      <path strokeLinecap="round" strokeLinejoin="round" d="M20 7l-8-4-8 4m16 0v10l-8 4M4 7v10l8 4m0-18v18" />
    </svg>
  )
}

function LogoutIcon() {
  return (
    <svg width="16" height="16" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
      <path strokeLinecap="round" strokeLinejoin="round" d="M15 3h4a2 2 0 012 2v14a2 2 0 01-2 2h-4M10 17l5-5-5-5M15 12H3" />
    </svg>
  )
}

function LockIcon() {
  return (
    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={2}>
      <path strokeLinecap="round" strokeLinejoin="round" d="M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z" />
    </svg>
  )
}

// ─── App Root ─────────────────────────────────────────────────────────────────

export default function App() {
  const { autenticado, usuario, cerrarSesion, esGerente } = useAuth()
  const [pantalla, setPantalla] = useState<PantallaApp>('login')
  const [productos, setProductos] = useState<ProductoResumen[]>([])
  const [categorias, setCategorias] = useState<Categoria[]>([])
  const [laboratorios, setLaboratorios] = useState<Laboratorio[]>([])
  const [ventas, setVentas] = useState<Venta[]>([])
  const [cargandoDatos, setCargandoDatos] = useState(false)

  const cargarDatos = useCallback(async () => {
    setCargandoDatos(true)
    try {
      const [productosPagina, categoriasPagina, laboratoriosPagina, ventasPagina] = await Promise.all([
        productoService.listar({ size: 100, page: 0 }),
        categoriaService.listar({ size: 100, page: 0 }),
        laboratorioService.listar({ size: 100, page: 0 }),
        ventaService.listar({ size: 20, page: 0, sort: 'fechaVenta,desc' }),
      ])
      setProductos(productosPagina.content)
      setCategorias(categoriasPagina.content)
      setLaboratorios(laboratoriosPagina.content)
      setVentas(ventasPagina.content)
    } catch {
      console.error('Error al cargar datos del backend')
    } finally {
      setCargandoDatos(false)
    }
  }, [])

  useEffect(() => {
    if (autenticado) {
      cargarDatos()
    }
  }, [autenticado, cargarDatos])

  const handleLogin = (roles: string) => {
    setPantalla(esGerente(roles) ? 'panel' : 'punto-venta')
  }

  const handleLogout = () => {
    cerrarSesion()
    setPantalla('login')
    setProductos([])
    setCategorias([])
    setLaboratorios([])
    setVentas([])
  }

  if (!autenticado || pantalla === 'login') {
    return <LoginScreen onLogin={handleLogin} />
  }

  return (
    <div className="flex h-screen overflow-hidden bg-slate-100">
      <Sidebar
        pantalla={pantalla}
        roles={usuario!.roles}
        nombreUsuario={usuario!.username}
        onNav={setPantalla}
        onLogout={handleLogout}
      />
      <main className="flex-1 flex flex-col overflow-hidden">
        {pantalla === 'panel' && esGerente && (
          <DashboardScreen ventas={ventas} productos={productos} cargando={cargandoDatos} />
        )}
        {pantalla === 'punto-venta' && (
          <POSScreen productos={productos} usuarioId={usuario!.id} onVentaRegistrada={cargarDatos} />
        )}
        {pantalla === 'catalogo' && (
          <CatalogoScreen
            productos={productos}
            categorias={categorias}
            laboratorios={laboratorios}
            cargando={cargandoDatos}
            esGerente={esGerente}
          />
        )}
      </main>
    </div>
  )
}

import { useState, useRef, useEffect, KeyboardEvent } from 'react'
import {
  BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer
} from 'recharts'

// ─── Types ───────────────────────────────────────────────────────────────────

type Screen = 'login' | 'dashboard' | 'pos' | 'inventory'
type Role = 'manager' | 'seller'
type User = { name: string; role: Role }

interface Product {
  id: number; name: string; category: string; lab: string
  price: number; stock: number; expiry: string; active: boolean
}

interface CartItem { product: Product; qty: number }
interface ChatMessage { role: 'user' | 'bot'; text: string; suggestions?: Product[] }

// ─── Mock Data ────────────────────────────────────────────────────────────────

const PRODUCTS: Product[] = [
  { id: 1, name: 'Paracetamol 500mg', category: 'Analgésicos', lab: 'Bayer', price: 3.50, stock: 142, expiry: '2026-08-15', active: true },
  { id: 2, name: 'Ibuprofeno 400mg', category: 'Analgésicos', lab: 'Farmindustria', price: 5.20, stock: 87, expiry: '2026-11-20', active: true },
  { id: 3, name: 'Amoxicilina 500mg', category: 'Antibióticos', lab: 'GlaxoSmithKline', price: 12.80, stock: 34, expiry: '2025-12-01', active: true },
  { id: 4, name: 'Loratadina 10mg', category: 'Antihistamínicos', lab: 'Novartis', price: 6.90, stock: 56, expiry: '2027-03-10', active: true },
  { id: 5, name: 'Omeprazol 20mg', category: 'Gastrointestinal', lab: 'AstraZeneca', price: 8.40, stock: 9, expiry: '2026-06-30', active: true },
  { id: 6, name: 'Metformina 850mg', category: 'Antidiabéticos', lab: 'Merck', price: 14.60, stock: 23, expiry: '2026-09-22', active: true },
  { id: 7, name: 'Atorvastatina 20mg', category: 'Cardiovascular', lab: 'Pfizer', price: 18.30, stock: 5, expiry: '2025-11-15', active: true },
  { id: 8, name: 'Clonazepam 0.5mg', category: 'Neurológico', lab: 'Roche', price: 22.50, stock: 0, expiry: '2026-07-08', active: false },
  { id: 9, name: 'Ciprofloxacino 500mg', category: 'Antibióticos', lab: 'Bayer', price: 16.70, stock: 41, expiry: '2027-01-14', active: true },
  { id: 10, name: 'Azitromicina 500mg', category: 'Antibióticos', lab: 'Pfizer', price: 19.90, stock: 28, expiry: '2026-10-05', active: true },
  { id: 11, name: 'Dexametasona 4mg', category: 'Corticosteroides', lab: 'Merck', price: 7.20, stock: 62, expiry: '2027-04-18', active: true },
  { id: 12, name: 'Ranitidina 150mg', category: 'Gastrointestinal', lab: 'GSK', price: 4.80, stock: 3, expiry: '2025-09-30', active: true },
]

const SALES_DATA = [
  { day: 'Lun', ventas: 1840 }, { day: 'Mar', ventas: 2210 },
  { day: 'Mié', ventas: 1960 }, { day: 'Jue', ventas: 2780 },
  { day: 'Vie', ventas: 3120 }, { day: 'Sáb', ventas: 2950 },
  { day: 'Dom', ventas: 1430 },
]

const RECENT_SALES = [
  { id: 'VTA-0891', date: '11/08/2026 14:32', total: 'S/ 47.60', status: 'Completada' },
  { id: 'VTA-0890', date: '11/08/2026 14:18', total: 'S/ 12.80', status: 'Completada' },
  { id: 'VTA-0889', date: '11/08/2026 13:55', total: 'S/ 89.20', status: 'Anulada' },
  { id: 'VTA-0888', date: '11/08/2026 13:41', total: 'S/ 33.40', status: 'Completada' },
  { id: 'VTA-0887', date: '11/08/2026 12:07', total: 'S/ 156.90', status: 'Completada' },
  { id: 'VTA-0886', date: '11/08/2026 11:48', total: 'S/ 22.10', status: 'Completada' },
]

const AI_RESPONSES: Record<string, { text: string; ids: number[] }> = {
  default: { text: 'He analizado el stock disponible. Aquí tienes opciones relevantes para el paciente:', ids: [1, 2, 4] },
  fiebre: { text: 'Para fiebre y tos recomiendo evaluar estas opciones que están en stock:', ids: [1, 2, 11] },
  antibiotico: { text: 'Para infecciones bacterianas, aquí los antibióticos disponibles:', ids: [3, 9, 10] },
  gastro: { text: 'Para síntomas gastrointestinales, estas son las opciones en stock:', ids: [5, 12] },
  alergia: { text: 'Para cuadros alérgicos, te sugiero:', ids: [4] },
}

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

function Sidebar({ screen, role, onNav, onLogout }: {
  screen: Screen; role: Role; onNav: (s: Screen) => void; onLogout: () => void
}) {
  const items = [
    { id: 'dashboard', label: 'Dashboard', icon: ChartIcon, roles: ['manager'] },
    { id: 'pos', label: 'Punto de Venta', icon: CashIcon, roles: ['manager', 'seller'] },
    { id: 'inventory', label: 'Catálogo', icon: BoxIcon, roles: ['manager', 'seller'] },
  ] as const

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
        {items.filter(i => i.roles.includes(role)).map(item => {
          const Icon = item.icon
          const active = screen === item.id
          return (
            <button
              key={item.id}
              onClick={() => onNav(item.id as Screen)}
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
          <div className="text-xs font-medium text-slate-900">{role === 'manager' ? 'Ana García' : 'Carlos Ríos'}</div>
          <div className="text-xs text-slate-400 capitalize">{role === 'manager' ? 'Gerente' : 'Vendedor'}</div>
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

function LoginScreen({ onLogin }: { onLogin: (role: Role) => void }) {
  const [loading, setLoading] = useState(false)
  const [selectedRole, setSelectedRole] = useState<Role>('seller')

  const handleLogin = () => {
    setLoading(true)
    setTimeout(() => { setLoading(false); onLogin(selectedRole) }, 1400)
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

          {/* Role selector */}
          <div className="mb-5">
            <label className="block text-xs font-600 text-slate-500 mb-2 uppercase tracking-wide">Ingresar como</label>
            <div className="grid grid-cols-2 gap-2">
              {(['manager', 'seller'] as Role[]).map(r => (
                <button
                  key={r}
                  onClick={() => setSelectedRole(r)}
                  className={`py-2.5 px-3 rounded-lg text-sm font-medium border transition-all ${
                    selectedRole === r
                      ? 'bg-emerald-600 text-white border-emerald-600 shadow-sm'
                      : 'bg-slate-50 text-slate-600 border-slate-200 hover:border-slate-300'
                  }`}
                >
                  {r === 'manager' ? '👤 Gerente' : '💊 Vendedor'}
                </button>
              ))}
            </div>
          </div>

          <div className="space-y-4 mb-6">
            <div>
              <label className="block text-xs font-600 text-slate-500 mb-1.5 uppercase tracking-wide">Usuario</label>
              <input
                type="text"
                defaultValue={selectedRole === 'manager' ? 'ana.garcia' : 'carlos.rios'}
                className="w-full px-3.5 py-2.5 rounded-lg border border-slate-200 text-sm text-slate-800 bg-slate-50 focus:bg-white focus:border-emerald-500 focus:ring-2 focus:ring-emerald-100 transition-all outline-none"
              />
            </div>
            <div>
              <label className="block text-xs font-600 text-slate-500 mb-1.5 uppercase tracking-wide">Contraseña</label>
              <input
                type="password"
                defaultValue="••••••••"
                className="w-full px-3.5 py-2.5 rounded-lg border border-slate-200 text-sm text-slate-800 bg-slate-50 focus:bg-white focus:border-emerald-500 focus:ring-2 focus:ring-emerald-100 transition-all outline-none"
              />
            </div>
          </div>

          <button
            onClick={handleLogin}
            disabled={loading}
            className="w-full py-3 rounded-xl bg-emerald-600 hover:bg-emerald-700 text-white text-sm font-600 transition-all shadow-sm shadow-emerald-200 disabled:opacity-70 flex items-center justify-center gap-2"
          >
            {loading ? (
              <>
                <svg className="animate-spin w-4 h-4" fill="none" viewBox="0 0 24 24">
                  <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="3" />
                  <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v8z" />
                </svg>
                Autenticando con Keycloak…
              </>
            ) : (
              <>
                <KeycloakIcon />
                Iniciar Sesión con Credenciales
              </>
            )}
          </button>

          <p className="text-center text-xs text-slate-400 mt-4 flex items-center justify-center gap-1.5">
            <svg width="12" height="12" fill="none" viewBox="0 0 16 16"><path d="M8 1a4 4 0 014 4v2H4V5a4 4 0 014-4zm5 6H3a1 1 0 00-1 1v6a1 1 0 001 1h10a1 1 0 001-1V8a1 1 0 00-1-1z" fill="#94A3B8" /></svg>
            Protegido por OAuth2 / Keycloak
          </p>
        </div>
      </div>
    </div>
  )
}

// ─── Screen: Dashboard ────────────────────────────────────────────────────────

function DashboardScreen() {
  const kpis = [
    { label: 'Ventas del Día', value: 'S/ 3,847.20', sub: '+12.4% vs ayer', icon: '💰', color: 'emerald' },
    { label: 'Bajo Stock', value: '4 productos', sub: 'Requieren reposición', icon: '⚠️', color: 'amber' },
    { label: 'Por Vencer', value: '3 productos', sub: 'Próximos 30 días', icon: '📅', color: 'red' },
    { label: 'Ventas del Mes', value: 'S/ 82,410', sub: 'Meta: S/ 90,000', icon: '📈', color: 'blue' },
  ]

  const colorMap: Record<string, string> = {
    emerald: 'border-l-4 border-emerald-500',
    amber: 'border-l-4 border-amber-500',
    red: 'border-l-4 border-red-500',
    blue: 'border-l-4 border-blue-500',
  }

  return (
    <div className="flex-1 overflow-y-auto p-6">
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
            <BarChart data={SALES_DATA} barSize={28}>
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
          <h2 className="text-sm font-600 text-slate-900 mb-4">Alertas de Stock</h2>
          <div className="space-y-3">
            {PRODUCTS.filter(p => p.stock < 10 || !p.active).slice(0, 5).map(p => (
              <div key={p.id} className="flex items-center gap-3 py-2 border-b border-slate-50 last:border-0">
                <div className={`w-2 h-2 rounded-full flex-shrink-0 ${p.stock === 0 ? 'bg-red-500' : p.stock < 10 ? 'bg-amber-500' : 'bg-slate-200'}`} />
                <div className="flex-1 min-w-0">
                  <div className="text-xs font-500 text-slate-800 truncate">{p.name}</div>
                  <div className="text-xs text-slate-400">{p.stock === 0 ? 'Sin stock' : `${p.stock} unid. restantes`}</div>
                </div>
                <Badge label={p.stock === 0 ? 'Crítico' : 'Bajo'} variant={p.stock === 0 ? 'red' : 'yellow'} />
              </div>
            ))}
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
            {RECENT_SALES.map((s, i) => (
              <tr key={s.id} className={`border-b border-slate-50 hover:bg-slate-50/60 transition-colors ${i === RECENT_SALES.length - 1 ? 'border-0' : ''}`}>
                <td className="px-6 py-3.5">
                  <span style={{ fontFamily: 'JetBrains Mono, monospace' }} className="text-xs font-500 text-slate-700">{s.id}</span>
                </td>
                <td className="px-6 py-3.5 text-xs text-slate-500">{s.date}</td>
                <td className="px-6 py-3.5">
                  <span style={{ fontFamily: 'JetBrains Mono, monospace' }} className="text-sm font-600 text-slate-800">{s.total}</span>
                </td>
                <td className="px-6 py-3.5">
                  <Badge label={s.status} variant={s.status === 'Completada' ? 'green' : 'red'} />
                </td>
                <td className="px-6 py-3.5">
                  <button className="text-xs text-slate-400 hover:text-slate-600">Ver →</button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
        <div className="px-6 py-3 border-t border-slate-100 flex items-center justify-between">
          <span className="text-xs text-slate-400">Mostrando 6 de 891 ventas</span>
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

function POSScreen() {
  const [query, setQuery] = useState('')
  const [cart, setCart] = useState<CartItem[]>([])
  const [chatInput, setChatInput] = useState('')
  const [messages, setMessages] = useState<ChatMessage[]>([
    { role: 'bot', text: '¡Hola! Soy tu asistente IA. Cuéntame los síntomas del paciente o pregúntame por algún medicamento específico.' }
  ])
  const [chatLoading, setChatLoading] = useState(false)
  const searchRef = useRef<HTMLInputElement>(null)
  const chatEndRef = useRef<HTMLDivElement>(null)

  useEffect(() => { searchRef.current?.focus() }, [])
  useEffect(() => { chatEndRef.current?.scrollIntoView({ behavior: 'smooth' }) }, [messages])

  const filtered = query.length >= 2
    ? PRODUCTS.filter(p => p.active && p.stock > 0 && p.name.toLowerCase().includes(query.toLowerCase()))
    : []

  const addToCart = (product: Product) => {
    setCart(prev => {
      const ex = prev.find(i => i.product.id === product.id)
      if (ex) return prev.map(i => i.product.id === product.id ? { ...i, qty: i.qty + 1 } : i)
      return [...prev, { product, qty: 1 }]
    })
    setQuery('')
    searchRef.current?.focus()
  }

  const updateQty = (id: number, qty: number) => {
    if (qty < 1) setCart(prev => prev.filter(i => i.product.id !== id))
    else setCart(prev => prev.map(i => i.product.id === id ? { ...i, qty } : i))
  }

  const total = cart.reduce((s, i) => s + i.product.price * i.qty, 0)

  const handleSearchKey = (e: KeyboardEvent<HTMLInputElement>) => {
    if (e.key === 'Enter' && filtered.length === 1) addToCart(filtered[0])
  }

  const sendChat = async () => {
    if (!chatInput.trim()) return
    const userMsg = chatInput.trim()
    setChatInput('')
    setMessages(prev => [...prev, { role: 'user', text: userMsg }])
    setChatLoading(true)
    await new Promise(r => setTimeout(r, 1200))

    const lower = userMsg.toLowerCase()
    let res = AI_RESPONSES.default
    if (lower.includes('fiebre') || lower.includes('tos') || lower.includes('dolor')) res = AI_RESPONSES.fiebre
    else if (lower.includes('antibio') || lower.includes('infec')) res = AI_RESPONSES.antibiotico
    else if (lower.includes('estomago') || lower.includes('gastro') || lower.includes('acidez')) res = AI_RESPONSES.gastro
    else if (lower.includes('alergi') || lower.includes('rinitis')) res = AI_RESPONSES.alergia

    const suggestions = PRODUCTS.filter(p => res.ids.includes(p.id) && p.stock > 0)
    setMessages(prev => [...prev, { role: 'bot', text: res.text, suggestions }])
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
        {filtered.length > 0 && (
          <div className="bg-white rounded-xl border border-slate-200 shadow-lg overflow-hidden -mt-1">
            {filtered.slice(0, 5).map(p => (
              <button key={p.id} onClick={() => addToCart(p)}
                className="w-full flex items-center gap-4 px-4 py-3 hover:bg-emerald-50 transition-colors border-b border-slate-50 last:border-0 text-left">
                <div className="flex-1 min-w-0">
                  <div className="text-sm font-500 text-slate-800">{p.name}</div>
                  <div className="text-xs text-slate-400">{p.category} · {p.lab}</div>
                </div>
                <div className="text-right flex-shrink-0">
                  <div style={{ fontFamily: 'JetBrains Mono, monospace' }} className="text-sm font-600 text-emerald-700">S/ {p.price.toFixed(2)}</div>
                  <div className="text-xs text-slate-400">{p.stock} en stock</div>
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
            <span className="text-xs text-slate-400">{cart.length} producto{cart.length !== 1 ? 's' : ''}</span>
          </div>

          {cart.length === 0 ? (
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
                  {cart.map(item => (
                    <tr key={item.product.id} className="border-b border-slate-50 hover:bg-slate-50/50">
                      <td className="px-4 py-3">
                        <div className="font-500 text-slate-800 text-xs">{item.product.name}</div>
                        <div className="text-xs text-slate-400">{item.product.category}</div>
                      </td>
                      <td className="px-4 py-3">
                        <div className="flex items-center gap-1">
                          <button onClick={() => updateQty(item.product.id, item.qty - 1)}
                            className="w-6 h-6 rounded bg-slate-100 hover:bg-slate-200 text-slate-600 text-xs flex items-center justify-center">−</button>
                          <span style={{ fontFamily: 'JetBrains Mono, monospace' }} className="w-6 text-center text-sm font-500">{item.qty}</span>
                          <button onClick={() => updateQty(item.product.id, item.qty + 1)}
                            className="w-6 h-6 rounded bg-slate-100 hover:bg-slate-200 text-slate-600 text-xs flex items-center justify-center">+</button>
                        </div>
                      </td>
                      <td className="px-4 py-3">
                        <span style={{ fontFamily: 'JetBrains Mono, monospace' }} className="text-xs text-slate-600">S/ {item.product.price.toFixed(2)}</span>
                      </td>
                      <td className="px-4 py-3">
                        <span style={{ fontFamily: 'JetBrains Mono, monospace' }} className="text-sm font-600 text-slate-800">S/ {(item.product.price * item.qty).toFixed(2)}</span>
                      </td>
                      <td className="px-4 py-3">
                        <button onClick={() => updateQty(item.product.id, 0)} className="text-slate-300 hover:text-red-400 transition-colors">✕</button>
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
              <span style={{ fontFamily: 'JetBrains Mono, monospace' }} className="text-2xl font-700 text-slate-900">S/ {total.toFixed(2)}</span>
            </div>
            <div className="grid grid-cols-2 gap-3">
              <button onClick={() => setCart([])}
                className="py-2.5 rounded-xl border-2 border-red-200 text-red-600 text-sm font-600 hover:bg-red-50 transition-colors">
                Anular Venta
              </button>
              <button disabled={cart.length === 0}
                className="py-2.5 rounded-xl bg-emerald-600 text-white text-sm font-600 hover:bg-emerald-700 disabled:opacity-40 disabled:cursor-not-allowed transition-colors shadow-sm shadow-emerald-200">
                Cobrar S/ {total.toFixed(2)}
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
          {messages.map((msg, i) => (
            <div key={i} className={`msg-enter flex ${msg.role === 'user' ? 'justify-end' : 'justify-start'} flex-col gap-2`}>
              {msg.role === 'bot' && (
                <div className="flex items-end gap-2">
                  <div className="w-6 h-6 rounded-full bg-emerald-100 flex items-center justify-center flex-shrink-0 text-xs">🤖</div>
                  <div className="bg-slate-100 text-slate-800 text-xs leading-relaxed px-3.5 py-2.5 rounded-2xl rounded-bl-sm max-w-[260px]">
                    {msg.text}
                  </div>
                </div>
              )}
              {msg.role === 'user' && (
                <div className="flex justify-end">
                  <div className="bg-emerald-600 text-white text-xs leading-relaxed px-3.5 py-2.5 rounded-2xl rounded-br-sm max-w-[220px]">
                    {msg.text}
                  </div>
                </div>
              )}
              {/* Suggestion Cards */}
              {msg.suggestions && msg.suggestions.map(p => (
                <div key={p.id} className="ml-8 bg-white border border-slate-200 rounded-xl p-3 shadow-sm">
                  <div className="flex items-start justify-between gap-2 mb-2">
                    <div>
                      <div className="text-xs font-600 text-slate-800 leading-tight">{p.name}</div>
                      <div className="text-xs text-slate-400 mt-0.5">{p.lab}</div>
                    </div>
                    <Badge label={`${p.stock} u.`} variant={p.stock < 10 ? 'yellow' : 'green'} />
                  </div>
                  <div className="flex items-center justify-between">
                    <span style={{ fontFamily: 'JetBrains Mono, monospace' }} className="text-sm font-700 text-emerald-700">S/ {p.price.toFixed(2)}</span>
                    <button onClick={() => addToCart(p)}
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
              onKeyDown={e => e.key === 'Enter' && sendChat()}
              placeholder="Ej: fiebre y tos, ¿qué hay en stock?"
              className="flex-1 text-xs px-3.5 py-2.5 rounded-xl border border-slate-200 focus:border-emerald-500 focus:ring-2 focus:ring-emerald-50 outline-none bg-slate-50 focus:bg-white transition-all"
            />
            <button onClick={sendChat} disabled={!chatInput.trim() || chatLoading}
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

function InventoryScreen() {
  const [tab, setTab] = useState<'productos' | 'categorias' | 'laboratorios'>('productos')
  const [search, setSearch] = useState('')
  const [page, setPage] = useState(1)
  const [showModal, setShowModal] = useState(false)
  const [editProduct, setEditProduct] = useState<Partial<Product>>({})
  const PER_PAGE = 8

  const filtered = PRODUCTS.filter(p =>
    p.name.toLowerCase().includes(search.toLowerCase()) ||
    p.category.toLowerCase().includes(search.toLowerCase()) ||
    p.lab.toLowerCase().includes(search.toLowerCase())
  )
  const paginated = filtered.slice((page - 1) * PER_PAGE, page * PER_PAGE)
  const totalPages = Math.ceil(filtered.length / PER_PAGE)

  const cats = [...new Set(PRODUCTS.map(p => p.category))].sort()
  const labs = [...new Set(PRODUCTS.map(p => p.lab))].sort()

  const isExpiringSoon = (d: string) => {
    const diff = (new Date(d).getTime() - Date.now()) / (1000 * 60 * 60 * 24)
    return diff < 60
  }

  return (
    <div className="flex-1 overflow-y-auto p-6">
      <div className="flex items-center justify-between mb-6">
        <div>
          <h1 style={{ fontFamily: 'DM Sans, sans-serif' }} className="text-2xl font-700 text-slate-900">Catálogo e Inventario</h1>
          <p className="text-sm text-slate-400 mt-0.5">{PRODUCTS.length} productos registrados</p>
        </div>
        <button onClick={() => { setEditProduct({}); setShowModal(true) }}
          className="flex items-center gap-2 px-4 py-2.5 bg-emerald-600 text-white text-sm font-600 rounded-xl hover:bg-emerald-700 transition-colors shadow-sm shadow-emerald-200">
          <span className="text-lg leading-none">+</span> Nuevo Producto
        </button>
      </div>

      {/* Tabs */}
      <div className="flex gap-1 mb-4 bg-slate-100 p-1 rounded-xl w-fit">
        {(['productos', 'categorias', 'laboratorios'] as const).map(t => (
          <button key={t} onClick={() => setTab(t)}
            className={`px-4 py-2 rounded-lg text-sm font-500 capitalize transition-all ${
              tab === t ? 'bg-white text-slate-900 shadow-sm' : 'text-slate-500 hover:text-slate-700'
            }`}>
            {t === 'productos' ? `Productos (${PRODUCTS.length})` : t === 'categorias' ? `Categorías (${cats.length})` : `Laboratorios (${labs.length})`}
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
              <input value={search} onChange={e => { setSearch(e.target.value); setPage(1) }}
                placeholder="Buscar productos…"
                className="w-full pl-9 pr-3 py-2 text-sm border border-slate-200 rounded-lg focus:border-emerald-500 focus:ring-2 focus:ring-emerald-50 outline-none" />
            </div>
            <select className="px-3 py-2 text-sm border border-slate-200 rounded-lg text-slate-600 outline-none focus:border-emerald-500">
              <option>Todas las categorías</option>
              {cats.map(c => <option key={c}>{c}</option>)}
            </select>
            <select className="px-3 py-2 text-sm border border-slate-200 rounded-lg text-slate-600 outline-none focus:border-emerald-500">
              <option>Todos los estados</option>
              <option>Activo</option>
              <option>Inactivo</option>
            </select>
            <span className="ml-auto text-xs text-slate-400">{filtered.length} resultados</span>
          </div>

          <table className="w-full text-sm">
            <thead>
              <tr className="border-b border-slate-100 bg-slate-50/80">
                {['Nombre / Laboratorio', 'Categoría', 'Precio', 'Stock', 'Vencimiento', 'Estado', ''].map(h => (
                  <th key={h} className="text-left px-5 py-3 text-xs font-600 text-slate-400 uppercase tracking-wider">{h}</th>
                ))}
              </tr>
            </thead>
            <tbody>
              {paginated.map(p => (
                <tr key={p.id} className="border-b border-slate-50 hover:bg-slate-50/60 transition-colors">
                  <td className="px-5 py-3.5">
                    <div className="font-500 text-slate-800 text-sm">{p.name}</div>
                    <div className="text-xs text-slate-400">{p.lab}</div>
                  </td>
                  <td className="px-5 py-3.5 text-xs text-slate-500">{p.category}</td>
                  <td className="px-5 py-3.5">
                    <span style={{ fontFamily: 'JetBrains Mono, monospace' }} className="text-sm font-600 text-slate-800">S/ {p.price.toFixed(2)}</span>
                  </td>
                  <td className="px-5 py-3.5">
                    <span style={{ fontFamily: 'JetBrains Mono, monospace' }} className={`text-sm font-600 ${p.stock === 0 ? 'text-red-600' : p.stock < 10 ? 'text-amber-600' : 'text-slate-700'}`}>
                      {p.stock}
                    </span>
                  </td>
                  <td className="px-5 py-3.5">
                    <span className={`text-xs font-500 ${isExpiringSoon(p.expiry) ? 'text-amber-600' : 'text-slate-500'}`}>
                      {new Date(p.expiry).toLocaleDateString('es-PE', { day: '2-digit', month: 'short', year: 'numeric' })}
                    </span>
                  </td>
                  <td className="px-5 py-3.5">
                    <Badge label={p.active ? 'Activo' : 'Inactivo'} variant={p.active ? 'green' : 'gray'} />
                  </td>
                  <td className="px-5 py-3.5">
                    <button onClick={() => { setEditProduct(p); setShowModal(true) }}
                      className="text-xs text-slate-400 hover:text-emerald-600 font-500 transition-colors">Editar</button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>

          {/* Pagination */}
          <div className="flex items-center justify-between px-5 py-3.5 border-t border-slate-100">
            <span className="text-xs text-slate-400">
              Página {page} de {totalPages} · {filtered.length} productos (20 por página en producción)
            </span>
            <div className="flex gap-1">
              <button onClick={() => setPage(p => Math.max(1, p - 1))} disabled={page === 1}
                className="px-3 py-1.5 text-xs rounded-lg border border-slate-200 text-slate-500 hover:bg-slate-50 disabled:opacity-40 disabled:cursor-not-allowed">
                ← Anterior
              </button>
              {Array.from({ length: Math.min(totalPages, 5) }, (_, i) => i + 1).map(n => (
                <button key={n} onClick={() => setPage(n)}
                  className={`w-8 h-8 text-xs rounded-lg ${page === n ? 'bg-emerald-600 text-white' : 'border border-slate-200 text-slate-500 hover:bg-slate-50'}`}>
                  {n}
                </button>
              ))}
              <button onClick={() => setPage(p => Math.min(totalPages, p + 1))} disabled={page === totalPages}
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
                {['Categoría', 'N° Productos', 'Stock Total', ''].map(h => (
                  <th key={h} className="text-left px-5 py-3 text-xs font-600 text-slate-400 uppercase tracking-wider">{h}</th>
                ))}
              </tr>
            </thead>
            <tbody>
              {cats.map(c => {
                const prods = PRODUCTS.filter(p => p.category === c)
                return (
                  <tr key={c} className="border-b border-slate-50 hover:bg-slate-50/60">
                    <td className="px-5 py-3.5 font-500 text-slate-800">{c}</td>
                    <td className="px-5 py-3.5 text-slate-500">{prods.length}</td>
                    <td className="px-5 py-3.5">
                      <span style={{ fontFamily: 'JetBrains Mono, monospace' }} className="text-sm font-600 text-slate-700">
                        {prods.reduce((s, p) => s + p.stock, 0)} unid.
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
              {labs.map(l => {
                const prods = PRODUCTS.filter(p => p.lab === l)
                const valor = prods.reduce((s, p) => s + p.stock * p.price, 0)
                return (
                  <tr key={l} className="border-b border-slate-50 hover:bg-slate-50/60">
                    <td className="px-5 py-3.5 font-500 text-slate-800">{l}</td>
                    <td className="px-5 py-3.5 text-slate-500">{prods.length}</td>
                    <td className="px-5 py-3.5">
                      <span style={{ fontFamily: 'JetBrains Mono, monospace' }} className="text-sm font-600 text-emerald-700">
                        S/ {valor.toFixed(2)}
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
      {showModal && (
        <div className="fixed inset-0 bg-black/40 backdrop-blur-sm flex items-center justify-center z-50 p-4">
          <div className="bg-white rounded-2xl shadow-2xl w-full max-w-lg overflow-hidden">
            <div className="flex items-center justify-between px-6 py-5 border-b border-slate-100">
              <div>
                <h2 style={{ fontFamily: 'DM Sans, sans-serif' }} className="text-lg font-700 text-slate-900">
                  {editProduct.id ? 'Editar Producto' : 'Nuevo Producto'}
                </h2>
                <p className="text-xs text-slate-400 mt-0.5">Complete todos los campos requeridos</p>
              </div>
              <button onClick={() => setShowModal(false)} className="w-8 h-8 rounded-lg hover:bg-slate-100 flex items-center justify-center text-slate-400 hover:text-slate-600 transition-colors">✕</button>
            </div>
            <div className="p-6 grid grid-cols-2 gap-4">
              <div className="col-span-2">
                <label className="block text-xs font-600 text-slate-500 mb-1.5 uppercase tracking-wide">Nombre del Medicamento *</label>
                <input defaultValue={editProduct.name || ''} placeholder="Ej: Paracetamol 500mg"
                  className="w-full px-3.5 py-2.5 rounded-lg border border-slate-200 text-sm focus:border-emerald-500 focus:ring-2 focus:ring-emerald-50 outline-none" />
              </div>
              <div>
                <label className="block text-xs font-600 text-slate-500 mb-1.5 uppercase tracking-wide">Categoría *</label>
                <select defaultValue={editProduct.category || ''} className="w-full px-3.5 py-2.5 rounded-lg border border-slate-200 text-sm text-slate-700 focus:border-emerald-500 outline-none appearance-none bg-white">
                  <option value="">Seleccionar…</option>
                  {cats.map(c => <option key={c}>{c}</option>)}
                </select>
              </div>
              <div>
                <label className="block text-xs font-600 text-slate-500 mb-1.5 uppercase tracking-wide">Laboratorio *</label>
                <select defaultValue={editProduct.lab || ''} className="w-full px-3.5 py-2.5 rounded-lg border border-slate-200 text-sm text-slate-700 focus:border-emerald-500 outline-none appearance-none bg-white">
                  <option value="">Seleccionar…</option>
                  {labs.map(l => <option key={l}>{l}</option>)}
                </select>
              </div>
              <div>
                <label className="block text-xs font-600 text-slate-500 mb-1.5 uppercase tracking-wide">Precio (S/) *</label>
                <input type="number" step="0.01" min="0" defaultValue={editProduct.price || ''} placeholder="0.00"
                  className="w-full px-3.5 py-2.5 rounded-lg border border-slate-200 text-sm focus:border-emerald-500 focus:ring-2 focus:ring-emerald-50 outline-none" />
              </div>
              <div>
                <label className="block text-xs font-600 text-slate-500 mb-1.5 uppercase tracking-wide">Stock Inicial</label>
                <input type="number" min="0" defaultValue={editProduct.stock || 0}
                  className="w-full px-3.5 py-2.5 rounded-lg border border-slate-200 text-sm focus:border-emerald-500 focus:ring-2 focus:ring-emerald-50 outline-none" />
              </div>
              <div>
                <label className="block text-xs font-600 text-slate-500 mb-1.5 uppercase tracking-wide">Fecha de Vencimiento</label>
                <input type="date" defaultValue={editProduct.expiry || ''}
                  className="w-full px-3.5 py-2.5 rounded-lg border border-slate-200 text-sm focus:border-emerald-500 focus:ring-2 focus:ring-emerald-50 outline-none" />
              </div>
              <div>
                <label className="block text-xs font-600 text-slate-500 mb-1.5 uppercase tracking-wide">Estado</label>
                <select defaultValue={editProduct.active !== false ? 'activo' : 'inactivo'}
                  className="w-full px-3.5 py-2.5 rounded-lg border border-slate-200 text-sm text-slate-700 focus:border-emerald-500 outline-none bg-white">
                  <option value="activo">Activo</option>
                  <option value="inactivo">Inactivo</option>
                </select>
              </div>
              <div className="col-span-2">
                <label className="block text-xs font-600 text-slate-500 mb-1.5 uppercase tracking-wide">Indicaciones Médicas</label>
                <textarea rows={3} defaultValue="" placeholder="Indicaciones, contraindicaciones, dosis recomendada…"
                  className="w-full px-3.5 py-2.5 rounded-lg border border-slate-200 text-sm focus:border-emerald-500 focus:ring-2 focus:ring-emerald-50 outline-none resize-none" />
              </div>
            </div>
            <div className="flex gap-3 px-6 py-4 border-t border-slate-100 bg-slate-50">
              <button onClick={() => setShowModal(false)}
                className="flex-1 py-2.5 rounded-xl border border-slate-200 text-sm font-500 text-slate-600 hover:bg-white transition-colors">
                Cancelar
              </button>
              <button onClick={() => setShowModal(false)}
                className="flex-1 py-2.5 rounded-xl bg-emerald-600 text-white text-sm font-600 hover:bg-emerald-700 transition-colors shadow-sm shadow-emerald-100">
                {editProduct.id ? 'Guardar Cambios' : 'Crear Producto'}
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

function KeycloakIcon() {
  return (
    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={2}>
      <path strokeLinecap="round" strokeLinejoin="round" d="M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z" />
    </svg>
  )
}

// ─── App Root ─────────────────────────────────────────────────────────────────

export default function App() {
  const [screen, setScreen] = useState<Screen>('login')
  const [user, setUser] = useState<User | null>(null)

  const handleLogin = (role: Role) => {
    setUser({ name: role === 'manager' ? 'Ana García' : 'Carlos Ríos', role })
    setScreen(role === 'manager' ? 'dashboard' : 'pos')
  }

  const handleLogout = () => { setUser(null); setScreen('login') }

  if (screen === 'login') return <LoginScreen onLogin={handleLogin} />

  return (
    <div className="flex h-screen overflow-hidden bg-slate-100">
      <Sidebar screen={screen} role={user!.role} onNav={setScreen} onLogout={handleLogout} />
      <main className="flex-1 flex flex-col overflow-hidden">
        {screen === 'dashboard' && <DashboardScreen />}
        {screen === 'pos' && <POSScreen />}
        {screen === 'inventory' && <InventoryScreen />}
      </main>
    </div>
  )
}

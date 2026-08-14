import { useState } from 'react';
import { Badge } from '../../components/ui/Badge';
import { formatearMoneda } from '../../utils/format';
import type { Categoria, Laboratorio, ProductoResumen } from '../../types/domain.types';

interface CatalogoScreenProps {
  productos: ProductoResumen[];
  categorias: Categoria[];
  laboratorios: Laboratorio[];
  cargando: boolean;
  esGerente: boolean;
}

const POR_PAGINA = 8;

export function CatalogoScreen({ productos, categorias, laboratorios, cargando, esGerente }: CatalogoScreenProps) {
  const [tab, setTab] = useState<'productos' | 'categorias' | 'laboratorios'>('productos');
  const [busqueda, setBusqueda] = useState('');
  const [pagina, setPagina] = useState(1);
  const [mostrarModal, setMostrarModal] = useState(false);
  const [productoEditando, setProductoEditando] = useState<Partial<ProductoResumen>>({});

  const filtrados = productos.filter((p) =>
    p.nombreComercial.toLowerCase().includes(busqueda.toLowerCase()) ||
    p.categoriaNombre.toLowerCase().includes(busqueda.toLowerCase()) ||
    p.laboratorioNombre.toLowerCase().includes(busqueda.toLowerCase()),
  );
  const paginados = filtrados.slice((pagina - 1) * POR_PAGINA, pagina * POR_PAGINA);
  const totalPaginas = Math.max(1, Math.ceil(filtrados.length / POR_PAGINA));

  return (
    <div className="flex-1 overflow-y-auto p-6">
      <div className="flex items-center justify-between mb-6">
        <div>
          <h1 style={{ fontFamily: 'DM Sans, sans-serif' }} className="text-2xl font-700 text-slate-900">Catálogo e Inventario</h1>
          <p className="text-sm text-slate-400 mt-0.5">{productos.length} productos registrados</p>
        </div>
        {esGerente && (
          <button onClick={() => { setProductoEditando({}); setMostrarModal(true); }}
            className="flex items-center gap-2 px-4 py-2.5 bg-emerald-600 text-white text-sm font-600 rounded-xl hover:bg-emerald-700 transition-colors shadow-sm shadow-emerald-200">
            <span className="text-lg leading-none">+</span> Nuevo Producto
          </button>
        )}
      </div>

      {cargando && <p className="text-sm text-slate-400 mb-4">Cargando catálogo…</p>}

      <div className="flex gap-1 mb-4 bg-slate-100 p-1 rounded-xl w-fit">
        {(['productos', 'categorias', 'laboratorios'] as const).map((t) => (
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
          <div className="flex items-center gap-3 px-5 py-4 border-b border-slate-100">
            <div className="relative flex-1 max-w-xs">
              <svg className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" width="14" height="14" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
                <path strokeLinecap="round" strokeLinejoin="round" d="M21 21l-4.35-4.35M17 11A6 6 0 115 11a6 6 0 0112 0z" />
              </svg>
              <input value={busqueda} onChange={(e) => { setBusqueda(e.target.value); setPagina(1); }}
                placeholder="Buscar productos…"
                className="w-full pl-9 pr-3 py-2 text-sm border border-slate-200 rounded-lg focus:border-emerald-500 focus:ring-2 focus:ring-emerald-50 outline-none" />
            </div>
            <span className="ml-auto text-xs text-slate-400">{filtrados.length} resultados</span>
          </div>

          <table className="w-full text-sm">
            <thead>
              <tr className="border-b border-slate-100 bg-slate-50/80">
                {['Nombre / Laboratorio', 'Categoría', 'Precio', 'Estado', ''].map((h) => (
                  <th key={h} className="text-left px-5 py-3 text-xs font-600 text-slate-400 uppercase tracking-wider">{h}</th>
                ))}
              </tr>
            </thead>
            <tbody>
              {paginados.map((p) => (
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
                      <button onClick={() => { setProductoEditando(p); setMostrarModal(true); }}
                        className="text-xs text-slate-400 hover:text-emerald-600 font-500 transition-colors">Editar</button>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>

          <div className="flex items-center justify-between px-5 py-3.5 border-t border-slate-100">
            <span className="text-xs text-slate-400">
              Página {pagina} de {totalPaginas} · {filtrados.length} productos
            </span>
            <div className="flex gap-1">
              <button onClick={() => setPagina((p) => Math.max(1, p - 1))} disabled={pagina === 1}
                className="px-3 py-1.5 text-xs rounded-lg border border-slate-200 text-slate-500 hover:bg-slate-50 disabled:opacity-40 disabled:cursor-not-allowed">
                ← Anterior
              </button>
              {Array.from({ length: Math.min(totalPaginas, 5) }, (_, i) => i + 1).map((n) => (
                <button key={n} onClick={() => setPagina(n)}
                  className={`w-8 h-8 text-xs rounded-lg ${pagina === n ? 'bg-emerald-600 text-white' : 'border border-slate-200 text-slate-500 hover:bg-slate-50'}`}>
                  {n}
                </button>
              ))}
              <button onClick={() => setPagina((p) => Math.min(totalPaginas, p + 1))} disabled={pagina === totalPaginas}
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
                {['Categoría', 'N° Productos', 'Estado', ''].map((h) => (
                  <th key={h} className="text-left px-5 py-3 text-xs font-600 text-slate-400 uppercase tracking-wider">{h}</th>
                ))}
              </tr>
            </thead>
            <tbody>
              {categorias.map((c) => {
                const prods = productos.filter((p) => p.categoriaId === c.id);
                return (
                  <tr key={c.id} className="border-b border-slate-50 hover:bg-slate-50/60">
                    <td className="px-5 py-3.5 font-500 text-slate-800">{c.nombre}</td>
                    <td className="px-5 py-3.5 text-slate-500">{prods.length}</td>
                    <td className="px-5 py-3.5">
                      <Badge label={c.estado ? 'Activa' : 'Inactiva'} variant={c.estado ? 'green' : 'gray'} />
                    </td>
                    <td className="px-5 py-3.5">
                      <span className="text-xs text-slate-400">—</span>
                    </td>
                  </tr>
                );
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
                {['Laboratorio', 'N° Productos', 'Valor en Stock', ''].map((h) => (
                  <th key={h} className="text-left px-5 py-3 text-xs font-600 text-slate-400 uppercase tracking-wider">{h}</th>
                ))}
              </tr>
            </thead>
            <tbody>
              {laboratorios.map((l) => {
                const prods = productos.filter((p) => p.laboratorioId === l.id);
                const valor = prods.reduce((s, p) => s + p.precioVenta, 0);
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
                      <span className="text-xs text-slate-400">—</span>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      )}

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
  );
}
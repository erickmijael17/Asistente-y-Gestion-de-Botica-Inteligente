import { useEffect, useState } from 'react';
import { Badge } from '../../components/ui/Badge';
import { formatearMoneda } from '../../utils/format';
import type { Categoria, Laboratorio, ProductoResumen } from '../../types/domain.types';
import { productoService } from '../../services/producto.service';

interface CatalogoScreenProps {
  productos: ProductoResumen[];
  categorias: Categoria[];
  laboratorios: Laboratorio[];
  cargando: boolean;
  esGerente: boolean;
  onRecargar?: () => void;
}

const POR_PAGINA = 8;

type FormState = {
  codigoInterno: string;
  codigoBarras: string;
  nombreComercial: string;
  nombreGenerico: string;
  tipoProducto: 'MEDICAMENTO' | 'SUPLEMENTO' | 'VITAMINA' | 'OTRO';
  categoriaId: string;
  laboratorioId: string;
  precioCompra: string;
  precioVenta: string;
  stockMinimo: string;
  descripcion: string;
};

const initialForm: FormState = {
  codigoInterno: '',
  codigoBarras: '',
  nombreComercial: '',
  nombreGenerico: '',
  tipoProducto: 'MEDICAMENTO',
  categoriaId: '',
  laboratorioId: '',
  precioCompra: '',
  precioVenta: '',
  stockMinimo: '0',
  descripcion: '',
};

export function CatalogoScreen({ productos, categorias, laboratorios, cargando, esGerente, onRecargar }: CatalogoScreenProps) {
  const [tab, setTab] = useState<'productos' | 'categorias' | 'laboratorios'>('productos');
  const [busqueda, setBusqueda] = useState('');
  const [pagina, setPagina] = useState(1);
  const [mostrarModal, setMostrarModal] = useState(false);
  const [productoEditando, setProductoEditando] = useState<ProductoResumen | null>(null);
  const [form, setForm] = useState<FormState>(initialForm);
  const [guardando, setGuardando] = useState(false);
  const [errorForm, setErrorForm] = useState('');
  const [exito, setExito] = useState('');

  const filtrados = productos.filter((p) =>
    p.nombreComercial.toLowerCase().includes(busqueda.toLowerCase()) ||
    p.categoriaNombre.toLowerCase().includes(busqueda.toLowerCase()) ||
    p.laboratorioNombre.toLowerCase().includes(busqueda.toLowerCase()),
  );
  const paginados = filtrados.slice((pagina - 1) * POR_PAGINA, pagina * POR_PAGINA);
  const totalPaginas = Math.max(1, Math.ceil(filtrados.length / POR_PAGINA));

  useEffect(() => {
    if (mostrarModal) {
      if (productoEditando?.id) {
        setForm({
          codigoInterno: productoEditando.codigoInterno || '',
          codigoBarras: productoEditando.codigoBarras || '',
          nombreComercial: productoEditando.nombreComercial || '',
          nombreGenerico: '',
          tipoProducto: (productoEditando.tipoProducto as FormState['tipoProducto']) || 'MEDICAMENTO',
          categoriaId: String(productoEditando.categoriaId || ''),
          laboratorioId: String(productoEditando.laboratorioId || ''),
          precioCompra: '',
          precioVenta: String(productoEditando.precioVenta || ''),
          stockMinimo: '0',
          descripcion: '',
        });
      } else {
        setForm(initialForm);
      }
      setErrorForm('');
      setExito('');
    }
  }, [mostrarModal, productoEditando]);

  const abrirNuevo = () => {
    setProductoEditando(null);
    setMostrarModal(true);
  };

  const abrirEditar = (p: ProductoResumen) => {
    setProductoEditando(p);
    setMostrarModal(true);
  };

  const handleGuardar = async () => {
    setErrorForm('');
    setExito('');
    if (!form.codigoInterno.trim() || !form.nombreComercial.trim() || !form.categoriaId || !form.laboratorioId || !form.precioCompra || !form.precioVenta) {
      setErrorForm('Completa los campos obligatorios: código interno, nombre comercial, categoría, laboratorio y precios.');
      return;
    }
    const precioCompra = Number(form.precioCompra);
    const precioVenta = Number(form.precioVenta);
    const stockMinimo = Number(form.stockMinimo);
    if (Number.isNaN(precioCompra) || precioCompra < 0) {
      setErrorForm('Precio compra debe ser >= 0');
      return;
    }
    if (Number.isNaN(precioVenta) || precioVenta <= 0) {
      setErrorForm('Precio venta debe ser > 0');
      return;
    }
    if (precioVenta < precioCompra) {
      setErrorForm('Precio venta debe ser >= precio compra');
      return;
    }

    setGuardando(true);
    try {
      const payload: Record<string, unknown> = {
        codigoInterno: form.codigoInterno.trim(),
        codigoBarras: form.codigoBarras.trim() || null,
        nombreComercial: form.nombreComercial.trim(),
        nombreGenerico: form.nombreGenerico.trim() || null,
        descripcion: form.descripcion.trim() || null,
        tipoProducto: form.tipoProducto,
        categoriaId: Number(form.categoriaId),
        laboratorioId: Number(form.laboratorioId),
        precioCompra,
        precioVenta,
        stockMinimo: Number.isNaN(stockMinimo) ? 0 : stockMinimo,
      };
      if (productoEditando?.id) {
        await productoService.actualizar(productoEditando.id, payload);
        setExito('Producto actualizado correctamente');
      } else {
        await productoService.crear(payload);
        setExito('Producto creado correctamente');
      }
      setTimeout(() => {
        setMostrarModal(false);
        onRecargar?.();
      }, 800);
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : '';
      // Axios error con response
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      const axiosErr = err as any;
      const backendMsg = axiosErr?.response?.data?.message || axiosErr?.response?.data?.error || msg;
      setErrorForm(backendMsg || 'No se pudo guardar el producto. Verifica los datos.');
    } finally {
      setGuardando(false);
    }
  };

  return (
    <div className="flex-1 overflow-y-auto p-6">
      <div className="flex items-center justify-between mb-6">
        <div>
          <h1 style={{ fontFamily: 'DM Sans, sans-serif' }} className="text-2xl font-700 text-slate-900">Catálogo e Inventario</h1>
          <p className="text-sm text-slate-400 mt-0.5">{productos.length} productos registrados</p>
        </div>
        {esGerente && (
          <button onClick={abrirNuevo}
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
                      <button onClick={() => abrirEditar(p)}
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
          <div className="bg-white rounded-2xl shadow-2xl w-full max-w-xl overflow-hidden max-h-[90vh] flex flex-col">
            <div className="flex items-center justify-between px-6 py-5 border-b border-slate-100">
              <div>
                <h2 style={{ fontFamily: 'DM Sans, sans-serif' }} className="text-lg font-700 text-slate-900">
                  {productoEditando?.id ? 'Editar Producto' : 'Nuevo Producto'}
                </h2>
                <p className="text-xs text-slate-400 mt-0.5">{productoEditando?.id ? 'Actualiza los datos del producto' : 'Completa los datos obligatorios (*)'}</p>
              </div>
              <button onClick={() => setMostrarModal(false)} className="w-8 h-8 rounded-lg hover:bg-slate-100 flex items-center justify-center text-slate-400 hover:text-slate-600 transition-colors">✕</button>
            </div>
            <div className="p-6 space-y-4 overflow-y-auto">
              {errorForm && <div className="p-3 rounded-lg bg-red-50 border border-red-200 text-red-700 text-xs">{errorForm}</div>}
              {exito && <div className="p-3 rounded-lg bg-emerald-50 border border-emerald-200 text-emerald-700 text-xs">{exito}</div>}
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-xs font-600 text-slate-500 mb-1">Código interno *</label>
                  <input value={form.codigoInterno} onChange={(e) => setForm({ ...form, codigoInterno: e.target.value })} placeholder="MED-001" className="w-full px-3 py-2 text-sm border border-slate-200 rounded-lg focus:border-emerald-500 focus:ring-2 focus:ring-emerald-50 outline-none" />
                </div>
                <div>
                  <label className="block text-xs font-600 text-slate-500 mb-1">Código barras</label>
                  <input value={form.codigoBarras} onChange={(e) => setForm({ ...form, codigoBarras: e.target.value })} placeholder="750..." className="w-full px-3 py-2 text-sm border border-slate-200 rounded-lg focus:border-emerald-500 focus:ring-2 focus:ring-emerald-50 outline-none" />
                </div>
              </div>
              <div>
                <label className="block text-xs font-600 text-slate-500 mb-1">Nombre comercial *</label>
                <input value={form.nombreComercial} onChange={(e) => setForm({ ...form, nombreComercial: e.target.value })} placeholder="Paracetamol 500mg" className="w-full px-3 py-2 text-sm border border-slate-200 rounded-lg focus:border-emerald-500 focus:ring-2 focus:ring-emerald-50 outline-none" />
              </div>
              <div>
                <label className="block text-xs font-600 text-slate-500 mb-1">Nombre genérico</label>
                <input value={form.nombreGenerico} onChange={(e) => setForm({ ...form, nombreGenerico: e.target.value })} placeholder="Paracetamol" className="w-full px-3 py-2 text-sm border border-slate-200 rounded-lg focus:border-emerald-500 focus:ring-2 focus:ring-emerald-50 outline-none" />
              </div>
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-xs font-600 text-slate-500 mb-1">Tipo *</label>
                  <select value={form.tipoProducto} onChange={(e) => setForm({ ...form, tipoProducto: e.target.value as FormState['tipoProducto'] })} className="w-full px-3 py-2 text-sm border border-slate-200 rounded-lg focus:border-emerald-500 outline-none">
                    <option value="MEDICAMENTO">MEDICAMENTO</option>
                    <option value="SUPLEMENTO">SUPLEMENTO</option>
                    <option value="VITAMINA">VITAMINA</option>
                    <option value="OTRO">OTRO</option>
                  </select>
                </div>
                <div>
                  <label className="block text-xs font-600 text-slate-500 mb-1">Stock mínimo *</label>
                  <input type="number" min={0} value={form.stockMinimo} onChange={(e) => setForm({ ...form, stockMinimo: e.target.value })} className="w-full px-3 py-2 text-sm border border-slate-200 rounded-lg focus:border-emerald-500 outline-none" />
                </div>
              </div>
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-xs font-600 text-slate-500 mb-1">Categoría *</label>
                  <select value={form.categoriaId} onChange={(e) => setForm({ ...form, categoriaId: e.target.value })} className="w-full px-3 py-2 text-sm border border-slate-200 rounded-lg focus:border-emerald-500 outline-none">
                    <option value="">Seleccione</option>
                    {categorias.map((c) => <option key={c.id} value={c.id}>{c.nombre}</option>)}
                  </select>
                </div>
                <div>
                  <label className="block text-xs font-600 text-slate-500 mb-1">Laboratorio *</label>
                  <select value={form.laboratorioId} onChange={(e) => setForm({ ...form, laboratorioId: e.target.value })} className="w-full px-3 py-2 text-sm border border-slate-200 rounded-lg focus:border-emerald-500 outline-none">
                    <option value="">Seleccione</option>
                    {laboratorios.map((l) => <option key={l.id} value={l.id}>{l.nombre}</option>)}
                  </select>
                </div>
              </div>
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-xs font-600 text-slate-500 mb-1">Precio compra *</label>
                  <input type="number" step="0.01" value={form.precioCompra} onChange={(e) => setForm({ ...form, precioCompra: e.target.value })} placeholder="10.00" className="w-full px-3 py-2 text-sm border border-slate-200 rounded-lg focus:border-emerald-500 outline-none" />
                </div>
                <div>
                  <label className="block text-xs font-600 text-slate-500 mb-1">Precio venta *</label>
                  <input type="number" step="0.01" value={form.precioVenta} onChange={(e) => setForm({ ...form, precioVenta: e.target.value })} placeholder="15.00" className="w-full px-3 py-2 text-sm border border-slate-200 rounded-lg focus:border-emerald-500 outline-none" />
                </div>
              </div>
              <div>
                <label className="block text-xs font-600 text-slate-500 mb-1">Descripción</label>
                <textarea value={form.descripcion} onChange={(e) => setForm({ ...form, descripcion: e.target.value })} rows={2} placeholder="Descripción breve..." className="w-full px-3 py-2 text-sm border border-slate-200 rounded-lg focus:border-emerald-500 outline-none resize-none" />
              </div>
              <p className="text-xs text-slate-400">Los campos con * son obligatorios. El producto quedará activo por defecto.</p>
            </div>
            <div className="flex gap-3 px-6 py-4 border-t border-slate-100 bg-slate-50">
              <button onClick={() => setMostrarModal(false)} disabled={guardando}
                className="flex-1 py-2.5 rounded-xl border border-slate-200 text-sm font-500 text-slate-600 hover:bg-white transition-colors disabled:opacity-50">
                Cancelar
              </button>
              <button onClick={handleGuardar} disabled={guardando}
                className="flex-1 py-2.5 rounded-xl bg-emerald-600 text-white text-sm font-600 hover:bg-emerald-700 transition-colors disabled:opacity-50 flex items-center justify-center gap-2">
                {guardando ? 'Guardando…' : productoEditando?.id ? 'Actualizar' : 'Crear Producto'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

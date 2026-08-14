import { useEffect, useRef, useState } from 'react';
import type { KeyboardEvent } from 'react';
import { Badge } from '../../components/ui/Badge';
import { ventaService } from '../../services/venta.service';
import { formatearMoneda } from '../../utils/format';
import type { ItemCarrito, MensajeChat, ProductoResumen } from '../../types/domain.types';

const RESPUESTAS_ASISTENTE: Record<string, { texto: string; palabrasClave: string[] }> = {
  default: { texto: 'He analizado el catálogo disponible. Aquí tienes opciones relevantes:', palabrasClave: [] },
  fiebre: { texto: 'Para fiebre y dolor recomiendo evaluar estas opciones del catálogo:', palabrasClave: ['fiebre', 'tos', 'dolor'] },
  antibiotico: { texto: 'Para infecciones bacterianas, estos productos están registrados:', palabrasClave: ['antibio', 'infec'] },
  gastro: { texto: 'Para síntomas gastrointestinales:', palabrasClave: ['estomago', 'gastro', 'acidez'] },
  alergia: { texto: 'Para cuadros alérgicos:', palabrasClave: ['alergi', 'rinitis'] },
};

interface POSScreenProps {
  productos: ProductoResumen[];
  onVentaRegistrada: () => void;
}

export function POSScreen({ productos, onVentaRegistrada }: POSScreenProps) {
  const [query, setQuery] = useState('');
  const [carrito, setCarrito] = useState<ItemCarrito[]>([]);
  const [chatInput, setChatInput] = useState('');
  const [mensajes, setMensajes] = useState<MensajeChat[]>([
    { rol: 'asistente', texto: '¡Hola! Soy tu asistente IA. Cuéntame los síntomas del paciente o pregúntame por algún medicamento específico.' },
  ]);
  const [chatLoading, setChatLoading] = useState(false);
  const [procesandoVenta, setProcesandoVenta] = useState(false);
  const searchRef = useRef<HTMLInputElement>(null);
  const chatEndRef = useRef<HTMLDivElement>(null);

  useEffect(() => { searchRef.current?.focus(); }, []);
  useEffect(() => { chatEndRef.current?.scrollIntoView({ behavior: 'smooth' }); }, [mensajes]);

  const filtrados = query.length >= 2
    ? productos.filter((p) => p.estado && p.nombreComercial.toLowerCase().includes(query.toLowerCase()))
    : [];

  const agregarAlCarrito = (producto: ProductoResumen) => {
    setCarrito((prev) => {
      const existente = prev.find((i) => i.producto.id === producto.id);
      if (existente) return prev.map((i) => (i.producto.id === producto.id ? { ...i, cantidad: i.cantidad + 1 } : i));
      return [...prev, { producto, cantidad: 1 }];
    });
    setQuery('');
    searchRef.current?.focus();
  };

  const actualizarCantidad = (id: number, cantidad: number) => {
    if (cantidad < 1) setCarrito((prev) => prev.filter((i) => i.producto.id !== id));
    else setCarrito((prev) => prev.map((i) => (i.producto.id === id ? { ...i, cantidad } : i)));
  };

  const total = carrito.reduce((s, i) => s + i.producto.precioVenta * i.cantidad, 0);

  const handleSearchKey = (e: KeyboardEvent<HTMLInputElement>) => {
    if (e.key === 'Enter' && filtrados.length === 1) agregarAlCarrito(filtrados[0]);
  };

  const registrarVenta = async () => {
    if (carrito.length === 0) return;
    setProcesandoVenta(true);
    try {
      await ventaService.crear({
        detalles: carrito.map((item) => ({
          productoId: item.producto.id,
          cantidad: item.cantidad,
        })),
      });
      setCarrito([]);
      onVentaRegistrada();
    } catch {
      alert('No se pudo registrar la venta. Verifique la conexión con el backend.');
    } finally {
      setProcesandoVenta(false);
    }
  };

  const enviarChat = async () => {
    if (!chatInput.trim()) return;
    const mensajeUsuario = chatInput.trim();
    setChatInput('');
    setMensajes((prev) => [...prev, { rol: 'usuario', texto: mensajeUsuario }]);
    setChatLoading(true);
    await new Promise((r) => setTimeout(r, 800));

    const lower = mensajeUsuario.toLowerCase();
    let respuesta = RESPUESTAS_ASISTENTE.default;
    for (const config of Object.values(RESPUESTAS_ASISTENTE)) {
      if (config.palabrasClave.some((p) => lower.includes(p))) {
        respuesta = config;
        break;
      }
    }

    const sugerencias = productos.filter((p) => p.estado).slice(0, 3);
    setMensajes((prev) => [...prev, { rol: 'asistente', texto: respuesta.texto, sugerencias }]);
    setChatLoading(false);
  };

  return (
    <div className="flex-1 flex overflow-hidden">
      <div className="flex-1 flex flex-col p-4 gap-3 min-w-0">
        <div className="flex items-center gap-3">
          <h1 style={{ fontFamily: 'DM Sans, sans-serif' }} className="text-xl font-700 text-slate-900">Punto de Venta</h1>
          <Badge label="Caja #1" variant="green" />
        </div>

        <div className="relative">
          <div className="absolute left-4 top-1/2 -translate-y-1/2 text-slate-400">
            <svg width="18" height="18" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
              <path strokeLinecap="round" strokeLinejoin="round" d="M21 21l-4.35-4.35M17 11A6 6 0 1 1 5 11a6 6 0 0 1 12 0z" />
            </svg>
          </div>
          <input
            ref={searchRef}
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            onKeyDown={handleSearchKey}
            placeholder="Buscar medicamento por nombre… (Enter para agregar si hay 1 resultado)"
            className="w-full pl-11 pr-4 py-3.5 rounded-xl border-2 border-slate-200 focus:border-emerald-500 focus:ring-4 focus:ring-emerald-50 text-sm text-slate-800 outline-none transition-all bg-white shadow-sm"
          />
          {query && (
            <button onClick={() => { setQuery(''); searchRef.current?.focus(); }}
              className="absolute right-3 top-1/2 -translate-y-1/2 text-slate-300 hover:text-slate-500">
              ✕
            </button>
          )}
        </div>

        {filtrados.length > 0 && (
          <div className="bg-white rounded-xl border border-slate-200 shadow-lg overflow-hidden -mt-1">
            {filtrados.slice(0, 5).map((p) => (
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
                    {['Producto', 'Cant.', 'P. Unit.', 'Subtotal', ''].map((h) => (
                      <th key={h} className="text-left px-4 py-2.5 text-xs font-600 text-slate-400 uppercase tracking-wider">{h}</th>
                    ))}
                  </tr>
                </thead>
                <tbody>
                  {carrito.map((item) => (
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

          <div className="border-t border-slate-200 p-4">
            <div className="flex items-center justify-between mb-4">
              <span className="text-sm text-slate-500 font-medium">Total a Pagar</span>
              <span style={{ fontFamily: 'JetBrains Mono, monospace' }} className="text-2xl font-700 text-slate-900">{formatearMoneda(total)}</span>
            </div>
            <div className="grid grid-cols-2 gap-3">
              <button onClick={() => setCarrito([])}
                className="py-2.5 rounded-xl border-2 border-red-200 text-red-600 text-sm font-600 hover:bg-red-50 transition-colors">
                Vaciar Carrito
              </button>
              <button disabled={carrito.length === 0 || procesandoVenta} onClick={registrarVenta}
                className="py-2.5 rounded-xl bg-emerald-600 text-white text-sm font-600 hover:bg-emerald-700 disabled:opacity-40 disabled:cursor-not-allowed transition-colors shadow-sm shadow-emerald-200">
                {procesandoVenta ? 'Procesando…' : `Cobrar ${formatearMoneda(total)}`}
              </button>
            </div>
          </div>
        </div>
      </div>

      <div style={{ width: 360, minWidth: 360 }} className="flex flex-col border-l border-slate-200 bg-white">
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
              {msg.sugerencias && msg.sugerencias.map((p) => (
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
                  {[0, 1, 2].map((d) => (
                    <div key={d} className="w-1.5 h-1.5 bg-slate-400 rounded-full animate-bounce" style={{ animationDelay: `${d * 0.15}s` }} />
                  ))}
                </div>
              </div>
            </div>
          )}
          <div ref={chatEndRef} />
        </div>

        <div className="p-3 border-t border-slate-100">
          <div className="flex gap-2">
            <input
              value={chatInput}
              onChange={(e) => setChatInput(e.target.value)}
              onKeyDown={(e) => e.key === 'Enter' && enviarChat()}
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
          <p className="text-xs text-slate-400 text-center mt-2">Presiona Enter para enviar</p>
        </div>
      </div>
    </div>
  );
}
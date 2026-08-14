import { useMemo } from 'react';
import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer } from 'recharts';
import { Badge } from '../../components/ui/Badge';
import { formatearFecha, formatearMoneda } from '../../utils/format';
import type { ProductoResumen, Venta } from '../../types/domain.types';

interface DashboardScreenProps {
  ventas: Venta[];
  productos: ProductoResumen[];
  cargando: boolean;
}

const colorMap: Record<string, string> = {
  emerald: 'border-l-4 border-emerald-500',
  amber: 'border-l-4 border-amber-500',
  blue: 'border-l-4 border-blue-500',
};

export function DashboardScreen({ ventas, productos, cargando }: DashboardScreenProps) {
  const ventasCompletadas = ventas.filter((v) => v.estado === 'COMPLETADA');
  const totalDia = ventasCompletadas.reduce((s, v) => s + v.total, 0);
  const productosInactivos = productos.filter((p) => !p.estado).length;

  const datosGrafico = useMemo(() => {
    const dias = Array.from({ length: 7 }, (_, i) => {
      const fecha = new Date();
      fecha.setDate(fecha.getDate() - (6 - i));
      return {
        clave: fecha.toISOString().slice(0, 10),
        etiqueta: fecha.toLocaleDateString('es-PE', { weekday: 'short' }),
        ventas: 0,
      };
    });
    const porDia = new Map(dias.map((d) => [d.clave, d]));
    ventasCompletadas.forEach((v) => {
      const clave = new Date(v.fechaVenta).toISOString().slice(0, 10);
      const dia = porDia.get(clave);
      if (dia) dia.ventas += v.total;
    });
    return dias.map((d) => ({ day: d.etiqueta, ventas: d.ventas }));
  }, [ventasCompletadas]);

  const kpis = [
    { label: 'Ventas Recientes', value: formatearMoneda(totalDia), sub: `${ventasCompletadas.length} transacciones`, icon: '💰', color: 'emerald' },
    { label: 'Productos Inactivos', value: `${productosInactivos}`, sub: 'Revisar catálogo', icon: '⚠️', color: 'amber' },
    { label: 'Catálogo Activo', value: `${productos.filter((p) => p.estado).length}`, sub: 'Productos habilitados', icon: '📦', color: 'blue' },
    { label: 'Total Ventas', value: `${ventas.length}`, sub: 'Registradas en el sistema', icon: '📈', color: 'blue' },
  ];

  return (
    <div className="flex-1 overflow-y-auto p-6">
      {cargando && <p className="text-sm text-slate-400 mb-4">Cargando datos del panel…</p>}
      <div className="mb-6">
        <h1 style={{ fontFamily: 'DM Sans, sans-serif' }} className="text-2xl font-700 text-slate-900">Dashboard</h1>
        <p className="text-sm text-slate-400 mt-0.5">
          {new Date().toLocaleDateString('es-PE', { weekday: 'long', day: 'numeric', month: 'long', year: 'numeric' })}
        </p>
      </div>

      <div className="grid grid-cols-4 gap-4 mb-6">
        {kpis.map((k) => (
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
        <div className="col-span-3 bg-white rounded-xl border border-slate-100 shadow-sm p-5">
          <div className="flex items-center justify-between mb-4">
            <div>
              <h2 className="text-sm font-600 text-slate-900">Ventas por Día</h2>
              <p className="text-xs text-slate-400">Últimos 7 días · En soles (S/)</p>
            </div>
            <Badge label="Últimos 7 días" variant="green" />
          </div>
          <ResponsiveContainer width="100%" height={200}>
            <BarChart data={datosGrafico} barSize={28}>
              <CartesianGrid strokeDasharray="3 3" stroke="#F1F5F9" vertical={false} />
              <XAxis dataKey="day" tick={{ fontSize: 12, fill: '#94A3B8' }} axisLine={false} tickLine={false} />
              <YAxis tick={{ fontSize: 11, fill: '#94A3B8', fontFamily: 'JetBrains Mono' }} axisLine={false} tickLine={false} tickFormatter={(v) => `${v / 1000}k`} />
              <Tooltip
                formatter={(value) => [`S/ ${Number(value).toLocaleString()}`, 'Ventas']}
                contentStyle={{ border: '1px solid #E2E8F0', borderRadius: 8, fontSize: 12 }}
              />
              <Bar dataKey="ventas" fill="#059669" radius={[4, 4, 0, 0]} />
            </BarChart>
          </ResponsiveContainer>
        </div>

        <div className="col-span-2 bg-white rounded-xl border border-slate-100 shadow-sm p-5">
          <h2 className="text-sm font-600 text-slate-900 mb-4">Productos Inactivos</h2>
          <div className="space-y-3">
            {productos.filter((p) => !p.estado).slice(0, 5).map((p) => (
              <div key={p.id} className="flex items-center gap-3 py-2 border-b border-slate-50 last:border-0">
                <div className="w-2 h-2 rounded-full flex-shrink-0 bg-red-500" />
                <div className="flex-1 min-w-0">
                  <div className="text-xs font-500 text-slate-800 truncate">{p.nombreComercial}</div>
                  <div className="text-xs text-slate-400">{p.laboratorioNombre}</div>
                </div>
                <Badge label="Inactivo" variant="red" />
              </div>
            ))}
            {productos.filter((p) => !p.estado).length === 0 && (
              <p className="text-xs text-slate-400">Todos los productos están activos.</p>
            )}
          </div>
        </div>
      </div>

      <div className="mt-4 bg-white rounded-xl border border-slate-100 shadow-sm">
        <div className="flex items-center justify-between px-6 py-4 border-b border-slate-100">
          <h2 className="text-sm font-600 text-slate-900">Últimas Ventas Registradas</h2>
          <span className="text-xs text-emerald-600 font-500">Historial reciente</span>
        </div>
        <table className="w-full text-sm">
          <thead>
            <tr className="border-b border-slate-100">
              {['ID Venta', 'Fecha y Hora', 'Total', 'Estado'].map((h) => (
                <th key={h} className="text-left px-6 py-3 text-xs font-600 text-slate-400 uppercase tracking-wider">{h}</th>
              ))}
            </tr>
          </thead>
          <tbody>
            {ventas.slice(0, 6).map((s) => (
              <tr key={s.id} className="border-b border-slate-50 hover:bg-slate-50/60 transition-colors">
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
              </tr>
            ))}
          </tbody>
        </table>
        <div className="px-6 py-3 border-t border-slate-100 flex items-center justify-between">
          <span className="text-xs text-slate-400">Mostrando {Math.min(ventas.length, 6)} de {ventas.length} ventas</span>
        </div>
      </div>
    </div>
  );
}
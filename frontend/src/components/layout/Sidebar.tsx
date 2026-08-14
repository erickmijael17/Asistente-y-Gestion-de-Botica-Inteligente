import { NavLink } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import { esGerente } from '../../utils/roles';
import { CrossIcon, ChartIcon, CashIcon, BoxIcon, LogoutIcon } from '../icons';

export function Sidebar() {
  const { usuario, cerrarSesion } = useAuth();
  const gerente = esGerente(usuario?.roles ?? '');

  const items = [
    { to: '/dashboard', label: 'Dashboard', icon: ChartIcon, visible: gerente },
    { to: '/pos', label: 'Punto de Venta', icon: CashIcon, visible: true },
    { to: '/catalogo', label: 'Catálogo', icon: BoxIcon, visible: true },
  ];

  const navClass = ({ isActive }: { isActive: boolean }) =>
    `w-full flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm font-medium transition-colors ${
      isActive ? 'bg-emerald-50 text-emerald-700' : 'text-slate-600 hover:bg-slate-50 hover:text-slate-900'
    }`;

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
        {items.filter((i) => i.visible).map((item) => {
          const Icon = item.icon;
          return (
            <NavLink key={item.to} to={item.to} className={navClass}>
              {({ isActive }) => (
                <>
                  <Icon active={isActive} />
                  {item.label}
                </>
              )}
            </NavLink>
          );
        })}
      </nav>

      <div className="px-3 py-4 border-t border-slate-100">
        <div className="px-3 py-2 mb-1">
          <div className="text-xs font-medium text-slate-900">{usuario?.username}</div>
          <div className="text-xs text-slate-400">{gerente ? 'Gerente' : 'Vendedor'}</div>
        </div>
        <button
          onClick={cerrarSesion}
          className="w-full flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm font-medium text-slate-500 hover:bg-red-50 hover:text-red-600 transition-colors"
        >
          <LogoutIcon />
          Cerrar Sesión
        </button>
      </div>
    </aside>
  );
}
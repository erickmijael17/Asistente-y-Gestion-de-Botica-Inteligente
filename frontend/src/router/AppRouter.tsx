import { Suspense, lazy } from 'react';
import { Navigate, Route, Routes, useLocation } from 'react-router-dom';
import { Sidebar } from '../components/layout/Sidebar';
import { useAuth } from '../context/AuthContext';
import { useCatalogo } from '../hooks/useCatalogo';

const LoginScreen = lazy(() => import('../features/auth/LoginScreen').then((m) => ({ default: m.LoginScreen })));
const DashboardScreen = lazy(() => import('../features/dashboard/DashboardScreen').then((m) => ({ default: m.DashboardScreen })));
const POSScreen = lazy(() => import('../features/pos/POSScreen').then((m) => ({ default: m.POSScreen })));
const CatalogoScreen = lazy(() => import('../features/catalogo/CatalogoScreen').then((m) => ({ default: m.CatalogoScreen })));

function Fallback() {
  return <div className="min-h-screen flex items-center justify-center text-sm text-slate-400">Cargando…</div>;
}

function ProtectedRoute({ children }: { children: React.ReactNode }) {
  const { autenticado } = useAuth();
  const location = useLocation();
  if (!autenticado) {
    return <Navigate to="/login" replace state={{ from: location.pathname }} />;
  }
  return <>{children}</>;
}

function GerenteRoute({ children }: { children: React.ReactNode }) {
  const { esGerente } = useAuth();
  if (!esGerente) {
    return <Navigate to="/pos" replace />;
  }
  return <>{children}</>;
}

function DashboardLayout() {
  const { errorDatos, productos, categorias, laboratorios, ventas, cargandoDatos, recargarVentas } = useCatalogo();
  const { esGerente } = useAuth();

  return (
    <div className="flex h-screen overflow-hidden bg-slate-100">
      <Sidebar />
      <main className="flex-1 flex flex-col overflow-hidden">
        {errorDatos && (
          <div className="px-6 py-2.5 bg-amber-50 border-b border-amber-200 text-amber-700 text-xs font-medium">
            {errorDatos}
          </div>
        )}
        <Suspense fallback={<Fallback />}>
          <Routes>
            <Route
              path="/dashboard"
              element={
                <GerenteRoute>
                  <DashboardScreen ventas={ventas} productos={productos} cargando={cargandoDatos} />
                </GerenteRoute>
              }
            />
            <Route
              path="/pos"
              element={<POSScreen productos={productos} onVentaRegistrada={recargarVentas} />}
            />
            <Route
              path="/catalogo"
              element={
                <CatalogoScreen
                  productos={productos}
                  categorias={categorias}
                  laboratorios={laboratorios}
                  cargando={cargandoDatos}
                  esGerente={esGerente}
                />
              }
            />
            <Route path="*" element={<Navigate to="/pos" replace />} />
          </Routes>
        </Suspense>
      </main>
    </div>
  );
}

export function AppRouter() {
  return (
    <Suspense fallback={<Fallback />}>
      <Routes>
        <Route path="/login" element={<LoginScreen />} />
        <Route
          path="/*"
          element={
            <ProtectedRoute>
              <DashboardLayout />
            </ProtectedRoute>
          }
        />
      </Routes>
    </Suspense>
  );
}
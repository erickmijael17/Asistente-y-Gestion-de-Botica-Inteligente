import { useAuth } from '../context/AuthContext';
import { useNavigate } from 'react-router-dom';

export const Home = () => {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  return (
    <div className="min-h-screen bg-gray-50">
      <nav className="bg-white shadow-sm border-b px-6 py-4 flex justify-between items-center">
        <h1 className="text-xl font-bold text-gray-800">Botica Inteligente</h1>
        <div className="flex items-center space-x-4">
          <span className="text-gray-600">
            Hola, <span className="font-semibold text-gray-900">{user?.username}</span> ({user?.roles})
          </span>
          <button
            onClick={handleLogout}
            className="text-sm bg-red-50 text-red-600 px-4 py-2 rounded hover:bg-red-100 transition-colors"
          >
            Cerrar Sesión
          </button>
        </div>
      </nav>
      
      <main className="p-8">
        <div className="bg-white rounded-lg shadow-sm p-6 border">
          <h2 className="text-lg font-medium mb-4">Bienvenido al Sistema</h2>
          <p className="text-gray-600">
            Esta es la página de inicio. Desde aquí podrás acceder a los módulos de ventas, inventario y más.
          </p>
        </div>
      </main>
    </div>
  );
};

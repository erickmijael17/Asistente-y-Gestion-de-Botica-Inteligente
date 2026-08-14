import { useCallback, useEffect, useState } from 'react';
import { productoService } from '../services/producto.service';
import { categoriaService } from '../services/categoria.service';
import { laboratorioService } from '../services/laboratorio.service';
import { ventaService } from '../services/venta.service';
import type { Categoria, Laboratorio, ProductoResumen, Venta } from '../types/domain.types';
import { useAuth } from '../context/AuthContext';

export function useCatalogo() {
  const { autenticado } = useAuth();
  const [productos, setProductos] = useState<ProductoResumen[]>([]);
  const [categorias, setCategorias] = useState<Categoria[]>([]);
  const [laboratorios, setLaboratorios] = useState<Laboratorio[]>([]);
  const [ventas, setVentas] = useState<Venta[]>([]);
  const [cargandoDatos, setCargandoDatos] = useState(false);
  const [errorDatos, setErrorDatos] = useState<string | null>(null);

  const cargarDatos = useCallback(async () => {
    setCargandoDatos(true);
    setErrorDatos(null);
    try {
      const [productosR, categoriasR, laboratoriosR, ventasR] = await Promise.allSettled([
        productoService.listar({ size: 100, page: 0 }),
        categoriaService.listar({ size: 100, page: 0 }),
        laboratorioService.listar({ size: 100, page: 0 }),
        ventaService.listar({ size: 20, page: 0, sort: 'fechaVenta,desc' }),
      ]);

      const fallos: string[] = [];
      if (productosR.status === 'fulfilled') setProductos(productosR.value.content);
      else fallos.push('productos');
      if (categoriasR.status === 'fulfilled') setCategorias(categoriasR.value.content);
      else fallos.push('categorías');
      if (laboratoriosR.status === 'fulfilled') setLaboratorios(laboratoriosR.value.content);
      else fallos.push('laboratorios');
      if (ventasR.status === 'fulfilled') setVentas(ventasR.value.content);
      else fallos.push('ventas');

      if (fallos.length > 0) {
        setErrorDatos(`No se pudo cargar: ${fallos.join(', ')}. Revise la consola para más detalles.`);
      }
    } finally {
      setCargandoDatos(false);
    }
  }, []);

  const recargarVentas = useCallback(async () => {
    try {
      const respuesta = await ventaService.listar({ size: 20, page: 0, sort: 'fechaVenta,desc' });
      setVentas(respuesta.content);
    } catch {
      setErrorDatos('No se pudo actualizar el historial de ventas.');
    }
  }, []);

  useEffect(() => {
    if (autenticado) {
      void cargarDatos();
    }
  }, [autenticado, cargarDatos]);

  return { productos, categorias, laboratorios, ventas, cargandoDatos, errorDatos, cargarDatos, recargarVentas };
}
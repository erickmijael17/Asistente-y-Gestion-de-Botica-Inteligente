import api from '../api/client';
import type { ApiResponse, PageResponse, PaginationParams } from '../types/api.types';
import type { ProductoResumen } from '../types/domain.types';

export interface ProductoFiltros extends PaginationParams {
  nombreComercial?: string;
  categoriaId?: number;
  laboratorioId?: number;
  estado?: boolean;
}

export const productoService = {
  listar: async (filtros: ProductoFiltros = {}): Promise<PageResponse<ProductoResumen>> => {
    const response = await api.get<ApiResponse<PageResponse<ProductoResumen>>>('/v1/productos', {
      params: filtros,
    });
    return response.data.data;
  },

  buscarActivos: async (termino: string, size = 50): Promise<ProductoResumen[]> => {
    const pagina = await productoService.listar({
      nombreComercial: termino,
      estado: true,
      size,
      page: 0,
    });
    return pagina.content;
  },
};

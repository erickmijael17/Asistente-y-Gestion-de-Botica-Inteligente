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
  crear: async (data: Record<string, unknown>): Promise<ProductoResumen> => {
    const response = await api.post<ApiResponse<ProductoResumen>>('/v1/productos', data);
    return response.data.data;
  },
  actualizar: async (id: number, data: Record<string, unknown>): Promise<ProductoResumen> => {
    const response = await api.put<ApiResponse<ProductoResumen>>(`/v1/productos/${id}`, data);
    return response.data.data;
  },
};
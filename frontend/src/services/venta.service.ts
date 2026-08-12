import api from '../api/client';
import type { ApiResponse, PageResponse, PaginationParams } from '../types/api.types';
import type { Venta } from '../types/domain.types';

export interface CrearVentaRequest {
  usuarioId: number;
  detalles: Array<{
    productoId: number;
    cantidad: number;
    precioUnitario: number;
  }>;
}

export const ventaService = {
  listar: async (params: PaginationParams = {}): Promise<PageResponse<Venta>> => {
    const response = await api.get<ApiResponse<PageResponse<Venta>>>('/v1/ventas', { params });
    return response.data.data;
  },

  crear: async (request: CrearVentaRequest): Promise<Venta> => {
    const response = await api.post<ApiResponse<Venta>>('/v1/ventas', request);
    return response.data.data;
  },

  anular: async (id: number): Promise<Venta> => {
    const response = await api.patch<ApiResponse<Venta>>(`/v1/ventas/${id}/anular`);
    return response.data.data;
  },
};

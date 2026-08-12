import api from '../api/client';
import type { ApiResponse, PageResponse, PaginationParams } from '../types/api.types';
import type { Categoria } from '../types/domain.types';

export const categoriaService = {
  listar: async (params: PaginationParams = {}): Promise<PageResponse<Categoria>> => {
    const response = await api.get<ApiResponse<PageResponse<Categoria>>>('/v1/categorias', { params });
    return response.data.data;
  },
};

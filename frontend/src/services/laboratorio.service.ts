import api from '../api/client';
import type { ApiResponse, PageResponse, PaginationParams } from '../types/api.types';
import type { Laboratorio } from '../types/domain.types';

export const laboratorioService = {
  listar: async (params: PaginationParams = {}): Promise<PageResponse<Laboratorio>> => {
    const response = await api.get<ApiResponse<PageResponse<Laboratorio>>>('/v1/laboratorios', { params });
    return response.data.data;
  },
};

export type RolUsuario = 'ROLE_OWNER' | 'ROLE_SELLER';

export interface UsuarioSesion {
  id: number;
  username: string;
  roles: string;
  nombres?: string;
}

export interface Categoria {
  id: number;
  nombre: string;
  descripcion: string;
  estado: boolean;
}

export interface Laboratorio {
  id: number;
  nombre: string;
  ruc: string;
  telefono: string;
  correo: string;
  direccion: string;
  sitioWeb: string;
  estado: boolean;
}

export interface ProductoResumen {
  id: number;
  codigoInterno: string;
  codigoBarras: string | null;
  nombreComercial: string;
  tipoProducto: string;
  precioVenta: number;
  estado: boolean;
  categoriaId: number;
  categoriaNombre: string;
  laboratorioId: number;
  laboratorioNombre: string;
}

export type EstadoVenta = 'COMPLETADA' | 'ANULADA';

export interface VentaDetalle {
  productoId: number;
  productoNombre?: string;
  cantidad: number;
  precioUnitario: number;
  subtotal?: number;
}

export interface Venta {
  id: number;
  usuarioId: number;
  usuarioUsername: string;
  fechaVenta: string;
  subtotal: number;
  impuestos: number;
  total: number;
  estado: EstadoVenta;
  detalles: VentaDetalle[];
}

export interface ItemCarrito {
  producto: ProductoResumen;
  cantidad: number;
}

export interface MensajeChat {
  rol: 'usuario' | 'asistente';
  texto: string;
  sugerencias?: ProductoResumen[];
}

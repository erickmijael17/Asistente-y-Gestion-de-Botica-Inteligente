export const formatearMoneda = (valor: number) => `S/ ${valor.toFixed(2)}`;

export const formatearFecha = (fecha: string) =>
  new Date(fecha).toLocaleString('es-PE', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  });

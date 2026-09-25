/** Texto de cada segmento de la URL en las migas de pan. Un segmento sin entrada se muestra capitalizado. */
export const SEGMENT_LABELS: Record<string, string> = {
  dashboard: 'Dashboard',
  profile: 'Mi perfil',
  users: 'Usuarios',
  clients: 'Clientes',
  suppliers: 'Proveedores',
  'product-categories': 'Categorías de productos',
  products: 'Productos',
  'service-categories': 'Categorías de servicios',
  services: 'Servicios',
  'quotation-settings': 'Parámetros de cotización',
  quotations: 'Cotizaciones',
  maintenance: 'Mantenimientos',
  'audit-logs': 'Auditoría',
  new: 'Nuevo',
  edit: 'Editar',
  'pdf-preview': 'Vista previa PDF',
};

/**
 * Cuando el segundo segmento es un id, la miga muestra el nombre del registro en vez del UUID.
 *
 * `fallback` es lo que se ve mientras llega el nombre (o si falla la peticion) y `label` extrae el
 * nombre del registro. Un recurso nuevo con detalle se suma aqui con una entrada.
 */
export interface EntityCrumb {
  fallback: string;
  label: (entity: Record<string, unknown>) => string | undefined;
}

const text = (value: unknown): string | undefined =>
  typeof value === 'string' && value.trim() ? value : undefined;

export const ENTITY_CRUMBS: Record<string, EntityCrumb> = {
  users: { fallback: 'Usuario', label: (e) => text(e['fullName']) },
  clients: { fallback: 'Cliente', label: (e) => text(e['nameOrBusinessName']) },
  suppliers: { fallback: 'Proveedor', label: (e) => text(e['businessName']) },
  'product-categories': { fallback: 'Categoría', label: (e) => text(e['name']) },
  products: { fallback: 'Producto', label: (e) => text(e['name']) },
  'service-categories': { fallback: 'Categoría', label: (e) => text(e['name']) },
  services: { fallback: 'Servicio', label: (e) => text(e['name']) },
  maintenance: {
    fallback: 'Mantenimiento',
    label: (e) => text(e['intervenedSystem']) ?? text((e['client'] as Record<string, unknown> | undefined)?.['nameOrBusinessName']),
  },
  quotations: { fallback: 'Cotización', label: (e) => text(e['quotationNumber']) },
};

export const looksLikeUuid = (value: string): boolean =>
  /^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i.test(value);

export const segmentLabel = (segment: string): string =>
  SEGMENT_LABELS[segment] ?? segment.replaceAll('-', ' ').replace(/^./, (letter) => letter.toUpperCase());

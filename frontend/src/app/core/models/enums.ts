/** ADMINISTRADOR es el super administrador; ADMIN_OPERATIVO, el administrador sin usuarios, parametros ni auditoria. */
export type Role = 'ADMINISTRADOR' | 'ADMIN_OPERATIVO' | 'TECNICO';

export type QuotationStatus = 'BORRADOR' | 'ENVIADA' | 'APROBADA' | 'RECHAZADA';

export type QuotationItemType = 'PRODUCTO' | 'SERVICIO';

export type MaintenanceType = 'PREVENTIVO' | 'CORRECTIVO';

export type MaintenanceStatus = 'PENDIENTE' | 'EN_PROCESO' | 'COMPLETADO' | 'CANCELADO';

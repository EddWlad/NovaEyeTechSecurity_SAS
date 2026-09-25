import { Route, Routes } from '@angular/router';

import { authGuard } from './core/guards/auth.guard';
import { roleGuard } from './core/guards/role.guard';
import { Role } from './core/models/enums';
import { AppShellComponent } from './core/layout/app-shell.component';
import { LoginPageComponent } from './features/auth/pages/login/login-page.component';

/*
 * Solo el login y el shell se cargan de entrada. Cada pantalla se descarga la primera vez que se
 * visita (y el router precarga el resto en segundo plano, ver app.config.ts): el bundle inicial
 * pasa de una sola pieza a lo minimo para pintar el login.
 */
const ADMIN: Role[] = ['ADMINISTRADOR'];
const BOTH: Role[] = ['ADMINISTRADOR', 'TECNICO'];

const loadDashboard = () =>
  import('./features/dashboard/pages/dashboard/dashboard-page.component').then((m) => m.DashboardPageComponent);
const loadProfile = () =>
  import('./features/profile/pages/profile/profile-page.component').then((m) => m.ProfilePageComponent);
const loadResourceList = () =>
  import('./features/crud/pages/resource-list/resource-list-page.component').then((m) => m.ResourceListPageComponent);
const loadResourceForm = () =>
  import('./features/crud/pages/resource-form/resource-form-page.component').then((m) => m.ResourceFormPageComponent);
const loadResourceDetail = () =>
  import('./features/crud/pages/resource-detail/resource-detail-page.component').then((m) => m.ResourceDetailPageComponent);
const loadQuotationSettings = () =>
  import('./features/quotation-settings/pages/quotation-settings/quotation-settings-page.component').then(
    (m) => m.QuotationSettingsPageComponent,
  );
const loadQuotationsList = () =>
  import('./features/quotations/pages/quotations-list/quotations-list-page.component').then((m) => m.QuotationsListPageComponent);
const loadQuotationForm = () =>
  import('./features/quotations/pages/quotation-form/quotation-form-page.component').then((m) => m.QuotationFormPageComponent);
const loadQuotationDetail = () =>
  import('./features/quotations/pages/quotation-detail/quotation-detail-page.component').then((m) => m.QuotationDetailPageComponent);
const loadQuotationPdfPreview = () =>
  import('./features/quotations/pages/quotation-pdf-preview/quotation-pdf-preview-page.component').then(
    (m) => m.QuotationPdfPreviewPageComponent,
  );
const loadMaintenanceList = () =>
  import('./features/maintenance/pages/maintenance-list/maintenance-list-page.component').then((m) => m.MaintenanceListPageComponent);
const loadMaintenanceForm = () =>
  import('./features/maintenance/pages/maintenance-form/maintenance-form-page.component').then((m) => m.MaintenanceFormPageComponent);
const loadMaintenanceDetail = () =>
  import('./features/maintenance/pages/maintenance-detail/maintenance-detail-page.component').then((m) => m.MaintenanceDetailPageComponent);
const loadAuditLogs = () =>
  import('./features/audit-logs/pages/audit-log-list/audit-log-list-page.component').then((m) => m.AuditLogListPageComponent);

/**
 * Las cuatro rutas de un recurso CRUD: listado, alta, detalle y edicion. Un recurso nuevo se suma con
 * una sola linea (y su definicion en resource-definitions.ts).
 *
 * @param read  roles que pueden ver el listado y el detalle
 * @param write roles que pueden crear y editar (por defecto los mismos que leen)
 *
 * El orden importa: `/new` debe ir antes de `/:id` para que "new" no se lea como un identificador.
 */
const crudRoutes = (resourceKey: string, read: Role[], write: Role[] = read): Route[] => [
  { path: resourceKey, loadComponent: loadResourceList, canActivate: [roleGuard], data: { roles: read, resourceKey } },
  { path: `${resourceKey}/new`, loadComponent: loadResourceForm, canActivate: [roleGuard], data: { roles: write, resourceKey } },
  { path: `${resourceKey}/:id`, loadComponent: loadResourceDetail, canActivate: [roleGuard], data: { roles: read, resourceKey } },
  { path: `${resourceKey}/:id/edit`, loadComponent: loadResourceForm, canActivate: [roleGuard], data: { roles: write, resourceKey } },
];

export const routes: Routes = [
  {
    path: 'login',
    component: LoginPageComponent,
  },
  {
    path: '',
    component: AppShellComponent,
    canActivate: [authGuard],
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'dashboard' },
      { path: 'dashboard', loadComponent: loadDashboard, canActivate: [roleGuard], data: { roles: BOTH } },
      { path: 'profile', loadComponent: loadProfile, canActivate: [roleGuard], data: { roles: BOTH } },

      ...crudRoutes('users', ADMIN),
      ...crudRoutes('clients', BOTH),
      ...crudRoutes('suppliers', ADMIN),
      ...crudRoutes('product-categories', ADMIN),
      ...crudRoutes('products', BOTH, ADMIN),
      ...crudRoutes('service-categories', ADMIN),
      ...crudRoutes('services', BOTH, ADMIN),

      { path: 'quotation-settings', loadComponent: loadQuotationSettings, canActivate: [roleGuard], data: { roles: ADMIN } },

      { path: 'quotations', loadComponent: loadQuotationsList, canActivate: [roleGuard], data: { roles: BOTH } },
      { path: 'quotations/new', loadComponent: loadQuotationForm, canActivate: [roleGuard], data: { roles: BOTH } },
      { path: 'quotations/:id', loadComponent: loadQuotationDetail, canActivate: [roleGuard], data: { roles: BOTH } },
      { path: 'quotations/:id/edit', loadComponent: loadQuotationForm, canActivate: [roleGuard], data: { roles: BOTH } },
      { path: 'quotations/:id/pdf-preview', loadComponent: loadQuotationPdfPreview, canActivate: [roleGuard], data: { roles: BOTH } },

      { path: 'maintenance', loadComponent: loadMaintenanceList, canActivate: [roleGuard], data: { roles: BOTH } },
      { path: 'maintenance/new', loadComponent: loadMaintenanceForm, canActivate: [roleGuard], data: { roles: BOTH } },
      { path: 'maintenance/:id', loadComponent: loadMaintenanceDetail, canActivate: [roleGuard], data: { roles: BOTH } },
      { path: 'maintenance/:id/edit', loadComponent: loadMaintenanceForm, canActivate: [roleGuard], data: { roles: BOTH } },

      { path: 'audit-logs', loadComponent: loadAuditLogs, canActivate: [roleGuard], data: { roles: ADMIN } },
    ],
  },
  { path: '**', redirectTo: '' },
];

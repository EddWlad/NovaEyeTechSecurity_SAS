import { Routes } from '@angular/router';

import { authGuard } from './core/guards/auth.guard';
import { roleGuard } from './core/guards/role.guard';
import { AppShellComponent } from './core/layout/app-shell.component';
import { LoginPageComponent } from './features/auth/pages/login/login-page.component';
import { DashboardPageComponent } from './features/dashboard/pages/dashboard/dashboard-page.component';
import { ProfilePageComponent } from './features/profile/pages/profile/profile-page.component';
import { ResourceListPageComponent } from './features/crud/pages/resource-list/resource-list-page.component';
import { ResourceFormPageComponent } from './features/crud/pages/resource-form/resource-form-page.component';
import { ResourceDetailPageComponent } from './features/crud/pages/resource-detail/resource-detail-page.component';
import { QuotationSettingsPageComponent } from './features/quotation-settings/pages/quotation-settings/quotation-settings-page.component';
import { QuotationsListPageComponent } from './features/quotations/pages/quotations-list/quotations-list-page.component';
import { QuotationFormPageComponent } from './features/quotations/pages/quotation-form/quotation-form-page.component';
import { QuotationDetailPageComponent } from './features/quotations/pages/quotation-detail/quotation-detail-page.component';
import { QuotationPdfPreviewPageComponent } from './features/quotations/pages/quotation-pdf-preview/quotation-pdf-preview-page.component';
import { MaintenanceListPageComponent } from './features/maintenance/pages/maintenance-list/maintenance-list-page.component';
import { MaintenanceFormPageComponent } from './features/maintenance/pages/maintenance-form/maintenance-form-page.component';
import { MaintenanceDetailPageComponent } from './features/maintenance/pages/maintenance-detail/maintenance-detail-page.component';
import { AuditLogListPageComponent } from './features/audit-logs/pages/audit-log-list/audit-log-list-page.component';

const adminOnly = { roles: ['ADMINISTRADOR'] };
const bothRoles = { roles: ['ADMINISTRADOR', 'TECNICO'] };

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
      {
        path: 'dashboard',
        component: DashboardPageComponent,
        canActivate: [roleGuard],
        data: bothRoles,
      },
      {
        path: 'profile',
        component: ProfilePageComponent,
        canActivate: [roleGuard],
        data: bothRoles,
      },

      { path: 'users', component: ResourceListPageComponent, canActivate: [roleGuard], data: { ...adminOnly, resourceKey: 'users' } },
      { path: 'users/new', component: ResourceFormPageComponent, canActivate: [roleGuard], data: { ...adminOnly, resourceKey: 'users' } },
      { path: 'users/:id', component: ResourceDetailPageComponent, canActivate: [roleGuard], data: { ...adminOnly, resourceKey: 'users' } },
      { path: 'users/:id/edit', component: ResourceFormPageComponent, canActivate: [roleGuard], data: { ...adminOnly, resourceKey: 'users' } },

      { path: 'clients', component: ResourceListPageComponent, canActivate: [roleGuard], data: { ...bothRoles, resourceKey: 'clients' } },
      { path: 'clients/new', component: ResourceFormPageComponent, canActivate: [roleGuard], data: { ...bothRoles, resourceKey: 'clients' } },
      { path: 'clients/:id', component: ResourceDetailPageComponent, canActivate: [roleGuard], data: { ...bothRoles, resourceKey: 'clients' } },
      { path: 'clients/:id/edit', component: ResourceFormPageComponent, canActivate: [roleGuard], data: { ...bothRoles, resourceKey: 'clients' } },

      { path: 'suppliers', component: ResourceListPageComponent, canActivate: [roleGuard], data: { ...adminOnly, resourceKey: 'suppliers' } },
      { path: 'suppliers/new', component: ResourceFormPageComponent, canActivate: [roleGuard], data: { ...adminOnly, resourceKey: 'suppliers' } },
      { path: 'suppliers/:id', component: ResourceDetailPageComponent, canActivate: [roleGuard], data: { ...adminOnly, resourceKey: 'suppliers' } },
      { path: 'suppliers/:id/edit', component: ResourceFormPageComponent, canActivate: [roleGuard], data: { ...adminOnly, resourceKey: 'suppliers' } },

      {
        path: 'product-categories',
        component: ResourceListPageComponent,
        canActivate: [roleGuard],
        data: { ...adminOnly, resourceKey: 'product-categories' },
      },
      {
        path: 'product-categories/new',
        component: ResourceFormPageComponent,
        canActivate: [roleGuard],
        data: { ...adminOnly, resourceKey: 'product-categories' },
      },
      {
        path: 'product-categories/:id',
        component: ResourceDetailPageComponent,
        canActivate: [roleGuard],
        data: { ...adminOnly, resourceKey: 'product-categories' },
      },
      {
        path: 'product-categories/:id/edit',
        component: ResourceFormPageComponent,
        canActivate: [roleGuard],
        data: { ...adminOnly, resourceKey: 'product-categories' },
      },

      { path: 'products', component: ResourceListPageComponent, canActivate: [roleGuard], data: { ...bothRoles, resourceKey: 'products' } },
      { path: 'products/new', component: ResourceFormPageComponent, canActivate: [roleGuard], data: { ...adminOnly, resourceKey: 'products' } },
      { path: 'products/:id', component: ResourceDetailPageComponent, canActivate: [roleGuard], data: { ...bothRoles, resourceKey: 'products' } },
      { path: 'products/:id/edit', component: ResourceFormPageComponent, canActivate: [roleGuard], data: { ...adminOnly, resourceKey: 'products' } },

      {
        path: 'service-categories',
        component: ResourceListPageComponent,
        canActivate: [roleGuard],
        data: { ...adminOnly, resourceKey: 'service-categories' },
      },
      {
        path: 'service-categories/new',
        component: ResourceFormPageComponent,
        canActivate: [roleGuard],
        data: { ...adminOnly, resourceKey: 'service-categories' },
      },
      {
        path: 'service-categories/:id',
        component: ResourceDetailPageComponent,
        canActivate: [roleGuard],
        data: { ...adminOnly, resourceKey: 'service-categories' },
      },
      {
        path: 'service-categories/:id/edit',
        component: ResourceFormPageComponent,
        canActivate: [roleGuard],
        data: { ...adminOnly, resourceKey: 'service-categories' },
      },

      { path: 'services', component: ResourceListPageComponent, canActivate: [roleGuard], data: { ...bothRoles, resourceKey: 'services' } },
      { path: 'services/new', component: ResourceFormPageComponent, canActivate: [roleGuard], data: { ...adminOnly, resourceKey: 'services' } },
      { path: 'services/:id', component: ResourceDetailPageComponent, canActivate: [roleGuard], data: { ...bothRoles, resourceKey: 'services' } },
      { path: 'services/:id/edit', component: ResourceFormPageComponent, canActivate: [roleGuard], data: { ...adminOnly, resourceKey: 'services' } },

      {
        path: 'quotation-settings',
        component: QuotationSettingsPageComponent,
        canActivate: [roleGuard],
        data: adminOnly,
      },

      {
        path: 'quotations',
        component: QuotationsListPageComponent,
        canActivate: [roleGuard],
        data: bothRoles,
      },
      {
        path: 'quotations/new',
        component: QuotationFormPageComponent,
        canActivate: [roleGuard],
        data: bothRoles,
      },
      {
        path: 'quotations/:id',
        component: QuotationDetailPageComponent,
        canActivate: [roleGuard],
        data: bothRoles,
      },
      {
        path: 'quotations/:id/edit',
        component: QuotationFormPageComponent,
        canActivate: [roleGuard],
        data: bothRoles,
      },
      {
        path: 'quotations/:id/pdf-preview',
        component: QuotationPdfPreviewPageComponent,
        canActivate: [roleGuard],
        data: bothRoles,
      },

      {
        path: 'maintenance',
        component: MaintenanceListPageComponent,
        canActivate: [roleGuard],
        data: bothRoles,
      },
      {
        path: 'maintenance/new',
        component: MaintenanceFormPageComponent,
        canActivate: [roleGuard],
        data: bothRoles,
      },
      {
        path: 'maintenance/:id',
        component: MaintenanceDetailPageComponent,
        canActivate: [roleGuard],
        data: bothRoles,
      },
      {
        path: 'maintenance/:id/edit',
        component: MaintenanceFormPageComponent,
        canActivate: [roleGuard],
        data: bothRoles,
      },

      {
        path: 'audit-logs',
        component: AuditLogListPageComponent,
        canActivate: [roleGuard],
        data: adminOnly,
      },
    ],
  },
  { path: '**', redirectTo: '' },
];

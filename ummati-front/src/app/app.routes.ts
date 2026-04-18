import { Routes } from '@angular/router';
import { authGuard, guestGuard } from './core/guards/auth.guard';

export const routes: Routes = [
  {
    path: '',
    loadComponent: () => import('./pages/home/home.component').then(m => m.HomeComponent),
  },
  {
    path: 'login',
    canActivate: [guestGuard],
    loadComponent: () => import('./pages/auth/login/login.component').then(m => m.LoginComponent),
  },
  {
    path: 'register',
    canActivate: [guestGuard],
    loadComponent: () => import('./pages/auth/register/register.component').then(m => m.RegisterComponent),
  },
  {
    path: 'forgot-password',
    loadComponent: () => import('./pages/auth/forgot-password/forgot-password.component').then(m => m.ForgotPasswordComponent),
  },
  {
    path: 'reset-password',
    loadComponent: () => import('./pages/auth/reset-password/reset-password.component').then(m => m.ResetPasswordComponent),
  },
  {
    path: 'onboarding',
    canActivate: [authGuard],
    loadComponent: () => import('./pages/onboarding/onboarding.component').then(m => m.OnboardingComponent),
  },
  {
    path: 'profile',
    canActivate: [authGuard],
    loadComponent: () => import('./pages/profile/profile.component').then(m => m.ProfileComponent),
  },
  {
    path: 'organizations',
    loadComponent: () => import('./pages/organizations/organization-list/organization-list.component').then(m => m.OrganizationListComponent),
  },
  {
    path: 'organizations/new',
    canActivate: [authGuard],
    loadComponent: () => import('./pages/organizations/organization-create/organization-create.component').then(m => m.OrganizationCreateComponent),
  },
  {
    path: 'organizations/:slug/manage',
    canActivate: [authGuard],
    loadComponent: () => import('./pages/organizations/organization-manage/organization-manage.component').then(m => m.OrganizationManageComponent),
  },
  {
    path: 'organizations/:slug',
    loadComponent: () => import('./pages/organizations/organization-detail/organization-detail.component').then(m => m.OrganizationDetailComponent),
  },
  {
    path: 'events',
    loadComponent: () => import('./pages/events/event-list/event-list.component').then(m => m.EventListComponent),
  },
  {
    path: 'events/:id',
    loadComponent: () => import('./pages/events/event-detail/event-detail.component').then(m => m.EventDetailComponent),
  },
  {
    path: 'events/:eventId/edit',
    canActivate: [authGuard],
    loadComponent: () => import('./pages/events/event-create/event-create.component').then(m => m.EventCreateComponent),
  },
  {
    path: 'organizations/:orgId/events/new',
    canActivate: [authGuard],
    loadComponent: () => import('./pages/events/event-create/event-create.component').then(m => m.EventCreateComponent),
  },
  {
    path: 'organizations/:orgId/events/manage',
    canActivate: [authGuard],
    loadComponent: () => import('./pages/events/event-manage/event-manage.component').then(m => m.EventManageComponent),
  },
  {
    path: 'dashboard',
    canActivate: [authGuard],
    loadComponent: () => import('./pages/dashboard/dashboard.component').then(m => m.DashboardComponent),
  },
  {
    path: 'notifications',
    canActivate: [authGuard],
    loadComponent: () => import('./pages/notifications/notifications.component').then(m => m.NotificationsComponent),
  },
  {
    path: 'admin',
    canActivate: [authGuard],
    loadComponent: () => import('./pages/admin/admin.component').then(m => m.AdminComponent),
  },
  {
    path: 'organizations/:orgId/dashboard',
    canActivate: [authGuard],
    loadComponent: () => import('./pages/dashboard/org-dashboard.component').then(m => m.OrgDashboardComponent),
  },
  {
    path: 'my-activities',
    canActivate: [authGuard],
    loadComponent: () => import('./pages/profile/my-activities.component').then(m => m.MyActivitiesComponent),
  },
  {
    path: 'settings',
    canActivate: [authGuard],
    loadComponent: () => import('./pages/settings/settings.component').then(m => m.SettingsComponent),
  },
  {
    path: 'admin/organizations/:slug/validate',
    canActivate: [authGuard],
    loadComponent: () => import('./pages/admin/admin-org-validation/admin-org-validation.component').then(m => m.AdminOrgValidationComponent),
  },
  {
    path: '**',
    redirectTo: '',
  },
];

import { RenderMode, ServerRoute } from '@angular/ssr';

export const serverRoutes: ServerRoute[] = [
  {
    path: '',
    renderMode: RenderMode.Client,
  },
  {
    path: 'organizations/:slug',
    renderMode: RenderMode.Client,
  },
  {
    path: 'organizations/:slug/manage',
    renderMode: RenderMode.Client,
  },
  {
    path: 'organizations',
    renderMode: RenderMode.Client,
  },
  {
    path: 'organizations/new',
    renderMode: RenderMode.Client,
  },
  {
    path: 'events',
    renderMode: RenderMode.Client,
  },
  {
    path: 'events/:id',
    renderMode: RenderMode.Client,
  },
  {
    path: 'events/:eventId/edit',
    renderMode: RenderMode.Client,
  },
  {
    path: 'organizations/:orgId/events/new',
    renderMode: RenderMode.Client,
  },
  {
    path: 'organizations/:orgId/events/manage',
    renderMode: RenderMode.Client,
  },
  {
    path: 'onboarding',
    renderMode: RenderMode.Client,
  },
  {
    path: 'profile',
    renderMode: RenderMode.Client,
  },
  {
    path: 'reset-password',
    renderMode: RenderMode.Client,
  },
  {
    path: 'dashboard',
    renderMode: RenderMode.Client,
  },
  {
    path: 'notifications',
    renderMode: RenderMode.Client,
  },
  {
    path: 'admin',
    renderMode: RenderMode.Client,
  },
  {
    path: 'organizations/:orgId/dashboard',
    renderMode: RenderMode.Client,
  },
  {
    path: 'my-activities',
    renderMode: RenderMode.Client,
  },
  {
    path: 'settings',
    renderMode: RenderMode.Client,
  },
  {
    path: 'admin/organizations/:slug/validate',
    renderMode: RenderMode.Client,
  },
  {
    path: '**',
    renderMode: RenderMode.Prerender,
  },
];

import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';
import { roleGuard } from './core/guards/role.guard';

export const routes: Routes = [
  {
    path: 'cart',
    loadComponent: () => import('./features/orders/cart-page.component').then((m) => m.CartPageComponent)
  },
  {
    path: 'checkout',
    canActivate: [authGuard],
    loadComponent: () => import('./features/orders/checkout-page.component').then((m) => m.CheckoutPageComponent)
  },
  {
    path: 'orders',
    canActivate: [authGuard],
    loadComponent: () => import('./features/orders/orders-page.component').then((m) => m.OrdersPageComponent)
  },
  {
    path: 'orders/:id',
    canActivate: [authGuard],
    loadComponent: () => import('./features/orders/order-detail-page.component').then((m) => m.OrderDetailPageComponent)
  },
  {
    path: 'products',
    loadComponent: () => import('./features/catalog/catalog-page.component').then((m) => m.CatalogPageComponent)
  },
  {
    path: 'products/:id',
    loadComponent: () => import('./features/catalog/product-detail.component').then((m) => m.ProductDetailComponent)
  },
  {
    path: 'login',
    loadComponent: () => import('./features/auth/login/login.component').then((m) => m.LoginComponent)
  },
  {
    path: 'register',
    loadComponent: () => import('./features/auth/register/register.component').then((m) => m.RegisterComponent)
  },
  {
    path: 'app',
    canActivate: [authGuard],
    loadComponent: () => import('./features/app-shell/app-shell.component').then((m) => m.AppShellComponent),
    children: [
      {
        path: '',
        pathMatch: 'full',
        loadComponent: () => import('./features/app-shell/home.component').then((m) => m.HomeComponent)
      },
      {
        path: 'forbidden',
        loadComponent: () => import('./features/app-shell/forbidden.component').then((m) => m.ForbiddenComponent)
      },
      {
        path: 'admin',
        canActivate: [roleGuard],
        data: { roles: ['ADMIN'] },
        children: [
          {
            path: '',
            pathMatch: 'full',
            redirectTo: 'products'
          },
          {
            path: 'inventory',
            loadComponent: () =>
              import('./features/admin/inventory/admin-inventory-page.component').then((m) => m.AdminInventoryPageComponent)
          },
          {
            path: 'discounts',
            loadComponent: () =>
              import('./features/admin/discounts/admin-discounts-page.component').then((m) => m.AdminDiscountsPageComponent)
          },
          {
            path: 'products',
            loadComponent: () =>
              import('./features/admin/products/admin-products-page.component').then((m) => m.AdminProductsPageComponent)
          },
          {
            path: 'reports',
            loadComponent: () =>
              import('./features/admin/reports/admin-reports-page.component').then((m) => m.AdminReportsPageComponent)
          },
          {
            path: 'audit',
            loadComponent: () =>
              import('./features/admin/audit/admin-audit-page.component').then((m) => m.AdminAuditPageComponent)
          }
        ]
      }
    ]
  },
  {
    path: '',
    pathMatch: 'full',
    redirectTo: 'products'
  },
  {
    path: '**',
    redirectTo: 'products'
  }
];

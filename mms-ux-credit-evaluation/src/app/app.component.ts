import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet],
  template: `
    <header class="app-header">
      <div class="app-header__inner">
        <div class="app-header__brand">
          <span class="app-header__logo">🏦</span>
          <span class="app-header__title">Austro</span>
          <span class="app-header__subtitle">Sistema de Evaluación de Créditos</span>
        </div>
      </div>
    </header>
    <main class="app-main">
      <router-outlet />
    </main>
  `,
  styles: [`
    .app-header {
      background: linear-gradient(135deg, #1a56db 0%, #1e429f 100%);
      color: white;
      padding: 0;
      box-shadow: 0 2px 8px rgba(0,0,0,0.15);
    }
    .app-header__inner {
      max-width: 960px;
      margin: 0 auto;
      padding: 16px 24px;
    }
    .app-header__brand {
      display: flex;
      align-items: center;
      gap: 12px;
    }
    .app-header__logo {
      font-size: 1.75rem;
    }
    .app-header__title {
      font-size: 1.375rem;
      font-weight: 700;
      letter-spacing: -0.02em;
    }
    .app-header__subtitle {
      font-size: 0.875rem;
      opacity: 0.8;
      border-left: 1px solid rgba(255,255,255,0.3);
      padding-left: 12px;
      margin-left: 4px;
    }
    .app-main {
      max-width: 960px;
      margin: 0 auto;
      padding: 32px 24px;
    }
    @media (max-width: 640px) {
      .app-header__subtitle { display: none; }
      .app-main { padding: 16px; }
    }
  `]
})
export class AppComponent {}

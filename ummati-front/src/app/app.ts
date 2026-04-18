import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { NavbarComponent } from './shared/components/navbar/navbar.component';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, NavbarComponent],
  template: `
    <!-- T-137: Skip to main content (accessibilité clavier) -->
    <a href="#main-content" class="skip-link">Aller au contenu principal</a>
    <app-navbar />
    <main id="main-content" tabindex="-1">
      <router-outlet />
    </main>
  `,
  styles: [`
    main {
      min-height: calc(100vh - 64px);
    }
    main:focus { outline: none; }
  `],
})
export class App {}

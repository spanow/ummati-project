import { Injectable, PLATFORM_ID, inject, signal } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';

export type ThemeChoice = 'light' | 'dark' | 'system';
export type ResolvedTheme = 'light' | 'dark';

const STORAGE_KEY = 'ummati_theme';

/**
 * Thème clair/sombre.
 *
 * L'attribut `data-theme` est déjà posé sur <html> par le script inline de
 * index.html (avant le premier rendu, pour éviter le flash blanc). Ce service
 * ne fait que refléter et modifier cet état — il ne l'initialise pas.
 *
 * 'system' = pas d'entrée en localStorage, on suit `prefers-color-scheme`
 * et on réagit à un changement de réglage OS à chaud.
 */
@Injectable({ providedIn: 'root' })
export class ThemeService {
  private platformId = inject(PLATFORM_ID);

  /** Préférence de l'utilisateur (peut valoir 'system'). */
  readonly choice = signal<ThemeChoice>('system');
  /** Thème réellement appliqué — 'system' déjà résolu. */
  readonly resolved = signal<ResolvedTheme>('light');

  constructor() {
    if (!isPlatformBrowser(this.platformId)) return;

    const stored = this.readStored();
    this.choice.set(stored);
    this.apply(stored);

    // Suivi du réglage OS tant que l'utilisateur n'a pas tranché lui-même.
    window.matchMedia?.('(prefers-color-scheme: dark)').addEventListener('change', () => {
      if (this.choice() === 'system') this.apply('system');
    });
  }

  setTheme(choice: ThemeChoice) {
    if (!isPlatformBrowser(this.platformId)) return;
    this.choice.set(choice);
    try {
      if (choice === 'system') localStorage.removeItem(STORAGE_KEY);
      else localStorage.setItem(STORAGE_KEY, choice);
    } catch {
      /* stockage indisponible : le thème reste appliqué pour la session */
    }
    this.apply(choice);
  }

  /** Bascule clair <-> sombre en partant du thème réellement affiché. */
  toggle() {
    this.setTheme(this.resolved() === 'dark' ? 'light' : 'dark');
  }

  private readStored(): ThemeChoice {
    try {
      const v = localStorage.getItem(STORAGE_KEY);
      return v === 'dark' || v === 'light' ? v : 'system';
    } catch {
      return 'system';
    }
  }

  private apply(choice: ThemeChoice) {
    const resolved: ResolvedTheme =
      choice === 'system' ? (this.prefersDark() ? 'dark' : 'light') : choice;
    this.resolved.set(resolved);
    document.documentElement.setAttribute('data-theme', resolved);
  }

  private prefersDark(): boolean {
    return !!window.matchMedia?.('(prefers-color-scheme: dark)').matches;
  }
}

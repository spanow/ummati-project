import { Injectable, PLATFORM_ID, inject, signal } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { AR } from '../i18n/ar';

export type Lang = 'fr' | 'ar';

const STORAGE_KEY = 'ummati_lang';

/** Lit la langue stockée — utilisable hors injection (app.config). */
export function getStoredLang(): Lang {
  if (typeof localStorage === 'undefined') return 'fr';
  return localStorage.getItem(STORAGE_KEY) === 'ar' ? 'ar' : 'fr';
}

/**
 * i18n minimaliste façon gettext : la clé EST le texte français.
 * Une chaîne absente du dictionnaire arabe s'affiche en français (fallback).
 * Le changement de langue recharge la page : le pipe peut rester pur,
 * et Material/le DOM basculent proprement en RTL.
 */
@Injectable({ providedIn: 'root' })
export class I18nService {
  private platformId = inject(PLATFORM_ID);
  readonly lang = signal<Lang>('fr');

  constructor() {
    if (isPlatformBrowser(this.platformId)) {
      const stored = getStoredLang();
      this.lang.set(stored);
      this.applyToDocument(stored);
    }
  }

  t(key: string): string {
    if (this.lang() === 'ar') {
      return AR[key] ?? key;
    }
    return key;
  }

  setLang(lang: Lang) {
    if (!isPlatformBrowser(this.platformId) || lang === this.lang()) return;
    localStorage.setItem(STORAGE_KEY, lang);
    // Reload : LOCALE_ID, dir et toutes les chaînes sont réévalués au bootstrap
    location.reload();
  }

  private applyToDocument(lang: Lang) {
    document.documentElement.lang = lang;
    document.documentElement.dir = lang === 'ar' ? 'rtl' : 'ltr';
  }
}

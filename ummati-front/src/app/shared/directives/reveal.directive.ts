import {
  Directive,
  ElementRef,
  Input,
  OnDestroy,
  OnInit,
  PLATFORM_ID,
  inject,
} from '@angular/core';
import { isPlatformBrowser } from '@angular/common';

/**
 * Révèle un élément quand il entre dans le champ de vision.
 *
 *   <section appReveal>…</section>
 *   <article appReveal [revealDelay]="80">…</article>
 *
 * L'élément part masqué (classe `.reveal` de styles.css) et reçoit
 * `.is-visible` au croisement. En SSR — ou si IntersectionObserver manque —
 * on affiche immédiatement : jamais de contenu invisible.
 */
@Directive({
  selector: '[appReveal]',
  standalone: true,
})
export class RevealDirective implements OnInit, OnDestroy {
  /** Décalage d'apparition en ms — pour cascader une grille. */
  @Input() revealDelay = 0;

  private el = inject(ElementRef<HTMLElement>);
  private platformId = inject(PLATFORM_ID);
  private observer?: IntersectionObserver;

  ngOnInit() {
    const node = this.el.nativeElement as HTMLElement;

    if (!isPlatformBrowser(this.platformId) || typeof IntersectionObserver === 'undefined') {
      node.classList.add('reveal', 'is-visible');
      return;
    }

    node.classList.add('reveal');
    if (this.revealDelay) node.style.transitionDelay = `${this.revealDelay}ms`;

    this.observer = new IntersectionObserver(
      (entries) => {
        for (const entry of entries) {
          if (!entry.isIntersecting) continue;
          node.classList.add('is-visible');
          this.disconnect(); // une seule fois : pas de ré-animation au scroll arrière
        }
      },
      { threshold: 0.12, rootMargin: '0px 0px -8% 0px' },
    );
    this.observer.observe(node);
  }

  ngOnDestroy() {
    this.disconnect();
  }

  private disconnect() {
    this.observer?.disconnect();
    this.observer = undefined;
  }
}

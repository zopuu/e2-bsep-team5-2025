import { Component } from '@angular/core';
import { BehaviorSubject } from 'rxjs';

@Component({
  selector: 'app-admin-shell',
  templateUrl: './admin-shell.component.html',
  styleUrls: ['./admin-shell.component.scss']
})
export class AdminShellComponent {
  open = false;                // mobile sidebar
  pendingCsrs = 3;             // TODO: wire to API

  title$ = new BehaviorSubject<string>('Admin');

  onActivate(comp: any) {
    const t =
      comp?.pageTitle ??
      comp?.title ??
      (comp?.constructor?.name?.replace?.(/Component$/, '') ?? 'Admin');

    // microtask: avoids NG0100 without a full tick
    Promise.resolve().then(() => this.title$.next(t));
  }

  openCmd() {
    alert('Command palette placeholder. Add quick actions here.');
  }
  ngAfterViewInit() {
    document.querySelectorAll('aside nav a').forEach(a => {
      a.addEventListener('pointerdown', (ev: any) => {
        const r = (ev.currentTarget as HTMLElement).getBoundingClientRect();
        (ev.currentTarget as HTMLElement).style.setProperty('--x', `${ev.clientX - r.left}px`);
        (ev.currentTarget as HTMLElement).style.setProperty('--y', `${ev.clientY - r.top}px`);
      });
    });
  }
}

import { Component } from '@angular/core';

@Component({
  selector: 'app-admin-shell',
  templateUrl: './admin-shell.component.html',
  styleUrls: ['./admin-shell.component.scss']
})
export class AdminShellComponent {
  open = false;                // mobile sidebar
  pageTitle = 'Dashboard';     // set by child pages via onActivate
  pendingCsrs = 3;             // TODO: wire to API

  onActivate(child: any) {
    this.pageTitle = child?.title ?? 'Admin';
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

import { Component } from '@angular/core';
import { BehaviorSubject } from 'rxjs';

@Component({
  selector: 'app-ca-shell',
  templateUrl: './ca-shell.component.html',
  styleUrls: ['./ca-shell.component.scss']
})
export class CaShellComponent {
  open = false;
  title$ = new BehaviorSubject<string>('CA Console');

  onActivate(child: any) {
    // let child set page title if it exposes it
    const t = child?.title || child?.pageTitle || child?.constructor?.name;
    if (typeof t === 'string' && t.trim().length) {
      this.title$.next(t);
    }
  }

  openCmd() {
    // placeholder for your ⌘K command palette
    // (kept to match AdminShell behavior)
  }
}

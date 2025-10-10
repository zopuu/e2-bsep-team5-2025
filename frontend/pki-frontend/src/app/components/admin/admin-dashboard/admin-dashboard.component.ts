import { Component } from '@angular/core';

@Component({
  selector: 'app-admin-dashboard',
  templateUrl: './admin-dashboard.component.html',
  styleUrls: ['./admin-dashboard.component.scss']
})
export class AdminDashboardComponent {
  title = 'Dashboard'; // picked by shell
  kpi = { pendingCsrs: 3, expiring30d: 7, revokedToday: 1, activeCas: 2 };
  pendingCsrs = []; expiring = []; events = []; // TODO: wire services
}

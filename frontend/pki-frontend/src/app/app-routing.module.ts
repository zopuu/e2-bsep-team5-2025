import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { RegistrationComponent } from './components/registration/registration.component';
import { LoginComponent } from './components/login/login.component';
import { ActivationComponent } from './components/activation/activation.component';
import { HomeComponent } from './components/home/home.component';
import { AdminComponent } from './components/admin/admin.component';
import { AdminGuard } from './guards/admin.guard';
import { AdminCaComponent } from './components/admin/admin-ca/admin-ca.component';

const routes: Routes = [
  
  { path: '', redirectTo: '/register', pathMatch: 'full' },
  { path: 'admin', component: AdminComponent, canActivate: [AdminGuard]  },
  { path: 'register', component: RegistrationComponent },
  { path: 'login', component: LoginComponent },
  { path: 'activate', component: ActivationComponent },
  { path: 'home', component: HomeComponent },
  { path: 'admin/ca', component: AdminCaComponent, canActivate: [AdminGuard] },
  { path: '**', redirectTo: '/register' },
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule { }

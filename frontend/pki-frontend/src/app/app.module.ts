import { NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { ReactiveFormsModule, FormsModule } from '@angular/forms';
import { HttpClientModule, HTTP_INTERCEPTORS } from '@angular/common/http';

import { AppRoutingModule } from './app-routing.module';
import { AppComponent } from './app.component';
import { RegistrationComponent } from './components/registration/registration.component';
import { ActivationComponent } from './components/activation/activation.component';
import { LoginComponent } from './components/login/login.component';
import { JwtInterceptor } from './interceptors/jwt.interceptor';
import { HomeComponent } from './components/home/home.component';
import { AdminComponent } from './components/admin/admin.component';
import { AdminCaComponent } from './components/admin/admin-ca/admin-ca.component';
import { AdminIntermediateComponent } from './components/admin/admin-intermediate/admin-intermediate.component';
import { AdminShellComponent } from './components/admin/admin-shell/admin-shell.component';
import { AdminDashboardComponent } from './components/admin/admin-dashboard/admin-dashboard.component';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { MatPaginator, MatPaginatorModule } from '@angular/material/paginator';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatSnackBarModule } from '@angular/material/snack-bar';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { AdminCertificatesComponent } from './components/admin/admin-certificates/admin-certificates.component';
import { MatDialog } from '@angular/material/dialog';
import { MatDialogModule } from '@angular/material/dialog';
import { ForgotPasswordComponent } from './components/forgot-password/forgot-password.component';
import { ResetPasswordComponent } from './components/reset-password/reset-password.component';
import { AdminUsersComponent } from './components/admin/admin-users/admin-users.component';
import { CaIssuersComponent } from './components/ca/ca-issuers/ca-issuers.component';
import { CaShellComponent } from './components/ca/ca-shell/ca-shell.component';

@NgModule({
  declarations: [
    AppComponent,
    RegistrationComponent,
    ActivationComponent,
    LoginComponent,
    HomeComponent,
    AdminComponent,
    AdminCaComponent,
    AdminIntermediateComponent,
    AdminShellComponent,
    AdminDashboardComponent,
    AdminCertificatesComponent,
    ForgotPasswordComponent,
    ResetPasswordComponent,
    AdminUsersComponent,
    CaIssuersComponent,
    CaShellComponent
  ],
  imports: [
    BrowserModule,
    BrowserAnimationsModule,
    AppRoutingModule,
    ReactiveFormsModule,
    HttpClientModule,
    CommonModule,
    RouterModule,
    MatPaginatorModule,
    ReactiveFormsModule,
    MatSnackBarModule,
    MatPaginatorModule,
    MatDialogModule,
    FormsModule,
    HttpClientModule
  ],
  providers: [
    { provide: HTTP_INTERCEPTORS, useClass: JwtInterceptor, multi: true }
  ],
  bootstrap: [AppComponent]
})
export class AppModule { }

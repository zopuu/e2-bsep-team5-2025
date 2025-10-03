import { ComponentFixture, TestBed } from '@angular/core/testing';

import { AdminCaComponent } from './admin-ca.component';

describe('AdminCaComponent', () => {
  let component: AdminCaComponent;
  let fixture: ComponentFixture<AdminCaComponent>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [AdminCaComponent]
    });
    fixture = TestBed.createComponent(AdminCaComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});

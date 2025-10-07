import { ComponentFixture, TestBed } from '@angular/core/testing';

import { AdminIntermediateComponent } from './admin-intermediate.component';

describe('AdminIntermediateComponent', () => {
  let component: AdminIntermediateComponent;
  let fixture: ComponentFixture<AdminIntermediateComponent>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [AdminIntermediateComponent]
    });
    fixture = TestBed.createComponent(AdminIntermediateComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});

import { ComponentFixture, TestBed } from '@angular/core/testing';

import { CaShellComponent } from './ca-shell.component';

describe('CaShellComponent', () => {
  let component: CaShellComponent;
  let fixture: ComponentFixture<CaShellComponent>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [CaShellComponent]
    });
    fixture = TestBed.createComponent(CaShellComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});

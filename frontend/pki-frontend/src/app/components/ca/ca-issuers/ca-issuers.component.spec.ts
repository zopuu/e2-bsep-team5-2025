import { ComponentFixture, TestBed } from '@angular/core/testing';

import { CaIssuersComponent } from './ca-issuers.component';

describe('CaIssuersComponent', () => {
  let component: CaIssuersComponent;
  let fixture: ComponentFixture<CaIssuersComponent>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [CaIssuersComponent]
    });
    fixture = TestBed.createComponent(CaIssuersComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});

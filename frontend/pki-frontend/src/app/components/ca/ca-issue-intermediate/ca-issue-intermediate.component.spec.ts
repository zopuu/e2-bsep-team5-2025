import { ComponentFixture, TestBed } from '@angular/core/testing';

import { CaIssueIntermediateComponent } from './ca-issue-intermediate.component';

describe('CaIssueIntermediateComponent', () => {
  let component: CaIssueIntermediateComponent;
  let fixture: ComponentFixture<CaIssueIntermediateComponent>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [CaIssueIntermediateComponent]
    });
    fixture = TestBed.createComponent(CaIssueIntermediateComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});

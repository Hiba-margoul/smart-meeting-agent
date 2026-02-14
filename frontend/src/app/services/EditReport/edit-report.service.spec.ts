import { TestBed } from '@angular/core/testing';

import { EditReportService } from './edit-report.service';

describe('EditReportService', () => {
  let service: EditReportService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(EditReportService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});

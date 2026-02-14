import { TestBed } from '@angular/core/testing';

import { NewTemplateService } from './new-template.service';

describe('NewTemplateService', () => {
  let service: NewTemplateService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(NewTemplateService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});

import { TestBed } from '@angular/core/testing';

import { LiveKiteService } from './live-kite.service';

describe('LiveKiteService', () => {
  let service: LiveKiteService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(LiveKiteService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});

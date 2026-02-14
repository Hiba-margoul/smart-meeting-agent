import { ComponentFixture, TestBed } from '@angular/core/testing';

import { RoomMeetComponent } from './room-meet.component';

describe('RoomMeetComponent', () => {
  let component: RoomMeetComponent;
  let fixture: ComponentFixture<RoomMeetComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [RoomMeetComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(RoomMeetComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});

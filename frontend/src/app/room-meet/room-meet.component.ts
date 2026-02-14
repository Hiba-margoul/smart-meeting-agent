import { Component, OnInit, OnDestroy } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { Participant } from 'livekit-client';
import { HttpClient } from '@angular/common/http';
import { LiveKiteService } from '../services/LiveKitService/live-kite.service';
import { NgClass, NgForOf, NgIf, UpperCasePipe, Location } from '@angular/common';
import {Meet} from '../model/meet';

@Component({
  selector: 'app-room-meet',
  standalone: true,
  imports: [
    UpperCasePipe,
    NgClass,
    NgForOf,
    NgIf
  ],
  templateUrl: './room-meet.component.html',
  styleUrl: './room-meet.component.css'
})
export class RoomMeetComponent implements OnInit, OnDestroy {

  participants: Participant[] = [];
  activeSpeakers: Participant[] = [];
  isMicOn = true;
  meetId: string = '';
  meet!: Meet;


  constructor(
    private route: ActivatedRoute,
    private livekitService: LiveKiteService,
    private http: HttpClient,
    private router: Router,
    private location : Location
  ) {}

  ngOnInit() {
    this.meetId = this.route.snapshot.paramMap.get('id')!;
    this.livekitService.getMeet(this.meetId).subscribe({
      next: (meetData) => {
        this.meet = meetData; // this.meet contient maintenant la vraie réunion
        console.log("Réunion récupérée :", this.meet);
      },
      error: (err) => {
        console.error("Erreur en récupérant la réunion :", err);
      }
    });

    this.http.post<any>('http://localhost:8089/meeting/join_meet', { "meetId": this.meetId })
      .subscribe(async (response) => {
        const token = response.token;
        await this.livekitService.joinRoom(token);
      });

    this.livekitService.participants$.subscribe(p => this.participants = p);
    this.livekitService.activeSpeakers$.subscribe(s => this.activeSpeakers = s);
  }

  isSpeaking(p: Participant): boolean {
    return this.activeSpeakers.includes(p);
  }

  toggleMic() {
    this.isMicOn = !this.isMicOn;
    this.livekitService.toggleMicro(this.isMicOn);
  }

  leaveMeet() {
    this.livekitService.closeMeet(this.meet.liveKitRoomName).subscribe({
      next: () => {
        console.log(this.meet.liveKitRoomName);
        console.log('Meet closed successfully');
        this.livekitService.leave(); // 👈 APRES
        this.goBack();
      },
      error: () => {
        this.livekitService.leave();
        this.goBack();
      }
    });
  }

  ngOnDestroy() {
    this.livekitService.leave();
  }

  // Petite fonction bonus pour générer une couleur unique par pseudo
  stringToColor(str: string): string {
    let hash = 0;
    for (let i = 0; i < str.length; i++) {
      hash = str.charCodeAt(i) + ((hash << 5) - hash);
    }
    const c = (hash & 0x00FFFFFF).toString(16).toUpperCase();
    return '#' + '00000'.substring(0, 6 - c.length) + c;
  }
  goBack() {
    this.location.back();
  }
  isAgent(p: Participant): boolean {
    const identifier = (p.name || p.identity || '').toLowerCase();
    return identifier.includes('agent') || identifier.includes('ia');
  }
}

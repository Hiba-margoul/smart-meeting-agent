import { Injectable } from '@angular/core';
import { Room, RoomEvent, RemoteParticipant, LocalParticipant, Participant } from 'livekit-client';
import { BehaviorSubject } from 'rxjs';
import {HttpClient} from '@angular/common/http';
import {Meet} from '../../model/meet';

@Injectable({
  providedIn: 'root'
})
export class LiveKiteService {
  private room: Room;

  // Observables pour que ton UI sache quoi afficher
  public participants$ = new BehaviorSubject<Participant[]>([]);
  public activeSpeakers$ = new BehaviorSubject<Participant[]>([]);

  constructor(private http: HttpClient) {
    // Configuration de base de la room
    this.room = new Room({
      // Pour l'audio only, on optimise
      adaptiveStream: true,
      dynacast: true,
    });
  }

  async joinRoom(token: string) {
    // 1. Connexion au serveur LiveKit (url de ton docker)
    await this.room.connect('ws://localhost:7880', token);
    console.log('Connecté à la room !', this.room.name);

    // 2. Activer le micro par défaut
    await this.room.localParticipant.setMicrophoneEnabled(true);

    // 3. Écouter les événements (Qui arrive, qui part, qui parle)
    this.setupEventListeners();

    // 4. Mettre à jour la liste initiale
    this.updateParticipants();
  }

  private setupEventListeners() {
    // Quand quelqu'un rejoint
    this.room.on(RoomEvent.ParticipantConnected, () => this.updateParticipants());
    this.room.on(RoomEvent.ParticipantDisconnected, () => this.updateParticipants());

    // Quand quelqu'un parle (visuel vert autour de l'avatar)
    this.room.on(RoomEvent.ActiveSpeakersChanged, (speakers) => {
      this.activeSpeakers$.next(speakers);
    });

    // Quand une piste audio arrive (L'ENTENDRE !)
    this.room.on(RoomEvent.TrackSubscribed, (track, publication, participant) => {
      if (track.kind === 'audio') {
        track.attach(); // Ça crée un élément <audio> invisible dans le DOM automatiquement
      }
    });
  }

  toggleMicro(enabled: boolean) {
    this.room.localParticipant.setMicrophoneEnabled(enabled);
  }

  leave() {
    this.room.disconnect();
  }

  private updateParticipants() {
    // On combine le participant local + les distants pour l'affichage
    const all = [this.room.localParticipant, ...Array.from(this.room.remoteParticipants.values())];
    this.participants$.next(all);
  }
  closeMeet(roomName: string) {
    return this.http.get(
      `http://localhost:8089/meeting/close_meet/${roomName}`,
      { responseType: 'text' }
    );
  }
  getMeet(meetId : string){
    return this.http.get<Meet>(
      `http://localhost:8089/meeting/id/${meetId}`,
    );
  }
}

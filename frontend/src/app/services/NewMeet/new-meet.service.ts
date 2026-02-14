import {inject, Injectable} from '@angular/core';
import {HttpClient, HttpErrorResponse} from '@angular/common/http';
import {catchError, Observable, throwError} from 'rxjs';
import {User} from '../../model/user.model';
import {Meet} from '../../model/meet';
import {MeetRequest} from '../../model/MeetRequest';
import {ReportTemplate} from '../../model/ReportTemplate';

@Injectable({
  providedIn: 'root'
})
export class NewMeetService {

  constructor() { }

  private url_base: string = 'http://localhost:8089/';
  private http = inject(HttpClient);

  getAllUsers() :Observable<User []> {
    return this.http.get<User[]>(`http://localhost:8089/users/`, { withCredentials: true })
      .pipe(
        catchError(this.handleError)  // Gestion des erreurs
      );

  }

  getAllTemplate(): Observable<ReportTemplate [] >{
    return this.http.get<ReportTemplate[]>(`http://localhost:8089/reportTemplate/templates`, { withCredentials: true })
      .pipe(
        catchError(this.handleError)  // Gestion des erreurs
      );
  }

  createMeet(meet : MeetRequest) : Observable<Meet>{

    return this.http.post<Meet>(`${this.url_base}meeting/create_meet`, meet, {withCredentials : true} );
  }
  private handleError(error: HttpErrorResponse) {
    if (error.status === 0) {
      // Erreur réseau ou backend inaccessible
      console.error('Erreur réseau ou serveur indisponible:', error.error);
    } else {
      // Backend renvoie une erreur HTTP
      console.error(
        `Backend a renvoyé le code ${error.status}, ` +
        `le message: ${error.message}`);
    }
    // On retourne une Observable qui émet une erreur
    return throwError(() => new Error('Quelque chose a mal tourné. Veuillez réessayer plus tard.'));
  }
}

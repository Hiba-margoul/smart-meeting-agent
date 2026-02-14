import {inject, Injectable} from '@angular/core';
import {HttpClient, HttpErrorResponse} from '@angular/common/http';
import {MeetRequest} from '../../model/MeetRequest';
import {Observable, throwError} from 'rxjs';
import {ReportTemplate} from '../../model/ReportTemplate';

@Injectable({
  providedIn: 'root'
})
export class NewTemplateService {
  private url_base: string = 'http://localhost:8089/';
  private http = inject(HttpClient);

  constructor() { }
  createTemplate(template :ReportTemplate) : Observable<ReportTemplate>{

    return this.http.post<ReportTemplate>(`${this.url_base}reportTemplate/create_template`, template, {withCredentials : true} );
  }
  private handleError(error: HttpErrorResponse) {
    if (error.status === 0) {

      console.error('Erreur réseau ou serveur indisponible:', error.error);
    } else {

      console.error(
        `Backend a renvoyé le code ${error.status}, ` +
        `le message: ${error.message}`);
    }

    return throwError(() => new Error('Quelque chose a mal tourné. Veuillez réessayer plus tard.'));
  }

}

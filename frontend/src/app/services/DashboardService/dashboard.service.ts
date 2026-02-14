import {inject, Injectable} from '@angular/core';
import {Meet} from '../../model/meet';
import {HttpClient} from '@angular/common/http';
import {map, Observable} from 'rxjs';
import {ReportTemplate} from '../../model/ReportTemplate';

@Injectable({
  providedIn: 'root'
})
export class DashboardService {
  private url_base: string = 'http://localhost:8089/meeting';
  private http = inject(HttpClient);

  constructor() { }

  getMeet(): Observable<Meet[]> {
    const email = localStorage.getItem("email");
    console.log("email", email);

    return this.http.get<Meet[]>(`${this.url_base}/meets`, { withCredentials: true })
      .pipe(
        map(meets => {
          const filteredMeets = meets.filter(meet =>
            meet.invitedUserIds?.includes(email ?? '') || meet.hostId == email
          );
          return filteredMeets.sort((a, b) => {
            return new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime();
          });
        })
      );
  }

  joinMeet(title :string, meetId :string) : Observable<string>{
    return  this.http.post<string>(`${this.url_base}/join_meet`,{title: title , meetId : meetId}, {withCredentials: true});
  }
  voirRapport(title : string , meetId : string) :Observable<ReportTemplate>{
    return this.http.get<ReportTemplate>(
      `${this.url_base}/report`,
      {
        params: {
          title: title,
          meetId: meetId
        }
      }
    );
  }

  getReportPdf(meetId: string) {
    // On précise 'blob' pour dire à Angular que c'est un fichier binaire
    return this.http.get(`http://localhost:8089/api/reports/${meetId}/pdf`, {
      responseType: 'blob'
    });
  }

}



import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {Report} from '../../model/Report';


@Injectable({
  providedIn: 'root'
})
export class ReportService {
  private apiUrl = 'http://localhost:8089/api/reports';

  constructor(private http: HttpClient) {}


  getReportByMeetId(meetId: string): Observable<Report> {
    return this.http.get<Report>(`${this.apiUrl}/${meetId}`);
  }


  saveOrUpdateReport(report: Report): Observable<Report> {
    return this.http.post<Report>(`${this.apiUrl}/save_or_update`, report);
  }
}

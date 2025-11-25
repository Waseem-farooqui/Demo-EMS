import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Position } from '../models/position.model';
import { environment } from '../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class PositionService {
  private apiUrl = `${environment.apiUrl}/positions`;

  constructor(private http: HttpClient) { }

  getAllPositions(): Observable<Position[]> {
    return this.http.get<Position[]>(this.apiUrl);
  }

  searchPositions(query: string): Observable<Position[]> {
    const params = new HttpParams().set('q', query || '');
    return this.http.get<Position[]>(`${this.apiUrl}/search`, { params });
  }
}


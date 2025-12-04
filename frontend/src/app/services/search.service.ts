import {Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {Observable} from 'rxjs';
import {environment} from '../../environments/environment';
import {Employee} from '../models/employee.model';
import {Document} from '../models/document.model';
import {Leave} from '../models/leave.model';

export interface RotaSearchResult {
  id: number;
  department: string;
  fileName: string;
  startDate: string;
  endDate: string;
  uploadedDate: string;
}

export interface SearchResults {
  employees: Employee[];
  documents: Document[];
  leaves: Leave[];
  rotas: RotaSearchResult[];
}

@Injectable({
  providedIn: 'root'
})
export class SearchService {
  private apiUrl = `${environment.apiUrl}/search`;

  constructor(private http: HttpClient) {}

  search(query: string): Observable<SearchResults> {
    return this.http.get<SearchResults>(this.apiUrl, {
      params: {q: query}
    });
  }
}


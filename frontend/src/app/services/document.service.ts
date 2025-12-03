import {Injectable} from '@angular/core';
import {HttpClient, HttpParams} from '@angular/common/http';
import {Observable} from 'rxjs';
import {Document} from '../models/document.model';
import {PageResponse} from '../models/page-response.model';
import {environment} from '../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class DocumentService {
  private apiUrl = `${environment.apiUrl}${environment.endpoints.documents}`;

  constructor(private http: HttpClient) { }

  uploadDocument(employeeId: number, documentType: string, file: File, visaType?: string): Observable<Document> {
    const formData = new FormData();
    formData.append('employeeId', employeeId.toString());
    formData.append('documentType', documentType);
    formData.append('file', file);
    if (visaType && documentType === 'VISA') {
      formData.append('visaType', visaType);
    }

    return this.http.post<Document>(`${this.apiUrl}/upload`, formData);
  }

  getAllDocuments(): Observable<Document[]> {
    return this.http.get<Document[]>(this.apiUrl);
  }

  getAllDocumentsPaginated(page: number = 0, size: number = 10): Observable<PageResponse<Document>> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());
    return this.http.get<PageResponse<Document>>(`${this.apiUrl}/paginated`, { params });
  }

  getDocumentById(id: number): Observable<Document> {
    return this.http.get<Document>(`${this.apiUrl}/${id}`);
  }

  getDocumentsByEmployeeId(employeeId: number): Observable<Document[]> {
    return this.http.get<Document[]>(`${this.apiUrl}/employee/${employeeId}`);
  }

  // Alias for consistency with component usage
  getEmployeeDocuments(employeeId: number): Observable<Document[]> {
    return this.getDocumentsByEmployeeId(employeeId);
  }

  downloadDocument(id: number): Observable<Blob> {
    return this.http.get(`${this.apiUrl}/${id}/download`, { responseType: 'blob' });
  }

  getExpiringDocuments(days: number = 90): Observable<Document[]> {
    return this.http.get<Document[]>(`${this.apiUrl}/expiring?days=${days}`);
  }

  deleteDocument(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  updateDocument(id: number, updateData: any): Observable<Document> {
    return this.http.put<Document>(`${this.apiUrl}/${id}`, updateData);
  }

  getDocumentImage(id: number): Observable<Blob> {
    return this.http.get(`${this.apiUrl}/${id}/image`, { responseType: 'blob' });
  }

  getDocumentPreview(id: number): Observable<Blob> {
    return this.http.get(`${this.apiUrl}/${id}/preview`, { responseType: 'blob' });
  }

  debugDocument(id: number): Observable<any> {
    return this.http.get(`${this.apiUrl}/${id}/debug`);
  }

  checkDuplicateDocument(documents: Document[], newDoc: Document): Document | null {
    return documents.find(doc =>
      doc.documentType === newDoc.documentType &&
      doc.documentNumber === newDoc.documentNumber &&
      doc.id !== newDoc.id
    ) || null;
  }
}


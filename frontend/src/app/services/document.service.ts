import {Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {Observable} from 'rxjs';
import {Document} from '../models/document.model';

@Injectable({
  providedIn: 'root'
})
export class DocumentService {
  private apiUrl = 'http://localhost:8080/api/documents';

  constructor(private http: HttpClient) { }

  uploadDocument(employeeId: number, documentType: string, file: File): Observable<Document> {
    const formData = new FormData();
    formData.append('employeeId', employeeId.toString());
    formData.append('documentType', documentType);
    formData.append('file', file);

    return this.http.post<Document>(`${this.apiUrl}/upload`, formData);
  }

  getAllDocuments(): Observable<Document[]> {
    return this.http.get<Document[]>(this.apiUrl);
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

  checkDuplicateDocument(documents: Document[], newDoc: Document): Document | null {
    return documents.find(doc =>
      doc.documentType === newDoc.documentType &&
      doc.documentNumber === newDoc.documentNumber &&
      doc.id !== newDoc.id
    ) || null;
  }
}


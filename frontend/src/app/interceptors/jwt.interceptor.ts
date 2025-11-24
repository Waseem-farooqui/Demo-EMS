import {Injectable} from '@angular/core';
import {HttpErrorResponse, HttpEvent, HttpHandler, HttpInterceptor, HttpRequest} from '@angular/common/http';
import {Observable, throwError} from 'rxjs';
import {catchError} from 'rxjs/operators';
import {AuthService} from '../services/auth.service';
import {Router} from '@angular/router';

@Injectable()
export class JwtInterceptor implements HttpInterceptor {

  constructor(private authService: AuthService, private router: Router) { }

  intercept(request: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    const token = this.authService.getToken();
    const user = this.authService.getUser();

    if (token) {
      const headers: any = {
        Authorization: `Bearer ${token}`
      };

      // Check if user has roles (not ROOT) but missing organizationUuid
      if (user && user.roles && !user.roles.includes('ROOT') && !user.organizationUuid) {
        console.error('❌ CRITICAL: Non-ROOT user missing organizationUuid!');

        // Show alert to user
        if (typeof window !== 'undefined') {
          alert('⚠️ Session Error: Your session is outdated.\n\nPlease log out and log back in to continue.\n\nThis will refresh your account information.');
        }

        // Logout and redirect
        this.authService.logout();
        this.router.navigate(['/login']);
        return throwError(() => new Error('Session outdated - please login again'));
      }

      // Add organization UUID if user has one (not ROOT user)
      if (user && user.organizationUuid) {
        headers['X-Organization-UUID'] = user.organizationUuid;
      }

      request = request.clone({
        setHeaders: headers
      });
    }

    return next.handle(request).pipe(
      catchError((error: HttpErrorResponse) => {
        if (error.status === 401) {
          // Unauthorized - redirect to login
          this.authService.logout();
          this.router.navigate(['/login']);
        }
        return throwError(() => error);
      })
    );
  }
}


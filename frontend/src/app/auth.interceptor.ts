import { HttpInterceptorFn, HttpRequest, HttpErrorResponse } from '@angular/common/http';
import { inject, Injector } from '@angular/core'; // 1. Import Injector
import { AuthService }  from './auth/auth.service';
import { catchError, switchMap, throwError } from 'rxjs';

export const authInterceptor: HttpInterceptorFn = (req, next) => {

  const injector = inject(Injector);
  if (req.url.endsWith('/auth/login') || req.url.endsWith('/auth/refresh')) {
    return next(req);
  }

  // 3. Manually get AuthService.
  // This happens at "Runtime" (request time), bypassing the "Build time" circular error.
  const authService = injector.get(AuthService);
  const accessToken = authService.getAccessToken();
  const  token = localStorage.getItem("id_token");
  console.log("token ",token);
  console.log("access token", accessToken)

  let authReq = req;

  if (accessToken) {
    authReq = req.clone({
      setHeaders: {
        Authorization: `Bearer ${accessToken}`
      },
      withCredentials: true
    });
  }

  return next(authReq).pipe(
    catchError((error: HttpErrorResponse) => {

      if (error.status === 401) {
        return authService.refreshToken().pipe(
          switchMap((res) => {
            const newAccessToken = res.accessToken;
            authService.setAccessToken(newAccessToken);

            const retryReq = authReq.clone({
              setHeaders: {
                Authorization: `Bearer ${newAccessToken}`
              }
            });

            return next(retryReq);
          }),
          catchError(err => {
            authService.logout();
            return throwError(() => err);
          })
        );
      }

      return throwError(() => error);
    })
  );
};

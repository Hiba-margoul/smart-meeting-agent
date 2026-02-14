import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import moment from "moment";
import { tap } from 'rxjs/operators';
import {TokenClaims} from '../model/tokenClaims.model';
import {jwtDecode} from 'jwt-decode';
import {JwtHelperService} from '@auth0/angular-jwt';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private url_base: string = 'http://localhost:8089';
  private http = inject(HttpClient);
  private accessTokenKey = 'id_token';
  helper = new JwtHelperService();

  constructor() {
  }


  login(email: string, password: string) {
    return this.http.post<any>(`${this.url_base}/auth/login`, {email, password}, {withCredentials: true})
      .pipe(
        tap(res => this.setSession(res))
      );

  }


  private setSession(authResult: any) {
    const token = authResult.token;
    const expiresAt = moment().add(authResult.expiresIn, 'second');
    const payload = JSON.parse(atob(token.split('.')[1]));

    const decodedToken = this.helper.decodeToken(token);
    const isExpired = this.helper.isTokenExpired(token);
    const expirationDate = this.helper.getTokenExpirationDate(token);

    console.log('Token décodé :', decodedToken);
    console.log('Est expiré ? :', isExpired);
    console.log('Date expiration :', expirationDate);
    console.log("role", decodedToken.role);
     localStorage.setItem("email", payload.sub);
    localStorage.setItem(this.accessTokenKey, token);
    localStorage.setItem("role", decodedToken.role);
    localStorage.setItem("expires_at", JSON.stringify(expiresAt.valueOf()));
  }


  logout() {
    localStorage.removeItem(this.accessTokenKey);
    localStorage.removeItem("expires_at");

    window.location.href = 'auth/login';
  }


  public isLoggedIn() {
    return moment().isBefore(this.getExpiration());
  }


  public isLoggedOut() {
    return !this.isLoggedIn();
  }

  /** Récupérer l'expiration du token */
  private getExpiration() {
    const expiration = localStorage.getItem("expires_at");
    if (!expiration) return moment(0); // token absent
    const expiresAt = JSON.parse(expiration);
    return moment(expiresAt);
  }


  getAccessToken(): string | null {
    console.log("acess token", localStorage.getItem(this.accessTokenKey));
    return localStorage.getItem(this.accessTokenKey);

  }

  /** Définir un nouveau token après refresh */
  setAccessToken(token: string) {
    localStorage.setItem(this.accessTokenKey, token);
  }

  /** Refresh token en cookie HttpOnly */
  refreshToken() {
    // Le cookie HttpOnly sera envoyé automatiquement via withCredentials
    return this.http.post<any>('/auth/refresh', {}, {withCredentials: true})
      .pipe(
        tap(res => {
          // Mettre à jour le token d'accès stocké
          if (res.accessToken) {
            this.setAccessToken(res.accessToken);
          }
        })
      );
  }

  getDecodedAccessToken(token: string): TokenClaims | null {
    try {
      return jwtDecode<TokenClaims>(token);
    } catch (Error) {
      return null;
    }
  }

  getUserRole(): string | null {
    const token = this.getAccessToken();
    if (!token) return null;

    const claims = this.getDecodedAccessToken(token);
    console.log("claims", claims);
    console.log('User Role:', claims?.role);
    return claims?.role ?? null;
  }
  getUserID() : string | null {
    const token = this.getAccessToken();
    if(!token) return null;
    const payload = JSON.parse(atob(token.split('.')[1]));
    return payload.sub;
  }
}

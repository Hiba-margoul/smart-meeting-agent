export interface TokenClaims {
  sub: string;       // Subject (usually username or email)
  exp: number;       // Expiration time
  iat: number;       // Issued at
  role?: string;     // Custom claim (example)
  userId?: number;   // Custom claim (example)
}

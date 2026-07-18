import React from 'react';
import { AuthProvider as OidcAuthProvider } from 'react-oidc-context';
import type { AuthProviderProps } from 'react-oidc-context';

/**
 * Keycloak OIDC configuration for the crud-frontend public client.
 *
 * Uses Authorization Code Flow + PKCE (the only safe flow for a public
 * browser client — no client secret is ever stored in the browser).
 *
 * The authority URL is the Keycloak realm's OIDC discovery endpoint root.
 * The library automatically fetches /.well-known/openid-configuration from it.
 *
 * For production: replace localhost:8081 with the real Keycloak hostname and
 * localhost:3000 with the real frontend hostname.
 */
const oidcConfig: AuthProviderProps = {
  authority: 'http://localhost:8081/auth/realms/crud-realm',
  client_id: 'crud-frontend',
  // window.location.origin automatically resolves to whatever port the app is
  // running on (3000 for Next.js, 5173 for Vite legacy mode).
  redirect_uri: typeof window !== 'undefined' ? window.location.origin + '/' : 'http://localhost:3000/',
  post_logout_redirect_uri: typeof window !== 'undefined' ? window.location.origin + '/' : 'http://localhost:3000/',
  scope: 'openid profile email',
  automaticSilentRenew: true,
  onSigninCallback: () => {
    window.history.replaceState({}, document.title, window.location.pathname);
  },
};

interface AuthProviderProps2 {
  children: React.ReactNode;
}

/**
 * Wraps the application in the OIDC context so any child component can call
 * `useAuth()` to get the current user, tokens, and login/logout functions.
 */
export const AuthProvider: React.FC<AuthProviderProps2> = ({ children }) => {
  return <OidcAuthProvider {...oidcConfig}>{children}</OidcAuthProvider>;
};

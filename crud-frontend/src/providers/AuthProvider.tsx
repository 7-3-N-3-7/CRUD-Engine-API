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
  redirect_uri: window.location.origin + '/',
  post_logout_redirect_uri: window.location.origin + '/',
  scope: 'openid profile email',
  // Automatically renew tokens before they expire (silent refresh via iframe)
  automaticSilentRenew: true,
  // Remove the `code` and `state` query params from the URL after login
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

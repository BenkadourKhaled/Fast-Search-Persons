import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useState,
  type ReactNode,
} from 'react';
import type { AuthUser, BackendConfig, UserRole } from '@/types';
import { FastSearchClient } from '@/lib/api';

const SESSION_KEY = 'fastsearch_session';
const CONFIG_KEY = 'fastsearch_config';

interface AuthContextValue {
  user: AuthUser | null;
  client: FastSearchClient;
  config: BackendConfig;
  login: (username: string, password: string) => Promise<void>;
  logout: () => void;
  setBaseUrl: (url: string) => void;
  hasRole: (...roles: UserRole[]) => boolean;
  isAuthenticated: boolean;
}

const defaultConfig: BackendConfig = {
  // Vide = même origine. En développement, le proxy Vite relaie /api et
  // /actuator vers http://localhost:8080.
  baseUrl: '',
};

function loadConfig(): BackendConfig {
  try {
    const raw = localStorage.getItem(CONFIG_KEY);
    if (raw) {
      const parsed = JSON.parse(raw) as Partial<BackendConfig>;
      return {
        baseUrl: typeof parsed.baseUrl === 'string' ? parsed.baseUrl : '',
      };
    }
  } catch {
    // Configuration illisible : on repart sur les valeurs sûres.
  }
  return defaultConfig;
}

function loadSession(): AuthUser | null {
  try {
    const raw = sessionStorage.getItem(SESSION_KEY);
    if (!raw) return null;
    const parsed = JSON.parse(raw) as AuthUser;
    if (
      typeof parsed.username !== 'string' ||
      typeof parsed.authHeader !== 'string' ||
      !Array.isArray(parsed.roles) ||
      !Array.isArray(parsed.groups)
    ) {
      return null;
    }
    return parsed;
  } catch {
    return null;
  }
}

function normalizeUrl(value: string): string {
  return value.trim().replace(/\/+$/, '');
}

function sameConfig(left: BackendConfig, right: BackendConfig): boolean {
  return normalizeUrl(left.baseUrl) === normalizeUrl(right.baseUrl);
}

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [config, setConfig] = useState<BackendConfig>(loadConfig);
  const [user, setUser] = useState<AuthUser | null>(loadSession);
  const [client] = useState(() => {
    const session = loadSession();
    return new FastSearchClient(loadConfig(), session?.authHeader ?? null);
  });

  const clearSession = useCallback(() => {
    client.setAuthHeader(null);
    setUser(null);
    try {
      sessionStorage.removeItem(SESSION_KEY);
    } catch {
      // sessionStorage indisponible : l'état mémoire suffit.
    }
  }, [client]);

  useEffect(() => {
    client.setUnauthorizedHandler(clearSession);
    return () => client.setUnauthorizedHandler(null);
  }, [client, clearSession]);

  const persistConfig = useCallback(
    (next: BackendConfig) => {
      const changed = !sameConfig(config, next);
      setConfig(next);
      client.setConfig(next);
      try {
        localStorage.setItem(CONFIG_KEY, JSON.stringify(next));
      } catch {
        // La configuration reste en mémoire.
      }
      if (changed) {
        // Les identifiants Basic ne sont jamais réutilisés vers un autre backend.
        clearSession();
      }
    },
    [client, config, clearSession]
  );

  const login = useCallback(
    async (username: string, password: string) => {
      const authenticated = await client.login(username, password);
      client.setAuthHeader(authenticated.authHeader);
      setUser(authenticated);
      try {
        sessionStorage.setItem(SESSION_KEY, JSON.stringify(authenticated));
      } catch {
        // Sans sessionStorage, la session reste valable dans cet onglet.
      }
    },
    [client]
  );

  const logout = useCallback(() => {
    clearSession();
  }, [clearSession]);

  const setBaseUrl = useCallback(
    (url: string) => persistConfig({ ...config, baseUrl: url.trim() }),
    [config, persistConfig]
  );

  const hasRole = useCallback(
    (...roles: UserRole[]) => {
      if (!user) return false;
      return roles.some((role) => user.roles.includes(role) || user.role === role);
    },
    [user]
  );

  return (
    <AuthContext.Provider
      value={{
        user,
        client,
        config,
        login,
        logout,
        setBaseUrl,
        hasRole,
        isAuthenticated: user !== null,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
}

// Le hook est volontairement colocalisé avec le fournisseur : une seule source
// de vérité pour le contexte et sa consommation.
// eslint-disable-next-line react-refresh/only-export-components
export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth doit être utilisé dans AuthProvider');
  return ctx;
}

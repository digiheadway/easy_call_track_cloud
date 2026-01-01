import { useState, useEffect } from 'react';
import './App.css';
import Login from './components/Auth/Login';
import Signup, { type SignupData } from './components/Auth/Signup';
import Dashboard from './components/Dashboard/Dashboard';
import api from './api/client';

type AuthView = 'login' | 'signup';

interface User {
  id: number;
  name: string;
  email: string;
  organizationName: string;
  org_id: string; // Backend returns org_id
  role: string;
}

function App() {
  const [isAuthenticated, setIsAuthenticated] = useState(false);
  const [authView, setAuthView] = useState<AuthView>('login');
  const [user, setUser] = useState<User | null>(null);
  const [loading, setLoading] = useState(true);
  const [theme, setTheme] = useState<'dark' | 'light'>(() => {
    return (localStorage.getItem('theme') as 'dark' | 'light') || 'dark';
  });

  useEffect(() => {
    document.documentElement.setAttribute('data-theme', theme);
    localStorage.setItem('theme', theme);
  }, [theme]);

  const toggleTheme = () => {
    setTheme(prev => prev === 'dark' ? 'light' : 'dark');
  };

  useEffect(() => {
    // Check for existing token
    const checkAuth = async () => {
      try {
        const token = api.getToken();
        if (token) {
          const response = await api.verifyToken();
          if (response.status && response.data.user) {
            setUser(response.data.user);
            setIsAuthenticated(true);
          } else {
            api.clearToken();
          }
        }
      } catch (error) {
        console.error('Auth check failed:', error);
        api.clearToken();
      } finally {
        setLoading(false);
      }
    };

    checkAuth();
  }, []);

  const handleLogin = async (email: string, password: string) => {
    try {
      const response = await api.login(email, password);
      if (response.status && response.data.user) {
        setUser(response.data.user);
        setIsAuthenticated(true);
      } else {
        throw new Error(response.message || 'Login failed');
      }
    } catch (error) {
      console.error('Login error:', error);
      throw error; // Propagate to Login component
    }
  };

  const handleSignup = async (data: SignupData) => {
    try {
      const response = await api.signup(data);
      if (response.status && response.data.user) {
        setUser(response.data.user);
        setIsAuthenticated(true);
      } else {
        throw new Error(response.message || 'Signup failed');
      }
    } catch (error) {
      console.error('Signup error:', error);
      throw error; // Propagate to Signup component
    }
  };

  const handleLogout = async () => {
    try {
      await api.logout();
    } catch (error) {
      console.error('Logout error:', error);
    } finally {
      api.clearToken();
      setIsAuthenticated(false);
      setUser(null);
      setAuthView('login');
    }
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center min-h-screen bg-[var(--bg-primary)]">
        <div className="text-[var(--primary)] text-xl">Loading CallCloud...</div>
      </div>
    );
  }

  if (!isAuthenticated) {
    return authView === 'login' ? (
      <Login
        onLogin={handleLogin}
        onSwitchToSignup={() => setAuthView('signup')}
      />
    ) : (
      <Signup
        onSignup={handleSignup}
        onSwitchToLogin={() => setAuthView('login')}
      />
    );
  }

  return user ? (
    <Dashboard user={user} onLogout={handleLogout} theme={theme} toggleTheme={toggleTheme} />
  ) : null;
}

export default App;

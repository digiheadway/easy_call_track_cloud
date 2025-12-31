import { useState } from 'react';
import './App.css';
import Login from './components/Auth/Login';
import Signup, { type SignupData } from './components/Auth/Signup';
import Dashboard from './components/Dashboard/Dashboard';

type AuthView = 'login' | 'signup';

interface User {
  name: string;
  email: string;
  organizationName: string;
  organizationId: string;
}

function App() {
  const [isAuthenticated, setIsAuthenticated] = useState(false);
  const [authView, setAuthView] = useState<AuthView>('login');
  const [user, setUser] = useState<User | null>(null);

  const handleLogin = (email: string, _password: string) => {
    // In a real app, this would make an API call
    // For demo purposes, we'll create a mock user
    setUser({
      name: 'Admin User',
      email: email,
      organizationName: 'Demo Organization',
      organizationId: 'DEMO01'
    });
    setIsAuthenticated(true);
  };

  const handleSignup = (data: SignupData) => {
    // In a real app, this would make an API call
    setUser({
      name: data.adminName,
      email: data.email,
      organizationName: data.organizationName,
      organizationId: data.organizationId
    });
    setIsAuthenticated(true);
  };

  const handleLogout = () => {
    setIsAuthenticated(false);
    setUser(null);
    setAuthView('login');
  };

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
    <Dashboard user={user} onLogout={handleLogout} />
  ) : null;
}

export default App;

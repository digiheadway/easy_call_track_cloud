import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider, useAuth } from './context/AuthContext';
import { PersonModalProvider } from './context/PersonModalContext';
import Layout from './components/Layout';
import LoginPage from './pages/LoginPage';
import SignupPage from './pages/SignupPage';
import Dashboard from './pages/Dashboard';
import Settings from './pages/Settings';
import CallsPage from './pages/CallsPage';
import ReportsPage from './pages/ReportsPage';
import EmployeesPage from './pages/EmployeesPage';
import ExcludedPage from './pages/ExcludedPage';
import { AudioPlayerProvider } from './context/AudioPlayerContext';
import FloatingAudioPlayer from './components/FloatingAudioPlayer';
import { Toaster } from 'sonner';

const PrivateRoute = ({ children }) => {
  const { user, loading } = useAuth();

  if (loading) return (
    <div className="min-h-screen flex items-center justify-center">
      <div className="animate-spin rounded-full h-10 w-10 border-b-2 border-blue-600"></div>
    </div>
  );

  return user ? (
    <Layout>
      {children}
    </Layout>
  ) : <Navigate to="/login" />;
};

function App() {
  return (
    <AuthProvider>
      <PersonModalProvider>
        <AudioPlayerProvider>
          <Toaster position="top-right" richColors closeButton />
          <Router basename="/dashboard">
            <Routes>
              <Route path="/login" element={<LoginPage />} />
              <Route path="/signup" element={<SignupPage />} />

              <Route path="/" element={<PrivateRoute><Dashboard /></PrivateRoute>} />
              <Route path="/reports" element={<PrivateRoute><ReportsPage /></PrivateRoute>} />
              <Route path="/calls" element={<PrivateRoute><CallsPage /></PrivateRoute>} />
              <Route path="/employees" element={<PrivateRoute><EmployeesPage /></PrivateRoute>} />
              <Route path="/excluded" element={<PrivateRoute><ExcludedPage /></PrivateRoute>} />
              <Route path="/settings" element={<PrivateRoute><Settings /></PrivateRoute>} />

              <Route path="*" element={<Navigate to="/" />} />
            </Routes>
            {/* Global Player */}
            <FloatingAudioPlayer />
          </Router>
        </AudioPlayerProvider>
      </PersonModalProvider>
    </AuthProvider>
  );
}

export default App;

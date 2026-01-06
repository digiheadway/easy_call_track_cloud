import React, { useState, useEffect } from 'react';
import { USER_CREDENTIALS, ASSIGNEES } from '../types/options';
import { LogIn } from 'lucide-react';

interface LoginProps {
  onLogin: (username: string) => void;
}

export const Login: React.FC<LoginProps> = ({ onLogin }) => {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');

  useEffect(() => {
    // Check for saved credentials
    const savedUser = localStorage.getItem('loggedInUser');
    const savedTimestamp = localStorage.getItem('loginTimestamp');
    
    if (savedUser && savedTimestamp) {
      const timestamp = parseInt(savedTimestamp);
      const now = Date.now();
      const sevenDays = 7 * 24 * 60 * 60 * 1000;
      
      if (now - timestamp < sevenDays) {
        onLogin(savedUser);
      } else {
        // Clear expired credentials
        localStorage.removeItem('loggedInUser');
        localStorage.removeItem('loginTimestamp');
      }
    }
  }, []);

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    setError('');

    const user = USER_CREDENTIALS.find(
      (u) => u.username === username && u.password === password
    );

    if (user) {
      // Save login info
      localStorage.setItem('loggedInUser', username);
      localStorage.setItem('loginTimestamp', Date.now().toString());
      onLogin(username);
    } else {
      setError('Invalid username or password');
    }
  };

  return (
    <div className="min-h-screen bg-gray-50 flex items-center justify-center p-4">
      <div className="bg-white rounded-lg shadow-lg p-6 w-full max-w-md">
        <div className="flex items-center justify-center mb-8">
          <LogIn size={32} className="text-blue-600" />
          <h1 className="text-2xl font-semibold ml-2">Lead Management</h1>
        </div>

        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Username
            </label>
            <select
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              className="w-full p-2 border rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
              required
            >
              <option value="">Select User</option>
              {ASSIGNEES.map((user) => (
                <option key={user} value={user}>
                  {user}
                </option>
              ))}
            </select>
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Password
            </label>
            <input
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              className="w-full p-2 border rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
              required
            />
          </div>

          {error && (
            <p className="text-red-600 text-sm">{error}</p>
          )}

          <button
            type="submit"
            className="w-full bg-blue-600 text-white py-2 px-4 rounded-lg hover:bg-blue-700 transition-colors"
          >
            Login
          </button>
        </form>
      </div>
    </div>
  );
};
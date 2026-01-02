import { createContext, useContext, useState, useEffect } from 'react';
import api from '../api/client';

const AuthContext = createContext();

export const AuthProvider = ({ children }) => {
    const [user, setUser] = useState(null);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        checkAuth();
    }, []);

    const checkAuth = async () => {
        const token = localStorage.getItem('cc_token');
        if (!token) {
            setLoading(false);
            return;
        }

        try {
            const res = await api.get('/auth.php?action=verify');
            setUser(res.data.user);
        } catch (err) {
            localStorage.removeItem('cc_token');
        } finally {
            setLoading(false);
        }
    };

    const login = async (email, password) => {
        try {
            const res = await api.post('/auth.php?action=login', { email, password });
            localStorage.setItem('cc_token', res.data.token);
            setUser(res.data.user);
            return res;
        } catch (err) {
            throw err;
        }
    };

    const signup = async (email, password) => {
        try {
            const res = await api.post('/auth.php?action=signup', { email, password });
            localStorage.setItem('cc_token', res.data.token);
            setUser(res.data.user);
            return res;
        } catch (err) {
            throw err;
        }
    };

    const logout = () => {
        localStorage.removeItem('cc_token');
        setUser(null);
    };

    return (
        <AuthContext.Provider value={{ user, loading, login, signup, logout }}>
            {children}
        </AuthContext.Provider>
    );
};

export const useAuth = () => useContext(AuthContext);

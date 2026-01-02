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

    const [unreadNotificationsCount, setUnreadNotificationsCount] = useState(0);

    useEffect(() => {
        if (user) {
            fetchUnreadCount();
            const interval = setInterval(fetchUnreadCount, 60000); // Check every minute
            return () => clearInterval(interval);
        }
    }, [user]);

    const fetchUnreadCount = async () => {
        try {
            const res = await api.get('/notifications.php?action=get');
            if (res.status) {
                setUnreadNotificationsCount(res.data.unread_count);
            }
        } catch (err) {
            console.error('Failed to fetch unread count', err);
        }
    };

    return (
        <AuthContext.Provider value={{ user, loading, login, signup, logout, unreadNotificationsCount, fetchUnreadCount }}>
            {children}
        </AuthContext.Provider>
    );
};

export const useAuth = () => useContext(AuthContext);

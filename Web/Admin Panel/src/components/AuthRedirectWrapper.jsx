import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { useEffect } from 'react';

// Wrapper component to handle post-action redirects
export function withAuthRedirect(Component, redirectTo = '/') {
    return function AuthRedirectWrapper(props) {
        const { user } = useAuth();
        const navigate = useNavigate();

        useEffect(() => {
            // If user is already logged in, redirect them to dashboard (or wherever)
            if (user) {
                navigate(redirectTo);
            }
        }, [user, navigate]);

        return <Component {...props} />;
    };
}

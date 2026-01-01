import { useState, useEffect } from 'react';
import './Auth.css';

interface SignupProps {
    onSignup: (data: SignupData) => Promise<void>;
    onSwitchToLogin: () => void;
}

export interface SignupData {
    organizationName: string;
    organizationId: string;
    adminName: string;
    email: string;
    password: string;
}

export default function Signup({ onSignup, onSwitchToLogin }: SignupProps) {
    const [formData, setFormData] = useState<SignupData>({
        organizationName: '',
        organizationId: '',
        adminName: '',
        email: '',
        password: '',
    });
    const [confirmPassword, setConfirmPassword] = useState('');
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);

    useEffect(() => {
        // Generate Org ID automatically on mount
        const characters = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789';
        let result = '';
        const length = 6;
        for (let i = 0; i < length; i++) {
            result += characters.charAt(Math.floor(Math.random() * characters.length));
        }
        setFormData(prev => ({
            ...prev,
            organizationId: result,
            organizationName: 'Default Organization',
            adminName: 'Admin User'
        }));
    }, []);

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        setError(null);

        if (formData.password !== confirmPassword) {
            setError('Passwords do not match!');
            return;
        }

        setLoading(true);

        try {
            // Update organization name based on email if we want it to be slightly more unique
            const defaultOrgName = formData.email.split('@')[0] + "'s Org";
            const finalData = {
                ...formData,
                organizationName: formData.organizationName === 'Default Organization' ? defaultOrgName : formData.organizationName,
                adminName: formData.adminName === 'Admin User' ? formData.email.split('@')[0] : formData.adminName
            };
            await onSignup(finalData);
        } catch (err: any) {
            setError(err.message || 'Failed to create account');
            setLoading(false);
        }
    };

    const handleChange = (field: keyof SignupData, value: string) => {
        setFormData({ ...formData, [field]: value });
    };

    return (
        <div className="auth-container">
            <div className="auth-background"></div>

            <div className="auth-card glass fade-in">
                <div className="auth-header">
                    <div className="brand-icon">
                        <svg width="48" height="48" viewBox="0 0 48 48" fill="none">
                            <circle cx="24" cy="24" r="20" fill="var(--primary)" fillOpacity="0.2" />
                            <path d="M24 14C18.48 14 14 18.48 14 24C14 29.52 18.48 34 24 34C29.52 34 34 29.52 34 24C34 18.48 29.52 14 24 14ZM24 30C20.69 30 18 27.31 18 24C18 20.69 20.69 18 24 18C27.31 18 30 20.69 30 24C30 27.31 27.31 30 24 30Z" fill="var(--primary)" />
                        </svg>
                    </div>
                    <h1 className="auth-title">Create Account</h1>
                    <p className="auth-subtitle">Join CallCloud today</p>
                </div>

                {error && (
                    <div className="bg-red-500/10 border border-red-500/20 text-red-500 px-4 py-3 rounded-lg mb-6 text-sm">
                        {error}
                    </div>
                )}

                <form onSubmit={handleSubmit} className="auth-form">
                    <div className="form-group">
                        <label className="form-label">Email Address</label>
                        <input
                            type="email"
                            className="form-input"
                            placeholder="admin@example.com"
                            value={formData.email}
                            onChange={(e) => handleChange('email', e.target.value)}
                            required
                        />
                    </div>

                    <div className="form-group">
                        <label className="form-label">Password</label>
                        <input
                            type="password"
                            className="form-input"
                            placeholder="••••••••"
                            value={formData.password}
                            onChange={(e) => handleChange('password', e.target.value)}
                            required
                            minLength={8}
                        />
                    </div>

                    <div className="form-group">
                        <label className="form-label">Confirm Password</label>
                        <input
                            type="password"
                            className="form-input"
                            placeholder="••••••••"
                            value={confirmPassword}
                            onChange={(e) => setConfirmPassword(e.target.value)}
                            required
                            minLength={8}
                        />
                    </div>

                    <button type="submit" className="btn btn-primary w-full" disabled={loading}>
                        {loading ? (
                            <>
                                <div className="spinner"></div>
                                Creating Account...
                            </>
                        ) : (
                            'Create Account'
                        )}
                    </button>
                </form>

                <div className="auth-divider">
                    <span>Already have an account?</span>
                </div>

                <button onClick={onSwitchToLogin} className="btn btn-secondary w-full">
                    Sign In
                </button>
            </div>
        </div>
    );
}

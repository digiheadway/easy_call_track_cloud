import { useState } from 'react';
import './Auth.css';

interface SignupProps {
    onSignup: (data: SignupData) => void;
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

    // Generate unique 6-character organization ID
    const generateOrgId = () => {
        const chars = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789';
        let result = '';
        for (let i = 0; i < 6; i++) {
            result += chars.charAt(Math.floor(Math.random() * chars.length));
        }
        setFormData({ ...formData, organizationId: result });
    };

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();

        if (formData.password !== confirmPassword) {
            alert('Passwords do not match!');
            return;
        }

        if (formData.organizationId.length !== 6) {
            alert('Organization ID must be 6 characters!');
            return;
        }

        setLoading(true);

        // Simulate API call
        setTimeout(() => {
            onSignup(formData);
            setLoading(false);
        }, 1000);
    };

    const handleChange = (field: keyof SignupData, value: string) => {
        setFormData({ ...formData, [field]: value });
    };

    return (
        <div className="auth-container">
            <div className="auth-background">
                <div className="gradient-orb orb-1"></div>
                <div className="gradient-orb orb-2"></div>
                <div className="gradient-orb orb-3"></div>
            </div>

            <div className="auth-card glass fade-in">
                <div className="auth-header">
                    <div className="brand-icon">
                        <svg width="48" height="48" viewBox="0 0 48 48" fill="none">
                            <circle cx="24" cy="24" r="20" fill="url(#gradient)" />
                            <path d="M24 14C18.48 14 14 18.48 14 24C14 29.52 18.48 34 24 34C29.52 34 34 29.52 34 24C34 18.48 29.52 14 24 14ZM24 30C20.69 30 18 27.31 18 24C18 20.69 20.69 18 24 18C27.31 18 30 20.69 30 24C30 27.31 27.31 30 24 30Z" fill="white" />
                            <defs>
                                <linearGradient id="gradient" x1="4" y1="4" x2="44" y2="44">
                                    <stop offset="0%" stopColor="#6366f1" />
                                    <stop offset="100%" stopColor="#8b5cf6" />
                                </linearGradient>
                            </defs>
                        </svg>
                    </div>
                    <h1 className="auth-title gradient-text">Create Account</h1>
                    <p className="auth-subtitle">Set up your CallCloud organization</p>
                </div>

                <form onSubmit={handleSubmit} className="auth-form">
                    <div className="form-group">
                        <label className="form-label">Organization Name</label>
                        <input
                            type="text"
                            className="form-input"
                            placeholder="Acme Corporation"
                            value={formData.organizationName}
                            onChange={(e) => handleChange('organizationName', e.target.value)}
                            required
                        />
                    </div>

                    <div className="form-group">
                        <label className="form-label">Organization ID (6 characters)</label>
                        <div className="input-group">
                            <input
                                type="text"
                                className="form-input"
                                placeholder="ABC123"
                                value={formData.organizationId}
                                onChange={(e) => handleChange('organizationId', e.target.value.toUpperCase().slice(0, 6))}
                                maxLength={6}
                                required
                            />
                            <button type="button" onClick={generateOrgId} className="btn btn-secondary">
                                Generate
                            </button>
                        </div>
                    </div>

                    <div className="form-group">
                        <label className="form-label">Admin Name</label>
                        <input
                            type="text"
                            className="form-input"
                            placeholder="John Doe"
                            value={formData.adminName}
                            onChange={(e) => handleChange('adminName', e.target.value)}
                            required
                        />
                    </div>

                    <div className="form-group">
                        <label className="form-label">Email Address</label>
                        <input
                            type="email"
                            className="form-input"
                            placeholder="admin@acme.com"
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

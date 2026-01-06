import { useState, useEffect } from 'react';
import { useAuth } from '../context/AuthContext';
import { useNavigate, Link } from 'react-router-dom';
import { Mail, Lock, ArrowLeft } from 'lucide-react';

export default function SignupPage() {
    const [email, setEmail] = useState('');
    const [password, setPassword] = useState('');
    const [confirmPassword, setConfirmPassword] = useState('');
    const [error, setError] = useState('');
    const [loading, setLoading] = useState(false);
    const { signup, user } = useAuth();
    const navigate = useNavigate();

    // Redirect if already logged in
    useEffect(() => {
        if (user) {
            navigate('/');
        }
    }, [user]);

    const handleSubmit = async (e) => {
        e.preventDefault();
        if (password !== confirmPassword) {
            return setError('Passwords do not match');
        }
        setError('');
        setLoading(true);
        try {
            await signup(email, password);
            navigate('/');
        } catch (err) {
            setError(err.message || 'Failed to create account');
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="min-h-screen flex flex-col relative overflow-hidden bg-gray-50 dark:bg-dark-bg text-gray-900 dark:text-white font-sans">
            {/* Background Elements */}
            <div className="absolute top-0 left-0 w-full h-full overflow-hidden -z-10 pointer-events-none">
                <div className="absolute bottom-0 left-1/4 w-96 h-96 bg-brand-500/10 rounded-full blur-[128px]"></div>
                <div className="absolute top-0 right-1/4 w-96 h-96 bg-purple-500/10 rounded-full blur-[128px]"></div>
            </div>

            {/* Navbar */}
            <nav className="w-full p-6 z-20">
                <div className="max-w-7xl mx-auto flex justify-between items-center">
                    <a href="/" className="flex items-center gap-2 group">
                        <div className="w-8 h-8 rounded-lg bg-gray-100 dark:bg-white/5 border border-gray-200 dark:border-white/10 flex items-center justify-center group-hover:bg-gray-200 dark:group-hover:bg-white/10 transition-colors">
                            <ArrowLeft className="text-gray-600 dark:text-white w-4 h-4" />
                        </div>
                        <span className="font-medium text-gray-600 dark:text-gray-300 hover:text-gray-900 dark:hover:text-white transition-colors">Back to Home</span>
                    </a>
                    <div className="hidden sm:block text-sm text-gray-500 dark:text-gray-400">
                        Already have an account? <Link to="/login" className="text-brand-600 dark:text-brand-400 hover:text-brand-500 dark:hover:text-brand-300 font-medium ml-1">Log in</Link>
                    </div>
                </div>
            </nav>

            <main className="flex-grow flex items-center justify-center px-4 py-10 relative z-10">
                <div className="w-full max-w-md p-8 sm:p-10 rounded-3xl shadow-2xl bg-white dark:bg-[#111827]/70 backdrop-blur-md border border-gray-100 dark:border-white/10">

                    <div className="text-center mb-8">
                        <h1 className="text-3xl font-bold mb-2 text-gray-900 dark:text-white">Create Account</h1>
                        <p className="text-gray-500 dark:text-gray-400">Start your 3-day free trial today</p>
                    </div>

                    {error && (
                        <div className="bg-red-500/10 text-red-200 p-3 rounded-xl text-sm mb-6 border border-red-500/20">
                            {error}
                        </div>
                    )}

                    <form onSubmit={handleSubmit} className="space-y-4">

                        <div>
                            <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">Email Address</label>
                            <div className="relative">
                                <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                                    <Mail className="text-gray-400 dark:text-gray-500 w-5 h-5" />
                                </div>
                                <input
                                    type="email"
                                    required
                                    className="w-full rounded-xl py-3 pl-10 pr-4 placeholder-gray-400 dark:placeholder-gray-500 bg-gray-50 dark:bg-white/5 border border-gray-200 dark:border-white/10 text-gray-900 dark:text-white focus:bg-white dark:focus:bg-white/10 focus:border-brand-500 focus:outline-none focus:ring-1 focus:ring-brand-500/50 transition-all"
                                    placeholder="name@company.com"
                                    value={email}
                                    onChange={(e) => setEmail(e.target.value)}
                                />
                            </div>
                        </div>

                        <div>
                            <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">Password</label>
                            <div className="relative">
                                <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                                    <Lock className="text-gray-400 dark:text-gray-500 w-5 h-5" />
                                </div>
                                <input
                                    type="password"
                                    required
                                    className="w-full rounded-xl py-3 pl-10 pr-4 placeholder-gray-400 dark:placeholder-gray-500 bg-gray-50 dark:bg-white/5 border border-gray-200 dark:border-white/10 text-gray-900 dark:text-white focus:bg-white dark:focus:bg-white/10 focus:border-brand-500 focus:outline-none focus:ring-1 focus:ring-brand-500/50 transition-all"
                                    placeholder="Create a strong password"
                                    value={password}
                                    onChange={(e) => setPassword(e.target.value)}
                                />
                            </div>
                        </div>

                        <div>
                            <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">Confirm Password</label>
                            <div className="relative">
                                <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                                    <Lock className="text-gray-400 dark:text-gray-500 w-5 h-5" />
                                </div>
                                <input
                                    type="password"
                                    required
                                    className="w-full rounded-xl py-3 pl-10 pr-4 placeholder-gray-400 dark:placeholder-gray-500 bg-gray-50 dark:bg-white/5 border border-gray-200 dark:border-white/10 text-gray-900 dark:text-white focus:bg-white dark:focus:bg-white/10 focus:border-brand-500 focus:outline-none focus:ring-1 focus:ring-brand-500/50 transition-all"
                                    placeholder="Confirm your password"
                                    value={confirmPassword}
                                    onChange={(e) => setConfirmPassword(e.target.value)}
                                />
                            </div>
                        </div>

                        <div className="flex items-start gap-3 mt-2">
                            <input type="checkbox" id="terms" className="mt-1 w-4 h-4 rounded border-gray-300 dark:border-gray-600 text-brand-600 dark:text-brand-500 focus:ring-brand-500 bg-gray-50 dark:bg-white/5" />
                            <label htmlFor="terms" className="text-sm text-gray-500 dark:text-gray-400">
                                I agree to the <a href="#" className="text-brand-600 dark:text-brand-400 hover:text-brand-500 dark:hover:text-brand-300">Terms of Service</a> and <a href="#" className="text-brand-600 dark:text-brand-400 hover:text-brand-500 dark:hover:text-brand-300">Privacy Policy</a>
                            </label>
                        </div>

                        <button
                            type="submit"
                            disabled={loading}
                            className="w-full bg-brand-600 hover:bg-brand-500 text-white font-bold py-3.5 rounded-xl transition-all shadow-lg shadow-brand-500/25 mt-4 disabled:opacity-50 disabled:cursor-not-allowed"
                        >
                            {loading ? 'Creating account...' : 'Create Account'}
                        </button>
                    </form>

                    <div className="mt-8 text-center sm:hidden">
                        <p className="text-gray-500 dark:text-gray-400 text-sm">
                            Already have an account?{' '}
                            <Link to="/login" className="text-brand-600 dark:text-brand-400 font-semibold hover:text-brand-500 dark:hover:text-brand-300 transition-colors">Log in</Link>
                        </p>
                    </div>
                </div>
            </main>
        </div>
    );
}

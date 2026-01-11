import { useState, useEffect } from 'react';
import { useAuth } from '../context/AuthContext';
import { useNavigate, Link } from 'react-router-dom';
import { Mail, Lock, ArrowRight, ArrowLeft, PhoneCall } from 'lucide-react';

export default function LoginPage() {
    const [email, setEmail] = useState('');
    const [password, setPassword] = useState('');
    const [error, setError] = useState('');
    const [loading, setLoading] = useState(false);
    const { login, user } = useAuth();
    const navigate = useNavigate();

    // Redirect if already logged in
    useEffect(() => {
        if (user) {
            navigate('/');
        }
    }, [user]);

    const handleSubmit = async (e) => {
        e.preventDefault();
        setError('');
        setLoading(true);
        try {
            await login(email, password);
            navigate('/');
        } catch (err) {
            setError(err.message || 'Failed to login');
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="min-h-screen flex flex-col relative overflow-hidden bg-gray-50  text-gray-900  font-sans">
            {/* Background Glows */}
            <div className="absolute top-0 left-0 w-full h-full overflow-hidden -z-10 pointer-events-none">
                <div className="absolute top-0 left-1/4 w-96 h-96 bg-brand-500/20 rounded-full blur-[128px]"></div>
                <div className="absolute bottom-0 right-1/4 w-96 h-96 bg-purple-500/20 rounded-full blur-[128px]"></div>
            </div>

            {/* Navbar */}
            <nav className="absolute top-0 w-full p-6 z-20">
                <div className="max-w-7xl mx-auto flex justify-between items-center">
                    <a href="/" className="flex items-center gap-2 group">
                        <div className="w-8 h-8 rounded-lg bg-gray-100  border border-gray-200  flex items-center justify-center group-hover:bg-gray-200  transition-colors">
                            <ArrowLeft className="text-gray-600  w-4 h-4" />
                        </div>
                        <span className="font-medium text-gray-600  hover:text-gray-900  transition-colors">Back to Home</span>
                    </a>
                </div>
            </nav>

            <main className="flex-grow flex items-center justify-center px-4 py-20 relative z-10">
                <div className="w-full max-w-md p-8 sm:p-10 rounded-3xl shadow-2xl relative bg-white  backdrop-blur-md border border-gray-100 ">
                    <div className="absolute -top-12 left-1/2 transform -translate-x-1/2 bg-white  p-2 rounded-2xl border border-gray-100  shadow-xl">
                        <div className="w-16 h-16 rounded-xl bg-gradient-to-br from-brand-500 to-purple-600 flex items-center justify-center">
                            <PhoneCall className="text-white w-8 h-8" />
                        </div>
                    </div>

                    <div className="text-center mt-8 mb-8">
                        <h1 className="text-3xl font-bold mb-2 text-gray-900 ">Welcome Back</h1>
                        <p className="text-gray-500 ">Sign in to access your dashboard</p>
                    </div>

                    {error && (
                        <div className="bg-red-500/10 text-red-200 p-3 rounded-xl text-sm mb-6 border border-red-500/20">
                            {error}
                        </div>
                    )}

                    <form onSubmit={handleSubmit} className="space-y-5">
                        <div>
                            <label className="block text-sm font-medium text-gray-700  mb-2">Email Address</label>
                            <div className="relative">
                                <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                                    <Mail className="text-gray-400  w-5 h-5" />
                                </div>
                                <input
                                    type="email"
                                    required
                                    className="w-full rounded-xl py-3 pl-10 pr-4 placeholder-gray-400  bg-gray-50  border border-gray-200  text-gray-900  focus:bg-white  focus:border-brand-500 focus:outline-none focus:ring-1 focus:ring-brand-500/50 transition-all"
                                    placeholder="name@company.com"
                                    value={email}
                                    onChange={(e) => setEmail(e.target.value)}
                                />
                            </div>
                        </div>

                        <div>
                            <div className="flex justify-between items-center mb-2">
                                <label className="block text-sm font-medium text-gray-700 ">Password</label>
                                <a href="#" className="text-sm text-brand-600  hover:text-brand-500  transition-colors">Forgot password?</a>
                            </div>
                            <div className="relative">
                                <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                                    <Lock className="text-gray-400  w-5 h-5" />
                                </div>
                                <input
                                    type="password"
                                    required
                                    className="w-full rounded-xl py-3 pl-10 pr-4 placeholder-gray-400  bg-gray-50  border border-gray-200  text-gray-900  focus:bg-white  focus:border-brand-500 focus:outline-none focus:ring-1 focus:ring-brand-500/50 transition-all"
                                    placeholder="••••••••"
                                    value={password}
                                    onChange={(e) => setPassword(e.target.value)}
                                />
                            </div>
                        </div>

                        <button
                            type="submit"
                            disabled={loading}
                            className="w-full bg-brand-600 hover:bg-brand-500 text-white font-bold py-3.5 rounded-xl transition-all shadow-lg shadow-brand-500/25 mt-2 flex items-center justify-center gap-2 disabled:opacity-50 disabled:cursor-not-allowed"
                        >
                            {loading ? 'Signing in...' : (
                                <>
                                    Sign In <ArrowRight className="w-4 h-4" />
                                </>
                            )}
                        </button>
                    </form>

                    <div className="mt-8 pt-6 border-t border-gray-100  text-center">
                        <p className="text-gray-500  text-sm">
                            Don't have an account?{' '}
                            <Link to="/signup" className="text-brand-600  font-semibold hover:text-brand-500  transition-colors">
                                Sign up for free
                            </Link>
                        </p>
                    </div>
                </div>
            </main>

            <footer className="absolute bottom-4 w-full text-center text-gray-500  text-xs">
                &copy; 2024 MiniClick. Secure Login.
            </footer>
        </div>
    );
}

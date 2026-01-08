import { useState, useEffect } from 'react';
import api from '../api/client';
import { useAuth } from '../context/AuthContext';
import UpiPaymentModal from '../components/UpiPaymentModal';
import {
    Users,
    HardDrive,
    Calendar,
    AlertCircle,
    Plus,
    Minus,
    Tag,
    ChevronRight,
    Loader2,
    CheckCircle,
    ShoppingBag,
    ShoppingCart,
    Sparkles,
    Trash2,
    X,
    CreditCard
} from 'lucide-react';

export default function PlansPage() {
    const { user } = useAuth();
    const [usage, setUsage] = useState(null);
    const [usageLoading, setUsageLoading] = useState(true);
    const [unitPrices, setUnitPrices] = useState({ price_per_user: 149, price_per_gb: 99 });

    // Duration / Discount State
    const [duration, setDuration] = useState(1);
    const durations = [
        { months: 1, discount: 0, label: '30 Days' },
        { months: 3, discount: 20, label: '90 Days', badge: 'Best Value' }
    ];

    // Cart Settings (Incremental additions)
    const [addUsers, setAddUsers] = useState(() => {
        const saved = localStorage.getItem('cc_cart_users');
        return saved ? parseInt(saved) : 0;
    });
    const [addStorage, setAddStorage] = useState(() => {
        const saved = localStorage.getItem('cc_cart_storage');
        return saved ? parseInt(saved) : 0;
    });
    const [isRenewing, setIsRenewing] = useState(() => {
        return localStorage.getItem('cc_cart_renewing') === 'true';
    });
    const [showCart, setShowCart] = useState(() => {
        // Auto-show cart if there are saved items
        const savedUsers = parseInt(localStorage.getItem('cc_cart_users') || '0');
        const savedStorage = parseInt(localStorage.getItem('cc_cart_storage') || '0');
        const savedRenewing = localStorage.getItem('cc_cart_renewing') === 'true';
        return savedUsers !== 0 || savedStorage !== 0 || savedRenewing;
    });

    // Persist cart to localStorage
    useEffect(() => {
        localStorage.setItem('cc_cart_users', addUsers.toString());
        localStorage.setItem('cc_cart_storage', addStorage.toString());
        localStorage.setItem('cc_cart_renewing', isRenewing.toString());
    }, [addUsers, addStorage, isRenewing]);

    // Promo / Checkout State
    const [promoCode, setPromoCode] = useState('');
    const [appliedPromo, setAppliedPromo] = useState(null);
    const [promoError, setPromoError] = useState('');
    const [checkingPromo, setCheckingPromo] = useState(false);
    const [checkoutLoading, setCheckoutLoading] = useState(false);
    const [success, setSuccess] = useState('');
    const [error, setError] = useState('');

    // UPI Payment Modal state
    const [showUpiModal, setShowUpiModal] = useState(false);
    const [pendingPaymentAmount, setPendingPaymentAmount] = useState(0);
    const [pendingOrderDetails, setPendingOrderDetails] = useState(null);

    useEffect(() => {
        fetchUsage();
        fetchUnitPrices();
    }, []);

    const fetchUsage = async () => {
        setUsageLoading(true);
        try {
            const res = await api.get('/billing.php?action=get_usage');
            setUsage(res.data);
        } catch (err) {
            console.error(err);
        } finally {
            setUsageLoading(false);
        }
    };

    const fetchUnitPrices = async () => {
        try {
            const res = await api.get('/billing.php?action=get_plans');
            setUnitPrices(res.data);
        } catch (err) {
            console.error(err);
        }
    };

    const handleApplyPromo = async () => {
        if (!promoCode) return;
        setCheckingPromo(true);
        setPromoError('');
        try {
            const res = await api.get(`/billing.php?action=validate_promo&code=${promoCode}`);
            setAppliedPromo(res.data);
            setSuccess('Promo code applied successfully!');
            setTimeout(() => setSuccess(''), 3000);
        } catch (err) {
            setPromoError(err.message || 'Invalid promo code');
            setAppliedPromo(null);
        } finally {
            setCheckingPromo(false);
        }
    };

    const handleCheckout = async () => {
        const total = calculateTotal();
        setCheckoutLoading(true);
        setError('');

        try {
            const daysLeft = getDaysRemaining();
            const payload = {
                action: 'checkout',
                user_count: Number(usage?.allowed_users || 0) + addUsers,
                storage_gb: Number(usage?.allowed_storage_gb || 0) + addStorage,
                duration_months: isRenewing ? duration : 0,
                is_renewing: isRenewing,
                remaining_days: daysLeft > 0 ? daysLeft : 0,
                add_users: addUsers,
                add_storage: addStorage,
                promo_code: appliedPromo?.code || ''
            };

            console.log('Checkout Payload:', payload);
            const res = await api.post('/billing.php?action=checkout', payload);

            if (res.status) {
                // Check if payment is required
                if (!res.data.payment_required) {
                    // No payment needed (free update)
                    setSuccess('Subscription updated successfully!');
                    // Reset cart
                    setAddUsers(0);
                    setAddStorage(0);
                    setIsRenewing(false);
                    setAppliedPromo(null);
                    setPromoCode('');
                    // Refresh usage
                    fetchUsage();
                    setTimeout(() => setSuccess(''), 3000);
                } else {
                    // Payment required - show UPI modal
                    setPendingPaymentAmount(res.data.amount);
                    setPendingOrderDetails(payload);
                    setShowUpiModal(true);
                }
            } else {
                setError(res.message || 'Checkout failed');
            }
        } catch (err) {
            console.error('Checkout error:', err);
            setError(err.response?.data?.message || err.message || 'Checkout failed');
        } finally {
            setCheckoutLoading(false);
        }
    };

    // Handle Manual UPI Payment Success
    const handleManualPaymentSuccess = async () => {
        if (!pendingOrderDetails) return;

        setCheckoutLoading(true); // Reuse checkout loading state for verification UI

        try {
            const payload = {
                action: 'confirm_manual_payment',
                ...pendingOrderDetails,
                is_renewing: isRenewing,
                remaining_days: getDaysRemaining() > 0 ? getDaysRemaining() : 0,
            };

            const res = await api.post('/billing.php', payload);

            if (res.status) {
                setSuccess('Payment confirmed! Subscription updated.');
                // Reset cart
                setAddUsers(0);
                setAddStorage(0);
                setIsRenewing(false);
                setAppliedPromo(null);
                setPromoCode('');
                // Refresh usage
                fetchUsage();
                setTimeout(() => setSuccess(''), 5000);
            } else {
                setError(res.message || 'Failed to update plan. Please contact support.');
            }
        } catch (err) {
            console.error('Manual confirmation error:', err);
            setError('Failed to update plan. Please contact support.');
        } finally {
            setCheckoutLoading(false);
            setShowUpiModal(false);
            setPendingOrderDetails(null);
        }
    };



    const getDaysRemaining = () => {
        if (!usage?.plan_expiry) return null;
        const expiry = new Date(usage.plan_expiry);
        const today = new Date();
        const diffTime = expiry - today;
        return Math.ceil(diffTime / (1000 * 60 * 60 * 24));
    };

    const calculateSubtotal = () => {
        const users = parseFloat(usage?.allowed_users ?? 0);
        const storage = parseFloat(usage?.allowed_storage_gb ?? 0);
        const perUser = parseFloat(unitPrices?.price_per_user ?? 149);
        const perGb = parseFloat(unitPrices?.price_per_gb ?? 99);
        const addU = parseFloat(addUsers ?? 0);
        const addS = parseFloat(addStorage ?? 0);
        const daysLeft = getDaysRemaining();

        let subtotal = 0;

        if (isRenewing) {
            // RENEWAL MODE
            const totalUsers = Math.max(1, users + addU); // Min 1 user
            const totalStorage = Math.max(0, storage + addS);

            // 1. Charge for ALL resources for the NEW term
            const renewalCost = (totalUsers * perUser + totalStorage * perGb) * (duration || 1);
            subtotal += renewalCost;

            // 2. If adding resources AND plan is still active, also charge prorated for additions from today → current expiry
            if (daysLeft && daysLeft > 0 && (addU > 0 || addS > 0)) {
                const proratedFactor = daysLeft / 30;
                const additionsProratedCost = (Math.max(0, addU) * perUser + Math.max(0, addS) * perGb) * proratedFactor;
                subtotal += additionsProratedCost;
            }
        } else {
            // ADD-ON MODE: Mid-cycle addition only
            if (daysLeft && daysLeft > 0) {
                const proratedFactor = daysLeft / 30;
                subtotal = (addU * perUser + addS * perGb) * proratedFactor;
            } else {
                // Plan expired: charge full month for additions
                subtotal = (addU * perUser + addS * perGb) * (duration || 1);
            }
        }

        return isNaN(subtotal) ? 0 : subtotal;
    };

    const calculateTotal = () => {
        const subtotal = calculateSubtotal();
        const durationDiscount = durations.find(d => d.months === duration)?.discount || 0;
        let total = subtotal * (1 - durationDiscount / 100);

        if (appliedPromo) {
            total = total - (total * (appliedPromo.discount_percent / 100));
        }
        return total;
    };

    if (usageLoading) {
        return (
            <div className="flex items-center justify-center min-h-[400px]">
                <Loader2 className="animate-spin text-blue-600" size={40} />
            </div>
        );
    }

    const daysRemaining = getDaysRemaining();

    return (
        <div className="max-w-7xl mx-auto space-y-8 animate-in fade-in duration-500">
            <header className="flex flex-col md:flex-row md:items-center justify-between gap-6">
                <div className="space-y-1">
                    <div className="flex items-center gap-3">
                        <h1 className="text-3xl font-black tracking-tight text-gray-900 dark:text-white">Subscription & Plans</h1>
                        <span className="px-2.5 py-1 bg-emerald-100 dark:bg-emerald-900/30 text-emerald-700 dark:text-emerald-400 text-[9px] font-black rounded-lg uppercase tracking-widest flex items-center gap-1 border border-emerald-200 dark:border-emerald-800">
                            <CheckCircle size={10} />
                            Active
                        </span>
                    </div>
                    <p className="text-gray-500 dark:text-gray-400 font-medium">Powering your business with unified communication.</p>
                </div>

                {/* Cart Toggle Button */}
                <button
                    onClick={() => setShowCart(!showCart)}
                    className={`relative flex items-center gap-2 px-4 py-3 rounded-2xl font-bold transition-all ${showCart
                        ? 'bg-blue-600 text-white shadow-lg shadow-blue-500/30'
                        : 'bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700 text-gray-700 dark:text-gray-200 hover:border-blue-300 dark:hover:border-blue-700 hover:shadow-md'
                        }`}
                >
                    <ShoppingCart size={20} />
                    {(() => {
                        const count = (addUsers !== 0 ? 1 : 0) + (addStorage !== 0 ? 1 : 0) + (isRenewing ? 1 : 0);
                        if (count > 0) return <span className="text-sm">{count} Items in Cart</span>;
                        return showCart ? <span className="text-sm">Hide Cart</span> : null;
                    })()}
                    {(addUsers !== 0 || addStorage !== 0 || isRenewing) && (
                        <span className="absolute -top-2 -right-2 w-5 h-5 bg-red-500 text-white text-[10px] font-black rounded-full flex items-center justify-center">
                            {(addUsers !== 0 ? 1 : 0) + (addStorage !== 0 ? 1 : 0) + (isRenewing ? 1 : 0)}
                        </span>
                    )}
                </button>
            </header>

            <div className="space-y-8 animate-in slide-in-from-bottom-4 duration-500">
                {/* Current Status row */}
                <div className="space-y-6">
                    <div className="grid grid-cols-1 md:grid-cols-3 gap-4">

                        <div className="p-4 bg-white dark:bg-gray-800 rounded-2xl border border-gray-200 dark:border-gray-700 shadow-sm flex items-center justify-between group hover:border-blue-300 dark:hover:border-blue-700 transition-all">
                            <div className="flex items-center gap-3">
                                <div className="w-10 h-10 bg-blue-50 dark:bg-blue-600/10 text-blue-600 dark:text-blue-400 rounded-xl flex items-center justify-center group-hover:scale-110 transition-transform">
                                    <Users size={20} />
                                </div>
                                <div>
                                    <p className="text-[10px] font-bold text-gray-400 dark:text-gray-500 uppercase tracking-widest">Current Slots</p>
                                    <div className="flex items-center gap-2">
                                        <p className="text-lg font-black text-gray-900 dark:text-white">{usage.allowed_users} Employees</p>
                                        {addUsers > 0 && (
                                            <span className="text-xs font-black text-blue-600 dark:text-blue-400 bg-blue-50 dark:bg-blue-900/30 px-2 py-0.5 rounded-full flex items-center gap-0.5">
                                                <Plus size={10} />{addUsers}
                                            </span>
                                        )}
                                    </div>
                                </div>
                            </div>
                            {addUsers === 0 && (
                                <button
                                    onClick={() => {
                                        setAddUsers(prev => prev + 1);
                                        setShowCart(true);
                                    }}
                                    className="h-8 px-3 bg-blue-600 text-white rounded-lg font-black text-[9px] uppercase tracking-wider hover:bg-blue-700 active:scale-95 transition-all shadow-lg shadow-blue-500/20 flex items-center gap-1.5"
                                >
                                    <Plus size={12} />
                                    +1 Slot
                                </button>
                            )}
                        </div>

                        <div className="p-4 bg-white dark:bg-gray-800 rounded-2xl border border-gray-200 dark:border-gray-700 shadow-sm flex items-center justify-between group hover:border-purple-300 dark:hover:border-purple-700 transition-all">
                            <div className="flex items-center gap-3">
                                <div className="w-10 h-10 bg-purple-50 dark:bg-purple-600/10 text-purple-600 dark:text-purple-400 rounded-xl flex items-center justify-center group-hover:scale-110 transition-transform">
                                    <HardDrive size={20} />
                                </div>
                                <div>
                                    <p className="text-[10px] font-bold text-gray-400 dark:text-gray-500 uppercase tracking-widest">Active Storage</p>
                                    <div className="flex items-center gap-2">
                                        <p className="text-lg font-black text-gray-900 dark:text-white">{usage.allowed_storage_gb > 0 ? `${usage.allowed_storage_gb} GB` : '0 GB'}</p>
                                        {addStorage > 0 && (
                                            <span className="text-xs font-black text-purple-600 dark:text-purple-400 bg-purple-50 dark:bg-purple-900/30 px-2 py-0.5 rounded-full flex items-center gap-0.5">
                                                <Plus size={10} />{addStorage} GB
                                            </span>
                                        )}
                                    </div>
                                </div>
                            </div>
                            {addStorage === 0 && (
                                <button
                                    onClick={() => {
                                        setAddStorage(prev => prev + 2);
                                        setShowCart(true);
                                    }}
                                    className="h-8 px-3 bg-purple-600 text-white rounded-lg font-black text-[9px] uppercase tracking-wider hover:bg-purple-700 active:scale-95 transition-all shadow-lg shadow-purple-500/20 flex items-center gap-1.5"
                                >
                                    <Plus size={12} />
                                    +2 GB
                                </button>
                            )}
                        </div>

                        <div className="p-4 bg-white dark:bg-gray-800 rounded-2xl border border-gray-200 dark:border-gray-700 shadow-sm flex items-center justify-between group hover:border-green-400 dark:hover:border-green-700 transition-all">
                            <div className="flex items-center gap-3">
                                <div className="w-10 h-10 bg-green-50 dark:bg-green-600/10 text-green-600 dark:text-green-500 rounded-xl flex items-center justify-center group-hover:scale-110 transition-transform">
                                    <Calendar size={20} />
                                </div>
                                <div>
                                    <p className="text-[10px] font-bold text-gray-400 dark:text-gray-500 uppercase tracking-widest">Renews On</p>
                                    <p className="text-lg font-black text-gray-900 dark:text-white font-mono tracking-tighter">
                                        {usage.plan_expiry ? new Date(usage.plan_expiry).toLocaleDateString(undefined, { day: 'numeric', month: 'short', year: 'numeric' }) : 'NA'}
                                    </p>
                                </div>
                            </div>
                            {!isRenewing && (
                                <button
                                    onClick={() => {
                                        setIsRenewing(true);
                                        if (daysRemaining === null || daysRemaining <= 0) {
                                            setAddUsers(1);
                                            setAddStorage(2);
                                        }
                                        setShowCart(true);
                                    }}
                                    className="h-8 px-3 bg-green-600 text-white rounded-lg font-black text-[9px] uppercase tracking-wider hover:bg-green-700 active:scale-95 transition-all shadow-lg shadow-green-500/20 flex items-center gap-1.5"
                                >
                                    <Plus size={12} />
                                    {daysRemaining === null || daysRemaining <= 0 ? 'Start Plan' : daysRemaining < 7 ? 'Renew' : 'Extend'}
                                </button>
                            )}
                        </div>
                    </div>
                </div>

                {/* Order Summary */}
                {showCart && (addUsers !== 0 || addStorage !== 0 || isRenewing) && (
                    <div className="animate-in fade-in slide-in-from-bottom-8 duration-700">
                        <div className="bg-white dark:bg-gray-800 rounded-[2.5rem] border border-gray-100 dark:border-gray-700 shadow-2xl overflow-hidden">
                            {/* Header */}
                            <div className="p-8 border-b border-gray-50 dark:border-gray-700 bg-gray-50/30 dark:bg-gray-800/50 flex items-center justify-between">
                                <div className="flex items-center gap-3">
                                    <div className="w-12 h-12 bg-blue-600 rounded-2xl flex items-center justify-center text-white shadow-lg shadow-blue-500/20">
                                        <ShoppingBag size={24} />
                                    </div>
                                    <div>
                                        <h3 className="font-black text-gray-900 dark:text-white uppercase tracking-widest text-lg">Order Summary</h3>
                                        <p className="text-sm text-gray-500 dark:text-gray-400 font-medium">Configure your subscription updates</p>
                                    </div>
                                </div>
                                <div className="hidden md:flex items-center gap-6">
                                    <div className="flex items-center gap-4 text-[10px] font-black uppercase tracking-widest text-gray-400 dark:text-gray-500">
                                        <span className="flex items-center gap-1.5"><CheckCircle size={14} className="text-emerald-500" /> Secure</span>
                                        <span className="flex items-center gap-1.5"><CheckCircle size={14} className="text-emerald-500" /> Instant</span>
                                    </div>
                                    <button
                                        onClick={() => { setAddUsers(0); setAddStorage(0); setIsRenewing(false); }}
                                        className="px-4 py-2 bg-white dark:bg-gray-700 text-gray-400 dark:text-gray-300 hover:text-red-500 dark:hover:text-red-400 rounded-xl border border-gray-100 dark:border-gray-600 text-[10px] font-black uppercase tracking-widest transition-all shadow-sm active:scale-95"
                                    >
                                        Reset Cart
                                    </button>
                                </div>
                            </div>

                            <div className="grid grid-cols-1 lg:grid-cols-2 gap-0 divide-y lg:divide-y-0 lg:divide-x divide-gray-100 dark:divide-gray-700">
                                {/* Left Side: Configuration */}
                                <div className="p-8 space-y-8 bg-white dark:bg-gray-800">
                                    <div className="space-y-6">
                                        <div className="flex items-center justify-between">
                                            <div className="flex flex-col gap-1">
                                                <h4 className="text-[10px] font-black uppercase tracking-[0.2em] text-gray-400">Selected Items</h4>
                                                {isRenewing && <span className="text-[10px] text-emerald-600 font-black uppercase tracking-widest flex items-center gap-1"><CheckCircle size={10} /> Renewing Current Plan</span>}
                                            </div>
                                        </div>

                                        <div className="space-y-4">
                                            {(addUsers > 0 || isRenewing) && (
                                                <div className="flex justify-between items-center text-sm p-4 bg-gray-50 dark:bg-gray-700/30 rounded-2xl border border-gray-100 dark:border-gray-700 group transition-all">
                                                    <div className="flex items-center gap-4">
                                                        <div className="w-10 h-10 bg-blue-600 rounded-xl flex items-center justify-center font-black text-white shadow-md">
                                                            {isRenewing ? (usage?.allowed_users || 0) + addUsers : addUsers}
                                                        </div>
                                                        <div className="flex flex-col">
                                                            <span className="font-black text-gray-900 dark:text-white">{isRenewing ? 'Total Employee Slots' : 'Additional Users'}</span>
                                                            <span className="text-xs text-gray-400 dark:text-gray-500 font-bold tracking-tighter">
                                                                {isRenewing ? `Incl. ${usage?.allowed_users || 0} existing` : `Rs ${unitPrices.price_per_user} / month`}
                                                            </span>
                                                        </div>
                                                    </div>
                                                    <div className="flex items-center gap-6">
                                                        <div className="flex items-center bg-white dark:bg-gray-700 border border-gray-200 dark:border-gray-600 rounded-xl overflow-hidden shadow-sm">
                                                            <button
                                                                onClick={() => {
                                                                    if (isRenewing) {
                                                                        // Allow negative addUsers but total must be >= 1
                                                                        const total = (usage?.allowed_users || 0) + addUsers - 1;
                                                                        if (total >= 1) setAddUsers(addUsers - 1);
                                                                    } else {
                                                                        setAddUsers(Math.max(0, addUsers - 1));
                                                                    }
                                                                }}
                                                                className="w-10 h-10 flex items-center justify-center hover:bg-gray-50 dark:hover:bg-gray-600 text-gray-400 dark:text-gray-300 hover:text-red-500 transition-colors border-r border-gray-100 dark:border-gray-600"
                                                            >
                                                                <Minus size={14} />
                                                            </button>
                                                            <span className="w-12 text-center text-sm font-black text-gray-900 dark:text-white">
                                                                {isRenewing ? (usage?.allowed_users || 0) + addUsers : addUsers}
                                                            </span>
                                                            <button onClick={() => setAddUsers(addUsers + 1)} className="w-10 h-10 flex items-center justify-center hover:bg-gray-50 dark:hover:bg-gray-600 text-gray-400 dark:text-gray-300 hover:text-blue-500 transition-colors border-l border-gray-100 dark:border-gray-600">
                                                                <Plus size={14} />
                                                            </button>
                                                        </div>
                                                        <span className="font-black text-gray-900 dark:text-white min-w-[80px] text-right text-base text-blue-600 dark:text-blue-400">
                                                            Rs {((isRenewing ? (usage?.allowed_users || 0) : 0) + addUsers) * unitPrices.price_per_user}
                                                        </span>
                                                    </div>
                                                </div>
                                            )}
                                            {(addStorage > 0 || isRenewing) && (
                                                <div className="flex justify-between items-center text-sm p-4 bg-gray-50 dark:bg-gray-700/30 rounded-2xl border border-gray-100 dark:border-gray-700 transition-all">
                                                    <div className="flex items-center gap-4">
                                                        <div className="w-10 h-10 bg-indigo-600 rounded-xl flex items-center justify-center font-black text-white shadow-md">
                                                            {isRenewing ? (usage?.allowed_storage_gb || 0) + addStorage : addStorage}
                                                        </div>
                                                        <div className="flex flex-col">
                                                            <span className="font-black text-gray-900 dark:text-white">{isRenewing ? 'Total Storage Limit' : 'Extra Storage'}</span>
                                                            <span className="text-xs text-gray-400 dark:text-gray-500 font-bold tracking-tighter">
                                                                {isRenewing
                                                                    ? `Current: ${usage?.allowed_storage_gb || 0} GB → New Total: ${(usage?.allowed_storage_gb || 0) + addStorage} GB`
                                                                    : `+${addStorage} GB @ Rs ${unitPrices.price_per_gb}/GB/month`}
                                                            </span>
                                                            {!isRenewing && addStorage > 0 && (
                                                                <span className="text-[10px] text-indigo-600 dark:text-indigo-400 font-bold mt-0.5">
                                                                    Your total storage will become: {(usage?.allowed_storage_gb || 0) + addStorage} GB
                                                                </span>
                                                            )}
                                                        </div>
                                                    </div>
                                                    <div className="flex items-center gap-6">
                                                        <div className="flex items-center bg-white dark:bg-gray-700 border border-gray-200 dark:border-gray-600 rounded-xl overflow-hidden shadow-sm">
                                                            <button
                                                                onClick={() => {
                                                                    if (isRenewing) {
                                                                        // Allow negative addStorage but total must be >= 0
                                                                        const total = (usage?.allowed_storage_gb || 0) + addStorage - 2;
                                                                        if (total >= 0) setAddStorage(addStorage - 2);
                                                                    } else {
                                                                        setAddStorage(Math.max(0, addStorage - 2));
                                                                    }
                                                                }}
                                                                className="w-10 h-10 flex items-center justify-center hover:bg-gray-50 dark:hover:bg-gray-600 text-gray-400 dark:text-gray-300 hover:text-red-500 transition-colors border-r border-gray-100 dark:border-gray-600"
                                                            >
                                                                <Minus size={14} />
                                                            </button>
                                                            <div className="w-16 text-center">
                                                                <span className="text-sm font-black text-gray-900 dark:text-white">
                                                                    {isRenewing ? (usage?.allowed_storage_gb || 0) + addStorage : addStorage}
                                                                </span>
                                                                <span className="text-[9px] font-bold text-gray-400 dark:text-gray-500 block -mt-0.5">GB</span>
                                                            </div>
                                                            <button onClick={() => setAddStorage(addStorage + 2)} className="w-10 h-10 flex items-center justify-center hover:bg-gray-50 dark:hover:bg-gray-600 text-gray-400 dark:text-gray-300 hover:text-blue-500 transition-colors border-l border-gray-100 dark:border-gray-600">
                                                                <Plus size={14} />
                                                            </button>
                                                        </div>
                                                        <span className="font-black text-gray-900 dark:text-white min-w-[80px] text-right text-base text-indigo-600 dark:text-indigo-400">
                                                            Rs {((isRenewing ? (usage?.allowed_storage_gb || 0) : 0) + addStorage) * unitPrices.price_per_gb}
                                                        </span>
                                                    </div>
                                                </div>
                                            )}
                                        </div>
                                    </div>

                                    {!isRenewing && (addUsers > 0 || addStorage > 0) && daysRemaining !== null && daysRemaining > 0 && (
                                        <div className="pt-6 border-t border-gray-50 dark:border-gray-700">
                                            <div className="flex items-center justify-between p-4 bg-amber-50 dark:bg-amber-900/20 rounded-2xl border border-amber-100 dark:border-amber-900/30">
                                                <div className="flex items-center gap-3">
                                                    <div className="w-10 h-10 bg-amber-100 dark:bg-amber-900/50 text-amber-600 dark:text-amber-500 rounded-xl flex items-center justify-center">
                                                        <Calendar size={18} />
                                                    </div>
                                                    <div>
                                                        <p className="text-[10px] font-black text-amber-700 dark:text-amber-500 uppercase tracking-widest">Prorated Add-on</p>
                                                        <p className="text-sm font-bold text-gray-900 dark:text-white">
                                                            Billing up to <span className="font-black text-amber-700 dark:text-amber-500">{usage.plan_expiry ? new Date(usage.plan_expiry).toLocaleDateString(undefined, { day: 'numeric', month: 'short', year: 'numeric' }) : 'Current Expiry'}</span>
                                                        </p>
                                                    </div>
                                                </div>
                                                <span className="text-[10px] font-black text-amber-600 dark:text-amber-500 bg-amber-100 dark:bg-amber-900/50 px-3 py-1.5 rounded-lg uppercase">{daysRemaining} Days Left</span>
                                            </div>
                                        </div>
                                    )}

                                    {isRenewing && (
                                        <div className="pt-8 border-t border-gray-50 dark:border-gray-700 space-y-5">
                                            <div className="flex items-center justify-between">
                                                <h4 className="text-[10px] font-black uppercase tracking-[0.2em] text-gray-400 dark:text-gray-500">Subscription Term</h4>
                                                <div className="flex items-center gap-2 text-[10px] font-black text-blue-600 dark:text-blue-400 bg-blue-50 dark:bg-blue-900/30 px-3 py-1 rounded-full uppercase">
                                                    <Calendar size={12} />
                                                    Bill for {durations.find(d => d.months === duration)?.label}
                                                </div>
                                            </div>
                                            <div className="grid grid-cols-2 gap-3">
                                                {durations.map((d) => (
                                                    <button
                                                        key={d.months}
                                                        type="button"
                                                        onClick={() => setDuration(d.months)}
                                                        className={`relative overflow-hidden p-4 rounded-2xl border-2 transition-all duration-300 text-left group ${duration === d.months ? 'border-blue-600 bg-blue-50/20 dark:bg-blue-900/20' : 'bg-gray-50 dark:bg-gray-700/50 border-gray-100 dark:border-gray-700 hover:border-blue-200 dark:hover:border-blue-800'}`}
                                                    >
                                                        {d.badge && (
                                                            <div className="absolute top-0 right-0 bg-blue-600 text-[8px] font-black text-white px-2 py-1 rounded-bl-xl uppercase scale-90 origin-top-right">
                                                                {d.badge}
                                                            </div>
                                                        )}
                                                        <p className={`text-[11px] font-black uppercase tracking-wider ${duration === d.months ? 'text-blue-600 dark:text-blue-400' : 'text-gray-500 dark:text-gray-400'}`}>{d.label}</p>
                                                        <div className="flex items-end justify-between mt-1">
                                                            <p className="text-[10px] text-gray-400 dark:text-gray-500 font-bold">{d.months === 1 ? '30' : '90'} Days</p>
                                                            {d.discount > 0 && <span className="text-[10px] font-black text-emerald-600 dark:text-emerald-400">-{d.discount}%</span>}
                                                        </div>
                                                    </button>
                                                ))}
                                            </div>

                                            {/* New Expiry Preview */}
                                            <div className="flex items-center justify-between p-4 bg-emerald-50 dark:bg-emerald-900/20 rounded-2xl border border-emerald-100 dark:border-emerald-900/30">
                                                <div className="flex items-center gap-3">
                                                    <div className="w-10 h-10 bg-emerald-100 dark:bg-emerald-900/50 text-emerald-600 dark:text-emerald-500 rounded-xl flex items-center justify-center">
                                                        <Calendar size={18} />
                                                    </div>
                                                    <div>
                                                        <p className="text-[10px] font-black text-emerald-700 dark:text-emerald-500 uppercase tracking-widest">New Expiry Date</p>
                                                        <p className="text-lg font-black text-gray-900 dark:text-white">
                                                            {(() => {
                                                                const baseDate = usage?.plan_expiry && new Date(usage.plan_expiry) > new Date()
                                                                    ? new Date(usage.plan_expiry)
                                                                    : new Date();
                                                                baseDate.setMonth(baseDate.getMonth() + duration);
                                                                return baseDate.toLocaleDateString(undefined, { day: 'numeric', month: 'short', year: 'numeric' });
                                                            })()}
                                                        </p>
                                                    </div>
                                                </div>
                                                <span className="text-[10px] font-black text-emerald-600 dark:text-emerald-500 bg-emerald-100 dark:bg-emerald-900/50 px-3 py-1.5 rounded-lg uppercase">+{duration === 1 ? '30' : '90'} Days</span>
                                            </div>
                                        </div>
                                    )}
                                </div>

                                {/* Right Side: Billing & Checkout */}
                                <div className="p-8 space-y-8 bg-gray-50/30 dark:bg-gray-800/50">
                                    <div className="space-y-6">
                                        <h4 className="text-xs font-black uppercase tracking-[0.2em] text-gray-400">Payment Breakdown</h4>
                                        <div className="space-y-3">
                                            {/* Prorated Addition Line (if renewing with additions and plan is active) */}
                                            {isRenewing && daysRemaining !== null && daysRemaining > 0 && (addUsers > 0 || addStorage > 0) && (
                                                <div className="flex justify-between items-center p-4 bg-amber-50 dark:bg-amber-900/20 rounded-xl border border-amber-100 dark:border-amber-900/30">
                                                    <div>
                                                        <span className="font-bold text-amber-700 dark:text-amber-500 uppercase tracking-widest text-xs block">Prorated Addition</span>
                                                        <span className="text-xs text-amber-600 dark:text-amber-400">For {daysRemaining} remaining days</span>
                                                    </div>
                                                    <span className="font-black text-amber-700 dark:text-amber-500 font-mono text-lg">
                                                        Rs {(((Math.max(0, addUsers) * (unitPrices?.price_per_user || 149)) + (Math.max(0, addStorage) * (unitPrices?.price_per_gb || 99))) * (daysRemaining / 30)).toFixed(2)}
                                                    </span>
                                                </div>
                                            )}

                                            {/* Renewal Cost Line */}
                                            {isRenewing && (
                                                <div className="flex justify-between items-center">
                                                    <div>
                                                        <span className="font-bold text-gray-500 dark:text-gray-400 uppercase tracking-widest text-xs block">Renewal ({duration === 1 ? '30' : '90'} Days)</span>
                                                        <span className="text-xs text-gray-400 dark:text-gray-500">{Math.max(1, (usage?.allowed_users || 0) + addUsers)} users + {Math.max(0, (usage?.allowed_storage_gb || 0) + addStorage)} GB</span>
                                                    </div>
                                                    <span className="font-black text-gray-900 dark:text-white font-mono text-lg">
                                                        Rs {((Math.max(1, (usage?.allowed_users || 0) + addUsers) * (unitPrices?.price_per_user || 149) + Math.max(0, (usage?.allowed_storage_gb || 0) + addStorage) * (unitPrices?.price_per_gb || 99)) * duration).toLocaleString()}
                                                    </span>
                                                </div>
                                            )}

                                            {/* Add-on Only Line (not renewing) */}
                                            {!isRenewing && (
                                                <div className="flex justify-between items-center">
                                                    <span className="font-bold text-gray-500 dark:text-gray-400 uppercase tracking-widest text-xs">
                                                        Prorated ({daysRemaining || 0} Days)
                                                    </span>
                                                    <span className="font-black text-gray-900 dark:text-white font-mono text-lg">Rs {calculateSubtotal().toLocaleString()}</span>
                                                </div>
                                            )}

                                            {/* Duration Discount (only for renewals with 90 days) */}
                                            {isRenewing && durations.find(d => d.months === duration)?.discount > 0 && (
                                                <div className="flex justify-between items-center p-4 bg-emerald-50 dark:bg-emerald-900/20 rounded-xl border border-emerald-100 dark:border-emerald-900/30">
                                                    <span className="text-emerald-700 dark:text-emerald-500 font-bold text-xs uppercase tracking-widest">Duration Savings ({durations.find(d => d.months === duration).discount}%)</span>
                                                    <span className="text-emerald-700 dark:text-emerald-500 font-black text-lg">-Rs {(((Math.max(1, (usage?.allowed_users || 0) + addUsers) * (unitPrices?.price_per_user || 149) + Math.max(0, (usage?.allowed_storage_gb || 0) + addStorage) * (unitPrices?.price_per_gb || 99)) * duration) * (durations.find(d => d.months === duration).discount / 100)).toFixed(2)}</span>
                                                </div>
                                            )}

                                            <div className="pt-2">
                                                <div className={`flex items-center gap-2 p-1.5 rounded-2xl border transition-all duration-300 ${appliedPromo ? 'border-emerald-500 bg-emerald-50 shadow-sm' : 'bg-white dark:bg-gray-800 border-gray-200 dark:border-gray-700 focus-within:border-blue-500 focus-within:ring-4 focus-within:ring-blue-500/10'}`}>
                                                    <div className="flex-1 relative flex items-center">
                                                        <Tag size={16} className={`ml-3 mr-2 ${appliedPromo ? 'text-emerald-500' : 'text-gray-400'}`} />
                                                        <input
                                                            type="text"
                                                            disabled={appliedPromo}
                                                            className="w-full bg-transparent border-none outline-none text-xs font-bold text-gray-900 dark:text-white placeholder:text-gray-400 h-9"
                                                            placeholder="Enter promo code"
                                                            value={promoCode}
                                                            onChange={(e) => setPromoCode(e.target.value.toUpperCase())}
                                                        />
                                                    </div>
                                                    {!appliedPromo ? (
                                                        <button onClick={handleApplyPromo} disabled={checkingPromo || !promoCode} className="px-5 py-2.5 bg-gray-900 dark:bg-blue-600 text-white rounded-xl text-[10px] font-black uppercase tracking-widest hover:bg-black dark:hover:bg-blue-500 transition-all disabled:opacity-50">
                                                            {checkingPromo ? <Loader2 size={12} className="animate-spin" /> : 'Apply'}
                                                        </button>
                                                    ) : (
                                                        <button onClick={() => { setAppliedPromo(null); setPromoCode(''); }} className="w-10 h-10 flex items-center justify-center bg-white dark:bg-white/5 text-emerald-600 dark:text-emerald-400 rounded-xl border border-emerald-100 dark:border-emerald-900/30 hover:text-red-500 dark:hover:text-red-400 transition-colors">
                                                            <Trash2 size={16} />
                                                        </button>
                                                    )}
                                                </div>
                                                {appliedPromo && (
                                                    <div className="flex justify-between items-center text-sm p-3 bg-emerald-50 dark:bg-emerald-900/20 rounded-xl border border-emerald-100 dark:border-emerald-900/30 mt-3 animate-in fade-in zoom-in-95 duration-300">
                                                        <span className="text-emerald-700 dark:text-emerald-400 font-bold text-[10px] uppercase tracking-widest flex items-center gap-2"><CheckCircle size={14} /> Promo Discount ({appliedPromo.discount_percent}%)</span>
                                                        <span className="text-emerald-700 dark:text-emerald-400 font-black">-Rs {((calculateSubtotal() * (1 - (durations.find(d => d.months === duration)?.discount || 0) / 100)) * appliedPromo.discount_percent / 100).toFixed(2)}</span>
                                                    </div>
                                                )}
                                                {promoError && <p className="text-[10px] text-red-500 mt-2 ml-2 font-bold flex items-center gap-1"><AlertCircle size={10} /> {promoError}</p>}
                                            </div>
                                        </div>

                                        <div className="pt-6 border-t border-gray-100 space-y-6">
                                            <div className="bg-gradient-to-r from-blue-600 to-indigo-700 rounded-3xl p-8 text-white shadow-xl shadow-blue-500/20 relative overflow-hidden group">
                                                <div className="absolute top-0 right-0 w-32 h-32 bg-white/10 rounded-full -mr-10 -mt-10 blur-2xl group-hover:scale-150 transition-transform duration-700" />
                                                <div className="relative z-10 flex items-end justify-between">
                                                    <div>
                                                        <p className="text-[10px] uppercase font-black text-white/60 tracking-[0.2em] mb-2">Total Due Now</p>
                                                        <p className="text-4xl font-black">Rs {calculateTotal().toLocaleString()}</p>
                                                    </div>
                                                    <Sparkles size={32} className="text-white/30" />
                                                </div>
                                            </div>

                                            <div className="space-y-4">
                                                <button
                                                    onClick={handleCheckout}
                                                    disabled={checkoutLoading || (addUsers === 0 && addStorage === 0 && !isRenewing)}
                                                    className="w-full h-16 bg-gray-900 dark:bg-blue-600 text-white rounded-[1.25rem] text-xl font-black gap-3 shadow-xl hover:bg-black dark:hover:bg-blue-500 active:scale-[0.98] transition-all flex items-center justify-center group disabled:opacity-50 disabled:cursor-not-allowed"
                                                >
                                                    {checkoutLoading ? (
                                                        <>
                                                            <Loader2 className="animate-spin" size={24} />
                                                            <span className="text-base">Processing...</span>
                                                        </>
                                                    ) : (
                                                        <>
                                                            <CreditCard size={22} />
                                                            <span>Pay via UPI</span>
                                                            <ChevronRight className="group-hover:translate-x-1 transition-transform" />
                                                        </>
                                                    )}
                                                </button>
                                                <div className="flex items-center justify-center gap-2 text-center text-[10px] text-gray-400 font-black uppercase tracking-widest px-4 opacity-70">
                                                    <span>Instant Activation</span>
                                                    <span className="text-gray-300">•</span>
                                                    <span>Secure UPI Payment</span>
                                                </div>
                                            </div>
                                        </div>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>
                )}

                {/* UPI Payment Modal */}
                <UpiPaymentModal
                    isOpen={showUpiModal}
                    onClose={() => setShowUpiModal(false)}
                    amount={pendingPaymentAmount}
                    onPaymentSuccess={handleManualPaymentSuccess}
                    orderDetails={pendingOrderDetails}
                />

                {/* Global Notifications */}
                <div className="fixed bottom-8 right-8 z-50 flex flex-col gap-3 pointer-events-none">
                    {success && (
                        <div className="bg-emerald-600 text-white px-6 py-4 rounded-2xl shadow-2xl flex items-center gap-3 animate-in slide-in-from-bottom-5 fade-in duration-300 pointer-events-auto">
                            <CheckCircle size={24} className="text-emerald-100" />
                            <span className="font-black text-sm">{success}</span>
                        </div>
                    )}
                    {error && (
                        <div className="bg-red-600 text-white px-6 py-4 rounded-2xl shadow-2xl flex items-center gap-3 animate-in slide-in-from-bottom-5 fade-in duration-300 pointer-events-auto">
                            <AlertCircle size={24} className="text-red-100" />
                            <span className="font-black text-sm">{error}</span>
                        </div>
                    )}
                </div>
            </div>
        </div>
    );
}

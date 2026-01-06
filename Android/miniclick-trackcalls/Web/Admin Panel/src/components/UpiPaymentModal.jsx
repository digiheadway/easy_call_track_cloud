import { useState, useEffect } from 'react';
import { createPortal } from 'react-dom';
import { motion, AnimatePresence } from 'framer-motion';
import {
    X,
    CheckCircle2,
    Loader2,
    Smartphone,
    Copy,
    Check,
    AlertCircle,
    ShieldCheck,
    ExternalLink,
    ArrowRight,
    CreditCard,
    Zap
} from 'lucide-react';
import api from '../api/client';
import { toast } from 'sonner';

/**
 * UPI Payment Modal - High Performance & Premium UI
 * Rebuilt for a professional "Paytm/PhonePe" checkout experience
 */

// UPI Configuration
const UPI_ID = '9068062563@ptaxis';
const UPI_NAME = 'MiniClick Calls';

export default function UpiPaymentModal({
    isOpen,
    onClose,
    amount,
    onPaymentSuccess,
    orderDetails
}) {
    const [step, setStep] = useState('qr'); // 'qr', 'utr', 'confirming', 'success'
    const [utr, setUtr] = useState('');
    const [copied, setCopied] = useState(false);
    const [countdown, setCountdown] = useState(3);
    const [imageLoaded, setImageLoaded] = useState(false);

    // Generate UPI link
    const upiLink = `upi://pay?pa=${UPI_ID}&pn=${encodeURIComponent(UPI_NAME)}&am=${amount}&cu=INR`;

    // Generate QR code URL using a reliable API
    const qrCodeUrl = `https://api.qrserver.com/v1/create-qr-code/?size=300x300&data=${encodeURIComponent(upiLink)}&bgcolor=ffffff`;

    useEffect(() => {
        if (isOpen) {
            setStep('qr');
            setUtr('');
            setCopied(false);
            setCountdown(3);
            setImageLoaded(false);
            document.body.style.overflow = 'hidden';
        } else {
            document.body.style.overflow = 'unset';
        }
        return () => { document.body.style.overflow = 'unset'; };
    }, [isOpen]);

    useEffect(() => {
        if (step === 'success' && countdown > 0) {
            const timer = setTimeout(() => setCountdown(countdown - 1), 1000);
            return () => clearTimeout(timer);
        }
        if (step === 'success' && countdown === 0) {
            onPaymentSuccess?.();
            onClose();
        }
    }, [step, countdown, onPaymentSuccess, onClose]);

    const handleCopyUpiId = () => {
        navigator.clipboard.writeText(UPI_ID);
        setCopied(true);
        setTimeout(() => setCopied(false), 2000);
    };

    const handleConfirmPayment = () => {
        setStep('utr');
    };

    const handleFinalSubmit = async () => {
        if (!utr.trim()) return;

        try {
            setStep('confirming');

            // Call API to record payment
            await api.post('/billing.php?action=submit_upi_payment', {
                utr: utr.trim(),
                amount: amount,
                user_count: orderDetails?.user_count || 0,
                storage_gb: orderDetails?.storage_gb || 0,
                duration_months: orderDetails?.duration_months || 1,
                is_renewing: orderDetails?.is_renewing || false
            });

            // If successful (api client throws on status: false)
            setStep('success');
            toast.success("Payment submitted successfully!");

        } catch (error) {
            setStep('utr'); // Go back to UTR step so they can try again or check ID
            toast.error(error.message || "Failed to submit payment details");
        }
    };

    if (!isOpen) return null;

    const modalVariants = {
        hidden: { opacity: 0, scale: 0.95, y: 20 },
        visible: {
            opacity: 1,
            scale: 1,
            y: 0,
            transition: { type: 'spring', damping: 25, stiffness: 300 }
        },
        exit: { opacity: 0, scale: 0.95, y: 20, transition: { duration: 0.2 } }
    };

    const overlayVariants = {
        hidden: { opacity: 0 },
        visible: { opacity: 1 },
        exit: { opacity: 0 }
    };

    return createPortal(
        <div className="fixed inset-0 z-[10000] flex items-center justify-center p-4 sm:p-6 overflow-hidden">
            {/* Animated Backdrop */}
            <AnimatePresence>
                {isOpen && (
                    <motion.div
                        initial="hidden"
                        animate="visible"
                        exit="exit"
                        variants={overlayVariants}
                        className="absolute inset-0 bg-slate-900/60 backdrop-blur-md"
                        onClick={step === 'qr' ? onClose : undefined}
                    />
                )}
            </AnimatePresence>

            {/* Main Modal Container */}
            <motion.div
                initial="hidden"
                animate="visible"
                exit="exit"
                variants={modalVariants}
                className="relative bg-white dark:bg-gray-800 w-full max-w-md rounded-[2.5rem] shadow-[0_32px_64px_-12px_rgba(0,0,0,0.3)] overflow-hidden flex flex-col max-h-[92vh] sm:max-h-[85vh]"
            >
                {/* 1. Dynamic Header Section */}
                <div className="relative bg-gradient-to-br from-indigo-600 via-blue-600 to-blue-700 text-white px-6 py-5 overflow-hidden shrink-0">
                    {/* Decorative Elements */}
                    <div className="absolute top-0 right-0 w-64 h-64 bg-white/10 rounded-full -mr-32 -mt-32 blur-3xl animate-pulse" />
                    <div className="absolute bottom-0 left-0 w-48 h-48 bg-indigo-400/20 rounded-full -ml-24 -mb-24 blur-2xl" />

                    <div className="relative z-10 flex justify-between items-start">
                        <div className="space-y-1">
                            <div className="flex items-center gap-2 mb-2">
                                <div className="p-1 px-2 bg-white/20 backdrop-blur-md rounded-lg border border-white/20 flex items-center gap-1.5">
                                    <Zap size={12} className="text-yellow-300 fill-yellow-300" />
                                    <span className="text-[10px] font-black uppercase tracking-widest text-white">Fast Setup</span>
                                </div>
                            </div>
                            <h3 className="text-white/70 font-medium text-[10px] tracking-widest uppercase">Invoice Total</h3>
                            <div className="flex items-baseline gap-2">
                                <span className="text-4xl font-black tracking-tight">₹{amount.toLocaleString()}</span>
                                <span className="text-white/50 text-xs font-bold uppercase">INR</span>
                            </div>
                        </div>

                        {step === 'qr' && (
                            <button
                                onClick={onClose}
                                className="group p-2.5 bg-white/10 hover:bg-white/20 border border-white/10 rounded-2xl text-white transition-all transform active:scale-90"
                            >
                                <X size={20} />
                            </button>
                        )}
                    </div>

                    {/* Plan Benefits Summary */}
                    {orderDetails && (
                        <div className="relative z-10 mt-4 pt-3 border-t border-white/20">
                            <p className="text-[10px] font-bold text-white/60 uppercase tracking-widest mb-2">After Payment, You'll Get:</p>
                            <div className="flex flex-wrap gap-2">
                                {(orderDetails.add_users > 0 || orderDetails.user_count > 0) && (
                                    <span className="px-2 py-1 bg-white/10 backdrop-blur-md rounded-lg border border-white/10 text-[10px] font-bold text-white">
                                        {orderDetails.add_users > 0 ? `+${orderDetails.add_users} Employee Slots` : `${orderDetails.user_count} Employee Slots`}
                                    </span>
                                )}
                                {(orderDetails.add_storage > 0 || orderDetails.storage_gb > 0) && (
                                    <span className="px-2 py-1 bg-white/10 backdrop-blur-md rounded-lg border border-white/10 text-[10px] font-bold text-white">
                                        {orderDetails.add_storage > 0 ? `+${orderDetails.add_storage} GB Storage` : `${orderDetails.storage_gb} GB Storage`}
                                    </span>
                                )}
                                {orderDetails.duration_months > 0 && (
                                    <span className="px-2 py-1 bg-white/10 backdrop-blur-md rounded-lg border border-white/10 text-[10px] font-bold text-white">
                                        +{orderDetails.duration_months === 1 ? '30 Days' : `${orderDetails.duration_months * 30} Days`}
                                    </span>
                                )}
                            </div>
                        </div>
                    )}
                </div>

                {/* 2. Main Action Area */}
                <div className="flex-1 flex flex-col overflow-hidden bg-white dark:bg-gray-800 relative z-20">
                    <div className="flex-1 overflow-y-auto scrollbar-hide">
                        <AnimatePresence mode="wait">
                            {step === 'qr' && (
                                <motion.div
                                    key="qr-step"
                                    initial={{ opacity: 0, y: 10 }}
                                    animate={{ opacity: 1, y: 0 }}
                                    exit={{ opacity: 0, y: -10 }}
                                    className="p-6 md:p-8 space-y-8"
                                >
                                    {/* QR Container */}
                                    <div className="flex flex-col items-center">
                                        <div className="relative p-1.5 bg-slate-50 dark:bg-gray-700 rounded-[2rem] border border-slate-100 dark:border-gray-600 shadow-inner">
                                            <div className="relative bg-white p-4 rounded-[1.5rem] shadow-lg border border-slate-50 dark:border-gray-600">
                                                {!imageLoaded && (
                                                    <div className="absolute inset-0 flex items-center justify-center bg-white rounded-[1.5rem]">
                                                        <Loader2 size={32} className="text-blue-500 animate-spin" />
                                                    </div>
                                                )}
                                                <img
                                                    src={qrCodeUrl}
                                                    alt="UPI QR Code"
                                                    className={`w-52 h-52 md:w-60 md:h-60 object-contain transition-opacity duration-500 ${imageLoaded ? 'opacity-100' : 'opacity-0'}`}
                                                    onLoad={() => setImageLoaded(true)}
                                                />
                                            </div>

                                            {/* Merchant Name Badge */}
                                            <div className="absolute -bottom-4 left-1/2 -translate-x-1/2 bg-white dark:bg-gray-800 px-4 py-2 rounded-full shadow-md border border-slate-100 dark:border-gray-600 flex items-center gap-2 whitespace-nowrap z-30">
                                                <div className="w-5 h-5 bg-blue-600 rounded-full flex items-center justify-center text-[10px] font-bold text-white">CT</div>
                                                <span className="text-[11px] font-bold text-slate-700 dark:text-gray-200">Scan with any UPI App

                                                </span>
                                            </div>
                                        </div>


                                    </div>

                                    {/* Divider */}
                                    <div className="flex items-center gap-4">
                                        <div className="h-[1px] flex-1 bg-slate-100 dark:bg-gray-700" />
                                        <span className="text-[10px] font-black text-slate-300 dark:text-gray-600 uppercase tracking-widest">or pay to ID</span>
                                        <div className="h-[1px] flex-1 bg-slate-100 dark:bg-gray-700" />
                                    </div>

                                    {/* UPI ID Input Area */}
                                    <div className="space-y-2">
                                        <div className="relative group">
                                            <div className="absolute -inset-1 bg-gradient-to-r from-blue-600 to-indigo-600 rounded-2xl blur opacity-0 group-hover:opacity-10 transition duration-500"></div>
                                            <div className="relative flex items-center gap-3 bg-slate-50 dark:bg-gray-700/50 border border-slate-200 dark:border-gray-600 rounded-2xl p-4 transition-all group-hover:border-blue-200 dark:group-hover:border-blue-500/50">
                                                <div className="w-10 h-10 bg-white dark:bg-gray-600 rounded-xl shadow-sm border border-slate-100 dark:border-gray-500 flex items-center justify-center text-blue-600 dark:text-blue-400">
                                                    <CreditCard size={20} />
                                                </div>
                                                <div className="flex-1">
                                                    <p className="text-[10px] text-slate-400 dark:text-gray-400 font-bold uppercase tracking-tight">UPI Address</p>
                                                    <p className="text-sm font-black text-slate-800 dark:text-white tracking-tight">{UPI_ID}</p>
                                                </div>
                                                <button
                                                    onClick={handleCopyUpiId}
                                                    className={`p-2.5 rounded-xl transition-all active:scale-90 ${copied
                                                        ? 'bg-emerald-500 text-white shadow-lg shadow-emerald-500/30'
                                                        : 'bg-white dark:bg-gray-600 text-slate-600 dark:text-gray-200 shadow-sm border border-slate-100 dark:border-gray-500 hover:border-slate-200'
                                                        }`}
                                                >
                                                    {copied ? <Check size={18} strokeWidth={3} /> : <Copy size={18} />}
                                                </button>
                                            </div>
                                        </div>
                                    </div>
                                </motion.div>
                            )}

                            {/* UTR ENTRY STEP */}
                            {step === 'utr' && (
                                <motion.div
                                    key="utr-step"
                                    initial={{ opacity: 0, x: 20 }}
                                    animate={{ opacity: 1, x: 0 }}
                                    exit={{ opacity: 0, x: -20 }}
                                    className="p-8 space-y-6"
                                >
                                    <div className="space-y-2">
                                        <button
                                            onClick={() => setStep('qr')}
                                            className="text-xs font-bold text-blue-600 dark:text-blue-400 flex items-center gap-1 hover:underline"
                                        >
                                            <ArrowRight size={14} className="rotate-180" />
                                            Back to QR Code
                                        </button>
                                        <h3 className="text-2xl font-black text-slate-900 dark:text-white tracking-tight">Confirm Payment</h3>
                                        <p className="text-slate-500 dark:text-gray-400 text-sm font-medium leading-relaxed">
                                            To verify your transaction, please enter the <span className="font-bold text-slate-800 dark:text-gray-200">UTR / Transaction ID</span> from your UPI app.
                                        </p>
                                    </div>

                                    {/* Limits Increase Info Banner */}
                                    {orderDetails && (
                                        <div className="p-4 rounded-2xl bg-gradient-to-r from-blue-50 to-indigo-50 dark:from-blue-900/20 dark:to-indigo-900/20 border border-blue-100 dark:border-blue-800">
                                            <div className="flex items-start gap-3">
                                                <div className="w-8 h-8 bg-blue-600 rounded-lg flex items-center justify-center shrink-0">
                                                    <Zap size={16} className="text-white" />
                                                </div>
                                                <div>
                                                    <p className="text-xs font-black text-blue-700 dark:text-blue-400 mb-1">Your Plan Benefits (After Verification)</p>
                                                    <p className="text-[11px] text-slate-600 dark:text-gray-300 font-medium leading-relaxed">
                                                        Once your payment is verified, your limits will be increased as per your chosen plan:
                                                    </p>
                                                    <div className="flex flex-wrap gap-1.5 mt-2">
                                                        {(orderDetails.add_users > 0 || orderDetails.user_count > 0) && (
                                                            <span className="px-2 py-1 bg-blue-100 dark:bg-blue-900/30 text-blue-700 dark:text-blue-300 rounded-md text-[10px] font-bold">
                                                                {orderDetails.add_users > 0 ? `+${orderDetails.add_users} Employees` : `${orderDetails.user_count} Employees`}
                                                            </span>
                                                        )}
                                                        {(orderDetails.add_storage > 0 || orderDetails.storage_gb > 0) && (
                                                            <span className="px-2 py-1 bg-purple-100 dark:bg-purple-900/30 text-purple-700 dark:text-purple-300 rounded-md text-[10px] font-bold">
                                                                {orderDetails.add_storage > 0 ? `+${orderDetails.add_storage} GB Storage` : `${orderDetails.storage_gb} GB Storage`}
                                                            </span>
                                                        )}
                                                        {orderDetails.duration_months > 0 && (
                                                            <span className="px-2 py-1 bg-green-100 dark:bg-green-900/30 text-green-700 dark:text-green-300 rounded-md text-[10px] font-bold">
                                                                +{orderDetails.duration_months === 1 ? '30 Days' : `${orderDetails.duration_months * 30} Days`} Validity
                                                            </span>
                                                        )}
                                                    </div>
                                                </div>
                                            </div>
                                        </div>
                                    )}

                                    <div className="space-y-4">
                                        <div className="space-y-2">
                                            <label className="text-[10px] font-black text-slate-400 dark:text-gray-500 uppercase tracking-widest px-1">UTR / Transaction Reference</label>
                                            <div className="relative group">
                                                <div className="absolute -inset-1 bg-blue-500/10 rounded-2xl blur opacity-0 group-hover:opacity-100 transition duration-500"></div>
                                                <input
                                                    type="text"
                                                    value={utr}
                                                    onChange={(e) => setUtr(e.target.value)}
                                                    placeholder="Enter 12-digit UTR number"
                                                    className="relative w-full bg-slate-50 dark:bg-gray-700/50 border-2 border-slate-100 dark:border-gray-600 rounded-2xl p-4 text-base font-bold text-slate-800 dark:text-white placeholder:text-slate-300 dark:placeholder:text-gray-500 outline-none focus:border-blue-500 dark:focus:border-blue-500 focus:bg-white dark:focus:bg-gray-800 transition-all"
                                                    autoFocus
                                                />
                                            </div>
                                        </div>

                                        <div className="p-4 rounded-2xl bg-slate-50 dark:bg-gray-700/50 border border-slate-100 dark:border-gray-600 flex gap-3 items-start">
                                            <ShieldCheck size={18} className="text-blue-500 dark:text-blue-400 shrink-0 mt-0.5" />
                                            <div className="space-y-1">
                                                <p className="text-[10px] font-black text-slate-400 dark:text-gray-400 uppercase tracking-widest">Where to find it?</p>
                                                <p className="text-[11px] text-slate-600 dark:text-gray-300 font-medium leading-relaxed">
                                                    Check your payment receipt in GPay, PhonePe, or Paytm for a 12-digit reference number.
                                                </p>
                                            </div>
                                        </div>

                                        {/* Payment Review Notice */}
                                        <div className="p-4 rounded-2xl bg-amber-50 dark:bg-amber-900/20 border border-amber-200 dark:border-amber-800 flex gap-3 items-start">
                                            <AlertCircle size={18} className="text-amber-600 dark:text-amber-500 shrink-0 mt-0.5" />
                                            <div className="space-y-1">
                                                <p className="text-[10px] font-black text-amber-700 dark:text-amber-500 uppercase tracking-widest">Important Notice</p>
                                                <p className="text-[11px] text-amber-800 dark:text-amber-400 font-medium leading-relaxed">
                                                    Your plan will be <span className="font-bold">activated instantly</span> after entering the UTR. However, your payment will be verified within 24 hours. <span className="font-bold text-red-600 dark:text-red-400">If no payment is found or UTR is invalid, your plan will be cancelled and reverted.</span>
                                                </p>
                                            </div>
                                        </div>
                                    </div>
                                </motion.div>
                            )}

                            {/* Other Steps (Confirming/Success) */}
                            {step === 'confirming' && (
                                <motion.div
                                    key="confirming-step"
                                    initial={{ opacity: 0, scale: 0.9 }}
                                    animate={{ opacity: 1, scale: 1 }}
                                    className="py-20 px-8 flex flex-col items-center text-center space-y-8 h-full justify-center"
                                >
                                    <div className="relative">
                                        <div className="w-24 h-24 bg-blue-50 rounded-[2rem] flex items-center justify-center">
                                            <Loader2 size={40} className="text-blue-600 animate-spin" strokeWidth={3} />
                                        </div>
                                        <div className="absolute -inset-4 bg-blue-100 rounded-[2.5rem] animate-ping opacity-20"></div>
                                    </div>
                                    <div className="space-y-3">
                                        <h3 className="text-2xl font-black text-slate-900 dark:text-white">Verifying Transaction</h3>
                                        <p className="text-slate-500 dark:text-gray-400 font-medium max-w-[280px] leading-relaxed">
                                            Communicating with banking servers. Please do not close this window.
                                        </p>
                                    </div>
                                </motion.div>
                            )}

                            {step === 'success' && (
                                <motion.div
                                    key="success-step"
                                    initial={{ opacity: 0, scale: 0.9 }}
                                    animate={{ opacity: 1, scale: 1 }}
                                    className="py-20 px-8 flex flex-col items-center text-center space-y-8 h-full justify-center"
                                >
                                    <div className="relative">
                                        <motion.div
                                            initial={{ scale: 0 }}
                                            animate={{ scale: 1 }}
                                            transition={{ type: 'spring', delay: 0.2 }}
                                            className="w-32 h-32 bg-emerald-500 rounded-[2.5rem] flex items-center justify-center shadow-2xl shadow-emerald-500/40 border-4 border-white dark:border-gray-800"
                                        >
                                            <CheckCircle2 size={64} className="text-white" strokeWidth={2.5} />
                                        </motion.div>
                                        <motion.div
                                            animate={{ scale: [1, 1.2, 1], opacity: [0.5, 0, 0.5] }}
                                            transition={{ duration: 2, repeat: Infinity }}
                                            className="absolute -inset-6 bg-emerald-100 dark:bg-emerald-900/20 rounded-[3rem] -z-10"
                                        />
                                    </div>
                                    <div className="space-y-3">
                                        <h3 className="text-3xl font-black text-slate-900 dark:text-white tracking-tight">Verified!</h3>
                                        <p className="text-slate-500 dark:text-gray-400 font-bold">Your subscription is now active.</p>
                                    </div>
                                    <div className="pt-6">
                                        <div className="inline-flex items-center gap-2 px-4 py-2 bg-slate-100 dark:bg-gray-700/50 rounded-full">
                                            <div className="w-2 h-2 bg-blue-600 dark:bg-blue-400 rounded-full animate-bounce" />
                                            <span className="text-xs font-black text-slate-400 dark:text-gray-400 uppercase tracking-widest">
                                                Closing in {countdown}s
                                            </span>
                                        </div>
                                    </div>
                                </motion.div>
                            )}
                        </AnimatePresence>
                    </div>

                    {/* STICKY FOOTER */}
                    <AnimatePresence mode="wait">
                        {step === 'qr' && (
                            <motion.div
                                key="qr-footer"
                                initial={{ opacity: 0, y: 20 }}
                                animate={{ opacity: 1, y: 0 }}
                                exit={{ opacity: 0, y: 20 }}
                                className="p-6 bg-white dark:bg-gray-800 border-t border-slate-50 dark:border-gray-700 shrink-0 z-30 shadow-[0_-4px_20px_-10px_rgba(0,0,0,0.05)]"
                            >
                                <button
                                    onClick={handleConfirmPayment}
                                    className="w-full h-14 bg-slate-950 dark:bg-black hover:bg-black text-white rounded-2xl font-black text-base shadow-xl transform transition-all active:scale-[0.98] flex items-center justify-center gap-3 relative overflow-hidden group"
                                >
                                    <CheckCircle2 size={20} className="text-blue-400" />
                                    <span>Confirm Payment</span>
                                    <ArrowRight size={18} className="text-white/40 group-hover:translate-x-1 transition-transform" />
                                </button>
                            </motion.div>
                        )}
                        {step === 'utr' && (
                            <motion.div
                                key="utr-footer"
                                initial={{ opacity: 0, y: 20 }}
                                animate={{ opacity: 1, y: 0 }}
                                exit={{ opacity: 0, y: 20 }}
                                className="p-6 bg-white dark:bg-gray-800 border-t border-slate-50 dark:border-gray-700 shrink-0 z-30 shadow-[0_-4px_20px_-10px_rgba(0,0,0,0.05)]"
                            >
                                <button
                                    onClick={handleFinalSubmit}
                                    disabled={!utr.trim()}
                                    className={`w-full h-14 rounded-2xl font-black text-base shadow-xl transform transition-all active:scale-[0.98] flex items-center justify-center gap-3 relative overflow-hidden group ${utr.trim()
                                        ? 'bg-blue-600 hover:bg-blue-700 text-white'
                                        : 'bg-slate-100 dark:bg-gray-700 text-slate-400 dark:text-gray-500 cursor-not-allowed'
                                        }`}
                                >
                                    <ShieldCheck size={20} className={utr.trim() ? "text-blue-200" : "text-slate-300 dark:text-gray-500"} />
                                    <span>Submit Transaction ID</span>
                                    <ArrowRight size={18} className={utr.trim() ? "text-white/40" : "text-slate-300 dark:text-gray-500"} />
                                </button>
                            </motion.div>
                        )}
                    </AnimatePresence>
                </div>


            </motion.div>
        </div>,
        document.body
    );
}

// Add these styles to your index.css if not already present
// .no-scrollbar::-webkit-scrollbar { display: none; }
// .no-scrollbar { -ms-overflow-style: none; scrollbar-width: none; }

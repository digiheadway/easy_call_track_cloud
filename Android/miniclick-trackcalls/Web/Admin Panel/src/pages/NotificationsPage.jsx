import { useState, useEffect } from 'react';
import api from '../api/client';
import { format, parseISO } from 'date-fns';
import { Bell, CheckCircle2, AlertTriangle, Info, XCircle, Trash2, Check, MoreVertical, Calendar, ArrowRight, Loader2, Inbox } from 'lucide-react';
import { toast } from 'sonner';
import { useAuth } from '../context/AuthContext';

export default function NotificationsPage() {
    const { fetchUnreadCount } = useAuth();
    const [notifications, setNotifications] = useState([]);
    const [unreadCount, setUnreadCount] = useState(0);
    const [loading, setLoading] = useState(true);
    const [filter, setFilter] = useState('all'); // 'all', 'unread'

    useEffect(() => {
        fetchNotifications();
    }, []);

    const fetchNotifications = async () => {
        setLoading(true);
        try {
            const res = await api.get('/notifications.php?action=get');
            if (res.status) {
                setNotifications(res.data.notifications);
                setUnreadCount(res.data.unread_count);
            }
        } catch (err) {
            console.error(err);
            toast.error('Failed to load notifications');
        } finally {
            setLoading(false);
        }
    };

    const markAsRead = async (id) => {
        try {
            const res = await api.post('/notifications.php', { action: 'mark_read', id });
            if (res.status) {
                if (id === 'all') {
                    setNotifications(notifications.map(n => ({ ...n, is_read: 1 })));
                    setUnreadCount(0);
                    fetchUnreadCount();
                    toast.success('All marked as read');
                } else {
                    setNotifications(notifications.map(n => n.id === id ? { ...n, is_read: 1 } : n));
                    setUnreadCount(prev => Math.max(0, prev - 1));
                    fetchUnreadCount();
                }
            }
        } catch (err) {
            console.error(err);
        }
    };

    const deleteNotification = async (id) => {
        try {
            const res = await api.post('/notifications.php', { action: 'delete', id });
            if (res.status) {
                setNotifications(notifications.filter(n => n.id !== id));
                const deleted = notifications.find(n => n.id === id);
                if (deleted && !deleted.is_read) {
                    setUnreadCount(prev => Math.max(0, prev - 1));
                    fetchUnreadCount();
                }
                toast.success('Notification deleted');
            }
        } catch (err) {
            console.error(err);
            toast.error('Failed to delete notification');
        }
    };

    const getTypeStyles = (type) => {
        switch (type) {
            case 'success':
                return { icon: CheckCircle2, color: 'text-emerald-600 ', bg: 'bg-emerald-50 ', border: 'border-emerald-100 ' };
            case 'warning':
                return { icon: AlertTriangle, color: 'text-amber-600 ', bg: 'bg-amber-50 ', border: 'border-amber-100 ' };
            case 'error':
                return { icon: XCircle, color: 'text-red-600 ', bg: 'bg-red-50 ', border: 'border-red-100 ' };
            default:
                return { icon: Info, color: 'text-blue-600 ', bg: 'bg-blue-50 ', border: 'border-blue-100 ' };
        }
    };

    const filteredNotifications = filter === 'unread'
        ? notifications.filter(n => !n.is_read)
        : notifications;

    return (
        <div className="max-w-4xl mx-auto space-y-8 animate-in fade-in duration-500">
            {/* Header */}
            <header className="flex flex-col md:flex-row md:items-center justify-between gap-4">
                <div className="space-y-1">
                    <h1 className="text-3xl font-black tracking-tight text-gray-900  flex items-center gap-3">
                        Notifications
                        {unreadCount > 0 && (
                            <span className="px-2.5 py-0.5 bg-blue-600  text-white text-[10px] font-black rounded-full uppercase tracking-widest">
                                {unreadCount} New
                            </span>
                        )}
                    </h1>
                    <p className="text-gray-500  font-medium">Stay updated with your account and system activities.</p>
                </div>

                <div className="flex items-center gap-2">
                    {unreadCount > 0 && (
                        <button
                            onClick={() => markAsRead('all')}
                            className="flex items-center gap-2 px-4 py-2 bg-white  border border-gray-200  text-gray-700  rounded-xl text-xs font-bold hover:bg-gray-50  transition-all active:scale-95"
                        >
                            <Check size={14} />
                            Mark all as read
                        </button>
                    )}
                    <button
                        onClick={fetchNotifications}
                        className="p-2 bg-white  border border-gray-200  text-gray-500  rounded-xl hover:text-blue-600  active:rotate-180 transition-all duration-500"
                    >
                        {loading ? <Loader2 size={20} className="animate-spin" /> : <Bell size={20} />}
                    </button>
                </div>
            </header>

            {/* Filters */}
            <div className="flex items-center gap-1 p-1 bg-gray-100/50  rounded-2xl w-fit">
                <button
                    onClick={() => setFilter('all')}
                    className={`px-6 py-2 rounded-xl text-xs font-black uppercase tracking-widest transition-all ${filter === 'all' ? 'bg-white  text-blue-600  shadow-sm' : 'text-gray-500  hover:text-gray-900 '}`}
                >
                    All
                </button>
                <button
                    onClick={() => setFilter('unread')}
                    className={`px-6 py-2 rounded-xl text-xs font-black uppercase tracking-widest transition-all ${filter === 'unread' ? 'bg-white  text-blue-600  shadow-sm' : 'text-gray-500  hover:text-gray-900 '}`}
                >
                    Unread
                </button>
            </div>

            {/* Notifications List */}
            <div className="space-y-3">
                {loading && notifications.length === 0 ? (
                    <div className="flex flex-col items-center justify-center py-20 bg-white  rounded-[2rem] border border-gray-100  border-dashed">
                        <Loader2 size={40} className="text-blue-200  animate-spin mb-4" />
                        <p className="text-gray-400  font-bold uppercase tracking-widest text-[10px]">Loading notifications...</p>
                    </div>
                ) : filteredNotifications.length === 0 ? (
                    <div className="flex flex-col items-center justify-center py-20 bg-white  rounded-[2rem] border border-gray-100  border-dashed animate-in fade-in zoom-in-95 duration-500">
                        <div className="w-16 h-16 bg-gray-50  text-gray-300  rounded-2xl flex items-center justify-center mb-4">
                            <Inbox size={32} />
                        </div>
                        <p className="text-gray-900  font-black uppercase tracking-widest text-sm">No Notifications</p>
                        <p className="text-gray-400  text-xs mt-1">You're all caught up!</p>
                    </div>
                ) : (
                    filteredNotifications.map((notification) => {
                        const styles = getTypeStyles(notification.type);
                        const Icon = styles.icon;

                        return (
                            <div
                                key={notification.id}
                                className={`group relative p-5 bg-white  rounded-[1.5rem] border transition-all duration-300 hover:shadow-xl hover:shadow-gray-100/50  flex gap-5 ${notification.is_read ? 'border-gray-100  opacity-80' : 'border-blue-100  shadow-lg shadow-blue-500/5  ring-1 ring-blue-50  bg-gradient-to-br from-white to-blue-50/10  '}`}
                            >
                                {/* Left Icon */}
                                <div className={`flex-shrink-0 w-12 h-12 rounded-2xl ${styles.bg} ${styles.color} flex items-center justify-center transition-transform group-hover:scale-110 shadow-sm`}>
                                    <Icon size={24} />
                                </div>

                                {/* Content */}
                                <div className="flex-1 min-w-0 space-y-1">
                                    <div className="flex items-center justify-between gap-4">
                                        <h3 className={`font-black text-gray-900  leading-none truncate ${!notification.is_read ? 'text-lg' : 'text-base'}`}>
                                            {notification.title}
                                        </h3>
                                        <span className="text-[10px] font-bold text-gray-400  flex items-center gap-1.5 whitespace-nowrap bg-gray-50  px-2 py-1 rounded-lg">
                                            <Calendar size={12} />
                                            {format(parseISO(notification.created_at.replace(' ', 'T')), 'MMM d, h:mm a')}
                                        </span>
                                    </div>
                                    <p className="text-gray-600  text-sm font-medium leading-relaxed">
                                        {notification.message}
                                    </p>

                                    {notification.link && (
                                        <a
                                            href={notification.link}
                                            className="inline-flex items-center gap-1 text-[10px] font-black uppercase tracking-widest text-blue-600  mt-2 hover:gap-2 transition-all"
                                        >
                                            View Details
                                            <ArrowRight size={12} />
                                        </a>
                                    )}
                                </div>

                                {/* Actions */}
                                <div className="hidden group-hover:flex flex-col gap-2 animate-in fade-in slide-in-from-right-2 duration-300">
                                    {!notification.is_read && (
                                        <button
                                            onClick={() => markAsRead(notification.id)}
                                            className="w-8 h-8 bg-blue-50  text-blue-600  rounded-xl flex items-center justify-center hover:bg-blue-600 hover:text-white   transition-all shadow-sm"
                                            title="Mark as read"
                                        >
                                            <Check size={16} />
                                        </button>
                                    )}
                                    <button
                                        onClick={() => deleteNotification(notification.id)}
                                        className="w-8 h-8 bg-red-50  text-red-500  rounded-xl flex items-center justify-center hover:bg-red-500 hover:text-white   transition-all shadow-sm"
                                        title="Delete"
                                    >
                                        <Trash2 size={16} />
                                    </button>
                                </div>

                                {/* Unread Dot */}
                                {!notification.is_read && (
                                    <div className="absolute top-4 left-4 w-3 h-3 bg-blue-600 rounded-full border-2 border-white  shadow-sm -translate-x-1/2 -translate-y-1/2" />
                                )}
                            </div>
                        );
                    })
                )}
            </div>

            {/* Legend/Info Footer */}
            {!loading && notifications.length > 0 && (
                <footer className="pt-8 border-t border-gray-100  flex flex-wrap gap-6 justify-center opacity-50">
                    <div className="flex items-center gap-2 text-[10px] font-black uppercase tracking-widest text-gray-400 ">
                        <div className="w-2 h-2 rounded-full bg-emerald-500" /> Success
                    </div>
                    <div className="flex items-center gap-2 text-[10px] font-black uppercase tracking-widest text-gray-400 ">
                        <div className="w-2 h-2 rounded-full bg-amber-500" /> Warning
                    </div>
                    <div className="flex items-center gap-2 text-[10px] font-black uppercase tracking-widest text-gray-400 ">
                        <div className="w-2 h-2 rounded-full bg-red-500" /> Error
                    </div>
                    <div className="flex items-center gap-2 text-[10px] font-black uppercase tracking-widest text-gray-400 ">
                        <div className="w-2 h-2 rounded-full bg-blue-500" /> Info
                    </div>
                </footer>
            )}
        </div>
    );
}

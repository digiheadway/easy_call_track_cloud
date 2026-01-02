import { useState, useRef, useEffect } from 'react';
import { motion } from 'framer-motion';
import { useAudioPlayer } from '../context/AudioPlayerContext';
import { usePersonModal } from '../context/PersonModalContext';
import { Play, Pause, X, User, ExternalLink, Activity } from 'lucide-react';

export default function FloatingAudioPlayer() {
    const { currentCall, stopRecording, isPlaying, setIsPlaying, togglePlay } = useAudioPlayer();
    const { openPersonModal } = usePersonModal();
    const audioRef = useRef(null);

    const [currentTime, setCurrentTime] = useState(0);
    const [duration, setDuration] = useState(0);
    const [speed, setSpeed] = useState(1);
    const [isDragging, setIsDragging] = useState(false);

    // Handle Source Change and Auto-Play
    useEffect(() => {
        if (currentCall && audioRef.current) {
            // Only update src if it's different to avoid reloading
            if (audioRef.current.src !== currentCall.recording_url) {
                audioRef.current.src = currentCall.recording_url;
                audioRef.current.playbackRate = speed;
            }
        } else {
            setCurrentTime(0);
            setDuration(0);
        }
    }, [currentCall]);

    // Handle Play/Pause based on Context
    useEffect(() => {
        if (!audioRef.current || !currentCall) return;

        if (isPlaying) {
            audioRef.current.play().catch(e => {
                console.error("Playback failed", e);
                setIsPlaying(false);
            });
        } else {
            audioRef.current.pause();
        }
    }, [isPlaying, currentCall]); // Depend on isPlaying

    // Update playback rate when speed changes
    useEffect(() => {
        if (audioRef.current) {
            audioRef.current.playbackRate = speed;
        }
    }, [speed]);

    const handleTimeUpdate = () => {
        if (audioRef.current) {
            setCurrentTime(audioRef.current.currentTime);
            setDuration(audioRef.current.duration || 0);
        }
    };

    const handleSeek = (e) => {
        const time = parseFloat(e.target.value);
        if (audioRef.current) {
            audioRef.current.currentTime = time;
            setCurrentTime(time);
        }
    };

    const handleEnded = () => {
        setIsPlaying(false);
        setCurrentTime(0);
    };

    const formatTime = (time) => {
        if (!time) return '0:00';
        const minutes = Math.floor(time / 60);
        const seconds = Math.floor(time % 60);
        return `${minutes}:${seconds.toString().padStart(2, '0')}`;
    };

    const cycleSpeed = () => {
        const speeds = [1, 1.25, 1.5, 2];
        const nextIdx = (speeds.indexOf(speed) + 1) % speeds.length;
        setSpeed(speeds[nextIdx]);
    };

    if (!currentCall) return null;

    return (
        <motion.div
            drag
            dragMomentum={false}
            onDragStart={() => setIsDragging(true)}
            onDragEnd={() => setIsDragging(false)}
            initial={{ x: 0, y: 0 }}
            className="fixed bottom-6 right-6 z-[9999] bg-white rounded-xl shadow-2xl border border-gray-100 w-[420px] overflow-hidden cursor-move font-sans"
            style={{ touchAction: 'none' }}
        >
            <div className="p-3 space-y-3">
                {/* Row 1: Info & Close */}
                <div className="flex items-center justify-between gap-3">
                    <div className="flex items-center gap-2 min-w-0 flex-1">
                        {/* Pulse */}
                        <div className="w-2 h-2 rounded-full bg-blue-500 animate-pulse shrink-0"></div>

                        {/* Name (Clickable) */}
                        <button
                            onClick={(e) => { e.stopPropagation(); openPersonModal(currentCall); }}
                            className="font-semibold text-gray-900 truncate hover:text-blue-600 transition-colors text-sm text-left"
                            title="View Contact Details"
                        >
                            {currentCall.contact_name || currentCall.phone_number || 'Unknown'}
                        </button>

                        {/* Type Badge */}
                        <span className={`text-[10px] font-medium px-1.5 py-0.5 rounded border uppercase shrink-0 ${(currentCall.type || '').toLowerCase().includes('in') ? 'bg-blue-50 text-blue-600 border-blue-100' :
                            (currentCall.type || '').toLowerCase().includes('miss') ? 'bg-red-50 text-red-600 border-red-100' :
                                'bg-green-50 text-green-600 border-green-100'
                            }`}>
                            {currentCall.type}
                        </span>
                    </div>

                    {/* Close */}
                    <button
                        onClick={(e) => { e.stopPropagation(); stopRecording(); }}
                        className="text-gray-400 hover:text-red-500 p-1 rounded-full hover:bg-gray-100 transition-colors shrink-0"
                    >
                        <X size={16} />
                    </button>
                </div>

                {/* Row 2: Controls */}
                <div className="flex items-center gap-3" onPointerDown={e => e.stopPropagation()}>
                    {/* Play/Pause */}
                    <button
                        onClick={togglePlay}
                        className="w-8 h-8 rounded-full bg-blue-600 hover:bg-blue-700 text-white flex items-center justify-center shrink-0 shadow-sm transition-all active:scale-95"
                    >
                        {isPlaying ? <Pause size={14} fill="currentColor" /> : <Play size={14} fill="currentColor" className="ml-0.5" />}
                    </button>

                    {/* Current Time */}
                    <span className="text-xs font-mono text-gray-600 min-w-[32px] text-right">{formatTime(currentTime)}</span>

                    {/* Seekbar */}
                    <div className="flex-1 h-3 flex items-center group/seek">
                        <input
                            type="range"
                            min="0"
                            max={duration || 100}
                            value={currentTime}
                            onChange={handleSeek}
                            className="w-full h-1 bg-gray-200 rounded-lg appearance-none cursor-pointer accent-blue-600 hover:accent-blue-700 transition-all hover:h-1.5"
                        />
                    </div>

                    {/* Total Time */}
                    <span className="text-xs font-mono text-gray-600 min-w-[32px]">{formatTime(duration)}</span>

                    {/* Speed */}
                    <button
                        onClick={cycleSpeed}
                        className="text-[10px] font-bold text-gray-500 hover:text-blue-600 bg-gray-100 hover:bg-blue-50 px-1.5 py-1 rounded w-8 text-center transition-colors shrink-0"
                    >
                        {speed}x
                    </button>
                </div>
            </div>

            <audio
                ref={audioRef}
                onTimeUpdate={handleTimeUpdate}
                onEnded={handleEnded}
                onLoadedMetadata={handleTimeUpdate}
                preload="auto"
            />
        </motion.div>
    );
}

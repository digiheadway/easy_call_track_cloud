import React, { useState, useRef, useEffect } from 'react';
import { Play, Pause } from 'lucide-react';

interface AudioPlayerProps {
    src: string;
    onPlay?: () => void;
    onPause?: () => void;
    isPlayingProp?: boolean; // Controlled state if needed
}

const AudioPlayer: React.FC<AudioPlayerProps> = ({ src, onPlay, onPause, isPlayingProp }) => {
    const audioRef = useRef<HTMLAudioElement>(null);
    const [isPlaying, setIsPlaying] = useState(false);
    const [currentTime, setCurrentTime] = useState(0);
    const [duration, setDuration] = useState(0);
    const [playbackRate, setPlaybackRate] = useState(1);
    const [showSpeedMenu, setShowSpeedMenu] = useState(false);

    // Sync with external controlled state if provided
    useEffect(() => {
        if (isPlayingProp !== undefined) {
            setIsPlaying(isPlayingProp);
            if (isPlayingProp) {
                audioRef.current?.play().catch(e => console.error("Play error:", e));
            } else {
                audioRef.current?.pause();
            }
        }
    }, [isPlayingProp]);

    const togglePlay = () => {
        if (isPlaying) {
            audioRef.current?.pause();
            if (onPause) onPause();
            if (isPlayingProp === undefined) setIsPlaying(false);
        } else {
            audioRef.current?.play().catch(e => console.error("Play error:", e));
            if (onPlay) onPlay();
            if (isPlayingProp === undefined) setIsPlaying(true);
        }
    };

    const handleTimeUpdate = () => {
        if (audioRef.current) {
            setCurrentTime(audioRef.current.currentTime);
        }
    };

    const handleLoadedMetadata = () => {
        if (audioRef.current) {
            setDuration(audioRef.current.duration);
        }
    };

    const handleSeek = (e: React.ChangeEvent<HTMLInputElement>) => {
        const time = parseFloat(e.target.value);
        if (audioRef.current) {
            audioRef.current.currentTime = time;
            setCurrentTime(time);
        }
    };

    const handleSpeedChange = (rate: number) => {
        if (audioRef.current) {
            audioRef.current.playbackRate = rate;
            setPlaybackRate(rate);
            setShowSpeedMenu(false);
        }
    };

    const handleEnded = () => {
        if (isPlayingProp === undefined) setIsPlaying(false);
        if (onPause) onPause();
    };

    const formatTime = (time: number) => {
        if (isNaN(time)) return "0:00";
        const minutes = Math.floor(time / 60);
        const seconds = Math.floor(time % 60);
        return `${minutes}:${seconds.toString().padStart(2, '0')}`;
    };

    return (
        <div className="flex items-center gap-2 bg-gray-100 rounded-full px-3 py-1.5 min-w-[200px]">
            <audio
                ref={audioRef}
                src={src}
                onTimeUpdate={handleTimeUpdate}
                onLoadedMetadata={handleLoadedMetadata}
                onEnded={handleEnded}
                className="hidden"
            />

            <button
                onClick={togglePlay}
                className="p-1 text-gray-700 hover:text-blue-600 transition-colors flex-shrink-0"
            >
                {isPlaying ? <Pause size={16} fill="currentColor" /> : <Play size={16} fill="currentColor" />}
            </button>

            <div className="text-xs text-gray-500 font-medium w-8 text-center flex-shrink-0">
                {formatTime(currentTime)}
            </div>

            <input
                type="range"
                min="0"
                max={duration || 0}
                value={currentTime}
                onChange={handleSeek}
                className="w-full h-1 bg-gray-300 rounded-lg appearance-none cursor-pointer accent-blue-600"
            />

            <div className="relative flex-shrink-0">
                <button
                    onClick={() => setShowSpeedMenu(!showSpeedMenu)}
                    className="text-xs font-bold text-gray-600 hover:text-blue-600 px-1"
                >
                    {playbackRate}x
                </button>

                {showSpeedMenu && (
                    <div className="absolute bottom-full right-0 mb-2 bg-white rounded-lg shadow-lg border border-gray-200 py-1 z-10 min-w-[60px]">
                        {[1, 1.5, 2].map(rate => (
                            <button
                                key={rate}
                                onClick={() => handleSpeedChange(rate)}
                                className={`block w-full text-left px-3 py-1 text-xs hover:bg-gray-50 ${playbackRate === rate ? 'text-blue-600 font-bold' : 'text-gray-700'}`}
                            >
                                {rate}x
                            </button>
                        ))}
                    </div>
                )}
            </div>
        </div>
    );
};

export default AudioPlayer;

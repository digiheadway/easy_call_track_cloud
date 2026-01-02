import { createContext, useContext, useState, useRef, useEffect } from 'react';

const AudioPlayerContext = createContext();

export function AudioPlayerProvider({ children }) {
    const [currentCall, setCurrentCall] = useState(null);
    const [isPlaying, setIsPlaying] = useState(false);
    const [currentTime, setCurrentTime] = useState(0);
    const [duration, setDuration] = useState(0);
    const [playbackRate, setPlaybackRate] = useState(1);
    const audioRef = useRef(new Audio());

    useEffect(() => {
        const audio = audioRef.current;

        const handleTimeUpdate = () => setCurrentTime(audio.currentTime);
        const handleDurationChange = () => setDuration(audio.duration);
        const handleEnded = () => {
            setIsPlaying(false);
            setCurrentTime(0);
        };

        audio.addEventListener('timeupdate', handleTimeUpdate);
        audio.addEventListener('durationchange', handleDurationChange);
        audio.addEventListener('ended', handleEnded);

        return () => {
            audio.removeEventListener('timeupdate', handleTimeUpdate);
            audio.removeEventListener('durationchange', handleDurationChange);
            audio.removeEventListener('ended', handleEnded);
            audio.pause();
        };
    }, []);

    useEffect(() => {
        if (currentCall?.recording_url) {
            if (audioRef.current.src !== currentCall.recording_url) {
                audioRef.current.src = currentCall.recording_url;
                audioRef.current.load();
                if (isPlaying) {
                    audioRef.current.play().catch(console.error);
                }
            }
        } else {
            audioRef.current.pause();
            audioRef.current.src = "";
        }
    }, [currentCall]);

    useEffect(() => {
        if (isPlaying) {
            audioRef.current.play().catch(err => {
                console.error("Playback error:", err);
                setIsPlaying(false);
            });
        } else {
            audioRef.current.pause();
        }
    }, [isPlaying]);

    useEffect(() => {
        audioRef.current.playbackRate = playbackRate;
    }, [playbackRate]);

    const playRecording = (call) => {
        if (currentCall?.id === call.id) {
            setIsPlaying(!isPlaying);
        } else {
            setCurrentCall(call);
            setIsPlaying(true);
        }
    };

    const stopRecording = () => {
        setCurrentCall(null);
        setIsPlaying(false);
        setCurrentTime(0);
    };

    const togglePlay = () => {
        setIsPlaying(prev => !prev);
    };

    const seek = (time) => {
        audioRef.current.currentTime = time;
        setCurrentTime(time);
    };

    return (
        <AudioPlayerContext.Provider value={{
            currentCall,
            isPlaying,
            setIsPlaying,
            playRecording,
            stopRecording,
            togglePlay,
            currentTime,
            duration,
            playbackRate,
            setPlaybackRate,
            seek
        }}>
            {children}
        </AudioPlayerContext.Provider>
    );
}

export function useAudioPlayer() {
    return useContext(AudioPlayerContext);
}

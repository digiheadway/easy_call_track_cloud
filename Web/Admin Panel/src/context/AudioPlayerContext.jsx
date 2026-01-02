import { createContext, useContext, useState } from 'react';

const AudioPlayerContext = createContext();

export function AudioPlayerProvider({ children }) {
    const [currentCall, setCurrentCall] = useState(null);
    const [isPlaying, setIsPlaying] = useState(false);

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
    };

    const togglePlay = () => {
        setIsPlaying(prev => !prev);
    };

    return (
        <AudioPlayerContext.Provider value={{ currentCall, isPlaying, setIsPlaying, playRecording, stopRecording, togglePlay }}>
            {children}
        </AudioPlayerContext.Provider>
    );
}

export function useAudioPlayer() {
    return useContext(AudioPlayerContext);
}

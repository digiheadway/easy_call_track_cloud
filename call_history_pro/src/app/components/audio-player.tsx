
'use client';
import { useState, useRef, useEffect } from 'react';
import { Play, Pause, FastForward, Rewind, Volume2 } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Slider } from '@/components/ui/slider';
import { DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuTrigger } from '@/components/ui/dropdown-menu';

interface AudioPlayerProps {
    src: string;
    id: string;
    currentlyPlaying: string | null;
    setCurrentlyPlaying: (id: string | null) => void;
}

function formatTime(seconds: number) {
  const floorSeconds = Math.floor(seconds);
  const min = Math.floor(floorSeconds / 60);
  const sec = floorSeconds % 60;
  return `${min}:${sec < 10 ? '0' : ''}${sec}`;
}

const playbackRates = [0.5, 1, 1.5, 2];

export function AudioPlayer({ src, id, currentlyPlaying, setCurrentlyPlaying }: AudioPlayerProps) {
  const audioRef = useRef<HTMLAudioElement | null>(null);
  const [isPlaying, setIsPlaying] = useState(false);
  const [duration, setDuration] = useState(0);
  const [currentTime, setCurrentTime] = useState(0);
  const [playbackRate, setPlaybackRate] = useState(1);
  const isCurrentlyPlayingThis = currentlyPlaying === id;

  useEffect(() => {
    const audio = new Audio(src);
    audioRef.current = audio;

    const setAudioData = () => {
      setDuration(audio.duration);
      setCurrentTime(audio.currentTime);
    }

    const setAudioTime = () => setCurrentTime(audio.currentTime);

    const handleEnded = () => {
      setIsPlaying(false);
      setCurrentlyPlaying(null);
    };

    audio.addEventListener('loadeddata', setAudioData);
    audio.addEventListener('timeupdate', setAudioTime);
    audio.addEventListener('ended', handleEnded);

    return () => {
      audio.pause();
      audio.removeEventListener('loadeddata', setAudioData);
      audio.removeEventListener('timeupdate', setAudioTime);
      audio.removeEventListener('ended', handleEnded);
      if (isCurrentlyPlayingThis) {
          setCurrentlyPlaying(null);
      }
    };
  }, [src, id, setCurrentlyPlaying]);
  
  useEffect(() => {
      if (!isCurrentlyPlayingThis && isPlaying) {
          audioRef.current?.pause();
          setIsPlaying(false);
      }
  },[currentlyPlaying, id, isPlaying, isCurrentlyPlayingThis]);


  const handlePlayPause = () => {
    if (audioRef.current) {
      if (isPlaying) {
        audioRef.current.pause();
        setCurrentlyPlaying(null);
      } else {
        audioRef.current.play().catch(e => console.error("Playback failed:", e));
        setCurrentlyPlaying(id);
      }
      setIsPlaying(!isPlaying);
    }
  };

  const handleSeek = (value: number[]) => {
    if (audioRef.current) {
      audioRef.current.currentTime = value[0];
      setCurrentTime(value[0]);
    }
  };
  
  const handlePlaybackRateChange = (rate: number) => {
      if(audioRef.current){
          audioRef.current.playbackRate = rate;
          setPlaybackRate(rate);
      }
  }

  return (
    <div className="mt-2 space-y-2 rounded-lg border p-2">
      <div className="flex items-center gap-2">
        <Button onClick={handlePlayPause} variant="ghost" size="icon" className="h-8 w-8">
            {isPlaying ? <Pause className="h-4 w-4" /> : <Play className="h-4 w-4" />}
        </Button>
        <div className="flex w-full items-center gap-2">
            <span className="text-xs text-muted-foreground w-10">{formatTime(currentTime)}</span>
            <Slider
                value={[currentTime]}
                max={duration || 1}
                step={1}
                onValueChange={handleSeek}
                className="w-full"
            />
            <span className="text-xs text-muted-foreground w-10">{formatTime(duration)}</span>
        </div>
        <DropdownMenu>
            <DropdownMenuTrigger asChild>
                 <Button variant="ghost" size="sm" className="w-16 text-xs">{playbackRate}x</Button>
            </DropdownMenuTrigger>
            <DropdownMenuContent>
                {playbackRates.map(rate => (
                     <DropdownMenuItem key={rate} onClick={() => handlePlaybackRateChange(rate)}>
                        {rate}x
                     </DropdownMenuItem>
                ))}
            </DropdownMenuContent>
        </DropdownMenu>
      </div>
    </div>
  );
}

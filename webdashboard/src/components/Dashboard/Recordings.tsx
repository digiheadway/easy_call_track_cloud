import { useState } from 'react';
import './Pages.css';

interface RecordingsProps {
    organizationId: string;
}

interface Recording {
    id: string;
    title: string;
    employee: string;
    contact: string;
    duration: string;
    fileSize: string;
    timestamp: string;
    tags: string[];
}

export default function Recordings({ organizationId }: RecordingsProps) {
    const [searchQuery, setSearchQuery] = useState('');
    const [playingId, setPlayingId] = useState<string | null>(null);
    const [currentTime, setCurrentTime] = useState(0);

    // Mock data
    const recordings: Recording[] = [
        {
            id: '1',
            title: 'Sales Call - ABC Corp',
            employee: 'John Smith',
            contact: 'ABC Corporation',
            duration: '12:34',
            fileSize: '8.2 MB',
            timestamp: '2024-12-31 14:30:00',
            tags: ['Sales', 'Important']
        },
        {
            id: '2',
            title: 'Client Meeting - XYZ',
            employee: 'Sarah Johnson',
            contact: 'XYZ Industries',
            duration: '8:45',
            fileSize: '5.8 MB',
            timestamp: '2024-12-31 13:15:00',
            tags: ['Meeting', 'Follow-up']
        },
        {
            id: '3',
            title: 'Support Call - Tech Solutions',
            employee: 'David Lee',
            contact: 'Tech Solutions Ltd',
            duration: '5:22',
            fileSize: '3.5 MB',
            timestamp: '2024-12-31 12:45:00',
            tags: ['Support']
        },
        {
            id: '4',
            title: 'Product Demo - Global Services',
            employee: 'James Brown',
            contact: 'Innovation Hub',
            duration: '15:12',
            fileSize: '10.1 MB',
            timestamp: '2024-12-31 10:05:00',
            tags: ['Demo', 'Product']
        },
    ];

    const filteredRecordings = recordings.filter(rec =>
        rec.title.toLowerCase().includes(searchQuery.toLowerCase()) ||
        rec.employee.toLowerCase().includes(searchQuery.toLowerCase()) ||
        rec.contact.toLowerCase().includes(searchQuery.toLowerCase()) ||
        rec.tags.some(tag => tag.toLowerCase().includes(searchQuery.toLowerCase()))
    );

    const handlePlayPause = (id: string) => {
        if (playingId === id) {
            setPlayingId(null);
        } else {
            setPlayingId(id);
            setCurrentTime(0);
        }
    };

    const handleDownload = (recording: Recording) => {
        alert(`Downloading ${recording.title}...`);
    };

    return (
        <div className="page-container">
            {/* Header */}
            <div className="page-header">
                <div className="search-bar">
                    <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                        <circle cx="11" cy="11" r="8" />
                        <path d="m21 21-4.35-4.35" />
                    </svg>
                    <input
                        type="text"
                        placeholder="Search recordings..."
                        value={searchQuery}
                        onChange={(e) => setSearchQuery(e.target.value)}
                    />
                </div>
                <button className="btn btn-primary">
                    <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                        <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4" />
                        <polyline points="17 8 12 3 7 8" />
                        <line x1="12" y1="3" x2="12" y2="15" />
                    </svg>
                    Upload Recording
                </button>
            </div>

            {/* Recording Stats */}
            <div className="recording-stats">
                <div className="stat-box">
                    <div className="stat-number">{recordings.length}</div>
                    <div className="stat-label">Total Recordings</div>
                </div>
                <div className="stat-box">
                    <div className="stat-number">142.5 GB</div>
                    <div className="stat-label">Storage Used</div>
                </div>
                <div className="stat-box">
                    <div className="stat-number">28:45:12</div>
                    <div className="stat-label">Total Duration</div>
                </div>
            </div>

            {/* Recordings Grid */}
            <div className="recordings-grid">
                {filteredRecordings.map((recording) => (
                    <div key={recording.id} className="recording-card card">
                        <div className="recording-header">
                            <div className="recording-icon">
                                <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                                    <circle cx="12" cy="12" r="10" />
                                    <polygon points="10 8 16 12 10 16 10 8" fill="currentColor" />
                                </svg>
                            </div>
                            <div className="recording-info">
                                <div className="recording-title">{recording.title}</div>
                                <div className="recording-meta">
                                    {recording.employee} • {recording.contact}
                                </div>
                            </div>
                        </div>

                        <div className="recording-player">
                            <button
                                className="play-button"
                                onClick={() => handlePlayPause(recording.id)}
                            >
                                {playingId === recording.id ? (
                                    <svg width="24" height="24" viewBox="0 0 24 24" fill="currentColor">
                                        <rect x="6" y="4" width="4" height="16" />
                                        <rect x="14" y="4" width="4" height="16" />
                                    </svg>
                                ) : (
                                    <svg width="24" height="24" viewBox="0 0 24 24" fill="currentColor">
                                        <polygon points="5 3 19 12 5 21 5 3" />
                                    </svg>
                                )}
                            </button>

                            <div className="player-timeline">
                                <div className="timeline-bar">
                                    <div
                                        className="timeline-progress"
                                        style={{ width: playingId === recording.id ? `${(currentTime / 100) * 100}%` : '0%' }}
                                    ></div>
                                </div>
                                <div className="timeline-time">
                                    <span>0:00</span>
                                    <span>{recording.duration}</span>
                                </div>
                            </div>
                        </div>

                        <div className="recording-details">
                            <div className="detail-item">
                                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                                    <circle cx="12" cy="12" r="10" />
                                    <polyline points="12 6 12 12 16 14" />
                                </svg>
                                <span>{recording.duration}</span>
                            </div>
                            <div className="detail-item">
                                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                                    <path d="M13 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V9z" />
                                    <polyline points="13 2 13 9 20 9" />
                                </svg>
                                <span>{recording.fileSize}</span>
                            </div>
                            <div className="detail-item">
                                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                                    <rect x="3" y="4" width="18" height="18" rx="2" ry="2" />
                                    <line x1="16" y1="2" x2="16" y2="6" />
                                    <line x1="8" y1="2" x2="8" y2="6" />
                                    <line x1="3" y1="10" x2="21" y2="10" />
                                </svg>
                                <span>{new Date(recording.timestamp).toLocaleDateString()}</span>
                            </div>
                        </div>

                        <div className="recording-tags">
                            {recording.tags.map((tag, index) => (
                                <span key={index} className="badge badge-primary">{tag}</span>
                            ))}
                        </div>

                        <div className="recording-actions">
                            <button className="btn btn-secondary" onClick={() => handleDownload(recording)}>
                                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                                    <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4" />
                                    <polyline points="7 10 12 15 17 10" />
                                    <line x1="12" y1="15" x2="12" y2="3" />
                                </svg>
                                Download
                            </button>
                            <button className="btn btn-ghost">
                                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                                    <circle cx="18" cy="5" r="3" />
                                    <circle cx="6" cy="12" r="3" />
                                    <circle cx="18" cy="19" r="3" />
                                    <line x1="8.59" y1="13.51" x2="15.42" y2="17.49" />
                                    <line x1="15.41" y1="6.51" x2="8.59" y2="10.49" />
                                </svg>
                                Share
                            </button>
                        </div>
                    </div>
                ))}
            </div>
        </div>
    );
}

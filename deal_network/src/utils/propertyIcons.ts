import {
    Globe, Lock, Clock, Tag, Heart, MessageSquare,
    Navigation, Flame, Compass, TreeDeciduous, Map, LucideIcon
} from 'lucide-react';

/**
 * Get highlight icon based on text content
 */
export function getHighlightIcon(text: string): LucideIcon | null {
    const textLower = text.toLowerCase();
    if (textLower.includes('corner')) return Navigation;
    if (textLower.includes('park')) return TreeDeciduous;
    if (textLower.includes('urgent')) return Flame;
    if (textLower.includes('road') || textLower.includes('meter')) return Navigation;
    if (textLower.includes('facing')) return Compass;
    if (textLower.includes('plot')) return Map;
    return null;
}

// Re-export commonly used icons for convenience
export {
    Globe,
    Lock,
    Clock,
    Tag,
    Heart,
    MessageSquare,
    Navigation,
    Flame,
    Compass,
    TreeDeciduous,
    Map
};

export type { LucideIcon };

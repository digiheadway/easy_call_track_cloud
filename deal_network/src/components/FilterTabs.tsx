import { Home, User, Globe, Heart } from 'lucide-react';

export type FilterType = 'all' | 'my' | 'public' | 'saved';

interface FilterTabsProps {
    activeFilter: FilterType;
    onFilterChange: (filter: FilterType) => void;
    counts?: {
        all: number;
        my: number;
        public: number;
        saved: number;
    };
}

export function FilterTabs({ activeFilter, onFilterChange, counts }: FilterTabsProps) {
    const tabs: { id: FilterType; label: string; icon: typeof Home }[] = [
        { id: 'all', label: 'All', icon: Home },
        { id: 'my', label: 'My Properties', icon: User },
        { id: 'public', label: 'Public', icon: Globe },
        { id: 'saved', label: 'Saved', icon: Heart },
    ];

    return (
        <div className="flex gap-2 p-4 overflow-x-auto bg-white border-b">
            {tabs.map((tab) => {
                const Icon = tab.icon;
                const isActive = activeFilter === tab.id;
                const count = counts?.[tab.id];

                return (
                    <button
                        key={tab.id}
                        onClick={() => onFilterChange(tab.id)}
                        className={`
              flex items-center gap-2 px-4 py-2 rounded-lg font-medium whitespace-nowrap
              transition-all duration-200
              ${isActive
                                ? 'bg-blue-600 text-white shadow-md'
                                : 'bg-gray-100 text-gray-700 hover:bg-gray-200'
                            }
            `}
                    >
                        <Icon className="w-4 h-4" />
                        <span>{tab.label}</span>
                        {count !== undefined && count > 0 && (
                            <span className={`
                text-xs px-2 py-0.5 rounded-full
                ${isActive ? 'bg-blue-500 text-white' : 'bg-gray-200 text-gray-600'}
              `}>
                                {count}
                            </span>
                        )}
                    </button>
                );
            })}
        </div>
    );
}

import { ChevronLeft, ChevronRight } from 'lucide-react';

interface PaginationMeta {
    currentPage: number;
    totalPages: number;
    totalItems: number;
    itemsPerPage: number;
}

interface PaginationControlsProps {
    meta: PaginationMeta | null;
    onPageChange: (page: number) => void;
    loading?: boolean;
}

export function PaginationControls({ meta, onPageChange, loading }: PaginationControlsProps) {
    if (!meta || meta.totalPages <= 1) {
        return null;
    }

    const { currentPage, totalPages, totalItems } = meta;
    const canGoPrev = currentPage > 1;
    const canGoNext = currentPage < totalPages;

    return (
        <div className="flex items-center justify-between px-4 py-3 bg-white border-t">
            <div className="text-sm text-gray-600">
                Page {currentPage} of {totalPages} ({totalItems} total)
            </div>

            <div className="flex gap-2">
                <button
                    onClick={() => onPageChange(currentPage - 1)}
                    disabled={!canGoPrev || loading}
                    className={`
            flex items-center gap-1 px-3 py-2 rounded-lg font-medium
            transition-colors duration-200
            ${canGoPrev && !loading
                            ? 'bg-blue-600 text-white hover:bg-blue-700'
                            : 'bg-gray-100 text-gray-400 cursor-not-allowed'
                        }
          `}
                >
                    <ChevronLeft className="w-4 h-4" />
                    Previous
                </button>

                <button
                    onClick={() => onPageChange(currentPage + 1)}
                    disabled={!canGoNext || loading}
                    className={`
            flex items-center gap-1 px-3 py-2 rounded-lg font-medium
            transition-colors duration-200
            ${canGoNext && !loading
                            ? 'bg-blue-600 text-white hover:bg-blue-700'
                            : 'bg-gray-100 text-gray-400 cursor-not-allowed'
                        }
          `}
                >
                    Next
                    <ChevronRight className="w-4 h-4" />
                </button>
            </div>
        </div>
    );
}

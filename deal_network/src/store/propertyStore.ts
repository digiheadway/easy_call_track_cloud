import { create } from 'zustand';
import { Property, FilterOptions } from '../types/property';
import { PaginationMeta } from '../services/api';

export type FilterType = 'all' | 'my' | 'public' | 'saved';

interface ToastState {
    message: string;
    type: 'success' | 'error';
}

interface PropertyState {
    // Properties
    myProperties: Property[];
    publicProperties: Property[];
    savedProperties: Property[];
    filteredProperties: Property[];

    // UI State
    loading: boolean;
    activeFilter: FilterType;
    selectedProperty: Property | null;
    editingProperty: Property | null;

    // Modal State
    showModal: boolean;
    showDetailsModal: boolean;
    showContactModal: boolean;
    showFilterMenu: boolean;

    // Search & Filter
    searchQuery: string;
    searchColumn: string;
    activeFilters: FilterOptions;

    // Pagination
    paginationMeta: PaginationMeta | null;

    // Toast
    toast: ToastState | null;

    // Actions
    setMyProperties: (properties: Property[]) => void;
    setPublicProperties: (properties: Property[]) => void;
    setSavedProperties: (properties: Property[]) => void;
    setFilteredProperties: (properties: Property[]) => void;

    setLoading: (loading: boolean) => void;
    setActiveFilter: (filter: FilterType) => void;
    setSelectedProperty: (property: Property | null) => void;
    setEditingProperty: (property: Property | null) => void;

    setShowModal: (show: boolean) => void;
    setShowDetailsModal: (show: boolean) => void;
    setShowContactModal: (show: boolean) => void;
    setShowFilterMenu: (show: boolean) => void;

    setSearchQuery: (query: string) => void;
    setSearchColumn: (column: string) => void;
    setActiveFilters: (filters: FilterOptions) => void;

    setPaginationMeta: (meta: PaginationMeta | null) => void;

    showToast: (message: string, type: 'success' | 'error') => void;
    clearToast: () => void;

    // Reset
    resetState: () => void;
}

const initialState = {
    myProperties: [],
    publicProperties: [],
    savedProperties: [],
    filteredProperties: [],

    loading: false,
    activeFilter: 'all' as FilterType,
    selectedProperty: null,
    editingProperty: null,

    showModal: false,
    showDetailsModal: false,
    showContactModal: false,
    showFilterMenu: false,

    searchQuery: '',
    searchColumn: 'general',
    activeFilters: {},

    paginationMeta: null,
    toast: null,
};

export const usePropertyStore = create<PropertyState>((set) => ({
    ...initialState,

    // Property actions
    setMyProperties: (properties) => set({ myProperties: properties }),
    setPublicProperties: (properties) => set({ publicProperties: properties }),
    setSavedProperties: (properties) => set({ savedProperties: properties }),
    setFilteredProperties: (properties) => set({ filteredProperties: properties }),

    // UI actions
    setLoading: (loading) => set({ loading }),
    setActiveFilter: (filter) => set({ activeFilter: filter }),
    setSelectedProperty: (property) => set({ selectedProperty: property }),
    setEditingProperty: (property) => set({ editingProperty: property }),

    // Modal actions
    setShowModal: (show) => set({ showModal: show }),
    setShowDetailsModal: (show) => set({ showDetailsModal: show }),
    setShowContactModal: (show) => set({ showContactModal: show }),
    setShowFilterMenu: (show) => set({ showFilterMenu: show }),

    // Search & Filter actions
    setSearchQuery: (query) => set({ searchQuery: query }),
    setSearchColumn: (column) => set({ searchColumn: column }),
    setActiveFilters: (filters) => set({ activeFilters: filters }),

    // Pagination actions
    setPaginationMeta: (meta) => set({ paginationMeta: meta }),

    // Toast actions
    showToast: (message, type) => set({ toast: { message, type } }),
    clearToast: () => set({ toast: null }),

    // Reset
    resetState: () => set(initialState),
}));

import { useState, useCallback, useRef } from 'react';
import { Property } from '../types/property';
import { propertyApi, PaginationOptions, PaginationMeta } from '../services/api';

export type FilterType = 'all' | 'my' | 'public' | 'saved';

interface UsePropertyDataReturn {
    myProperties: Property[];
    publicProperties: Property[];
    savedProperties: Property[];
    filteredProperties: Property[];
    loading: boolean;
    paginationMeta: PaginationMeta | null;
    setMyProperties: React.Dispatch<React.SetStateAction<Property[]>>;
    setPublicProperties: React.Dispatch<React.SetStateAction<Property[]>>;
    setSavedProperties: React.Dispatch<React.SetStateAction<Property[]>>;
    setFilteredProperties: React.Dispatch<React.SetStateAction<Property[]>>;
    setPaginationMeta: React.Dispatch<React.SetStateAction<PaginationMeta | null>>;
    setLoading: React.Dispatch<React.SetStateAction<boolean>>;
    loadMyProperties: (paginationOptions?: PaginationOptions) => Promise<void>;
    loadPublicProperties: (paginationOptions?: PaginationOptions) => Promise<void>;
    loadSavedProperties: (paginationOptions?: PaginationOptions) => Promise<void>;
    loadingRef: React.MutableRefObject<boolean>;
    loadedDataRef: React.MutableRefObject<{ ownerId: number; my: boolean; public: boolean; saved: boolean; page?: number; per_page?: number } | null>;
    forceReloadRef: React.MutableRefObject<boolean>;
    requestIdRef: React.MutableRefObject<number>;
    pendingRequestRef: React.MutableRefObject<{ ownerId: number; activeFilter: FilterType; page: number; per_page: number } | null>;
}

export function usePropertyData(
    ownerId: number,
    pagination: PaginationOptions,
    onError: (message: string) => void
): UsePropertyDataReturn {
    const [myProperties, setMyProperties] = useState<Property[]>([]);
    const [publicProperties, setPublicProperties] = useState<Property[]>([]);
    const [savedProperties, setSavedProperties] = useState<Property[]>([]);
    const [filteredProperties, setFilteredProperties] = useState<Property[]>([]);
    const [loading, setLoading] = useState(false);
    const [paginationMeta, setPaginationMeta] = useState<PaginationMeta | null>(null);

    const loadingRef = useRef(false);
    const loadedDataRef = useRef<{ ownerId: number; my: boolean; public: boolean; saved: boolean; page?: number; per_page?: number } | null>(null);
    const forceReloadRef = useRef(false);
    const requestIdRef = useRef(0);
    const pendingRequestRef = useRef<{ ownerId: number; activeFilter: FilterType; page: number; per_page: number } | null>(null);

    const loadMyProperties = useCallback(async (paginationOptions?: PaginationOptions) => {
        if (!ownerId || ownerId <= 0) return;
        try {
            const paginationParams = paginationOptions || pagination;
            const response = await propertyApi.getUserProperties(ownerId, paginationParams);
            setMyProperties(response.data);
            setPaginationMeta(response.meta);
        } catch (error) {
            const errorMessage = error instanceof Error ? error.message : 'Failed to load properties';
            onError(errorMessage);
        }
    }, [ownerId, pagination, onError]);

    const loadPublicProperties = useCallback(async (paginationOptions?: PaginationOptions) => {
        if (!ownerId || ownerId <= 0) return;
        try {
            const paginationParams = paginationOptions || pagination;
            const response = await propertyApi.getPublicProperties(ownerId, paginationParams);
            setPublicProperties(response.data);
            setPaginationMeta(response.meta);
        } catch (error) {
            const errorMessage = error instanceof Error ? error.message : 'Failed to load public properties';
            onError(errorMessage);
        }
    }, [ownerId, pagination, onError]);

    const loadSavedProperties = useCallback(async (paginationOptions?: PaginationOptions) => {
        if (!ownerId || ownerId <= 0) return;
        try {
            const paginationParams = paginationOptions || pagination;
            const response = await propertyApi.getSavedProperties(ownerId, paginationParams);
            setSavedProperties(response.data);
            setPaginationMeta(response.meta);
        } catch (error) {
            const errorMessage = error instanceof Error ? error.message : 'Failed to load saved properties';
            onError(errorMessage);
        }
    }, [ownerId, pagination, onError]);

    return {
        myProperties,
        publicProperties,
        savedProperties,
        filteredProperties,
        loading,
        paginationMeta,
        setMyProperties,
        setPublicProperties,
        setSavedProperties,
        setFilteredProperties,
        setPaginationMeta,
        setLoading,
        loadMyProperties,
        loadPublicProperties,
        loadSavedProperties,
        loadingRef,
        loadedDataRef,
        forceReloadRef,
        requestIdRef,
        pendingRequestRef,
    };
}

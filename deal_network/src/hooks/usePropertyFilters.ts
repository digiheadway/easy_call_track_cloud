import { useState, useCallback, useRef } from 'react';
import { Property, FilterOptions } from '../types/property';
import { propertyApi, PaginationOptions, PaginationMeta } from '../services/api';
import { FilterType } from './usePropertyData';

interface UsePropertyFiltersProps {
    ownerId: number;
    activeFilter: FilterType;
    myProperties: Property[];
    publicProperties: Property[];
    savedProperties: Property[];
    pagination: PaginationOptions;
    onError: (message: string) => void;
    setFilteredProperties: (properties: Property[]) => void;
    setPaginationMeta: (meta: PaginationMeta | null) => void;
    setLoading: (loading: boolean) => void;
}

export function usePropertyFilters({
    ownerId,
    activeFilter,
    myProperties,
    publicProperties,
    savedProperties,
    pagination,
    onError,
    setFilteredProperties,
    setPaginationMeta,
    setLoading
}: UsePropertyFiltersProps) {
    const [searchQuery, setSearchQuery] = useState('');
    const [searchColumn, setSearchColumn] = useState('general');
    const [activeFilters, setActiveFilters] = useState<FilterOptions>({});

    const pendingFilterRequestRef = useRef<string | null>(null);
    const filterRequestIdRef = useRef(0);
    const filterLoadingRef = useRef(false);

    // Helper function to apply filters client-side
    const applyClientSideFilters = useCallback((properties: Property[], filters: FilterOptions): Property[] => {
        return properties.filter(property => {
            if (filters.city && property.city !== filters.city) return false;
            if (filters.area && property.area !== filters.area) return false;
            if (filters.type && property.type !== filters.type) return false;
            if (filters.min_price !== undefined && property.price_min < filters.min_price) return false;
            if (filters.max_price !== undefined && property.price_max > filters.max_price) return false;
            if (filters.size_min !== undefined && property.size_min < filters.size_min) return false;
            if (filters.max_size !== undefined && property.size_max > filters.max_size) return false;
            if (filters.size_unit && property.size_unit !== filters.size_unit) return false;
            if (filters.description && !property.description.toLowerCase().includes(filters.description.toLowerCase())) return false;
            if (filters.location && !property.location.toLowerCase().includes(filters.location.toLowerCase())) return false;
            if (filters.tags) {
                const filterTags = Array.isArray(filters.tags) ? filters.tags.join(',') : filters.tags;
                if (!property.tags?.toLowerCase().includes(filterTags.toLowerCase())) return false;
            }
            if (filters.highlights) {
                const filterHighlights = Array.isArray(filters.highlights) ? filters.highlights.join(',') : filters.highlights;
                if (!property.highlights?.toLowerCase().includes(filterHighlights.toLowerCase())) return false;
            }
            return true;
        });
    }, []);

    // Map activeFilter to API list parameter
    const getListParam = (filter: FilterType): 'mine' | 'others' | 'both' | 'saved' => {
        switch (filter) {
            case 'my': return 'mine';
            case 'public': return 'others';
            case 'saved': return 'saved';
            case 'all': return 'both';
            default: return 'both';
        }
    };

    const handleSearch = useCallback(
        async (query: string, column?: string, filterOverride?: FilterType) => {
            setSearchQuery(query);
            if (column !== undefined) {
                setSearchColumn(column);
            }

            const currentFilter = filterOverride || activeFilter;
            const listParam = getListParam(currentFilter);
            const currentColumn = column !== undefined ? column : searchColumn;

            // If no query and no active filters, show default list
            if (!query.trim() && Object.keys(activeFilters).length === 0) {
                pendingFilterRequestRef.current = null;
                if (currentFilter === 'all') {
                    setFilteredProperties([...myProperties, ...publicProperties]);
                } else if (currentFilter === 'my') {
                    setFilteredProperties(myProperties);
                } else if (currentFilter === 'public') {
                    setFilteredProperties(publicProperties);
                } else if (currentFilter === 'saved') {
                    setFilteredProperties(savedProperties);
                }
                return;
            }

            const normalizedPage = 1;
            const normalizedPerPage = 40;

            // Create unique request key
            const sortedFilters = Object.keys(activeFilters).sort().reduce((acc, key) => {
                (acc as any)[key] = activeFilters[key as keyof FilterOptions];
                return acc;
            }, {} as FilterOptions);
            const requestKey = `search:${ownerId}:${listParam}:${query.trim()}:${currentColumn}:${JSON.stringify(sortedFilters)}:${normalizedPage}:${normalizedPerPage}`;

            // Prevent duplicate requests
            if (pendingFilterRequestRef.current !== null && pendingFilterRequestRef.current !== requestKey) {
                return;
            }

            if (pendingFilterRequestRef.current === requestKey && filterLoadingRef.current) {
                return;
            }

            if (pendingFilterRequestRef.current !== requestKey) {
                pendingFilterRequestRef.current = requestKey;
            }
            const currentFilterRequestId = ++filterRequestIdRef.current;

            filterLoadingRef.current = true;
            setLoading(true);

            try {
                if (filterRequestIdRef.current !== currentFilterRequestId) {
                    return;
                }

                if (query.trim()) {
                    const searchResponse = await propertyApi.searchProperties(
                        ownerId,
                        listParam,
                        query,
                        currentColumn,
                        { page: normalizedPage, per_page: normalizedPerPage },
                        false,
                        Object.keys(activeFilters).length > 0 ? activeFilters : undefined
                    );

                    if (filterRequestIdRef.current !== currentFilterRequestId) {
                        return;
                    }

                    setFilteredProperties(searchResponse.data);
                    setPaginationMeta(searchResponse.meta);
                } else if (Object.keys(activeFilters).length > 0) {
                    const filterResponse = await propertyApi.filterProperties(ownerId, listParam, activeFilters, { page: normalizedPage, per_page: normalizedPerPage });

                    if (filterRequestIdRef.current !== currentFilterRequestId) {
                        return;
                    }

                    setFilteredProperties(filterResponse.data);
                    setPaginationMeta(filterResponse.meta);
                }
            } catch (error) {
                if (filterRequestIdRef.current === currentFilterRequestId) {
                    onError('Search failed');
                    setFilteredProperties([]);
                    setPaginationMeta(null);
                }
            } finally {
                if (filterRequestIdRef.current === currentFilterRequestId && pendingFilterRequestRef.current === requestKey) {
                    pendingFilterRequestRef.current = null;
                    filterLoadingRef.current = false;
                }
                if (filterRequestIdRef.current === currentFilterRequestId) {
                    filterLoadingRef.current = false;
                    setLoading(false);
                }
            }
        },
        [activeFilter, myProperties, publicProperties, savedProperties, ownerId, activeFilters, searchColumn, onError, setFilteredProperties, setPaginationMeta, setLoading]
    );

    const handleFilter = useCallback(
        async (filters: FilterOptions, filterOverride?: FilterType) => {
            setActiveFilters(filters);

            const currentFilter = filterOverride || activeFilter;
            const listParam = getListParam(currentFilter);

            // If no filters and no search query, show default list
            if (Object.keys(filters).length === 0 && !searchQuery.trim()) {
                pendingFilterRequestRef.current = null;
                if (currentFilter === 'all') {
                    setFilteredProperties([...myProperties, ...publicProperties]);
                } else if (currentFilter === 'my') {
                    setFilteredProperties(myProperties);
                } else if (currentFilter === 'public') {
                    setFilteredProperties(publicProperties);
                } else if (currentFilter === 'saved') {
                    setFilteredProperties(savedProperties);
                }
                return;
            }

            const normalizedPage = 1;
            const normalizedPerPage = 40;

            // Create unique request key
            const sortedFilters = Object.keys(filters).sort().reduce((acc, key) => {
                (acc as any)[key] = filters[key as keyof FilterOptions];
                return acc;
            }, {} as FilterOptions);
            const requestKey = `filter:${ownerId}:${listParam}:${JSON.stringify(sortedFilters)}:${searchQuery.trim()}:${searchColumn}:${normalizedPage}:${normalizedPerPage}`;

            // Prevent duplicate requests
            if (pendingFilterRequestRef.current !== null && pendingFilterRequestRef.current !== requestKey) {
                return;
            }

            if (pendingFilterRequestRef.current === requestKey && filterLoadingRef.current) {
                return;
            }

            if (pendingFilterRequestRef.current !== requestKey) {
                pendingFilterRequestRef.current = requestKey;
            }
            const currentFilterRequestId = ++filterRequestIdRef.current;

            filterLoadingRef.current = true;
            setLoading(true);

            try {
                if (filterRequestIdRef.current !== currentFilterRequestId) {
                    return;
                }

                if (searchQuery.trim()) {
                    const searchResponse = await propertyApi.searchProperties(
                        ownerId,
                        listParam,
                        searchQuery,
                        searchColumn,
                        { page: normalizedPage, per_page: normalizedPerPage },
                        false,
                        Object.keys(filters).length > 0 ? filters : undefined
                    );

                    if (filterRequestIdRef.current !== currentFilterRequestId) {
                        return;
                    }

                    setFilteredProperties(searchResponse.data);
                    setPaginationMeta(searchResponse.meta);
                } else {
                    const filterResponse = await propertyApi.filterProperties(ownerId, listParam, filters, { page: normalizedPage, per_page: normalizedPerPage });

                    if (filterRequestIdRef.current !== currentFilterRequestId) {
                        return;
                    }

                    setFilteredProperties(filterResponse.data);
                    setPaginationMeta(filterResponse.meta);
                }
            } catch (error) {
                if (filterRequestIdRef.current === currentFilterRequestId) {
                    onError('Filter failed');
                    setFilteredProperties([]);
                    setPaginationMeta(null);
                }
            } finally {
                if (filterRequestIdRef.current === currentFilterRequestId && pendingFilterRequestRef.current === requestKey) {
                    pendingFilterRequestRef.current = null;
                    filterLoadingRef.current = false;
                }
                if (filterRequestIdRef.current === currentFilterRequestId) {
                    filterLoadingRef.current = false;
                    setLoading(false);
                }
            }
        },
        [activeFilter, myProperties, publicProperties, savedProperties, ownerId, searchQuery, searchColumn, onError, setFilteredProperties, setPaginationMeta, setLoading]
    );

    return {
        searchQuery,
        setSearchQuery,
        searchColumn,
        setSearchColumn,
        activeFilters,
        setActiveFilters,
        handleSearch,
        handleFilter,
        applyClientSideFilters,
        pendingFilterRequestRef,
        filterRequestIdRef,
        filterLoadingRef,
    };
}

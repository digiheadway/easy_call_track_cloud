import { useEffect, Suspense, lazy, useState } from 'react';
import { Plus, Map as MapIcon, List } from 'lucide-react';
import { useAuth } from '../contexts/AuthContext';
import { usePropertyStore, FilterType } from '../store/propertyStore';
import { usePropertyHandlers } from '../hooks/usePropertyHandlers';
import { Property } from '../types/property';
import { PropertyListView } from '../components/PropertyListView';
import { FilterTabs } from '../components/FilterTabs';
import { SearchFilter } from '../components/SearchFilter';
import { Toast } from '../components/Toast';

const PropertyModal = lazy(() => import('../components/PropertyModal').then(m => ({ default: m.PropertyModal })));
const PropertyDetailsModal = lazy(() => import('../components/PropertyDetailsModal').then(m => ({ default: m.PropertyDetailsModal })));
const PropertyMap = lazy(() => import('../components/PropertyMap').then(m => ({ default: m.PropertyMap })));

export function PropertiesPage() {
    const { ownerId } = useAuth();
    const [viewMode, setViewMode] = useState<'list' | 'map'>('list');

    // Get state from store
    const {
        myProperties,
        publicProperties,
        savedProperties,
        filteredProperties,
        loading,
        activeFilter,
        selectedProperty,
        editingProperty,
        showModal,
        showDetailsModal,
        searchQuery,
        searchColumn,
        activeFilters,
        toast,
        setActiveFilter,
        setShowModal,
        setShowDetailsModal,
        setSelectedProperty,
        setEditingProperty,
        setFilteredProperties,
        setSearchQuery,
        setSearchColumn,
        setActiveFilters,
        showToast,
        clearToast,
    } = usePropertyStore();

    // Property handlers
    const {
        handleAddProperty: addPropertyBase,
        handleUpdateProperty: updatePropertyBase,
        handleDeleteProperty: deletePropertyBase,
        handleTogglePublic,
        handleUpdateHighlightsAndTags,
        handleUpdateLocation,
        handleUpdateLandmarkLocation,
        handleFavProperty,
    } = usePropertyHandlers({
        ownerId,
        onSuccess: (message) => showToast(message, 'success'),
        onError: (message) => showToast(message, 'error'),
        onPropertyUpdated: () => {
            // Reload properties logic here
        },
    });

    // Load properties on mount
    useEffect(() => {
        // Property loading logic
    }, [ownerId]);

    // Handle filter change
    const handleFilterChange = (filter: FilterType) => {
        setActiveFilter(filter);

        // Update filtered properties based on filter
        if (filter === 'all') {
            setFilteredProperties([...myProperties, ...publicProperties]);
        } else if (filter === 'my') {
            setFilteredProperties(myProperties);
        } else if (filter === 'public') {
            setFilteredProperties(publicProperties);
        } else if (filter === 'saved') {
            setFilteredProperties(savedProperties);
        }
    };

    const handleViewDetails = (property: Property) => {
        setSelectedProperty(property);
        setShowDetailsModal(true);
    };

    const handleAddClick = () => {
        setEditingProperty(null);
        setShowModal(true);
    };

    return (
        <div className="flex flex-col h-screen">
            {/* Header */}
            <header className="bg-white border-b px-4 py-3 flex items-center justify-between">
                <h1 className="text-xl font-bold">Properties</h1>

                <div className="flex gap-2">
                    {/* View Mode Toggle */}
                    <div className="flex gap-1 bg-gray-100 rounded-lg p-1">
                        <button
                            onClick={() => setViewMode('list')}
                            className={`px-3 py-1.5 rounded ${viewMode === 'list' ? 'bg-white shadow' : ''}`}
                        >
                            <List className="w-4 h-4" />
                        </button>
                        <button
                            onClick={() => setViewMode('map')}
                            className={`px-3 py-1.5 rounded ${viewMode === 'map' ? 'bg-white shadow' : ''}`}
                        >
                            <MapIcon className="w-4 h-4" />
                        </button>
                    </div>

                    {/* Add Button */}
                    <button
                        onClick={handleAddClick}
                        className="bg-blue-600 text-white px-4 py-2 rounded-lg flex items-center gap-2 hover:bg-blue-700"
                    >
                        <Plus className="w-4 h-4" />
                        Add Property
                    </button>
                </div>
            </header>

            {/* Filter Tabs */}
            <FilterTabs
                activeFilter={activeFilter}
                onFilterChange={handleFilterChange}
                counts={{
                    all: myProperties.length + publicProperties.length,
                    my: myProperties.length,
                    public: publicProperties.length,
                    saved: savedProperties.length,
                }}
            />

            {/* Search & Filter */}
            <div className="px-4 py-3 bg-white border-b">
                <SearchFilter
                    key={searchQuery}
                    searchQuery={searchQuery}
                    searchColumn={searchColumn}
                    activeFilters={activeFilters}
                    onSearch={(query, column) => {
                        setSearchQuery(query);
                        if (column) setSearchColumn(column);
                    }}
                    onFilter={setActiveFilters}
                    onClear={() => {
                        setSearchQuery('');
                        setActiveFilters({});
                    }}
                />
            </div>

            {/* Main Content */}
            <div className="flex-1 overflow-hidden">
                {viewMode === 'list' ? (
                    <PropertyListView
                        properties={filteredProperties}
                        loading={loading}
                        ownerId={ownerId}
                        onViewDetails={handleViewDetails}
                        selectedProperty={selectedProperty}
                    />
                ) : (
                    <Suspense fallback={<div>Loading map...</div>}>
                        <PropertyMap
                            properties={filteredProperties}
                            onPropertyClick={handleViewDetails}
                            selectedProperty={selectedProperty}
                        />
                    </Suspense>
                )}
            </div>

            {/* Modals */}
            {showModal && (
                <Suspense fallback={null}>
                    <PropertyModal
                        isOpen={showModal}
                        onClose={() => setShowModal(false)}
                        onSubmit={editingProperty ? updatePropertyBase : addPropertyBase}
                        initialData={editingProperty || undefined}
                    />
                </Suspense>
            )}

            {showDetailsModal && selectedProperty && (
                <Suspense fallback={null}>
                    <PropertyDetailsModal
                        property={selectedProperty}
                        isOpen={showDetailsModal}
                        onClose={() => {
                            setShowDetailsModal(false);
                            setSelectedProperty(null);
                        }}
                        onEdit={() => {
                            setEditingProperty(selectedProperty);
                            setShowModal(true);
                        }}
                        onDelete={() => deletePropertyBase(selectedProperty.id)}
                        onTogglePublic={handleTogglePublic}
                        onUpdateHighlightsAndTags={handleUpdateHighlightsAndTags}
                        onUpdateLocation={handleUpdateLocation}
                        onUpdateLandmarkLocation={handleUpdateLandmarkLocation}
                        onFavorite={handleFavProperty}
                    />
                </Suspense>
            )}

            {/* Toast */}
            {toast && (
                <Toast
                    message={toast.message}
                    type={toast.type}
                    onClose={clearToast}
                />
            )}
        </div>
    );
}

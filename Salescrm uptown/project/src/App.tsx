import React, { useState, useEffect } from 'react';
import { LeadList } from './components/LeadList';
import { LeadForm } from './components/LeadForm';
import { SearchAndFilters } from './components/SearchAndFilters';
import { Lead, USER_CREDENTIALS } from './types/options';
import { Plus, LogOut, User } from 'lucide-react';
import { Toaster } from 'react-hot-toast';
import toast from 'react-hot-toast';
import { sendWebhook, shouldTriggerWebhook } from './utils/webhook';
import { parseNextActionTimeFilter } from './utils/filters';
import { saveToCache, getFromCache, clearCache } from './utils/cache';
import { Login } from './components/Login';

function App() {
  const [loggedInUser, setLoggedInUser] = useState<string | null>(null);
  const [isAdmin, setIsAdmin] = useState(false);
  const [leads, setLeads] = useState<Lead[]>([]);
  const [view, setView] = useState<'list' | 'grid'>(() => {
    const savedView = localStorage.getItem('leadViewPreference');
    return savedView === 'list' || savedView === 'grid' ? savedView : 'list';
  });
  const [selectedLead, setSelectedLead] = useState<Partial<Lead> | null>(null);
  const [isFormOpen, setIsFormOpen] = useState(false);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [metadata, setMetadata] = useState<{
    total: number;
    page: number;
    per_page: number;
    total_pages: number;
  }>({
    total: 0,
    page: 1,
    per_page: 20,
    total_pages: 0,
  });

  const [page, setPage] = useState(() => {
    const stored = localStorage.getItem('leadListPage');
    return stored ? parseInt(stored) : 1;
  });
  const [perPage] = useState(20);
  const [searchTerm, setSearchTerm] = useState(() => {
    return localStorage.getItem('leadSearchTerm') || '';
  });
  const [sortField, setSortField] = useState<keyof Lead>(() => {
    return (localStorage.getItem('leadSortField') as keyof Lead) || 'id';
  });
  const [sortDirection, setSortDirection] = useState<'asc' | 'desc'>(() => {
    return (localStorage.getItem('leadSortDirection') as 'asc' | 'desc') || 'desc';
  });
  const [filters, setFilters] = useState<Record<string, string>>(() => {
    const stored = localStorage.getItem('leadFilters');
    return stored ? JSON.parse(stored) : {};
  });
  const [tags, setTags] = useState<string[]>(() => {
    const stored = localStorage.getItem('leadTags');
    return stored ? JSON.parse(stored) : [];
  });
  const [isFiltersOpen, setIsFiltersOpen] = useState(false);
  const [isColumnsOpen, setIsColumnsOpen] = useState(false);

  const handleLogin = (username: string) => {
    const user = USER_CREDENTIALS.find(u => u.username === username);
    setLoggedInUser(username);
    setIsAdmin(user?.isAdmin || false);
    
    // Only set the assigned_to filter for non-admin users
    if (!user?.isAdmin) {
      setFilters(prev => ({ ...prev, assigned_to: username }));
    }
  };

  const handleLogout = () => {
    localStorage.removeItem('loggedInUser');
    localStorage.removeItem('loginTimestamp');
    setLoggedInUser(null);
    setIsAdmin(false);
    setFilters({});
    clearCache();
  };

  useEffect(() => {
    const lastOpenedLeadStr = localStorage.getItem('lastOpenedLead');
    if (lastOpenedLeadStr) {
      try {
        const lastOpenedLead = JSON.parse(lastOpenedLeadStr);
        setSelectedLead(lastOpenedLead);
        setIsFormOpen(true);
      } catch (error) {
        console.error('Error parsing last opened lead:', error);
      }
    }
  }, []);

  useEffect(() => {
    localStorage.setItem('leadViewPreference', view);
    localStorage.setItem('leadListPage', page.toString());
    localStorage.setItem('leadSearchTerm', searchTerm);
    localStorage.setItem('leadSortField', sortField);
    localStorage.setItem('leadSortDirection', sortDirection);
    localStorage.setItem('leadFilters', JSON.stringify(filters));
    localStorage.setItem('leadTags', JSON.stringify(tags));
  }, [view, page, searchTerm, sortField, sortDirection, filters, tags]);

  useEffect(() => {
    localStorage.setItem('leadViewPreference', view);
  }, [view]);

  useEffect(() => {
    const handleBackButton = (event: PopStateEvent) => {
      if (isFormOpen) {
        event.preventDefault();
        handleCloseModal();
        return;
      }
      if (isFiltersOpen) {
        event.preventDefault();
        setIsFiltersOpen(false);
        return;
      }
      if (isColumnsOpen) {
        event.preventDefault();
        setIsColumnsOpen(false);
        return;
      }
      if (searchTerm) {
        event.preventDefault();
        setSearchTerm('');
        return;
      }
    };

    window.addEventListener('popstate', handleBackButton);
    return () => window.removeEventListener('popstate', handleBackButton);
  }, [isFormOpen, isFiltersOpen, isColumnsOpen, searchTerm]);

  useEffect(() => {
    if (isFormOpen || isFiltersOpen || isColumnsOpen || searchTerm) {
      window.history.pushState(null, '', window.location.pathname);
    }
  }, [isFormOpen, isFiltersOpen, isColumnsOpen, searchTerm]);

  const handleSearch = async (term: string) => {
    setSearchTerm(term);
    setPage(1);

    const phoneNumber = term.replace(/[\s+]/g, '');
    if (/^\d{10,}$/.test(phoneNumber)) {
      try {
        await navigator.clipboard.writeText(phoneNumber);
      } catch (err) {
        console.error('Failed to copy to clipboard:', err);
      }
    }
  };

  const fetchLead = async (id: number) => {
    try {
      const response = await fetch(
        `https://prop.digiheadway.in/api/new_lead.php?id=${id}`
      );
      if (!response.ok) throw new Error('Failed to fetch lead');

      const data = await response.json();
      if (data && !data.error) {
        setSelectedLead(data);
        setIsFormOpen(true);
      } else {
        throw new Error('Lead not found');
      }
    } catch (error) {
      console.error('Error fetching lead:', error);
      toast.error('Failed to load lead');
      handleCloseModal();
    }
  };

  const fetchLeads = async () => {
    try {
      setIsLoading(true);
      setError(null);

      const queryParams = new URLSearchParams({
        page: page.toString(),
        per_page: perPage.toString(),
        sort_field: sortField,
        sort_order: sortDirection,
        ...(searchTerm && { search: searchTerm }),
        ...(tags.length > 0 && { tags: tags.join(',') }),
        ...filters,
      });

      const cachedData = getFromCache();
      if (cachedData) {
        setLeads(cachedData.leads);
        setMetadata(cachedData.metadata);
        setIsLoading(false);
      }

      try {
        const response = await fetch(
          `https://prop.digiheadway.in/api/new_lead.php?${queryParams}`
        );

        if (!response.ok) {
          throw new Error(`HTTP error! status: ${response.status}`);
        }

        const data = await response.json();

        if (data.error) {
          throw new Error(data.error);
        }

        setLeads(data.data || []);
        setMetadata({
          total: parseInt(data.meta.total),
          page: parseInt(data.meta.page),
          per_page: parseInt(data.meta.per_page),
          total_pages: parseInt(data.meta.total_pages),
        });

        saveToCache({
          leads: data.data || [],
          metadata: {
            total: parseInt(data.meta.total),
            page: parseInt(data.meta.page),
            per_page: parseInt(data.meta.per_page),
            total_pages: parseInt(data.meta.total_pages),
          },
          timestamp: Date.now(),
        });

        const params = new URLSearchParams(window.location.search);
        const leadId = params.get('lead');
        if (leadId) {
          const lead = data.data?.find((l: Lead) => l.id === parseInt(leadId));
          if (lead) {
            setSelectedLead(lead);
            setIsFormOpen(true);
          }
        }
      } catch (error) {
        console.error('Network error:', error);
        if (!cachedData) {
          setError('Unable to load leads. Using cached data if available.');
        }
      }
    } catch (error) {
      console.error('Error fetching leads:', error);
      setError('Unable to load leads. Please try again.');
      setLeads([]);
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    clearCache();
  }, [searchTerm, sortField, sortDirection, filters, tags]);

  useEffect(() => {
    const debounceTimer = setTimeout(() => {
      fetchLeads();
    }, 300);

    return () => clearTimeout(debounceTimer);
  }, [page, searchTerm, sortField, sortDirection, filters, tags]);

  const handleLeadSubmit = async (lead: Partial<Lead>) => {
    try {
      const isNewLead = !lead.id;
      const url =
        'https://prop.digiheadway.in/api/new_lead.php' +
        (lead.id ? `?id=${lead.id}` : '');
      const response = await fetch(url, {
        method: lead.id ? 'PUT' : 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(lead),
      });

      if (!response.ok) throw new Error('Failed to save lead');

      const updatedLead = await response.json();

      if (isNewLead) {
        setLeads((prevLeads) => [updatedLead, ...prevLeads]);
        sendWebhook(updatedLead);
        handleCloseModal();
        toast.success('Lead created successfully', { duration: 2000 });
        fetchLeads();
      } else {
        setLeads((prevLeads) => {
          return prevLeads.map((l) =>
            l.id === lead.id ? { ...l, ...updatedLead } : l
          );
        });
      }
    } catch (error) {
      console.error('Error saving lead:', error);
      toast.error('Failed to save lead. Please try again.', { duration: 5000 });
    }
  };

  const handleLeadDelete = async (id: number) => {
    try {
      const response = await fetch(
        `https://prop.digiheadway.in/api/new_lead.php?id=${id}`,
        {
          method: 'DELETE',
        }
      );

      if (!response.ok) {
        const errorData = await response.json();
        throw new Error(errorData.message || 'Failed to delete lead');
      }

      setLeads((prevLeads) => prevLeads.filter((lead) => lead.id !== id));
      toast.success('Lead deleted successfully', { duration: 2000 });
    } catch (error) {
      console.error('Error deleting lead:', error);
      toast.error('Failed to delete lead. Please try again.', {
        duration: 5000,
      });
      throw error;
    }
  };

  const handleCloseModal = () => {
    setSelectedLead(null);
    setIsFormOpen(false);
    localStorage.removeItem('lastOpenedLead');
  };

  const handleInlineEdit = async (
    leadId: number,
    field: keyof Lead,
    value: any
  ) => {
    try {
      const lead = leads.find((l) => l.id === leadId);
      if (!lead) return;

      const updatedLead = { ...lead, [field]: value };

      setLeads((prevLeads) =>
        prevLeads.map((l) => (l.id === leadId ? { ...l, [field]: value } : l))
      );

      await handleLeadSubmit(updatedLead);

      if (shouldTriggerWebhook(field, false)) {
        sendWebhook(updatedLead);
      }
    } catch (error) {
      setLeads((prevLeads) =>
        prevLeads.map((l) => (l.id === leadId ? lead : l))
      );
      console.error('Error updating lead:', error);
      toast.error('Failed to update. Please try again.', { duration: 5000 });
    }
  };

  const handleSort = (field: keyof Lead) => {
    const newDirection =
      field === sortField && sortDirection === 'desc' ? 'asc' : 'desc';
    setSortDirection(newDirection);
    setSortField(field);
    setPage(1);
  };

  const handleFilterRemove = (key: string) => {
    if (!isAdmin && key === 'assigned_to') {
      return; // Prevent removing assigned_to filter for non-admin users
    }
    const newFilters = { ...filters };
    delete newFilters[key];
    setFilters(newFilters);
    setPage(1);
  };

  const handleClearFilters = () => {
    if (isAdmin) {
      setFilters({});
    } else {
      setFilters({ assigned_to: loggedInUser || '' });
    }
    setTags([]);
    setPage(1);
  };

  if (!loggedInUser) {
    return <Login onLogin={handleLogin} />;
  }

  return (
    <div className="min-h-screen bg-gray-50 flex flex-col">
      <Toaster position="top-right" />
      <div className="max-w-[1600px] mx-auto w-full flex-1 flex flex-col">
        <div className="bg-white rounded-xl shadow-sm overflow-hidden flex-1 flex flex-col">
          <SearchAndFilters
            searchTerm={searchTerm}
            setSearchTerm={handleSearch}
            view={view}
            onViewChange={setView}
            onOpenFilters={() => setIsFiltersOpen(true)}
            onOpenColumns={() => setIsColumnsOpen(true)}
            appliedFilters={{
              ...filters,
              tags: tags.length > 0 ? tags : undefined,
            }}
            onRemoveFilter={handleFilterRemove}
            onClearFilters={handleClearFilters}
            isAdmin={isAdmin}
          />

          <LeadList
            leads={leads}
            view={view}
            onEdit={handleInlineEdit}
            onDelete={handleLeadDelete}
            isLoading={isLoading}
            page={metadata.page}
            setPage={setPage}
            totalPages={metadata.total_pages}
            sortField={sortField}
            sortDirection={sortDirection}
            onSort={handleSort}
            onFiltersChange={setFilters}
            onTagsChange={setTags}
            filters={filters}
            tags={tags}
            error={error}
            isFiltersOpen={isFiltersOpen}
            setIsFiltersOpen={setIsFiltersOpen}
            isColumnsOpen={isColumnsOpen}
            setIsColumnsOpen={setIsColumnsOpen}
            perPage={metadata.per_page}
            totalResults={metadata.total}
            onLeadSelect={(lead) => {
              setSelectedLead(lead);
              setIsFormOpen(true);
            }}
            isAdmin={isAdmin}
          />
        </div>

        {(isFormOpen || selectedLead) && (
          <LeadForm
            lead={selectedLead || {}}
            onClose={handleCloseModal}
            onSubmit={handleLeadSubmit}
            onDelete={handleLeadDelete}
          />
        )}
        <div className="px-4 py-2 flex items-center m-auto gap-2 my-10">
          <div className="bg-white rounded-full shadow-lg px-4 py-2 flex items-center gap-2">
            <User size={20} className="text-gray-600" />
            <span className="text-sm font-medium">{loggedInUser}</span>
            <button
              onClick={handleLogout}
              className="text-gray-600 hover:text-gray-900"
            >
              <LogOut size={20} />
            </button>
          </div>
        </div>
        <div className="fixed bottom-4 right-4 flex items-center gap-2 z-30">
          <button
            onClick={() => {
              setSelectedLead({});
              setIsFormOpen(true);
            }}
            className="w-14 h-14 bg-blue-600 text-white rounded-full shadow-lg hover:bg-blue-700 transition-colors flex items-center justify-center"
          >
            <Plus size={24} />
          </button>
        </div>
      </div>
    </div>
  );
}

export default App;
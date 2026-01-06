import React, {
  createContext,
  useContext,
  useState,
  useEffect,
  ReactNode,
  useCallback,
  useRef,
} from "react";
import { toast } from "react-toastify";
import { Lead, Todo, FilterOption } from "../types";
import {
  stageOptions,
  priorityOptions,
  sourceOptions,
  purposeOptions,
  segmentOptions,
  todoTypeOptions,
  todoStatusOptions,
  intentOptions,
  tagOptions,
  Option,
  getOptionByApiValue,
  getOptionByValue,
  getApiValue,
} from "../data/options";
import { useOptimizedDataFetching } from "../hooks/useOptimizedDataFetching";

interface ApiLead {
  id: string;
  name: string;
  phone: string;
  alternative_contact_details: string | null;
  address: string | null;
  about_him: string | null;
  requirement_description: string | null;
  note: string | null;
  budget: string;
  preferred_area: string | null;
  size: string | null;
  preferred_type: string | null;
  purpose: string | null;
  stage: string;
  priority: string | null;
  next_action: string | null;
  next_action_note: string | null;
  intent: string | null;
  assigned_to: string | null;
  source: string | null;
  list_name: string | null;
  tags: string | null;
  data_1: string | null;
  data_2: string | null;
  data_3: string | null;
  segment: string | null;
  created_at: string;
  updated_at: string;
  is_deleted: string;
}

interface ApiTask {
  id: string;
  lead_id: string;
  type: string;
  description: string | null;
  response_note: string | null;
  status: string;
  timedate: string;
  participant: string | null;
  created_at: string;
  updated_at: string;
  lead: ApiLead;
}

interface ApiResponse<T> {
  status: string;
  data: T;
  total?: number;
  message?: string;
}

export interface AppOptions {
  preferredLocation: Option[];
  preferredSize: Option[];
  propertyType: Option[];
  tags: Option[];
  assignedTo: Option[];
  participants: Option[];
  lists: Option[];
}

interface AppContextType {
  leads: Lead[];
  todos: Todo[];
  leadFilters: FilterOption[];
  todoFilters: FilterOption[];
  activeLeadId: number | null;
  isLoading: boolean;
  error: string | null;
  addLead: (
    lead: Omit<Lead, "id" | "createdAt" | "updatedAt">
  ) => Promise<void>;
  updateLead: (id: number, lead: Partial<Lead>) => Promise<void>;
  deleteLead: (id: number) => void;
  addTodo: (
    todo: Omit<Todo, "id" | "createdAt" | "updatedAt">
  ) => Promise<void>;
  updateTodo: (id: number, todo: Partial<Todo>) => Promise<void>;
  deleteTodo: (id: number) => void;
  setLeadFilters: (filters: FilterOption[]) => void;
  setTodoFilters: (filters: FilterOption[]) => void;
  removeLeadFilter: (index: number) => void;
  removeTodoFilter: (index: number) => void;
  clearLeadFilters: () => void;
  clearTodoFilters: () => void;
  setActiveLeadId: (id: number | null) => void;
  getFilteredLeads: () => Lead[];
  getFilteredTodos: () => Todo[];
  getLeadById: (id: number) => Lead | undefined;
  getTodosByLeadId: (leadId: number) => Todo[];
  fetchLeads: (params?: {
    page?: number;
    perPage?: number;
    sortField?: string;
    sortOrder?: "asc" | "desc";
    search?: string;
    currentFilters?: FilterOption[];
  }) => Promise<{ data: Lead[]; total: number }>;
  fetchSingleLead: (id: number) => Promise<Lead | null>;
  fetchTodos: (params?: {
    type?: string;
    page?: number;
    perPage?: number;
    sortOrder?: "asc" | "desc";
  }) => Promise<void>;
  invalidateLeadsCache: () => void; // Add this new function
  options: AppOptions;
}

const AppContext = createContext<AppContextType | undefined>(undefined);

const API_BASE_URL = "https://prop.digiheadway.in/api/v3";
const CONTACT_API_URL = "https://prop.digiheadway.in/api/create_contact.php";
const OPTIONS_API_URL = "https://prop.digiheadway.in/api/v3/options.php";

export const AppProvider: React.FC<{ children: ReactNode }> = ({
  children,
}) => {
  const [leads, setLeads] = useState<Lead[]>([]);
  const [todos, setTodos] = useState<Todo[]>([]);
  const [leadFilters, setLeadFiltersState] = useState<FilterOption[]>([]);
  const [todoFilters, setTodoFiltersState] = useState<FilterOption[]>([]);
  const [activeLeadId, setActiveLeadId] = useState<number | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [options, setOptions] = useState<AppOptions>({
    ...tagOptions,
    lists: [],
  });

  // Global cache for leads data that persists across component re-mounts
  const globalLeadsCache = useRef<{
    data: Lead[];
    params: any;
    timestamp: number;
    total?: number;
  }>({
    data: [],
    params: null,
    timestamp: 0
  });

  // Request deduplication
  const activeRequests = useRef<Map<string, Promise<any>>>(new Map());

  // Create a unique key for request deduplication
  const createRequestKey = (type: string, params: any) => {
    return `${type}-${JSON.stringify(params)}`;
  };

  // Generic request handler with deduplication
  const makeRequest = async <T,>(
    key: string,
    requestFn: () => Promise<T>
  ): Promise<T> => {
    if (activeRequests.current.has(key)) {
      return activeRequests.current.get(key);
    }

    const promise = requestFn().finally(() => {
      activeRequests.current.delete(key);
    });

    activeRequests.current.set(key, promise);
    return promise;
  };

  const transformApiLeadToLead = (apiLead: ApiLead): Lead => ({
    id: parseInt(apiLead.id),
    name: apiLead.name || "",
    phone: apiLead.phone || "",
    alternatePhone: apiLead.alternative_contact_details || "",
    stage:
      getOptionByApiValue(stageOptions, apiLead.stage)?.value || "Fresh Lead",
    ourRating: apiLead.priority || "3",
    budget: parseInt(apiLead.budget) || 0,
    preferredLocation: apiLead.preferred_area
      ? apiLead.preferred_area.split(",")
      : [],
    preferredSize: apiLead.size ? apiLead.size.split(",") : [],
    note: apiLead.note || "",
    requirementDescription: apiLead.requirement_description || "",
    propertyType: apiLead.preferred_type
      ? apiLead.preferred_type.split(",")
      : [],
    intent: getOptionByApiValue(intentOptions, apiLead.intent)?.value || "",
    purpose:
      getOptionByApiValue(purposeOptions, apiLead.purpose)?.value || "Other",
    about: apiLead.about_him || "",
    address: apiLead.address || "",
    segment: getOptionByApiValue(segmentOptions, apiLead.segment)?.value || "C",
    source:
      getOptionByApiValue(sourceOptions, apiLead.source)?.value || "Other",
    priority:
      getOptionByApiValue(priorityOptions, apiLead.priority)?.value ||
      "General",
    tags: apiLead.tags ? apiLead.tags.split(",") : [],
    assignedTo: apiLead.assigned_to ? apiLead.assigned_to.split(",") : [],
    data1: apiLead.data_1 || "",
    data2: apiLead.data_2 || "",
    data3: apiLead.data_3 || "",
    createdAt: apiLead.created_at,
    updatedAt: apiLead.updated_at,
    isDeleted: apiLead.is_deleted === "1",
  });

  const fetchTodos = useCallback(
    async (
      params: {
        type?: string;
        page?: number;
        perPage?: number;
        sortOrder?: "asc" | "desc";
      } = {}
    ) => {
      const { type, page = 1, perPage = 20, sortOrder = "desc" } = params;

      const requestKey = createRequestKey("todos", params);

      return makeRequest(requestKey, async () => {
        setIsLoading(true);

        try {
          const queryParams = new URLSearchParams({
            action: "get_tasks",
            page: page.toString(),
            per_page: perPage.toString(),
            sort_order: sortOrder.toUpperCase(),
          });

          if (type) {
            const apiType = getApiValue(todoTypeOptions, type);
            queryParams.append("type", apiType);
          }

          const response = await fetch(`${API_BASE_URL}/?${queryParams}`);

          if (!response.ok) {
            throw new Error("Failed to fetch tasks");
          }

          const data = await response.json();

          if (data.status === "success") {
            const transformedTodos: Todo[] = data.data.map((task: ApiTask) => {
              if (task.lead) {
                const transformedLead = transformApiLeadToLead(task.lead);
                setLeads((prevLeads) => {
                  const existingLeadIndex = prevLeads.findIndex(
                    (l) => l.id === transformedLead.id
                  );
                  if (existingLeadIndex >= 0) {
                    const updatedLeads = [...prevLeads];
                    updatedLeads[existingLeadIndex] = transformedLead;
                    return updatedLeads;
                  }
                  return [...prevLeads, transformedLead];
                });
              }

              return {
                id: parseInt(task.id),
                leadId: parseInt(task.lead_id),
                type:
                  getOptionByApiValue(todoTypeOptions, task.type)?.value ||
                  "Other",

                description: task.description || "",
                responseNote: task.response_note || "",
                status:
                  getOptionByApiValue(todoStatusOptions, task.status)?.value ||
                  "Pending",
                dateTime: task.timedate,
                participants: task.participant ? [task.participant] : [],
                createdAt: task.created_at,
                updatedAt: task.updated_at,
              };
            });

            setTodos(transformedTodos);
          }
        } catch (err) {
          console.error("Error fetching tasks:", err);
          toast.error("Failed to fetch tasks");
        } finally {
          setIsLoading(false);
        }
      });
    },
    []
  );

  const fetchLeads = useCallback(
    async (
      params: {
        page?: number;
        perPage?: number;
        sortField?: string;
        sortOrder?: "asc" | "desc";
        search?: string;
        currentFilters?: FilterOption[];
      } = {}
    ): Promise<{ data: Lead[]; total: number }> => {
      const {
        page = 1,
        perPage = 20,
        sortField = "created_at",
        sortOrder = "desc",
        search = "",
        currentFilters = leadFilters,
      } = params;

      // Create a more stable cache key that properly handles filter changes
      const cacheKey = JSON.stringify({
        page,
        perPage,
        sortField,
        sortOrder,
        search,
        filters: currentFilters.sort((a, b) => a.field.localeCompare(b.field))
      });

      const cacheAge = Date.now() - globalLeadsCache.current.timestamp;
      const cacheValid = cacheAge < 2 * 60 * 1000; // 2 minutes cache

      // Only use cache if parameters are exactly the same and cache is valid
      if (
        globalLeadsCache.current.data.length > 0 &&
        globalLeadsCache.current.params === cacheKey &&
        cacheValid
      ) {
        // Return cached data immediately
        setLeads(globalLeadsCache.current.data);
        return { data: globalLeadsCache.current.data, total: globalLeadsCache.current.total || 0 };
      }

      const requestKey = createRequestKey("leads", params);

      return makeRequest(requestKey, async () => {
        setIsLoading(true);
        setError(null);

        try {
          const queryParams = new URLSearchParams({
            action: "get_leads",
            page: page.toString(),
            per_page: perPage.toString(),
            sort_field: sortField,
            sort_order: sortOrder.toUpperCase(),
          });

          if (search) {
            queryParams.append("query", search);
          }

          currentFilters.forEach((filter) => {
            switch (filter.field) {
              case "stage":
                const stageApiValue = getApiValue(stageOptions, filter.value as string);
                // If the stage value doesn't match any known stage, use "Other" as fallback
                const finalStageValue = stageApiValue === filter.value && !stageOptions.find(opt => opt.value === filter.value)
                  ? "Other"
                  : stageApiValue;
                queryParams.append("stage", finalStageValue);
                break;

              case "priority":
                queryParams.append(
                  "priority",
                  getApiValue(priorityOptions, filter.value as string)
                );
                break;


              case "intent":
                queryParams.append(
                  "intent",
                  getApiValue(intentOptions, filter.value as string)
                );
                break;

              case "source":
                queryParams.append(
                  "source",
                  getApiValue(sourceOptions, filter.value as string)
                );
                break;

              case "segment":
                queryParams.append(
                  "segment",
                  getApiValue(segmentOptions, filter.value as string)
                );
                break;

              case "assignedTo":
                if (Array.isArray(filter.value)) {
                  queryParams.append("assigned_to", filter.value.join(","));
                } else {
                  queryParams.append("assigned_to", filter.value.toString());
                }
                break;

              case "tags":
                if (Array.isArray(filter.value)) {
                  queryParams.append("tags", filter.value.join(","));
                } else {
                  queryParams.append("tags", filter.value.toString());
                }
                break;

              case "budget":
                if (filter.operator === ">=") {
                  queryParams.append("budget_min", filter.value.toString());
                } else if (filter.operator === "<=") {
                  queryParams.append("budget_max", filter.value.toString());
                }
                break;
            }
          });

          const response = await fetch(`${API_BASE_URL}/?${queryParams}`);

          if (!response.ok) {
            throw new Error("Failed to fetch leads");
          }

          const apiResponse: ApiResponse<ApiLead[]> = await response.json();

          if (apiResponse.status === "success") {
            const transformedLeads: Lead[] = apiResponse.data.map((item: ApiLead) =>
              transformApiLeadToLead(item)
            );

            const total = apiResponse.total || 0;

            // Store in global cache with the stable cache key
            globalLeadsCache.current = {
              data: transformedLeads,
              params: cacheKey,
              timestamp: Date.now(),
              total: total
            };

            setLeads(transformedLeads);
            return { data: transformedLeads, total: total }; // Return both data and total
          } else {
            throw new Error(apiResponse.message || "Failed to fetch leads");
          }
        } catch (err) {
          setError(err instanceof Error ? err.message : "An error occurred");
          console.error("Error fetching leads:", err);
          throw err;
        } finally {
          setIsLoading(false);
        }
      });
    },
    [leadFilters]
  );

  // Only fetch todos once on mount
  useEffect(() => {
    fetchTodos();
    fetchOptions();
  }, []);

  const fetchOptions = useCallback(async () => {
    try {
      const response = await fetch(OPTIONS_API_URL);
      if (!response.ok) throw new Error("Failed to fetch options");
      const data = await response.json();

      const createOption = (value: string): Option => ({
        value,
        label: value,
        apiValue: value,
      });

      const newTags = (data.tags || []).map(createOption);
      const newAssignedTo = (data.assigned_to || []).map(createOption);
      const newLists = (data.lists || []).map(createOption);

      setOptions((prev: AppOptions) => ({
        ...prev,
        tags: newTags.length > 0 ? newTags : prev.tags,
        assignedTo: newAssignedTo.length > 0 ? newAssignedTo : prev.assignedTo,
        participants: newAssignedTo.length > 0 ? newAssignedTo : prev.participants,
        lists: newLists,
      }));
    } catch (err) {
      console.error("Error fetching options:", err);
      // We don't set global error here to avoid blocking the UI if options fail
    }
  }, []);

  const updateContactAPI = async (lead: Lead) => {
    try {
      const contactData = {
        id: lead.id,
        name: lead.name,
        phone: lead.phone,
        budget: lead.budget.toString(),
        purpose: lead.purpose,
        stage: getApiValue(stageOptions, lead.stage),
        priority: getApiValue(priorityOptions, lead.priority),
        intent: getApiValue(intentOptions, lead.intent),
        segment: getApiValue(segmentOptions, lead.segment),
        created_at: lead.createdAt,
        updated_at: lead.updatedAt,
        is_deleted: "0",
      };

      const response = await fetch(
        `${CONTACT_API_URL}?source=sales&id=${lead.id}`,
        {
          method: "POST",
          headers: {
            "Content-Type": "application/json",
            Accept: "application/json",
            Origin: window.location.origin,
          },
          body: JSON.stringify(contactData),
        }
      );

      if (!response.ok) {
        throw new Error("Failed to update contact");
      }

      const data = await response.json();
      if (data.status !== "success") {
        throw new Error(data.message || "Failed to update contact");
      }
    } catch (error) {
      console.error("Error updating contact:", error);
      // Don't throw the error - we don't want this to block the main lead operations
    }
  };

  const addLead = async (
    lead: Omit<Lead, "id" | "createdAt" | "updatedAt">
  ) => {
    try {
      const apiLead = {
        name: lead.name,
        phone: lead.phone,
        alternative_contact_details: lead.alternatePhone,
        address: lead.address,
        stage: getApiValue(stageOptions, lead.stage),
        ourRating: lead.ourRating,
        intent: getApiValue(intentOptions, lead.intent),
        budget: lead.budget.toString(),
        preferred_area: lead.preferredLocation.join(","),
        size: lead.preferredSize.join(","),
        note: lead.note,
        requirement_description: lead.requirementDescription,
        preferred_type: lead.propertyType.join(","),
        purpose: getApiValue(purposeOptions, lead.purpose),
        about_him: lead.about,
        segment: getApiValue(segmentOptions, lead.segment),
        source: getApiValue(sourceOptions, lead.source),
        priority: getApiValue(priorityOptions, lead.priority),
        tags: lead.tags.join(","),
        assigned_to: lead.assignedTo.join(","),
        data_1: lead.data1,
        data_2: lead.data2,
        data_3: lead.data3,
      };

      const response = await fetch(`${API_BASE_URL}/?action=add_lead`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify(apiLead),
      });

      const data = await response.json();

      if (data.status === "success") {
        const newLead = {
          ...lead,
          id: parseInt(data.id),
          createdAt: new Date().toISOString(),
          updatedAt: new Date().toISOString(),
        };

        // Update the contact API only if data3 is '1' (sync enabled)
        if (newLead.data3 === '1') {
          await updateContactAPI(newLead);
        }

        // Invalidate cache and fetch fresh data
        invalidateLeadsCache();

        // Fetch fresh data
        await fetchLeads().then(result => {
          // Update the leads state with the new data
          setLeads(result.data);
        });
        setActiveLeadId(parseInt(data.id));
        toast.success("Lead added successfully");
      } else {
        throw new Error(data.message || "Failed to add lead");
      }
    } catch (err) {
      const errorMessage =
        err instanceof Error ? err.message : "Failed to add lead";
      toast.error(errorMessage);
      setError(errorMessage);
      throw err;
    }
  };

  const updateLead = async (id: number, leadUpdate: Partial<Lead>) => {
    try {
      const currentLead = leads.find((lead) => lead.id === id);
      if (!currentLead) {
        throw new Error("Lead not found");
      }

      const hasChanges = Object.entries(leadUpdate).some(([key, value]) => {
        if (Array.isArray(value)) {
          return (
            JSON.stringify(value) !==
            JSON.stringify(currentLead[key as keyof Lead])
          );
        }
        return value !== currentLead[key as keyof Lead];
      });

      if (!hasChanges) {
        return;
      }

      const apiUpdate: any = {
        id: id.toString(),
      };

      if (leadUpdate.name !== undefined) apiUpdate.name = leadUpdate.name;
      if (leadUpdate.phone !== undefined) apiUpdate.phone = leadUpdate.phone;
      if (leadUpdate.alternatePhone !== undefined)
        apiUpdate.alternative_contact_details = leadUpdate.alternatePhone;
      if (leadUpdate.address !== undefined)
        apiUpdate.address = leadUpdate.address;
      if (leadUpdate.about !== undefined)
        apiUpdate.about_him = leadUpdate.about;
      if (leadUpdate.requirementDescription !== undefined)
        apiUpdate.requirement_description = leadUpdate.requirementDescription;
      if (leadUpdate.note !== undefined) apiUpdate.note = leadUpdate.note;
      if (leadUpdate.budget !== undefined)
        apiUpdate.budget = leadUpdate.budget.toString();
      if (leadUpdate.preferredLocation !== undefined)
        apiUpdate.preferred_area = leadUpdate.preferredLocation.join(",");
      if (leadUpdate.preferredSize !== undefined)
        apiUpdate.size = leadUpdate.preferredSize.join(",");
      if (leadUpdate.propertyType !== undefined)
        apiUpdate.preferred_type = leadUpdate.propertyType.join(",");
      if (leadUpdate.purpose !== undefined)
        apiUpdate.purpose = getApiValue(purposeOptions, leadUpdate.purpose);
      if (leadUpdate.stage !== undefined)
        apiUpdate.stage = getApiValue(stageOptions, leadUpdate.stage);
      if (leadUpdate.intent !== undefined)
        apiUpdate.intent = getApiValue(intentOptions, leadUpdate.intent);
      if (leadUpdate.priority !== undefined)
        apiUpdate.priority = getApiValue(priorityOptions, leadUpdate.priority);
      if (leadUpdate.nextAction !== undefined)
        apiUpdate.next_action = leadUpdate.nextAction;
      if (leadUpdate.nextActionNote !== undefined)
        apiUpdate.next_action_note = leadUpdate.nextActionNote;
      if (leadUpdate.ourRating !== undefined)
        apiUpdate.ourRating = leadUpdate.ourRating;
      if (leadUpdate.assignedTo !== undefined)
        apiUpdate.assigned_to = leadUpdate.assignedTo.join(",");
      if (leadUpdate.source !== undefined)
        apiUpdate.source = getApiValue(sourceOptions, leadUpdate.source);
      if (leadUpdate.listName !== undefined)
        apiUpdate.list_name = leadUpdate.listName;
      if (leadUpdate.tags !== undefined)
        apiUpdate.tags = leadUpdate.tags.join(",");
      if (leadUpdate.data1 !== undefined) apiUpdate.data_1 = leadUpdate.data1;
      if (leadUpdate.data2 !== undefined) apiUpdate.data_2 = leadUpdate.data2;
      if (leadUpdate.data3 !== undefined) apiUpdate.data_3 = leadUpdate.data3;
      if (leadUpdate.segment !== undefined)
        apiUpdate.segment = getApiValue(segmentOptions, leadUpdate.segment);

      const response = await fetch(`${API_BASE_URL}/?action=edit_lead`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify(apiUpdate),
      });

      const data = await response.json();

      if (data.status === "success") {
        const updatedLead = {
          ...currentLead,
          ...leadUpdate,
          updatedAt: new Date().toISOString(),
        };

        // Check if we need to update the contact API
        const contactFields = ["name", "phone", "budget", "address", "about"];
        const needsContactUpdate = Object.keys(leadUpdate).some((key) =>
          contactFields.includes(key)
        );

        // Only update contact API if data3 is '1' (sync enabled) and contact fields changed
        if (needsContactUpdate && updatedLead.data3 === '1') {
          await updateContactAPI(updatedLead);
        }

        // Update the lead in the local state
        setLeads(leads.map((lead) => (lead.id === id ? updatedLead : lead)));

        // Only invalidate cache if we're not on the lead detail page
        // This prevents unnecessary refreshes when editing lead details
        if (activeLeadId !== id) {
          invalidateLeadsCache();
        } else {
          // If we're on the lead detail page, invalidate cache but skip the event
          // This updates the global cache without triggering LeadsList refresh
          invalidateLeadsCache({ skipEvent: true });
        }

        toast.success("Lead updated successfully");
      } else {
        throw new Error(data.message || "Failed to update lead");
      }
    } catch (err) {
      const errorMessage =
        err instanceof Error ? err.message : "Failed to update lead";
      toast.error(errorMessage);
      setError(errorMessage);
      throw err;
    }
  };

  const deleteLead = (id: number) => {
    setLeads(leads.filter((lead) => lead.id !== id));
    setTodos(todos.filter((todo) => todo.leadId !== id));
    if (activeLeadId === id) {
      setActiveLeadId(null);
    }
  };

  const addTodo = async (
    todo: Omit<Todo, "id" | "createdAt" | "updatedAt">
  ) => {
    try {
      const apiTodo = {
        lead_id: todo.leadId.toString(),
        type: getApiValue(todoTypeOptions, todo.type),
        title: todo.description || '',
        description: todo.description,
        response_note: todo.responseNote,
        status: getApiValue(todoStatusOptions, todo.status),
        timedate: todo.dateTime,
        participant: todo.participants[0],
      };

      const response = await fetch(`${API_BASE_URL}/?action=add_task`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify(apiTodo),
      });

      const data = await response.json();

      if (data.status === "success") {
        await fetchTodos();
        toast.success("Task added successfully");
      } else {
        throw new Error(data.message || "Failed to add task");
      }
    } catch (err) {
      const errorMessage =
        err instanceof Error ? err.message : "Failed to add task";
      toast.error(errorMessage);
      setError(errorMessage);
      throw err;
    }
  };

  const updateTodo = async (id: number, todoUpdate: Partial<Todo>) => {
    try {
      const apiUpdate: any = {
        id: id.toString(),
      };

      if (todoUpdate.type !== undefined)
        apiUpdate.type = getApiValue(todoTypeOptions, todoUpdate.type);

      if (todoUpdate.description !== undefined)
        apiUpdate.description = todoUpdate.description;
      if (todoUpdate.responseNote !== undefined)
        apiUpdate.response_note = todoUpdate.responseNote;
      if (todoUpdate.status !== undefined)
        apiUpdate.status = getApiValue(todoStatusOptions, todoUpdate.status);
      if (todoUpdate.dateTime !== undefined)
        apiUpdate.timedate = todoUpdate.dateTime;
      if (todoUpdate.participants !== undefined)
        apiUpdate.participant = todoUpdate.participants[0];

      const response = await fetch(`${API_BASE_URL}/?action=edit_task`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify(apiUpdate),
      });

      const data = await response.json();

      if (data.status === "success") {
        setTodos(
          todos.map((todo) =>
            todo.id === id
              ? { ...todo, ...todoUpdate, updatedAt: new Date().toISOString() }
              : todo
          )
        );
        toast.success("Task updated successfully");
      } else {
        throw new Error(data.message || "Failed to update task");
      }
    } catch (err) {
      const errorMessage =
        err instanceof Error ? err.message : "Failed to update task";
      toast.error(errorMessage);
      setError(errorMessage);
      throw err;
    }
  };

  const deleteTodo = (id: number) => {
    setTodos(todos.filter((todo) => todo.id !== id));
  };

  const setLeadFilters = (newFilters: FilterOption[]) => {
    setLeadFiltersState(newFilters);
  };

  const setTodoFilters = (newFilters: FilterOption[]) => {
    setTodoFiltersState(newFilters);
  };

  const removeLeadFilter = (index: number) => {
    const newFilters = [...leadFilters];
    newFilters.splice(index, 1);
    setLeadFiltersState(newFilters);
  };

  const removeTodoFilter = (index: number) => {
    const newFilters = [...todoFilters];
    newFilters.splice(index, 1);
    setTodoFiltersState(newFilters);
  };

  const clearLeadFilters = () => {
    setLeadFiltersState([]);
    // Clear global cache when filters are cleared
    globalLeadsCache.current = {
      data: [],
      params: null,
      timestamp: 0
    };
  };

  const clearTodoFilters = () => {
    setTodoFiltersState([]);
  };

  const getFilteredLeads = () => {
    return leads;
  };

  const getFilteredTodos = () => {
    if (todoFilters.length === 0) return todos;

    return todos.filter((todo) => {
      return todoFilters.every((filter) => {
        if (filter.field === "leadId" && activeLeadId) {
          return todo.leadId === activeLeadId;
        }

        const fieldValue = todo[filter.field as keyof Todo];

        if (Array.isArray(fieldValue) && Array.isArray(filter.value)) {
          return filter.value.some((v) => fieldValue.includes(v));
        }

        if (Array.isArray(fieldValue) && typeof filter.value === "string") {
          return fieldValue.includes(filter.value);
        }

        if (filter.operator === "=") {
          return fieldValue === filter.value;
        }

        if (filter.operator === "contains" && typeof fieldValue === "string") {
          return fieldValue
            .toLowerCase()
            .includes(String(filter.value).toLowerCase());
        }

        return false;
      });
    });
  };

  const getLeadById = (id: number) => {
    return leads.find((lead) => lead.id === id);
  };

  const getTodosByLeadId = (leadId: number) => {
    return todos.filter((todo) => todo.leadId === leadId);
  };

  const fetchSingleLead = useCallback(
    async (id: number) => {
      const requestKey = createRequestKey("singleLead", id);
      return makeRequest(requestKey, async () => {
        setIsLoading(true);
        setError(null);

        try {
          const response = await fetch(`${API_BASE_URL}/?action=get_lead&id=${id}`);
          if (!response.ok) {
            throw new Error("Failed to fetch single lead");
          }
          const data = await response.json();
          if (data.status === "success") {
            const transformedLead = transformApiLeadToLead(data.data);
            setLeads((prevLeads) => {
              const existingLeadIndex = prevLeads.findIndex(
                (l) => l.id === transformedLead.id
              );
              if (existingLeadIndex >= 0) {
                const updatedLeads = [...prevLeads];
                updatedLeads[existingLeadIndex] = transformedLead;
                return updatedLeads;
              }
              return [...prevLeads, transformedLead];
            });
            return transformedLead;
          } else {
            throw new Error(data.message || "Failed to fetch single lead");
          }
        } catch (err) {
          setError(err instanceof Error ? err.message : "An error occurred");
          console.error("Error fetching single lead:", err);
          return null;
        } finally {
          setIsLoading(false);
        }
      });
    },
    []
  );

  // Add a cache invalidation function
  const invalidateLeadsCache = useCallback((options?: { skipEvent?: boolean }) => {
    // Clear the global cache
    globalLeadsCache.current = {
      data: [],
      params: '',
      timestamp: 0,
      total: 0
    };

    // Only dispatch event if not skipped
    if (!options?.skipEvent) {
      // Dispatch a custom event to notify components to clear their local cache
      window.dispatchEvent(new CustomEvent('leadsCacheInvalidated'));
    }
  }, []);

  const contextValue: AppContextType = {
    leads,
    todos,
    leadFilters,
    todoFilters,
    activeLeadId,
    isLoading,
    error,
    addLead,
    updateLead,
    deleteLead,
    addTodo,
    updateTodo,
    deleteTodo,
    getFilteredLeads,
    getFilteredTodos,
    getLeadById,
    getTodosByLeadId,
    setLeadFilters,
    setTodoFilters,
    clearLeadFilters,
    clearTodoFilters,
    removeLeadFilter,
    removeTodoFilter,
    setActiveLeadId,
    fetchLeads,
    fetchSingleLead,
    fetchTodos,
    invalidateLeadsCache, // Add this to the context value
    options,
  };

  return (
    <AppContext.Provider
      value={contextValue}
    >
      {children}
    </AppContext.Provider>
  );
};

export const useAppContext = () => {
  const context = useContext(AppContext);
  if (context === undefined) {
    throw new Error("useAppContext must be used within an AppProvider");
  }
  return context;
};

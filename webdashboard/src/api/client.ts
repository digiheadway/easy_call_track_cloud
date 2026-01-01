/**
 * CallCloud Admin API Client
 * ===========================
 * Frontend JavaScript client for backend API integration
 */

// Configuration
const API_BASE_URL = 'https://calltrack.mylistings.in/callcloud/api';
const STORAGE_TOKEN_KEY = 'callcloud_auth_token';

// API Client Class
class CallCloudAPI {
    private baseUrl: string;
    private token: string | null;

    constructor() {
        this.baseUrl = API_BASE_URL;
        this.token = this.getToken();
    }

    /**
     * Get stored auth token
     */
    getToken() {
        return localStorage.getItem(STORAGE_TOKEN_KEY);
    }

    /**
     * Set auth token
     */
    setToken(token: string) {
        this.token = token;
        localStorage.setItem(STORAGE_TOKEN_KEY, token);
    }

    /**
     * Remove auth token
     */
    clearToken() {
        this.token = null;
        localStorage.removeItem(STORAGE_TOKEN_KEY);
    }

    /**
     * Make API request
     */
    async request(endpoint: string, options: any = {}) {
        const headers = {
            'Content-Type': 'application/json',
            ...options.headers
        };

        if (this.token) {
            headers['Authorization'] = `Bearer ${this.token}`;
        }

        const config = {
            ...options,
            headers
        };

        try {
            const response = await fetch(`${this.baseUrl}/${endpoint}`, config);
            const data = await response.json();

            if (!response.ok) {
                throw new Error(data.message || 'API request failed');
            }

            return data;
        } catch (error) {
            console.error('API Error:', error);
            throw error;
        }
    }

    // ===== AUTH API =====

    async signup(userData: any) {
        const response = await this.request('auth.php?action=signup', {
            method: 'POST',
            body: JSON.stringify(userData)
        });

        if (response.status && response.data.token) {
            this.setToken(response.data.token);
        }

        return response;
    }

    async login(email: string, password: string) {
        const response = await this.request('auth.php?action=login', {
            method: 'POST',
            body: JSON.stringify({ email, password })
        });

        if (response.status && response.data.token) {
            this.setToken(response.data.token);
        }

        return response;
    }

    async logout() {
        const response = await this.request('auth.php?action=logout', {
            method: 'POST'
        });

        this.clearToken();
        return response;
    }

    async verifyToken() {
        try {
            return await this.request('auth.php?action=verify');
        } catch (error) {
            this.clearToken();
            throw error;
        }
    }

    // ===== EMPLOYEES API =====

    async getEmployees() {
        return await this.request('employees.php', {
            method: 'GET'
        });
    }

    async getEmployeeStats() {
        return await this.request('employees.php?action=stats', {
            method: 'GET'
        });
    }

    async createEmployee(employeeData: any) {
        return await this.request('employees.php', {
            method: 'POST',
            body: JSON.stringify(employeeData)
        });
    }

    async updateEmployee(id: any, employeeData: any) {
        return await this.request(`employees.php?id=${id}`, {
            method: 'PUT',
            body: JSON.stringify(employeeData)
        });
    }

    async deleteEmployee(id: any) {
        return await this.request(`employees.php?id=${id}`, {
            method: 'DELETE'
        });
    }

    // ===== CALLS API =====

    async getCalls(filters = {}) {
        const params = new URLSearchParams(filters);
        return await this.request(`calls.php?${params.toString()}`, {
            method: 'GET'
        });
    }

    async getCallStats() {
        return await this.request('calls.php?action=stats', {
            method: 'GET'
        });
    }

    async createCall(callData: any) {
        return await this.request('calls.php', {
            method: 'POST',
            body: JSON.stringify(callData)
        });
    }

    // ===== RECORDINGS API =====

    async getRecordings(search = '') {
        const params = search ? `?search=${encodeURIComponent(search)}` : '';
        return await this.request(`recordings.php${params}`, {
            method: 'GET'
        });
    }

    async getRecordingStats() {
        return await this.request('recordings.php?action=stats', {
            method: 'GET'
        });
    }

    async createRecording(recordingData: any) {
        return await this.request('recordings.php', {
            method: 'POST',
            body: JSON.stringify(recordingData)
        });
    }

    async deleteRecording(id: any) {
        return await this.request(`recordings.php?id=${id}`, {
            method: 'DELETE'
        });
    }

    // ===== REPORTS API =====

    async getReport(type = 'overview', dateRange = 'week') {
        return await this.request(`reports.php?type=${type}&dateRange=${dateRange}`, {
            method: 'GET'
        });
    }

    async getOverviewReport(dateRange = 'week') {
        return await this.getReport('overview', dateRange);
    }

    async getEmployeePerformance(dateRange = 'week') {
        return await this.getReport('employee', dateRange);
    }

    async getCallAnalytics(dateRange = 'week') {
        return await this.getReport('calls', dateRange);
    }

    // ===== CONTACTS API =====

    async getContacts(filters = {}) {
        const params = new URLSearchParams(filters);
        return await this.request(`contacts.php?${params.toString()}`, {
            method: 'GET'
        });
    }

    async createContact(contactData: any) {
        return await this.request('contacts.php', {
            method: 'POST',
            body: JSON.stringify(contactData)
        });
    }

    async updateContactLabel(phone: string, label: string) {
        return await this.request('contacts.php', {
            method: 'POST',
            body: JSON.stringify({ phone, label })
        });
    }

    async deleteContact(id: number) {
        return await this.request(`contacts.php?id=${id}`, {
            method: 'DELETE'
        });
    }
}

// Create and export singleton instance
const api = new CallCloudAPI();

export default api;

// Also export for non-module usage
if (typeof window !== 'undefined') {
    (window as any).CallCloudAPI = api;
}

import axios from 'axios';

const BASE_URL = 'https://api.miniclickcrm.com/api';

const api = axios.create({
    baseURL: BASE_URL,
    headers: {
        'Content-Type': 'application/json'
    }
});

api.interceptors.request.use((config) => {
    const token = localStorage.getItem('cc_token');
    if (token) {
        config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
});

api.interceptors.response.use(
    (response) => {
        if (response.data && response.data.status === false) {
            return Promise.reject(new Error(response.data.message || 'API Error'));
        }
        return response.data;
    },
    (error) => {
        if (error.response && error.response.status === 401) {
            localStorage.removeItem('cc_token');
            window.location.href = '/login';
        }
        return Promise.reject(error);
    }
);

export default api;

import api from './api';

const authService = {
  login: async (email, password) => {
    const response = await api.post('/auth/login', { email, password });
    return response;
  },

  register: async (userData) => {
    const response = await api.post('/auth/register', userData);
    return response;
  },

  logout: async () => {
    await api.post('/auth/logout');
  },

  getCurrentUser: async () => {
    const response = await api.get('/auth/profile');
    return response;
  },

  updateProfile: async (profileData) => {
    const response = await api.put('/auth/profile', profileData);
    return response;
  },

  refreshToken: async () => {
    const response = await api.post('/auth/refresh');
    return response;
  },

  googleLogin: async (token) => {
    const response = await api.post('/auth/oauth/google', { token });
    return response;
  },
};

export default authService;
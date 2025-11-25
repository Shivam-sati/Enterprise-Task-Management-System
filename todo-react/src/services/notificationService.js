import api from './api';

const notificationService = {
  getNotificationHistory: async () => {
    const response = await api.get('/notifications/history');
    return response;
  },

  updatePreferences: async (preferences) => {
    const response = await api.put('/notifications/preferences', preferences);
    return response;
  },

  getPreferences: async () => {
    const response = await api.get('/notifications/preferences');
    return response;
  },

  markAsRead: async (notificationId) => {
    await api.put(`/notifications/${notificationId}/read`);
  },

  markAllAsRead: async () => {
    await api.put('/notifications/read-all');
  },

  testNotification: async (type) => {
    const response = await api.post('/notifications/test', { type });
    return response;
  },

  deleteNotification: async (notificationId) => {
    await api.delete(`/notifications/${notificationId}`);
  },
};

export default notificationService;
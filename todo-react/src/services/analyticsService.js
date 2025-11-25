import api from './api';

const analyticsService = {
  getProductivityMetrics: async (timeRange = '7d') => {
    const response = await api.get(`/analytics/productivity?timeRange=${timeRange}`);
    return response;
  },

  getHeatmapData: async (timeRange = '30d') => {
    const response = await api.get(`/analytics/heatmap?timeRange=${timeRange}`);
    return response;
  },

  getCompletionTrends: async (timeRange = '30d') => {
    const response = await api.get(`/analytics/trends?timeRange=${timeRange}`);
    return response;
  },

  getActivityReport: async (timeRange = '7d') => {
    const response = await api.get(`/analytics/reports?timeRange=${timeRange}`);
    return response;
  },

  exportData: async (format = 'csv', timeRange = '30d') => {
    const response = await api.get(`/analytics/export?format=${format}&timeRange=${timeRange}`, {
      responseType: 'blob',
    });
    return response;
  },
};

export default analyticsService;
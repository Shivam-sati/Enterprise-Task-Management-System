import api from './api';

const collaborationService = {
  getSharedTasks: async () => {
    const response = await api.get('/collaboration/shared');
    return response;
  },

  shareTask: async (shareData) => {
    const response = await api.post('/collaboration/share', shareData);
    return response;
  },

  updatePermissions: async (sharedListId, permissions) => {
    const response = await api.put(`/collaboration/permissions`, {
      sharedListId,
      permissions,
    });
    return response;
  },

  getActivityFeed: async (sharedListId) => {
    const response = await api.get(`/collaboration/activity?sharedListId=${sharedListId}`);
    return response;
  },

  leaveSharedTask: async (sharedListId) => {
    await api.delete(`/collaboration/shared/${sharedListId}/leave`);
  },

  removeUserFromSharedTask: async (sharedListId, userId) => {
    await api.delete(`/collaboration/shared/${sharedListId}/members/${userId}`);
  },
};

export default collaborationService;
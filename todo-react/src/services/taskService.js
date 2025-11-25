import api from './api';

const taskService = {
  getTasks: async (filters = {}) => {
    const params = new URLSearchParams();
    Object.keys(filters).forEach(key => {
      if (filters[key] && filters[key] !== 'all') {
        if (Array.isArray(filters[key])) {
          filters[key].forEach(value => params.append(key, value));
        } else {
          params.append(key, filters[key]);
        }
      }
    });
    
    const response = await api.get(`/tasks?${params.toString()}`);
    return response;
  },

  getTask: async (taskId) => {
    const response = await api.get(`/tasks/${taskId}`);
    return response;
  },

  createTask: async (taskData) => {
    const response = await api.post('/tasks', taskData);
    return response;
  },

  updateTask: async (taskId, updates) => {
    const response = await api.put(`/tasks/${taskId}`, updates);
    return response;
  },

  deleteTask: async (taskId) => {
    await api.delete(`/tasks/${taskId}`);
  },

  searchTasks: async (query) => {
    const response = await api.get(`/tasks/search?q=${encodeURIComponent(query)}`);
    return response;
  },

  createSubtask: async (taskId, subtaskData) => {
    const response = await api.post(`/tasks/${taskId}/subtasks`, subtaskData);
    return response;
  },

  updateSubtask: async (taskId, subtaskId, updates) => {
    const response = await api.put(`/tasks/${taskId}/subtasks/${subtaskId}`, updates);
    return response;
  },

  deleteSubtask: async (taskId, subtaskId) => {
    await api.delete(`/tasks/${taskId}/subtasks/${subtaskId}`);
  },

  addDependency: async (taskId, dependencyId) => {
    const response = await api.post(`/tasks/${taskId}/dependencies`, { dependencyId });
    return response;
  },

  removeDependency: async (taskId, dependencyId) => {
    await api.delete(`/tasks/${taskId}/dependencies/${dependencyId}`);
  },

  getTags: async () => {
    const response = await api.get('/tasks/tags');
    return response;
  },

  createTag: async (tagData) => {
    const response = await api.post('/tasks/tags', tagData);
    return response;
  },
};

export default taskService;
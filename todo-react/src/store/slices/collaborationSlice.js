import { createSlice, createAsyncThunk } from '@reduxjs/toolkit';
import collaborationService from '../../services/collaborationService';

// Async thunks
export const fetchSharedTasks = createAsyncThunk(
  'collaboration/fetchSharedTasks',
  async (_, { rejectWithValue }) => {
    try {
      const response = await collaborationService.getSharedTasks();
      return response;
    } catch (error) {
      return rejectWithValue(error.response?.data?.message || 'Failed to fetch shared tasks');
    }
  }
);

export const shareTask = createAsyncThunk(
  'collaboration/shareTask',
  async (shareData, { rejectWithValue }) => {
    try {
      const response = await collaborationService.shareTask(shareData);
      return response;
    } catch (error) {
      return rejectWithValue(error.response?.data?.message || 'Failed to share task');
    }
  }
);

export const updatePermissions = createAsyncThunk(
  'collaboration/updatePermissions',
  async ({ sharedListId, permissions }, { rejectWithValue }) => {
    try {
      const response = await collaborationService.updatePermissions(sharedListId, permissions);
      return response;
    } catch (error) {
      return rejectWithValue(error.response?.data?.message || 'Failed to update permissions');
    }
  }
);

export const fetchActivityFeed = createAsyncThunk(
  'collaboration/fetchActivityFeed',
  async (sharedListId, { rejectWithValue }) => {
    try {
      const response = await collaborationService.getActivityFeed(sharedListId);
      return response;
    } catch (error) {
      return rejectWithValue(error.response?.data?.message || 'Failed to fetch activity feed');
    }
  }
);

const initialState = {
  sharedTasks: [],
  activityFeed: [],
  connectedUsers: [],
  isLoading: false,
  error: null,
  websocketConnected: false,
};

const collaborationSlice = createSlice({
  name: 'collaboration',
  initialState,
  reducers: {
    clearError: (state) => {
      state.error = null;
    },
    setWebSocketConnected: (state, action) => {
      state.websocketConnected = action.payload;
    },
    addConnectedUser: (state, action) => {
      const user = action.payload;
      if (!state.connectedUsers.find(u => u.userId === user.userId)) {
        state.connectedUsers.push(user);
      }
    },
    removeConnectedUser: (state, action) => {
      const userId = action.payload;
      state.connectedUsers = state.connectedUsers.filter(u => u.userId !== userId);
    },
    addActivityFeedItem: (state, action) => {
      state.activityFeed.unshift(action.payload);
    },
    updateSharedTaskRealtime: (state, action) => {
      const updatedTask = action.payload;
      const index = state.sharedTasks.findIndex(t => t.taskId === updatedTask.taskId);
      if (index !== -1) {
        state.sharedTasks[index] = updatedTask;
      }
    },
  },
  extraReducers: (builder) => {
    builder
      // Fetch shared tasks
      .addCase(fetchSharedTasks.pending, (state) => {
        state.isLoading = true;
        state.error = null;
      })
      .addCase(fetchSharedTasks.fulfilled, (state, action) => {
        state.isLoading = false;
        state.sharedTasks = action.payload;
      })
      .addCase(fetchSharedTasks.rejected, (state, action) => {
        state.isLoading = false;
        state.error = action.payload;
      })
      // Share task
      .addCase(shareTask.fulfilled, (state, action) => {
        state.sharedTasks.push(action.payload);
      })
      .addCase(shareTask.rejected, (state, action) => {
        state.error = action.payload;
      })
      // Update permissions
      .addCase(updatePermissions.fulfilled, (state, action) => {
        const index = state.sharedTasks.findIndex(t => t.sharedListId === action.payload.sharedListId);
        if (index !== -1) {
          state.sharedTasks[index] = action.payload;
        }
      })
      .addCase(updatePermissions.rejected, (state, action) => {
        state.error = action.payload;
      })
      // Fetch activity feed
      .addCase(fetchActivityFeed.fulfilled, (state, action) => {
        state.activityFeed = action.payload;
      })
      .addCase(fetchActivityFeed.rejected, (state, action) => {
        state.error = action.payload;
      });
  },
});

export const {
  clearError,
  setWebSocketConnected,
  addConnectedUser,
  removeConnectedUser,
  addActivityFeedItem,
  updateSharedTaskRealtime,
} = collaborationSlice.actions;

export default collaborationSlice.reducer;
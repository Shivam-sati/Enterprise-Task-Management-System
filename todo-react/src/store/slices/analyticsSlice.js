import { createSlice, createAsyncThunk } from '@reduxjs/toolkit';
import analyticsService from '../../services/analyticsService';

// Async thunks
export const fetchProductivityMetrics = createAsyncThunk(
  'analytics/fetchProductivityMetrics',
  async (timeRange = '7d', { rejectWithValue }) => {
    try {
      const response = await analyticsService.getProductivityMetrics(timeRange);
      return response;
    } catch (error) {
      return rejectWithValue(error.response?.data?.message || 'Failed to fetch metrics');
    }
  }
);

export const fetchHeatmapData = createAsyncThunk(
  'analytics/fetchHeatmapData',
  async (timeRange = '30d', { rejectWithValue }) => {
    try {
      const response = await analyticsService.getHeatmapData(timeRange);
      return response;
    } catch (error) {
      return rejectWithValue(error.response?.data?.message || 'Failed to fetch heatmap data');
    }
  }
);

export const fetchCompletionTrends = createAsyncThunk(
  'analytics/fetchCompletionTrends',
  async (timeRange = '30d', { rejectWithValue }) => {
    try {
      const response = await analyticsService.getCompletionTrends(timeRange);
      return response;
    } catch (error) {
      return rejectWithValue(error.response?.data?.message || 'Failed to fetch trends');
    }
  }
);

export const fetchActivityReport = createAsyncThunk(
  'analytics/fetchActivityReport',
  async (timeRange = '7d', { rejectWithValue }) => {
    try {
      const response = await analyticsService.getActivityReport(timeRange);
      return response;
    } catch (error) {
      return rejectWithValue(error.response?.data?.message || 'Failed to fetch activity report');
    }
  }
);

const initialState = {
  productivityMetrics: {
    tasksCompleted: 0,
    tasksCreated: 0,
    completionRate: 0,
    averageCompletionTime: 0,
    productivityScore: 0,
    streakDays: 0,
  },
  heatmapData: [],
  completionTrends: [],
  activityReport: {
    dailyActivity: [],
    topTags: [],
    priorityDistribution: [],
  },
  isLoading: false,
  error: null,
  selectedTimeRange: '7d',
};

const analyticsSlice = createSlice({
  name: 'analytics',
  initialState,
  reducers: {
    clearError: (state) => {
      state.error = null;
    },
    setTimeRange: (state, action) => {
      state.selectedTimeRange = action.payload;
    },
    updateMetricsRealtime: (state, action) => {
      // Update metrics when tasks are completed/created in real-time
      const { type, data } = action.payload;
      switch (type) {
        case 'TASK_COMPLETED':
          state.productivityMetrics.tasksCompleted += 1;
          break;
        case 'TASK_CREATED':
          state.productivityMetrics.tasksCreated += 1;
          break;
        default:
          break;
      }
      // Recalculate completion rate
      if (state.productivityMetrics.tasksCreated > 0) {
        state.productivityMetrics.completionRate = 
          (state.productivityMetrics.tasksCompleted / state.productivityMetrics.tasksCreated) * 100;
      }
    },
  },
  extraReducers: (builder) => {
    builder
      // Fetch productivity metrics
      .addCase(fetchProductivityMetrics.pending, (state) => {
        state.isLoading = true;
        state.error = null;
      })
      .addCase(fetchProductivityMetrics.fulfilled, (state, action) => {
        state.isLoading = false;
        state.productivityMetrics = action.payload;
      })
      .addCase(fetchProductivityMetrics.rejected, (state, action) => {
        state.isLoading = false;
        state.error = action.payload;
      })
      // Fetch heatmap data
      .addCase(fetchHeatmapData.fulfilled, (state, action) => {
        state.heatmapData = action.payload;
      })
      .addCase(fetchHeatmapData.rejected, (state, action) => {
        state.error = action.payload;
      })
      // Fetch completion trends
      .addCase(fetchCompletionTrends.fulfilled, (state, action) => {
        state.completionTrends = action.payload;
      })
      .addCase(fetchCompletionTrends.rejected, (state, action) => {
        state.error = action.payload;
      })
      // Fetch activity report
      .addCase(fetchActivityReport.fulfilled, (state, action) => {
        state.activityReport = action.payload;
      })
      .addCase(fetchActivityReport.rejected, (state, action) => {
        state.error = action.payload;
      });
  },
});

export const {
  clearError,
  setTimeRange,
  updateMetricsRealtime,
} = analyticsSlice.actions;

export default analyticsSlice.reducer;
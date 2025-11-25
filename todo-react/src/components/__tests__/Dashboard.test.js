import React from 'react';
import { render, screen } from '@testing-library/react';
import { Provider } from 'react-redux';
import { BrowserRouter } from 'react-router-dom';
import { configureStore } from '@reduxjs/toolkit';
import Dashboard from '../Dashboard';
import authReducer from '../../store/slices/authSlice';
import taskReducer from '../../store/slices/taskSlice';
import analyticsReducer from '../../store/slices/analyticsSlice';

// Mock store for testing
const createMockStore = (initialState = {}) => {
  return configureStore({
    reducer: {
      auth: authReducer,
      tasks: taskReducer,
      analytics: analyticsReducer,
    },
    preloadedState: {
      auth: {
        isAuthenticated: true,
        user: { id: '1', name: 'Test User', email: 'test@example.com' },
        loading: false,
        error: null,
      },
      tasks: {
        tasks: [
          {
            id: '1',
            title: 'Test Task',
            description: 'Test Description',
            status: 'PENDING',
            priority: 'HIGH',
            dueDate: '2024-12-31',
          },
          {
            id: '2',
            title: 'Completed Task',
            description: 'Completed Description',
            status: 'COMPLETED',
            priority: 'MEDIUM',
          },
        ],
        loading: false,
        error: null,
        filters: {},
      },
      analytics: {
        productivityMetrics: {
          tasksCompleted: 5,
          focusTime: 8,
          productivityScore: 85,
        },
        loading: false,
        error: null,
      },
      ...initialState,
    },
  });
};

const renderWithProviders = (component, store = createMockStore()) => {
  return render(
    <Provider store={store}>
      <BrowserRouter>
        {component}
      </BrowserRouter>
    </Provider>
  );
};

describe('Dashboard Component', () => {
  test('renders welcome message with user name', () => {
    renderWithProviders(<Dashboard />);
    
    expect(screen.getByText(/Welcome back, Test User!/)).toBeInTheDocument();
  });

  test('displays task statistics correctly', () => {
    renderWithProviders(<Dashboard />);
    
    // Check if task stats are displayed
    expect(screen.getByText('Total Tasks')).toBeInTheDocument();
    expect(screen.getByText('Completed')).toBeInTheDocument();
    expect(screen.getByText('Pending')).toBeInTheDocument();
    expect(screen.getByText('Overdue')).toBeInTheDocument();
    
    // Check if the numbers are correct based on mock data
    expect(screen.getByText('2')).toBeInTheDocument(); // Total tasks
    expect(screen.getByText('1')).toBeInTheDocument(); // Completed tasks
  });

  test('displays quick action links', () => {
    renderWithProviders(<Dashboard />);
    
    expect(screen.getByText('View All Tasks')).toBeInTheDocument();
    expect(screen.getByText('Analytics')).toBeInTheDocument();
    expect(screen.getByText('Collaboration')).toBeInTheDocument();
  });

  test('displays recent tasks section', () => {
    renderWithProviders(<Dashboard />);
    
    expect(screen.getByText('Recent Tasks')).toBeInTheDocument();
    expect(screen.getByText('Test Task')).toBeInTheDocument();
    expect(screen.getByText('Completed Task')).toBeInTheDocument();
  });

  test('displays productivity metrics when available', () => {
    renderWithProviders(<Dashboard />);
    
    expect(screen.getByText("Today's Productivity")).toBeInTheDocument();
    expect(screen.getByText('5')).toBeInTheDocument(); // Tasks completed
    expect(screen.getByText('8h')).toBeInTheDocument(); // Focus time
    expect(screen.getByText('85%')).toBeInTheDocument(); // Productivity score
  });

  test('handles empty tasks state', () => {
    const storeWithNoTasks = createMockStore({
      tasks: {
        tasks: [],
        loading: false,
        error: null,
        filters: {},
      },
    });

    renderWithProviders(<Dashboard />, storeWithNoTasks);
    
    expect(screen.getByText('No tasks yet. Create your first task!')).toBeInTheDocument();
  });
});
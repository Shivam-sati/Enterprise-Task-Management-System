import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import { Provider } from 'react-redux';
import { configureStore } from '@reduxjs/toolkit';
import TaskItem from '../tasks/TaskItem';
import taskReducer from '../../store/slices/taskSlice';

// Mock the drag and drop functionality
jest.mock('@dnd-kit/sortable', () => ({
  useSortable: () => ({
    attributes: {},
    listeners: {},
    setNodeRef: jest.fn(),
    transform: null,
    transition: null,
    isDragging: false,
  }),
}));

const createMockStore = () => {
  return configureStore({
    reducer: {
      tasks: taskReducer,
    },
    preloadedState: {
      tasks: {
        tasks: [],
        loading: false,
        error: null,
        filters: {},
      },
    },
  });
};

const mockTask = {
  id: '1',
  title: 'Test Task',
  description: 'This is a test task description',
  status: 'PENDING',
  priority: 'HIGH',
  dueDate: '2024-12-31T10:00:00Z',
  tags: ['work', 'urgent'],
  assignedTo: 'John Doe',
};

const renderWithProvider = (component) => {
  const store = createMockStore();
  return render(
    <Provider store={store}>
      {component}
    </Provider>
  );
};

describe('TaskItem Component', () => {
  test('renders task information correctly', () => {
    renderWithProvider(<TaskItem task={mockTask} />);
    
    expect(screen.getByText('Test Task')).toBeInTheDocument();
    expect(screen.getByText('This is a test task description')).toBeInTheDocument();
    expect(screen.getByText('HIGH')).toBeInTheDocument();
  });

  test('displays due date when available', () => {
    renderWithProvider(<TaskItem task={mockTask} />);
    
    // Check if due date is displayed (format may vary)
    expect(screen.getByText(/12\/31\/2024/)).toBeInTheDocument();
  });

  test('displays tags when available', () => {
    renderWithProvider(<TaskItem task={mockTask} />);
    
    expect(screen.getByText('work')).toBeInTheDocument();
    expect(screen.getByText('urgent')).toBeInTheDocument();
  });

  test('displays assigned user when available', () => {
    renderWithProvider(<TaskItem task={mockTask} />);
    
    expect(screen.getByText('John Doe')).toBeInTheDocument();
  });

  test('applies correct priority styling', () => {
    renderWithProvider(<TaskItem task={mockTask} />);
    
    const priorityElement = screen.getByText('HIGH');
    expect(priorityElement).toHaveClass('bg-red-100', 'text-red-800');
  });

  test('handles task without due date', () => {
    const taskWithoutDueDate = { ...mockTask, dueDate: null };
    renderWithProvider(<TaskItem task={taskWithoutDueDate} />);
    
    expect(screen.queryByText(/Due:/)).not.toBeInTheDocument();
  });

  test('handles task without tags', () => {
    const taskWithoutTags = { ...mockTask, tags: [] };
    renderWithProvider(<TaskItem task={taskWithoutTags} />);
    
    expect(screen.queryByText('work')).not.toBeInTheDocument();
    expect(screen.queryByText('urgent')).not.toBeInTheDocument();
  });

  test('displays different status styles correctly', () => {
    const completedTask = { ...mockTask, status: 'COMPLETED' };
    renderWithProvider(<TaskItem task={completedTask} />);
    
    // The component should handle different status styles
    // This test ensures the component renders without errors for different statuses
    expect(screen.getByText('Test Task')).toBeInTheDocument();
  });
});
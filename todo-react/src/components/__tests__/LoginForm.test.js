import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { Provider } from 'react-redux';
import { BrowserRouter } from 'react-router-dom';
import { configureStore } from '@reduxjs/toolkit';
import LoginForm from '../auth/LoginForm';
import authReducer from '../../store/slices/authSlice';

const createMockStore = (initialState = {}) => {
  return configureStore({
    reducer: {
      auth: authReducer,
    },
    preloadedState: {
      auth: {
        isAuthenticated: false,
        user: null,
        loading: false,
        error: null,
        ...initialState,
      },
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

describe('LoginForm Component', () => {
  test('renders login form elements', () => {
    renderWithProviders(<LoginForm />);
    
    expect(screen.getByText('Sign In')).toBeInTheDocument();
    expect(screen.getByLabelText(/email/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/password/i)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /sign in/i })).toBeInTheDocument();
  });

  test('allows user to enter email and password', () => {
    renderWithProviders(<LoginForm />);
    
    const emailInput = screen.getByLabelText(/email/i);
    const passwordInput = screen.getByLabelText(/password/i);
    
    fireEvent.change(emailInput, { target: { value: 'test@example.com' } });
    fireEvent.change(passwordInput, { target: { value: 'password123' } });
    
    expect(emailInput.value).toBe('test@example.com');
    expect(passwordInput.value).toBe('password123');
  });

  test('displays validation errors for empty fields', async () => {
    renderWithProviders(<LoginForm />);
    
    const submitButton = screen.getByRole('button', { name: /sign in/i });
    fireEvent.click(submitButton);
    
    // HTML5 validation should prevent submission
    const emailInput = screen.getByLabelText(/email/i);
    expect(emailInput).toBeRequired();
  });

  test('shows loading state during authentication', () => {
    const storeWithLoading = createMockStore({
      loading: true,
    });
    
    renderWithProviders(<LoginForm />, storeWithLoading);
    
    expect(screen.getByText(/signing in/i)).toBeInTheDocument();
  });

  test('displays error message when authentication fails', () => {
    const storeWithError = createMockStore({
      error: 'Invalid credentials',
    });
    
    renderWithProviders(<LoginForm />, storeWithError);
    
    expect(screen.getByText('Invalid credentials')).toBeInTheDocument();
  });

  test('contains link to registration page', () => {
    renderWithProviders(<LoginForm />);
    
    expect(screen.getByText(/don't have an account/i)).toBeInTheDocument();
    expect(screen.getByRole('link', { name: /sign up/i })).toBeInTheDocument();
  });

  test('form submission calls login action', async () => {
    const mockStore = createMockStore();
    const mockDispatch = jest.fn();
    mockStore.dispatch = mockDispatch;
    
    renderWithProviders(<LoginForm />, mockStore);
    
    const emailInput = screen.getByLabelText(/email/i);
    const passwordInput = screen.getByLabelText(/password/i);
    const submitButton = screen.getByRole('button', { name: /sign in/i });
    
    fireEvent.change(emailInput, { target: { value: 'test@example.com' } });
    fireEvent.change(passwordInput, { target: { value: 'password123' } });
    fireEvent.click(submitButton);
    
    // Verify that dispatch was called (the actual action would be mocked in a real test)
    await waitFor(() => {
      expect(mockDispatch).toHaveBeenCalled();
    });
  });
});
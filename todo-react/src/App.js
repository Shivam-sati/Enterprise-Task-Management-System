import React, { useEffect } from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { useSelector, useDispatch } from 'react-redux';
import { ToastContainer } from 'react-toastify';
import 'react-toastify/dist/ReactToastify.css';
import './App.css';

// Components
import LoginForm from './components/auth/LoginForm';
import RegisterForm from './components/auth/RegisterForm';
import Dashboard from './components/Dashboard';
import TaskBoard from './components/tasks/TaskBoard';
import Analytics from './components/analytics/Analytics';
import Collaboration from './components/collaboration/Collaboration';
import Navbar from './components/layout/Navbar';
import NotificationCenter from './components/notifications/NotificationCenter';

// Services
import websocketService from './services/websocketService';
import { checkAuthStatus } from './store/slices/authSlice';

function App() {
  const dispatch = useDispatch();
  const { isAuthenticated, user } = useSelector((state) => state.auth);

  useEffect(() => {
    // Check if user is already authenticated on app load
    dispatch(checkAuthStatus());
  }, [dispatch]);

  useEffect(() => {
    // Initialize WebSocket connection when authenticated
    if (isAuthenticated && user) {
      websocketService.connect(user.id);
    }

    return () => {
      websocketService.disconnect();
    };
  }, [isAuthenticated, user]);

  if (!isAuthenticated) {
    return (
      <Router>
        <div className="min-h-screen bg-gray-50">
          <Routes>
            <Route path="/login" element={<LoginForm />} />
            <Route path="/register" element={<RegisterForm />} />
            <Route path="*" element={<Navigate to="/login" replace />} />
          </Routes>
          <ToastContainer
            position="top-right"
            autoClose={5000}
            hideProgressBar={false}
            newestOnTop={false}
            closeOnClick
            rtl={false}
            pauseOnFocusLoss
            draggable
            pauseOnHover
          />
        </div>
      </Router>
    );
  }

  return (
    <Router>
      <div className="min-h-screen bg-gray-50">
        <Navbar />
        <main className="container mx-auto px-4 py-8">
          <Routes>
            <Route path="/" element={<Dashboard />} />
            <Route path="/tasks" element={<TaskBoard />} />
            <Route path="/analytics" element={<Analytics />} />
            <Route path="/collaboration" element={<Collaboration />} />
            <Route path="*" element={<Navigate to="/" replace />} />
          </Routes>
        </main>
        <NotificationCenter />
        <ToastContainer
          position="top-right"
          autoClose={5000}
          hideProgressBar={false}
          newestOnTop={false}
          closeOnClick
          rtl={false}
          pauseOnFocusLoss
          draggable
          pauseOnHover
        />
      </div>
    </Router>
  );
}

export default App;

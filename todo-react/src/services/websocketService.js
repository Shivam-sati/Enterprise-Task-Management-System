import io from 'socket.io-client';
import { store } from '../store';
import {
  setWebSocketConnected,
  addConnectedUser,
  removeConnectedUser,
  addActivityFeedItem,
  updateSharedTaskRealtime,
} from '../store/slices/collaborationSlice';
import { addNotification, addToast } from '../store/slices/notificationSlice';
import { updateTaskStatus } from '../store/slices/taskSlice';

class WebSocketService {
  constructor() {
    this.socket = null;
    this.isConnected = false;
  }

  connect() {
    const token = localStorage.getItem('token');
    if (!token) return;

    const WEBSOCKET_URL = process.env.REACT_APP_WEBSOCKET_URL || 'http://localhost:8085';
    
    this.socket = io(WEBSOCKET_URL, {
      auth: {
        token: token,
      },
      transports: ['websocket'],
    });

    this.socket.on('connect', () => {
      console.log('WebSocket connected');
      this.isConnected = true;
      store.dispatch(setWebSocketConnected(true));
      store.dispatch(addToast({
        type: 'success',
        message: 'Connected to collaboration server',
        duration: 3000,
      }));
    });

    this.socket.on('disconnect', () => {
      console.log('WebSocket disconnected');
      this.isConnected = false;
      store.dispatch(setWebSocketConnected(false));
    });

    this.socket.on('connect_error', (error) => {
      console.error('WebSocket connection error:', error);
      store.dispatch(addToast({
        type: 'error',
        message: 'Failed to connect to collaboration server',
        duration: 5000,
      }));
    });

    // Collaboration events
    this.socket.on('user_joined', (user) => {
      store.dispatch(addConnectedUser(user));
      store.dispatch(addToast({
        type: 'info',
        message: `${user.name} joined the collaboration`,
        duration: 3000,
      }));
    });

    this.socket.on('user_left', (userId) => {
      store.dispatch(removeConnectedUser(userId));
    });

    this.socket.on('task_updated', (taskData) => {
      store.dispatch(updateSharedTaskRealtime(taskData.task));
      store.dispatch(addActivityFeedItem({
        activityId: Date.now().toString(),
        userId: taskData.userId,
        userName: taskData.userName,
        action: 'TASK_UPDATED',
        details: {
          taskTitle: taskData.task.title,
          changes: taskData.changes,
        },
        timestamp: new Date().toISOString(),
      }));

      if (taskData.userId !== store.getState().auth.user?.userId) {
        store.dispatch(addToast({
          type: 'info',
          message: `${taskData.userName} updated "${taskData.task.title}"`,
          duration: 4000,
        }));
      }
    });

    this.socket.on('task_status_changed', (data) => {
      store.dispatch(updateTaskStatus({
        taskId: data.taskId,
        status: data.status,
      }));

      if (data.userId !== store.getState().auth.user?.userId) {
        store.dispatch(addToast({
          type: 'info',
          message: `${data.userName} marked "${data.taskTitle}" as ${data.status.toLowerCase()}`,
          duration: 4000,
        }));
      }
    });

    this.socket.on('activity_feed_update', (activity) => {
      store.dispatch(addActivityFeedItem(activity));
    });

    // Notification events
    this.socket.on('notification', (notification) => {
      store.dispatch(addNotification(notification));
      store.dispatch(addToast({
        type: notification.type || 'info',
        message: notification.message,
        duration: 5000,
      }));
    });

    this.socket.on('task_reminder', (reminder) => {
      store.dispatch(addToast({
        type: 'warning',
        message: `Reminder: "${reminder.taskTitle}" is due ${reminder.dueTime}`,
        duration: 8000,
      }));
    });

    this.socket.on('deadline_alert', (alert) => {
      store.dispatch(addToast({
        type: 'error',
        message: `Deadline Alert: "${alert.taskTitle}" is overdue!`,
        duration: 10000,
      }));
    });
  }

  disconnect() {
    if (this.socket) {
      this.socket.disconnect();
      this.socket = null;
      this.isConnected = false;
      store.dispatch(setWebSocketConnected(false));
    }
  }

  // Join a shared task room
  joinSharedTask(sharedListId) {
    if (this.socket && this.isConnected) {
      this.socket.emit('join_shared_task', { sharedListId });
    }
  }

  // Leave a shared task room
  leaveSharedTask(sharedListId) {
    if (this.socket && this.isConnected) {
      this.socket.emit('leave_shared_task', { sharedListId });
    }
  }

  // Send task update to other users
  broadcastTaskUpdate(taskData) {
    if (this.socket && this.isConnected) {
      this.socket.emit('task_update', taskData);
    }
  }

  // Send typing indicator
  sendTypingIndicator(sharedListId, isTyping) {
    if (this.socket && this.isConnected) {
      this.socket.emit('typing', { sharedListId, isTyping });
    }
  }

  // Send cursor position for real-time collaboration
  sendCursorPosition(sharedListId, position) {
    if (this.socket && this.isConnected) {
      this.socket.emit('cursor_position', { sharedListId, position });
    }
  }
}

const websocketService = new WebSocketService();
export default websocketService;
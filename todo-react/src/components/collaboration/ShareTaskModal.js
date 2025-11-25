import React, { useState } from 'react';
import { useDispatch } from 'react-redux';
import { XMarkIcon, UserPlusIcon } from '@heroicons/react/24/outline';
import { shareTask } from '../../store/slices/collaborationSlice';
import { toast } from 'react-toastify';

const ShareTaskModal = ({ isOpen, onClose, task }) => {
  const dispatch = useDispatch();
  const [email, setEmail] = useState('');
  const [permission, setPermission] = useState('VIEW');
  const [message, setMessage] = useState('');
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);

    try {
      await dispatch(shareTask({
        taskId: task?.id,
        email,
        permission,
        message,
      })).unwrap();

      toast.success('Task shared successfully!');
      onClose();
      setEmail('');
      setPermission('VIEW');
      setMessage('');
    } catch (error) {
      toast.error(error.message || 'Failed to share task');
    } finally {
      setLoading(false);
    }
  };

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
      <div className="bg-white rounded-lg shadow-xl max-w-md w-full mx-4">
        <div className="flex justify-between items-center p-6 border-b border-gray-200">
          <h2 className="text-lg font-medium text-gray-900">Share Task</h2>
          <button
            onClick={onClose}
            className="text-gray-400 hover:text-gray-600"
          >
            <XMarkIcon className="h-6 w-6" />
          </button>
        </div>

        <form onSubmit={handleSubmit} className="p-6 space-y-4">
          {task && (
            <div className="bg-gray-50 p-4 rounded-lg">
              <h3 className="font-medium text-gray-900">{task.title}</h3>
              <p className="text-sm text-gray-600 mt-1">{task.description}</p>
            </div>
          )}

          <div>
            <label htmlFor="email" className="block text-sm font-medium text-gray-700 mb-1">
              Email Address
            </label>
            <input
              type="email"
              id="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent"
              placeholder="colleague@example.com"
              required
            />
          </div>

          <div>
            <label htmlFor="permission" className="block text-sm font-medium text-gray-700 mb-1">
              Permission Level
            </label>
            <select
              id="permission"
              value={permission}
              onChange={(e) => setPermission(e.target.value)}
              className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent"
            >
              <option value="VIEW">View Only</option>
              <option value="EDIT">Can Edit</option>
            </select>
            <p className="text-xs text-gray-500 mt-1">
              {permission === 'VIEW' 
                ? 'User can view the task but cannot make changes'
                : 'User can view and edit the task'
              }
            </p>
          </div>

          <div>
            <label htmlFor="message" className="block text-sm font-medium text-gray-700 mb-1">
              Message (Optional)
            </label>
            <textarea
              id="message"
              value={message}
              onChange={(e) => setMessage(e.target.value)}
              rows={3}
              className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent"
              placeholder="Add a message for the recipient..."
            />
          </div>

          <div className="flex justify-end space-x-3 pt-4">
            <button
              type="button"
              onClick={onClose}
              className="px-4 py-2 text-sm font-medium text-gray-700 bg-gray-100 rounded-md hover:bg-gray-200 transition-colors"
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={loading}
              className="px-4 py-2 text-sm font-medium text-white bg-blue-600 rounded-md hover:bg-blue-700 transition-colors disabled:opacity-50 flex items-center space-x-2"
            >
              {loading ? (
                <div className="animate-spin rounded-full h-4 w-4 border-b-2 border-white"></div>
              ) : (
                <UserPlusIcon className="h-4 w-4" />
              )}
              <span>{loading ? 'Sharing...' : 'Share Task'}</span>
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};

export default ShareTaskModal;
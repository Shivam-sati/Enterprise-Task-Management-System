import React, { useState } from 'react';
import { useDispatch } from 'react-redux';
import { useSortable } from '@dnd-kit/sortable';
import { CSS } from '@dnd-kit/utilities';
import { updateTask, deleteTask } from '../../store/slices/taskSlice';
import { addToast } from '../../store/slices/notificationSlice';
import { 
  CheckCircleIcon, 
  ClockIcon, 
  ExclamationTriangleIcon,
  TrashIcon,
  PencilIcon,
  Bars3Icon
} from '@heroicons/react/24/outline';
import { CheckCircleIcon as CheckCircleIconSolid } from '@heroicons/react/24/solid';

const TaskItem = ({ task }) => {
  const dispatch = useDispatch();
  const [isEditing, setIsEditing] = useState(false);
  const [editTitle, setEditTitle] = useState(task.title);

  const {
    attributes,
    listeners,
    setNodeRef,
    transform,
    transition,
    isDragging,
  } = useSortable({ id: task.taskId });

  const style = {
    transform: CSS.Transform.toString(transform),
    transition,
    opacity: isDragging ? 0.5 : 1,
  };

  const getPriorityColor = (priority) => {
    switch (priority) {
      case 'CRITICAL':
        return 'text-red-600 bg-red-100';
      case 'HIGH':
        return 'text-orange-600 bg-orange-100';
      case 'MEDIUM':
        return 'text-yellow-600 bg-yellow-100';
      case 'LOW':
        return 'text-green-600 bg-green-100';
      default:
        return 'text-gray-600 bg-gray-100';
    }
  };

  const getStatusIcon = (status) => {
    switch (status) {
      case 'COMPLETED':
        return <CheckCircleIconSolid className="h-5 w-5 text-green-600" />;
      case 'IN_PROGRESS':
        return <ClockIcon className="h-5 w-5 text-blue-600" />;
      case 'TODO':
        return <CheckCircleIcon className="h-5 w-5 text-gray-400" />;
      default:
        return <CheckCircleIcon className="h-5 w-5 text-gray-400" />;
    }
  };

  const handleStatusToggle = async () => {
    const newStatus = task.status === 'COMPLETED' ? 'TODO' : 'COMPLETED';
    try {
      await dispatch(updateTask({
        id: task.taskId,
        updates: { status: newStatus }
      })).unwrap();
      
      dispatch(addToast({
        type: 'success',
        message: `Task ${newStatus === 'COMPLETED' ? 'completed' : 'reopened'}`,
        duration: 3000,
      }));
    } catch (error) {
      dispatch(addToast({
        type: 'error',
        message: 'Failed to update task status',
        duration: 5000,
      }));
    }
  };

  const handleTitleEdit = async () => {
    if (editTitle.trim() && editTitle !== task.title) {
      try {
        await dispatch(updateTask({
          id: task.taskId,
          updates: { title: editTitle.trim() }
        })).unwrap();
        
        dispatch(addToast({
          type: 'success',
          message: 'Task updated successfully',
          duration: 3000,
        }));
      } catch (error) {
        dispatch(addToast({
          type: 'error',
          message: 'Failed to update task',
          duration: 5000,
        }));
        setEditTitle(task.title);
      }
    }
    setIsEditing(false);
  };

  const handleDelete = async () => {
    if (window.confirm('Are you sure you want to delete this task?')) {
      try {
        await dispatch(deleteTask(task.taskId)).unwrap();
        dispatch(addToast({
          type: 'success',
          message: 'Task deleted successfully',
          duration: 3000,
        }));
      } catch (error) {
        dispatch(addToast({
          type: 'error',
          message: 'Failed to delete task',
          duration: 5000,
        }));
      }
    }
  };

  const isOverdue = task.dueDate && new Date(task.dueDate) < new Date() && task.status !== 'COMPLETED';
  const isDueSoon = task.dueDate && 
    new Date(task.dueDate) > new Date() && 
    new Date(task.dueDate) <= new Date(Date.now() + 24 * 60 * 60 * 1000);

  return (
    <div
      ref={setNodeRef}
      style={style}
      className={`p-4 hover:bg-gray-50 transition-colors ${
        task.status === 'COMPLETED' ? 'opacity-75' : ''
      }`}
    >
      <div className="flex items-center space-x-4">
        {/* Drag Handle */}
        <button
          {...attributes}
          {...listeners}
          className="text-gray-400 hover:text-gray-600 cursor-grab active:cursor-grabbing"
        >
          <Bars3Icon className="h-5 w-5" />
        </button>

        {/* Status Toggle */}
        <button
          onClick={handleStatusToggle}
          className="flex-shrink-0 hover:scale-110 transition-transform"
        >
          {getStatusIcon(task.status)}
        </button>

        {/* Task Content */}
        <div className="flex-1 min-w-0">
          <div className="flex items-center space-x-2">
            {isEditing ? (
              <input
                type="text"
                value={editTitle}
                onChange={(e) => setEditTitle(e.target.value)}
                onBlur={handleTitleEdit}
                onKeyPress={(e) => e.key === 'Enter' && handleTitleEdit()}
                className="flex-1 text-sm font-medium text-gray-900 bg-transparent border-b border-gray-300 focus:outline-none focus:border-primary-500"
                autoFocus
              />
            ) : (
              <h3
                className={`text-sm font-medium ${
                  task.status === 'COMPLETED' 
                    ? 'text-gray-500 line-through' 
                    : 'text-gray-900'
                } cursor-pointer hover:text-primary-600`}
                onClick={() => setIsEditing(true)}
              >
                {task.title}
              </h3>
            )}

            {/* Priority Badge */}
            <span className={`inline-flex items-center px-2 py-1 rounded-full text-xs font-medium ${getPriorityColor(task.priority)}`}>
              {task.priority}
            </span>

            {/* Due Date Warning */}
            {isOverdue && (
              <ExclamationTriangleIcon className="h-4 w-4 text-red-500" title="Overdue" />
            )}
            {isDueSoon && (
              <ClockIcon className="h-4 w-4 text-yellow-500" title="Due soon" />
            )}
          </div>

          {task.description && (
            <p className={`mt-1 text-sm ${
              task.status === 'COMPLETED' ? 'text-gray-400' : 'text-gray-600'
            }`}>
              {task.description}
            </p>
          )}

          <div className="mt-2 flex items-center space-x-4 text-xs text-gray-500">
            {task.dueDate && (
              <span className={isOverdue ? 'text-red-600 font-medium' : ''}>
                Due: {new Date(task.dueDate).toLocaleDateString()}
              </span>
            )}
            
            {task.tags && task.tags.length > 0 && (
              <div className="flex space-x-1">
                {task.tags.map((tag, index) => (
                  <span
                    key={index}
                    className="inline-flex items-center px-2 py-1 rounded-full text-xs bg-blue-100 text-blue-800"
                  >
                    {tag}
                  </span>
                ))}
              </div>
            )}
          </div>
        </div>

        {/* Actions */}
        <div className="flex items-center space-x-2">
          <button
            onClick={() => setIsEditing(true)}
            className="text-gray-400 hover:text-gray-600"
            title="Edit task"
          >
            <PencilIcon className="h-4 w-4" />
          </button>
          
          <button
            onClick={handleDelete}
            className="text-gray-400 hover:text-red-600"
            title="Delete task"
          >
            <TrashIcon className="h-4 w-4" />
          </button>
        </div>
      </div>
    </div>
  );
};

export default TaskItem;
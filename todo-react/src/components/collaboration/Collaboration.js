import React, { useEffect, useState } from 'react';
import { useSelector, useDispatch } from 'react-redux';
import { 
  UserGroupIcon, 
  ShareIcon, 
  ChatBubbleLeftRightIcon,
  BellIcon,
  PlusIcon
} from '@heroicons/react/24/outline';
import {
  fetchSharedTasks,
  fetchActivityFeed,
  shareTask,
  updatePermissions,
} from '../../store/slices/collaborationSlice';
import ShareTaskModal from './ShareTaskModal';
import ActivityFeed from './ActivityFeed';
import TeamMembers from './TeamMembers';

const Collaboration = () => {
  const dispatch = useDispatch();
  const {
    sharedTasks,
    activityFeed,
    teamMembers,
    loading,
  } = useSelector((state) => state.collaboration);
  const { tasks } = useSelector((state) => state.tasks);
  
  const [showShareModal, setShowShareModal] = useState(false);
  const [selectedTask, setSelectedTask] = useState(null);
  const [activeTab, setActiveTab] = useState('shared-tasks');

  useEffect(() => {
    dispatch(fetchSharedTasks());
    dispatch(fetchActivityFeed());
  }, [dispatch]);

  const handleShareTask = (task) => {
    setSelectedTask(task);
    setShowShareModal(true);
  };

  const tabs = [
    { id: 'shared-tasks', label: 'Shared Tasks', icon: ShareIcon },
    { id: 'activity', label: 'Activity Feed', icon: BellIcon },
    { id: 'team', label: 'Team Members', icon: UserGroupIcon },
  ];

  if (loading) {
    return (
      <div className="flex justify-center items-center h-64">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600"></div>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex justify-between items-center">
        <h1 className="text-3xl font-bold text-gray-900">Collaboration</h1>
        <button
          onClick={() => setShowShareModal(true)}
          className="bg-blue-600 text-white px-4 py-2 rounded-lg flex items-center space-x-2 hover:bg-blue-700 transition-colors"
        >
          <ShareIcon className="h-5 w-5" />
          <span>Share Task</span>
        </button>
      </div>

      {/* Tabs */}
      <div className="border-b border-gray-200">
        <nav className="-mb-px flex space-x-8">
          {tabs.map((tab) => {
            const Icon = tab.icon;
            return (
              <button
                key={tab.id}
                onClick={() => setActiveTab(tab.id)}
                className={`flex items-center space-x-2 py-2 px-1 border-b-2 font-medium text-sm ${
                  activeTab === tab.id
                    ? 'border-blue-500 text-blue-600'
                    : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
                }`}
              >
                <Icon className="h-5 w-5" />
                <span>{tab.label}</span>
              </button>
            );
          })}
        </nav>
      </div>

      {/* Tab Content */}
      <div className="mt-6">
        {activeTab === 'shared-tasks' && (
          <div className="space-y-6">
            {/* My Tasks Available for Sharing */}
            <div className="bg-white rounded-lg shadow">
              <div className="px-6 py-4 border-b border-gray-200">
                <h2 className="text-lg font-medium text-gray-900">My Tasks</h2>
                <p className="text-sm text-gray-600">Tasks you can share with team members</p>
              </div>
              <div className="p-6">
                {tasks.length > 0 ? (
                  <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
                    {tasks.slice(0, 6).map((task) => (
                      <div key={task.id} className="border border-gray-200 rounded-lg p-4">
                        <div className="flex justify-between items-start mb-2">
                          <h3 className="font-medium text-gray-900">{task.title}</h3>
                          <button
                            onClick={() => handleShareTask(task)}
                            className="text-blue-600 hover:text-blue-800"
                          >
                            <ShareIcon className="h-4 w-4" />
                          </button>
                        </div>
                        <p className="text-sm text-gray-600 mb-2">{task.description}</p>
                        <div className="flex justify-between items-center text-xs text-gray-500">
                          <span className={`px-2 py-1 rounded-full ${
                            task.status === 'COMPLETED' ? 'bg-green-100 text-green-800' :
                            task.status === 'IN_PROGRESS' ? 'bg-yellow-100 text-yellow-800' :
                            'bg-gray-100 text-gray-800'
                          }`}>
                            {task.status}
                          </span>
                          <span>{task.priority}</span>
                        </div>
                      </div>
                    ))}
                  </div>
                ) : (
                  <p className="text-gray-600 text-center py-8">No tasks available to share</p>
                )}
              </div>
            </div>

            {/* Shared Tasks */}
            <div className="bg-white rounded-lg shadow">
              <div className="px-6 py-4 border-b border-gray-200">
                <h2 className="text-lg font-medium text-gray-900">Shared with Me</h2>
                <p className="text-sm text-gray-600">Tasks shared by team members</p>
              </div>
              <div className="p-6">
                {sharedTasks.length > 0 ? (
                  <div className="space-y-4">
                    {sharedTasks.map((sharedTask) => (
                      <div key={sharedTask.id} className="border border-gray-200 rounded-lg p-4">
                        <div className="flex justify-between items-start mb-2">
                          <div>
                            <h3 className="font-medium text-gray-900">{sharedTask.task.title}</h3>
                            <p className="text-sm text-gray-600">
                              Shared by {sharedTask.sharedBy.name}
                            </p>
                          </div>
                          <div className="flex items-center space-x-2">
                            <span className={`px-2 py-1 rounded-full text-xs ${
                              sharedTask.permission === 'EDIT' ? 'bg-green-100 text-green-800' :
                              'bg-blue-100 text-blue-800'
                            }`}>
                              {sharedTask.permission}
                            </span>
                            <ChatBubbleLeftRightIcon className="h-4 w-4 text-gray-400" />
                          </div>
                        </div>
                        <p className="text-sm text-gray-600 mb-2">{sharedTask.task.description}</p>
                        <div className="flex justify-between items-center text-xs text-gray-500">
                          <span>Due: {sharedTask.task.dueDate ? new Date(sharedTask.task.dueDate).toLocaleDateString() : 'No due date'}</span>
                          <span>Shared: {new Date(sharedTask.sharedAt).toLocaleDateString()}</span>
                        </div>
                      </div>
                    ))}
                  </div>
                ) : (
                  <p className="text-gray-600 text-center py-8">No tasks shared with you</p>
                )}
              </div>
            </div>
          </div>
        )}

        {activeTab === 'activity' && <ActivityFeed activities={activityFeed} />}
        
        {activeTab === 'team' && <TeamMembers members={teamMembers} />}
      </div>

      {/* Share Task Modal */}
      {showShareModal && (
        <ShareTaskModal
          isOpen={showShareModal}
          onClose={() => {
            setShowShareModal(false);
            setSelectedTask(null);
          }}
          task={selectedTask}
        />
      )}
    </div>
  );
};

export default Collaboration;
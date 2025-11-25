import React from 'react';
import { 
  CheckCircleIcon, 
  PencilIcon, 
  ShareIcon, 
  UserPlusIcon,
  ClockIcon
} from '@heroicons/react/24/outline';

const ActivityFeed = ({ activities = [] }) => {
  const getActivityIcon = (type) => {
    switch (type) {
      case 'TASK_COMPLETED':
        return <CheckCircleIcon className="h-5 w-5 text-green-600" />;
      case 'TASK_UPDATED':
        return <PencilIcon className="h-5 w-5 text-blue-600" />;
      case 'TASK_SHARED':
        return <ShareIcon className="h-5 w-5 text-purple-600" />;
      case 'MEMBER_ADDED':
        return <UserPlusIcon className="h-5 w-5 text-indigo-600" />;
      default:
        return <ClockIcon className="h-5 w-5 text-gray-600" />;
    }
  };

  const getActivityColor = (type) => {
    switch (type) {
      case 'TASK_COMPLETED':
        return 'bg-green-50 border-green-200';
      case 'TASK_UPDATED':
        return 'bg-blue-50 border-blue-200';
      case 'TASK_SHARED':
        return 'bg-purple-50 border-purple-200';
      case 'MEMBER_ADDED':
        return 'bg-indigo-50 border-indigo-200';
      default:
        return 'bg-gray-50 border-gray-200';
    }
  };

  const formatTimeAgo = (timestamp) => {
    const now = new Date();
    const time = new Date(timestamp);
    const diffInMinutes = Math.floor((now - time) / (1000 * 60));

    if (diffInMinutes < 1) return 'Just now';
    if (diffInMinutes < 60) return `${diffInMinutes}m ago`;
    if (diffInMinutes < 1440) return `${Math.floor(diffInMinutes / 60)}h ago`;
    return `${Math.floor(diffInMinutes / 1440)}d ago`;
  };

  return (
    <div className="bg-white rounded-lg shadow">
      <div className="px-6 py-4 border-b border-gray-200">
        <h2 className="text-lg font-medium text-gray-900">Recent Activity</h2>
        <p className="text-sm text-gray-600">Stay updated with team activities</p>
      </div>
      
      <div className="p-6">
        {activities.length > 0 ? (
          <div className="space-y-4">
            {activities.map((activity) => (
              <div
                key={activity.id}
                className={`border rounded-lg p-4 ${getActivityColor(activity.type)}`}
              >
                <div className="flex items-start space-x-3">
                  <div className="flex-shrink-0 mt-1">
                    {getActivityIcon(activity.type)}
                  </div>
                  <div className="flex-1 min-w-0">
                    <div className="flex items-center justify-between">
                      <p className="text-sm font-medium text-gray-900">
                        {activity.user.name}
                      </p>
                      <p className="text-xs text-gray-500">
                        {formatTimeAgo(activity.timestamp)}
                      </p>
                    </div>
                    <p className="text-sm text-gray-700 mt-1">
                      {activity.description}
                    </p>
                    {activity.taskTitle && (
                      <p className="text-xs text-gray-600 mt-2 font-medium">
                        Task: {activity.taskTitle}
                      </p>
                    )}
                  </div>
                </div>
              </div>
            ))}
          </div>
        ) : (
          <div className="text-center py-12">
            <ClockIcon className="h-12 w-12 text-gray-400 mx-auto mb-4" />
            <p className="text-gray-600">No recent activity</p>
            <p className="text-sm text-gray-500 mt-1">
              Activity will appear here when team members interact with shared tasks
            </p>
          </div>
        )}
      </div>
    </div>
  );
};

export default ActivityFeed;
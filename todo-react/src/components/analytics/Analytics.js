import React, { useEffect, useState } from 'react';
import { useSelector, useDispatch } from 'react-redux';
import {
  LineChart,
  Line,
  BarChart,
  Bar,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  Legend,
  ResponsiveContainer,
  PieChart,
  Pie,
  Cell,
} from 'recharts';
import { CalendarIcon, ChartBarIcon, ClockIcon } from '@heroicons/react/24/outline';
import {
  fetchProductivityMetrics,
  fetchCompletionTrends,
  fetchActivityReport,
} from '../../store/slices/analyticsSlice';

const Analytics = () => {
  const dispatch = useDispatch();
  const {
    productivityMetrics,
    completionTrends,
    activityReport,
    isLoading,
  } = useSelector((state) => state.analytics);

  const [selectedPeriod, setSelectedPeriod] = useState('week');

  useEffect(() => {
    dispatch(fetchProductivityMetrics());
    dispatch(fetchCompletionTrends(selectedPeriod));
    dispatch(fetchActivityReport(selectedPeriod));
  }, [dispatch, selectedPeriod]);

  const COLORS = ['#3B82F6', '#10B981', '#F59E0B', '#EF4444'];

  const taskStatusData = [
    { name: 'Completed', value: productivityMetrics?.tasksCompleted || 0, color: '#10B981' },
    { name: 'In Progress', value: productivityMetrics?.tasksInProgress || 0, color: '#F59E0B' },
    { name: 'Pending', value: productivityMetrics?.tasksPending || 0, color: '#6B7280' },
    { name: 'Overdue', value: productivityMetrics?.tasksOverdue || 0, color: '#EF4444' },
  ];

  if (isLoading) {
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
        <h1 className="text-3xl font-bold text-gray-900">Analytics Dashboard</h1>
        <div className="flex space-x-2">
          {['day', 'week', 'month', 'year'].map((period) => (
            <button
              key={period}
              onClick={() => setSelectedPeriod(period)}
              className={`px-4 py-2 rounded-lg text-sm font-medium transition-colors ${
                selectedPeriod === period
                  ? 'bg-blue-600 text-white'
                  : 'bg-gray-200 text-gray-700 hover:bg-gray-300'
              }`}
            >
              {period.charAt(0).toUpperCase() + period.slice(1)}
            </button>
          ))}
        </div>
      </div>

      {/* Key Metrics */}
      <div className="grid grid-cols-1 md:grid-cols-4 gap-6">
        <div className="bg-white rounded-lg shadow p-6">
          <div className="flex items-center">
            <div className="flex-shrink-0">
              <ChartBarIcon className="h-8 w-8 text-blue-600" />
            </div>
            <div className="ml-4">
              <p className="text-sm font-medium text-gray-600">Productivity Score</p>
              <p className="text-2xl font-bold text-gray-900">
                {productivityMetrics?.productivityScore || 0}%
              </p>
            </div>
          </div>
        </div>

        <div className="bg-white rounded-lg shadow p-6">
          <div className="flex items-center">
            <div className="flex-shrink-0">
              <ClockIcon className="h-8 w-8 text-green-600" />
            </div>
            <div className="ml-4">
              <p className="text-sm font-medium text-gray-600">Focus Time</p>
              <p className="text-2xl font-bold text-gray-900">
                {productivityMetrics?.focusTime || 0}h
              </p>
            </div>
          </div>
        </div>

        <div className="bg-white rounded-lg shadow p-6">
          <div className="flex items-center">
            <div className="flex-shrink-0">
              <CalendarIcon className="h-8 w-8 text-purple-600" />
            </div>
            <div className="ml-4">
              <p className="text-sm font-medium text-gray-600">Tasks Completed</p>
              <p className="text-2xl font-bold text-gray-900">
                {productivityMetrics?.tasksCompleted || 0}
              </p>
            </div>
          </div>
        </div>

        <div className="bg-white rounded-lg shadow p-6">
          <div className="flex items-center">
            <div className="flex-shrink-0">
              <ChartBarIcon className="h-8 w-8 text-yellow-600" />
            </div>
            <div className="ml-4">
              <p className="text-sm font-medium text-gray-600">Avg. Completion Time</p>
              <p className="text-2xl font-bold text-gray-900">
                {productivityMetrics?.avgCompletionTime || 0}h
              </p>
            </div>
          </div>
        </div>
      </div>

      {/* Charts Row 1 */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Task Completion Trends */}
        <div className="bg-white rounded-lg shadow p-6">
          <h2 className="text-lg font-medium text-gray-900 mb-4">Task Completion Trends</h2>
          <ResponsiveContainer width="100%" height={300}>
            <LineChart data={completionTrends}>
              <CartesianGrid strokeDasharray="3 3" />
              <XAxis dataKey="date" />
              <YAxis />
              <Tooltip />
              <Legend />
              <Line
                type="monotone"
                dataKey="completed"
                stroke="#10B981"
                strokeWidth={2}
                name="Completed Tasks"
              />
              <Line
                type="monotone"
                dataKey="created"
                stroke="#3B82F6"
                strokeWidth={2}
                name="Created Tasks"
              />
            </LineChart>
          </ResponsiveContainer>
        </div>

        {/* Task Status Distribution */}
        <div className="bg-white rounded-lg shadow p-6">
          <h2 className="text-lg font-medium text-gray-900 mb-4">Task Status Distribution</h2>
          <ResponsiveContainer width="100%" height={300}>
            <PieChart>
              <Pie
                data={taskStatusData}
                cx="50%"
                cy="50%"
                labelLine={false}
                label={({ name, percent }) => `${name} ${(percent * 100).toFixed(0)}%`}
                outerRadius={80}
                fill="#8884d8"
                dataKey="value"
              >
                {taskStatusData.map((entry, index) => (
                  <Cell key={`cell-${index}`} fill={entry.color} />
                ))}
              </Pie>
              <Tooltip />
            </PieChart>
          </ResponsiveContainer>
        </div>
      </div>

      {/* Charts Row 2 */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Time Spent Analytics */}
        <div className="bg-white rounded-lg shadow p-6">
          <h2 className="text-lg font-medium text-gray-900 mb-4">Time Spent by Category</h2>
          <ResponsiveContainer width="100%" height={300}>
            <BarChart data={activityReport?.dailyActivity || []}>
              <CartesianGrid strokeDasharray="3 3" />
              <XAxis dataKey="category" />
              <YAxis />
              <Tooltip />
              <Legend />
              <Bar dataKey="timeSpent" fill="#3B82F6" name="Hours Spent" />
            </BarChart>
          </ResponsiveContainer>
        </div>

        {/* Productivity Heatmap */}
        <div className="bg-white rounded-lg shadow p-6">
          <h2 className="text-lg font-medium text-gray-900 mb-4">Weekly Productivity Heatmap</h2>
          <div className="grid grid-cols-7 gap-2">
            {Array.from({ length: 7 }, (_, dayIndex) => (
              <div key={dayIndex} className="text-center">
                <div className="text-xs font-medium text-gray-600 mb-2">
                  {['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'][dayIndex]}
                </div>
                {Array.from({ length: 24 }, (_, hourIndex) => {
                  const intensity = Math.random(); // Replace with actual data
                  return (
                    <div
                      key={hourIndex}
                      className={`w-4 h-4 mb-1 rounded-sm ${
                        intensity > 0.7
                          ? 'bg-green-500'
                          : intensity > 0.4
                          ? 'bg-green-300'
                          : intensity > 0.2
                          ? 'bg-green-100'
                          : 'bg-gray-100'
                      }`}
                      title={`${hourIndex}:00 - Productivity: ${(intensity * 100).toFixed(0)}%`}
                    />
                  );
                })}
              </div>
            ))}
          </div>
        </div>
      </div>

      {/* Insights */}
      <div className="bg-white rounded-lg shadow p-6">
        <h2 className="text-lg font-medium text-gray-900 mb-4">Productivity Insights</h2>
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          <div className="bg-blue-50 p-4 rounded-lg">
            <h3 className="font-medium text-blue-900">Peak Performance</h3>
            <p className="text-sm text-blue-700 mt-1">
              You're most productive between 9 AM - 11 AM
            </p>
          </div>
          <div className="bg-green-50 p-4 rounded-lg">
            <h3 className="font-medium text-green-900">Completion Rate</h3>
            <p className="text-sm text-green-700 mt-1">
              85% task completion rate this week
            </p>
          </div>
          <div className="bg-yellow-50 p-4 rounded-lg">
            <h3 className="font-medium text-yellow-900">Improvement Area</h3>
            <p className="text-sm text-yellow-700 mt-1">
              Consider breaking down large tasks
            </p>
          </div>
        </div>
      </div>
    </div>
  );
};

export default Analytics;
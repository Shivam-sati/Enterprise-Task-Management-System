import React, { useState, useEffect } from 'react';
import { useSelector, useDispatch } from 'react-redux';
import { DndContext, DragOverlay, closestCorners } from '@dnd-kit/core';
import { SortableContext, verticalListSortingStrategy } from '@dnd-kit/sortable';
import { PlusIcon } from '@heroicons/react/24/outline';
import TaskList from './TaskList';
import TaskFilters from './TaskFilters';
import CreateTaskModal from './CreateTaskModal';
import { fetchTasks, updateTaskStatus, reorderTasks } from '../../store/slices/taskSlice';

const TaskBoard = () => {
  const dispatch = useDispatch();
  const { tasks, loading, filters } = useSelector((state) => state.tasks);
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [activeTask, setActiveTask] = useState(null);

  useEffect(() => {
    dispatch(fetchTasks());
  }, [dispatch]);

  const taskColumns = {
    PENDING: tasks.filter(task => task.status === 'PENDING'),
    IN_PROGRESS: tasks.filter(task => task.status === 'IN_PROGRESS'),
    COMPLETED: tasks.filter(task => task.status === 'COMPLETED'),
  };

  const handleDragStart = (event) => {
    const { active } = event;
    const task = tasks.find(t => t.id === active.id);
    setActiveTask(task);
  };

  const handleDragEnd = (event) => {
    const { active, over } = event;
    setActiveTask(null);

    if (!over) return;

    const activeTask = tasks.find(t => t.id === active.id);
    const overColumn = over.id;

    // If dropping on a column header, update task status
    if (['PENDING', 'IN_PROGRESS', 'COMPLETED'].includes(overColumn)) {
      if (activeTask.status !== overColumn) {
        dispatch(updateTaskStatus({ 
          taskId: activeTask.id, 
          status: overColumn 
        }));
      }
    }
  };

  const columnTitles = {
    PENDING: 'To Do',
    IN_PROGRESS: 'In Progress',
    COMPLETED: 'Completed',
  };

  const columnColors = {
    PENDING: 'border-gray-300 bg-gray-50',
    IN_PROGRESS: 'border-yellow-300 bg-yellow-50',
    COMPLETED: 'border-green-300 bg-green-50',
  };

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
        <h1 className="text-3xl font-bold text-gray-900">Task Board</h1>
        <button
          onClick={() => setShowCreateModal(true)}
          className="bg-blue-600 text-white px-4 py-2 rounded-lg flex items-center space-x-2 hover:bg-blue-700 transition-colors"
        >
          <PlusIcon className="h-5 w-5" />
          <span>New Task</span>
        </button>
      </div>

      {/* Filters */}
      <TaskFilters />

      {/* Task Board */}
      <DndContext
        collisionDetection={closestCorners}
        onDragStart={handleDragStart}
        onDragEnd={handleDragEnd}
      >
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
          {Object.entries(taskColumns).map(([status, columnTasks]) => (
            <div
              key={status}
              className={`rounded-lg border-2 border-dashed p-4 min-h-96 ${columnColors[status]}`}
            >
              <div className="flex justify-between items-center mb-4">
                <h2 className="text-lg font-semibold text-gray-900">
                  {columnTitles[status]}
                </h2>
                <span className="bg-white text-gray-600 px-2 py-1 rounded-full text-sm">
                  {columnTasks.length}
                </span>
              </div>
              
              <SortableContext
                items={columnTasks.map(task => task.id)}
                strategy={verticalListSortingStrategy}
              >
                <TaskList tasks={columnTasks} />
              </SortableContext>
            </div>
          ))}
        </div>

        <DragOverlay>
          {activeTask ? (
            <div className="bg-white p-4 rounded-lg shadow-lg border border-gray-200 opacity-90">
              <h3 className="font-medium text-gray-900">{activeTask.title}</h3>
              <p className="text-sm text-gray-600 mt-1">{activeTask.description}</p>
            </div>
          ) : null}
        </DragOverlay>
      </DndContext>

      {/* Create Task Modal */}
      {showCreateModal && (
        <CreateTaskModal
          isOpen={showCreateModal}
          onClose={() => setShowCreateModal(false)}
        />
      )}
    </div>
  );
};

export default TaskBoard;
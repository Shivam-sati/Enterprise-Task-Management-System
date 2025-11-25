import React, { useEffect, useState } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { DndContext, closestCenter, KeyboardSensor, PointerSensor, useSensor, useSensors } from '@dnd-kit/core';
import { arrayMove, SortableContext, sortableKeyboardCoordinates, verticalListSortingStrategy } from '@dnd-kit/sortable';
import { fetchTasks, reorderTasks, setFilters } from '../../store/slices/taskSlice';
import TaskItem from './TaskItem';
import TaskFilters from './TaskFilters';
import CreateTaskModal from './CreateTaskModal';
import { PlusIcon } from '@heroicons/react/24/outline';

const TaskList = () => {
  const dispatch = useDispatch();
  const { tasks, filteredTasks, isLoading, filters } = useSelector((state) => state.tasks);
  const [showCreateModal, setShowCreateModal] = useState(false);

  const sensors = useSensors(
    useSensor(PointerSensor),
    useSensor(KeyboardSensor, {
      coordinateGetter: sortableKeyboardCoordinates,
    })
  );

  useEffect(() => {
    dispatch(fetchTasks());
  }, [dispatch]);

  const handleDragEnd = (event) => {
    const { active, over } = event;

    if (active.id !== over.id) {
      const oldIndex = tasks.findIndex((task) => task.taskId === active.id);
      const newIndex = tasks.findIndex((task) => task.taskId === over.id);

      dispatch(reorderTasks({
        sourceIndex: oldIndex,
        destinationIndex: newIndex,
      }));
    }
  };

  const getFilteredTasks = () => {
    let filtered = [...tasks];

    if (filters.status !== 'all') {
      filtered = filtered.filter(task => task.status === filters.status);
    }

    if (filters.priority !== 'all') {
      filtered = filtered.filter(task => task.priority === filters.priority);
    }

    if (filters.tags.length > 0) {
      filtered = filtered.filter(task => 
        task.tags.some(tag => filters.tags.includes(tag))
      );
    }

    if (filters.search) {
      const searchLower = filters.search.toLowerCase();
      filtered = filtered.filter(task => 
        task.title.toLowerCase().includes(searchLower) ||
        task.description.toLowerCase().includes(searchLower)
      );
    }

    return filtered;
  };

  const displayTasks = getFilteredTasks();

  if (isLoading) {
    return (
      <div className="flex justify-center items-center h-64">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-600"></div>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <div className="flex justify-between items-center">
        <h1 className="text-2xl font-bold text-gray-900">My Tasks</h1>
        <button
          onClick={() => setShowCreateModal(true)}
          className="inline-flex items-center px-4 py-2 border border-transparent text-sm font-medium rounded-md shadow-sm text-white bg-primary-600 hover:bg-primary-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-primary-500"
        >
          <PlusIcon className="h-5 w-5 mr-2" />
          New Task
        </button>
      </div>

      <TaskFilters />

      <div className="bg-white shadow rounded-lg">
        {displayTasks.length === 0 ? (
          <div className="text-center py-12">
            <p className="text-gray-500 text-lg">No tasks found</p>
            <p className="text-gray-400 text-sm mt-2">
              {filters.search || filters.status !== 'all' || filters.priority !== 'all' || filters.tags.length > 0
                ? 'Try adjusting your filters'
                : 'Create your first task to get started'
              }
            </p>
          </div>
        ) : (
          <DndContext
            sensors={sensors}
            collisionDetection={closestCenter}
            onDragEnd={handleDragEnd}
          >
            <SortableContext items={displayTasks.map(task => task.taskId)} strategy={verticalListSortingStrategy}>
              <div className="divide-y divide-gray-200">
                {displayTasks.map((task) => (
                  <TaskItem key={task.taskId} task={task} />
                ))}
              </div>
            </SortableContext>
          </DndContext>
        )}
      </div>

      {showCreateModal && (
        <CreateTaskModal
          isOpen={showCreateModal}
          onClose={() => setShowCreateModal(false)}
        />
      )}
    </div>
  );
};

export default TaskList;
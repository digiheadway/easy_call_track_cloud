import React, { useState, useEffect } from 'react';
import { useAppContext } from '../../contexts/AppContext';
import {
  format,
  startOfWeek,
  endOfWeek,
  startOfMonth,
  endOfMonth,
  eachDayOfInterval,
  isToday,
  isSameDay,
  isEqual,
  addWeeks,
  subWeeks,
  getWeek,
} from 'date-fns';
import TodoViewModal from '../todos/TodoViewModal';
import TodoActionModal from '../todos/TodoActionModal';
import { Todo, TodoType } from '../../types';
import Badge from '../ui/Badge';
import {
  CheckSquare,
  CheckCircle,
  XCircle,
  RotateCcw,
    CalendarClock,
  PhoneCall,
  Users2,
  ChevronLeft,
  ChevronRight,
  Calendar as CalendarIcon,
} from 'lucide-react';

const Calendar: React.FC = () => {
  const { todos, fetchTodos, getLeadById } = useAppContext();
  const [currentDate, setCurrentDate] = React.useState(new Date());
  const [selectedDate, setSelectedDate] = useState<Date | null>(null);
  const [selectedTodo, setSelectedTodo] = useState<Todo | null>(null);
  const [isViewModalOpen, setIsViewModalOpen] = useState(false);
  const [isActionModalOpen, setIsActionModalOpen] = useState(false);
  const [actionType, setActionType] = useState<
    'complete' | 'cancel' | 'reschedule' | 'reopen'
  >('complete');
  const [selectedTypes, setSelectedTypes] = useState<TodoType[]>([]);
  const [showClosedTasks, setShowClosedTasks] = useState(false);
  const [view, setView] = useState<'week' | 'month'>('week');

  useEffect(() => {
    fetchTodos();
  }, []);

  const weekStart = startOfWeek(currentDate, { weekStartsOn: 0 });
  const weekEnd = endOfWeek(currentDate, { weekStartsOn: 0 });
  const daysInView = eachDayOfInterval({
    start: view === 'week' ? weekStart : startOfMonth(currentDate),
    end: view === 'week' ? weekEnd : endOfMonth(currentDate),
  });

  const getTodosForDay = (date: Date) => {
    return todos
      .filter(
        (todo) =>
          isSameDay(new Date(todo.dateTime), date) &&
          (selectedTypes.length === 0 || selectedTypes.includes(todo.type)) &&
          (showClosedTasks || todo.status === 'Pending')
      )
      .sort(
        (a, b) =>
          new Date(a.dateTime).getTime() - new Date(b.dateTime).getTime()
      );
  };

  const getTodoCountsByType = (date: Date) => {
    const todosForDay = getTodosForDay(date);
    const counts: Record<string, number> = {};
    
    todosForDay.forEach(todo => {
      counts[todo.type] = (counts[todo.type] || 0) + 1;
    });
    
    return counts;
  };

  const handleTypeToggle = (type: TodoType) => {
    setSelectedTypes((prev) =>
      prev.includes(type) ? prev.filter((t) => t !== type) : [...prev, type]
    );
  };

  const handleTodoClick = (todo: Todo) => {
    setSelectedTodo(todo);
    setIsViewModalOpen(true);
  };

  const handleTodoAction = (
    todo: Todo,
    action: 'complete' | 'cancel' | 'reschedule' | 'reopen'
  ) => {
    setSelectedTodo(todo);
    setActionType(action);
    setIsActionModalOpen(true);
  };

  const handleDayClick = (date: Date) => {
    setSelectedDate(isEqual(date, selectedDate || new Date()) ? null : date);
  };

  const navigatePrevious = () => {
    setCurrentDate((prev) =>
      view === 'week'
        ? subWeeks(prev, 1)
        : new Date(prev.setMonth(prev.getMonth() - 1))
    );
  };

  const navigateNext = () => {
    setCurrentDate((prev) =>
      view === 'week'
        ? addWeeks(prev, 1)
        : new Date(prev.setMonth(prev.getMonth() + 1))
    );
  };

  const selectedDayTodos = selectedDate ? getTodosForDay(selectedDate) : [];

  const getTypeColor = (type: TodoType) => {
    switch (type) {
      case 'Meeting':
        return 'purple';
      case 'Follow Up':
        return 'blue';
      case 'Schedule Site Visit':
        return 'green';
      default:
        return 'gray';
    }
  };

  return (
    <div className="p-2">
      <div className="bg-white rounded-lg shadow">
        <div className="flex items-center justify-between p-4 border-b">
          <h2 className="text-lg font-semibold">
            {view === 'week'
              ? `Week ${getWeek(currentDate)} - ${format(
                  currentDate,
                  'MMMM yyyy'
                )}`
              : format(currentDate, 'MMMM yyyy')}
          </h2>
          <div className="flex items-center space-x-4">
            <div className="flex items-center space-x-2">
              <button
                onClick={() => setView('week')}
                className={`btn ${
                  view === 'week' ? 'btn-primary' : 'btn-outline'
                } py-1`}
              >
                W
              </button>
              <button
                onClick={() => setView('month')}
                className={`btn ${
                  view === 'month' ? 'btn-primary' : 'btn-outline'
                } py-1`}
              >
                M
              </button>
            </div>
            <div className="flex space-x-2">
              <button
                onClick={navigatePrevious}
                className="btn btn-outline py-1"
              >
                <ChevronLeft size={18} />
              </button>
              <button
                onClick={() => setCurrentDate(new Date())}
                className="btn btn-outline py-1"
              >
                Today
              </button>
              <button onClick={navigateNext} className="btn btn-outline py-1">
                <ChevronRight size={18} />
              </button>
            </div>
          </div>
        </div>

        <div className="p-4 border-b flex justify-between items-center">
          <label className="flex items-center space-x-2">
            <input
              type="checkbox"
              checked={showClosedTasks}
              onChange={(e) => setShowClosedTasks(e.target.checked)}
              className="rounded border-gray-300 text-blue-600 focus:ring-blue-500"
            />
            <span className="text-sm text-gray-700">Show closed tasks</span>
          </label>

          <div className="flex space-x-2">
            <button
              className={`btn ${
                selectedTypes.includes('Follow Up')
                  ? 'btn-primary'
                  : 'btn-outline'
              }`}
              onClick={() => handleTypeToggle('Follow Up')}
              title="Follow-ups"
            >
              <PhoneCall size={18} />
            </button>
            <button
              className={`btn ${
                selectedTypes.includes('Meeting')
                  ? 'btn-primary'
                  : 'btn-outline'
              }`}
              onClick={() => handleTypeToggle('Meeting')}
              title="Meetings"
            >
              <Users2 size={18} />
            </button>
            <button
              className={`btn ${
                selectedTypes.includes('Other') ? 'btn-primary' : 'btn-outline'
              }`}
              onClick={() => handleTypeToggle('Other')}
              title="Other Tasks"
            >
              <CheckSquare size={18} />
            </button>
          </div>
        </div>

        <div className="overflow-x-auto">
          <div className="min-w-[800px]">
            <div className="grid grid-cols-7 gap-px bg-gray-200">
              {['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'].map((day) => (
                <div
                  key={day}
                  className="bg-gray-50 p-2 text-center text-sm font-medium text-gray-500"
                >
                  {day}
                </div>
              ))}

              {daysInView.map((day, dayIdx) => {
                const dayTodos = getTodosForDay(day);
                const isSelected = selectedDate && isSameDay(day, selectedDate);
                const todoCounts = getTodoCountsByType(day);

                return (
                  <div
                    key={day.toString()}
                    onClick={() => handleDayClick(day)}
                    className={`${
                      view === 'week' ? 'min-h-[200px]' : 'min-h-[120px]'
                    } bg-white p-2 cursor-pointer transition-colors ${
                      isToday(day) ? 'bg-blue-50' : ''
                    } ${isSelected ? 'ring-2 ring-blue-500' : ''} hover:bg-gray-50`}
                  >
                    <p
                      className={`text-sm ${
                        isToday(day) ? 'font-bold text-blue-600' : 'text-gray-500'
                      }`}
                    >
                      {format(day, 'd')}
                    </p>

                    <div className="mt-2 space-y-2">
                      {view === 'week' ? (
                        dayTodos.map((todo) => (
                          <div
                            key={todo.id}
                            className={`rounded-xl p-2 text-sm shadow-sm transition-all cursor-pointer group border
                              ${
                                todo.status === 'Pending'
                                  ? 'bg-blue-50 text-blue-800 border-blue-200'
                                  : todo.status === 'Completed'
                                  ? 'bg-green-50 text-green-800 border-green-200'
                                  : 'bg-red-50 text-red-800 border-red-200'
                              } hover:shadow-md hover:bg-opacity-90`}
                            onClick={(e) => {
                              e.stopPropagation();
                              handleTodoClick(todo);
                            }}
                          >
                            <div className="flex items-center justify-between mb-1">
                              <span className="font-medium">
                                {format(new Date(todo.dateTime), 'h:mm a')} -{' '}
                                <span className="font-semibold text-gray-600">
                                  #{todo.id}
                                </span>
                              </span>
                            </div>
                            <div className="truncate text-xs text-gray-800 mb-2">
                              {todo.description || todo.type}
                            </div>
                            <Badge
                              label={todo.type}
                              color={getTypeColor(todo.type)}
                              small
                            />
                          </div>
                        ))
                      ) : (
                        // Month view - show counts by type
                        <div className="space-y-1">
                          {Object.entries(todoCounts).map(([type, count]) => (
                            count > 0 && (
                              <div key={type} className="text-xs text-gray-600">
                                {count} {type}
                              </div>
                            )
                          ))}
                        </div>
                      )}
                    </div>
                  </div>
                );
              })}
            </div>
          </div>
        </div>

        {selectedDate && selectedDayTodos.length > 0 && (
          <div className="border-t">
            <div className="p-4">
              <h3 className="text-lg font-medium mb-4">
                Tasks for {format(selectedDate, 'MMMM d, yyyy')}
              </h3>
              <div className="space-y-3">
                {selectedDayTodos.map((todo) => {
                  const lead = getLeadById(todo.leadId);
                  return (
                    <div
                      key={todo.id}
                      className="flex items-start justify-between p-4 bg-white rounded-xl border shadow-sm hover:shadow-md transition-all"
                    >
                      <div
                        className="flex-1 space-y-1 cursor-pointer"
                        onClick={() => handleTodoClick(todo)}
                      >
                        <div className="flex items-center flex-wrap gap-2 text-sm">
                          <Badge
                            label={todo.type}
                            color={getTypeColor(todo.type)}
                          />
                          {todo.status !== 'Pending' && (
                            <Badge
                              label={todo.status}
                              color={
                                todo.status === 'Completed' ? 'green' : 'red'
                              }
                            />
                          )}
                        </div>

                      

                        {todo.description && (
                          <p className="text-sm text-gray-600">
                            {todo.description}
                          </p>
                        )}

                        {lead && (
                          <button
                            className="text-blue-700 hover:text-blue-900 text-sm"
                            onClick={(e) => {
                              e.stopPropagation();
                              window.location.href = `/leads/${lead.id}`;
                            }}
                          >
                            #{lead.id} – {lead.name} – {lead.budget}L
                          </button>
                        )}

                        <div className="text-xs text-gray-500 flex items-center gap-2 pt-1">
                          <CalendarIcon size={14} className="text-gray-400" />
                          {format(new Date(todo.dateTime), 'h:mm a')}
                          <span className="text-gray-400 font-medium">
                            #{todo.id}
                          </span>
                        </div>
                      </div>

                      <div className="flex items-center gap-2 ml-4 mt-1">
                        {todo.status === 'Pending' ? (
                          <>
                            <button
                              className="p-1.5 text-green-600 hover:bg-green-50 rounded-full"
                              onClick={() => handleTodoAction(todo, 'complete')}
                              title="Mark as Completed"
                            >
                              <CheckCircle size={18} />
                            </button>
                            <button
                              className="p-1.5 text-indigo-600 hover:bg-indigo-50 rounded-full"
                              onClick={() => handleTodoAction(todo, 'reschedule')}
                              title="Reschedule Task"
                            >
                              <CalendarClock size={18} />
                            </button>
                            <button
                              className="p-1.5 text-red-600 hover:bg-red-50 rounded-full"
                              onClick={() => handleTodoAction(todo, 'cancel')}
                              title="Cancel Task"
                            >
                              <XCircle size={18} />
                            </button>
                          </>
                        ) : (
                          <button
                            className="p-1.5 text-blue-600 hover:bg-blue-50 rounded-full"
                            onClick={() => handleTodoAction(todo, 'reopen')}
                            title="Reopen Task"
                          >
                            <RotateCcw size={18} />
                          </button>
                        )}
                      </div>
                    </div>
                  );
                })}
              </div>
            </div>
          </div>
        )}
      </div>

      {selectedTodo && (
        <>
          <TodoViewModal
            isOpen={isViewModalOpen}
            onClose={() => {
              setIsViewModalOpen(false);
              setSelectedTodo(null);
            }}
            todo={selectedTodo}
          />
          <TodoActionModal
            isOpen={isActionModalOpen}
            onClose={() => {
              setIsActionModalOpen(false);
              setSelectedTodo(null);
            }}
            todo={selectedTodo}
            actionType={actionType}
          />
        </>
      )}
    </div>
  );
};

export default Calendar;
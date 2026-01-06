import { StatusBar } from 'expo-status-bar';
import {
  StyleSheet,
  Text,
  View,
  ScrollView,
  TouchableOpacity,
  Platform,
  Alert,
  ActivityIndicator,
  Modal,
  TextInput,
  RefreshControl
} from 'react-native';
import { useEffect, useState, useRef } from 'react';
import { LinearGradient } from 'expo-linear-gradient';
import * as Notifications from 'expo-notifications';
import * as Device from 'expo-device';

// Configure notifications
Notifications.setNotificationHandler({
  handleNotification: async () => ({
    shouldShowAlert: true,
    shouldPlaySound: true,
    shouldSetBadge: true,
  }),
});

const API_URL = 'https://prop.digiheadway.in/api/v3/?action=get_tasks&page=1&per_page=20&sort_order=DESC';

export default function App() {
  const [tasks, setTasks] = useState([]);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [error, setError] = useState(null);
  const [permissionGranted, setPermissionGranted] = useState(false);

  // Reminder modal state
  const [reminderModalVisible, setReminderModalVisible] = useState(false);
  const [selectedTask, setSelectedTask] = useState(null);
  const [reminderDate, setReminderDate] = useState('');
  const [reminderTime, setReminderTime] = useState('');
  const [scheduledReminders, setScheduledReminders] = useState({});

  const notificationListener = useRef();
  const responseListener = useRef();

  useEffect(() => {
    registerForNotifications();
    fetchTasks();

    notificationListener.current = Notifications.addNotificationReceivedListener(notification => {
      console.log('Notification received:', notification);
    });

    responseListener.current = Notifications.addNotificationResponseReceivedListener(response => {
      console.log('Notification tapped:', response);
    });

    return () => {
      Notifications.removeNotificationSubscription(notificationListener.current);
      Notifications.removeNotificationSubscription(responseListener.current);
    };
  }, []);

  const registerForNotifications = async () => {
    if (Platform.OS === 'web') {
      setPermissionGranted(false);
      return;
    }

    if (!Device.isDevice) {
      setPermissionGranted(false);
      return;
    }

    const { status: existingStatus } = await Notifications.getPermissionsAsync();
    let finalStatus = existingStatus;

    if (existingStatus !== 'granted') {
      const { status } = await Notifications.requestPermissionsAsync();
      finalStatus = status;
    }

    setPermissionGranted(finalStatus === 'granted');
  };

  const fetchTasks = async () => {
    try {
      setError(null);
      const response = await fetch(API_URL, {
        headers: {
          'User-Agent': 'Mozilla/5.0 (Linux; Android 6.0; Nexus 5 Build/MRA58N) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Mobile Safari/537.36',
          'Referer': 'https://uptwn-sales2.netlify.app/',
        },
      });

      const data = await response.json();

      if (data.status === 'success') {
        setTasks(data.data || []);
      } else {
        setError('Failed to fetch tasks');
      }
    } catch (err) {
      console.error('Fetch error:', err);
      setError('Network error. Please try again.');
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  };

  const onRefresh = () => {
    setRefreshing(true);
    fetchTasks();
  };

  const openReminderModal = (task) => {
    setSelectedTask(task);
    // Default to task's scheduled time
    const taskDate = new Date(task.timedate);
    setReminderDate(taskDate.toISOString().split('T')[0]); // YYYY-MM-DD
    setReminderTime(taskDate.toTimeString().slice(0, 5)); // HH:MM
    setReminderModalVisible(true);
  };

  const scheduleReminder = async () => {
    if (!selectedTask) return;

    if (Platform.OS === 'web') {
      Alert.alert('Web Notice', 'Reminders work on mobile devices. The reminder time has been saved locally.');
      setScheduledReminders(prev => ({
        ...prev,
        [selectedTask.id]: `${reminderDate} ${reminderTime}`
      }));
      setReminderModalVisible(false);
      return;
    }

    if (!permissionGranted) {
      Alert.alert('Permission Required', 'Please enable notifications in settings.');
      return;
    }

    try {
      const reminderDateTime = new Date(`${reminderDate}T${reminderTime}:00`);
      const now = new Date();

      if (reminderDateTime <= now) {
        Alert.alert('Invalid Time', 'Please select a future time for the reminder.');
        return;
      }

      const secondsUntilReminder = Math.floor((reminderDateTime - now) / 1000);

      await Notifications.scheduleNotificationAsync({
        content: {
          title: `📋 Task Reminder: ${selectedTask.type}`,
          body: selectedTask.title,
          data: { taskId: selectedTask.id, leadName: selectedTask.lead?.name },
          sound: true,
        },
        trigger: {
          seconds: secondsUntilReminder,
        },
      });

      setScheduledReminders(prev => ({
        ...prev,
        [selectedTask.id]: `${reminderDate} ${reminderTime}`
      }));

      Alert.alert('Success! ✅', `Reminder set for ${formatDateTime(reminderDateTime)}`);
      setReminderModalVisible(false);
    } catch (err) {
      console.error('Schedule error:', err);
      Alert.alert('Error', 'Failed to schedule reminder.');
    }
  };

  const formatDateTime = (date) => {
    return new Date(date).toLocaleString('en-IN', {
      day: 'numeric',
      month: 'short',
      hour: '2-digit',
      minute: '2-digit',
    });
  };

  const formatTaskTime = (timedate) => {
    const date = new Date(timedate);
    const now = new Date();
    const diffMs = date - now;
    const diffHours = Math.floor(diffMs / (1000 * 60 * 60));
    const diffDays = Math.floor(diffHours / 24);

    if (diffDays < 0) return 'Overdue';
    if (diffDays === 0 && diffHours < 24) return `In ${diffHours}h`;
    if (diffDays === 1) return 'Tomorrow';
    return `In ${diffDays} days`;
  };

  const getStatusColor = (status) => {
    switch (status?.toLowerCase()) {
      case 'pending': return '#f59e0b';
      case 'completed': return '#10b981';
      case 'cancelled': return '#ef4444';
      default: return '#6b7280';
    }
  };

  const getPriorityBadge = (priority) => {
    const p = parseInt(priority) || 0;
    if (p >= 4) return { label: 'High', color: '#ef4444' };
    if (p >= 2) return { label: 'Medium', color: '#f59e0b' };
    return { label: 'Low', color: '#10b981' };
  };

  return (
    <LinearGradient
      colors={['#0f172a', '#1e293b', '#334155']}
      style={styles.container}
    >
      <StatusBar style="light" />

      {/* Header */}
      <View style={styles.header}>
        <Text style={styles.greeting}>Hi 👋</Text>
        <Text style={styles.title}>Your Tasks</Text>
        <Text style={styles.subtitle}>
          {loading ? 'Loading...' : `${tasks.length} tasks found`}
        </Text>
      </View>

      {/* Task List */}
      {loading ? (
        <View style={styles.centerContainer}>
          <ActivityIndicator size="large" color="#60a5fa" />
          <Text style={styles.loadingText}>Loading tasks...</Text>
        </View>
      ) : error ? (
        <View style={styles.centerContainer}>
          <Text style={styles.errorText}>❌ {error}</Text>
          <TouchableOpacity style={styles.retryButton} onPress={fetchTasks}>
            <Text style={styles.retryText}>Retry</Text>
          </TouchableOpacity>
        </View>
      ) : (
        <ScrollView
          style={styles.scrollView}
          contentContainerStyle={styles.scrollContent}
          refreshControl={
            <RefreshControl refreshing={refreshing} onRefresh={onRefresh} tintColor="#60a5fa" />
          }
        >
          {tasks.map((task) => {
            const priority = getPriorityBadge(task.lead?.priority);
            const hasReminder = scheduledReminders[task.id];

            return (
              <View key={task.id} style={styles.taskCard}>
                {/* Task Header */}
                <View style={styles.taskHeader}>
                  <View style={[styles.typeBadge, { backgroundColor: '#3b82f6' }]}>
                    <Text style={styles.typeText}>{task.type}</Text>
                  </View>
                  <View style={[styles.statusBadge, { backgroundColor: getStatusColor(task.status) }]}>
                    <Text style={styles.statusText}>{task.status}</Text>
                  </View>
                </View>

                {/* Lead Name */}
                <Text style={styles.leadName}>
                  👤 {task.lead?.name || 'Unknown'} • {task.lead?.phone || 'No phone'}
                </Text>

                {/* Task Title */}
                <Text style={styles.taskTitle} numberOfLines={2}>
                  {task.title}
                </Text>

                {/* Task Time */}
                <View style={styles.timeRow}>
                  <Text style={styles.taskTime}>
                    🕐 {formatDateTime(task.timedate)}
                  </Text>
                  <Text style={[styles.timeAgo, task.timedate < new Date().toISOString() && styles.overdue]}>
                    {formatTaskTime(task.timedate)}
                  </Text>
                </View>

                {/* Priority & Tags */}
                <View style={styles.metaRow}>
                  <View style={[styles.priorityBadge, { backgroundColor: priority.color }]}>
                    <Text style={styles.priorityText}>{priority.label} Priority</Text>
                  </View>
                  {task.lead?.tags && (
                    <Text style={styles.tags}>🏷️ {task.lead.tags}</Text>
                  )}
                </View>

                {/* Reminder Button */}
                <TouchableOpacity
                  style={[styles.reminderButton, hasReminder && styles.reminderSet]}
                  onPress={() => openReminderModal(task)}
                  activeOpacity={0.8}
                >
                  <Text style={styles.reminderIcon}>{hasReminder ? '✅' : '🔔'}</Text>
                  <Text style={styles.reminderButtonText}>
                    {hasReminder ? `Reminder: ${hasReminder}` : 'Set Reminder'}
                  </Text>
                </TouchableOpacity>
              </View>
            );
          })}
        </ScrollView>
      )}

      {/* Reminder Modal */}
      <Modal
        visible={reminderModalVisible}
        transparent
        animationType="slide"
        onRequestClose={() => setReminderModalVisible(false)}
      >
        <View style={styles.modalOverlay}>
          <View style={styles.modalContent}>
            <Text style={styles.modalTitle}>⏰ Set Reminder</Text>

            {selectedTask && (
              <Text style={styles.modalTaskTitle} numberOfLines={2}>
                {selectedTask.title}
              </Text>
            )}

            <Text style={styles.inputLabel}>Date (YYYY-MM-DD)</Text>
            <TextInput
              style={styles.input}
              value={reminderDate}
              onChangeText={setReminderDate}
              placeholder="2025-12-24"
              placeholderTextColor="#666"
            />

            <Text style={styles.inputLabel}>Time (HH:MM)</Text>
            <TextInput
              style={styles.input}
              value={reminderTime}
              onChangeText={setReminderTime}
              placeholder="14:30"
              placeholderTextColor="#666"
            />

            <View style={styles.modalButtons}>
              <TouchableOpacity
                style={[styles.modalButton, styles.cancelButton]}
                onPress={() => setReminderModalVisible(false)}
              >
                <Text style={styles.cancelButtonText}>Cancel</Text>
              </TouchableOpacity>

              <TouchableOpacity
                style={[styles.modalButton, styles.confirmButton]}
                onPress={scheduleReminder}
              >
                <Text style={styles.confirmButtonText}>Set Reminder</Text>
              </TouchableOpacity>
            </View>

            {Platform.OS === 'web' && (
              <Text style={styles.webNote}>
                📱 Run on mobile device for actual notifications
              </Text>
            )}
          </View>
        </View>
      </Modal>
    </LinearGradient>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
  },
  header: {
    paddingTop: 60,
    paddingHorizontal: 20,
    paddingBottom: 16,
  },
  greeting: {
    fontSize: 28,
    fontWeight: '800',
    color: '#fff',
  },
  title: {
    fontSize: 20,
    fontWeight: '600',
    color: '#60a5fa',
    marginTop: 4,
  },
  subtitle: {
    fontSize: 14,
    color: 'rgba(255,255,255,0.6)',
    marginTop: 4,
  },
  centerContainer: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
    padding: 20,
  },
  loadingText: {
    color: 'rgba(255,255,255,0.7)',
    fontSize: 16,
    marginTop: 16,
  },
  errorText: {
    color: '#ef4444',
    fontSize: 16,
    textAlign: 'center',
  },
  retryButton: {
    marginTop: 16,
    backgroundColor: '#3b82f6',
    paddingVertical: 12,
    paddingHorizontal: 24,
    borderRadius: 8,
  },
  retryText: {
    color: '#fff',
    fontWeight: '600',
  },
  scrollView: {
    flex: 1,
  },
  scrollContent: {
    paddingHorizontal: 16,
    paddingBottom: 30,
  },
  taskCard: {
    backgroundColor: 'rgba(255,255,255,0.08)',
    borderRadius: 16,
    padding: 16,
    marginBottom: 14,
    borderWidth: 1,
    borderColor: 'rgba(255,255,255,0.1)',
  },
  taskHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    marginBottom: 10,
  },
  typeBadge: {
    paddingVertical: 4,
    paddingHorizontal: 10,
    borderRadius: 6,
  },
  typeText: {
    color: '#fff',
    fontSize: 12,
    fontWeight: '600',
  },
  statusBadge: {
    paddingVertical: 4,
    paddingHorizontal: 10,
    borderRadius: 6,
  },
  statusText: {
    color: '#fff',
    fontSize: 12,
    fontWeight: '600',
  },
  leadName: {
    color: '#94a3b8',
    fontSize: 13,
    marginBottom: 8,
  },
  taskTitle: {
    color: '#fff',
    fontSize: 15,
    fontWeight: '600',
    lineHeight: 22,
    marginBottom: 10,
  },
  timeRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 10,
  },
  taskTime: {
    color: '#60a5fa',
    fontSize: 13,
  },
  timeAgo: {
    color: '#10b981',
    fontSize: 12,
    fontWeight: '600',
  },
  overdue: {
    color: '#ef4444',
  },
  metaRow: {
    flexDirection: 'row',
    alignItems: 'center',
    marginBottom: 12,
    flexWrap: 'wrap',
    gap: 8,
  },
  priorityBadge: {
    paddingVertical: 3,
    paddingHorizontal: 8,
    borderRadius: 4,
  },
  priorityText: {
    color: '#fff',
    fontSize: 11,
    fontWeight: '600',
  },
  tags: {
    color: '#94a3b8',
    fontSize: 12,
  },
  reminderButton: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    backgroundColor: 'rgba(96, 165, 250, 0.2)',
    paddingVertical: 12,
    borderRadius: 10,
    borderWidth: 1,
    borderColor: 'rgba(96, 165, 250, 0.3)',
  },
  reminderSet: {
    backgroundColor: 'rgba(16, 185, 129, 0.2)',
    borderColor: 'rgba(16, 185, 129, 0.3)',
  },
  reminderIcon: {
    fontSize: 16,
    marginRight: 8,
  },
  reminderButtonText: {
    color: '#fff',
    fontSize: 14,
    fontWeight: '600',
  },
  // Modal Styles
  modalOverlay: {
    flex: 1,
    backgroundColor: 'rgba(0,0,0,0.7)',
    justifyContent: 'center',
    alignItems: 'center',
    padding: 20,
  },
  modalContent: {
    backgroundColor: '#1e293b',
    borderRadius: 20,
    padding: 24,
    width: '100%',
    maxWidth: 400,
    borderWidth: 1,
    borderColor: 'rgba(255,255,255,0.1)',
  },
  modalTitle: {
    fontSize: 22,
    fontWeight: '700',
    color: '#fff',
    textAlign: 'center',
    marginBottom: 16,
  },
  modalTaskTitle: {
    color: '#94a3b8',
    fontSize: 14,
    textAlign: 'center',
    marginBottom: 20,
    lineHeight: 20,
  },
  inputLabel: {
    color: '#94a3b8',
    fontSize: 13,
    marginBottom: 6,
  },
  input: {
    backgroundColor: 'rgba(255,255,255,0.1)',
    borderRadius: 10,
    paddingVertical: 14,
    paddingHorizontal: 16,
    color: '#fff',
    fontSize: 16,
    marginBottom: 16,
    borderWidth: 1,
    borderColor: 'rgba(255,255,255,0.1)',
  },
  modalButtons: {
    flexDirection: 'row',
    gap: 12,
    marginTop: 8,
  },
  modalButton: {
    flex: 1,
    paddingVertical: 14,
    borderRadius: 10,
    alignItems: 'center',
  },
  cancelButton: {
    backgroundColor: 'rgba(255,255,255,0.1)',
  },
  cancelButtonText: {
    color: '#94a3b8',
    fontWeight: '600',
  },
  confirmButton: {
    backgroundColor: '#3b82f6',
  },
  confirmButtonText: {
    color: '#fff',
    fontWeight: '600',
  },
  webNote: {
    color: '#94a3b8',
    fontSize: 12,
    textAlign: 'center',
    marginTop: 16,
  },
});

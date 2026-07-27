import apiClient from './axios.config';

export const getNotifications = () => {
  return apiClient.get('/api/notifications');
};

export const getUnreadCount = () => {
  return apiClient.get('/api/notifications/unread-count');
};

export const markAllAsRead = () => {
  return apiClient.post('/api/notifications/mark-all-as-read');
};

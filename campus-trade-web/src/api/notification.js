import apiClient from './axios.config';

export const getNotifications = (params) => {
  return apiClient.get('/notifications', { params });
};

export const getUnreadCount = () => {
  return apiClient.get('/notifications/unread-count');
};

export const markAllAsRead = () => {
  return apiClient.post('/notifications/mark-all-as-read');
};

export const markAsRead = (id) => apiClient.post(`/notifications/${id}/read`);

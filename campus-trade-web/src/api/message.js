import apiClient from './axios.config';

export const sendMessage = (data) => {
  return apiClient.post('/messages', data);
};

export const getMessageHistory = (otherUserId) => {
  return apiClient.get(`/messages/history/${otherUserId}`);
};

// 【新增】获取会话列表的API函数
export const getConversations = () => {
  return apiClient.get('/messages/conversations');
};

export const getMessageUnreadCount = () => {
  return apiClient.get('/messages/unread-count');
};

export const markConversationAsRead = (otherUserId) => {
  return apiClient.post(`/messages/read/${otherUserId}`);
};

import apiClient from './axios.config';

export const getMyPurchases = () => {
  return apiClient.get(`/orders/my-purchases`);
};

export const getMySales = () => {
  return apiClient.get(`/orders/my-sales`);
};

export const createOrder = (data) => {
    return apiClient.post('/orders', data);
};

export const cancelOrder = (orderId) => {
  return apiClient.post(`/orders/${orderId}/cancel`);
};

export const confirmOrderCompletion = (orderId) => {
  return apiClient.post(`/orders/${orderId}/confirm-completion`);
};

export const shipOrder = (orderId, data) => {
  return apiClient.post(`/orders/${orderId}/ship`, data);
};

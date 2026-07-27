import apiClient from './axios.config';

export const suggestPublish = (payload) => {
  return apiClient.post('/ai/publish/suggest', payload);
};

export const suggestPrice = (payload) => {
  return apiClient.post('/ai/price/suggest', payload);
};

export const checkProductRisk = (payload) => {
  return apiClient.post('/products/risk-check', payload);
};

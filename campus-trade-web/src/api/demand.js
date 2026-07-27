import apiClient from './axios.config';

export const createDemand = (payload) => {
  return apiClient.post('/demands', payload);
};

export const getMyDemands = () => {
  return apiClient.get('/demands/my');
};

export const deleteDemand = (id) => {
  return apiClient.delete(`/demands/${id}`);
};

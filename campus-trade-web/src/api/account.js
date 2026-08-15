import apiClient from './axios.config';

export const getAccount = () => apiClient.get('/account');
export const recharge = (data) => apiClient.post('/account/recharges', data);
export const getAccountFlows = (params) => apiClient.get('/account/flows', { params });
export const getRechargeOrders = (params) => apiClient.get('/account/recharges', { params });

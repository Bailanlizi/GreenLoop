import apiClient from './axios.config';

// --- User Management ---
export const getAllUsers = (params) => {
    return apiClient.get('/admin/users', { params });
};
export const updateUserStatus = (id, status) => {
    return apiClient.put(`/admin/users/${id}/status`, { status });
};
export const createUserAdmin = (data) => {
    return apiClient.post('/admin/users', data);
};
export const updateUserAdmin = (id, data) => {
    return apiClient.put(`/admin/users/${id}`, data);
};
export const deleteUserAdmin = (id) => {
    return apiClient.delete(`/admin/users/${id}`);
};

// --- Product Management ---
export const getAllProductsAdmin = (params) => {
    return apiClient.get('/admin/products', { params });
};
export const updateProductStatusAdmin = (id, status) => {
    return apiClient.put(`/admin/products/${id}/status`, { status });
};
export const createProductByAdmin = (data) => {
    return apiClient.post('/admin/products', data);
};
export const updateProductByAdmin = (id, data) => {
    return apiClient.put(`/admin/products/${id}`, data);
};
export const deleteProductAdmin = (id) => {
    return apiClient.delete(`/admin/products/${id}`);
};
export const getProductDetailAdmin = (id) => {
    return apiClient.get(`/admin/products/${id}`);
};

// --- Order Management ---
// 【修改】让 getAllOrdersAdmin 函数可以接收分页参数
export const getAllOrdersAdmin = (params) => {
    return apiClient.get('/admin/orders', { params });
};
export const forceCancelOrderAdmin = (id) => {
    return apiClient.post(`/admin/orders/${id}/force-cancel`);
};

// --- Finance Audit ---
export const getFinanceAccounts = (params) => apiClient.get('/admin/finance/accounts', { params });
export const getFinancePayments = (params) => apiClient.get('/admin/finance/payments', { params });
export const getFinanceRefunds = (params) => apiClient.get('/admin/finance/refunds', { params });
export const getFinanceSettlements = (params) => apiClient.get('/admin/finance/settlements', { params });
export const getFinanceFlows = (params) => apiClient.get('/admin/finance/flows', { params });


// --- Location Management ---
export const getAllLocationsAdmin = () => { return apiClient.get('/admin/locations'); };
export const addLocationAdmin = (data) => { return apiClient.post('/admin/locations', data); };
export const updateLocationAdmin = (id, data) => { return apiClient.put(`/admin/locations/${id}`, data); };
export const deleteLocationAdmin = (id) => { return apiClient.delete(`/admin/locations/${id}`); };

// --- Dashboard Stats ---
export const getSummaryStats = () => {
    return apiClient.get('/admin/stats/summary');
};
export const getDailyRegistrationStats = () => {
    return apiClient.get('/admin/stats/daily-registrations');
};
export const getUserGrowthStats = (days = 15) => {
    return apiClient.get('/admin/stats/user-growth', { params: { days } });
};
export const getOrderTrendStats = (days = 15) => {
    return apiClient.get('/admin/stats/order-trends', { params: { days } });
};
export const getProductTrendStats = (days = 15) => {
    return apiClient.get('/admin/stats/product-trends', { params: { days } });
};

// --- Delivery Management ---
export const getDeliveryStats = () => {
    return apiClient.get('/admin/delivery/stats');
};
export const exportDeliveryOrders = (params) => {
    return apiClient.get('/admin/delivery/export', { 
        params, 
        responseType: 'blob' 
    });
};



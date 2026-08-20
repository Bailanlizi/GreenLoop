import apiClient from './axios.config';

// 添加收藏
export const addFavorite = (productId) => {
  return apiClient.post(`/products/${productId}/favorite`);
};

// 取消收藏
export const removeFavorite = (productId) => {
  return apiClient.delete(`/products/${productId}/favorite`);
};

// 获取当前用户收藏的所有商品ID
export const getMyFavoriteIds = () => {
  return apiClient.get('/me/favorites/ids');
};

// 获取当前用户收藏的所有商品详情
export const getMyFavorites = () => {
    return apiClient.get('/me/favorites');
};

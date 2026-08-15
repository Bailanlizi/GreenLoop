<template>
  <div class="delivery-management">
    <div class="toolbar"><h2>配送监控</h2><el-button @click="refresh">刷新</el-button></div>
    <div class="stats" v-if="stats">
      <span>待发货：{{ stats.awaitingShipment }}</span><span>已发货：{{ stats.shipped }}</span><span>已完成：{{ stats.completed }}</span><span>订单总数：{{ stats.total }}</span>
    </div>
    <div class="filters"><el-input v-model="filters.orderId" placeholder="订单编号" clearable @keyup.enter="fetchOrders" /><el-select v-model="filters.deliveryMethod" clearable placeholder="履约方式" @change="fetchOrders"><el-option label="快递配送" value="SHIPPING" /><el-option label="线下面交" value="MEETUP" /></el-select><el-button type="primary" @click="fetchOrders">查询</el-button></div>
    <el-table :data="orders" v-loading="loading">
      <el-table-column prop="id" label="订单编号" min-width="180" /><el-table-column prop="productTitle" label="商品" min-width="160" /><el-table-column prop="buyerNickname" label="买家" width="120" /><el-table-column prop="sellerNickname" label="卖家" width="120" />
      <el-table-column label="状态" width="120"><template #default="scope">{{ formatStatus(scope.row.orderStatus) }}</template></el-table-column>
      <el-table-column prop="shippingProvider" label="快递公司" width="120" /><el-table-column prop="trackingNumber" label="快递单号" min-width="150" />
    </el-table>
  </div>
</template>

<script setup>
import { reactive, ref, onMounted } from 'vue';
import { getAllOrdersAdmin, getDeliveryStats } from '../../api/admin';

const orders = ref([]);
const stats = ref(null);
const loading = ref(false);
const filters = reactive({ orderId: '', deliveryMethod: 'SHIPPING' });
const statusMap = { AWAITING_SHIPMENT: '待发货', SHIPPED: '已发货', COMPLETED: '已完成', CANCELLED: '已取消' };
const formatStatus = (status) => statusMap[status] || '未知';
const fetchOrders = async () => { loading.value = true; try { const response = await getAllOrdersAdmin({ ...filters, page: 1, size: 100 }); orders.value = response.data.data.list; } finally { loading.value = false; } };
const fetchStats = async () => { const response = await getDeliveryStats(); stats.value = response.data.data; };
const refresh = () => Promise.all([fetchOrders(), fetchStats()]);
onMounted(refresh);
</script>

<style scoped>
.toolbar, .filters, .stats { display: flex; align-items: center; gap: 12px; margin-bottom: 16px; }
.toolbar { justify-content: space-between; }
.stats { color: var(--el-text-color-regular); }
</style>

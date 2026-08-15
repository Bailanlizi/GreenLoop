<template>
  <div class="order-management">
    <div class="toolbar">
      <h2>订单管理</h2>
      <div class="actions">
        <el-input v-model="filters.orderId" placeholder="订单编号" clearable @keyup.enter="fetchOrders" />
        <el-select v-model="filters.deliveryMethod" placeholder="履约方式" clearable @change="fetchOrders">
          <el-option label="线下面交" value="MEETUP" /><el-option label="快递配送" value="SHIPPING" />
        </el-select>
        <el-button type="primary" @click="fetchOrders">查询</el-button>
      </div>
    </div>
    <el-table :data="orders" v-loading="loading">
      <el-table-column prop="id" label="订单编号" min-width="180" />
      <el-table-column prop="productTitle" label="商品" min-width="160" />
      <el-table-column prop="buyerNickname" label="买家" width="120" />
      <el-table-column prop="sellerNickname" label="卖家" width="120" />
      <el-table-column label="履约方式" width="120"><template #default="scope">{{ scope.row.deliveryMethod === 'SHIPPING' ? '快递配送' : '线下面交' }}</template></el-table-column>
      <el-table-column label="状态" width="120"><template #default="scope">{{ formatStatus(scope.row.orderStatus) }}</template></el-table-column>
      <el-table-column label="操作" width="130" align="center"><template #default="scope"><el-button v-if="canForceCancel(scope.row)" type="danger" size="small" @click="forceCancel(scope.row)">强制取消</el-button><span v-else>--</span></template></el-table-column>
    </el-table>
    <el-pagination v-if="pagination.total" layout="total, prev, pager, next" :total="pagination.total" v-model:current-page="pagination.page" @current-change="fetchOrders" class="pagination" />
  </div>
</template>

<script setup>
import { reactive, ref, onMounted } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { forceCancelOrderAdmin, getAllOrdersAdmin } from '../../api/admin';

const orders = ref([]);
const loading = ref(false);
const filters = reactive({ orderId: '', deliveryMethod: '' });
const pagination = reactive({ page: 1, size: 10, total: 0 });
const statusMap = { AWAITING_MEETUP: '待交易', AWAITING_SHIPMENT: '待发货', SHIPPED: '已发货', COMPLETED: '已完成', CANCELLED: '已取消' };
const formatStatus = (status) => statusMap[status] || '未知';
const canForceCancel = (order) => ['AWAITING_MEETUP', 'AWAITING_SHIPMENT'].includes(order.orderStatus);

const fetchOrders = async () => {
  loading.value = true;
  try {
    const response = await getAllOrdersAdmin({ ...filters, page: pagination.page, size: pagination.size });
    orders.value = response.data.data.list;
    pagination.total = response.data.data.total;
  } finally { loading.value = false; }
};
const forceCancel = async (order) => {
  await ElMessageBox.confirm(`确定强制取消订单 ${order.id} 吗？商品将恢复可售。`, '强制取消订单', { type: 'warning' });
  await forceCancelOrderAdmin(order.id);
  ElMessage.success('订单已强制取消');
  fetchOrders();
};
onMounted(fetchOrders);
</script>

<style scoped>
.toolbar { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.actions { display: flex; gap: 8px; }
.pagination { margin-top: 16px; justify-content: center; }
</style>

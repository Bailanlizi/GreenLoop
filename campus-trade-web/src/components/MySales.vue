<template>
  <div>
    <h3>我卖出的宝贝</h3>
    <el-table :data="orders" style="width: 100%" v-loading="loading">
      <el-table-column label="商品" min-width="200">
        <template #default="scope">
          <div class="product-cell" @click="goToProduct(scope.row.productId)">
            <el-image :src="scope.row.productImage" fit="cover" class="product-image" />
            <span>{{ scope.row.productTitle }}</span>
          </div>
        </template>
      </el-table-column>
      <el-table-column prop="totalPrice" label="价格" width="100"><template #default="scope">￥{{ scope.row.totalPrice }}</template></el-table-column>
      <el-table-column prop="buyerNickname" label="买家" width="150" />
      <el-table-column label="订单状态" width="150"><template #default="scope"><span class="status-tag">{{ formatStatus(scope.row.orderStatus) }}</span></template></el-table-column>
      <el-table-column label="操作" width="150" align="center">
        <template #default="scope">
          <el-button v-if="scope.row.orderStatus === 'AWAITING_SHIPMENT' && scope.row.deliveryMethod === 'SHIPPING'" type="primary" size="small" @click="openShipDialog(scope.row)">发货</el-button>
          <span v-else>--</span>
        </template>
      </el-table-column>
    </el-table>
    <el-empty v-if="!loading && orders.length === 0" description="您还没有卖出任何商品" />

    <el-dialog v-model="shipDialogVisible" title="填写发货信息" width="420px">
      <el-form :model="shipForm" :rules="shipRules" ref="shipFormRef" label-position="top">
        <el-form-item label="快递公司" prop="shippingProvider"><el-input v-model="shipForm.shippingProvider" placeholder="请输入快递公司" /></el-form-item>
        <el-form-item label="快递单号" prop="trackingNumber"><el-input v-model="shipForm.trackingNumber" placeholder="请输入快递单号" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="shipDialogVisible = false">取消</el-button><el-button type="primary" :loading="submitting" @click="handleShip">确认发货</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup>
import { reactive, ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { getMySales, shipOrder } from '../api/order';

const router = useRouter();
const orders = ref([]);
const loading = ref(false);
const submitting = ref(false);
const shipDialogVisible = ref(false);
const shipFormRef = ref(null);
const selectedOrder = ref(null);
const shipForm = reactive({ shippingProvider: '', trackingNumber: '' });
const shipRules = {
  shippingProvider: [{ required: true, message: '请输入快递公司', trigger: 'blur' }],
  trackingNumber: [{ required: true, message: '请输入快递单号', trigger: 'blur' }],
};

const fetchOrders = async () => {
  loading.value = true;
  try {
    const res = await getMySales();
    orders.value = res.data.data;
  } finally {
    loading.value = false;
  }
};

const openShipDialog = (order) => {
  selectedOrder.value = order;
  shipForm.shippingProvider = '';
  shipForm.trackingNumber = '';
  shipDialogVisible.value = true;
};

const handleShip = async () => {
  const valid = await shipFormRef.value.validate().catch(() => false);
  if (!valid) return;
  submitting.value = true;
  try {
    await shipOrder(selectedOrder.value.id, shipForm);
    ElMessage.success('发货成功');
    shipDialogVisible.value = false;
    fetchOrders();
  } finally {
    submitting.value = false;
  }
};

const goToProduct = (id) => router.push(`/product/${id}`);
const statusMap = { PENDING_PAYMENT: '待买家支付', AWAITING_MEETUP: '待交易', AWAITING_SHIPMENT: '待发货', SHIPPED: '已发货', COMPLETED: '已完成', CANCELLED: '已取消' };
const formatStatus = (status) => statusMap[status] || '未知';
onMounted(fetchOrders);
</script>

<style scoped>
.product-cell { display: flex; align-items: center; cursor: pointer; gap: 10px; }
.product-image { width: 60px; height: 60px; border-radius: 4px; flex-shrink: 0; }
.status-tag { color: var(--el-text-color-regular); }
</style>

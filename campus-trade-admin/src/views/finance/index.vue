<template>
  <section class="finance-page">
    <div class="toolbar">
      <h2>资金管理</h2>
      <div class="filters">
        <el-input v-model="filters.keyword" :placeholder="activeTab === 'accounts' || activeTab === 'flows' ? '用户、账号或业务单号' : '订单编号'" clearable @keyup.enter="fetchData" />
        <el-select v-if="activeTab === 'flows'" v-model="filters.type" placeholder="业务类型" clearable @change="fetchData">
          <el-option v-for="item in flowTypes" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
        <el-select v-else-if="activeTab !== 'accounts'" v-model="filters.status" placeholder="状态" clearable @change="fetchData">
          <el-option label="冻结中" value="FROZEN" /><el-option label="已退款" value="REFUNDED" /><el-option label="已结算" value="SETTLED" /><el-option label="成功" value="SUCCESS" />
        </el-select>
        <el-button type="primary" @click="fetchData">查询</el-button>
      </div>
    </div>

    <el-tabs v-model="activeTab" @tab-change="changeTab">
      <el-tab-pane label="账户" name="accounts">
        <el-table :data="rows" v-loading="loading">
          <el-table-column prop="userId" label="用户ID" width="110" /><el-table-column prop="username" label="账号" min-width="130" /><el-table-column prop="nickname" label="用户" min-width="120" />
          <el-table-column label="可用余额" width="140"><template #default="scope">￥{{ money(scope.row.availableBalance) }}</template></el-table-column>
          <el-table-column label="冻结余额" width="140"><template #default="scope">￥{{ money(scope.row.frozenBalance) }}</template></el-table-column>
          <el-table-column prop="status" label="状态" width="100" /><el-table-column prop="updateTime" label="更新时间" width="180" />
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="支付" name="payments">
        <business-table :rows="rows" number-key="paymentNo" number-label="支付单号" party-key="buyerNickname" party-label="买家" />
      </el-tab-pane>
      <el-tab-pane label="退款" name="refunds">
        <business-table :rows="rows" number-key="refundNo" number-label="退款单号" party-key="buyerNickname" party-label="买家" />
      </el-tab-pane>
      <el-tab-pane label="结算" name="settlements">
        <el-table :data="rows" v-loading="loading">
          <el-table-column prop="settlementNo" label="结算单号" min-width="220" /><el-table-column prop="orderId" label="订单编号" min-width="150" />
          <el-table-column prop="buyerNickname" label="买家" width="110" /><el-table-column prop="sellerNickname" label="卖家" width="110" />
          <el-table-column label="金额" width="130"><template #default="scope">￥{{ money(scope.row.amount) }}</template></el-table-column>
          <el-table-column prop="status" label="状态" width="100" /><el-table-column prop="successTime" label="结算时间" width="180" />
        </el-table>
      </el-tab-pane>
      <el-tab-pane label="资金流水" name="flows">
        <el-table :data="rows" v-loading="loading">
          <el-table-column prop="flowNo" label="流水号" min-width="220" /><el-table-column prop="nickname" label="用户" width="110" />
          <el-table-column label="业务类型" width="130"><template #default="scope">{{ flowType(scope.row.businessType) }}</template></el-table-column>
          <el-table-column prop="businessNo" label="业务单号" min-width="220" />
          <el-table-column label="可用变动" width="120"><template #default="scope">{{ signed(scope.row.availableChange) }}</template></el-table-column>
          <el-table-column label="冻结变动" width="120"><template #default="scope">{{ signed(scope.row.frozenChange) }}</template></el-table-column>
          <el-table-column prop="createTime" label="时间" width="180" />
        </el-table>
      </el-tab-pane>
    </el-tabs>
    <el-pagination v-if="pagination.total" layout="total, prev, pager, next" :total="pagination.total" v-model:current-page="pagination.page" @current-change="fetchData" />
  </section>
</template>

<script>
import { defineComponent, h } from 'vue';
import { ElTable, ElTableColumn } from 'element-plus';

const BusinessTable = defineComponent({
  props: { rows: Array, numberKey: String, numberLabel: String, partyKey: String, partyLabel: String },
  setup(props) {
    return () => h(ElTable, { data: props.rows }, () => [
      h(ElTableColumn, { prop: props.numberKey, label: props.numberLabel, minWidth: 220 }),
      h(ElTableColumn, { prop: 'orderId', label: '订单编号', minWidth: 150 }),
      h(ElTableColumn, { prop: props.partyKey, label: props.partyLabel, width: 110 }),
      h(ElTableColumn, { prop: 'amount', label: '金额', width: 120 }),
      h(ElTableColumn, { prop: 'status', label: '状态', width: 100 }),
      h(ElTableColumn, { prop: 'createTime', label: '创建时间', width: 180 })
    ]);
  }
});

export default { components: { BusinessTable } };
</script>

<script setup>
import { onMounted, reactive, ref } from 'vue';
import { getFinanceAccounts, getFinanceFlows, getFinancePayments, getFinanceRefunds, getFinanceSettlements } from '../../api/admin';

const activeTab = ref('accounts');
const rows = ref([]);
const loading = ref(false);
const filters = reactive({ keyword: '', status: '', type: '' });
const pagination = reactive({ page: 1, size: 10, total: 0 });
const flowTypes = [
  { label: '充值', value: 'RECHARGE' }, { label: '支付冻结', value: 'PAYMENT_FREEZE' },
  { label: '退款', value: 'REFUND' }, { label: '买家结算', value: 'SETTLEMENT_OUT' }, { label: '销售入账', value: 'SETTLEMENT_IN' }
];
const flowTypeMap = Object.fromEntries(flowTypes.map(item => [item.value, item.label]));
const flowType = (type) => flowTypeMap[type] || type;
const money = (value) => Number(value || 0).toFixed(2);
const signed = (value) => `${Number(value) > 0 ? '+' : ''}${money(value)}`;

const fetchData = async () => {
  loading.value = true;
  try {
    const common = { page: pagination.page, size: pagination.size };
    const calls = {
      accounts: () => getFinanceAccounts({ ...common, keyword: filters.keyword }),
      payments: () => getFinancePayments({ ...common, orderId: filters.keyword, status: filters.status }),
      refunds: () => getFinanceRefunds({ ...common, orderId: filters.keyword, status: filters.status }),
      settlements: () => getFinanceSettlements({ ...common, orderId: filters.keyword, status: filters.status }),
      flows: () => getFinanceFlows({ ...common, keyword: filters.keyword, businessType: filters.type })
    };
    const data = (await calls[activeTab.value]()).data.data;
    rows.value = data.list;
    pagination.total = data.total;
  } finally { loading.value = false; }
};
const changeTab = () => { filters.keyword = ''; filters.status = ''; filters.type = ''; pagination.page = 1; fetchData(); };
onMounted(fetchData);
</script>

<style scoped>
.toolbar { display: flex; justify-content: space-between; align-items: center; gap: 16px; margin-bottom: 8px; }
.filters { display: flex; gap: 8px; }
.filters .el-input { width: 230px; }
.filters .el-select { width: 140px; }
.el-pagination { margin-top: 16px; justify-content: center; }
@media (max-width: 800px) { .toolbar, .filters { align-items: stretch; flex-direction: column; } .filters .el-input, .filters .el-select { width: 100%; } }
</style>

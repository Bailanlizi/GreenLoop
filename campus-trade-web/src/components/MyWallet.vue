<template>
  <section class="wallet-page" v-loading="loading">
    <div class="page-heading">
      <div><h3>资金账户</h3><span class="account-status">{{ account.status === 'ACTIVE' ? '正常' : '不可用' }}</span></div>
      <el-button type="primary" @click="rechargeVisible = true">模拟充值</el-button>
    </div>

    <div class="balance-grid">
      <div class="balance-item"><span>可用余额</span><strong>￥{{ money(account.availableBalance) }}</strong></div>
      <div class="balance-item"><span>冻结余额</span><strong>￥{{ money(account.frozenBalance) }}</strong></div>
    </div>

    <el-tabs v-model="activeTab" @tab-change="loadTab">
      <el-tab-pane label="资金流水" name="flows">
        <el-table :data="flows" empty-text="暂无资金流水">
          <el-table-column prop="createTime" label="时间" width="180" />
          <el-table-column label="业务类型" width="130"><template #default="scope">{{ flowType(scope.row.businessType) }}</template></el-table-column>
          <el-table-column prop="businessNo" label="业务单号" min-width="210" />
          <el-table-column label="可用变动" width="120"><template #default="scope"><span :class="amountClass(scope.row.availableChange)">{{ signedMoney(scope.row.availableChange) }}</span></template></el-table-column>
          <el-table-column label="冻结变动" width="120"><template #default="scope"><span :class="amountClass(scope.row.frozenChange)">{{ signedMoney(scope.row.frozenChange) }}</span></template></el-table-column>
          <el-table-column label="可用余额" width="120"><template #default="scope">￥{{ money(scope.row.availableAfter) }}</template></el-table-column>
          <el-table-column prop="remark" label="备注" min-width="130" />
        </el-table>
        <el-pagination v-if="flowPage.total" layout="total, prev, pager, next" :total="flowPage.total" v-model:current-page="flowPage.page" @current-change="fetchFlows" />
      </el-tab-pane>
      <el-tab-pane label="充值记录" name="recharges">
        <el-table :data="recharges" empty-text="暂无充值记录">
          <el-table-column prop="createTime" label="时间" width="180" />
          <el-table-column prop="rechargeNo" label="充值单号" min-width="220" />
          <el-table-column label="金额" width="140"><template #default="scope"><span class="income">+￥{{ money(scope.row.amount) }}</span></template></el-table-column>
          <el-table-column label="状态" width="100"><template #default>成功</template></el-table-column>
        </el-table>
      </el-tab-pane>
    </el-tabs>

    <el-dialog v-model="rechargeVisible" title="模拟充值" width="400px">
      <el-form label-position="top">
        <el-form-item label="充值金额">
          <el-input-number v-model="rechargeAmount" :min="0.01" :max="99999999.99" :precision="2" :step="100" controls-position="right" />
        </el-form-item>
      </el-form>
      <template #footer><el-button @click="rechargeVisible = false">取消</el-button><el-button type="primary" :loading="submitting" @click="submitRecharge">确认充值</el-button></template>
    </el-dialog>
  </section>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue';
import { ElMessage } from 'element-plus';
import { getAccount, getAccountFlows, getRechargeOrders, recharge } from '../api/account';

const account = reactive({ availableBalance: 0, frozenBalance: 0, status: '' });
const loading = ref(false);
const submitting = ref(false);
const rechargeVisible = ref(false);
const rechargeAmount = ref(100);
const activeTab = ref('flows');
const flows = ref([]);
const recharges = ref([]);
const flowPage = reactive({ page: 1, size: 10, total: 0 });

const requestId = () => globalThis.crypto?.randomUUID?.() || `${Date.now()}-${Math.random().toString(16).slice(2)}`;
const money = (value) => Number(value || 0).toFixed(2);
const signedMoney = (value) => `${Number(value) > 0 ? '+' : ''}￥${money(value)}`;
const amountClass = (value) => Number(value) > 0 ? 'income' : Number(value) < 0 ? 'expense' : '';
const flowTypeMap = { RECHARGE: '充值', PAYMENT_FREEZE: '支付冻结', REFUND: '退款', SETTLEMENT_OUT: '买家结算', SETTLEMENT_IN: '销售入账' };
const flowType = (type) => flowTypeMap[type] || type;

const fetchAccount = async () => Object.assign(account, (await getAccount()).data.data);
const fetchFlows = async () => {
  const data = (await getAccountFlows({ page: flowPage.page, size: flowPage.size })).data.data;
  flows.value = data.list;
  flowPage.total = data.total;
};
const fetchRecharges = async () => { recharges.value = (await getRechargeOrders({ page: 1, size: 50 })).data.data.list; };
const loadTab = (name) => name === 'flows' ? fetchFlows() : fetchRecharges();

const submitRecharge = async () => {
  submitting.value = true;
  try {
    await recharge({ amount: rechargeAmount.value, requestId: requestId() });
    ElMessage.success('充值成功');
    rechargeVisible.value = false;
    await Promise.all([fetchAccount(), fetchFlows(), fetchRecharges()]);
  } finally { submitting.value = false; }
};

onMounted(async () => {
  loading.value = true;
  try { await Promise.all([fetchAccount(), fetchFlows()]); } finally { loading.value = false; }
});
</script>

<style scoped>
.page-heading { display: flex; align-items: center; justify-content: space-between; margin-bottom: 20px; }
.page-heading h3 { display: inline; margin-right: 10px; }
.account-status { color: var(--el-color-success); font-size: 13px; }
.balance-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); border: 1px solid var(--el-border-color-lighter); margin-bottom: 24px; }
.balance-item { padding: 22px; display: flex; flex-direction: column; gap: 8px; }
.balance-item + .balance-item { border-left: 1px solid var(--el-border-color-lighter); }
.balance-item span { color: var(--el-text-color-secondary); }
.balance-item strong { font-size: 28px; font-weight: 600; }
.income { color: var(--el-color-success); }
.expense { color: var(--el-color-danger); }
.el-pagination { margin-top: 16px; justify-content: center; }
@media (max-width: 600px) { .balance-grid { grid-template-columns: 1fr; } .balance-item + .balance-item { border-left: 0; border-top: 1px solid var(--el-border-color-lighter); } }
</style>

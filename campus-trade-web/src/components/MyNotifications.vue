<template>
  <div class="notifications">
    <div class="section-header"><div><h3>通知中心</h3><p class="section-subtitle">交易、资金与求购匹配提醒</p></div><el-button size="small" type="primary" :disabled="!hasUnread" @click="handleMarkAll">全部已读</el-button></div>
    <el-radio-group v-model="readStatus" @change="reload"><el-radio-button label="ALL">全部</el-radio-button><el-radio-button label="UNREAD">未读</el-radio-button></el-radio-group>
    <div v-loading="loading" class="notification-list">
      <el-card v-for="item in notifications" :key="item.id" class="notification-card" :class="{ unread: !item.isRead }" shadow="never" @click="openNotification(item)">
        <div class="notification-body"><strong>{{ labelFor(item.type) }}</strong><p>{{ item.content }}</p><span>{{ formatDate(item.createTime) }}</span></div>
        <el-tag v-if="!item.isRead" size="small" type="danger">未读</el-tag>
      </el-card>
      <el-empty v-if="!loading && notifications.length === 0" description="暂无通知" />
    </div>
    <el-pagination v-if="total > 0" layout="prev, pager, next" :current-page="page" :page-size="20" :total="total" @current-change="changePage" />
  </div>
</template>
<script setup>
import { computed, onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { getNotifications, markAllAsRead, markAsRead } from '../api/notification';
const router=useRouter(), notifications=ref([]), loading=ref(false), readStatus=ref('ALL'), page=ref(1), total=ref(0);
const hasUnread=computed(()=>notifications.value.some(item=>!item.isRead));
const labels={ORDER_CREATED:'新订单',ORDER_PAID:'买家已付款',ORDER_SHIPPED:'订单已发货',ORDER_SETTLED:'款项已结算',ORDER_REFUNDED:'退款到账',ORDER_CANCELLED:'订单取消',ORDER_PAYMENT_EXPIRED:'支付超时',DEMAND_MATCH:'求购匹配'};
const labelFor=(type)=>labels[type]||'系统通知';
const fetchNotifications=async()=>{loading.value=true;try{const {data}=await getNotifications({page:page.value,size:20,readStatus:readStatus.value});notifications.value=data.data?.list||[];total.value=data.data?.total||0;}catch{ElMessage.error('加载通知失败');}finally{loading.value=false;}};
const reload=()=>{page.value=1;fetchNotifications();}; const changePage=(next)=>{page.value=next;fetchNotifications();};
const notifyReadChanged=()=>window.dispatchEvent(new Event('notification-read-state-changed'));
const openNotification=async(item)=>{if(!item.isRead){try{await markAsRead(item.id);item.isRead=true;notifyReadChanged();}catch{ElMessage.error('标记已读失败');return;}} const sales=['ORDER_CREATED','ORDER_PAID','ORDER_SETTLED'].includes(item.type); if(item.relatedType==='PRODUCT'||item.type==='DEMAND_MATCH') router.push(`/product/${item.relatedId}`); else router.push(sales?'/dashboard/sales':'/dashboard/purchases');};
const handleMarkAll=async()=>{try{await markAllAsRead();notifications.value.forEach(item=>item.isRead=true);notifyReadChanged();ElMessage.success('已全部标记为已读');}catch{ElMessage.error('操作失败');}};
const formatDate=v=>v?new Date(v).toLocaleString():''; onMounted(fetchNotifications);
</script>
<style scoped>
.section-header{display:flex;align-items:center;justify-content:space-between;margin-bottom:16px}.section-subtitle{margin:4px 0;color:var(--color-muted)}.notification-list{min-height:160px;margin:16px 0}.notification-card{margin-bottom:10px;cursor:pointer;border-left:4px solid transparent}.notification-card.unread{border-left-color:#f56c6c;background:#fffafa}.notification-body{display:flex;align-items:center;gap:14px}.notification-body p{flex:1;margin:0}.notification-body span{color:#909399;font-size:12px}.el-pagination{justify-content:center}
</style>

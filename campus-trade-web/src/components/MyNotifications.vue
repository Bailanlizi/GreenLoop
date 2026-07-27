<template>
  <div>
    <div class="section-header">
      <div>
        <h3>通知中心</h3>
        <p class="section-subtitle">包含需求匹配与系统提醒</p>
      </div>
      <el-button size="small" type="primary" @click="handleMarkAll">全部已读</el-button>
    </div>

    <el-table v-loading="loading" :data="notifications" style="width: 100%">
      <el-table-column prop="content" label="内容" min-width="280" />
      <el-table-column prop="type" label="类型" width="140" />
      <el-table-column prop="createTime" label="时间" width="160">
        <template #default="{ row }">{{ formatDate(row.createTime) }}</template>
      </el-table-column>
      <el-table-column prop="isRead" label="状态" width="100">
        <template #default="{ row }">
          <el-tag size="small" :type="row.isRead ? 'info' : 'success'">
            {{ row.isRead ? '已读' : '未读' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="120">
        <template #default="{ row }">
          <el-button v-if="row.relatedId" type="primary" link @click="openRelated(row.relatedId)">查看</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-empty v-if="!loading && notifications.length === 0" description="暂无通知" />
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { getNotifications, markAllAsRead } from '../api/notification';

const router = useRouter();
const notifications = ref([]);
const loading = ref(false);

const fetchNotifications = async () => {
  loading.value = true;
  try {
    const response = await getNotifications();
    notifications.value = response.data.data || [];
  } catch (error) {
    ElMessage.error('加载通知失败');
  } finally {
    loading.value = false;
  }
};

const handleMarkAll = async () => {
  try {
    await markAllAsRead();
    ElMessage.success('已全部标记为已读');
    fetchNotifications();
  } catch (error) {
    ElMessage.error('操作失败');
  }
};

const openRelated = (id) => {
  router.push(`/product/${id}`);
};

const formatDate = (value) => {
  if (!value) return '';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return '';
  return date.toLocaleDateString();
};

onMounted(fetchNotifications);
</script>

<style>
.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 16px;
}
.section-subtitle {
  margin: 4px 0 0;
  color: var(--color-muted);
  font-size: 0.9rem;
}
</style>

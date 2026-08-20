<template>
  <el-container class="main-layout">
    <el-header class="header">
      <div class="logo-area" @click="$router.push('/')">
        <el-icon :size="30" color="#67C23A"><SwitchFilled /></el-icon>
        <span class="logo-title">GreenLoop</span>
      </div>
      <div class="menu-area">
        <el-menu mode="horizontal" :router="true" :default-active="$route.path" background-color="transparent" :ellipsis="false">
          <el-menu-item index="/">首页</el-menu-item>
          <el-menu-item index="/publish">发布商品</el-menu-item>
        </el-menu>
      </div>
      <div class="user-area">
        <div v-if="authStore.isAuthenticated">
          <el-dropdown>
            <div class="avatar-badge">
              <el-avatar :icon="UserFilled" :src="authStore.user?.avatar || ''" />
              <span v-if="messageUnreadCount > 0 || notificationUnreadCount > 0" class="unread-dot"></span>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item disabled>{{ authStore.user?.nickname || '用户' }}</el-dropdown-item>
                <el-dropdown-item divided @click="$router.push('/dashboard')">个人中心</el-dropdown-item>
                <el-dropdown-item @click="$router.push('/dashboard/favorites')">我的收藏</el-dropdown-item>
                <el-dropdown-item @click="$router.push('/messages')">
                  <span class="dropdown-message-item">
                    我的消息
                    <span v-if="messageUnreadCount > 0" class="menu-unread-dot"></span>
                  </span>
                </el-dropdown-item>
                <el-dropdown-item @click="$router.push('/dashboard/notifications')">
                  <span class="dropdown-message-item">通知中心 <span v-if="notificationUnreadCount > 0" class="menu-unread-dot"></span></span>
                </el-dropdown-item>
                <el-dropdown-item @click="handleLogout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
        <div v-else>
          <el-button @click="$router.push('/login')">登录/注册</el-button>
        </div>
        <el-button class="btn-info" @click="toggleTheme">
          <el-icon style="margin-right:4px;">
            <Sunny v-if="theme==='light'" />
            <Moon v-else />
          </el-icon>
          {{ theme==='dark' ? '白天模式' : '夜间模式' }}
        </el-button>
      </div>
    </el-header>
    <el-main class="main-content">
      <router-view></router-view>
    </el-main>
  </el-container>
</template>

<script setup>
import { useAuthStore } from './stores/authStore';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { UserFilled, SwitchFilled, Moon, Sunny } from '@element-plus/icons-vue';
import { onMounted, onUnmounted, ref, watch } from 'vue';
import { getMessageUnreadCount } from './api/message';
import { getUnreadCount as getNotificationUnreadCount } from './api/notification';

const authStore = useAuthStore();
const router = useRouter();

const theme = ref('light');
const messageUnreadCount = ref(0);
const notificationUnreadCount = ref(0);
let unreadPollingTimer = null;

const setTheme = (val) => {
  theme.value = val;
  document.documentElement.setAttribute('data-theme', val);
  localStorage.setItem('theme', val);
};

const toggleTheme = () => {
  setTheme(theme.value === 'dark' ? 'light' : 'dark');
};

const fetchMessageUnreadCount = async () => {
  if (!authStore.isAuthenticated) {
    messageUnreadCount.value = 0;
    return;
  }
  try {
    const response = await getMessageUnreadCount();
    messageUnreadCount.value = response.data.data?.count || 0;
  } catch (error) {
    console.error('加载未读消息数失败:', error);
  }
};
const fetchNotificationUnreadCount = async () => {
  if (!authStore.isAuthenticated) { notificationUnreadCount.value = 0; return; }
  try { const response = await getNotificationUnreadCount(); notificationUnreadCount.value = response.data.data?.count || 0; }
  catch (error) { console.error('加载未读通知数失败:', error); }
};

const startUnreadPolling = () => {
  if (unreadPollingTimer) clearInterval(unreadPollingTimer);
  fetchMessageUnreadCount();
  fetchNotificationUnreadCount();
  unreadPollingTimer = setInterval(() => { fetchMessageUnreadCount(); fetchNotificationUnreadCount(); }, 5000);
};

const stopUnreadPolling = () => {
  if (unreadPollingTimer) {
    clearInterval(unreadPollingTimer);
    unreadPollingTimer = null;
  }
  messageUnreadCount.value = 0;
  notificationUnreadCount.value = 0;
};

onMounted(() => {
  if (authStore.isAuthenticated) {
    authStore.fetchFavoriteIds();
    startUnreadPolling();
  }
  const saved = localStorage.getItem('theme');
  setTheme(saved === 'dark' ? 'dark' : 'light');
  document.documentElement.setAttribute('data-theme', theme.value);
  window.addEventListener('message-read-state-changed', fetchMessageUnreadCount);
  window.addEventListener('notification-read-state-changed', fetchNotificationUnreadCount);
});

onUnmounted(() => {
  stopUnreadPolling();
  window.removeEventListener('message-read-state-changed', fetchMessageUnreadCount);
  window.removeEventListener('notification-read-state-changed', fetchNotificationUnreadCount);
});

watch(() => authStore.isAuthenticated, (isAuthenticated) => {
  if (isAuthenticated) {
    startUnreadPolling();
  } else {
    stopUnreadPolling();
  }
});

const handleLogout = () => {
  authStore.logout();
  stopUnreadPolling();
  ElMessage.success('已成功退出登录');
  router.push('/login');
};
</script>

<style>
body {
  margin: 0;
  font-family: 'Helvetica Neue', Helvetica, 'PingFang SC', 'Hiragino Sans GB', 'Microsoft YaHei', '微软雅黑', Arial, sans-serif;
}
.main-layout {
  min-height: 100vh;
}
.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid var(--color-border);
  background-color: var(--color-bg-card);
  color: var(--color-text);
  padding: 0 20px;
  position: sticky;
  top: 0;
  z-index: 100;
  box-shadow: var(--shadow-soft);
  backdrop-filter: blur(10px);
}
.logo-area {
  display: flex;
  align-items: center;
  cursor: pointer;
}
.logo-title {
  font-size: 22px;
  font-weight: bold;
  margin-left: 10px;
}
.menu-area {
  flex-grow: 1;
  display: flex;
  justify-content: center;
}
.el-menu--horizontal {
  border-bottom: none;
}
.el-menu-item {
  font-size: 16px;
}
.user-area {
  display: flex;
  align-items: center;
  gap: 16px;
}
.user-area .el-dropdown {
  cursor: pointer;
}
.avatar-badge {
  position: relative;
  display: inline-flex;
}
.unread-dot,
.menu-unread-dot {
  display: inline-block;
  width: 9px;
  height: 9px;
  border-radius: 50%;
  background: #f56c6c;
}
.unread-dot {
  position: absolute;
  top: 1px;
  right: 1px;
  border: 2px solid var(--color-bg-card);
}
.dropdown-message-item {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}
.main-content {
  padding: 20px;
  max-width: var(--layout-max-width);
  margin: 0 auto;
  width: 100%;
  background: var(--color-bg);
  color: var(--color-text);
}

@media (max-width: 900px) {
  .header {
    flex-wrap: wrap;
    gap: 12px;
    padding: 10px 12px;
  }
  .menu-area {
    order: 3;
    width: 100%;
    justify-content: flex-start;
  }
  .user-area {
    order: 2;
    width: 100%;
    justify-content: space-between;
  }
  .el-menu--horizontal {
    width: 100%;
    overflow-x: auto;
  }
  .el-menu-item {
    padding: 0 12px;
  }
}

@media (max-width: 600px) {
  .logo-title {
    font-size: 18px;
  }
  .user-area {
    gap: 8px;
    flex-wrap: wrap;
  }
  .main-content {
    padding: 12px;
  }
}
</style>

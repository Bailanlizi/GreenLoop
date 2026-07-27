<template>
  <div class="messages-layout">
    <el-card>
      <el-container style="height: calc(100vh - 120px);">
        <el-aside width="300px" class="conversation-list">
          <div class="list-header">会话列表</div>
          <el-scrollbar>
            <div v-if="loading" class="loading-placeholder">加载中...</div>
            <div
              v-for="conv in conversations"
              :key="conv.otherUserId"
              :class="['conversation-item', { 'is-active': $route.params.otherUserId === conv.otherUserId }]"
              @click="selectConversation(conv.otherUserId)"
            >
              <div class="conv-avatar-wrap">
                <el-avatar :size="50" :src="conv.otherUserAvatar" icon="UserFilled" />
                <span v-if="conv.unreadCount > 0" class="conversation-unread-dot"></span>
              </div>
              <div class="conv-info">
                <div class="conv-nickname-row">
                  <span class="conv-nickname">{{ conv.otherUserNickname }}</span>
                  <span v-if="conv.unreadCount > 0" class="conv-unread-count">{{ conv.unreadCount > 99 ? '99+' : conv.unreadCount }}</span>
                </div>
                <div class="conv-last-msg">{{ conv.lastMessageContent }}</div>
              </div>
            </div>
            <el-empty v-if="!loading && conversations.length === 0" description="暂无会话" :image-size="80"></el-empty>
          </el-scrollbar>
        </el-aside>
        <el-main class="chat-window-wrapper">
          <router-view
            :key="$route.params.otherUserId"
            @new-message="handleConversationChanged"
            @read-state-changed="handleConversationChanged"
          ></router-view>
        </el-main>
      </el-container>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { getConversations } from '../../api/message';

const router = useRouter();
const conversations = ref([]);
const loading = ref(false);

const fetchConversations = async () => {
  loading.value = true;
  try {
    const response = await getConversations();
    conversations.value = response.data.data || [];
  } catch (error) {
    console.error('加载会话列表失败:', error);
  } finally {
    loading.value = false;
  }
};

const selectConversation = (userId) => {
  router.push(`/messages/${userId}`);
};

const handleConversationChanged = () => {
  fetchConversations();
  window.dispatchEvent(new Event('message-read-state-changed'));
};

onMounted(fetchConversations);
</script>

<style scoped>
.messages-layout .el-card, .messages-layout .el-card >>> .el-card__body {
  padding: 0;
  height: calc(100vh - 100px);
}
.conversation-list { border-right: 1px solid #e6e6e6; display: flex; flex-direction: column; }
.list-header { padding: 20px; font-size: 18px; font-weight: bold; border-bottom: 1px solid #e6e6e6; flex-shrink: 0; }
.conversation-item {
  display: flex;
  align-items: center;
  padding: 15px 20px;
  cursor: pointer;
  border-bottom: 1px solid #f0f0f0;
}
.conversation-item:hover { background-color: #f5f7fa; }
.conversation-item.is-active { background-color: #ecf5ff; }
.conv-avatar-wrap {
  position: relative;
  display: inline-flex;
  flex-shrink: 0;
}
.conversation-unread-dot {
  position: absolute;
  top: 2px;
  right: 2px;
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: #f56c6c;
  border: 2px solid #fff;
}
.conv-info { margin-left: 15px; overflow: hidden; flex: 1; min-width: 0; }
.conv-nickname-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}
.conv-nickname { font-weight: 500; color: #303133; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.conv-unread-count {
  min-width: 18px;
  height: 18px;
  padding: 0 5px;
  border-radius: 9px;
  background: #f56c6c;
  color: #fff;
  font-size: 12px;
  line-height: 18px;
  text-align: center;
  flex-shrink: 0;
}
.conv-last-msg {
  font-size: 13px;
  color: #909399;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  margin-top: 5px;
}
.loading-placeholder { text-align: center; padding: 20px; color: #909399; }
.chat-window-wrapper { padding: 0; display: flex; flex-direction: column; }
</style>

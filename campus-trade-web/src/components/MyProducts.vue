<template>
  <div>
    <div class="section-header">
      <div>
        <h3>我发布的商品</h3>
        <p class="section-subtitle">在这里统一管理上架与编辑</p>
      </div>
      <el-button type="primary" @click="goPublish">发布商品</el-button>
    </div>

    <el-row :gutter="20" v-loading="loading">
      <el-col :xs="24" :sm="12" :md="8" :lg="6" v-for="product in products" :key="product.id" style="margin-bottom: 20px;">
        <el-card shadow="hover" class="product-card my-product-card" @click="goToDetail(product.id)">
          <div class="product-image-area">
            <img :src="product.coverImage" class="product-image" alt="商品图片" @error="onImageError" />
          </div>
          <div class="product-info-area">
            <div class="product-title">{{ product.title }}</div>
            <div class="product-submeta">
              <span class="product-price">¥{{ product.price }}</span>
              <el-tag size="small" :type="statusTagType(product.status)">{{ statusLabel(product.status) }}</el-tag>
            </div>
            <div class="product-meta-row">
              <span class="product-time">{{ formatDate(product.createTime) }}</span>
            </div>
          </div>
          <div class="product-actions">
            <el-button size="small" type="primary" @click.stop="editProduct(product.id)">编辑</el-button>
            <el-button size="small" type="warning" @click.stop="toggleStatus(product)">
              {{ product.status === 'AVAILABLE' ? '下架' : '上架' }}
            </el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-empty v-if="!loading && products.length === 0" description="暂无发布商品">
      <template #image>
        <el-icon style="font-size:48px;color:var(--color-primary)"><Goods /></el-icon>
      </template>
    </el-empty>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import { Goods } from '@element-plus/icons-vue';
import { getMyProducts, updateProductStatus } from '../api/product';

const router = useRouter();
const products = ref([]);
const loading = ref(false);

const fetchProducts = async () => {
  loading.value = true;
  try {
    const response = await getMyProducts();
    products.value = response.data.data || [];
  } catch (error) {
    ElMessage.error('加载我的商品失败');
  } finally {
    loading.value = false;
  }
};

const goPublish = () => router.push('/publish');
const goToDetail = (id) => router.push(`/product/${id}`);
const editProduct = (id) => router.push(`/edit-product/${id}`);

const toggleStatus = async (product) => {
  const nextStatus = product.status === 'AVAILABLE' ? 'DELISTED' : 'AVAILABLE';
  try {
    await ElMessageBox.confirm(`确定要${nextStatus === 'AVAILABLE' ? '上架' : '下架'}该商品吗？`, '确认操作', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    });
    await updateProductStatus(product.id, nextStatus);
    ElMessage.success('操作成功');
    fetchProducts();
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      ElMessage.error('操作失败');
    }
  }
};

const statusLabel = (status) => {
  if (status === 'AVAILABLE') return '在售';
  if (status === 'DELISTED') return '已下架';
  if (status === 'SOLD') return '已售出';
  return status || '未知';
};

const statusTagType = (status) => {
  if (status === 'AVAILABLE') return 'success';
  if (status === 'DELISTED') return 'info';
  if (status === 'SOLD') return 'warning';
  return 'info';
};

const formatDate = (value) => {
  if (!value) return '';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return '';
  return date.toLocaleDateString();
};

const onImageError = (event) => {
  event.target.src = 'https://placehold.co/400x300/e8e8e8/969696?text=Image+Not+Found';
};

onMounted(fetchProducts);
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
.my-product-card {
  height: auto;
}
.my-product-card .product-image-area {
  height: 150px;
}
.product-actions {
  display: flex;
  gap: 8px;
  padding: 10px 12px 12px;
  border-top: 1px solid var(--color-border);
  background: var(--color-bg-card);
}
.product-meta-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 0.82rem;
  color: var(--color-muted);
}
@media (max-width: 600px) {
  .section-header {
    flex-direction: column;
    align-items: flex-start;
  }
  .my-product-card .product-image-area {
    height: 140px;
  }
}
</style>

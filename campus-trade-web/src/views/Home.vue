<template>
  <el-container>
    <el-main>
      <h2 class="page-title">发现好物</h2>
      <el-card class="filter-card" shadow="never">
        <el-row :gutter="20" align="middle">
          <el-col :xs="24" :sm="12" :md="8">
            <el-input
              v-model="filters.keyword"
              placeholder="搜索你感兴趣的宝贝..."
              clearable
              @clear="handleFilterChange"
              @keyup.enter="handleFilterChange"
            >
              <template #append>
                <el-button :icon="Search" @click="handleFilterChange" />
              </template>
            </el-input>
          </el-col>

          <el-col :xs="12" :sm="6" :md="4">
            <el-select
              v-model="filters.categoryId"
              placeholder="全部分类"
              style="width: 100%;"
              @change="handleFilterChange"
              clearable
            >
              <el-option
                v-for="category in categories"
                :key="category.id"
                :label="category.name"
                :value="category.id"
              />
            </el-select>
          </el-col>

          <el-col :xs="24" :sm="12" :md="6">
            <div class="price-range">
              <el-input-number
                v-model="filters.minPrice"
                :min="0"
                :precision="2"
                controls-position="right"
                placeholder="最低价"
                style="flex: 1;"
                @change="handleFilterChange"
              />
              <span class="price-separator">-</span>
              <el-input-number
                v-model="filters.maxPrice"
                :min="filters.minPrice || 0"
                :precision="2"
                controls-position="right"
                placeholder="最高价"
                style="flex: 1;"
                @change="handleFilterChange"
              />
            </div>
          </el-col>

          <el-col :xs="12" :sm="6" :md="3">
            <el-select v-model="filters.orderBy" style="width: 100%;" @change="handleFilterChange">
              <el-option label="最新发布" value="latest" />
              <el-option label="价格从低到高" value="price_asc" />
              <el-option label="价格从高到低" value="price_desc" />
            </el-select>
          </el-col>

          <el-col :xs="12" :sm="6" :md="3">
            <el-select v-model="filters.searchMode" style="width: 100%;" @change="handleFilterChange">
              <el-option label="标准检索" value="standard" />
              <el-option label="智能检索" value="semantic" />
            </el-select>
          </el-col>
        </el-row>
      </el-card>

      <el-row :gutter="20" v-loading="loading" style="margin-top: 20px;">
        <el-col
          :xs="24"
          :sm="12"
          :md="8"
          :lg="6"
          v-for="product in products"
          :key="product.id"
          style="margin-bottom: 20px;"
        >
          <el-card shadow="hover" class="product-card home-product-card" @click="goToDetail(product.id)">
            <div class="product-image-area">
              <img :src="product.coverImage" class="product-image" alt="商品图片" @error="onImageError" />
              <div v-if="product.imageUrls && product.imageUrls.length > 0" class="image-count-overlay">
                <el-icon><CameraFilled /></el-icon>
                <span>1 / {{ 1 + product.imageUrls.length }}</span>
              </div>
            </div>
            <div class="product-info-area">
              <div class="product-title">{{ product.title }}</div>
              <div class="product-submeta">
                <span class="product-price">¥{{ product.price }}</span>
                <span class="product-time">{{ formatDate(product.createTime) }}</span>
              </div>
              <div class="product-badges">
                <span v-if="product.conditionLevel" class="badge badge-condition">
                  {{ formatCondition(product.conditionLevel) }}
                </span>
                <span v-for="opt in formatDeliveryOptions(product.deliveryOptions)" :key="opt" class="badge">
                  {{ opt }}
                </span>
              </div>
              <div class="product-seller-row">
                <el-avatar :size="24" :src="product.sellerAvatar" :icon="UserFilled" />
                <span class="product-seller">{{ product.sellerNickname }}</span>
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>

      <el-empty v-if="!loading && products.length === 0" description="暂无符合条件的商品">
        <template #image>
          <el-icon style="font-size:48px;color:var(--color-primary)"><Goods /></el-icon>
        </template>
      </el-empty>
    </el-main>
  </el-container>
</template>

<script setup>
import { ref, onMounted, reactive } from 'vue';
import { useRouter } from 'vue-router';
import { Search, CameraFilled, Goods, UserFilled } from '@element-plus/icons-vue';
import { ElMessage } from 'element-plus';
import { getProducts } from '../api/product';
import apiClient from '../api/axios.config';
import { debounce } from '../utils/debounce';

const router = useRouter();
const products = ref([]);
const categories = ref([]);
const loading = ref(false);

const filters = reactive({
  keyword: '',
  categoryId: '',
  minPrice: undefined,
  maxPrice: undefined,
  orderBy: 'latest',
  searchMode: 'standard',
});

const validateFilters = () => {
  if (filters.minPrice !== undefined && filters.maxPrice !== undefined && filters.minPrice > filters.maxPrice) {
    ElMessage.warning('最低价不能高于最高价');
    return false;
  }
  return true;
};

const fetchProducts = async () => {
  if (!validateFilters()) return;
  loading.value = true;
  try {
    const params = {};
    for (const key in filters) {
      if (filters[key] !== '' && filters[key] !== undefined && filters[key] !== null) {
        params[key] = filters[key];
      }
    }
    const response = await getProducts(params);
    if (!response || !response.data || !response.data.data) {
      products.value = [];
      return;
    }
    const data = response.data.data;
    if (!Array.isArray(data)) {
      products.value = [];
      return;
    }
    const productMap = new Map();
    data.forEach((item) => {
      if (!productMap.has(item.id)) {
        productMap.set(item.id, { ...item, imageUrls: [] });
      }
      if (item.imageUrls && item.imageUrls[0]) {
        const existingProduct = productMap.get(item.id);
        if (item.imageUrls[0] !== existingProduct.coverImage) {
          existingProduct.imageUrls.push(item.imageUrls[0]);
        }
      }
    });
    products.value = Array.from(productMap.values());
  } catch (error) {
    console.error('获取商品列表失败:', error);
    ElMessage.error('搜索失败，请稍后重试');
  } finally {
    loading.value = false;
  }
};

const fetchCategories = async () => {
  try {
    const response = await apiClient.get('/categories');
    categories.value = response.data.data;
  } catch (error) {
    console.error('获取分类列表失败:', error);
  }
};

const goToDetail = (id) => { router.push(`/product/${id}`); };
const onImageError = (e) => { e.target.src = 'https://placehold.co/400x300/e8e8e8/969696?text=Image+Not+Found'; };

const conditionLabels = ['成色一般', '明显使用', '轻微使用', '几乎全新', '全新'];
const formatCondition = (level) => {
  const index = Number(level) - 1;
  return conditionLabels[index] || '成色';
};
const deliveryOptionMap = {
  MEETUP: '面对面交',
  SHIPPING: '可邮寄',
};
const formatDeliveryOptions = (deliveryOptions) => {
  if (!deliveryOptions) return [];
  return String(deliveryOptions)
    .split(',')
    .map((item) => item.trim())
    .filter(Boolean)
    .map((item) => deliveryOptionMap[item] || item);
};
const formatDate = (value) => {
  if (!value) return '';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return '';
  return date.toLocaleDateString();
};

const handleFilterChange = debounce(() => {
  fetchProducts();
}, 500);

onMounted(() => {
  fetchProducts();
  fetchCategories();
});
</script>

<style>
.filter-card {
  margin-bottom: 20px;
  border-radius: var(--radius-card);
  box-shadow: var(--shadow-soft);
  background: var(--color-bg-card);
}
.price-range {
  display: flex;
  align-items: center;
}
.price-separator {
  margin: 0 10px;
  color: #909399;
}
.product-card {
  display: flex;
  flex-direction: column;
  border-radius: var(--radius-card);
  overflow: hidden;
  box-shadow: var(--shadow-soft);
  background: var(--color-bg-card);
  border: 1px solid var(--color-border);
  transition: box-shadow 0.2s, transform 0.2s, border-color 0.2s;
  cursor: pointer;
}
.product-card:hover {
  box-shadow: var(--shadow-strong);
  border-color: rgba(0, 122, 255, 0.25);
  transform: translateY(-2px);
}
.home-product-card {
  height: 320px;
}
.home-product-card > .el-card__body {
  display: flex;
  flex-direction: column;
  height: 100%;
  padding: 0;
}
.home-product-card .product-image-area {
  flex: 0 0 160px;
  height: 160px;
  aspect-ratio: 4 / 3;
}
.product-image-area {
  width: 100%;
  background: #f5f6fa;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  border-radius: 0;
  overflow: hidden;
}
.product-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
  border-radius: 0;
  display: block;
}
.image-count-overlay {
  position: absolute;
  bottom: 10px;
  right: 10px;
  background: rgba(0, 0, 0, 0.55);
  color: #fff;
  border-radius: 10px;
  padding: 2px 8px;
  font-size: 12px;
  display: flex;
  align-items: center;
}
.product-info-area {
  padding: 10px 12px 12px;
  background: var(--color-bg-card);
  border-top: 1px solid var(--color-border);
  display: flex;
  flex-direction: column;
  gap: 6px;
  flex: 1;
  overflow: hidden;
}
.product-title {
  font-size: 0.96rem;
  font-weight: 600;
  color: var(--color-text);
  line-height: 1.35;
  margin-bottom: 2px;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  min-height: 2.6em;
}
.product-submeta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}
.product-price {
  font-size: 1rem;
  font-weight: 700;
  color: var(--color-danger);
}
.product-time {
  font-size: 0.82rem;
  color: var(--color-muted);
}
.product-badges {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  min-height: 24px;
  max-height: 24px;
  overflow: hidden;
}
.badge {
  font-size: 0.78rem;
  color: var(--color-muted);
  background: rgba(15, 23, 42, 0.05);
  border-radius: var(--radius-pill);
  padding: 2px 8px;
}
.badge-condition {
  color: var(--color-primary);
  background: rgba(0, 122, 255, 0.1);
}
.product-seller-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: auto;
}
.product-seller {
  font-size: 0.9rem;
  color: var(--color-text);
  max-width: 140px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
@media (max-width: 600px) {
  .filter-card .el-row {
    row-gap: 10px;
  }
  .home-product-card {
    height: 300px;
  }
  .home-product-card .product-image-area {
    height: 150px;
  }
  .product-info-area {
    padding: 10px 12px;
  }
  .product-title {
    font-size: 0.94rem;
  }
  .product-price {
    font-size: 0.96rem;
  }
}
</style>

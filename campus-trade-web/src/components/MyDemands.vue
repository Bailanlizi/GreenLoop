<template>
  <div>
    <div class="section-header">
      <div>
        <h3>我的需求</h3>
        <p class="section-subtitle">发布需求后，系统会在匹配到新商品时通知你</p>
      </div>
    </div>

    <el-card class="demand-card" shadow="never">
      <el-form :model="form" label-width="90px">
        <el-row :gutter="16">
          <el-col :xs="24" :sm="12" :md="8">
            <el-form-item label="关键词">
              <el-input v-model="form.keyword" placeholder="如 iPad / 教材 / 耳机" />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12" :md="8">
            <el-form-item label="分类">
              <el-select v-model="form.categoryId" placeholder="不限分类" clearable :loading="categoriesLoading">
                <el-option v-for="category in categories" :key="category.id" :label="category.name" :value="category.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12" :md="8">
            <el-form-item label="成色">
              <el-rate v-model="form.conditionLevel" :texts="conditionTexts" show-text />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12" :md="8">
            <el-form-item label="最低价">
              <el-input-number v-model="form.minPrice" :min="0" :precision="2" />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12" :md="8">
            <el-form-item label="最高价">
              <el-input-number v-model="form.maxPrice" :min="0" :precision="2" />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12" :md="8">
            <el-form-item label="配送">
              <el-checkbox-group v-model="form.deliveryOptions">
                <el-checkbox label="MEETUP">线下面交</el-checkbox>
                <el-checkbox label="SHIPPING">支持邮寄</el-checkbox>
              </el-checkbox-group>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item>
          <el-button type="primary" :loading="submitting" @click="handleSubmit">发布需求</el-button>
          <el-button type="default" @click="resetForm">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <div class="list-header">
      <h4>已发布需求</h4>
      <el-button type="default" size="small" @click="fetchDemands">刷新</el-button>
    </div>

    <el-row :gutter="16" v-loading="loading">
      <el-col :xs="24" :sm="12" :md="8" v-for="demand in demands" :key="demand.id">
        <el-card class="demand-item" shadow="hover">
          <div class="demand-title">
            <span>{{ demand.keyword || '不限关键词' }}</span>
            <el-tag size="small" type="success">生效中</el-tag>
          </div>
          <div class="demand-meta">
            <div>分类：{{ formatCategory(demand.categoryId) }}</div>
            <div>价格：{{ formatPriceRange(demand.minPrice, demand.maxPrice) }}</div>
            <div>成色：{{ formatCondition(demand.conditionLevel) }}</div>
            <div>配送：{{ formatDelivery(demand.deliveryOptions) }}</div>
          </div>
          <div class="demand-actions">
            <el-button type="danger" plain size="small" @click="handleDelete(demand.id)">删除</el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-empty v-if="!loading && demands.length === 0" description="暂无需求，快发布一个吧" />
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import apiClient from '../api/axios.config';
import { createDemand, deleteDemand, getMyDemands } from '../api/demand';

const categories = ref([]);
const categoriesLoading = ref(false);
const loading = ref(false);
const submitting = ref(false);
const demands = ref([]);
const conditionTexts = ['一般', '明显使用', '轻微使用', '几乎全新', '全新'];

const form = ref({
  keyword: '',
  categoryId: '',
  minPrice: undefined,
  maxPrice: undefined,
  conditionLevel: 3,
  deliveryOptions: []
});

const categoryMap = computed(() => {
  return new Map(categories.value.map((category) => [category.id, category.name]));
});

const fetchCategories = async () => {
  categoriesLoading.value = true;
  try {
    const response = await apiClient.get('/categories');
    categories.value = response.data.data || [];
  } catch (error) {
    ElMessage.error('加载分类失败');
  } finally {
    categoriesLoading.value = false;
  }
};

const fetchDemands = async () => {
  loading.value = true;
  try {
    const response = await getMyDemands();
    demands.value = response.data.data || [];
  } catch (error) {
    ElMessage.error('加载需求失败');
  } finally {
    loading.value = false;
  }
};

const handleSubmit = async () => {
  if (form.value.minPrice && form.value.maxPrice && form.value.minPrice > form.value.maxPrice) {
    ElMessage.warning('最低价不能高于最高价');
    return;
  }
  submitting.value = true;
  try {
    await createDemand({
      ...form.value,
      categoryId: form.value.categoryId || null
    });
    ElMessage.success('需求已发布');
    resetForm();
    fetchDemands();
  } catch (error) {
    ElMessage.error('发布需求失败');
  } finally {
    submitting.value = false;
  }
};

const handleDelete = async (id) => {
  try {
    await ElMessageBox.confirm('确定删除该需求吗？', '删除确认', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    });
    await deleteDemand(id);
    ElMessage.success('已删除');
    fetchDemands();
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      ElMessage.error('删除失败');
    }
  }
};

const resetForm = () => {
  form.value = {
    keyword: '',
    categoryId: '',
    minPrice: undefined,
    maxPrice: undefined,
    conditionLevel: 3,
    deliveryOptions: []
  };
};

const formatPriceRange = (minPrice, maxPrice) => {
  if (!minPrice && !maxPrice) return '不限';
  if (minPrice && maxPrice) return `¥${minPrice} - ¥${maxPrice}`;
  if (minPrice) return `≥ ¥${minPrice}`;
  return `≤ ¥${maxPrice}`;
};

const formatCategory = (categoryId) => {
  if (!categoryId) return '不限';
  return categoryMap.value.get(categoryId) || `分类${categoryId}`;
};

const formatCondition = (level) => {
  if (!level) return '不限';
  return conditionTexts[level - 1] || `等级${level}`;
};

const formatDelivery = (value) => {
  if (!value) return '不限';
  return String(value)
    .split(',')
    .filter(Boolean)
    .map((item) => (item === 'MEETUP' ? '线下面交' : item === 'SHIPPING' ? '支持邮寄' : item))
    .join(' / ');
};

onMounted(() => {
  fetchCategories();
  fetchDemands();
});
</script>

<style>
.demand-card {
  margin-bottom: 20px;
  border-radius: var(--radius-card);
}
.list-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: 12px 0;
}
.demand-item {
  margin-bottom: 16px;
  border-radius: var(--radius-card);
}
.demand-title {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: 600;
  margin-bottom: 8px;
}
.demand-meta {
  display: grid;
  gap: 6px;
  color: var(--color-muted);
  font-size: 0.9rem;
}
.demand-actions {
  margin-top: 12px;
}
@media (max-width: 600px) {
  .demand-card {
    padding: 4px;
  }
}
</style>

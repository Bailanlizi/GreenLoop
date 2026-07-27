<template>
  <el-card>
    <template #header>
      <h3>{{ isEdit ? '编辑商品信息' : '发布新的二手宝贝' }}</h3>
    </template>
    <el-form :model="form" :rules="rules" label-width="120px" ref="formRef" class="form-layout">
      <el-form-item label="商品图片" prop="fileList">
        <el-upload
          v-model:file-list="form.fileList"
          action="/api/files/upload"
          list-type="picture-card"
          :multiple="true"
          :limit="3"
          :on-success="handleUploadSuccess"
          :on-remove="handleRemove"
          :on-preview="handlePictureCardPreview"
          :before-upload="beforeUpload"
          :headers="{ 'Authorization': `Bearer ${authStore.token}` }"
          :class="{ 'hide-upload': form.fileList.length >= 3 }"
        >
          <el-icon><Plus /></el-icon>
        </el-upload>
        <p class="upload-tip">第一张作为封面图，最多上传 3 张图片</p>
      </el-form-item>

      <el-form-item label="标题" prop="title">
        <el-input v-model="form.title" placeholder="一个吸引人的标题更容易被看到"></el-input>
      </el-form-item>
      <el-form-item label="描述" prop="description">
        <el-input v-model="form.description" type="textarea" rows="4" placeholder="详细描述你的商品状态与配件"></el-input>
      </el-form-item>
      <el-form-item label="智能助手">
        <el-button type="primary" :loading="aiLoading" @click="handleAiSuggest">一键润色</el-button>
      </el-form-item>
      <el-form-item v-if="aiHints.highlights.length" label="卖点">
        <el-tag v-for="(item, index) in aiHints.highlights" :key="index" type="success" style="margin-right: 8px;">
          {{ item }}
        </el-tag>
      </el-form-item>
      <el-form-item v-if="aiHints.tags.length" label="标签">
        <el-tag v-for="(item, index) in aiHints.tags" :key="index" style="margin-right: 8px;">
          {{ item }}
        </el-tag>
      </el-form-item>
      <el-form-item label="价格" prop="price">
        <el-input-number v-model="form.price" :precision="2" :step="1" :min="0.01" />
      </el-form-item>
      <el-form-item label="智能定价">
        <el-button type="primary" :loading="aiPriceLoading" @click="handleAiPrice">获取建议</el-button>
        <el-button v-if="aiPriceResult && aiPriceResult.suggestedPrice" type="default" @click="applySuggestedPrice">
          采用建议价
        </el-button>
      </el-form-item>
      <el-form-item v-if="aiPriceResult" label="定价建议">
        <div class="ai-price-card">
          <div class="ai-price-main">
            <span>建议价：</span>
            <strong>¥{{ aiPriceResult.suggestedPrice ?? '-' }}</strong>
          </div>
          <div class="ai-price-range">
            区间：¥{{ aiPriceResult.suggestedMin ?? '-' }} - ¥{{ aiPriceResult.suggestedMax ?? '-' }}
          </div>
          <div v-if="aiPriceResult.summary" class="ai-price-summary">{{ aiPriceResult.summary }}</div>
          <div v-if="aiPriceResult.tips && aiPriceResult.tips.length" class="ai-price-tips">
            <el-tag v-for="(tip, index) in aiPriceResult.tips" :key="index" style="margin-right: 6px;">
              {{ tip }}
            </el-tag>
          </div>
        </div>
      </el-form-item>
      <el-form-item label="分类" prop="categoryId">
        <el-select v-model="form.categoryId" placeholder="选择分类" :loading="categoriesLoading">
          <el-option
            v-for="category in categories"
            :key="category.id"
            :label="category.name"
            :value="category.id"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="成色" prop="conditionLevel">
        <el-rate v-model="form.conditionLevel" :texts="conditionTexts" show-text />
      </el-form-item>
      <el-form-item label="配送方式" prop="deliveryOptions">
        <el-checkbox-group v-model="form.deliveryOptions">
          <el-checkbox label="MEETUP">线下面交</el-checkbox>
          <el-checkbox label="SHIPPING">支持邮寄</el-checkbox>
        </el-checkbox-group>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="handleSubmit" :loading="submitting">
          {{ isEdit ? '更新商品' : '确认发布' }}
        </el-button>
      </el-form-item>
    </el-form>

    <el-dialog v-model="dialogVisible">
      <img w-full :src="dialogImageUrl" alt="Preview Image" style="width: 100%" />
    </el-dialog>
  </el-card>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue';
import { useRouter } from 'vue-router';
import { useAuthStore } from '../stores/authStore';
import { ElMessage, ElMessageBox } from 'element-plus';
import { createProduct, getProductById, updateProduct } from '../api/product';
import { suggestPublish, checkProductRisk, suggestPrice } from '../api/ai';
import apiClient from '../api/axios.config';
import { Plus } from '@element-plus/icons-vue';

const props = defineProps({ id: String });
const router = useRouter();
const authStore = useAuthStore();
const formRef = ref(null);
const submitting = ref(false);
const aiLoading = ref(false);
const aiPriceLoading = ref(false);
const aiHints = ref({ highlights: [], tags: [] });
const aiPriceResult = ref(null);

const form = reactive({
  title: '',
  description: '',
  price: 0.01,
  categoryId: '',
  conditionLevel: 3,
  fileList: [],
  deliveryOptions: ['MEETUP']
});

const conditionTexts = ['一般', '明显使用', '轻微使用', '几乎全新', '全新'];
const dialogImageUrl = ref('');
const dialogVisible = ref(false);

const rules = {
  title: [{ required: true, message: '请输入标题', trigger: 'blur' }],
  price: [{ required: true, message: '请输入价格', trigger: 'blur' }],
  categoryId: [{ required: true, message: '请选择分类', trigger: 'change' }],
  fileList: [{ type: 'array', required: true, message: '请至少上传一张图片', trigger: 'change' }],
  deliveryOptions: [{ type: 'array', required: true, message: '请选择配送方式', trigger: 'change' }]
};

const isEdit = computed(() => !!props.id);
const categories = ref([]);
const categoriesLoading = ref(false);

const handleUploadSuccess = (response, uploadFile, uploadFiles) => {
  uploadFile.url = response.data.url;
  form.fileList = [...uploadFiles];
  ElMessage.success('图片上传成功');
};

const handleRemove = (uploadFile, uploadFiles) => {
  form.fileList = uploadFiles;
};

const handlePictureCardPreview = (uploadFile) => {
  dialogImageUrl.value = uploadFile.url;
  dialogVisible.value = true;
};

const beforeUpload = (rawFile) => {
  const isAcceptedType = ['image/jpeg', 'image/png', 'image/gif', 'image/webp'].includes(rawFile.type);
  if (!isAcceptedType) {
    ElMessage.error('图片必须是 JPG, PNG, GIF 或 WEBP 格式');
    return false;
  }
  const isLt10M = rawFile.size / 1024 / 1024 < 10;
  if (!isLt10M) {
    ElMessage.error('图片大小不能超过 10MB');
    return false;
  }
  return true;
};

const handleSubmit = async () => {
  if (!formRef.value) return;
  await formRef.value.validate(async (valid) => {
    if (valid) {
      submitting.value = true;
      try {
        const imageUrls = form.fileList.map(file => file.url);
        const payload = {
          title: form.title,
          description: form.description,
          price: form.price,
          categoryId: form.categoryId,
          conditionLevel: form.conditionLevel,
          coverImage: imageUrls[0],
          imageUrls: imageUrls.slice(1),
          deliveryOptions: form.deliveryOptions
        };

        if (!isEdit.value) {
          await confirmRisk(payload);
        }

        if (isEdit.value) {
          await updateProduct(props.id, payload);
          ElMessage.success('更新成功');
          router.push(`/product/${props.id}`);
        } else {
          await createProduct(payload);
          ElMessage.success('发布成功');
          router.push('/');
        }
      } catch (error) {
        console.error(error);
      } finally {
        submitting.value = false;
      }
    }
  });
};

const handleAiSuggest = async () => {
  aiLoading.value = true;
  try {
    const payload = {
      title: form.title,
      description: form.description,
      categoryId: form.categoryId,
      price: form.price,
      conditionLevel: form.conditionLevel,
      deliveryOptions: form.deliveryOptions
    };
    const response = await suggestPublish(payload);
    const data = response.data.data || {};
    if (data.title) {
      form.title = data.title;
    }
    if (data.description) {
      form.description = data.description;
    }
    aiHints.value = {
      highlights: Array.isArray(data.highlights) ? data.highlights : [],
      tags: Array.isArray(data.tags) ? data.tags : []
    };
    ElMessage.success('智能润色完成');
  } catch (error) {
    ElMessage.error('智能润色失败');
  } finally {
    aiLoading.value = false;
  }
};

const handleAiPrice = async () => {
  aiPriceLoading.value = true;
  try {
    const payload = {
      title: form.title,
      description: form.description,
      categoryId: form.categoryId || null,
      conditionLevel: form.conditionLevel,
      deliveryOptions: form.deliveryOptions,
      currentPrice: form.price
    };
    const response = await suggestPrice(payload);
    aiPriceResult.value = response.data.data || null;
    ElMessage.success('定价建议已生成');
  } catch (error) {
    ElMessage.error('获取定价建议失败');
  } finally {
    aiPriceLoading.value = false;
  }
};

const applySuggestedPrice = () => {
  if (aiPriceResult.value && aiPriceResult.value.suggestedPrice) {
    form.price = aiPriceResult.value.suggestedPrice;
  }
};

const confirmRisk = async (payload) => {
  try {
    const response = await checkProductRisk(payload);
    const risk = response.data.data;
    if (risk && risk.riskLevel && risk.riskLevel !== 'LOW') {
      const reasons = Array.isArray(risk.reasons) ? risk.reasons.join('；') : '检测到风险';
      await ElMessageBox.confirm(`检测到风险：${reasons}，是否继续发布？`, '风控提示', {
        confirmButtonText: '继续发布',
        cancelButtonText: '取消',
        type: 'warning'
      });
    }
  } catch (error) {
    if (error === 'cancel' || error === 'close') {
      throw error;
    }
    ElMessage.warning('风控检测失败，已继续发布');
  }
};

const fetchProductData = async (id) => {
  try {
    const res = await getProductById(id);
    const productData = res.data.data;
    Object.assign(form, productData);
    form.title = productData.title;
    form.description = productData.description;
    form.price = productData.price;
    form.categoryId = productData.categoryId;
    form.conditionLevel = productData.conditionLevel;
    form.deliveryOptions = productData.deliveryOptions ? productData.deliveryOptions.split(',') : [];
    const images = [productData.coverImage, ...(productData.imageUrls || [])].filter(Boolean);
    form.fileList = images.map((url) => ({
      name: url.substring(url.lastIndexOf('/') + 1) || 'image.png',
      url: url
    }));
  } catch (error) {
    console.error(error);
    ElMessage.error('加载商品数据失败');
    router.push('/');
  }
};

const fetchCategories = async () => {
  categoriesLoading.value = true;
  try {
    const response = await apiClient.get('/categories');
    categories.value = response.data.data;
  } catch (error) {
    ElMessage.error('加载分类失败');
  } finally {
    categoriesLoading.value = false;
  }
};

onMounted(() => {
  fetchCategories();
  if (isEdit.value) {
    fetchProductData(props.id);
  }
});
</script>

<style>
.upload-tip {
  color: #909399;
  font-size: 12px;
  margin-top: 5px;
}
.hide-upload .el-upload--picture-card {
    display: none;
}
.ai-price-card {
  background: var(--color-bg-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-card);
  padding: 12px 16px;
  display: grid;
  gap: 8px;
}
.ai-price-main {
  font-size: 1rem;
  color: var(--color-text);
}
.ai-price-main strong {
  color: var(--color-danger);
  font-size: 1.05rem;
}
.ai-price-range {
  color: var(--color-muted);
  font-size: 0.9rem;
}
.ai-price-summary {
  color: var(--color-text);
  font-size: 0.9rem;
}
.ai-price-tips {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}
</style>

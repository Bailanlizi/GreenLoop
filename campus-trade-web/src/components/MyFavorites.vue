<template>
    <div>
        <h3>我的收藏</h3>
        <el-row :gutter="20" v-loading="loading">
            <el-col :xs="24" :sm="12" :md="8" :lg="6" v-for="product in favoriteProducts" :key="product.id" style="margin-bottom: 20px;">
                <el-card shadow="hover" class="product-card my-favorite-card" @click="goToDetail(product.id)">
                    <div class="product-image-area">
                        <img :src="product.coverImage" class="product-image" alt="商品图片" @error="onImageError"/>
                    </div>
                    <div class="product-info-area">
                        <div class="product-title">{{ product.title }}</div>
                        <div class="product-submeta">
                            <span class="product-price">¥{{ product.price }}</span>
                            <span class="product-time">{{ formatDate(product.createTime) }}</span>
                        </div>
                        <div class="product-seller-row">
                            <el-avatar :size="24" :src="product.sellerAvatar" :icon="UserFilled" />
                            <span class="product-seller">{{ product.sellerNickname }}</span>
                        </div>
                    </div>
                    <div class="unfavorite-btn">
                        <el-button type="danger" plain size="small" @click.stop="handleRemoveFavorite(product.id)">取消收藏</el-button>
                    </div>
                </el-card>
            </el-col>
        </el-row>
        <el-empty v-if="!loading && favoriteProducts.length === 0" description="您还没有收藏任何商品"></el-empty>
    </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { useAuthStore } from '../stores/authStore';
import { getMyFavorites } from '../api/favorite';
import { ElMessage } from 'element-plus';
import { UserFilled } from '@element-plus/icons-vue';

const router = useRouter();
const authStore = useAuthStore();
const favoriteProducts = ref([]);
const loading = ref(false);

const fetchFavorites = async () => {
    loading.value = true;
    try {
        const response = await getMyFavorites();
        favoriteProducts.value = response.data.data;
    } catch (error) {
        console.error("加载收藏列表失败:", error);
    } finally {
        loading.value = false;
    }
};

const goToDetail = (id) => {
    router.push(`/product/${id}`);
};

const handleRemoveFavorite = async (productId) => {
    await authStore.removeFromFavorites(productId);
    ElMessage.success("已取消收藏");
    fetchFavorites();
};

const onImageError = (e) => {
    e.target.src = 'https://placehold.co/400x300/e8e8e8/969696?text=Image+Not+Found';
};

const formatDate = (value) => {
    if (!value) return '';
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) return '';
    return date.toLocaleDateString();
};

onMounted(fetchFavorites);
</script>

<style>
:deep(.el-card__body) {
    padding: 0;
}
.my-favorite-card {
    height: 320px;
    position: relative;
}
.my-favorite-card .product-image-area {
    height: 160px;
}
.unfavorite-btn {
    position: absolute;
    top: 10px;
    right: 10px;
    opacity: 0;
    transition: opacity 0.3s;
}
.product-card:hover .unfavorite-btn {
    opacity: 1;
}
@media (max-width: 600px) {
    .my-favorite-card {
        height: 300px;
    }
    .my-favorite-card .product-image-area {
        height: 150px;
    }
}
</style>

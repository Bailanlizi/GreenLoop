<template>
  <el-row :gutter="20">
    <el-col :xs="24" :md="5">
      <el-card>
        <el-menu :default-active="$route.path" router>
          <el-menu-item-group title="交易管理">
            <el-menu-item index="/dashboard/purchases">
              <el-icon><Goods /></el-icon>
              <span>我买到的</span>
            </el-menu-item>
            <el-menu-item index="/dashboard/products">
              <el-icon><Goods /></el-icon>
              <span>我发布的</span>
            </el-menu-item>
            <el-menu-item index="/dashboard/sales">
              <el-icon><Sell /></el-icon>
              <span>我卖出的</span>
            </el-menu-item>
            <el-menu-item index="/dashboard/favorites">
              <el-icon><Star /></el-icon>
              <span>我的收藏</span>
            </el-menu-item>
          </el-menu-item-group>
          <el-menu-item-group title="需求与通知">
            <el-menu-item index="/dashboard/demands">
              <el-icon><Star /></el-icon>
              <span>我的需求</span>
            </el-menu-item>
            <el-menu-item index="/dashboard/notifications">
              <el-icon><Star /></el-icon>
              <span>通知中心</span>
            </el-menu-item>
          </el-menu-item-group>
          <el-menu-item-group title="账户管理">
            <el-menu-item index="/dashboard/profile">
              <el-icon><User /></el-icon>
              <span>个人信息</span>
            </el-menu-item>
            <el-menu-item index="/dashboard/addresses">
              <el-icon><Location /></el-icon>
              <span>收货地址</span>
            </el-menu-item>
          </el-menu-item-group>
        </el-menu>
      </el-card>
    </el-col>
    <el-col :xs="24" :md="19">
      <el-card>
        <router-view></router-view>
      </el-card>
    </el-col>
  </el-row>
</template>

<script setup>
import { Goods, Sell, User, Star, Location } from '@element-plus/icons-vue';
</script>

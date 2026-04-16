<template>
  <el-form
    inline
    @submit.prevent="emit('search')"
  >
    <slot />
    <el-form-item>
      <el-button
        type="primary"
        @click="emit('search')"
      >
        搜索
      </el-button>
      <el-button @click="onReset">
        重置
      </el-button>
    </el-form-item>
  </el-form>
</template>

<script setup lang="ts">
const emit = defineEmits<{ search: []; reset: [] }>();
function onReset() { emit('reset'); emit('search'); }
</script>

<template>
  <el-tag
    type="info"
    effect="plain"
    :size="size"
    class="mock-badge"
  >
    <slot>Mock 模式</slot>
  </el-tag>
</template>

<script setup lang="ts">
/**
 * Visual badge marking UI actions that depend on TLQ transport and therefore
 * currently no-op (or invoke the in-memory TLQ Mock) until the real TLQ SDK
 * is wired up. Placed next to preview/submit/resubmit buttons so reviewers
 * can tell at a glance that the action is not yet production-live.
 *
 * Defaults to the text "Mock 模式"; callers may override via the default slot
 * to provide richer explanations such as "待 TLQ 到位".
 */
defineProps<{ size?: 'small' | 'default' | 'large' }>();
</script>

<style scoped>
.mock-badge {
  margin-left: 8px;
}
</style>

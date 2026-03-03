<template>
  <div ref="container" class="w-full h-full bg-black relative">
    <!-- Show loader only if we haven't loaded yet -->
    <div v-if="loading" class="absolute inset-0 flex items-center justify-center bg-black z-10">
      <div class="w-6 h-6 border-2 border-zinc-800 border-t-zinc-400 rounded-full animate-spin"></div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount } from 'vue'
import { AikoThreeScene } from '~/utils/AikoThreeScene'

const container = ref<HTMLElement | null>(null)
// Initialize to false if we suspect frequent remounts, but here we want to know if it remounts
const loading = ref(true)
let aikoScene: AikoThreeScene | null = null

onMounted(() => {
  if (!container.value) return
  
  console.log('[AikoWindowScene] mounted' + (aikoScene ? ' (re-mounted)' : ''))
  
  aikoScene = new AikoThreeScene(
    container.value, 
    (isLoading) => {
        loading.value = isLoading
    }
  )

  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  if (aikoScene) {
    aikoScene.destroy()
  }
})

function handleResize() {
  if (aikoScene) {
    aikoScene.handleResize()
  }
}

defineExpose({
  handleResize
})
</script>

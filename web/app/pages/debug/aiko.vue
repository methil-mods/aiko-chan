<template>
  <div class="debug-page fixed inset-0 bg-black text-white font-vcr flex flex-col overflow-hidden lowercase">
    <header class="p-4 border-b border-zinc-800 flex justify-between items-center z-20 bg-black/80 backdrop-blur-md">
      <div class="flex items-center gap-6">
        <NuxtLink to="/" class="px-4 py-1.5 bg-zinc-800 hover:bg-zinc-700 rounded text-xs transition-colors tracking-tighter border border-zinc-700">
          ← back to monitor
        </NuxtLink>
        <div class="flex flex-col">
          <h1 class="text-lg font-bold text-pink-500 italic tracking-tight">aiko_system_debug_v1.0.4</h1>
          <span class="text-xs text-zinc-600 uppercase tracking-[0.2em] leading-none mt-1 font-bold">internal use only</span>
        </div>
      </div>

      <!-- Controls -->
      <div class="flex items-center gap-8">
        <div class="flex items-center gap-3">
          <span class="text-xs text-zinc-500 font-bold">grid_helper:</span>
          <button 
            @click="toggleGrid" 
            class="px-3 py-1 border text-xs transition-colors font-bold"
            :class="gridVisible ? 'border-pink-500 text-pink-500 bg-pink-500/10' : 'border-zinc-700 text-zinc-500'"
          >
            {{ gridVisible ? 'on' : 'off' }}
          </button>
        </div>
        <div class="text-xs text-zinc-500 flex gap-6 border-l border-zinc-800 pl-8 font-bold">
          <span>rotate: lmb</span>
          <span>pan: rmb</span>
          <span>zoom: scroll</span>
        </div>
      </div>
    </header>
    
    <main class="flex-1 relative bg-[#050508]">
      <div ref="container" class="absolute inset-0 w-full h-full"></div>
      
      <!-- Overlay UI -->
      <div v-if="loading" class="absolute inset-0 flex items-center justify-center bg-black z-50">
        <div class="flex flex-col items-center gap-4">
          <div class="w-10 h-10 border-2 border-pink-500/20 border-t-pink-500 rounded-full animate-spin"></div>
          <p class="text-xs text-pink-500 animate-pulse tracking-[0.3em] uppercase font-bold">initializing_scene</p>
        </div>
      </div>

      <!-- Debug HUD -->
      <div class="absolute top-8 left-8 p-6 bg-black/60 border border-white/5 backdrop-blur-md rounded-sm pointer-events-none space-y-4 min-w-[240px]">
        <div class="text-zinc-500 text-xs font-bold uppercase tracking-widest border-b border-white/5 pb-3 mb-3 italic">diagnostic_feed</div>
        
        <div class="space-y-2 text-sm">
          <div class="flex justify-between">
            <span class="text-zinc-600 font-bold">renderer_status</span>
            <span :class="loading ? 'text-yellow-600' : 'text-green-500'" class="font-bold">{{ loading ? 'pending' : 'active' }}</span>
          </div>
          <div class="flex justify-between">
            <span class="text-zinc-600 font-bold">pixel_ratio</span>
            <span class="text-zinc-400 font-bold">{{ pixelRatio }}x</span>
          </div>
          <div class="flex justify-between">
            <span class="text-zinc-600 font-bold">pip_view</span>
            <span class="text-blue-400 font-bold">final_render</span>
          </div>
        </div>

        <div class="pt-2">
          <div class="h-1.5 w-full bg-zinc-900 rounded-full overflow-hidden">
            <div class="h-full bg-pink-500/40 w-2/3 animate-[pulse_2s_infinite]"></div>
          </div>
        </div>
      </div>

      <!-- PiP Label -->
      <div class="absolute bottom-8 right-8 pointer-events-none lowercase font-mono">
        <div class="text-xs text-zinc-500 mb-3 text-right font-bold tracking-widest">final_preview_no_helpers</div>
        <div class="w-[25vw] aspect-video border border-white/10 shadow-2xl"></div>
      </div>
    </main>
    
    <footer class="px-6 py-3 border-t border-zinc-900 bg-black flex justify-between items-center text-xs text-zinc-700 font-mono tracking-[0.3em] uppercase font-bold">
      <span>aiko_core_engine</span>
      <span>2026_build_stable</span>
    </footer>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { AikoThreeScene } from '~/utils/AikoThreeScene'

const container = ref<HTMLElement | null>(null)
const loading = ref(true)
const pixelRatio = ref(1)
const gridVisible = ref(true)
let aikoScene: AikoThreeScene | null = null

onMounted(async () => {
  if (!container.value) return
  
  await nextTick()
  pixelRatio.value = window.devicePixelRatio
  
  aikoScene = new AikoThreeScene(
    container.value, 
    (isLoading) => loading.value = isLoading,
    true
  )

  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  if (aikoScene) {
    aikoScene.destroy()
  }
})

function toggleGrid() {
  gridVisible.value = !gridVisible.value
  if (aikoScene) {
    aikoScene.toggleGrid(gridVisible.value)
  }
}

function handleResize() {
  if (aikoScene) {
    aikoScene.handleResize()
  }
}
</script>

<style>
.debug-page {
  -webkit-font-smoothing: none;
}
.debug-page a {
  text-decoration: none;
}
</style>

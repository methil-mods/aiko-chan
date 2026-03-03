<template>
  <div 
    ref="windowRef"
    class="bg-[var(--palette-1)] border-[6px] border-[var(--palette-8)] flex flex-col pointer-events-auto overflow-hidden select-none cam-window shadow-[8px_8px_0px_0px_rgba(0,0,0,0.1)]"
    :style="{
      position: 'absolute',
      left: pos.x + 'px',
      top: pos.y + 'px',
      width: size.w + 'px',
      height: size.h + 'px',
      zIndex: 100
    }"
  >
    <!-- Palette Title Bar (Darkest/Lightest Contrast) -->
    <div 
      @mousedown="startDrag"
      class="bg-[var(--palette-8)] text-[var(--palette-1)] px-4 py-2 flex items-center justify-between cursor-move border-b-[2px] border-[var(--palette-8)] shrink-0"
    >
      <div class="flex items-center gap-2">
        <div class="font-emoji">v</div>
        <span class="text-base lowercase tracking-tighter">aiko chat</span>
      </div>
      <div class="flex gap-2 pointer-events-auto">
        <button class="w-6 h-6 bg-[var(--palette-1)] border-2 border-[var(--palette-8)] flex items-center justify-center hover:bg-[var(--palette-2)] transition-colors group">
          <div class="w-3 h-0.5 bg-[var(--palette-8)] group-hover:scale-110"></div>
        </button>
        <button class="w-6 h-6 bg-[var(--palette-1)] border-2 border-[var(--palette-8)] flex items-center justify-center hover:bg-[var(--palette-2)] transition-colors group">
          <div class="w-3 h-3 border-2 border-[var(--palette-8)] group-hover:scale-110"></div>
        </button>
        <button class="w-6 h-6 bg-[var(--palette-3)] border-2 border-[var(--palette-8)] flex items-center justify-center hover:bg-[var(--palette-2)] transition-colors group">
          <span class="text-[var(--palette-8)] font-bold text-lg leading-none -translate-y-0.5 group-hover:scale-125">×</span>
        </button>
      </div>
    </div>

    <!-- Main Body -->
    <div class="flex flex-1 bg-[var(--palette-1)] min-h-0">
      <!-- Image Viewport -->
      <div class="flex-1 bg-[var(--palette-8)] relative overflow-hidden flex items-center justify-center border-r-[2px] border-[var(--palette-8)]">
        <AikoWindowScene ref="sceneRef" />
      </div>

      <!-- Chat Area -->
      <div class="w-[320px] lg:w-[400px] flex flex-col bg-[var(--palette-1)] shrink-0 min-h-0">
        <div class="flex-1 overflow-y-auto p-6 space-y-8 bg-[var(--palette-2)]/10">
          <div v-for="(msg, idx) in messages" :key="idx" 
            class="fade-in flex flex-col"
            :class="msg.role === 'user' ? 'items-end' : 'items-start'"
          >
            <!-- Label and Date -->
            <div class="flex items-baseline gap-2 mb-2" :class="msg.role === 'user' ? 'flex-row-reverse' : 'flex-row'">
              <span :class="[
                'text-[10px] uppercase tracking-[0.2em] font-bold',
                msg.role === 'assistant' ? 'text-[var(--palette-3)]' : 'text-[var(--palette-4)]'
              ]">
                {{ msg.role === 'assistant' ? 'aiko' : 'user' }}
              </span>
              <span class="text-[9px] text-[var(--palette-6)] opacity-50">13:52</span>
            </div>
            
            <!-- Message Bubble -->
            <div 
              class="text-[14px] leading-relaxed p-4 border-[2px] border-[var(--palette-8)] shadow-[4px_4px_0px_0px_rgba(0,0,0,0.05)] max-w-[90%]"
              :class="[
                msg.role === 'assistant' 
                  ? 'bg-[var(--palette-1)] text-[var(--palette-8)]' 
                  : 'bg-[var(--palette-7)] text-[var(--palette-1)]'
              ]"
            >
              {{ msg.content }}
            </div>
          </div>
          
          <div v-if="isTyping" class="flex gap-2 py-2">
            <div class="w-2 h-2 bg-[var(--palette-3)] animate-bounce"></div>
            <div class="w-2 h-2 bg-[var(--palette-3)] animate-bounce" style="animation-delay: 0.1s"></div>
            <div class="w-2 h-2 bg-[var(--palette-3)] animate-bounce" style="animation-delay: 0.2s"></div>
          </div>
        </div>

        <!-- Input Area -->
        <div class="p-6 bg-[var(--palette-1)] border-t-[2px] border-[var(--palette-8)]">
          <form @submit.prevent="onSend" class="flex gap-3">
            <input 
              v-model="input"
              type="text" 
              placeholder="type your message..." 
              class="flex-1 bg-[var(--palette-1)] px-4 py-3 text-[14px] text-[var(--palette-8)] border-[2px] border-[var(--palette-8)] focus:outline-none focus:bg-[var(--palette-2)]/20 placeholder:text-[var(--palette-5)] lowercase select-text min-w-0"
            />
            <button 
              type="submit"
              :disabled="!input.trim() || isTyping"
              class="w-14 h-[48px] bg-[var(--palette-8)] text-[var(--palette-1)] hover:bg-[var(--palette-7)] disabled:opacity-20 transition-all active:translate-y-1 shadow-[0px_4px_0px_0px_rgba(0,0,0,0.2)] flex items-center justify-center shrink-0"
            >
              <span class="font-emoji text-2xl leading-none -rotate-45">➔</span>
            </button>
          </form>
        </div>
      </div>
    </div>

    <!-- Resize Handle -->
    <div 
      @mousedown.stop="startResize"
      class="absolute bottom-0 right-0 w-8 h-8 cursor-nwse-resize z-50 flex items-center justify-center p-1"
    >
      <div class="w-full h-full border-r-2 border-b-2 border-[var(--palette-8)] opacity-30"></div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, reactive, watch } from 'vue'

const props = defineProps<{
  messages: Array<{ role: 'user' | 'assistant', content: string }>
  isTyping: boolean
}>()

const emit = defineEmits(['send'])
const input = ref('')
const windowRef = ref<HTMLElement | null>(null)
const sceneRef = ref<{ handleResize: () => void } | null>(null)

const pos = reactive({ x: 100, y: 100 })
const size = reactive({ w: 1024, h: 720 })

// Watch for manual size updates to trigger renderer resize
watch(() => [size.w, size.h], () => {
  if (sceneRef.value) {
    sceneRef.value.handleResize()
  }
}, { flush: 'post' })

// Dragging
let isDragging = false
let dragOffset = { x: 0, y: 0 }

function startDrag(e: MouseEvent) {
  if ((e.target as HTMLElement).closest('button')) return
  isDragging = true
  dragOffset.x = e.clientX - pos.x
  dragOffset.y = e.clientY - pos.y
  window.addEventListener('mousemove', onDrag)
  window.addEventListener('mouseup', stopDrag)
}

function onDrag(e: MouseEvent) {
  if (!isDragging) return
  pos.x = e.clientX - dragOffset.x
  pos.y = e.clientY - dragOffset.y
}

function stopDrag() {
  isDragging = false
  window.removeEventListener('mousemove', onDrag)
  window.removeEventListener('mouseup', stopDrag)
}

// Resizing
let isResizing = false
let initialSize = { w: 0, h: 0 }
let initialPos = { x: 0, y: 0 }

function startResize(e: MouseEvent) {
  isResizing = true
  initialSize.w = size.w
  initialSize.h = size.h
  initialPos.x = e.clientX
  initialPos.y = e.clientY
  window.addEventListener('mousemove', onResize)
  window.addEventListener('mouseup', stopResize)
}

function onResize(e: MouseEvent) {
  if (!isResizing) return
  const dw = e.clientX - initialPos.x
  const dh = e.clientY - initialPos.y
  size.w = Math.max(500, initialSize.w + dw)
  size.h = Math.max(400, initialSize.h + dh)
}

function stopResize() {
  isResizing = false
  window.removeEventListener('mousemove', onResize)
  window.removeEventListener('mouseup', stopResize)
}

function onSend() {
  if (!input.value.trim()) return
  emit('send', input.value)
  input.value = ''
}

onMounted(() => {
  pos.x = Math.max(0, (window.innerWidth - size.w) / 2)
  pos.y = Math.max(0, (window.innerHeight - size.h) / 2)
})
</script>

<style scoped>
.cam-window {
  transition: none !important;
  will-change: left, top, width, height;
}

.fade-in {
  animation: fadeIn 0.2s steps(4) forwards;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(4px); }
  to { opacity: 1; transform: translateY(0); }
}

::-webkit-scrollbar {
  width: 12px;
}
::-webkit-scrollbar-track {
  background: var(--palette-1);
  border-left: 2px solid var(--palette-8);
}
::-webkit-scrollbar-thumb {
  background: var(--palette-8);
  border: 2px solid var(--palette-1);
}
::-webkit-scrollbar-thumb:hover {
  background: var(--palette-7);
}
</style>

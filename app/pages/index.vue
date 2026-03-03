<template>
  <div class="relative w-full h-screen overflow-hidden bg-[var(--palette-2)] selection:bg-[var(--palette-3)]/30">
    <!-- Centered aikocam window -->
    <AikoCam 
      :messages="messages" 
      :is-typing="isTyping"
      @send="handleSend"
    />
    
    <!-- OS Bottom Activity Bar -->
    <div class="fixed bottom-0 left-0 w-full h-12 bg-[var(--palette-8)] border-t-[4px] border-[var(--palette-7)] flex items-center justify-between px-6 z-[200]">
      <div class="flex items-center gap-6 h-full">
        <!-- Active Tasks -->
        <div class="flex items-center gap-1 h-full py-1.5">
          <div class="px-4 h-full bg-[var(--palette-1)] text-[var(--palette-8)] flex items-center gap-2 border-[2px] border-[var(--palette-7)]">
            <div class="w-2 h-2 bg-[var(--palette-3)] animate-pulse"></div>
            <span class="text-[11px] font-bold lowercase">aiko chat</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'

useHead({
  title: 'aiko'
})

interface Message {
  role: 'user' | 'assistant'
  content: string
}

const messages = ref<Message[]>([
  {
    role: 'assistant',
    content: "aikocam version 1.0.4 - system check: ok. remote connection established via secure tunnel. awaiting user input..."
  }
])
const isTyping = ref(false)

async function handleSend(content: string) {
  if (isTyping.value) return
  
  messages.value.push({ role: 'user', content })
  
  isTyping.value = true
  await new Promise(resolve => setTimeout(resolve, 800 + Math.random() * 1000))
  
  isTyping.value = false
  messages.value.push({
    role: 'assistant',
    content: getAikoResponse(content)
  })
}

function getAikoResponse(input: string): string {
  const responses = [
    "log file updated. tracking user interaction...",
    "the 3d environment is stable. rendering aiko sub-processes.",
    "data packets received. analyzing sentiment...",
    "i'm here in the virtual workspace. what's the next task?",
    "awaiting further instructions from the primary controller.",
    "stream feed optimization complete. signal strength: 98%.",
    "user footprint logged. maintaining connection stability."
  ]
  const randomIdx = Math.floor(Math.random() * responses.length)
  return responses[randomIdx] ?? "connection stable. awaiting commands."
}
</script>

<style scoped>
body {
  margin: 0;
  padding: 0;
  background: var(--palette-8);
  overflow: hidden;
}
</style>

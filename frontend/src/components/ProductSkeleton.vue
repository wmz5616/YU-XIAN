<template>
  <div class="bg-white rounded-xl border border-slate-100 overflow-hidden shadow-sm">
    <div class="h-48 w-full bg-slate-200 animate-pulse relative overflow-hidden">
      <div class="absolute inset-0 -translate-x-full animate-[shimmer_1.5s_infinite] bg-gradient-to-r from-transparent via-white/20 to-transparent"></div>
    </div>
    
    <div class="p-5 space-y-3">
      <div class="h-5 bg-slate-200 rounded w-3/4 animate-pulse"></div>
      <div class="h-3 bg-slate-200 rounded w-1/4 animate-pulse"></div>
      <div class="space-y-2 pt-2">
        <div class="h-3 bg-slate-100 rounded w-full animate-pulse"></div>
        <div class="h-3 bg-slate-100 rounded w-5/6 animate-pulse"></div>
      </div>
      <div class="flex justify-between items-center pt-4">
        <div class="h-6 bg-slate-200 rounded w-1/3 animate-pulse"></div>
        <div class="h-8 w-8 bg-slate-200 rounded-full animate-pulse"></div>
      </div>
    </div>
  </div>
</template>

<style>
@keyframes shimmer {
  100% { transform: translateX(100%); }
}
</style>
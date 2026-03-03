<template>
  <div>
    <NuxtPage />
  </div>
</template>

<style>
@import url('https://fonts.googleapis.com/css2?family=Tahoma&family=Outfit:wght@400;700&display=swap');

:root {
  /* Lospec Hortensia Diamond Palette */
  --palette-1: #d4faff; /* Lightest */
  --palette-2: #dabaff;
  --palette-3: #fc9bd3;
  --palette-4: #bc8dff;
  --palette-5: #8977c9;
  --palette-6: #4c587e;
  --palette-7: #3c4059;
  --palette-8: #332b48; /* Darkest */

  /* Semantic Aliases */
  --bg-primary: var(--palette-8);
  --bg-secondary: var(--palette-1);
  --text-primary: var(--palette-1);
  --text-inverse: var(--palette-8);
  --accent: var(--palette-3);
}

@font-face {
  font-family: 'PixelPoiiz';
  src: url('/fonts/pixelpoiiz.ttf') format('truetype');
  font-weight: normal;
  font-style: normal;
  font-display: swap;
}

@font-face {
  font-family: 'VCR_OSD';
  src: url('/fonts/vcrosd.ttf') format('truetype');
  font-weight: normal;
  font-style: normal;
  font-display: swap;
}

@font-face {
  font-family: 'EmojiFont';
  src: url('/fonts/emoji.ttf') format('truetype');
  font-weight: normal;
  font-style: normal;
  font-display: swap;
}

body {
  font-family: 'VCR_OSD', monospace;
  margin: 0;
  padding: 0;
  background: var(--bg-primary);
  color: var(--text-primary);
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
}

.font-pixel {
  font-family: 'VCR_OSD', monospace;
}

.font-vcr {
  font-family: 'VCR_OSD', monospace;
}

.font-emoji {
  font-family: 'EmojiFont', sans-serif;
}

::-webkit-scrollbar {
  width: 0px;
}

/* Palette utility classes */
.bg-p1 { background-color: var(--palette-1); }
.bg-p2 { background-color: var(--palette-2); }
.bg-p3 { background-color: var(--palette-3); }
.bg-p4 { background-color: var(--palette-4); }
.bg-p5 { background-color: var(--palette-5); }
.bg-p6 { background-color: var(--palette-6); }
.bg-p7 { background-color: var(--palette-7); }
.bg-p8 { background-color: var(--palette-8); }

.text-p1 { color: var(--palette-1); }
.text-p2 { color: var(--palette-2); }
.text-p3 { color: var(--palette-3); }
.text-p4 { color: var(--palette-4); }
.text-p5 { color: var(--palette-5); }
.text-p6 { color: var(--palette-6); }
.text-p7 { color: var(--palette-7); }
.text-p8 { color: var(--palette-8); }

.border-p1 { border-color: var(--palette-1); }
.border-p8 { border-color: var(--palette-8); }
</style>

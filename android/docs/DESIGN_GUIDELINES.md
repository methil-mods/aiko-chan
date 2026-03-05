# Aiko-chan Design Guidelines
## Y2K Pastel Windows XP Cursed Fashion Aesthetic

---

## 1. Overview

This document defines the design system for **Aiko-chan**, an AI chat application with a unique Y2K pastel Windows XP aesthetic combined with Japanese e-girl/cursed fashion elements.

### Design Philosophy
- **Nostalgic**: Early 2000s Windows UI elements
- **Pastel**: Soft lavender, pink, and purple color palette
- **Digital**: Dithered patterns, pixelated textures
- **Japanese**: J-pop/e-girl visual culture integration
- **Cursed**: Intentionally uncanny, glitchy aesthetic

---

## 2. Color Palette

### Primary Colors

| Name | Hex | Usage |
|------|-----|-------|
| `LightViolet` | `#E0B8FF` | Backgrounds, window title bars, borders |
| `DarkPurple` | `#352B4A` | Primary text, dark backgrounds |
| `LightestPink` | `#F7E0FB` | Surface backgrounds, button fills |
| `PinkAccent` | `#C08ACA` | Accent elements, highlights |
| `DarkVioletText` | `#22183A` | Body text on light backgrounds |

### Extended Palette (from Figma)

| Name | Hex | Usage |
|------|-----|-------|
| `SplashBG` | `#D8B8F0` | Splash screen background |
| `OnboardingBG` | `#4A3A6A` | Onboarding dark background |
| `WindowBorder` | `#E0B8FF` | XP window borders |
| `MessageBubbleAiko` | `#F7E0FB` | Aiko's message bubbles |
| `MessageBubbleUser` | `#FFFFFF` | User message bubbles |
| `KeyboardGray` | `#D0D0D0` | Custom keyboard background |

### Color Scheme Mapping

```kotlin
// Light Theme (Primary)
primary = PinkAccent          // #C08ACA
secondary = DarkPurple        // #352B4A
tertiary = LightestPink       // #F7E0FB
background = LightViolet      // #E0B8FF
surface = LightestPink        // #F7E0FB
onPrimary = Color.White
onSecondary = Color.White
onTertiary = DarkPurple
onBackground = DarkVioletText
onSurface = DarkVioletText
```

---

## 3. Typography

### Font Families

| Style | Font | Usage |
|-------|------|-------|
| Headers | Pixel font / MS Sans Serif | Window titles, headers |
| Body | Default sans-serif | Message content |
| Japanese | System Japanese | Japanese text (話す，etc.) |

### Type Scale

| Token | Size | Weight | Line Height | Usage |
|-------|------|--------|-------------|-------|
| `windowTitle` | 12sp | Bold | - | XP window title bars |
| `buttonLarge` | 20sp | Bold | - | Primary buttons |
| `body` | 16sp | Normal | 24sp | Message content |
| `input` | 16sp | Normal | - | Input fields |
| `statLabel` | 12sp | Normal | - | Stat labels (LUV, NRG) |

---

## 4. UI Components

### 4.1 XP Window (`XpWindow`)

The signature component - Windows XP-style window chrome.

```
┌─────────────────────────────────┐
│ [Title Bar - LightViolet]  ─ × │  ← 30dp height
├─────────────────────────────────┤
│                                 │
│    Content Area (White)         │
│                                 │
│    3dp LightViolet border       │
└─────────────────────────────────┘
```

**Properties:**
- Border: 3dp solid `LightViolet`
- Title bar height: 30dp
- Title bar background: `LightViolet`
- Content background: White
- Shadow: 4dp
- Corner radius: 0dp (sharp corners)

### 4.2 Y2K Button (`Y2kButton`)

```
┌─────────────────────────┐
│   話す (Discuter)       │  ← LightestPink fill
└─────────────────────────┘
  ↑ 3dp LightViolet border
```

**Properties:**
- Background: `LightestPink`
- Border: 3dp solid `LightViolet`
- Shadow: 4dp (`DarkPurple` ambient + spot)
- Corner radius: 0dp (sharp)
- Text: `DarkPurple`, Bold, 20sp

### 4.3 Chat Bubble (`ChatBubble`)

```
Aiko:  [Avatar]  ┌─────────────┐
                  │ Message     │  ← LightestPink
                  └─────────────┘
                    ↑ 2dp LightViolet border

User:      ┌─────────────┐
           │ Message     │  ← White
           └─────────────┘ [Avatar]
```

**Properties:**
- Max width: 260dp
- Corner radius: 8dp
- Border: 2dp solid `LightViolet`
- Shadow: 4dp
- Avatar: 40dp circular with 2dp `LightViolet` border

### 4.4 Input Area (`Y2kInputArea`)

```
┌─────────────────────────────────────┐
│  Discuter...                    [→] │  ← 56dp height
└─────────────────────────────────────┘
  ↑ 3dp LightViolet border
```

**Properties:**
- Height: 56dp
- Border: 3dp solid `LightViolet`
- Background: White
- Shadow: 4dp

### 4.5 Custom Keyboard (`AikoCustomKeyboard`)

```
┌─────────────────────────────────────┐
│  Q W E R T Y U I O P               │
│   A S D F G H J K L                │
│    Z X C V B N M ⌫                 │
└─────────────────────────────────────┘
  ↑ KeyboardGray background
```

**Properties:**
- Background: `#D0D0D0` (keyboard gray)
- Key background: White
- Key corner radius: 4dp
- Key height: 45dp
- Spacing: 4dp horizontal, 8dp vertical

---

## 5. Layout Patterns

### 5.1 Message Screen Layout

```
┌─────────────────────────────────────┐
│                                     │
│  ┌──────────┐  ┌───────────────┐   │
│  │  Mood    │  │    Stats      │   │  ← 140dp height
│  │ [Image]  │  │  LUV ♥♥♥♥♥   │   │
│  │          │  │  NRG ✦✦✦✦   │   │
│  └──────────┘  └───────────────┘   │
│                                     │
│  ────────────────────────────────   │
│  [Chat Messages - LazyColumn]       │
│  ────────────────────────────────   │
│                                     │
│  ┌─────────────────────────────┐   │
│  │  Input Area                 │   │  ← 56dp
│  └─────────────────────────────┘   │
│                                     │
│  ┌─────────────────────────────┐   │
│  │  Custom Keyboard (optional) │   │  ← 250dp
│  └─────────────────────────────┘   │
└─────────────────────────────────────┘
```

### 5.2 Onboarding Screen Layout

```
┌─────────────────────────────────────┐
│                                     │
│         [Dither Background]         │
│                                     │
│           ┌───────────┐             │
│           │           │             │
│           │  [Aiko]   │             │  ← Centered
│           │  e-girl   │             │
│           │   art     │             │
│           └───────────┘             │
│                                     │
│      ┌───────────────────┐          │
│      │   話す (Discuter) │          │  ← 220dp × 60dp
│      └───────────────────┘          │
│                                     │
│           [Methil Logo]             │  ← 60dp, bottom
└─────────────────────────────────────┘
```

### 5.3 Splash Screen Animation

```
Phase 1 (0-1.5s):     Phase 2 (1.5-3.5s):    Phase 3 (3.5-4.3s):
┌─────────────┐       ┌─────────────┐        ┌─────────────┐
│             │       │             │        │░░░░░░░░░░░░░│
│   [Methil   │       │   [Aiko     │        │░░░░░░░░░░░░░│
│    Logo]    │  →    │  Destructed │   →    │░░░░░[Logo]░░│
│             │       │    Logo]    │        │░░░░░░░░░░░░░│
│             │       │             │        │░░░░░░░░░░░░░│
└─────────────┘       └─────────────┘        └─────────────┘
  scale: 1.0            scale: 0.5             Dither slide-up
  y: 0                  y: +300dp              Logo: 0.6 scale
```

---

## 6. Assets & Resources

### 6.1 Required Assets

| Asset | File | Usage |
|-------|------|-------|
| Methil Logo | `logo-methil-jap.svg` | Splash screen, onboarding |
| Aiko Logo | `aikochan-logo-destructed-tp 1.png` | Splash phase 2+ |
| Dither Background | `dither-bg-dark.png` | Onboarding, splash transition |
| Chat Background | `chat-bg.png` | Message screen background |
| E-girl Normal | `e-girl.png` | Onboarding character |
| E-girl Emotion | `e-girl-emo.png` | Mood window |
| E-girl Profile Pic | `e-girl-pp.png` | Chat bubble avatar |

### 6.2 Icons

| Icon | File | Usage |
|------|------|-------|
| Heart Fill | `heart_fill_ico.svg` | Stat ratings (LUV) |
| Spark Fill | `spark_fill_ico.svg` | Stat ratings (NRG) |
| Send | `send_ico.svg` | Input area send button |
| Minimize | `reduce_ico.svg` | Window controls |
| Close | `close_ico.svg` | Window controls |
| Moon | `moon_ico.svg`, `moon_fill_ico.svg` | Mood indicators |
| Eye | `eye_ico.svg`, `eye_fill_ico.svg` | Mood indicators |

---

## 7. Animation Guidelines

### 7.1 Timing

| Animation | Duration | Easing |
|-----------|----------|--------|
| Splash logo transition | 1000ms | FastOutSlowIn |
| Splash dither slide | 800ms | Default |
| Logo crossfade | 500ms | Default |
| Chat message scroll | 300ms | Default |

### 7.2 Patterns

**Slide Transitions:**
- Vertical slide for dither overlay
- Offset: `initialOffsetY = { it }` (full height)

**Scale Transitions:**
- Logo: 1.0 → 0.5 → 0.6
- Use `animateFloatAsState` with `FastOutSlowInEasing`

---

## 8. Japanese Text Elements

| Context | Japanese | Romaji | Meaning |
|---------|----------|--------|---------|
| Start Button | 話す | Hanasu | "Talk" / "Discuter" |
| Mood Window | 感情 | Kanjō | "Emotion/Mood" |
| Stats Window | 身体の状態 | Karada no jōtai | "Body condition" |
| Fatigue | 疲労 | Hirō | "Fatigue" |
| Guilty | 有罪 | Yūzai | "Guilty" |
| Aroused | 欲情した | Yokujō shita | "Aroused" |
| Input Placeholder | 愛子と話す | Aiko to hanasu | "Talk with Aiko" |

---

## 9. Implementation Notes

### 9.1 Current Deviations from Figma

1. **Keyboard**: Currently using generic gray, should match Figma's custom Y2K styling
2. **Window Controls**: Using placeholder icons, need proper XP-style minimize/close
3. **Stat Icons**: Using placeholder URLs, need local asset references
4. **Font**: Not yet using pixel/Microsoft Sans Serif for window titles
5. **Dither Pattern**: Asset URLs are placeholders

### 9.2 Recommended Improvements

1. Add custom pixel font for authentic Y2K feel
2. Implement proper dithered background patterns
3. Add CRT scanline effect option
4. Implement window drag functionality
5. Add Japanese IME keyboard toggle
6. Create animated emoji/reaction system matching Figma mood indicators

---

## 10. References

- Figma Design Board: [Link to design]
- Windows XP UI Guidelines: Microsoft Visual Styles
- Y2K Aesthetic Reference: Early 2000s web design
- Japanese E-girl Culture: Visual Kei, Decora, Yami Kawaii

---

*Last updated: 2026*
*Version: 1.0*
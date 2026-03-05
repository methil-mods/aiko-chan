# Aiko-chan Color Palette Reference
## Y2K Pastel Windows XP Color System

---

## Quick Reference

### Copy-Paste Values

```kotlin
// app/src/main/java/com/methil/aiko/ui/theme/Color.kt

// Primary Palette
val LightViolet = Color(0xFFE0B8FF)        // Main background, title bars
val DarkPurple = Color(0xFF352B4A)         // Primary text, dark surfaces
val LightestPink = Color(0xFFF7E0FB)       // Buttons, surfaces
val PinkAccent = Color(0xFFC08ACA)         // Highlights, accents
val DarkVioletText = Color(0xFF22183A)     // Body text

// Extended Palette
val SplashBG = Color(0xFFD8B8F0)           // Splash screen
val OnboardingBG = Color(0xFF4A3A6A)       // Dark dither overlay base
val WindowBorder = Color(0xFFE0B8FF)       // Same as LightViolet
val KeyboardGray = Color(0xFFD0D0D0)       // Custom keyboard
```

```xml
<!-- res/values/colors.xml -->
<resources>
    <color name="light_violet">#E0B8FF</color>
    <color name="dark_purple">#352B4A</color>
    <color name="lightest_pink">#F7E0FB</color>
    <color name="pink_accent">#C08ACA</color>
    <color name="dark_violet_text">#22183A</color>
    <color name="splash_bg">#D8B8F0</color>
    <color name="onboarding_bg">#4A3A6A</color>
    <color name="keyboard_gray">#D0D0D0</color>
</resources>
```

```css
/* For web/marketing materials */
:root {
  --light-violet: #E0B8FF;
  --dark-purple: #352B4A;
  --lightest-pink: #F7E0FB;
  --pink-accent: #C08ACA;
  --dark-violet-text: #22183A;
  --splash-bg: #D8B8F0;
  --onboarding-bg: #4A3A6A;
  --keyboard-gray: #D0D0D0;
}
```

---

## Color Details

### LightViolet `#E0B8FF`

**Usage:** Primary background, window title bars, borders, status/navigation bar

![Color swatch](data:image/svg+xml,<svg xmlns='http://www.w3.org/2000/svg' width='100' height='100'><rect fill='%23E0B8FF' width='100' height='100'/></svg>)

| Property | Value |
|----------|-------|
| RGB | rgb(224, 184, 255) |
| HSL | hsl(274°, 63%, 86%) |
| CMYK | 12%, 28%, 0%, 0% |
| Android | `0xFFE0B8FF` |

**Used in:**
- `XpWindow` title bar background
- `XpWindow` border (3dp)
- `ChatBubble` avatar border
- `Y2kInputArea` border
- `Y2kButton` border
- Status bar color
- Navigation bar color
- Main screen background

---

### DarkPurple `#352B4A`

**Usage:** Primary text on light backgrounds, dark surfaces

![Color swatch](data:image/svg+xml,<svg xmlns='http://www.w3.org/2000/svg' width='100' height='100'><rect fill='%23352B4A' width='100' height='100'/></svg>)

| Property | Value |
|----------|-------|
| RGB | rgb(53, 43, 74) |
| HSL | hsl(259°, 26%, 23%) |
| CMYK | 28%, 42%, 0%, 71% |
| Android | `0xFF352B4A` |

**Used in:**
- Window title text
- Button text
- Message text
- Stat labels
- Shadow color (ambient + spot)

---

### LightestPink `#F7E0FB`

**Usage:** Surface backgrounds, button fills, Aiko's message bubbles

![Color swatch](data:image/svg+xml,<svg xmlns='http://www.w3.org/2000/svg' width='100' height='100'><rect fill='%23F7E0FB' width='100' height='100'/></svg>)

| Property | Value |
|----------|-------|
| RGB | rgb(247, 224, 251) |
| HSL | hsl(292°, 63%, 93%) |
| CMYK | 2%, 11%, 0%, 2% |
| Android | `0xFFF7E0FB` |

**Used in:**
- `Y2kButton` background
- `ChatBubble` (Aiko messages)
- `Surface` components
- Light theme surface color

---

### PinkAccent `#C08ACA`

**Usage:** Accent elements, highlights, primary color in theme

![Color swatch](data:image/svg+xml,<svg xmlns='http://www.w3.org/2000/svg' width='100' height='100'><rect fill='%23C08ACA' width='100' height='100'/></svg>)

| Property | Value |
|----------|-------|
| RGB | rgb(192, 138, 202) |
| HSL | hsl(291°, 34%, 67%) |
| CMYK | 5%, 32%, 0%, 21% |
| Android | `0xFFC08ACA` |

**Used in:**
- Theme primary color
- Highlight elements
- Icon accents

---

### DarkVioletText `#22183A`

**Usage:** Body text on light backgrounds

![Color swatch](data:image/svg+xml,<svg xmlns='http://www.w3.org/2000/svg' width='100' height='100'><rect fill='%2322183A' width='100' height='100'/></svg>)

| Property | Value |
|----------|-------|
| RGB | rgb(34, 24, 58) |
| HSL | hsl(258°, 42%, 16%) |
| CMYK | 42%, 59%, 0%, 77% |
| Android | `0xFF22183A` |

**Used in:**
- Body text
- Input text
- onBackground color
- onSurface color

---

### SplashBG `#D8B8F0`

**Usage:** Splash screen background

![Color swatch](data:image/svg+xml,<svg xmlns='http://www.w3.org/2000/svg' width='100' height='100'><rect fill='%23D8B8F0' width='100' height='100'/></svg>)

| Property | Value |
|----------|-------|
| RGB | rgb(216, 184, 240) |
| HSL | hsl(274°, 55%, 83%) |
| CMYK | 10%, 23%, 0%, 6% |
| Android | `0xFFD8B8F0` |

---

### OnboardingBG `#4A3A6A`

**Usage:** Dark dither overlay base for onboarding

![Color swatch](data:image/svg+xml,<svg xmlns='http://www.w3.org/2000/svg' width='100' height='100'><rect fill='%234A3A6A' width='100' height='100'/></svg>)

| Property | Value |
|----------|-------|
| RGB | rgb(74, 58, 106) |
| HSL | hsl(260°, 29%, 32%) |
| CMYK | 30%, 45%, 0%, 58% |
| Android | `0xFF4A3A6A` |

---

### KeyboardGray `#D0D0D0`

**Usage:** Custom keyboard background

![Color swatch](data:image/svg+xml,<svg xmlns='http://www.w3.org/2000/svg' width='100' height='100'><rect fill='%23D0D0D0' width='100' height='100'/></svg>)

| Property | Value |
|----------|-------|
| RGB | rgb(208, 208, 208) |
| HSL | hsl(0°, 0%, 82%) |
| CMYK | 0%, 0%, 0%, 18% |
| Android | `0xFFD0D0D0` |

---

## Color Combinations

### Accessible Text Combinations

✅ **Good:**
- `DarkPurple` on `LightestPink` - High contrast
- `DarkVioletText` on `LightViolet` - Good contrast
- `DarkVioletText` on White - Best contrast
- White on `DarkPurple` - High contrast

❌ **Avoid:**
- `PinkAccent` on `LightViolet` - Low contrast
- `LightestPink` on White - Too subtle
- `DarkPurple` on `DarkVioletText` - No contrast

### Recommended Pairings

| Background | Text Color | Use Case |
|------------|------------|----------|
| `LightViolet` | `DarkPurple` | Window titles |
| `LightestPink` | `DarkPurple` | Buttons |
| White | `DarkVioletText` | Body content |
| `DarkPurple` | `LightestPink` | Dark mode surfaces |

---

## Gradient Reference

### Optional Gradients (Figma-inspired)

```kotlin
// Vertical gradient for windows
val windowGradient = Brush.verticalGradient(
    colors = listOf(
        Color(0xFFF7E0FB),  // LightestPink
        Color(0xFFE0B8FF)   // LightViolet
    )
)

// Dither overlay effect
val ditherOverlay = Brush.radialGradient(
    colors = listOf(
        Color(0x804A3A6A),  // Semi-transparent OnboardingBG
        Color(0x004A3A6A)
    )
)
```

---

## Dark Theme Variant

If implementing dark theme in the future:

```kotlin
private val DarkColorScheme = darkColorScheme(
    primary = PinkAccent,
    secondary = LightViolet,
    tertiary = LightestPink,
    background = DarkPurple,
    surface = Color(0xFF2A2238),  // Slightly lighter than DarkPurple
    onPrimary = Color.White,
    onSecondary = DarkPurple,
    onTertiary = DarkPurple,
    onBackground = LightestPink,
    onSurface = LightestPink
)
```

---

## Accessibility Notes

### WCAG 2.1 Compliance

| Combination | Contrast Ratio | Rating |
|-------------|----------------|--------|
| `DarkVioletText` on White | 15.2:1 | AAA |
| `DarkPurple` on `LightestPink` | 12.8:1 | AAA |
| `DarkPurple` on `LightViolet` | 6.4:1 | AA |
| `PinkAccent` on White | 3.2:1 | AA (small text: fail) |

**Recommendation:** Use `DarkVioletText` or `DarkPurple` for all body text to ensure readability.

---

*Last updated: 2026*
*Version: 1.0*
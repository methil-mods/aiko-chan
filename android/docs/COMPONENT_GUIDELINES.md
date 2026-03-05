# Aiko-chan Component Guidelines
## UI Component Implementation Reference

---

## Core Components Overview

This document provides detailed implementation guidelines for all UI components in the Aiko-chan application.

```
Component Hierarchy:
├── XpWindow (Container)
│   ├── Title Bar
│   └── Content Area
├── Y2kButton (Interactive)
├── ChatBubble (Display)
├── Y2kInputArea (Input)
└── AikoCustomKeyboard (Input)
```

---

## 1. XpWindow Component

### Purpose
Windows XP-style window container for content sections (Mood, Stats windows).

### Visual Specification

```
┌─────────────────────────────────────┐ ← 3dp LightViolet border
│ ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━ ─ × │ ← 30dp height, LightViolet bg
├─────────────────────────────────────┤
│                                     │
│           Content Area              │
│           (White background)        │
│                                     │
└─────────────────────────────────────┘
```

### Implementation

```kotlin
@Composable
fun XpWindow(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier
            .shadow(4.dp, shape = RoundedCornerShape(0.dp))
            .background(Color.White),
        border = BorderStroke(3.dp, LightViolet),
        color = Color.White
    ) {
        Column {
            // Title Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(30.dp)
                    .background(LightViolet)
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = title,
                    color = DarkPurple,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    // Window control icons
                }
            }
            // Content
            Box(modifier = Modifier.fillMaxSize()) {
                content()
            }
        }
    }
}
```

### Properties Table

| Property | Value | Notes |
|----------|-------|-------|
| Border width | 3dp | LightViolet color |
| Title bar height | 30dp | Fixed height |
| Title bar bg | LightViolet | #E0B8FF |
| Content bg | White | #FFFFFF |
| Shadow | 4dp | Sharp corners |
| Corner radius | 0dp | Sharp corners (Y2K style) |
| Title font | 12sp, Bold | DarkPurple color |

### Usage Example

```kotlin
XpWindow(title = "感情 (Mood)") {
    AsyncImage(
        model = R.drawable.e_girl_emo,
        contentDescription = null,
        modifier = Modifier.fillMaxSize(),
        contentScale = ContentScale.Crop
    )
}

XpWindow(title = "身体の状態 (Stats)") {
    Column(modifier = Modifier.padding(8.dp)) {
        StatRow("LUV", heartIcons)
        StatRow("NRG", sparkIcons)
    }
}
```

---

## 2. Y2kButton Component

### Purpose
Primary action button with Y2K aesthetic.

### Visual Specification

```
┌─────────────────────────────────┐
│                                 │ ← LightestPink fill
│      話す (Discuter)            │ ← DarkPurple, Bold, 20sp
│                                 │
└─────────────────────────────────┘
  ↑ 3dp LightViolet border
```

### Implementation

```kotlin
@Composable
fun Y2kButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(0.dp),
                ambientColor = DarkPurple,
                spotColor = DarkPurple
            ),
        color = LightestPink,
        border = BorderStroke(3.dp, LightViolet)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = text,
                color = DarkPurple,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
```

### Properties Table

| Property | Value | Notes |
|----------|-------|-------|
| Background | LightestPink | #F7E0FB |
| Border | 3dp | LightViolet |
| Shadow | 4dp | DarkPurple ambient + spot |
| Corner radius | 0dp | Sharp corners |
| Text color | DarkPurple | #352B4A |
| Text size | 20sp | Bold weight |
| Click ripple | None | indication = null |

### Usage Example

```kotlin
Y2kButton(
    text = "話す (Discuter)",
    onClick = { navController.navigate("message") },
    modifier = Modifier
        .width(220.dp)
        .height(60.dp)
)
```

---

## 3. ChatBubble Component

### Purpose
Display individual chat messages with avatar.

### Visual Specification

```
Aiko's Message:
  ┌────┐  ┌─────────────────┐
  │ 👤 │  │ Hello there!    │ ← LightestPink bg
  └────┘  └─────────────────┘
           ↑ 2dp LightViolet border

User's Message:
      ┌─────────────────┐ ┌────
      │ How are you?    │ │ 👤 │ ← White bg
      └─────────────────┘ └────┘
           ↑ 2dp LightViolet border
```

### Implementation

```kotlin
@Composable
fun ChatBubble(message: Message) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isAiko) Arrangement.Start else Arrangement.End,
        verticalAlignment = Alignment.Bottom
    ) {
        if (message.isAiko) {
            AsyncImage(
                model = R.drawable.e_girl_pp,
                contentDescription = null,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .border(2.dp, LightViolet, CircleShape)
            )
            Spacer(modifier = Modifier.width(8.dp))
        }

        Surface(
            modifier = Modifier
                .widthIn(max = 260.dp)
                .shadow(4.dp, shape = RoundedCornerShape(8.dp)),
            color = if (message.isAiko) LightestPink else Color.White,
            border = BorderStroke(2.dp, LightViolet),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(
                text = message.text,
                modifier = Modifier.padding(12.dp),
                color = DarkPurple,
                fontSize = 16.sp
            )
        }
    }
}
```

### Properties Table

| Property | Value | Notes |
|----------|-------|-------|
| Max width | 260dp | Constrains message width |
| Corner radius | 8dp | Rounded bubbles |
| Border | 2dp | LightViolet |
| Shadow | 4dp | |
| Avatar size | 40dp | Circular with border |
| Avatar border | 2dp | LightViolet |
| Padding | 12dp | Inside bubble |
| Text size | 16sp | DarkPurple |

### Background Colors

| Sender | Background |
|--------|------------|
| Aiko | LightestPink (#F7E0FB) |
| User | White (#FFFFFF) |

### Usage Example

```kotlin
val messages = listOf(
    Message("Hello there! I'm Aiko.", isAiko = true),
    Message("How are you feeling today?", isAiko = true),
    Message("I'm doing great!", isAiko = false)
)

LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
    items(messages) { msg ->
        ChatBubble(msg)
    }
}
```

---

## 4. Y2kInputArea Component

### Purpose
Message input field with send button.

### Visual Specification

```
┌─────────────────────────────────────────────┐
│                                             │
│  Discuter...                          [→]  │ ← 56dp height
│                                             │
└─────────────────────────────────────────────┘
  ↑ 3dp LightViolet border
```

### Implementation

```kotlin
@Composable
fun Y2kInputArea(
    text: String,
    onInputClick: () -> Unit,
    onSend: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .height(56.dp)
            .shadow(4.dp),
        border = BorderStroke(3.dp, LightViolet),
        color = Color.White
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clickable { onInputClick() }
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = if (text.isEmpty()) "Discuter..." else text,
                    color = if (text.isEmpty()) Color.Gray else DarkPurple,
                    fontSize = 16.sp
                )
            }
            IconButton(onClick = onSend) {
                Icon(
                    painter = painterResource(R.drawable.send_ico),
                    contentDescription = "Send",
                    tint = DarkPurple,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}
```

### Properties Table

| Property | Value | Notes |
|----------|-------|-------|
| Height | 56dp | Fixed |
| Border | 3dp | LightViolet |
| Background | White | |
| Shadow | 4dp | |
| Horizontal padding | 16.dp | Outer padding |
| Inner padding | 8.dp | Content padding |
| Text size | 16sp | |
| Icon size | 24dp | Send button |

### Usage Example

```kotlin
var inputText by remember { mutableStateOf("") }
var isKeyboardOpen by remember { mutableStateOf(false) }

Y2kInputArea(
    text = inputText,
    onInputClick = { isKeyboardOpen = !isKeyboardOpen },
    onSend = {
        if (inputText.isNotBlank()) {
            sendMessage(inputText)
            inputText = ""
        }
    }
)
```

---

## 5. AikoCustomKeyboard Component

### Purpose
Custom Y2K-styled keyboard for text input.

### Visual Specification

```
┌─────────────────────────────────────────────┐
│  Q  W  E  R  T  Y  U  I  O  P              │
│    A  S  D  F  G  H  J  K  L               │
│     Z  X  C  V  B  N  M  ⌫                 │
└─────────────────────────────────────────────┘
  ↑ KeyboardGray background (#D0D0D0)
```

### Implementation

```kotlin
@Composable
fun AikoCustomKeyboard(
    onKeyClick: (String) -> Unit,
    onDelete: () -> Unit
) {
    val keys = listOf(
        "Q", "W", "E", "R", "T", "Y", "U", "I", "O", "P",
        "A", "S", "D", "F", "G", "H", "J", "K", "L",
        "Z", "X", "C", "V", "B", "N", "M", "⌫"
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(250.dp),
        color = Color(0xFFD0D0D0),
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 35.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(keys) { key ->
                    KeyboardKey(
                        text = key,
                        onClick = {
                            if (key == "⌫") onDelete() else onKeyClick(key)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun KeyboardKey(
    text: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .height(45.dp)
            .clickable { onClick() },
        color = Color.White,
        shape = RoundedCornerShape(4.dp),
        shadowElevation = 2.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = text,
                color = Color.Black,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
```

### Properties Table

| Property | Value | Notes |
|----------|-------|-------|
| Total height | 250dp | Fixed |
| Background | #D0D0D0 | KeyboardGray |
| Top corners | 16dp | Rounded |
| Key height | 45dp | Fixed |
| Key bg | White | |
| Key corners | 4dp | Rounded |
| Key shadow | 2dp | |
| Key spacing | 4dp horizontal, 8dp vertical | |
| Key text | 18sp, Medium | Black |

### Key Layout

| Row | Keys |
|-----|------|
| 1 | Q W E R T Y U I O P |
| 2 | A S D F G H J K L |
| 3 | Z X C V B N M ⌫ |

### Usage Example

```kotlin
var inputText by remember { mutableStateOf("") }

if (isKeyboardOpen) {
    AikoCustomKeyboard(
        onKeyClick = { inputText += it },
        onDelete = { inputText = inputText.dropLast(1) }
    )
}
```

---

## 6. StatRow Component

### Purpose
Display stat labels with icon ratings (LUV, NRG).

### Visual Specification

```
LUV  ♥ ♥ ♥ ♥ ♥
NRG  ✦ ✦  ✦ ✦
```

### Implementation

```kotlin
@Composable
fun StatRow(label: String, iconUrl: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = DarkPurple,
            modifier = Modifier.width(35.dp)
        )
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            repeat(5) {
                Icon(
                    painter = painterResource(R.drawable.heart_fill_ico),
                    contentDescription = null,
                    tint = DarkPurple,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}
```

### Properties Table

| Property | Value | Notes |
|----------|-------|-------|
| Label width | 35.dp | Fixed |
| Label size | 12.sp | DarkPurple |
| Icon count | 5 | Fixed |
| Icon size | 14.dp | |
| Icon spacing | 2.dp | |

### Usage Example

```kotlin
XpWindow(title = "身体の状態") {
    Column(
        modifier = Modifier.padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        StatRow("LUV", R.drawable.heart_fill_ico)
        StatRow("NRG", R.drawable.spark_fill_ico)
        StatRow("MOOD", R.drawable.moon_fill_ico)
    }
}
```

---

## Component Spacing Guide

### Screen Margins

| Element | Value |
|---------|-------|
| Screen edge padding | 16.dp |
| Between windows | 16.dp |
| Between chat bubbles | 12.dp |
| Top control area height | 140.dp |

### Internal Spacing

| Component | Padding |
|-----------|---------|
| XpWindow title | 8.dp horizontal |
| XpWindow content | Varies |
| ChatBubble | 12.dp |
| Y2kInputArea | 8.dp horizontal inner |
| Keyboard | 8.dp outer |

---

## Component State Management

### Interactive States

| Component | States |
|-----------|--------|
| Y2kButton | Default, Pressed |
| ChatBubble | Static |
| Y2kInputArea | Default, Focused (keyboard open) |
| AikoCustomKeyboard | Hidden, Visible |
| XpWindow | Static (content varies) |

### Animation Triggers

| Component | Animation | Trigger |
|-----------|-----------|---------|
| Y2kInputArea | Keyboard slide | Click |
| AikoCustomKeyboard | Slide up/down | Toggle |
| ChatBubble | Scroll to bottom | New message |

---

## Accessibility Considerations

### Content Descriptions

```kotlin
// Always add content descriptions for images
AsyncImage(
    model = R.drawable.e_girl_pp,
    contentDescription = "Aiko profile picture",
    // ...
)

// Icon buttons need descriptions
IconButton(onClick = onSend) {
    Icon(
        painter = painterResource(R.drawable.send_ico),
        contentDescription = "Send message",
        // ...
    )
}
```

### Touch Targets

- Minimum touch target: 48.dp × 48.dp
- Keyboard keys: 35.dp minimum width
- Buttons: Minimum 60.dp height recommended

---

*Last updated: 2026*
*Version: 1.0*
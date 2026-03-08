package com.methil.aiko.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import com.methil.aiko.R

sealed class KeyboardKeyAction {
    data class Type(val char: String) : KeyboardKeyAction()
    object Delete : KeyboardKeyAction()
    object Enter : KeyboardKeyAction()
    object Shift : KeyboardKeyAction()
    object Space : KeyboardKeyAction()
    object Numbers : KeyboardKeyAction()
}

data class KeyboardKeyData(
    val drawableRes: Int,
    val action: KeyboardKeyAction,
    val flex: Float = 1f,
    val height: Dp = 30.dp
)

@Preview(showBackground = true)
@Composable
fun AikoCustomKeyboardPreview() {
    AikoCustomKeyboard(onKeyClick = {}, onDelete = {}, onEnter = {})
}

@Composable
fun AikoCustomKeyboard(
    onKeyClick: (String) -> Unit,
    onDelete: () -> Unit,
    onEnter: () -> Unit
) {
    val row1 = listOf(
        KeyboardKeyData(R.drawable.kb_q, KeyboardKeyAction.Type("q")),
        KeyboardKeyData(R.drawable.kb_w, KeyboardKeyAction.Type("w")),
        KeyboardKeyData(R.drawable.kb_e, KeyboardKeyAction.Type("e")),
        KeyboardKeyData(R.drawable.kb_r, KeyboardKeyAction.Type("r")),
        KeyboardKeyData(R.drawable.kb_t, KeyboardKeyAction.Type("t")),
        KeyboardKeyData(R.drawable.kb_y, KeyboardKeyAction.Type("y")),
        KeyboardKeyData(R.drawable.kb_u, KeyboardKeyAction.Type("u")),
        KeyboardKeyData(R.drawable.kb_i, KeyboardKeyAction.Type("i")),
        KeyboardKeyData(R.drawable.kb_o, KeyboardKeyAction.Type("o")),
        KeyboardKeyData(R.drawable.kb_p, KeyboardKeyAction.Type("p"))
    )

    val row2 = listOf(
        KeyboardKeyData(R.drawable.kb_a, KeyboardKeyAction.Type("a")),
        KeyboardKeyData(R.drawable.kb_s, KeyboardKeyAction.Type("s")),
        KeyboardKeyData(R.drawable.kb_d, KeyboardKeyAction.Type("d")),
        KeyboardKeyData(R.drawable.kb_f, KeyboardKeyAction.Type("f")),
        KeyboardKeyData(R.drawable.kb_g, KeyboardKeyAction.Type("g")),
        KeyboardKeyData(R.drawable.kb_h, KeyboardKeyAction.Type("h")),
        KeyboardKeyData(R.drawable.kb_j, KeyboardKeyAction.Type("j")),
        KeyboardKeyData(R.drawable.kb_k, KeyboardKeyAction.Type("k")),
        KeyboardKeyData(R.drawable.kb_l, KeyboardKeyAction.Type("l"))
    )

    val row3 = listOf(
        KeyboardKeyData(R.drawable.kb_maj, KeyboardKeyAction.Shift, flex = 1.5f, height = 42.dp),
        KeyboardKeyData(R.drawable.kb_z, KeyboardKeyAction.Type("z")),
        KeyboardKeyData(R.drawable.kb_x, KeyboardKeyAction.Type("x")),
        KeyboardKeyData(R.drawable.kb_c, KeyboardKeyAction.Type("c")),
        KeyboardKeyData(R.drawable.kb_v, KeyboardKeyAction.Type("v")),
        KeyboardKeyData(R.drawable.kb_b, KeyboardKeyAction.Type("b")),
        KeyboardKeyData(R.drawable.kb_n, KeyboardKeyAction.Type("n")),
        KeyboardKeyData(R.drawable.kb_m, KeyboardKeyAction.Type("m")),
        KeyboardKeyData(R.drawable.kb_erase, KeyboardKeyAction.Delete, flex = 1.5f, height = 42.dp)
    )

    val row4 = listOf(
        KeyboardKeyData(R.drawable.kb_number, KeyboardKeyAction.Numbers, flex = 1.5f, height = 44.dp),
        KeyboardKeyData(R.drawable.kb_comma, KeyboardKeyAction.Type(","), height = 44.dp),
        KeyboardKeyData(R.drawable.kb_space, KeyboardKeyAction.Space, flex = 4f, height = 42.dp),
        KeyboardKeyData(R.drawable.kb_point, KeyboardKeyAction.Type("."), height = 44.dp),
        KeyboardKeyData(R.drawable.kb_enter, KeyboardKeyAction.Enter, flex = 1.5f, height = 44.dp)
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        color = Color(0xFFE6D7FF), // Matching the light violet in the screenshot
        shape = RoundedCornerShape(topStart = 0.dp, topEnd = 0.dp),
        border = BorderStroke(2.dp, Color(0xFFE0B8FF))
    ) {
        Column(
            modifier = Modifier
                .padding(8.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            KeyboardRow(row1, onKeyClick, onDelete, onEnter)
            KeyboardRow(row2, onKeyClick, onDelete, onEnter)
            KeyboardRow(row3, onKeyClick, onDelete, onEnter)
            KeyboardRow(row4, onKeyClick, onDelete, onEnter)
        }
    }
}

@Composable
fun KeyboardRow(
    keys: List<KeyboardKeyData>,
    onKeyClick: (String) -> Unit,
    onDelete: () -> Unit,
    onEnter: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        keys.forEach { key ->
            KeyboardKey(
                key = key,
                modifier = Modifier.weight(key.flex),
                onClick = {
                    when (key.action) {
                        is KeyboardKeyAction.Type -> onKeyClick(key.action.char)
                        KeyboardKeyAction.Delete -> onDelete()
                        KeyboardKeyAction.Space -> onKeyClick(" ")
                        KeyboardKeyAction.Enter -> onEnter()
                        else -> { /* Handle Shift/Numbers if needed */ }
                    }
                }
            )
        }
    }
}

@Composable
fun KeyboardKey(
    key: KeyboardKeyData,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val interactionSource = androidx.compose.runtime.remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val density = androidx.compose.ui.platform.LocalDensity.current

    var showPopup by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }

    androidx.compose.runtime.LaunchedEffect(isPressed) {
        if (isPressed) {
            showPopup = true
        } else if (showPopup) {
            kotlinx.coroutines.delay(150)
            showPopup = false
        }
    }

    Box(
        modifier = modifier
            .height(key.height)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.TopCenter
    ) {
        Image(
            painter = painterResource(id = key.drawableRes),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.FillBounds
        )

        if (showPopup && key.action is KeyboardKeyAction.Type) {
            val offsetY = with(density) { (-60).dp.roundToPx() }
            androidx.compose.ui.window.Popup(
                alignment = Alignment.TopCenter,
                offset = androidx.compose.ui.unit.IntOffset(0, offsetY),
                properties = androidx.compose.ui.window.PopupProperties(clippingEnabled = false)
            ) {
                Box(
                    modifier = Modifier
                        .width(48.dp)
                        .height(58.dp)
                        .background(
                            Color(0xFFF3EDFF),
                            RoundedCornerShape(10.dp)
                        )
                        .border(
                            1.5.dp,
                            Color(0xFFCBA4F5),
                            RoundedCornerShape(10.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.material3.Text(
                        text = key.action.char.uppercase(),
                        fontSize = 24.sp,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                        color = Color(0xFF2D2D2D)
                    )
                }
            }
        }
    }
}

package com.samsung.android.health.sdk.sample.healthdiary.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
fun SandboxTabRow(
    selectedTabIndex: Int,
    tabs: List<String>,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (tabs.isEmpty()) return

    val safeSelectedIndex = selectedTabIndex.coerceIn(0, tabs.lastIndex)

    ScrollableTabRow(
        selectedTabIndex = safeSelectedIndex,
        modifier = modifier.fillMaxWidth(),

        // Fondo neutro para evitar “todo azul”
        containerColor = Color.White,
        contentColor = Color.Black,
        indicator = { tabPositions ->
            val current = tabPositions.getOrNull(safeSelectedIndex) ?: return@ScrollableTabRow
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(Alignment.Bottom)
            ) {
                Box(
                    modifier = Modifier
                        .width(current.width)
                        .offset(x = current.left)
                        .height(2.dp)
                        .background(Color(0xFF007AFF))
                )
            }
        },
        divider = {}
    ) {
        tabs.forEachIndexed { index, title ->
            Tab(
                selected = safeSelectedIndex == index,
                onClick = { onTabSelected(index) },
                selectedContentColor = Color.Black,
                unselectedContentColor = Color.Black,
                text = {
                    Text(
                        text = title,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            )
        }
    }
}

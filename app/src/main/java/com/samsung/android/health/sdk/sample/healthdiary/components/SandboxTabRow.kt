package com.samsung.android.health.sdk.sample.healthdiary.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.samsung.android.health.sdk.sample.healthdiary.ui.theme.SandboxTheme

/**
 * Sandbox Tab Row Component
 * 
 * A reusable tab row component following Sandbox design system.
 * Wrapper around Material3 ScrollableTabRow with consistent styling.
 * 
 * @param selectedTabIndex Currently selected tab index
 * @param tabs List of tab labels
 * @param onTabSelected Callback when tab is selected
 * @param modifier Modifier for styling
 */
@Composable
fun SandboxTabRow(
    selectedTabIndex: Int,
    tabs: List<String>,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    ScrollableTabRow(
        selectedTabIndex = selectedTabIndex,
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.primary,
        indicator = { tabPositions ->
            val currentTabPosition = tabPositions[selectedTabIndex]
            TabRowDefaults.Indicator(
                modifier = Modifier
                    .width(currentTabPosition.width)
                    .offset(x = currentTabPosition.left),
                color = MaterialTheme.colorScheme.primary
            )
        }
    ) {
        tabs.forEachIndexed { index, title ->
            Tab(
                selected = selectedTabIndex == index,
                onClick = { onTabSelected(index) },
                text = { Text(title) }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SandboxTabRowPreview() {
    SandboxTheme {
        var selectedTab by remember { mutableStateOf(0) }
        SandboxTabRow(
            selectedTabIndex = selectedTab,
            tabs = listOf("Tab 1", "Tab 2", "Tab 3", "Tab 4"),
            onTabSelected = { selectedTab = it }
        )
    }
}

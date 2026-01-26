package com.samsung.android.health.sdk.sample.healthdiary.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import com.samsung.android.health.sdk.sample.healthdiary.ui.theme.SandboxTheme

/**
 * Sandbox Top Bar Component
 * 
 * A reusable top app bar component following Sandbox design system.
 * 
 * @param title The bar title
 * @param modifier Modifier for styling
 * @param navigationIcon Optional navigation icon (typically back arrow)
 * @param onNavigationClick Callback when navigation icon is clicked
 * @param actions Optional action icons
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SandboxTopBar(
    title: String,
    modifier: Modifier = Modifier,
    navigationIcon: ImageVector? = Icons.Default.ArrowBack,
    onNavigationClick: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    TopAppBar(
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge
            )
        },
        navigationIcon = {
            if (navigationIcon != null && onNavigationClick != null) {
                IconButton(onClick = onNavigationClick) {
                    Icon(
                        imageVector = navigationIcon,
                        contentDescription = "Navigate back"
                    )
                }
            }
        },
        actions = actions,
        modifier = modifier,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            navigationIconContentColor = MaterialTheme.colorScheme.onSurface
        )
    )
}

@Preview(showBackground = true)
@Composable
private fun SandboxTopBarPreview() {
    SandboxTheme {
        Column {
            SandboxTopBar(
                title = "Screen Title",
                onNavigationClick = {}
            )
            SandboxTopBar(
                title = "No Navigation",
                navigationIcon = null,
                onNavigationClick = null
            )
        }
    }
}

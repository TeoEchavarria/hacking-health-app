package com.samsung.android.health.sdk.sample.healthdiary.views

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.samsung.android.health.sdk.sample.healthdiary.BuildConfig
import com.samsung.android.health.sdk.sample.healthdiary.components.CardElevation
import com.samsung.android.health.sdk.sample.healthdiary.components.*

/**
 * Sandbox Gallery Screen
 * 
 * Debug-only screen that showcases all Sandbox components and their variants.
 * Only available in debug builds.
 */
@Composable
fun SandboxGalleryScreen(
    onNavigateBack: () -> Unit
) {
    // Only show in debug builds
    if (!BuildConfig.DEBUG) {
        return
    }
    
    var selectedCategory by remember { mutableStateOf("Buttons") }
    val categories = listOf("Buttons", "Inputs", "Cards", "Navigation", "Feedback", "Other")
    
    Scaffold(
        topBar = {
            SandboxTopBar(
                title = "Sandbox Gallery",
                onNavigationClick = onNavigateBack
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            // Category Tabs
            SandboxTabRow(
                selectedTabIndex = categories.indexOf(selectedCategory),
                tabs = categories,
                onTabSelected = { index ->
                    selectedCategory = categories[index]
                },
                modifier = Modifier.fillMaxWidth()
            )
            
            // Content
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                when (selectedCategory) {
                    "Buttons" -> {
                        item { ButtonsSection() }
                    }
                    "Inputs" -> {
                        item { InputsSection() }
                    }
                    "Cards" -> {
                        item { CardsSection() }
                    }
                    "Navigation" -> {
                        item { NavigationSection() }
                    }
                    "Feedback" -> {
                        item { FeedbackSection() }
                    }
                    "Other" -> {
                        item { OtherSection() }
                    }
                }
            }
        }
    }
}

@Composable
private fun ButtonsSection() {
    SandboxSection(title = "Buttons") {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            SandboxButton(
                text = "Primary Button",
                onClick = {},
                fullWidth = true
            )
            SandboxButton(
                text = "Secondary Button",
                onClick = {},
                variant = ButtonVariant.Secondary,
                fullWidth = true
            )
            SandboxButton(
                text = "Text Button",
                onClick = {},
                variant = ButtonVariant.Text,
                fullWidth = true
            )
            SandboxButton(
                text = "With Icon",
                onClick = {},
                icon = Icons.Default.Add,
                fullWidth = true
            )
            SandboxButton(
                text = "Loading",
                onClick = {},
                isLoading = true,
                fullWidth = true
            )
            SandboxButton(
                text = "Disabled",
                onClick = {},
                enabled = false,
                fullWidth = true
            )
        }
    }
}

@Composable
private fun InputsSection() {
    var textValue by remember { mutableStateOf("") }
    var passwordValue by remember { mutableStateOf("") }
    
    SandboxSection(title = "Inputs") {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            SandboxInput(
                value = textValue,
                onValueChange = { textValue = it },
                label = "Text Input",
                placeholder = "Enter text"
            )
            SandboxInput(
                value = passwordValue,
                onValueChange = { passwordValue = it },
                label = "Password Input",
                placeholder = "Enter password",
                keyboardType = androidx.compose.ui.text.input.KeyboardType.Password
            )
            SandboxInput(
                value = "",
                onValueChange = {},
                label = "Error State",
                placeholder = "This has an error",
                isError = true,
                errorMessage = "This field is required"
            )
            SandboxInput(
                value = "",
                onValueChange = {},
                label = "Disabled",
                placeholder = "Disabled input",
                enabled = false
            )
        }
    }
}

@Composable
private fun CardsSection() {
    SandboxSection(title = "Cards") {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            SandboxCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Card with Low Elevation")
            }
            SandboxCard(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardElevation.Medium
            ) {
                Text("Card with Medium Elevation")
            }
            SandboxCard(
                modifier = Modifier.fillMaxWidth(),
                onClick = {},
                elevation = CardElevation.High
            ) {
                Text("Clickable Card")
            }
        }
    }
}

@Composable
private fun NavigationSection() {
    SandboxSection(title = "Navigation") {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            SandboxHeader(
                title = "Large Header",
                variant = HeaderVariant.Large
            )
            SandboxHeader(
                title = "Medium Header",
                variant = HeaderVariant.Medium
            )
            SandboxHeader(
                title = "Small Header",
                variant = HeaderVariant.Small
            )
            SandboxHeader(
                title = "Header with Subtitle",
                subtitle = "This is a subtitle"
            )
            
            Divider()
            
            SandboxListItem(
                title = "List Item",
                onClick = {}
            )
            SandboxListItem(
                title = "With Subtitle",
                subtitle = "Secondary text here",
                onClick = {}
            )
            SandboxListItem(
                title = "With Icon",
                subtitle = "Has a leading icon",
                onClick = {},
                leadingIcon = Icons.Default.Settings
            )
        }
    }
}

@Composable
private fun FeedbackSection() {
    SandboxSection(title = "Feedback") {
        Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
            // Badges
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SandboxBadge(text = "Primary", variant = BadgeVariant.Primary)
                SandboxBadge(text = "Secondary", variant = BadgeVariant.Secondary)
                SandboxBadge(text = "Error", variant = BadgeVariant.Error)
                SandboxBadge(text = "Success", variant = BadgeVariant.Success)
                SandboxBadge(text = "Neutral", variant = BadgeVariant.Neutral)
            }
            
            // Loaders
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                SandboxLoader(variant = LoaderVariant.Small)
                SandboxLoader(variant = LoaderVariant.Medium)
                SandboxLoader(variant = LoaderVariant.Large)
            }
            
            SandboxLoader(
                variant = LoaderVariant.Medium,
                message = "Loading..."
            )
            
            // Empty State
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            ) {
                SandboxEmptyState(
                    title = "No Data Available",
                    message = "There's nothing to display here yet.",
                    icon = Icons.Default.Info
                )
            }
            
            // Toggle
            SandboxToggle(
                checked = true,
                onCheckedChange = {},
                label = "Toggle with Label"
            )
            SandboxToggle(
                checked = false,
                onCheckedChange = {},
                label = "Unchecked Toggle"
            )
        }
    }
}

@Composable
private fun OtherSection() {
    SandboxSection(title = "Other Components") {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            // Icon Buttons
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                SandboxIconButton(
                    icon = Icons.Default.Settings,
                    onClick = {},
                    variant = IconButtonVariant.Standard
                )
                SandboxIconButton(
                    icon = Icons.Default.Settings,
                    onClick = {},
                    variant = IconButtonVariant.Filled
                )
                SandboxIconButton(
                    icon = Icons.Default.Settings,
                    onClick = {},
                    variant = IconButtonVariant.Outlined
                )
            }
            
            // Chat Bubbles
            SandboxChatBubble(
                text = "This is a user message",
                isUser = true,
                timestamp = "10:30 AM"
            )
            SandboxChatBubble(
                text = "This is an assistant message with longer text",
                isUser = false,
                timestamp = "10:31 AM"
            )
        }
    }
}

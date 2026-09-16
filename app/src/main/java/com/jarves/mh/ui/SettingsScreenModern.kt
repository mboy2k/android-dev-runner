package com.jarves.mh.ui

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jarves.mh.data.AppPreferences
import com.jarves.mh.model.*
import com.jarves.mh.ui.theme.AppThemeMode
import com.jarves.mh.ui.theme.PocketOrange
import kotlinx.coroutines.launch

private enum class ZCodeSettingsTab { MODELS, BRAIN, SKILLS, SUBAGENTS, SYSTEM }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    state: AppUiState,
    onSaveProvider: (ProviderProfile, String) -> Unit,
    onDiscoverModels: suspend (ProviderProfile, String) -> com.jarves.mh.network.ModelDiscoveryResult,
    onValidateProvider: suspend (ProviderProfile, String, List<com.jarves.mh.network.DiscoveredModel>) -> com.jarves.mh.network.ConnectionValidation,
    onSetThemeMode: (AppThemeMode) -> Unit,
    onPing: () -> Unit,
    onClearTerminal: () -> Unit,
    getSavedApiKey: (ProviderKind) -> String,
    onInstallDevStack: (DevStack) -> Unit = {},
    initialDebugUpdateManifestUrl: String = "",
    onSetDebugUpdateManifestUrl: (String) -> Unit = {},
    onClearDebugUpdateManifestUrl: () -> Unit = {},
    onSaveCustomProvider: (CustomProviderConfig) -> Unit = {},
    onDeleteCustomProvider: (String) -> Unit = {},
    onSelectCustomModel: (CustomProviderConfig, CustomModelItem) -> Unit = { _, _ -> },
    onSaveSystemPrompt: (String) -> Unit = {},
    onBack: () -> Unit = {},
) {
    val context = LocalContext.current
    val prefs = remember { AppPreferences(context) }
    var activeTab by rememberSaveable { mutableStateOf(ZCodeSettingsTab.MODELS) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = when (activeTab) {
                                ZCodeSettingsTab.MODELS -> "Model settings"
                                ZCodeSettingsTab.BRAIN -> "AI Brain Settings"
                                ZCodeSettingsTab.SKILLS -> "Skills"
                                ZCodeSettingsTab.SUBAGENTS -> "Subagents"
                                ZCodeSettingsTab.SYSTEM -> "System Settings"
                            },
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onPing) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
            ) {
                NavigationBarItem(
                    selected = activeTab == ZCodeSettingsTab.MODELS,
                    onClick = { activeTab = ZCodeSettingsTab.MODELS },
                    icon = { Icon(Icons.Default.Dns, contentDescription = null) },
                    label = { Text("Models", fontSize = 11.sp) }
                )
                NavigationBarItem(
                    selected = activeTab == ZCodeSettingsTab.BRAIN,
                    onClick = { activeTab = ZCodeSettingsTab.BRAIN },
                    icon = { Icon(Icons.Default.Memory, contentDescription = null) },
                    label = { Text("Bộ Não", fontSize = 11.sp) }
                )
                NavigationBarItem(
                    selected = activeTab == ZCodeSettingsTab.SKILLS,
                    onClick = { activeTab = ZCodeSettingsTab.SKILLS },
                    icon = { Icon(Icons.Default.Code, contentDescription = null) },
                    label = { Text("Skills", fontSize = 11.sp) }
                )
                NavigationBarItem(
                    selected = activeTab == ZCodeSettingsTab.SUBAGENTS,
                    onClick = { activeTab = ZCodeSettingsTab.SUBAGENTS },
                    icon = { Icon(Icons.Default.SmartToy, contentDescription = null) },
                    label = { Text("Subagents", fontSize = 11.sp) }
                )
                NavigationBarItem(
                    selected = activeTab == ZCodeSettingsTab.SYSTEM,
                    onClick = { activeTab = ZCodeSettingsTab.SYSTEM },
                    icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                    label = { Text("Hệ thống", fontSize = 11.sp) }
                )
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            when (activeTab) {
                ZCodeSettingsTab.MODELS -> UnifiedModelSettings(
                    prefs = prefs,
                    onSaveCustomProvider = onSaveCustomProvider,
                    onDeleteCustomProvider = onDeleteCustomProvider,
                    onSelectCustomModel = onSelectCustomModel,
                )
                ZCodeSettingsTab.BRAIN -> BrainSettingsView(
                    prefs = prefs,
                    onSaveSystemPrompt = onSaveSystemPrompt,
                )
                ZCodeSettingsTab.SKILLS -> SkillsManagementView(prefs = prefs)
                ZCodeSettingsTab.SUBAGENTS -> SubagentsManagementView(prefs = prefs)
                ZCodeSettingsTab.SYSTEM -> SystemSettingsView(
                    state = state,
                    onSetThemeMode = onSetThemeMode,
                    onClearTerminal = onClearTerminal,
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------------
// 1. UNIFIED MODEL SETTINGS (ZCODE STANDARD - PHOTO 1: media_1789585470926.png)
// ---------------------------------------------------------------------------------
@Composable
private fun UnifiedModelSettings(
    prefs: AppPreferences,
    onSaveCustomProvider: (CustomProviderConfig) -> Unit,
    onDeleteCustomProvider: (String) -> Unit,
    onSelectCustomModel: (CustomProviderConfig, CustomModelItem) -> Unit,
) {
    var providers by remember { mutableStateOf(prefs.loadCustomProviders()) }
    var selectedProviderId by remember {
        mutableStateOf(providers.firstOrNull()?.id ?: "hy4-default")
    }
    val selectedProvider = providers.firstOrNull { it.id == selectedProviderId } ?: providers.firstOrNull()

    var showAddProviderDialog by remember { mutableStateOf(false) }
    var showAddModelDialog by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        Text(
            text = "Manage custom model providers. Once configured, they can be selected during chat.",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

        Row(Modifier.fillMaxSize()) {
            // LEFT COLUMN: Provider list (40% width)
            Column(
                modifier = Modifier
                    .weight(0.38f)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f))
                    .padding(8.dp)
            ) {
                Text(
                    "Providers",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )

                // Built-in Z.ai row
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color.Transparent,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFF1E293B),
                            modifier = Modifier.size(18.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("Z", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        Spacer(Modifier.width(6.dp))
                        Text("Z.ai", fontSize = 12.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                        Box(Modifier.size(6.dp).background(Color.Gray, CircleShape))
                    }
                }

                Spacer(Modifier.height(8.dp))
                Text(
                    "Custom providers",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )

                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(providers, key = { it.id }) { prov ->
                        val isSelected = prov.id == selectedProvider?.id
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Transparent,
                            border = if (isSelected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)) else null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp)
                                .clickable { selectedProviderId = prov.id }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ViewInAr,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = prov.name.ifBlank { "Untitled" },
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .background(if (prov.isEnabled) Color(0xFF10B981) else Color.Gray, CircleShape)
                                )
                            }
                        }
                    }
                }

                TextButton(
                    onClick = { showAddProviderDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("+ Add provider", fontSize = 12.sp)
                }
            }

            VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            // RIGHT COLUMN: Provider Configuration Details (62% width)
            if (selectedProvider != null) {
                var editName by remember(selectedProvider.id) { mutableStateOf(selectedProvider.name) }
                var editUrl by remember(selectedProvider.id) { mutableStateOf(selectedProvider.baseUrl) }
                var editKey by remember(selectedProvider.id) { mutableStateOf(selectedProvider.apiKey) }
                var editFormat by remember(selectedProvider.id) { mutableStateOf(selectedProvider.apiFormat) }
                var keyVisible by rememberSaveable { mutableStateOf(false) }

                fun persistCurrentProvider(modelsList: List<CustomModelItem> = selectedProvider.models, enabled: Boolean = selectedProvider.isEnabled) {
                    val updated = selectedProvider.copy(
                        name = editName.trim(),
                        baseUrl = editUrl.trim(),
                        apiKey = editKey.trim(),
                        apiFormat = editFormat,
                        models = modelsList,
                        isEnabled = enabled
                    )
                    val newProvList = providers.map { if (it.id == updated.id) updated else it }
                    providers = newProvList
                    prefs.saveCustomProviders(newProvList)
                    onSaveCustomProvider(updated)
                }

                LazyColumn(
                    modifier = Modifier
                        .weight(0.62f)
                        .fillMaxHeight()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        // Title bar with Rename, Status, and Delete
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = selectedProvider.name.ifBlank { "Provider" },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (selectedProvider.isEnabled) Color(0xFF10B981) else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.clickable {
                                    persistCurrentProvider(enabled = !selectedProvider.isEnabled)
                                }
                            ) {
                                Text(
                                    text = if (selectedProvider.isEnabled) "Enabled" else "Disable",
                                    color = if (selectedProvider.isEnabled) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                            Spacer(Modifier.width(6.dp))
                            IconButton(
                                onClick = {
                                    val remaining = providers.filterNot { it.id == selectedProvider.id }
                                    providers = remaining
                                    prefs.saveCustomProviders(remaining)
                                    onDeleteCustomProvider(selectedProvider.id)
                                    selectedProviderId = remaining.firstOrNull()?.id ?: ""
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                            }
                        }
                    }

                    item {
                        // Base URL input
                        Text("Base URL", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        OutlinedTextField(
                            value = editUrl,
                            onValueChange = { editUrl = it; persistCurrentProvider() },
                            placeholder = { Text("http://138.2.95.239:8787/v1", fontSize = 12.sp) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    item {
                        // API format dropdown
                        Text("API format", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        var formatExpanded by remember { mutableStateOf(false) }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier.fillMaxWidth().clickable { formatExpanded = true }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (editFormat == "ANTHROPIC_MESSAGES") "Anthropic messages (/v1/messages)" else "Chat completions (/chat/completions)",
                                    fontSize = 12.sp,
                                    modifier = Modifier.weight(1f)
                                )
                                Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                            DropdownMenu(expanded = formatExpanded, onDismissRequest = { formatExpanded = false }) {
                                DropdownMenuItem(
                                    text = { Text("Chat completions (/chat/completions)", fontSize = 12.sp) },
                                    onClick = {
                                        editFormat = "OPENAI_CHAT"
                                        formatExpanded = false
                                        persistCurrentProvider()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Anthropic messages (/v1/messages)", fontSize = 12.sp) },
                                    onClick = {
                                        editFormat = "ANTHROPIC_MESSAGES"
                                        formatExpanded = false
                                        persistCurrentProvider()
                                    }
                                )
                            }
                        }
                    }

                    item {
                        // API key input with eye toggle
                        Text("API key", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        OutlinedTextField(
                            value = editKey,
                            onValueChange = { editKey = it; persistCurrentProvider() },
                            visualTransformation = if (keyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { keyVisible = !keyVisible }) {
                                    Icon(if (keyVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(16.dp))
                                }
                            },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    item {
                        // Model list header
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Model list", fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        }
                    }

                    // Models items
                    items(selectedProvider.models, key = { it.id }) { modelItem ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = modelItem.id,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.weight(1f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (modelItem.supportsImage) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        modifier = Modifier.padding(horizontal = 4.dp)
                                    ) {
                                        Text("Vision", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp))
                                    }
                                }
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier.padding(horizontal = 4.dp)
                                ) {
                                    Text("1M", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp))
                                }
                                IconButton(
                                    onClick = {
                                        val newModels = selectedProvider.models.filterNot { it.id == modelItem.id }
                                        persistCurrentProvider(modelsList = newModels)
                                    },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Default.DeleteOutline, contentDescription = "Remove", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(15.dp))
                                }
                            }
                        }
                    }

                    item {
                        Button(
                            onClick = { showAddModelDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("+ Add model", color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp)
                        }
                    }
                }

                // Add Model Dialog
                if (showAddModelDialog) {
                    var newModelName by remember { mutableStateOf("") }
                    var hasVision by remember { mutableStateOf(false) }
                    AlertDialog(
                        onDismissRequest = { showAddModelDialog = false },
                        title = { Text("Add Model") },
                        text = {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = newModelName,
                                    onValueChange = { newModelName = it },
                                    placeholder = { Text("deepseek-v4.1-flash") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(checked = hasVision, onCheckedChange = { hasVision = it })
                                    Text("Supports Vision (Images)", fontSize = 12.sp)
                                }
                            }
                        },
                        confirmButton = {
                            TextButton(onClick = {
                                if (newModelName.isNotBlank()) {
                                    val updatedList = selectedProvider.models + CustomModelItem(id = newModelName.trim(), supportsImage = hasVision)
                                    persistCurrentProvider(modelsList = updatedList)
                                }
                                showAddModelDialog = false
                            }) {
                                Text("Add")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showAddModelDialog = false }) { Text("Cancel") }
                        }
                    )
                }
            }
        }
    }

    // Add Provider Dialog
    if (showAddProviderDialog) {
        var provName by remember { mutableStateOf("") }
        var provUrl by remember { mutableStateOf("http://138.2.95.239:8787/v1") }
        var provKey by remember { mutableStateOf("nTNuTJ6W9pKxR3qVhCmD2sLbAwYeF4gT") }
        var provModel by remember { mutableStateOf("deepseek-v4.1-flash") }

        AlertDialog(
            onDismissRequest = { showAddProviderDialog = false },
            title = { Text("Add Custom Provider") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = provName,
                        onValueChange = { provName = it },
                        label = { Text("Provider Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = provUrl,
                        onValueChange = { provUrl = it },
                        label = { Text("Base URL") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = provKey,
                        onValueChange = { provKey = it },
                        label = { Text("API Key") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = provModel,
                        onValueChange = { provModel = it },
                        label = { Text("Default Model") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (provName.isNotBlank()) {
                        val newProv = CustomProviderConfig(
                            name = provName.trim(),
                            baseUrl = provUrl.trim(),
                            apiKey = provKey.trim(),
                            models = listOf(CustomModelItem(id = provModel.trim())),
                            isEnabled = true
                        )
                        val updated = providers + newProv
                        providers = updated
                        prefs.saveCustomProviders(updated)
                        onSaveCustomProvider(newProv)
                        selectedProviderId = newProv.id
                    }
                    showAddProviderDialog = false
                }) {
                    Text("Save Provider")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddProviderDialog = false }) { Text("Cancel") }
            }
        )
    }
}

// ---------------------------------------------------------------------------------
// 2. AI BRAIN SETTINGS (CONSTITUTION & SYSTEM PROMPT SEPARATE TAB)
// ---------------------------------------------------------------------------------
@Composable
private fun BrainSettingsView(
    prefs: AppPreferences,
    onSaveSystemPrompt: (String) -> Unit,
) {
    var brainText by remember { mutableStateOf(prefs.brainConstitution) }
    var isSaved by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Memory, contentDescription = null, tint = PocketOrange, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text("AI Brain Constitution", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("Nội dung được nạp trực tiếp vào /root/.claude/CLAUDE.md", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            TextButton(
                onClick = {
                    brainText = prefs.defaultBrainConstitution
                    prefs.brainConstitution = prefs.defaultBrainConstitution
                    onSaveSystemPrompt(prefs.defaultBrainConstitution)
                    isSaved = true
                }
            ) {
                Text("Reset", color = MaterialTheme.colorScheme.error)
            }
        }

        OutlinedTextField(
            value = brainText,
            onValueChange = { brainText = it; isSaved = false },
            modifier = Modifier.fillMaxWidth().weight(1f),
            textStyle = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isSaved) "✓ Đã lưu vào bộ não AI thành công!" else "",
                color = Color(0xFF10B981),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
            Button(
                onClick = {
                    prefs.brainConstitution = brainText
                    onSaveSystemPrompt(brainText)
                    isSaved = true
                },
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Lưu bộ não (Save Brain)")
            }
        }
    }
}

// ---------------------------------------------------------------------------------
// 3. SKILLS MANAGEMENT VIEW (PHOTO 2: media_1789586275844.png)
// ---------------------------------------------------------------------------------
@Composable
private fun SkillsManagementView(prefs: AppPreferences) {
    var skills by remember { mutableStateOf(prefs.loadSkills()) }
    var searchQuery by remember { mutableStateOf("") }
    val filtered = remember(skills, searchQuery) {
        val q = searchQuery.trim().lowercase()
        if (q.isBlank()) skills else skills.filter { it.name.lowercase().contains(q) || it.description.lowercase().contains(q) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search skills...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Installed ${skills.size}", fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Button(
                onClick = { /* Add skill */ },
                colors = ButtonDefaults.buttonColors(containerColor = Color.Black),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("+ New", color = Color.White, fontSize = 12.sp)
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filtered, key = { it.id }) { skill ->
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(skill.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text(
                                skill.description,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Switch(
                            checked = skill.isEnabled,
                            onCheckedChange = { checked ->
                                val updated = skills.map { if (it.id == skill.id) it.copy(isEnabled = checked) else it }
                                skills = updated
                                prefs.saveSkills(updated)
                            }
                        )
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------------
// 4. SUBAGENTS MANAGEMENT VIEW (PHOTO 3: media_1789586277732.png)
// ---------------------------------------------------------------------------------
@Composable
private fun SubagentsManagementView(prefs: AppPreferences) {
    var subagents by remember { mutableStateOf(prefs.loadSubagents()) }
    var searchQuery by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search subagents...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Installed 0 items", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Button(
                onClick = {},
                colors = ButtonDefaults.buttonColors(containerColor = Color.Black),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("+ New", color = Color.White, fontSize = 12.sp)
            }
        }

        // Empty installed container (Dashed border style)
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text("No subagents found", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text(
                    "Fill in the subagent name, tools, and system prompt, then save to return to the list.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(Modifier.height(6.dp))
                Button(
                    onClick = {},
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Black),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("+ New", color = Color.White, fontSize = 12.sp)
                }
            }
        }

        Text("Built-in subagents 2 items", fontWeight = FontWeight.Bold, fontSize = 13.sp)

        // Built-in list
        subagents.filter { it.isBuiltIn }.forEach { agent ->
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF38BDF8).copy(alpha = 0.2f),
                        modifier = Modifier.size(32.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.SmartToy, contentDescription = null, tint = Color(0xFF0284C7), modifier = Modifier.size(16.dp))
                        }
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(agent.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Spacer(Modifier.width(6.dp))
                            Surface(shape = RoundedCornerShape(4.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                                Text(agent.toolsSummary, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                            }
                        }
                        Text(agent.description, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        color = Color.Transparent
                    ) {
                        Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("Inherit default", fontSize = 11.sp)
                            Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------------
// 5. SYSTEM SETTINGS VIEW (APPEARANCE & /sdcard STORAGE ACCESS)
// ---------------------------------------------------------------------------------
@Composable
private fun SystemSettingsView(
    state: AppUiState,
    onSetThemeMode: (AppThemeMode) -> Unit,
    onClearTerminal: () -> Unit,
) {
    val context = LocalContext.current
    var hasStoragePermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                Environment.isExternalStorageManager()
            } else true
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text("Giao diện (Theme)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = { onSetThemeMode(AppThemeMode.DARK) },
                    modifier = Modifier.weight(1f),
                    colors = if (state.themeMode == AppThemeMode.DARK) ButtonDefaults.outlinedButtonColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)) else ButtonDefaults.outlinedButtonColors()
                ) {
                    Text("Tối (Dark)")
                }
                OutlinedButton(
                    onClick = { onSetThemeMode(AppThemeMode.LIGHT) },
                    modifier = Modifier.weight(1f),
                    colors = if (state.themeMode == AppThemeMode.LIGHT) ButtonDefaults.outlinedButtonColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)) else ButtonDefaults.outlinedButtonColors()
                ) {
                    Text("Sáng (Light)")
                }
            }
        }

        item {
            HorizontalDivider()
            Text("Quyền truy cập bộ nhớ điện thoại (/sdcard)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text(
                "Cho phép AI và Terminal đọc, ghi, sửa file trực tiếp ở mọi thư mục trên máy (Download, Documents...)",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 4.dp)
            )

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (hasStoragePermission) Color(0xFF10B981).copy(alpha = 0.15f) else PocketOrange.copy(alpha = 0.15f),
                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)
            ) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (hasStoragePermission) Icons.Default.CheckCircle else Icons.Default.Warning,
                        contentDescription = null,
                        tint = if (hasStoragePermission) Color(0xFF10B981) else PocketOrange
                    )
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(if (hasStoragePermission) "Đã cấp quyền All Files Access" else "Chưa cấp quyền quản lý tất cả tệp", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("/storage/emulated/0 đã được gắn vào Linux PRoot", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (!hasStoragePermission) {
                        Button(
                            onClick = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                    runCatching {
                                        val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                                            data = Uri.parse("package:${context.packageName}")
                                        }
                                        context.startActivity(intent)
                                    }
                                }
                            }
                        ) {
                            Text("Cấp quyền")
                        }
                    }
                }
            }
        }

        item {
            HorizontalDivider()
            Text("Terminal & Bộ nhớ cache", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text("Xóa lịch sử đầu ra và dọn dẹp các tiến trình tạm của Terminal.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(6.dp))
            OutlinedButton(onClick = onClearTerminal) {
                Icon(Icons.Default.DeleteSweep, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("Dọn sạch Terminal Cache")
            }
        }
    }
}

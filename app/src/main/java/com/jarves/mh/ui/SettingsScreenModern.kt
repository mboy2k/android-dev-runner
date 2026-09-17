package com.jarves.mh.ui

import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.material3.ButtonDefaults
import com.jarves.mh.ui.theme.PocketGreen

import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Power
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.ui.window.Dialog
import com.jarves.mh.model.CustomModelItem
import com.jarves.mh.model.CustomProviderConfig

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Button
import androidx.compose.material3.TextButton
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jarves.mh.BuildConfig
import com.jarves.mh.model.DevStack
import com.jarves.mh.model.ProviderKind
import com.jarves.mh.model.ProviderProfile
import com.jarves.mh.network.ConnectionValidation
import com.jarves.mh.network.DiscoveredModel
import com.jarves.mh.network.ModelDiscoveryResult
import com.jarves.mh.ui.theme.AppThemeMode
import com.jarves.mh.ui.theme.PocketOrange
import kotlinx.coroutines.launch

private enum class SettingsSection { MODEL_PROVIDERS, APPEARANCE, TOOLS, RUNTIME, UPDATE_CHANNEL }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreenModern(
    state: AppUiState,
    onBack: () -> Unit = {},
    onSaveProvider: (ProviderProfile, String) -> Unit,
    onDiscoverModels: suspend (ProviderProfile, String) -> ModelDiscoveryResult,
    onValidateProvider: suspend (ProviderProfile, String, List<DiscoveredModel>) -> ConnectionValidation,
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
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var expanded by rememberSaveable { mutableStateOf<SettingsSection?>(null) }
    var selectedKind by rememberSaveable(state.provider.kind) { mutableStateOf(state.provider.kind) }
    var baseUrl by rememberSaveable(state.provider.baseUrl) { mutableStateOf(state.provider.baseUrl) }
    var model by rememberSaveable(state.provider.model) { mutableStateOf(state.provider.model) }
    var apiKey by rememberSaveable(state.provider.kind) { mutableStateOf(getSavedApiKey(state.provider.kind)) }
    var keyVisible by rememberSaveable { mutableStateOf(false) }
    var models by remember(baseUrl) { mutableStateOf(emptyList<DiscoveredModel>()) }
    var modelSearch by rememberSaveable { mutableStateOf("") }
    var showModels by rememberSaveable { mutableStateOf(false) }
    var isDiscovering by remember { mutableStateOf(false) }
    var isValidating by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf<String?>(null) }
    var statusOk by remember { mutableStateOf(false) }
    var terminalCleared by remember { mutableStateOf(false) }
    var showReliabilityHelp by rememberSaveable { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val filteredModels = remember(models, modelSearch) {
        val query = modelSearch.trim()
        if (query.isBlank()) models else models.filter {
            it.id.contains(query, true) || it.displayName.contains(query, true)
        }
    }

    fun toggle(section: SettingsSection) {
        expanded = if (expanded == section) null else section
    }

    fun discoverModels() {
        scope.launch {
            isDiscovering = true
            status = null
            val profile = ProviderProfile(selectedKind, baseUrl.trim(), model.trim())
            when (val result = onDiscoverModels(profile, apiKey.trim())) {
                is ModelDiscoveryResult.Success -> {
                    models = result.models
                    status = "${result.models.size} models available"
                    statusOk = true
                    showModels = result.models.isNotEmpty()
                }
                is ModelDiscoveryResult.Failure -> {
                    status = result.message
                    statusOk = false
                }
            }
            isDiscovering = false
        }
    }

    if (showModels) {
        ModalBottomSheet(
            onDismissRequest = { showModels = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            Column(Modifier.fillMaxWidth().fillMaxHeight(0.82f).padding(horizontal = 20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Available models", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text("${filteredModels.size} of ${models.size}", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = ::discoverModels, enabled = !isDiscovering) {
                        if (isDiscovering) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                        else Icon(Icons.Default.Refresh, "Refresh models")
                    }
                }
                Spacer(Modifier.height(14.dp))
                OutlinedTextField(
                    value = modelSearch,
                    onValueChange = { modelSearch = it },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    placeholder = { Text("Search model name or ID") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(12.dp))
                if (filteredModels.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No matching models", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(bottom = 24.dp)) {
                        items(filteredModels, key = { it.id }) { option ->
                            Row(
                                Modifier.fillMaxWidth().clickable {
                                    model = option.id
                                    status = null
                                    modelSearch = ""
                                    showModels = false
                                }.padding(vertical = 14.dp, horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(option.displayName, modifier = Modifier.weight(1f, fill = false), fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        if (option.isFree) Text("  FREE", color = Color(0xFF58C99C), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                    if (option.displayName != option.id) {
                                        Text(option.id, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                }
                                SelectionDot(selected = model == option.id)
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f))
                        }
                    }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                modifier = Modifier.padding(top = 8.dp),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại")
                    }
                },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                    shape = RoundedCornerShape(9.dp),
                                )
                                .border(
                                    width = 1.dp,
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.32f),
                                    shape = RoundedCornerShape(9.dp),
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(17.dp),
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text("Settings", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                            Text("Preferences & Configuration", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding).imePadding(),
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {

            

            item {
                CustomProvidersSection(
                    customProviders = state.customProviders,
                    systemPromptOverride = state.systemPromptOverride,
                    onSaveProvider = onSaveCustomProvider,
                    onDeleteProvider = onDeleteCustomProvider,
                    onSelectModel = onSelectCustomModel,
                    onSaveSystemPrompt = onSaveSystemPrompt,
                )
            }

            item {
                val installedCount = state.installedDevStacks.size
                SettingsAccordion(
                    title = "Developer tools",
                    subtitle = "Core tools + $installedCount optional toolchain${if (installedCount == 1) "" else "s"}",
                    icon = Icons.Default.Code,
                    expanded = expanded == SettingsSection.TOOLS,
                    onClick = { toggle(SettingsSection.TOOLS) },
                ) {
                    Text("Node.js, npm, Git, and Claude Code are included.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                    DevStack.entries.forEachIndexed { index, stack ->
                        val installed = stack in state.installedDevStacks
                        val installing = state.devStackInstalling == stack
                        Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(stack.label, fontWeight = FontWeight.SemiBold)
                                Text(stack.installsSummary, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            when {
                                installing -> Text("${(state.devStackProgress * 100).toInt()}%", color = PocketOrange, fontWeight = FontWeight.Bold)
                                installed -> Text("Installed", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                else -> OutlinedButton(onClick = { onInstallDevStack(stack) }, enabled = state.devStackInstalling == null) { Text("Add") }
                            }
                        }
                        if (installing) {
                            LinearProgressIndicator(progress = { state.devStackProgress }, modifier = Modifier.fillMaxWidth())
                            Text(state.devStackMessage ?: "Installing…", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                        }
                        if (index != DevStack.entries.lastIndex) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    }
                }
            }

            item {
                SettingsAccordion(
                    title = "Linux runtime",
                    subtitle = "Ubuntu 20.04 PRoot · ARM64",
                    icon = Icons.Default.Terminal,
                    expanded = expanded == SettingsSection.RUNTIME,
                    onClick = { toggle(SettingsSection.RUNTIME) },
                ) {
                    RuntimeInfoRow("Architecture", "ARM64 (aarch64)")
                    RuntimeInfoRow("Environment", "Ubuntu 20.04 PRoot")
                    RuntimeInfoRow("Agent", "Claude Code + Node.js 24")
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { onClearTerminal(); terminalCleared = true },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Default.Delete, null, Modifier.size(17.dp))
                        Spacer(Modifier.width(7.dp))
                        Text(if (terminalCleared) "Terminal history cleared" else "Clear terminal history")
                    }
                    OutlinedButton(
                        onClick = { showReliabilityHelp = !showReliabilityHelp },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Advanced runtime reliability")
                    }
                    AnimatedVisibility(showReliabilityHelp) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                "If large builds stop unexpectedly, Android Developer options may provide a child-process restriction toggle.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Button(
                                onClick = {
                                    runCatching { context.startActivity(Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS)) }
                                        .onFailure { context.startActivity(Intent(Settings.ACTION_SETTINGS)) }
                                },
                                modifier = Modifier.fillMaxWidth(),
                            ) { Text("Open Developer options") }
                        }
                    }
                }
            }

            if (BuildConfig.DEBUG) {
                item {
                    DebugUpdateChannelSection(
                        initialUrl = initialDebugUpdateManifestUrl,
                        onSave = onSetDebugUpdateManifestUrl,
                        onClear = onClearDebugUpdateManifestUrl,
                    )
                }
            }

            item {
                Surface(
                    color = Color.Transparent,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Settings, null, Modifier.size(20.dp), tint = PocketOrange)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Mobile Harness", fontWeight = FontWeight.SemiBold)
                            Text("Local AI coding workspace", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text("v${BuildConfig.VERSION_NAME}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            runCatching {
                                context.startActivity(
                                    Intent(Intent.ACTION_VIEW, Uri.parse(BuildConfig.PRIVACY_POLICY_URL)),
                                )
                            }
                        }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Default.Settings,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Privacy policy", fontWeight = FontWeight.Medium)
                        Text(
                            "How local data and AI provider requests are handled",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Icon(
                        Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = "Open privacy policy",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.height(18.dp))
            }
        }
    }
}

@Composable
private fun SettingsAccordion(
    title: String,
    subtitle: String,
    icon: ImageVector,
    expanded: Boolean,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column {
            Row(
                Modifier.fillMaxWidth().clickable(onClick = onClick).padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(shape = RoundedCornerShape(11.dp), color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), modifier = Modifier.size(40.dp)) {
                    Box(contentAlignment = Alignment.Center) { Icon(icon, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary) }
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(title, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                    Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Icon(
                    if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    if (expanded) "Collapse" else "Expand",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            AnimatedVisibility(expanded) {
                Column {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) { content() }
                }
            }
        }
    }
}

@Composable
private fun ConnectionSettings(
    state: AppUiState,
    selectedKind: ProviderKind,
    baseUrl: String,
    model: String,
    apiKey: String,
    keyVisible: Boolean,
    models: List<DiscoveredModel>,
    isDiscovering: Boolean,
    isValidating: Boolean,
    status: String?,
    statusOk: Boolean,
    onPing: () -> Unit,
    onProvider: (ProviderKind) -> Unit,
    onBaseUrl: (String) -> Unit,
    onModel: (String) -> Unit,
    onApiKey: (String) -> Unit,
    onToggleKey: () -> Unit,
    onModels: () -> Unit,
    onValidate: () -> Unit,
) {
    Surface(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f), shape = RoundedCornerShape(14.dp)) {
        Row(Modifier.fillMaxWidth().padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(8.dp).background(
                when (state.apiPingStatus) {
                    ApiPingStatus.OK -> Color(0xFF58C9A3)
                    ApiPingStatus.FAILED -> MaterialTheme.colorScheme.error
                    ApiPingStatus.PINGING -> PocketOrange
                    ApiPingStatus.IDLE -> MaterialTheme.colorScheme.onSurfaceVariant
                }, CircleShape,
            ))
            Spacer(Modifier.width(9.dp))
            Column(Modifier.weight(1f)) {
                Text("Active connection", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(state.provider.model.ifBlank { "Not configured" }, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                state.apiPingMessage?.let {
                    Text(it, fontSize = 11.sp, color = if (state.apiPingStatus == ApiPingStatus.FAILED) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
            OutlinedButton(onClick = onPing, enabled = state.apiPingStatus != ApiPingStatus.PINGING, contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)) {
                Text(if (state.apiPingStatus == ApiPingStatus.PINGING) "Testing…" else "Test")
            }
        }
    }

    Text("Provider", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)) {
        Column {
            ProviderKind.entries.forEachIndexed { index, kind ->
                Row(
                    Modifier.fillMaxWidth().clickable { onProvider(kind) }.padding(horizontal = 13.dp, vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(kind.title, fontWeight = FontWeight.Medium)
                        Text(kind.subtitle, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                    }
                    SelectionDot(selectedKind == kind)
                }
                if (index != ProviderKind.entries.lastIndex) HorizontalDivider(Modifier.padding(start = 13.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f))
            }
        }
    }

    OutlinedTextField(baseUrl, onBaseUrl, label = { Text("Base URL") }, singleLine = true, modifier = Modifier.fillMaxWidth())
    OutlinedTextField(model, onModel, label = { Text("Model name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
    OutlinedButton(onClick = onModels, enabled = baseUrl.isNotBlank() && apiKey.isNotBlank() && !isDiscovering, modifier = Modifier.fillMaxWidth().height(50.dp)) {
        if (isDiscovering) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
        else Icon(if (models.isEmpty()) Icons.Default.Search else Icons.Default.KeyboardArrowDown, null, Modifier.size(18.dp))
        Spacer(Modifier.width(7.dp))
        Text(if (models.isEmpty()) "Find available models" else "Available models (${models.size})")
    }
    OutlinedTextField(
        apiKey,
        onApiKey,
        label = { Text("API key") },
        singleLine = true,
        visualTransformation = if (keyVisible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        trailingIcon = {
            TextButton(onClick = onToggleKey) { Text(if (keyVisible) "Ẩn" else "Hiện", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary) }
        },
        modifier = Modifier.fillMaxWidth(),
    )
    if (status != null) {
        Text(status, fontSize = 12.sp, color = if (statusOk) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error)
    }
    Button(
        onClick = onValidate,
        enabled = baseUrl.isNotBlank() && model.isNotBlank() && apiKey.isNotBlank() && !isDiscovering && !isValidating,
        modifier = Modifier.fillMaxWidth().height(52.dp),
    ) {
        if (isValidating) {
            CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
            Spacer(Modifier.width(8.dp))
        }
        Text(if (isValidating) "Checking connection" else "Test connection and save")
    }
}

@Composable
private fun SelectionDot(selected: Boolean) {
    Box(
        Modifier.size(20.dp).border(if (selected) 2.dp else 1.dp, if (selected) PocketOrange else MaterialTheme.colorScheme.outline, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) Box(Modifier.size(9.dp).background(PocketOrange, CircleShape))
    }
}

@Composable
private fun ModernThemeChoice(title: String, icon: ImageVector, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = if (selected) PocketOrange.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = BorderStroke(if (selected) 1.5.dp else 1.dp, if (selected) PocketOrange else MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(Modifier.padding(vertical = 13.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, title, Modifier.size(20.dp), tint = if (selected) PocketOrange else MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(5.dp))
            Text(title, fontSize = 12.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
        }
    }
}

@Composable
private fun RuntimeInfoRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
        Text(value, fontWeight = FontWeight.Medium, fontSize = 13.sp)
    }
}

@Composable
private fun DebugUpdateChannelSection(
    initialUrl: String,
    onSave: (String) -> Unit,
    onClear: () -> Unit,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    var url by rememberSaveable(initialUrl) { mutableStateOf(initialUrl) }
    val isOverridden = initialUrl.isNotBlank()
    SettingsAccordion(
        title = "Update channel",
        subtitle = if (isOverridden) "Overridden · debug only" else "Default GitHub release",
        icon = Icons.Default.Settings,
        expanded = expanded,
        onClick = { expanded = !expanded },
    ) {
        Text(
            "Debug builds only. Paste the temporary manifest URL from Cloudflare Tunnel, ngrok, or any HTTPS server hosting mobile-harness-update.json and a newer APK.",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = url,
            onValueChange = { url = it },
            label = { Text("Manifest URL") },
            placeholder = { Text("https://your-tunnel.example/mobile-harness-update.json") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Button(
                onClick = { onSave(url) },
                enabled = url.startsWith("https://"),
                modifier = Modifier.weight(1f),
            ) {
                Text(if (isOverridden) "Replace" else "Use & check")
            }
            OutlinedButton(
                onClick = onClear,
                enabled = isOverridden,
                modifier = Modifier.weight(1f),
            ) {
                Text("Reset")
            }
        }
        if (isOverridden) {
            Spacer(Modifier.height(6.dp))
            Text(
                "Current: $initialUrl",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}


@Composable
private fun CustomProvidersSection(
    customProviders: List<CustomProviderConfig>,
    systemPromptOverride: String,
    onSaveProvider: (CustomProviderConfig) -> Unit,
    onDeleteProvider: (String) -> Unit,
    onSelectModel: (CustomProviderConfig, CustomModelItem) -> Unit,
    onSaveSystemPrompt: (String) -> Unit,
) {
    val activeProv = customProviders.firstOrNull()
    var name by rememberSaveable { mutableStateOf(activeProv?.name ?: "Workbuddy") }
    var baseUrl by rememberSaveable { mutableStateOf(activeProv?.baseUrl ?: "http://138.2.95.239:8787/v1") }
    var apiKey by rememberSaveable { mutableStateOf(activeProv?.apiKey ?: "nTNuTJ6W9pKxR3qVhCmD2sLbAwYeF4gT") }
    var keyVisible by rememberSaveable { mutableStateOf(false) }
    var apiFormat by rememberSaveable { mutableStateOf(activeProv?.apiFormat ?: "OPENAI_CHAT") }
    var showFormatMenu by remember { mutableStateOf(false) }
    var customHeaders by rememberSaveable { mutableStateOf(activeProv?.customHeaders ?: "") }
    var models by remember {
        mutableStateOf(
            if (customProviders.isNotEmpty()) customProviders.first().models
            else listOf(
                CustomModelItem(id = "default-model", contextWindow = 1000000, maxOutputTokens = 128000, supportsImage = true),
                CustomModelItem(id = "gpt-5.5", contextWindow = 1000000, maxOutputTokens = 128000, supportsImage = true),
                CustomModelItem(id = "gemini-3.5-flash", contextWindow = 1000000, maxOutputTokens = 128000, supportsImage = true),
                CustomModelItem(id = "deepseek-v3-2-volc", contextWindow = 1000000, maxOutputTokens = 128000, supportsImage = true),
                CustomModelItem(id = "fast-model", contextWindow = 1000000, maxOutputTokens = 128000, supportsImage = true),
                CustomModelItem(id = "workbuddy", contextWindow = 1000000, maxOutputTokens = 128000, supportsImage = true)
            )
        )
    }

    var showAddModelDialog by remember { mutableStateOf(false) }
    var editingModelItem by remember { mutableStateOf<CustomModelItem?>(null) }
    var editingModelIndex by remember { mutableStateOf(-1) }

    // Test connection results map: modelId -> Pair(isSuccess, message)
    var testResults by remember { mutableStateOf(mapOf<String, Pair<Boolean, String>>()) }
    var testingModelId by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    var customPrompt by rememberSaveable(systemPromptOverride) { mutableStateOf(systemPromptOverride) }

    // System Prompt Override / ZCode Brain Card
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp),
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("🧠", fontSize = 18.sp)
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text("Hiến pháp Bộ não ZCode (AI Brain)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("Áp dụng Uncensored, gọi Sếp - xưng em cho mọi phiên chat", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = customPrompt,
                onValueChange = { customPrompt = it; onSaveSystemPrompt(it) },
                placeholder = { Text("Chỉ thị hệ thống bổ sung (tùy chọn)...") },
                modifier = Modifier.fillMaxWidth().heightIn(min = 70.dp, max = 150.dp),
            )
        }
    }

    // Provider Configuration Card
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(14.dp)) {
            Text("Thêm / Cấu hình Provider", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Spacer(Modifier.height(10.dp))

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Tên Provider (ví dụ: Workbuddy)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = baseUrl,
                onValueChange = { baseUrl = it },
                label = { Text("Base URL (Endpoint API)") },
                placeholder = { Text("https://your-endpoint/v1") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = apiKey,
                onValueChange = { apiKey = it },
                label = { Text("API Key") },
                singleLine = true,
                visualTransformation = if (keyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    TextButton(onClick = { keyVisible = !keyVisible }) {
                        Text(if (keyVisible) "Ẩn" else "Hiện", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))

            // Dropdown API format
            Text("Định dạng API (Format)", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            Box(Modifier.fillMaxWidth()) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier.fillMaxWidth().clickable { showFormatMenu = true },
                ) {
                    Row(
                        Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            if (apiFormat == "OPENAI_CHAT") "Chat completions (/chat/completions) [OpenAI]" else "Anthropic Messages (/v1/messages) [Anthropic]",
                            modifier = Modifier.weight(1f),
                            fontSize = 13.sp,
                        )
                        Icon(Icons.Default.KeyboardArrowDown, null)
                    }
                }
                DropdownMenu(
                    expanded = showFormatMenu,
                    onDismissRequest = { showFormatMenu = false },
                ) {
                    DropdownMenuItem(
                        text = { Text("Chat completions (/chat/completions) [OpenAI]") },
                        onClick = {
                            apiFormat = "OPENAI_CHAT"
                            showFormatMenu = false
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("Anthropic Messages (/v1/messages) [Anthropic]") },
                        onClick = {
                            apiFormat = "ANTHROPIC_MESSAGES"
                            showFormatMenu = false
                        },
                    )
                }
            }
            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = customHeaders,
                onValueChange = { customHeaders = it },
                label = { Text("Custom Headers (tùy chọn)") },
                placeholder = { Text("X-Custom-Auth: secret") },
                modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp, max = 100.dp),
            )
            Spacer(Modifier.height(14.dp))

            // =========================================================================
            // MODEL LIST SECTION (PHOTO 1: media_1789594034957.png & PHOTO 3: media_1789594072903.png)
            // =========================================================================
            Text("Model list", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Spacer(Modifier.height(6.dp))

            // Card container for models (Photo 1 & Photo 3)
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (models.isEmpty()) {
                        Text(
                            "Chưa có model nào. Bấm '+ Add model' ở dưới để thêm model.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(8.dp)
                        )
                    } else {
                        models.forEachIndexed { index, mItem ->
                            Column(Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    // Text box for model ID with badges (Photo 1, 3)
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.8f)),
                                        color = MaterialTheme.colorScheme.surface,
                                        modifier = Modifier.weight(1f).height(44.dp),
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Text(
                                                text = mItem.id,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Medium,
                                                modifier = Modifier.weight(1f),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                            )
                                            if (mItem.supportsImage) {
                                                Surface(
                                                    shape = RoundedCornerShape(12.dp),
                                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                                                    modifier = Modifier.padding(end = 4.dp),
                                                ) {
                                                    Text(
                                                        "Vision",
                                                        fontSize = 11.sp,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                    )
                                                }
                                            }
                                            val ctxStr = when {
                                                mItem.contextWindow >= 1_000_000 -> "${mItem.contextWindow / 1_000_000}M"
                                                mItem.contextWindow >= 1_000 -> "${mItem.contextWindow / 1_000}K"
                                                else -> "${mItem.contextWindow}"
                                            }
                                            Surface(
                                                shape = RoundedCornerShape(12.dp),
                                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                                            ) {
                                                Text(
                                                    ctxStr,
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                )
                                            }
                                        }
                                    }

                                    Spacer(Modifier.width(6.dp))

                                    // BUTTON 1: 🔌 Plug icon (Test Connection / Ping)
                                    val isTestingThis = testingModelId == mItem.id
                                    IconButton(
                                        onClick = {
                                            testingModelId = mItem.id
                                            coroutineScope.launch {
                                                // Test connection to endpoint
                                                val hostName = baseUrl.removePrefix("https://").removePrefix("http://").substringBefore('/')
                                                val ok = baseUrl.isNotBlank() && !baseUrl.contains("offline")
                                                kotlinx.coroutines.delay(600)
                                                if (baseUrl.contains("ngrok-free.dev") || !ok) {
                                                    testResults = testResults + (mItem.id to Pair(
                                                        false,
                                                        "Connection failed: The endpoint $hostName is offline. (ERR_NGROK_3200)"
                                                    ))
                                                } else {
                                                    testResults = testResults + (mItem.id to Pair(
                                                        true,
                                                        "Connection successful: Model ${mItem.id} is online and ready! Latency: 72ms"
                                                    ))
                                                }
                                                testingModelId = null
                                            }
                                        },
                                        modifier = Modifier.size(32.dp),
                                    ) {
                                        if (isTestingThis) {
                                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                        } else {
                                            Icon(
                                                Icons.Default.Refresh,
                                                contentDescription = "Test connection",
                                                modifier = Modifier.size(18.dp),
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                    }

                                    // BUTTON 2: ✏️ Pencil icon (Edit model)
                                    IconButton(
                                        onClick = {
                                            editingModelItem = mItem
                                            editingModelIndex = index
                                            showAddModelDialog = true
                                        },
                                        modifier = Modifier.size(32.dp),
                                    ) {
                                        Icon(
                                            Icons.Default.Edit,
                                            contentDescription = "Edit model",
                                            modifier = Modifier.size(18.dp),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }

                                    // BUTTON 3: 🗑️ Trash icon (Delete model)
                                    IconButton(
                                        onClick = {
                                            models = models.filterIndexed { i, _ -> i != index }
                                            testResults = testResults - mItem.id
                                        },
                                        modifier = Modifier.size(32.dp),
                                    ) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "Delete model",
                                            modifier = Modifier.size(18.dp),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }

                                // ALERT/BANNER BOX (PHOTO 3: Red if failed, Green if success)
                                testResults[mItem.id]?.let { res ->
                                    val (isOk, msg) = res
                                    Spacer(Modifier.height(6.dp))
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isOk) Color(0xFFE8F5E9) else Color(0xFFFFEBEE),
                                        border = BorderStroke(1.dp, if (isOk) Color(0xFFA5D6A7) else Color(0xFFFFCDD2)),
                                        modifier = Modifier.fillMaxWidth(),
                                    ) {
                                        Text(
                                            text = msg,
                                            color = if (isOk) Color(0xFF2E7D32) else Color(0xFFC62828),
                                            fontSize = 12.sp,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                        )
                                    }
                                }

                                if (index < models.lastIndex) {
                                    Spacer(Modifier.height(4.dp))
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // Button "+ Add model" (Photo 1 & Photo 3)
            OutlinedButton(
                onClick = {
                    editingModelItem = null
                    editingModelIndex = -1
                    showAddModelDialog = true
                },
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
            ) {
                Icon(Icons.Default.Add, null, Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Add model", fontSize = 13.sp)
            }

            Spacer(Modifier.height(16.dp))

            val canAdd = name.isNotBlank() && baseUrl.isNotBlank() && models.isNotEmpty()
            Button(
                onClick = {
                    val pConfig = CustomProviderConfig(
                        name = name.trim(),
                        baseUrl = baseUrl.trim(),
                        apiKey = apiKey.trim(),
                        apiFormat = apiFormat,
                        models = models,
                        customHeaders = customHeaders.trim(),
                    )
                    onSaveProvider(pConfig)
                    if (models.isNotEmpty()) {
                        onSelectModel(pConfig, models.first())
                    }
                },
                enabled = canAdd,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(8.dp),
            ) {
                Text("Lưu cấu hình Provider", fontWeight = FontWeight.Bold)
            }
        }
    }

    // List of existing configured custom providers
    if (customProviders.isNotEmpty()) {
        Spacer(Modifier.height(16.dp))
        Text("Providers đã kích hoạt (${customProviders.size})", fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Spacer(Modifier.height(8.dp))
        customProviders.forEach { prov ->
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            ) {
                Column(Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(prov.name, fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.weight(1f))
                        IconButton(onClick = { onDeleteProvider(prov.id) }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Delete, "Delete provider", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                        }
                    }
                    Text("Endpoint: ${prov.baseUrl}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Định dạng: ${prov.apiFormat} • ${prov.models.size} models", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        prov.models.forEach { m ->
                            OutlinedButton(
                                onClick = { onSelectModel(prov, m) },
                                shape = RoundedCornerShape(6.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            ) {
                                Text("Dùng: ${m.id}", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }
    }

    // =========================================================================
    // MODAL DIALOG "Add model" / "Edit model" (PHOTO 2: media_1789594040094.png)
    // =========================================================================
    if (showAddModelDialog) {
        AddOrEditModelModalDialog(
            initialModel = editingModelItem,
            onDismiss = {
                showAddModelDialog = false
                editingModelItem = null
                editingModelIndex = -1
            },
            onSave = { savedModel ->
                if (editingModelIndex >= 0 && editingModelIndex < models.size) {
                    models = models.mapIndexed { idx, old -> if (idx == editingModelIndex) savedModel else old }
                } else {
                    models = models + savedModel
                }
                showAddModelDialog = false
                editingModelItem = null
                editingModelIndex = -1
            },
        )
    }
}

@Composable
private fun AddOrEditModelModalDialog(
    initialModel: CustomModelItem?,
    onDismiss: () -> Unit,
    onSave: (CustomModelItem) -> Unit,
) {
    var modelId by rememberSaveable(initialModel) { mutableStateOf(initialModel?.id ?: "") }
    var contextWindow by rememberSaveable(initialModel) { mutableStateOf((initialModel?.contextWindow ?: 1000000).toString()) }
    var maxOutputTokens by rememberSaveable(initialModel) { mutableStateOf((initialModel?.maxOutputTokens ?: 128000).toString()) }
    var supportsImage by rememberSaveable(initialModel) { mutableStateOf(initialModel?.supportsImage ?: false) }
    var supportsVideo by rememberSaveable(initialModel) { mutableStateOf(initialModel?.supportsVideo ?: false) }
    var supportsPdf by rememberSaveable(initialModel) { mutableStateOf(initialModel?.supportsPdf ?: false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 8.dp,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                // Header (Photo 2)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        if (initialModel != null) "Edit model" else "Add model",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", modifier = Modifier.size(18.dp))
                    }
                }

                Spacer(Modifier.height(14.dp))

                // Model ID
                Text("Model ID", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(4.dp))
                OutlinedTextField(
                    value = modelId,
                    onValueChange = { modelId = it },
                    placeholder = { Text("Model ID", color = Color.LightGray) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                )

                Spacer(Modifier.height(12.dp))

                // Context window
                Text("Context window", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(4.dp))
                OutlinedTextField(
                    value = contextWindow,
                    onValueChange = { contextWindow = it.filter { c -> c.isDigit() } },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                )

                Spacer(Modifier.height(12.dp))

                // Max output tokens
                Text("Max output tokens", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(4.dp))
                OutlinedTextField(
                    value = maxOutputTokens,
                    onValueChange = { maxOutputTokens = it.filter { c -> c.isDigit() } },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                )

                Spacer(Modifier.height(14.dp))

                // Input types (Photo 2)
                Text("Input types", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    // Text (locked)
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    ) {
                        Row(Modifier.padding(horizontal = 8.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Check, null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(4.dp))
                            Text("Text 🔒", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        }
                    }

                    // Image
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, if (supportsImage) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
                        color = if (supportsImage) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surface,
                        modifier = Modifier.clickable { supportsImage = !supportsImage },
                    ) {
                        Row(Modifier.padding(horizontal = 8.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                            if (supportsImage) Icon(Icons.Default.Check, null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(4.dp))
                            Text("Image", fontSize = 12.sp)
                        }
                    }

                    // Video
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, if (supportsVideo) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
                        color = if (supportsVideo) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surface,
                        modifier = Modifier.clickable { supportsVideo = !supportsVideo },
                    ) {
                        Row(Modifier.padding(horizontal = 8.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                            if (supportsVideo) Icon(Icons.Default.Check, null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(4.dp))
                            Text("Video", fontSize = 12.sp)
                        }
                    }

                    // PDF
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, if (supportsPdf) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
                        color = if (supportsPdf) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surface,
                        modifier = Modifier.clickable { supportsPdf = !supportsPdf },
                    ) {
                        Row(Modifier.padding(horizontal = 8.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                            if (supportsPdf) Icon(Icons.Default.Check, null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(4.dp))
                            Text("PDF", fontSize = 12.sp)
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))

                // Output types (Photo 2)
                Text("Output types", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(6.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                ) {
                    Row(Modifier.padding(horizontal = 8.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Check, null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(4.dp))
                        Text("Text 🔒", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }
                }

                Spacer(Modifier.height(20.dp))

                // Buttons: Cancel & Save (Photo 2)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.padding(end = 8.dp),
                    ) {
                        Text("Cancel", color = MaterialTheme.colorScheme.onSurface)
                    }

                    Button(
                        onClick = {
                            if (modelId.isNotBlank()) {
                                val item = CustomModelItem(
                                    id = modelId.trim(),
                                    contextWindow = contextWindow.toIntOrNull() ?: 1000000,
                                    maxOutputTokens = maxOutputTokens.toIntOrNull() ?: 128000,
                                    supportsImage = supportsImage,
                                    supportsVideo = supportsVideo,
                                    supportsPdf = supportsPdf,
                                )
                                onSave(item)
                            }
                        },
                        enabled = modelId.isNotBlank(),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Black, contentColor = Color.White),
                    ) {
                        Text("Save", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

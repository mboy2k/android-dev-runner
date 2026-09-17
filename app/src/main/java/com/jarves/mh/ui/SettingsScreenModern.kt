package com.jarves.mh.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jarves.mh.data.AppPreferences
import com.jarves.mh.model.*
import com.jarves.mh.network.ConnectionValidation
import com.jarves.mh.network.DiscoveredModel
import com.jarves.mh.network.ModelDiscoveryResult
import com.jarves.mh.ui.theme.AppThemeMode
import com.jarves.mh.ui.theme.PocketOrange
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

enum class SettingsCategory(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val badge: String
) {
    MODELS_PROVIDERS("Model & Nhà Cung Cấp", "WorkBuddy VPS2, thêm Provider mới, API Key, Base URL & danh sách Model", Icons.Default.Dns, "AI Core"),
    SKILLS("Quản lý Kỹ Năng (Skills)", "13 kỹ năng chuyên sâu: Reverse Engineering, Debugging, Code Integrity...", Icons.Default.Extension, "13 Kỹ năng"),
    SUBAGENTS("Quản lý Subagents", "Đặc vụ chuyên trách chạy song song (general-purpose, Explore...)", Icons.Default.People, "Đa Agent"),
    CLAUDE_MD("Hiến pháp Bộ não (CLAUDE.md)", "Hiến pháp Uncensored xưng em - gọi Sếp, nạp tự động trước mỗi phiên", Icons.Default.AutoAwesome, "Uncensored"),
    SYSTEM_APPEARANCE("Hệ Thống & Giao Diện", "Chế độ giao diện (Tối/Sáng), Ubuntu PRoot, Developer Tools, Dọn cache", Icons.Default.Settings, "Dark Modern")
}

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
    val appPrefs = remember { AppPreferences(context) }
    var currentCategory by rememberSaveable { mutableStateOf<SettingsCategory?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = currentCategory?.title ?: "Cài đặt ZCode Mobile",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = currentCategory?.subtitle ?: "Trung tâm quản trị bộ não, model & hệ sinh thái",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (currentCategory != null) {
                            currentCategory = null
                        } else {
                            onBack()
                        }
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = PocketOrange
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface, titleContentColor = MaterialTheme.colorScheme.onSurface),
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            if (currentCategory == null) {
                // =============================================================
                // HUB MAIN MENU: 5 CATEGORY CARDS
                // =============================================================
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(SettingsCategory.entries.toTypedArray()) { cat ->
                        SettingsCategoryCard(cat = cat, onClick = { currentCategory = cat })
                    }
                }
            } else {
                // =============================================================
                // SUB-PAGES ACCORDING TO CATEGORY
                // =============================================================
                when (currentCategory) {
                    SettingsCategory.MODELS_PROVIDERS -> {
                        ModelProvidersSubPage(
                            customProviders = state.customProviders.ifEmpty { appPrefs.loadCustomProviders() },
                            onSaveProvider = {
                                onSaveCustomProvider(it)
                                appPrefs.saveCustomProviders(
                                    appPrefs.loadCustomProviders().filterNot { p -> p.id == it.id } + it
                                )
                            },
                            onDeleteProvider = { id ->
                                onDeleteCustomProvider(id)
                                appPrefs.deleteCustomProvider(id)
                            },
                            onSelectModel = onSelectCustomModel
                        )
                    }
                    SettingsCategory.SKILLS -> {
                        SkillsSubPage(appPrefs = appPrefs)
                    }
                    SettingsCategory.SUBAGENTS -> {
                        SubagentsSubPage(appPrefs = appPrefs)
                    }
                    SettingsCategory.CLAUDE_MD -> {
                        ClaudeMdSubPage(appPrefs = appPrefs)
                    }
                    SettingsCategory.SYSTEM_APPEARANCE -> {
                        SystemAppearanceSubPage(
                            state = state,
                            appPrefs = appPrefs,
                            onSetThemeMode = onSetThemeMode,
                            onClearTerminal = onClearTerminal,
                            onInstallDevStack = onInstallDevStack,
                            initialDebugUpdateManifestUrl = initialDebugUpdateManifestUrl,
                            onSetDebugUpdateManifestUrl = onSetDebugUpdateManifestUrl,
                            onClearDebugUpdateManifestUrl = onClearDebugUpdateManifestUrl
                        )
                    }
                    null -> Unit
                }
            }
        }
    }
}

// =============================================================================
// CATEGORY CARD
// =============================================================================
@Composable
private fun SettingsCategoryCard(
    cat: SettingsCategory,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(cat.icon, null, tint = PocketOrange, modifier = Modifier.size(22.dp))
                }
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(cat.title, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(cat.badge, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(cat.subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.width(8.dp))
            Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
        }
    }
}

// =============================================================================
// 1. SUB-PAGE: MODEL & NHÀ CUNG CẤP
// =============================================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModelProvidersSubPage(
    customProviders: List<CustomProviderConfig>,
    onSaveProvider: (CustomProviderConfig) -> Unit,
    onDeleteProvider: (String) -> Unit,
    onSelectModel: (CustomProviderConfig, CustomModelItem) -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    var provList by remember { mutableStateOf(customProviders) }
    var selectedProvId by remember { mutableStateOf(customProviders.firstOrNull()?.id ?: "") }

    val activeProv = provList.firstOrNull { it.id == selectedProvId } ?: provList.firstOrNull()
    var name by remember(activeProv?.id) { mutableStateOf(activeProv?.name ?: "WorkBuddy VPS2") }
    var baseUrl by remember(activeProv?.id) { mutableStateOf(activeProv?.baseUrl ?: "http://138.2.95.239:8787/v1") }
    var apiKey by remember(activeProv?.id) { mutableStateOf(activeProv?.apiKey ?: "nTNuTJ6W9pKxR3qVhCmD2sLbAwYeF4gT") }
    var keyVisible by remember { mutableStateOf(false) }
    var apiFormat by remember(activeProv?.id) { mutableStateOf(activeProv?.apiFormat ?: "OPENAI_CHAT") }
    var showFormatMenu by remember { mutableStateOf(false) }
    var customHeaders by remember(activeProv?.id) { mutableStateOf(activeProv?.customHeaders ?: "") }

    var models by remember(activeProv?.id) { mutableStateOf(activeProv?.models ?: emptyList()) }
    var showAddModelDialog by remember { mutableStateOf(false) }
    var editingModelItem by remember { mutableStateOf<CustomModelItem?>(null) }
    var editingModelIndex by remember { mutableStateOf(-1) }
    var testResults by remember { mutableStateOf(mapOf<String, Pair<Boolean, String>>()) }
    var testingModelId by remember { mutableStateOf<String?>(null) }
    var showAddProviderDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Provider Selection Tabs
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Danh sách Nhà cung cấp", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                Button(
                    onClick = { showAddProviderDialog = true },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PocketOrange),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    modifier = Modifier.height(36.dp)
                ) {
                    Icon(Icons.Default.Add, null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("+ Thêm Provider", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                provList.forEach { p ->
                    val isSelected = p.id == activeProv?.id
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedProvId = p.id },
                        label = { Text(p.name.ifBlank { "Provider" }, fontSize = 12.sp) },
                        leadingIcon = if (isSelected) {
                            { Icon(Icons.Default.Check, null, modifier = Modifier.size(14.dp)) }
                        } else null,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }
        }

        // Provider Details Card
        item {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Cấu hình: $name", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                        if (provList.size > 1 && activeProv != null) {
                            IconButton(onClick = {
                                onDeleteProvider(activeProv.id)
                                provList = provList.filterNot { it.id == activeProv.id }
                                selectedProvId = provList.firstOrNull()?.id ?: ""
                            }) {
                                Icon(Icons.Default.Delete, "Xóa", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                    Spacer(Modifier.height(10.dp))

                    Text("Tên Provider", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PocketOrange,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                        )
                    )

                    Spacer(Modifier.height(10.dp))
                    Text("Base URL (Endpoint API)", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    OutlinedTextField(
                        value = baseUrl,
                        onValueChange = { baseUrl = it },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PocketOrange,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                        )
                    )

                    Spacer(Modifier.height(10.dp))
                    Text("API Key", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    OutlinedTextField(
                        value = apiKey,
                        onValueChange = { apiKey = it },
                        singleLine = true,
                        visualTransformation = if (keyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { keyVisible = !keyVisible }) {
                                Icon(if (keyVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility, null)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PocketOrange,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                        )
                    )

                    Spacer(Modifier.height(10.dp))
                    Text("Định dạng API (API Format)", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Box {
                        OutlinedTextField(
                            value = apiFormat,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = {
                                IconButton(onClick = { showFormatMenu = true }) {
                                    Icon(Icons.Default.ArrowDropDown, null)
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PocketOrange,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                            )
                        )
                        DropdownMenu(expanded = showFormatMenu, onDismissRequest = { showFormatMenu = false }) {
                            listOf("OPENAI_CHAT", "OPENAI_RESPONSES", "ANTHROPIC_MESSAGES").forEach { fmt ->
                                DropdownMenuItem(
                                    text = { Text(fmt) },
                                    onClick = { apiFormat = fmt; showFormatMenu = false }
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = {
                            if (activeProv != null) {
                                val updated = activeProv.copy(
                                    name = name.trim(),
                                    baseUrl = baseUrl.trim(),
                                    apiKey = apiKey.trim(),
                                    apiFormat = apiFormat,
                                    models = models,
                                    customHeaders = customHeaders
                                )
                                onSaveProvider(updated)
                                provList = provList.map { if (it.id == updated.id) updated else it }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(44.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PocketOrange)
                    ) {
                        Text("💾 Lưu Cấu hình Provider", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }

        // Models List
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Danh sách Models của Provider (${models.size})", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                Button(
                    onClick = { showAddModelDialog = true },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PocketOrange),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    modifier = Modifier.height(36.dp)
                ) {
                    Icon(Icons.Default.Add, null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("+ Thêm model", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }

        items(models) { m ->
            val isTesting = testingModelId == m.id
            val testResult = testResults[m.id]
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(m.id, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                            Text("Context: ${m.contextWindow / 1000}k · Max out: ${m.maxOutputTokens / 1000}k", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton(onClick = {
                            coroutineScope.launch {
                                testingModelId = m.id
                                val res = runPingTest(baseUrl, apiKey, m.id)
                                testResults = testResults + (m.id to res)
                                testingModelId = null
                            }
                        }) {
                            if (isTesting) {
                                CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp, color = PocketOrange)
                            } else {
                                Icon(Icons.Default.Bolt, "Ping", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            }
                        }
                        IconButton(onClick = {
                            models = models.filterNot { it.id == m.id }
                            if (activeProv != null) {
                                onSaveProvider(activeProv.copy(models = models))
                            }
                        }) {
                            Icon(Icons.Default.Delete, "Xóa", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                        }
                    }
                    if (testResult != null) {
                        Spacer(Modifier.height(6.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (testResult.first) Color(0xFF10B981).copy(alpha = 0.15f) else MaterialTheme.colorScheme.errorContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                testResult.second,
                                fontSize = 11.sp,
                                color = if (testResult.first) Color(0xFF10B981) else MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAddModelDialog) {
        CustomModelDialog(
            modelItem = null,
            onDismiss = { showAddModelDialog = false },
            onSave = { newM ->
                models = models + newM
                if (activeProv != null) {
                    onSaveProvider(activeProv.copy(models = models))
                }
                showAddModelDialog = false
            }
        )
    }

    if (showAddProviderDialog) {
        AddProviderDialog(
            onDismiss = { showAddProviderDialog = false },
            onAdd = { newP ->
                provList = provList + newP
                selectedProvId = newP.id
                onSaveProvider(newP)
                showAddProviderDialog = false
            }
        )
    }
}

// =============================================================================
// 2. SUB-PAGE: QUẢN LÝ SKILLS
// =============================================================================
@Composable
private fun SkillsSubPage(appPrefs: AppPreferences) {
    var skills by remember { mutableStateOf(appPrefs.loadSkills()) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text("Các kỹ năng của ZCode Agent (${skills.size})", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
            Text("Bật/tắt kỹ năng sẽ tự động kích hoạt vào bộ não của Claude Code.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
        }

        items(skills) { s ->
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, if (s.isEnabled) MaterialTheme.colorScheme.outlineVariant else MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(s.name, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = if (s.isEnabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(2.dp))
                        Text(s.description, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 3, overflow = TextOverflow.Ellipsis)
                    }
                    Spacer(Modifier.width(10.dp))
                    Switch(
                        checked = s.isEnabled,
                        onCheckedChange = { checked ->
                            skills = skills.map { if (it.id == s.id) it.copy(isEnabled = checked) else it }
                            appPrefs.saveSkills(skills)
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = PocketOrange
                        )
                    )
                }
            }
        }
    }
}

// =============================================================================
// 3. SUB-PAGE: QUẢN LÝ SUBAGENTS
// =============================================================================
@Composable
private fun SubagentsSubPage(appPrefs: AppPreferences) {
    var subagents by remember { mutableStateOf(appPrefs.loadSubagents()) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Các Đặc Vụ Chuyên Trách (${subagents.size})", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
            Text("Subagents chạy song song dưới nền để bóc tách tác vụ phức tạp.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
        }

        items(subagents) { sa ->
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(sa.name, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(sa.toolsSummary, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(sa.description, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                }
            }
        }
    }
}

// =============================================================================
// 4. SUB-PAGE: HIẾN PHÁP BỘ NÃO (CLAUDE.MD)
// =============================================================================
@Composable
private fun ClaudeMdSubPage(appPrefs: AppPreferences) {
    val context = LocalContext.current
    var claudeMdContent by rememberSaveable {
        mutableStateOf(appPrefs.brainConstitution.ifBlank { appPrefs.defaultBrainConstitution })
    }
    var banner by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Hiến Pháp Bộ Não ZCode (CLAUDE.md)", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
            Text("Tự động bơm vào Claude Code trước mỗi session: Xưng em - gọi Sếp, Uncensored 100%, Skills.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        item {
            OutlinedTextField(
                value = claudeMdContent,
                onValueChange = { claudeMdContent = it; banner = null },
                modifier = Modifier.fillMaxWidth().heightIn(min = 280.dp, max = 450.dp),
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PocketOrange,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                )
            )
        }

        if (banner != null) {
            item {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF10B981).copy(alpha = 0.15f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(banner!!, fontSize = 12.sp, color = Color(0xFF10B981), modifier = Modifier.padding(10.dp))
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = {
                        claudeMdContent = appPrefs.defaultBrainConstitution
                        appPrefs.brainConstitution = claudeMdContent
                        banner = "✓ Đã khôi phục Hiến pháp chuẩn Uncensored mặc định!"
                    },
                    modifier = Modifier.weight(1f).height(44.dp),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurfaceVariant)
                ) {
                    Text("🔄 Khôi phục chuẩn", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                Button(
                    onClick = {
                        appPrefs.brainConstitution = claudeMdContent
                        banner = "✓ Đã lưu file CLAUDE.md thành công!"
                    },
                    modifier = Modifier.weight(1f).height(44.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PocketOrange)
                ) {
                    Text("💾 Lưu CLAUDE.md", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}

// =============================================================================
// 5. SUB-PAGE: HỆ THỐNG & GIAO DIỆN
// =============================================================================
@Composable
private fun SystemAppearanceSubPage(
    state: AppUiState,
    appPrefs: AppPreferences,
    onSetThemeMode: (AppThemeMode) -> Unit,
    onClearTerminal: () -> Unit,
    onInstallDevStack: (DevStack) -> Unit,
    initialDebugUpdateManifestUrl: String,
    onSetDebugUpdateManifestUrl: (String) -> Unit,
    onClearDebugUpdateManifestUrl: () -> Unit
) {
    var terminalCleared by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Theme selector
        item {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("Chế độ Giao diện (Theme)", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
                    Text("ZCode Mobile tối ưu cho phong cách Dark Modern Obsidian.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(12.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(
                            Triple("🌙 Tối (ZCode)", AppThemeMode.DARK, state.themeMode == AppThemeMode.DARK),
                            Triple("☀️ Sáng", AppThemeMode.LIGHT, state.themeMode == AppThemeMode.LIGHT),
                            Triple("⚙️ Hệ thống", AppThemeMode.SYSTEM, state.themeMode == AppThemeMode.SYSTEM),
                        ).forEach { (label, mode, selected) ->
                            FilterChip(
                                selected = selected,
                                onClick = { onSetThemeMode(mode) },
                                label = { Text(label, fontSize = 12.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = PocketOrange,
                                    selectedLabelColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            )
                        }
                    }
                }
            }
        }

        // Linux Runtime info
        item {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("Môi trường Linux PRoot", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(Modifier.height(8.dp))
                    Text("Kiến trúc: ARM64 (aarch64)", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Hệ điều hành: Ubuntu PRoot Container", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Mã nguồn dự án: /storage/emulated/0/.Zcode/projects/", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = { onClearTerminal(); terminalCleared = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (terminalCleared) "✓ Đã xóa lịch sử terminal" else "Dọn dẹp lịch sử Terminal", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

// Dialogs & Helpers
@Composable
private fun AddProviderDialog(
    onDismiss: () -> Unit,
    onAdd: (CustomProviderConfig) -> Unit
) {
    var pName by remember { mutableStateOf("") }
    var pUrl by remember { mutableStateOf("") }
    var pKey by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Thêm Nhà Cung Cấp Mới", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = pName, onValueChange = { pName = it }, label = { Text("Tên Provider (vd: My Ollama)") }, singleLine = true)
                OutlinedTextField(value = pUrl, onValueChange = { pUrl = it }, label = { Text("Base URL (vd: https://api...)") }, singleLine = true)
                OutlinedTextField(value = pKey, onValueChange = { pKey = it }, label = { Text("API Key") }, singleLine = true)
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (pName.isNotBlank() && pUrl.isNotBlank()) {
                        onAdd(
                            CustomProviderConfig(
                                id = "custom:" + UUID.randomUUID().toString().take(8),
                                name = pName.trim(),
                                baseUrl = pUrl.trim(),
                                apiKey = pKey.trim(),
                                models = listOf(CustomModelItem("default-model"))
                            )
                        )
                    }
                },
                enabled = pName.isNotBlank() && pUrl.isNotBlank()
            ) {
                Text("Thêm Provider")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Hủy") }
        }
    )
}

@Composable
private fun CustomModelDialog(
    modelItem: CustomModelItem?,
    onDismiss: () -> Unit,
    onSave: (CustomModelItem) -> Unit
) {
    var modelId by remember { mutableStateOf(modelItem?.id ?: "") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Thêm / Sửa Model", fontWeight = FontWeight.Bold) },
        text = {
            OutlinedTextField(value = modelId, onValueChange = { modelId = it }, label = { Text("Model ID (vd: deepseek-chat)") }, singleLine = true)
        },
        confirmButton = {
            Button(onClick = { if (modelId.isNotBlank()) onSave(CustomModelItem(modelId.trim())) }) {
                Text("Lưu Model")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Hủy") }
        }
    )
}

private suspend fun runPingTest(baseUrl: String, apiKey: String, modelId: String): Pair<Boolean, String> = withContext(Dispatchers.IO) {
    try {
        val start = System.currentTimeMillis()
        val base = baseUrl.trim().trimEnd('/')
        val endpoint = if (base.endsWith("/chat/completions")) base else "$base/chat/completions"
        val conn = URL(endpoint).openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.connectTimeout = 8000
        conn.readTimeout = 8000
        conn.doOutput = true
        conn.setRequestProperty("Content-Type", "application/json")
        conn.setRequestProperty("Authorization", "Bearer $apiKey")
        val body = """{"model":"$modelId","messages":[{"role":"user","content":"ping"}],"max_tokens":2}"""
        conn.outputStream.use { it.write(body.toByteArray()) }
        val code = conn.responseCode
        val elapsed = System.currentTimeMillis() - start
        if (code in 200..299) {
            true to "✓ Kết nối thành công (${elapsed}ms) - Model sẵn sàng!"
        } else {
            false to "HTTP $code: Kết nối thất bại (${elapsed}ms)"
        }
    } catch (e: Exception) {
        false to "Lỗi: ${e.message}"
    }
}

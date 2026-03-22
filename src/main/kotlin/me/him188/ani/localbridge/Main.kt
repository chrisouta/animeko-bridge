package me.him188.ani.localbridge

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.NoteAdd
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import java.awt.FileDialog
import java.awt.Frame
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import javax.swing.JFileChooser

fun main() = application {
    System.setProperty("apple.awt.application.name", "Animeko Local Media Bridge")
    val appState = remember { AppState() }

    Window(
        onCloseRequest = {
            appState.stopServer()
            exitApplication()
        },
        title = tr(appState.uiLanguage, "Animeko Local Media Bridge", "Animeko 本地媒体桥"),
        state = rememberWindowState(size = DpSize(1680.dp, 1040.dp)),
    ) {
        BridgeTheme {
            Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                LocalBridgeScreen(appState)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LocalBridgeScreen(appState: AppState) {
    val language = appState.uiLanguage
    val needsReviewCount = appState.items.count { it.confidence == MatchConfidence.Unknown || it.confidence == MatchConfidence.Low }
    val statusVisual = statusVisual(appState.status)

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFF4F0E6),
                        Color(0xFFECE7D8),
                        Color(0xFFF7F4EC),
                    ),
                ),
            ),
    ) {
        val threeColumnLayout = maxWidth >= 1520.dp

        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            WorkspaceHeader(
                language = language,
                folderCount = appState.folders.size,
                indexedCount = appState.items.size,
                reviewCount = needsReviewCount,
                isServerRunning = appState.isServerRunning,
                isScanning = appState.isScanning,
                statusText = appState.status.text(language),
                statusVisual = statusVisual,
                onLanguageChange = { appState.uiLanguage = it },
            )

            if (threeColumnLayout) {
                Row(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    SidebarColumn(
                        appState = appState,
                        modifier = Modifier.width(380.dp).fillMaxHeight(),
                    )
                    IndexedFilesPanel(
                        appState = appState,
                        language = language,
                        needsReviewCount = needsReviewCount,
                        modifier = Modifier.weight(1.08f).fillMaxHeight(),
                    )
                    MatchDetailsPanel(
                        appState = appState,
                        language = language,
                        modifier = Modifier.weight(0.92f).fillMaxHeight(),
                    )
                }
            } else {
                Row(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    SidebarColumn(
                        appState = appState,
                        modifier = Modifier.widthIn(min = 320.dp, max = 360.dp).fillMaxHeight(),
                    )
                    Column(
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        IndexedFilesPanel(
                            appState = appState,
                            language = language,
                            needsReviewCount = needsReviewCount,
                            modifier = Modifier.weight(0.55f).fillMaxWidth(),
                        )
                        MatchDetailsPanel(
                            appState = appState,
                            language = language,
                            modifier = Modifier.weight(0.45f).fillMaxWidth(),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BridgeTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Color(0xFF2D6A57),
            onPrimary = Color.White,
            secondary = Color(0xFF6C7F68),
            onSecondary = Color.White,
            tertiary = Color(0xFFAA6A2F),
            background = Color(0xFFF6F2E8),
            surface = Color(0xFFFFFCF5),
            surfaceVariant = Color(0xFFE9E3D4),
            onSurface = Color(0xFF1F251F),
            onSurfaceVariant = Color(0xFF5D665F),
            outline = Color(0xFFCAC2B2),
            error = Color(0xFFB94D3E),
        ),
        content = content,
    )
}

@Composable
private fun SidebarColumn(
    appState: AppState,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        LibraryPanel(appState)
        ServerPanel(appState)
    }
}

@Composable
private fun WorkspaceHeader(
    language: UiLanguage,
    folderCount: Int,
    indexedCount: Int,
    reviewCount: Int,
    isServerRunning: Boolean,
    isScanning: Boolean,
    statusText: String,
    statusVisual: StatusVisual,
    onLanguageChange: (UiLanguage) -> Unit,
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(24.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        tr(language, "Animeko Local Media Bridge", "Animeko 本地媒体桥"),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        tr(
                            language,
                            "A denser workspace for scanning folders, correcting episode matches, and exposing them through the local Jellyfin bridge.",
                            "更紧凑的工作台，用来扫描本地文件、修正剧集匹配，并通过本地 Jellyfin 桥接服务提供给 Ani。",
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                    )
                }
                Spacer(Modifier.width(16.dp))
                LanguageSwitcher(language, onLanguageChange)
            }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                StatChip(tr(language, "Folders", "文件夹"), folderCount.toString(), Color(0xFFE2F0E8))
                StatChip(tr(language, "Indexed", "已索引"), indexedCount.toString(), Color(0xFFE6EDF6))
                StatChip(tr(language, "Needs Review", "待检查"), reviewCount.toString(), Color(0xFFF7E7D7))
                StatChip(
                    tr(language, "Scanner", "扫描器"),
                    tr(language, if (isScanning) "Busy" else "Ready", if (isScanning) "扫描中" else "就绪"),
                    if (isScanning) Color(0xFFF3E7D6) else Color(0xFFE8EFE1),
                )
                StatChip(
                    tr(language, "Server", "服务"),
                    tr(language, if (isServerRunning) "Running" else "Stopped", if (isServerRunning) "运行中" else "已停止"),
                    if (isServerRunning) Color(0xFFDFF1E3) else Color(0xFFF0E4DE),
                )
            }
            StatusBanner(
                text = statusText,
                tone = statusVisual,
            )
        }
    }
}

@Composable
private fun LanguageSwitcher(
    language: UiLanguage,
    onLanguageChange: (UiLanguage) -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            tr(language, "Language", "语言"),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = language == UiLanguage.English,
                onClick = { onLanguageChange(UiLanguage.English) },
                label = { Text("EN") },
            )
            FilterChip(
                selected = language == UiLanguage.Chinese,
                onClick = { onLanguageChange(UiLanguage.Chinese) },
                label = { Text("中文") },
            )
        }
    }
}

@Composable
private fun StatChip(label: String, value: String, color: Color) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(18.dp))
            .background(color)
            .padding(horizontal = 12.dp, vertical = 9.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LibraryPanel(appState: AppState) {
    var manualPath by remember { mutableStateOf("") }
    val language = appState.uiLanguage

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SectionHeading(
                title = tr(language, "Library", "媒体库"),
                subtitle = tr(
                    language,
                    "Add folders, paste paths, and scan whenever you are ready.",
                    "添加文件夹、粘贴路径，并在准备好后开始扫描。",
                ),
            )

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Button(onClick = {
                    chooseDirectories(language)?.let(appState::addFolders)
                }) {
                    Icon(Icons.Rounded.FolderOpen, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(tr(language, "Add Folders", "添加文件夹"))
                }
                OutlinedButton(onClick = appState::scan, enabled = !appState.isScanning && appState.folders.isNotEmpty()) {
                    Icon(Icons.Rounded.Refresh, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(tr(language, if (appState.isScanning) "Scanning..." else "Scan Now", if (appState.isScanning) "扫描中..." else "开始扫描"))
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = manualPath,
                    onValueChange = { manualPath = it },
                    label = { Text(tr(language, "Add Folder By Path", "通过路径添加文件夹")) },
                    placeholder = { Text("/Volumes/Anime/迷宫饭") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                )
                OutlinedButton(onClick = {
                    parsePathsFromInput(manualPath).takeIf { it.isNotEmpty() }?.let(appState::addFolders)
                    manualPath = ""
                }) {
                    Icon(Icons.AutoMirrored.Rounded.NoteAdd, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(tr(language, "Add", "添加"))
                }
            }

            if (appState.folders.isEmpty()) {
                PlaceholderMessage(tr(language, "No folders added yet.", "还没有添加文件夹。"))
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 280.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    appState.folders.forEach { folder ->
                        FolderRow(folder = folder, language = language, onRemove = { appState.removeFolder(folder) })
                    }
                }
            }
        }
    }
}

@Composable
private fun IndexedFilesPanel(
    appState: AppState,
    language: UiLanguage,
    needsReviewCount: Int,
    modifier: Modifier = Modifier,
) {
    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SectionHeading(
                title = tr(language, "Indexed Files", "已索引文件"),
                subtitle = tr(
                    language,
                    "${appState.items.size} files indexed, with $needsReviewCount still needing manual review.",
                    "已索引 ${appState.items.size} 个文件，其中 $needsReviewCount 个仍需要人工检查。",
                ),
            )
            HorizontalDivider()
            if (appState.items.isEmpty()) {
                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    PlaceholderMessage(tr(language, "Run a scan to populate the local library.", "开始扫描后，这里会显示本地媒体库中的文件。"))
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(appState.items, key = { it.id }) { item ->
                        IndexedFileCard(
                            item = item,
                            language = language,
                            selected = item.id == appState.selectedItemId,
                            onSelect = { appState.selectedItemId = item.id },
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ServerPanel(appState: AppState) {
    val language = appState.uiLanguage
    var portText by remember(appState.serverSettings.port) { mutableStateOf(appState.serverSettings.port.toString()) }
    var userIdText by remember(appState.serverSettings.userId) { mutableStateOf(appState.serverSettings.userId) }
    var apiKeyText by remember(appState.serverSettings.apiKey) { mutableStateOf(appState.serverSettings.apiKey) }
    var bindExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(portText, userIdText, apiKeyText) {
        appState.serverSettings = appState.serverSettings.copy(
            port = portText.toIntOrNull() ?: appState.serverSettings.port,
            userId = userIdText,
            apiKey = apiKeyText,
        )
    }

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SectionHeading(
                title = tr(language, "Server", "服务"),
                subtitle = tr(
                    language,
                    "Expose scanned files through a small Jellyfin-compatible bridge for this Mac or the local network.",
                    "通过一个轻量 Jellyfin 兼容桥接服务向本机或局域网公开已扫描的文件。",
                ),
            )

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = portText,
                    onValueChange = { portText = it.filter(Char::isDigit) },
                    label = { Text(tr(language, "Port", "端口")) },
                    enabled = !appState.isServerRunning,
                    modifier = Modifier.weight(0.32f),
                    singleLine = true,
                )
                ExposedDropdownMenuBox(
                    expanded = bindExpanded,
                    onExpandedChange = { if (!appState.isServerRunning) bindExpanded = !bindExpanded },
                    modifier = Modifier.weight(0.68f),
                ) {
                    OutlinedTextField(
                        value = appState.serverSettings.bindMode.label(language),
                        onValueChange = {},
                        readOnly = true,
                        enabled = !appState.isServerRunning,
                        label = { Text(tr(language, "Exposure", "访问范围")) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = bindExpanded) },
                        modifier = Modifier.menuAnchor(
                            type = ExposedDropdownMenuAnchorType.PrimaryNotEditable,
                            enabled = !appState.isServerRunning,
                        ).fillMaxWidth(),
                    )
                    DropdownMenu(
                        expanded = bindExpanded,
                        onDismissRequest = { bindExpanded = false },
                    ) {
                        BindMode.entries.forEach { mode ->
                            DropdownMenuItem(
                                text = { Text(mode.label(language)) },
                                onClick = {
                                    bindExpanded = false
                                    appState.serverSettings = appState.serverSettings.copy(bindMode = mode)
                                },
                            )
                        }
                    }
                }
            }

            OutlinedTextField(
                value = userIdText,
                onValueChange = {
                    userIdText = it
                    appState.serverSettings = appState.serverSettings.copy(userId = it)
                },
                label = { Text(tr(language, "User ID", "用户 ID")) },
                enabled = !appState.isServerRunning,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            OutlinedTextField(
                value = apiKeyText,
                onValueChange = {
                    apiKeyText = it
                    appState.serverSettings = appState.serverSettings.copy(apiKey = it)
                },
                label = { Text(tr(language, "API Key", "API 密钥")) },
                enabled = !appState.isServerRunning,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (!appState.isServerRunning) {
                    Button(onClick = appState::startServer, enabled = appState.items.isNotEmpty()) {
                        Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(tr(language, "Start Server", "启动服务"))
                    }
                } else {
                    OutlinedButton(onClick = appState::stopServer) {
                        Icon(Icons.Rounded.Stop, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(tr(language, "Stop Server", "停止服务"))
                    }
                }
                FilterChip(
                    selected = appState.isServerRunning,
                    onClick = {},
                    label = { Text(tr(language, if (appState.isServerRunning) "Online" else "Offline", if (appState.isServerRunning) "在线" else "离线")) },
                )
            }

            if (appState.isServerRunning) {
                ConnectionPanel(
                    urls = appState.visibleUrls(),
                    settings = appState.serverSettings,
                    language = language,
                )
            } else {
                HintCard(
                    tr(
                        language,
                        "Server settings lock while the server is running so the Base URL and API key do not drift out of sync.",
                        "服务运行时会锁定这些设置，避免 Base URL 和 API 密钥在使用中发生变化。",
                    ),
                )
            }
        }
    }
}

@Composable
private fun MatchDetailsPanel(
    appState: AppState,
    language: UiLanguage,
    modifier: Modifier = Modifier,
) {
    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
    ) {
        val selected = appState.selectedItem
        if (selected == null) {
            Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                PlaceholderMessage(tr(language, "Select an indexed file to refine its subject and episode match.", "选择一个已索引文件，以调整它的条目和剧集匹配结果。"))
            }
        } else {
            var subjectName by remember(selected.id) { mutableStateOf(selected.subjectName) }
            var episodeIndexText by remember(selected.id) { mutableStateOf(selected.episodeIndex?.toString().orEmpty()) }
            var episodeName by remember(selected.id) { mutableStateOf(selected.episodeName) }

            Column(
                modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                SectionHeading(
                    title = tr(language, "Match Details", "匹配详情"),
                    subtitle = tr(
                        language,
                        "Review the selected file, compare the scanner guess, and adjust anything that looks off.",
                        "检查当前文件与扫描结果的匹配情况，发现不准确时手动修正。",
                    ),
                )

                OutlinedCard(
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(tr(language, "Selected File", "当前文件"), fontWeight = FontWeight.SemiBold)
                            FilterChip(selected = false, onClick = {}, label = { Text(selected.confidence.label(language)) })
                        }
                        SelectionContainer {
                            Text(
                                selected.file.toString(),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = subjectName,
                    onValueChange = { subjectName = it },
                    label = { Text(tr(language, "Subject Name", "条目名称")) },
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = episodeIndexText,
                        onValueChange = { episodeIndexText = it.filter(Char::isDigit) },
                        label = { Text(tr(language, "Episode Number", "剧集编号")) },
                        modifier = Modifier.weight(0.35f),
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = episodeName,
                        onValueChange = { episodeName = it },
                        label = { Text(tr(language, "Episode Name", "剧集名称")) },
                        modifier = Modifier.weight(0.65f),
                    )
                }

                Text(tr(language, "Aliases", "别名"), fontWeight = FontWeight.SemiBold)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (selected.aliases.isEmpty()) {
                        SoftLabel(tr(language, "No aliases detected", "未检测到别名"))
                    } else {
                        selected.aliases.forEach { SoftLabel(it) }
                    }
                }

                Text(tr(language, "Scanner Notes", "扫描说明"), fontWeight = FontWeight.SemiBold)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    selected.notes.forEach { note ->
                        SoftNote(note.text(language))
                    }
                }
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        appState.updateSelectedItem(
                            subjectName = subjectName,
                            episodeIndex = episodeIndexText.toIntOrNull(),
                            episodeName = episodeName,
                        )
                    },
                ) {
                    Text(tr(language, "Save Match", "保存匹配结果"))
                }
            }
        }
    }
}

@Composable
private fun FolderRow(folder: Path, language: UiLanguage, onRemove: () -> Unit) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SelectionContainer(modifier = Modifier.weight(1f)) {
                Text(
                    folder.toString(),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            TextButton(onClick = onRemove) {
                Text(tr(language, "Remove", "移除"))
            }
        }
    }
}

@Composable
private fun IndexedFileCard(
    item: IndexedMediaFile,
    language: UiLanguage,
    selected: Boolean,
    onSelect: () -> Unit,
) {
    val containerColor = if (selected) Color(0xFFE7F1EB) else MaterialTheme.colorScheme.surface
    val borderColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)

    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onSelect),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, borderColor, RoundedCornerShape(20.dp))
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    item.subjectName.ifBlank { item.filename },
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.width(10.dp))
                FilterChip(selected = false, onClick = {}, label = { Text(item.confidence.label(language)) })
            }
            Text(
                tr(language, "EP ${item.episodeIndex?.toString() ?: "?"} · ${item.episodeName}", "第 ${item.episodeIndex?.toString() ?: "?"} 集 · ${item.episodeName}"),
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                item.filename,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (item.notes.isNotEmpty()) {
                Text(
                    item.notes.first().text(language),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun ConnectionPanel(
    urls: List<String>,
    settings: ServerSettings,
    language: UiLanguage,
) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.outlinedCardColors(containerColor = Color(0xFFF1F7F3)),
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(tr(language, "Connect Ani with these values", "使用这些参数连接 Ani"), fontWeight = FontWeight.SemiBold)
            SelectionContainer(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ConnectionValueRow(
                        label = tr(language, "Base URL", "基础地址"),
                        value = urls.firstOrNull().orEmpty(),
                    )
                    ConnectionValueRow(
                        label = tr(language, "User ID", "用户 ID"),
                        value = settings.userId,
                    )
                    ConnectionValueRow(
                        label = tr(language, "API Key", "API 密钥"),
                        value = settings.apiKey,
                    )
                    urls.drop(1).forEach { url ->
                        ConnectionValueRow(
                            label = tr(language, "LAN URL", "局域网地址"),
                            value = url,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ConnectionValueRow(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            value.ifBlank { "-" },
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun HintCard(text: String) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.outlinedCardColors(containerColor = Color(0xFFFFF7EB)),
    ) {
        Text(
            text,
            modifier = Modifier.padding(14.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SoftLabel(text: String) {
    Text(
        text,
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 10.dp, vertical = 7.dp),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun SoftNote(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(RoundedCornerShape(100))
                .background(MaterialTheme.colorScheme.primary),
        )
        Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun StatusBanner(text: String, tone: StatusVisual) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = tone.container),
    ) {
        Text(
            text,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            color = tone.content,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun PlaceholderMessage(text: String) {
    Text(
        text,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.bodyLarge,
    )
}

@Composable
private fun SectionHeading(title: String, subtitle: String) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private data class StatusVisual(val container: Color, val content: Color)

private fun statusVisual(status: BridgeStatus): StatusVisual {
    return when (status) {
        BridgeStatus.NoValidFolders,
        BridgeStatus.NeedFolder,
        is BridgeStatus.ScanFailed,
        is BridgeStatus.ServerFailed -> StatusVisual(Color(0xFFF7E2DE), Color(0xFF7D3128))

        is BridgeStatus.AddedFolders,
        is BridgeStatus.Indexed,
        is BridgeStatus.MatchUpdated,
        is BridgeStatus.ServerStarted -> StatusVisual(Color(0xFFE1F0E5), Color(0xFF28533B))

        BridgeStatus.Idle,
        is BridgeStatus.RemovedFolder,
        BridgeStatus.ServerStopped -> StatusVisual(Color(0xFFE8E4D8), Color(0xFF4F564D))
    }
}

private fun tr(language: UiLanguage, english: String, chinese: String): String {
    return when (language) {
        UiLanguage.English -> english
        UiLanguage.Chinese -> chinese
    }
}

private fun chooseDirectories(language: UiLanguage): List<Path>? {
    if (isMacOs()) {
        chooseDirectoriesWithNativeDialog(language)?.takeIf { it.isNotEmpty() }?.let { return it }
    }
    val chooser = JFileChooser().apply {
        fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
        isMultiSelectionEnabled = true
        dialogTitle = tr(language, "Choose Media Folders", "选择媒体文件夹")
    }
    val result = chooser.showOpenDialog(null)
    if (result != JFileChooser.APPROVE_OPTION) return null
    return chooser.selectedFiles.map { it.toPath() }
}

private fun chooseDirectoriesWithNativeDialog(language: UiLanguage): List<Path>? {
    val oldValue = System.getProperty("apple.awt.fileDialogForDirectories")
    val frame = Frame()
    return try {
        System.setProperty("apple.awt.fileDialogForDirectories", "true")
        FileDialog(frame, tr(language, "Choose Media Folders", "选择媒体文件夹")).apply {
            isMultipleMode = true
            isVisible = true
        }.files
            ?.map(File::toPath)
            ?.filter { Files.exists(it) }
    } finally {
        frame.dispose()
        if (oldValue == null) {
            System.clearProperty("apple.awt.fileDialogForDirectories")
        } else {
            System.setProperty("apple.awt.fileDialogForDirectories", oldValue)
        }
    }
}

private fun parsePathsFromInput(input: String): List<Path> {
    return input.lineSequence()
        .map { it.trim().trim('"') }
        .filter { it.isNotEmpty() }
        .map { Path.of(it) }
        .toList()
}

private fun isMacOs(): Boolean = System.getProperty("os.name").contains("mac", ignoreCase = true)

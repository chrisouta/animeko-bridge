package me.him188.ani.localbridge

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.nio.file.Files
import java.nio.file.Path

class AppState {
    private val foldersState = mutableStateListOf<Path>()
    private val itemsState = mutableStateListOf<IndexedMediaFile>()

    var selectedItemId by mutableStateOf<String?>(null)
    var uiLanguage by mutableStateOf(UiLanguage.default())
    var isScanning by mutableStateOf(false)
    var status by mutableStateOf<BridgeStatus>(BridgeStatus.Idle)
    var serverSettings by mutableStateOf(ServerSettings())
    var isServerRunning by mutableStateOf(false)

    private var server: LocalJellyfinServer? = null

    val folders: List<Path> get() = foldersState
    val items: List<IndexedMediaFile> get() = itemsState
    val selectedItem: IndexedMediaFile?
        get() = itemsState.firstOrNull { it.id == selectedItemId }

    fun addFolders(paths: List<Path>) {
        val normalized = paths.mapNotNull { path ->
            val absolute = path.toAbsolutePath().normalize()
            when {
                Files.isDirectory(absolute) -> absolute
                Files.isRegularFile(absolute) -> absolute.parent
                else -> null
            }
        }.distinct()

        normalized.filterNot { foldersState.contains(it) }.forEach(foldersState::add)
        status = when {
            normalized.isEmpty() -> BridgeStatus.NoValidFolders
            foldersState.isNotEmpty() -> BridgeStatus.AddedFolders(normalized.size)
            else -> status
        }
    }

    fun removeFolder(path: Path) {
        foldersState.remove(path)
        status = BridgeStatus.RemovedFolder(path.fileName.toString())
    }

    fun scan() {
        if (foldersState.isEmpty()) {
            status = BridgeStatus.NeedFolder
            return
        }
        isScanning = true
        runCatching {
            val report = scanDirectoryTree(foldersState)
            itemsState.clear()
            itemsState.addAll(report.items)
            if (selectedItemId !in itemsState.map { it.id }.toSet()) {
                selectedItemId = itemsState.firstOrNull()?.id
            }
            status = BridgeStatus.Indexed(report.items.size, report.roots.size)
        }.onFailure {
            status = BridgeStatus.ScanFailed(it.message ?: it::class.simpleName.orEmpty())
        }
        isScanning = false
    }

    fun updateSelectedItem(
        subjectName: String,
        episodeIndex: Int?,
        episodeName: String,
    ) {
        val currentId = selectedItemId ?: return
        val index = itemsState.indexOfFirst { it.id == currentId }
        if (index < 0) return
        itemsState[index] = itemsState[index].withEditableValues(subjectName.trim(), episodeIndex, episodeName.trim())
        status = BridgeStatus.MatchUpdated(itemsState[index].filename)
    }

    fun startServer() {
        if (isServerRunning) return
        val instance = LocalJellyfinServer(
            settings = { serverSettings },
            items = { itemsState.toList() },
        )
        runCatching {
            instance.start()
            server = instance
            isServerRunning = true
            status = BridgeStatus.ServerStarted(serverSettings.bindMode)
        }.onFailure {
            instance.close()
            status = BridgeStatus.ServerFailed(it.message ?: it::class.simpleName.orEmpty())
        }
    }

    fun stopServer() {
        server?.stop()
        server = null
        isServerRunning = false
        status = BridgeStatus.ServerStopped
    }

    fun visibleUrls(): List<String> = server?.visibleUrls().orEmpty()
}

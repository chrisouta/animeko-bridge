package me.him188.ani.localbridge

import java.nio.file.Path
import java.util.Locale

enum class UiLanguage {
    English,
    Chinese;

    companion object {
        fun default(): UiLanguage {
            return if (Locale.getDefault().language.startsWith("zh", ignoreCase = true)) {
                Chinese
            } else {
                English
            }
        }
    }
}

enum class MatchConfidence(val score: Int) {
    High(3),
    Medium(2),
    Low(1),
    Unknown(0),
}

fun MatchConfidence.label(language: UiLanguage): String = when (this) {
    MatchConfidence.High -> when (language) {
        UiLanguage.English -> "High"
        UiLanguage.Chinese -> "高"
    }

    MatchConfidence.Medium -> when (language) {
        UiLanguage.English -> "Medium"
        UiLanguage.Chinese -> "中"
    }

    MatchConfidence.Low -> when (language) {
        UiLanguage.English -> "Low"
        UiLanguage.Chinese -> "低"
    }

    MatchConfidence.Unknown -> when (language) {
        UiLanguage.English -> "Needs review"
        UiLanguage.Chinese -> "需要检查"
    }
}

data class IndexedSubtitle(
    val id: String,
    val path: Path,
    val codec: String,
    val language: String? = null,
    val title: String? = null,
)

data class IndexedMediaFile(
    val id: String,
    val file: Path,
    val subjectName: String,
    val episodeIndex: Int?,
    val episodeName: String,
    val aliases: List<String>,
    val subtitles: List<IndexedSubtitle>,
    val confidence: MatchConfidence,
    val notes: List<ScanNote>,
) {
    val filename: String get() = file.fileName.toString()
    val searchableTitles: List<String>
        get() = buildList {
            add(subjectName)
            addAll(aliases)
            add(filename)
        }.distinct()

    fun withEditableValues(
        subjectName: String,
        episodeIndex: Int?,
        episodeName: String,
    ): IndexedMediaFile {
        return copy(
            subjectName = subjectName,
            episodeIndex = episodeIndex,
            episodeName = episodeName,
        )
    }
}

sealed interface ScanNote {
    data object ParsedTitleFromFilename : ScanNote
    data object UsedParentFolderAsTitle : ScanNote
    data object UsedRawFilenameAsTitle : ScanNote
    data class DetectedEpisode(val number: Int) : ScanNote
    data object EpisodeNeedsReview : ScanNote
}

fun ScanNote.text(language: UiLanguage): String = when (this) {
    ScanNote.ParsedTitleFromFilename -> when (language) {
        UiLanguage.English -> "Parsed title from filename"
        UiLanguage.Chinese -> "已从文件名解析标题"
    }

    ScanNote.UsedParentFolderAsTitle -> when (language) {
        UiLanguage.English -> "Used parent folder as title"
        UiLanguage.Chinese -> "已使用上级文件夹作为标题"
    }

    ScanNote.UsedRawFilenameAsTitle -> when (language) {
        UiLanguage.English -> "Used raw filename as title"
        UiLanguage.Chinese -> "已使用原始文件名作为标题"
    }

    is ScanNote.DetectedEpisode -> when (language) {
        UiLanguage.English -> "Detected episode $number"
        UiLanguage.Chinese -> "已识别第 $number 集"
    }

    ScanNote.EpisodeNeedsReview -> when (language) {
        UiLanguage.English -> "Episode number needs review"
        UiLanguage.Chinese -> "集数需要人工检查"
    }
}

data class ScanReport(
    val roots: List<Path>,
    val items: List<IndexedMediaFile>,
    val skippedFiles: List<Path>,
)

enum class BindMode(val host: String, val label: String) {
    Loopback("127.0.0.1", "This device only"),
    Lan("0.0.0.0", "Local network"),
}

fun BindMode.label(language: UiLanguage): String = when (this) {
    BindMode.Loopback -> when (language) {
        UiLanguage.English -> "This device only"
        UiLanguage.Chinese -> "仅本机"
    }

    BindMode.Lan -> when (language) {
        UiLanguage.English -> "Local network"
        UiLanguage.Chinese -> "局域网"
    }
}

data class ServerSettings(
    val port: Int = 8096,
    val userId: String = "localbridge",
    val apiKey: String = "animeko-local-bridge",
    val bindMode: BindMode = BindMode.Loopback,
)

sealed interface BridgeStatus {
    data object Idle : BridgeStatus
    data class AddedFolders(val count: Int) : BridgeStatus
    data object NoValidFolders : BridgeStatus
    data class RemovedFolder(val name: String) : BridgeStatus
    data object NeedFolder : BridgeStatus
    data class Indexed(val mediaCount: Int, val folderCount: Int) : BridgeStatus
    data class ScanFailed(val message: String) : BridgeStatus
    data class MatchUpdated(val filename: String) : BridgeStatus
    data class ServerStarted(val bindMode: BindMode) : BridgeStatus
    data class ServerFailed(val message: String) : BridgeStatus
    data object ServerStopped : BridgeStatus
}

fun BridgeStatus.text(language: UiLanguage): String = when (this) {
    BridgeStatus.Idle -> when (language) {
        UiLanguage.English -> "Choose one or more folders, then scan them."
        UiLanguage.Chinese -> "先添加一个或多个文件夹，然后开始扫描。"
    }

    is BridgeStatus.AddedFolders -> when (language) {
        UiLanguage.English -> "Added $count folder(s). Run a scan when you are ready."
        UiLanguage.Chinese -> "已添加 $count 个文件夹。准备好后开始扫描。"
    }

    BridgeStatus.NoValidFolders -> when (language) {
        UiLanguage.English -> "No valid folders were added."
        UiLanguage.Chinese -> "没有添加有效的文件夹。"
    }

    is BridgeStatus.RemovedFolder -> when (language) {
        UiLanguage.English -> "Removed $name."
        UiLanguage.Chinese -> "已移除 $name。"
    }

    BridgeStatus.NeedFolder -> when (language) {
        UiLanguage.English -> "Add at least one folder first."
        UiLanguage.Chinese -> "请先添加至少一个文件夹。"
    }

    is BridgeStatus.Indexed -> when (language) {
        UiLanguage.English -> "Indexed $mediaCount media files from $folderCount folder(s)."
        UiLanguage.Chinese -> "已从 $folderCount 个文件夹中索引 $mediaCount 个媒体文件。"
    }

    is BridgeStatus.ScanFailed -> when (language) {
        UiLanguage.English -> "Scan failed: $message"
        UiLanguage.Chinese -> "扫描失败：$message"
    }

    is BridgeStatus.MatchUpdated -> when (language) {
        UiLanguage.English -> "Updated match for $filename."
        UiLanguage.Chinese -> "已更新 $filename 的匹配结果。"
    }

    is BridgeStatus.ServerStarted -> when (language) {
        UiLanguage.English -> "Server started on ${bindMode.label(language).lowercase()}."
        UiLanguage.Chinese -> "服务已启动，当前模式：${bindMode.label(language)}。"
    }

    is BridgeStatus.ServerFailed -> when (language) {
        UiLanguage.English -> "Server failed to start: $message"
        UiLanguage.Chinese -> "服务启动失败：$message"
    }

    BridgeStatus.ServerStopped -> when (language) {
        UiLanguage.English -> "Server stopped."
        UiLanguage.Chinese -> "服务已停止。"
    }
}

package com.jian.relive.core.common.preferences

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.jian.relive.core.common.language.AppLanguage
import com.jian.relive.core.model.AppStats
import com.jian.relive.core.model.CompressionResumeState
import java.util.Locale

/**
 * 智能相册筛选规则配置。
 *
 * @property dateRange 时间范围过滤条件。
 * @property location 地理位置关键字。
 * @property keywords 分类识别标签关键字列表。
 */
data class AlbumRules(
    val dateRange: DateRange? = null,
    val location: String? = null,
    val keywords: List<String> = emptyList()
)

/**
 * 时间跨度区间。
 *
 * @property start 起始毫秒时间戳。
 * @property end 截止毫秒时间戳。
 */
data class DateRange(
    val start: Long,
    val end: Long
)

/**
 * 自定义智能相册定义。
 *
 * @property id 唯一标识。
 * @property name 相册名称。
 * @property rules 过滤规则集合。
 */
data class SmartAlbumDefinition(
    val id: String,
    val name: String,
    val rules: AlbumRules
)

/**
 * 应用程序偏好与状态持久化管理器。
 *
 * @param context 上下文引用。
 */
class PreferencesManager(context: Context) {
    private val prefs = context.getSharedPreferences("photoo_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    private var _stats: AppStats? = null
    private var _processedIds: MutableSet<Long>? = null
    private var _pendingTrashIds: MutableSet<Long>? = null
    private var _likedIds: MutableSet<Long>? = null
    private var _comments: MutableMap<Long, List<String>>? = null
    private var _lowQualityResults: MutableMap<Long, String>? = null
    private var _language: AppLanguage? = null
    private var _muteVideos: Boolean? = null
    private var _hapticsEnabled: Boolean? = null
    private var _danmakuEnabled: Boolean? = null
    private var _videoCompressBitrateKey: String? = null
    private var _imageCompressThresholdMb: Int? = null
    private var _imageCompressQualityKey: String? = null
    private var _imageCompressResume: CompressionResumeState? = null
    private var _videoCompressResume: CompressionResumeState? = null
    private var _onboardingCompleted: Boolean? = null
    private var _movedIds: MutableSet<Long>? = null
    private var _userCreatedFolders: MutableSet<String>? = null
    private var _scannedLowQualityIds: MutableSet<Long>? = null
    private var _folderUsage: MutableMap<String, Int>? = null
    private var _folderLastUsed: MutableMap<String, Long>? = null
    private var _likesFolderSelectionCounts: MutableMap<String, Int>? = null
    private var _smartAlbums: MutableList<SmartAlbumDefinition>? = null

    var stats: AppStats
        get() {
            if (_stats == null) {
                val json = prefs.getString("stats", null)
                _stats = try {
                    if (json != null) gson.fromJson(json, AppStats::class.java) else AppStats()
                } catch (e: Exception) {
                    AppStats()
                }
            }
            return _stats!!
        }
        set(value) {
            _stats = value
            prefs.edit().putString("stats", gson.toJson(value)).apply()
        }

    fun reloadStats() {
        _stats = null
    }

    var processedIds: MutableSet<Long>
        get() {
            if (_processedIds == null) {
                val json = prefs.getString("processed_ids", null)
                val type = object : TypeToken<HashSet<Long>>() {}.type
                _processedIds = try {
                    if (json != null) gson.fromJson(json, type) else HashSet()
                } catch (e: Exception) {
                    HashSet()
                }
            }
            return _processedIds!!
        }
        set(value) {
            _processedIds = value
            prefs.edit().putString("processed_ids", gson.toJson(value)).apply()
        }

    var pendingTrashIds: MutableSet<Long>
        get() {
            if (_pendingTrashIds == null) {
                val json = prefs.getString("pending_trash", null)
                val type = object : TypeToken<HashSet<Long>>() {}.type
                _pendingTrashIds = try {
                    if (json != null) gson.fromJson(json, type) else HashSet()
                } catch (e: Exception) {
                    HashSet()
                }
            }
            return _pendingTrashIds!!
        }
        set(value) {
            _pendingTrashIds = value
            prefs.edit().putString("pending_trash", gson.toJson(value)).apply()
        }

    var likedIds: MutableSet<Long>
        get() {
            if (_likedIds == null) {
                val json = prefs.getString("liked_ids", null)
                val type = object : TypeToken<HashSet<Long>>() {}.type
                _likedIds = try {
                    if (json != null) gson.fromJson(json, type) else HashSet()
                } catch (e: Exception) {
                    HashSet()
                }
            }
            return _likedIds!!
        }
        set(value) {
            _likedIds = value
            prefs.edit().putString("liked_ids", gson.toJson(value)).apply()
        }

    var movedIds: MutableSet<Long>
        get() {
            if (_movedIds == null) {
                val json = prefs.getString("moved_ids", null)
                val type = object : TypeToken<HashSet<Long>>() {}.type
                _movedIds = try {
                    if (json != null) gson.fromJson(json, type) else HashSet()
                } catch (e: Exception) {
                    HashSet()
                }
            }
            return _movedIds!!
        }
        set(value) {
            _movedIds = value
            prefs.edit().putString("moved_ids", gson.toJson(value)).apply()
        }

    var userCreatedFolders: MutableSet<String>
        get() {
            if (_userCreatedFolders == null) {
                val json = prefs.getString("user_created_folders", null)
                val type = object : TypeToken<HashSet<String>>() {}.type
                _userCreatedFolders = try {
                    if (json != null) gson.fromJson(json, type) else HashSet()
                } catch (e: Exception) {
                    HashSet()
                }
            }
            return _userCreatedFolders!!
        }
        set(value) {
            _userCreatedFolders = value
            prefs.edit().putString("user_created_folders", gson.toJson(value)).apply()
        }

    var lowQualityResults: MutableMap<Long, String>
        get() {
            if (_lowQualityResults == null) {
                val json = prefs.getString("low_quality_results", null)
                val type = object : TypeToken<HashMap<Long, String>>() {}.type
                _lowQualityResults = try {
                    if (json != null) gson.fromJson(json, type) else HashMap()
                } catch (e: Exception) {
                    HashMap()
                }
            }
            return _lowQualityResults!!
        }
        set(value) {
            _lowQualityResults = value
            prefs.edit().putString("low_quality_results", gson.toJson(value)).apply()
        }

    var scannedLowQualityIds: MutableSet<Long>
        get() {
            if (_scannedLowQualityIds == null) {
                val json = prefs.getString("scanned_low_quality_ids", null)
                val type = object : TypeToken<HashSet<Long>>() {}.type
                _scannedLowQualityIds = try {
                    if (json != null) gson.fromJson(json, type) else HashSet()
                } catch (e: Exception) {
                    HashSet()
                }
            }
            return _scannedLowQualityIds!!
        }
        set(value) {
            _scannedLowQualityIds = value
            prefs.edit().putString("scanned_low_quality_ids", gson.toJson(value)).apply()
        }

    var folderUsage: MutableMap<String, Int>
        get() {
            if (_folderUsage == null) {
                val json = prefs.getString("folder_usage", null)
                val type = object : TypeToken<HashMap<String, Int>>() {}.type
                _folderUsage = try {
                    if (json != null) gson.fromJson(json, type) else HashMap()
                } catch (e: Exception) {
                    HashMap()
                }
            }
            return _folderUsage!!
        }
        set(value) {
            _folderUsage = value
            prefs.edit().putString("folder_usage", gson.toJson(value)).apply()
        }

    var folderLastUsed: MutableMap<String, Long>
        get() {
            if (_folderLastUsed == null) {
                val json = prefs.getString("folder_last_used", null)
                val type = object : TypeToken<HashMap<String, Long>>() {}.type
                _folderLastUsed = try {
                    if (json != null) gson.fromJson(json, type) else HashMap()
                } catch (e: Exception) {
                    HashMap()
                }
            }
            return _folderLastUsed!!
        }
        set(value) {
            _folderLastUsed = value
            prefs.edit().putString("folder_last_used", gson.toJson(value)).apply()
        }

    var likesFolderSelectionCounts: MutableMap<String, Int>
        get() {
            if (_likesFolderSelectionCounts == null) {
                val json = prefs.getString("likes_folder_selection_counts", null)
                val type = object : TypeToken<HashMap<String, Int>>() {}.type
                _likesFolderSelectionCounts = try {
                    if (json != null) gson.fromJson(json, type) else HashMap()
                } catch (e: Exception) {
                    HashMap()
                }
            }
            return _likesFolderSelectionCounts!!
        }
        set(value) {
            _likesFolderSelectionCounts = value
            prefs.edit().putString("likes_folder_selection_counts", gson.toJson(value)).apply()
        }

    var smartAlbums: MutableList<SmartAlbumDefinition>
        get() {
            if (_smartAlbums == null) {
                val json = prefs.getString("smart_albums", null)
                val type = object : TypeToken<ArrayList<SmartAlbumDefinition>>() {}.type
                _smartAlbums = try {
                    if (json != null) gson.fromJson(json, type) else ArrayList()
                } catch (e: Exception) {
                    ArrayList()
                }
            }
            return _smartAlbums!!
        }
        set(value) {
            _smartAlbums = value
            prefs.edit().putString("smart_albums", gson.toJson(value)).apply()
        }

    var danmakuEnabled: Boolean
        get() {
            if (_danmakuEnabled == null) {
                _danmakuEnabled = prefs.getBoolean("danmaku_enabled", false)
            }
            return _danmakuEnabled!!
        }
        set(value) {
            _danmakuEnabled = value
            prefs.edit().putBoolean("danmaku_enabled", value).apply()
        }

    var videoCompressBitrateKey: String
        get() {
            if (_videoCompressBitrateKey == null) {
                _videoCompressBitrateKey = prefs.getString("video_compress_bitrate_key", "balanced") ?: "balanced"
            }
            return _videoCompressBitrateKey!!
        }
        set(value) {
            _videoCompressBitrateKey = value
            prefs.edit().putString("video_compress_bitrate_key", value).apply()
        }

    var imageCompressThresholdMb: Int
        get() {
            if (_imageCompressThresholdMb == null) {
                _imageCompressThresholdMb = prefs.getInt("image_compress_threshold_mb", 3)
            }
            return _imageCompressThresholdMb!!
        }
        set(value) {
            _imageCompressThresholdMb = value
            prefs.edit().putInt("image_compress_threshold_mb", value).apply()
        }

    var imageCompressQualityKey: String
        get() {
            if (_imageCompressQualityKey == null) {
                _imageCompressQualityKey = prefs.getString("image_compress_quality_key", "balanced") ?: "balanced"
            }
            return _imageCompressQualityKey!!
        }
        set(value) {
            _imageCompressQualityKey = value
            prefs.edit().putString("image_compress_quality_key", value).apply()
        }

    var imageCompressResume: CompressionResumeState
        get() {
            if (_imageCompressResume == null) {
                val json = prefs.getString("image_compress_resume", null)
                val type = object : TypeToken<CompressionResumeState>() {}.type
                _imageCompressResume = try {
                    if (json != null) gson.fromJson(json, type) else CompressionResumeState()
                } catch (e: Exception) {
                    CompressionResumeState()
                }
            }
            return _imageCompressResume!!
        }
        set(value) {
            _imageCompressResume = value
            prefs.edit().putString("image_compress_resume", gson.toJson(value)).apply()
        }

    var videoCompressResume: CompressionResumeState
        get() {
            if (_videoCompressResume == null) {
                val json = prefs.getString("video_compress_resume", null)
                val type = object : TypeToken<CompressionResumeState>() {}.type
                _videoCompressResume = try {
                    if (json != null) gson.fromJson(json, type) else CompressionResumeState()
                } catch (e: Exception) {
                    CompressionResumeState()
                }
            }
            return _videoCompressResume!!
        }
        set(value) {
            _videoCompressResume = value
            prefs.edit().putString("video_compress_resume", gson.toJson(value)).apply()
        }

    var onboardingCompleted: Boolean
        get() {
            if (_onboardingCompleted == null) {
                _onboardingCompleted = prefs.getBoolean("onboarding_completed", false)
            }
            return _onboardingCompleted!!
        }
        set(value) {
            _onboardingCompleted = value
            prefs.edit().putBoolean("onboarding_completed", value).apply()
        }

    var language: AppLanguage
        get() {
            if (_language == null) {
                val code = prefs.getString("language_code", null)
                _language = if (code != null) {
                    val migratedCode = when (code) {
                        "zh-CN" -> "zh-SC"
                        "zh-TW" -> "zh-TC"
                        else -> code
                    }
                    AppLanguage.entries.find { it.code == migratedCode } ?: AppLanguage.English
                } else {
                    val sysLang = Locale.getDefault().language
                    when (sysLang) {
                        "zh" -> {
                            val country = Locale.getDefault().country
                            if (country == "TW" || country == "HK" || country == "MO") AppLanguage.TraditionalChinese else AppLanguage.SimplifiedChinese
                        }
                        "ko" -> AppLanguage.Korean
                        "ja" -> AppLanguage.Japanese
                        "fr" -> AppLanguage.French
                        "de" -> AppLanguage.German
                        "it" -> AppLanguage.Italian
                        "pt" -> AppLanguage.Portuguese
                        "es" -> AppLanguage.Spanish
                        "ru" -> AppLanguage.Russian
                        "th" -> AppLanguage.Thai
                        "vi" -> AppLanguage.Vietnamese
                        "id" -> AppLanguage.Indonesian
                        "ar" -> AppLanguage.Arabic
                        else -> AppLanguage.English
                    }
                }
            }
            return _language!!
        }
        set(value) {
            _language = value
            prefs.edit().putString("language_code", value.code).apply()
        }

    var muteVideos: Boolean
        get() {
            if (_muteVideos == null) {
                _muteVideos = prefs.getBoolean("mute_videos", true)
            }
            return _muteVideos!!
        }
        set(value) {
            _muteVideos = value
            prefs.edit().putBoolean("mute_videos", value).apply()
        }

    var soundEffectsEnabled: Boolean
        get() = prefs.getBoolean("sound_effects_enabled", true)
        set(value) = prefs.edit().putBoolean("sound_effects_enabled", value).apply()

    var hapticsEnabled: Boolean
        get() {
            if (_hapticsEnabled == null) {
                _hapticsEnabled = prefs.getBoolean("haptics_enabled", true)
            }
            return _hapticsEnabled!!
        }
        set(value) {
            _hapticsEnabled = value
            prefs.edit().putBoolean("haptics_enabled", value).apply()
        }

    var avatarType: String
        get() = prefs.getString("avatar_type", "girl") ?: "girl"
        set(value) = prefs.edit().putString("avatar_type", value).apply()

    var batchSize: Int
        get() = prefs.getInt("batch_size", 16)
        set(value) = prefs.edit().putInt("batch_size", value).apply()

    var queueOrder: String
        get() = prefs.getString("queue_order", "random") ?: "random"
        set(value) = prefs.edit().putString("queue_order", value).apply()

    var filterFolder: String
        get() = prefs.getString("filter_folder", "All") ?: "All"
        set(value) = prefs.edit().putString("filter_folder", value).apply()

    var filterType: String
        get() = prefs.getString("filter_type", "All") ?: "All"
        set(value) = prefs.edit().putString("filter_type", value).apply()

    var filterMonth: String
        get() = prefs.getString("filter_month", "All") ?: "All"
        set(value) = prefs.edit().putString("filter_month", value).apply()

    var comments: MutableMap<Long, List<String>>
        get() {
            if (_comments == null) {
                val json = prefs.getString("comments", null)
                val type = object : TypeToken<HashMap<Long, List<String>>>() {}.type
                _comments = try {
                    if (json != null) gson.fromJson(json, type) else HashMap()
                } catch (e: Exception) {
                    HashMap()
                }
            }
            return _comments!!
        }
        set(value) {
            _comments = value
            prefs.edit().putString("comments", gson.toJson(value)).apply()
        }

    fun saveProcessedIdsToDisk(ids: Set<Long>) {
        prefs.edit().putString("processed_ids", gson.toJson(ids)).apply()
    }

    fun savePendingTrashIdsToDisk(ids: Set<Long>) {
        prefs.edit().putString("pending_trash", gson.toJson(ids)).apply()
    }

    fun saveLowQualityResultsToDisk(results: Map<Long, String>) {
        prefs.edit().putString("low_quality_results", gson.toJson(results)).apply()
    }

    fun saveScannedLowQualityIdsToDisk(ids: Set<Long>) {
        prefs.edit().putString("scanned_low_quality_ids", gson.toJson(ids)).apply()
    }
}

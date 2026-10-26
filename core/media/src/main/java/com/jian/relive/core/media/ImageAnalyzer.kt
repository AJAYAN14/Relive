package com.jian.relive.core.media

import android.content.Context
import android.net.Uri
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.Locale

/**
 * 端侧图像标签视觉分析器。
 *
 * 集成 ML Kit 视觉分析引擎，支持提取图片内容类别标签并进行多语言字典映射。
 */
object ImageAnalyzer {
    private var labelMap: Map<String, Map<String, String>>? = null

    private fun ensureMapLoaded(context: Context) {
        if (labelMap != null) return

        try {
            val json = context.assets.open("imagenet_labels_localized.json").bufferedReader().use { it.readText() }
            val type = object : TypeToken<Map<String, Map<String, String>>>() {}.type
            labelMap = Gson().fromJson(json, type)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            labelMap = emptyMap()
        }
    }

    /**
     * 对指定图片 URI 执行离线标签分析。
     *
     * @param context 上下文。
     * @param uri 待分析媒体 URI。
     * @param langCode 目标本地化语言代码，默认 "en"。
     * @param coroutineDispatcher 协程调度器，默认 Dispatchers.IO。
     * @return 过滤后置信度前 3 的分类标签列表。
     */
    suspend fun analyze(
        context: Context,
        uri: Uri,
        langCode: String = "en",
        coroutineDispatcher: CoroutineDispatcher = Dispatchers.IO
    ): List<String> {
        return withContext(coroutineDispatcher) {
            ensureMapLoaded(context)
            try {
                val image = InputImage.fromFilePath(context, uri)
                val labeler = ImageLabeling.getClient(ImageLabelerOptions.DEFAULT_OPTIONS)
                val labels = labeler.process(image).await()

                labels.filter { it.confidence > 0.75f }
                    .take(3)
                    .map { label ->
                        val text = label.text
                        val lowerText = text.lowercase(Locale.ROOT)
                        if (langCode == "en") {
                            text
                        } else {
                            val translations = labelMap?.get(lowerText)
                            val mapped = translations?.get(langCode)
                            mapped ?: text
                        }
                    }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                emptyList()
            }
        }
    }
}

package com.jian.relive.core.common.language

/**
 * 应用程序支持的语言枚举。
 *
 * @property code 语言代码标识符。
 * @property displayName 语言原生名称展示文本。
 */
enum class AppLanguage(val code: String, val displayName: String) {
    English("en", "English"),
    SimplifiedChinese("zh-SC", "简体中文"),
    TraditionalChinese("zh-TC", "繁体中文"),
    Korean("ko", "한국어"),
    Japanese("ja", "日本語"),
    French("fr", "Français"),
    German("de", "Deutsch"),
    Italian("it", "Italiano"),
    Portuguese("pt", "Português"),
    Spanish("es", "Español"),
    Russian("ru", "Русский"),
    Thai("th", "ไทย"),
    Vietnamese("vi", "Tiếng Việt"),
    Indonesian("id", "Bahasa Indonesia"),
    Arabic("ar", "العربية")
}

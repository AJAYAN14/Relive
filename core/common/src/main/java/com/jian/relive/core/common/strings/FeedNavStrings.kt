package com.jian.relive.core.common.strings

import com.jian.relive.core.common.language.AppLanguage

/**
 * 实况媒体流导航与操作文案。
 */
data class FeedNavStrings(
    val navOrganize: String,
    val navLiveFeed: String,
    val navStats: String,
    val navSettings: String,
    val feedFilterAll: String,
    val feedFilterLive: String,
    val feedFilterVideo: String,
    val feedEmptyTitle: String,
    val feedEmptySubtitle: String,
    val soundMuted: String,
    val soundUnmuted: String,
    val feedDeleteConfirmTitle: String,
    val feedDeleteConfirmMessage: String,
    val feedDeleteButton: String,
    val feedCancelButton: String,
    val feedDeleteSuccess: String
)

/**
 * 根据应用语言获取实况流对应的展示文案集合。
 *
 * @param lang 当前应用语言枚举。
 * @return 对应的文案数据结构。
 */
fun getFeedNavStrings(lang: AppLanguage): FeedNavStrings {
    return when (lang) {
        AppLanguage.SimplifiedChinese -> FeedNavStrings(
            navOrganize = "整理",
            navLiveFeed = "实况流",
            navStats = "统计",
            navSettings = "设置",
            feedFilterAll = "全部",
            feedFilterLive = "实况",
            feedFilterVideo = "视频",
            feedEmptyTitle = "暂无实况或视频",
            feedEmptySubtitle = "相册中的实况照片与视频将呈现在这里",
            soundMuted = "已静音",
            soundUnmuted = "已开启声音",
            feedDeleteConfirmTitle = "删除此项目？",
            feedDeleteConfirmMessage = "确定要将此实况照片/视频移入废纸篓吗？",
            feedDeleteButton = "移至废纸篓",
            feedCancelButton = "取消",
            feedDeleteSuccess = "已移入废纸篓"
        )
        AppLanguage.TraditionalChinese -> FeedNavStrings(
            navOrganize = "整理",
            navLiveFeed = "實況流",
            navStats = "統計",
            navSettings = "設定",
            feedFilterAll = "全部",
            feedFilterLive = "實況",
            feedFilterVideo = "影片",
            feedEmptyTitle = "暫無實況或影片",
            feedEmptySubtitle = "相簿中的實況相片與影片將呈現在這裡",
            soundMuted = "已靜音",
            soundUnmuted = "已開啟聲音",
            feedDeleteConfirmTitle = "刪除此項目？",
            feedDeleteConfirmMessage = "確定要將此實況相片/影片移入垃圾桶嗎？",
            feedDeleteButton = "移至垃圾桶",
            feedCancelButton = "取消",
            feedDeleteSuccess = "已移入垃圾桶"
        )
        AppLanguage.Japanese -> FeedNavStrings(
            navOrganize = "整理",
            navLiveFeed = "ライブフィード",
            navStats = "統計",
            navSettings = "設定",
            feedFilterAll = "すべて",
            feedFilterLive = "ライブ",
            feedFilterVideo = "動画",
            feedEmptyTitle = "ライブ写真や動画はありません",
            feedEmptySubtitle = "アルバム内のライブ写真と動画がここに表示されます",
            soundMuted = "ミュート中",
            soundUnmuted = "ミュート解除",
            feedDeleteConfirmTitle = "この項目を削除しますか？",
            feedDeleteConfirmMessage = "このライブ写真/動画をゴミ箱に移動してもよろしいですか？",
            feedDeleteButton = "ゴミ箱に移動",
            feedCancelButton = "キャンセル",
            feedDeleteSuccess = "ゴミ箱に移動しました"
        )
        AppLanguage.Korean -> FeedNavStrings(
            navOrganize = "정리",
            navLiveFeed = "라이브 피드",
            navStats = "통계",
            navSettings = "설정",
            feedFilterAll = "전체",
            feedFilterLive = "라이브",
            feedFilterVideo = "비디오",
            feedEmptyTitle = "라이브 사진 또는 비디오가 없습니다",
            feedEmptySubtitle = "앨범의 라이브 사진과 비디오가 여기에 표시됩니다",
            soundMuted = "음소거됨",
            soundUnmuted = "소리 켜짐",
            feedDeleteConfirmTitle = "이 항목을 삭제하시겠습니까?",
            feedDeleteConfirmMessage = "이 라이브 사진/비디오를 휴지통으로 이동하시겠습니까?",
            feedDeleteButton = "휴지통으로 이동",
            feedCancelButton = "취소",
            feedDeleteSuccess = "휴지통으로 이동됨"
        )
        else -> FeedNavStrings(
            navOrganize = "Organize",
            navLiveFeed = "Live Feed",
            navStats = "Stats",
            navSettings = "Settings",
            feedFilterAll = "All",
            feedFilterLive = "Live",
            feedFilterVideo = "Video",
            feedEmptyTitle = "No Live Photos or Videos",
            feedEmptySubtitle = "Live photos and videos from your library will appear here",
            soundMuted = "Muted",
            soundUnmuted = "Unmuted",
            feedDeleteConfirmTitle = "Delete this item?",
            feedDeleteConfirmMessage = "Are you sure you want to move this live photo/video to trash?",
            feedDeleteButton = "Move to Trash",
            feedCancelButton = "Cancel",
            feedDeleteSuccess = "Moved to Trash"
        )
    }
}

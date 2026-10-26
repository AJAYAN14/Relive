package com.jian.relive.feature.organize.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CreateNewFolder
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jian.relive.core.ui.theme.ReliveAmber500
import com.jian.relive.core.ui.theme.ReliveBlack
import com.jian.relive.core.ui.theme.ReliveBlue50
import com.jian.relive.core.ui.theme.ReliveBlue500
import com.jian.relive.core.ui.theme.ReliveWhite
import com.jian.relive.feature.organize.R
import com.jian.relive.feature.organize.model.FolderData

/**
 * 拖拽相册归类投放横向栏组件。
 *
 * 当卡片向下拖动时滑入展示，包含“新建相册”卡片及常用相册目标，上报全局矩形位置以支持手势落点碰撞检测与弹簧放大动效。
 */
@Composable
fun FolderDropRow(
    visible: Boolean,
    folders: List<FolderData>,
    activeDropTargetId: String?,
    onFolderPositionsChanged: (Map<String, Rect>) -> Unit,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState()
) {
    val rects = remember { mutableStateMapOf<String, Rect>() }

    LaunchedEffect(visible) {
        if (!visible) {
            rects.clear()
            onFolderPositionsChanged(emptyMap())
        }
    }

    val updateRect = remember {
        { id: String, rect: Rect ->
            rects[id] = rect
            onFolderPositionsChanged(rects.toMap())
        }
    }

    val removeRect = remember {
        { id: String ->
            if (rects.remove(id) != null) {
                onFolderPositionsChanged(rects.toMap())
            }
        }
    }

    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(animationSpec = tween(200)) { it } + fadeIn(animationSpec = tween(200)),
        exit = slideOutVertically(animationSpec = tween(200)) { it } + fadeOut(animationSpec = tween(200)),
        modifier = modifier
    ) {
        LazyRow(
            state = listState,
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            item(key = "new_folder") {
                val newFolderTitle = stringResource(R.string.organize_new_folder)
                DisposableEffect(Unit) {
                    onDispose { removeRect("new_folder") }
                }
                FolderDropItem(
                    folder = FolderData("new_folder", newFolderTitle, null, 0),
                    isActive = activeDropTargetId == "new_folder",
                    onPositioned = { rect ->
                        updateRect("new_folder", rect)
                    }
                )
            }

            items(folders, key = { it.id }) { folder ->
                DisposableEffect(folder.id) {
                    onDispose { removeRect(folder.id) }
                }
                FolderDropItem(
                    folder = folder,
                    isActive = activeDropTargetId == folder.id,
                    onPositioned = { rect ->
                        updateRect(folder.id, rect)
                    }
                )
            }
        }
    }
}

/**
 * 单个归类目标相册项。
 */
@Composable
private fun FolderDropItem(
    folder: FolderData,
    isActive: Boolean,
    onPositioned: (Rect) -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (isActive) 1.2f else 0.85f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 320f),
        label = "FolderDropScale"
    )
    val lift by animateDpAsState(
        targetValue = if (isActive) (-8).dp else 0.dp,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 320f),
        label = "FolderDropLift"
    )
    val itemWidth by animateDpAsState(
        targetValue = if (isActive) 88.dp else 68.dp,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 320f),
        label = "FolderDropWidth"
    )
    val borderColor = if (isActive) ReliveBlue500 else Color.Transparent
    val backgroundColor = if (isActive) ReliveBlue50 else ReliveWhite

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .offset(y = lift)
            .scale(scale)
            .width(itemWidth)
            .onGloballyPositioned { coordinates ->
                val position = coordinates.positionInRoot()
                val size = coordinates.size
                onPositioned(
                    Rect(
                        left = position.x,
                        top = position.y,
                        right = position.x + size.width,
                        bottom = position.y + size.height
                    )
                )
            }
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(backgroundColor)
                .border(2.dp, borderColor, RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (folder.id == "new_folder") {
                Icon(
                    imageVector = Icons.Outlined.CreateNewFolder,
                    contentDescription = stringResource(R.string.organize_new_folder),
                    tint = if (isActive) ReliveBlue500 else ReliveBlack.copy(alpha = 0.6f),
                    modifier = Modifier.size(32.dp)
                )
            } else {
                Icon(
                    imageVector = Icons.Outlined.Folder,
                    contentDescription = folder.name,
                    tint = if (isActive) ReliveBlue500 else ReliveAmber500,
                    modifier = Modifier.size(32.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = folder.name,
            fontSize = 12.sp,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
            color = if (isActive) ReliveBlue500 else ReliveBlack.copy(alpha = 0.8f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        if (folder.count > 0) {
            Text(
                text = "${folder.count}",
                fontSize = 10.sp,
                color = ReliveBlack.copy(alpha = 0.4f),
                textAlign = TextAlign.Center
            )
        }
    }
}

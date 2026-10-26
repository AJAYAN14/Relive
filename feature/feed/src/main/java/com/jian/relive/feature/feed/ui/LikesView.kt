package com.jian.relive.feature.feed.ui

import android.location.Geocoder
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.HeartBroken
import androidx.compose.material.icons.outlined.Notes
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.exifinterface.media.ExifInterface
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.jian.relive.core.common.preferences.PreferencesManager
import com.jian.relive.core.media.MediaShareHelper
import com.jian.relive.core.model.MediaItem
import com.jian.relive.core.ui.animation.bounceClick
import com.jian.relive.core.ui.theme.ReliveBlack
import com.jian.relive.core.ui.theme.ReliveGray100
import com.jian.relive.core.ui.theme.ReliveGray500
import com.jian.relive.core.ui.theme.ReliveGray600
import com.jian.relive.core.ui.theme.ReliveRed500
import com.jian.relive.core.ui.theme.ReliveRed600
import com.jian.relive.core.ui.theme.ReliveWhite
import com.jian.relive.feature.feed.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.roundToInt

/**
 * 喜欢与收藏照片沉浸式全屏浏览视图。
 *
 * 支持纵向分页翻滚、实况视频循环播放、按相册筛选、弹幕评论随记互动以及系统分享与移除喜欢。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LikesView(
    likedItems: List<MediaItem>,
    allMedia: List<MediaItem>,
    onBack: () -> Unit,
    onUnlike: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val prefs = remember { PreferencesManager(context) }
    val scope = rememberCoroutineScope()
    val topInset = 48.dp

    var danmakuEnabled by remember { mutableStateOf(prefs.danmakuEnabled) }
    var selectedBucketName by remember { mutableStateOf<String?>(null) }
    var showDropdown by remember { mutableStateOf(false) }

    val displayQueue = remember { mutableStateOf<List<MediaItem>>(emptyList()) }
    val pagerState = rememberPagerState(pageCount = { kotlin.math.max(1, displayQueue.value.size) })
    val folderCache = remember { mutableStateMapOf<String, List<MediaItem>>() }
    var isFolderLoading by remember { mutableStateOf(false) }

    val folders = remember(allMedia) {
        allMedia.mapNotNull { it.bucketName }
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()
    }

    LaunchedEffect(allMedia) {
        folderCache.clear()
    }

    LaunchedEffect(likedItems, selectedBucketName, allMedia) {
        val bucket = selectedBucketName
        if (bucket == null) {
            isFolderLoading = false
            displayQueue.value = if (likedItems.isNotEmpty()) {
                withContext(Dispatchers.Default) { likedItems.shuffled() }
            } else {
                emptyList()
            }
        } else {
            val cached = folderCache[bucket]
            if (cached != null) {
                isFolderLoading = false
                displayQueue.value = cached
            } else {
                isFolderLoading = true
                val source = withContext(Dispatchers.IO) {
                    allMedia.filter { it.bucketName == bucket && File(it.path.orEmpty()).exists() }
                }
                val sorted = withContext(Dispatchers.Default) {
                    source.sortedByDescending { it.dateAdded }
                }
                folderCache[bucket] = sorted
                displayQueue.value = sorted
                isFolderLoading = false
            }
        }

        if (displayQueue.value.isNotEmpty()) {
            pagerState.scrollToPage(0)
        }
    }

    var showCommentSheet by remember { mutableStateOf(false) }
    var currentCommentItem by remember { mutableStateOf<MediaItem?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var showDeleteCommentDialog by remember { mutableStateOf(false) }
    var commentToDeleteIndex by remember { mutableIntStateOf(-1) }

    var showRemoveDialog by remember { mutableStateOf(false) }
    var itemToRemove by remember { mutableStateOf<MediaItem?>(null) }

    if (showCommentSheet && currentCommentItem != null) {
        val item = currentCommentItem!!
        val itemId = item.id
        var comments by remember { mutableStateOf(prefs.comments[itemId] ?: emptyList()) }
        var input by remember { mutableStateOf(TextFieldValue("")) }
        val clipboardManager = LocalClipboardManager.current
        var expandedCommentIndex by remember { mutableIntStateOf(-1) }

        ModalBottomSheet(
            onDismissRequest = { showCommentSheet = false },
            sheetState = sheetState,
            containerColor = ReliveWhite,
            scrimColor = ReliveBlack.copy(alpha = 0.5f),
            modifier = Modifier
                .fillMaxHeight(0.55f)
                .imePadding()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                Text(
                    text = "${comments.size} ${stringResource(R.string.feed_likes_comments_header)}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = ReliveBlack,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(bottom = 16.dp)
                )

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(comments.size) { index ->
                        val comment = comments[index]
                        val isCommentMenuExpanded = expandedCommentIndex == index
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isCommentMenuExpanded) ReliveBlack.copy(alpha = 0.06f) else Color.Transparent
                                )
                                .combinedClickable(
                                    onClick = {},
                                    onLongClick = { expandedCommentIndex = index }
                                )
                                .padding(horizontal = 8.dp, vertical = 12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.Top
                            ) {
                                val avatarRes = if (prefs.avatarType == "boy") {
                                    com.jian.relive.core.ui.R.drawable.boy
                                } else {
                                    com.jian.relive.core.ui.R.drawable.girl
                                }
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(avatarRes)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(ReliveGray100)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = stringResource(R.string.feed_likes_comment_user_me),
                                        fontSize = 14.sp,
                                        color = ReliveGray500,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = comment,
                                        fontSize = 16.sp,
                                        color = ReliveBlack
                                    )
                                }
                            }

                            DropdownMenu(
                                expanded = isCommentMenuExpanded,
                                onDismissRequest = { expandedCommentIndex = -1 },
                                modifier = Modifier
                                    .background(ReliveBlack.copy(alpha = 0.9f))
                                    .width(180.dp)
                            ) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.feed_likes_copy_comment), color = ReliveWhite) },
                                    onClick = {
                                        clipboardManager.setText(AnnotatedString(comment))
                                        Toast.makeText(context, context.getString(R.string.feed_likes_comment_copied), Toast.LENGTH_SHORT).show()
                                        expandedCommentIndex = -1
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.feed_likes_delete_comment), color = ReliveRed500) },
                                    onClick = {
                                        commentToDeleteIndex = index
                                        showDeleteCommentDialog = true
                                        expandedCommentIndex = -1
                                    }
                                )
                            }
                        }
                    }
                }

                if (showDeleteCommentDialog && commentToDeleteIndex != -1) {
                    AlertDialog(
                        onDismissRequest = {
                            showDeleteCommentDialog = false
                            commentToDeleteIndex = -1
                        },
                        title = { Text(stringResource(R.string.feed_likes_delete_comment_title)) },
                        text = { Text(stringResource(R.string.feed_likes_delete_comment_message)) },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    if (commentToDeleteIndex in comments.indices) {
                                        val newComments = comments.toMutableList()
                                        newComments.removeAt(commentToDeleteIndex)
                                        comments = newComments
                                        val allComments = prefs.comments
                                        allComments[itemId] = newComments
                                        prefs.comments = allComments
                                    }
                                    showDeleteCommentDialog = false
                                    commentToDeleteIndex = -1
                                }
                            ) {
                                Text(stringResource(R.string.feed_delete_button), color = ReliveRed600)
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = {
                                showDeleteCommentDialog = false
                                commentToDeleteIndex = -1
                            }) {
                                Text(stringResource(R.string.feed_cancel_button), color = ReliveGray600)
                            }
                        },
                        containerColor = ReliveWhite
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp, top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextField(
                        value = input,
                        onValueChange = { input = it },
                        placeholder = { Text(stringResource(R.string.feed_likes_comment_placeholder)) },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = ReliveGray100,
                            unfocusedContainerColor = ReliveGray100,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier.weight(1f)
                    )

                    if (input.text.isNotEmpty()) {
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = {
                                val newComment = input.text.trim()
                                if (newComment.isNotEmpty()) {
                                    val newComments = comments + newComment
                                    comments = newComments
                                    val allComments = prefs.comments
                                    allComments[itemId] = newComments
                                    prefs.comments = allComments
                                    input = TextFieldValue("")
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.Send,
                                contentDescription = stringResource(R.string.feed_likes_send),
                                tint = ReliveRed500
                            )
                        }
                    }
                }
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ReliveBlack)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {}
    ) {
        if (isFolderLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = ReliveWhite, strokeWidth = 3.dp)
            }
        } else if (displayQueue.value.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = if (selectedBucketName == null) {
                        stringResource(R.string.feed_likes_no_items)
                    } else {
                        stringResource(R.string.feed_likes_folder_no_items)
                    },
                    color = ReliveWhite.copy(alpha = 0.5f),
                    fontSize = 16.sp
                )
            }
        } else {
            VerticalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                key = { index ->
                    val itemId = displayQueue.value.getOrNull(index)?.id
                    if (itemId != null) "${itemId}_$index" else "empty_$index"
                }
            ) { page ->
                val item = displayQueue.value.getOrNull(page) ?: return@VerticalPager
                val isActive = page == pagerState.currentPage
                var currentItem by remember { mutableStateOf(item) }
                var exifLoaded by remember { mutableStateOf(false) }

                LaunchedEffect(item.id) {
                    if (!exifLoaded && item.isPhoto) {
                        withContext(Dispatchers.IO) {
                            try {
                                context.contentResolver.openInputStream(item.uri)?.use { stream: InputStream ->
                                    val exif = ExifInterface(stream)
                                    val latLong = exif.latLong
                                    if (latLong != null) {
                                        val geocoder = Geocoder(context, Locale.getDefault())
                                        geocoder.getFromLocation(latLong[0], latLong[1], 1) { addresses ->
                                            if (!addresses.isNullOrEmpty()) {
                                                val address = addresses[0]
                                                val loc = if (address.locality != null) {
                                                    "${address.locality}, ${address.countryName ?: ""}".trimEnd(',', ' ')
                                                } else {
                                                    address.getAddressLine(0) ?: "Unknown Location"
                                                }
                                                currentItem = currentItem.copy(location = loc)
                                            }
                                        }
                                    }
                                    exifLoaded = true
                                }
                            } catch (_: Exception) {
                            }
                        }
                    }
                }

                Box(Modifier.fillMaxSize()) {
                    if (item.isVideo || item.isLivePhoto) {
                        LiveFeedPlayer(
                            item = item,
                            isMuted = false,
                            shouldPlay = isActive,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(item.uri)
                                .crossfade(true)
                                .build(),
                            contentDescription = null,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    if (danmakuEnabled) {
                        val comments = prefs.comments[item.id] ?: emptyList()
                        DanmakuOverlay(comments = comments)
                    }

                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(start = 16.dp, bottom = 24.dp)
                    ) {
                        if (currentItem.location.isNotBlank() && currentItem.location != "Unknown Location") {
                            Text(
                                text = currentItem.location,
                                color = ReliveWhite,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                        val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                            .format(Date(currentItem.dateAdded * 1000))
                        Text(
                            text = dateStr,
                            color = ReliveWhite.copy(alpha = 0.8f),
                            fontSize = 12.sp
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = topInset)
                    .zIndex(10f),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = ReliveBlack.copy(alpha = 0.6f),
                    contentColor = ReliveWhite,
                    shadowElevation = 4.dp,
                    onClick = { showDropdown = true },
                    modifier = Modifier
                        .height(40.dp)
                        .bounceClick()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    ) {
                        Text(
                            text = selectedBucketName ?: stringResource(R.string.feed_likes_title),
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Spacer(Modifier.width(4.dp))
                        Icon(
                            imageVector = if (showDropdown) Icons.Default.KeyboardArrowUp else Icons.Default.ArrowDropDown,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                DropdownMenu(
                    expanded = showDropdown,
                    onDismissRequest = { showDropdown = false },
                    offset = DpOffset(0.dp, 4.dp),
                    modifier = Modifier
                        .background(ReliveBlack.copy(alpha = 0.9f))
                        .heightIn(max = 320.dp)
                        .width(200.dp)
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.feed_likes_title), fontWeight = FontWeight.Bold, color = ReliveWhite) },
                        onClick = {
                            selectedBucketName = null
                            showDropdown = false
                        },
                        leadingIcon = { Icon(Icons.Outlined.Favorite, null, tint = ReliveRed500) },
                        trailingIcon = if (selectedBucketName == null) {
                            { Icon(Icons.Outlined.Check, null, tint = ReliveWhite, modifier = Modifier.size(18.dp)) }
                        } else null
                    )

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = ReliveWhite.copy(alpha = 0.2f))

                    folders.forEach { name ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = name,
                                    color = ReliveWhite,
                                    fontWeight = if (selectedBucketName == name) FontWeight.Bold else FontWeight.Normal,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            onClick = {
                                selectedBucketName = name
                                showDropdown = false
                            },
                            leadingIcon = {
                                Icon(Icons.Outlined.Folder, null, tint = ReliveWhite.copy(alpha = 0.6f))
                            },
                            trailingIcon = if (selectedBucketName == name) {
                                { Icon(Icons.Outlined.Check, null, tint = ReliveWhite, modifier = Modifier.size(18.dp)) }
                            } else null
                        )
                    }
                }
            }

            val currentItem = displayQueue.value.getOrNull(pagerState.currentPage)
            if (currentItem != null) {
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 16.dp, bottom = 64.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    val commentCount = prefs.comments[currentItem.id]?.size ?: 0

                    Box {
                        IconButton(
                            onClick = {
                                currentCommentItem = currentItem
                                showCommentSheet = true
                            },
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(ReliveBlack.copy(alpha = 0.4f))
                                .bounceClick(onClick = {
                                    currentCommentItem = currentItem
                                    showCommentSheet = true
                                })
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.ChatBubbleOutline,
                                contentDescription = null,
                                tint = ReliveWhite,
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        if (commentCount > 0) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .background(ReliveBlack.copy(alpha = 0.6f), CircleShape)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = if (commentCount > 99) "99+" else commentCount.toString(),
                                    color = ReliveWhite,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    IconButton(
                        onClick = {
                            MediaShareHelper.shareMedia(context, currentItem)
                        },
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(ReliveBlack.copy(alpha = 0.4f))
                            .bounceClick(onClick = {
                                MediaShareHelper.shareMedia(context, currentItem)
                            })
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Share,
                            contentDescription = stringResource(R.string.feed_share),
                            tint = ReliveWhite,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Box {
                        IconButton(
                            onClick = {
                                danmakuEnabled = !danmakuEnabled
                                prefs.danmakuEnabled = danmakuEnabled
                            },
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(ReliveBlack.copy(alpha = 0.4f))
                                .bounceClick(onClick = {
                                    danmakuEnabled = !danmakuEnabled
                                    prefs.danmakuEnabled = danmakuEnabled
                                })
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Notes,
                                contentDescription = stringResource(R.string.feed_likes_danmaku),
                                tint = ReliveWhite,
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        if (danmakuEnabled) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(4.dp)
                                    .background(ReliveWhite, CircleShape)
                                    .padding(2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Check,
                                    contentDescription = null,
                                    tint = ReliveBlack,
                                    modifier = Modifier.size(10.dp)
                                )
                            }
                        }
                    }

                    if (selectedBucketName == null) {
                        IconButton(
                            onClick = {
                                itemToRemove = currentItem
                                showRemoveDialog = true
                            },
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(ReliveBlack.copy(alpha = 0.4f))
                                .bounceClick(onClick = {
                                    itemToRemove = currentItem
                                    showRemoveDialog = true
                                })
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.HeartBroken,
                                contentDescription = stringResource(R.string.feed_likes_remove_like),
                                tint = ReliveWhite,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }
            }
        }

        if (showRemoveDialog && itemToRemove != null) {
            AlertDialog(
                onDismissRequest = {
                    showRemoveDialog = false
                    itemToRemove = null
                },
                title = { Text(stringResource(R.string.feed_likes_remove_like_title)) },
                text = { Text(stringResource(R.string.feed_likes_remove_like_message)) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val item = itemToRemove!!
                            displayQueue.value = displayQueue.value.filter { it.id != item.id }
                            onUnlike(item.id)
                            showRemoveDialog = false
                            itemToRemove = null
                        }
                    ) {
                        Text(stringResource(R.string.feed_likes_remove), color = ReliveRed600)
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showRemoveDialog = false
                        itemToRemove = null
                    }) {
                        Text(stringResource(R.string.feed_cancel_button), color = ReliveGray600)
                    }
                },
                containerColor = ReliveWhite
            )
        }

        IconButton(
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(top = topInset, start = 16.dp)
                .clip(CircleShape)
                .background(ReliveBlack.copy(alpha = 0.2f))
                .bounceClick(onClick = onBack)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                contentDescription = stringResource(R.string.feed_likes_back),
                tint = ReliveWhite
            )
        }
    }
}

/**
 * 屏幕弹幕轨道图层。
 */
@Composable
private fun DanmakuOverlay(
    comments: List<String>,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize().clipToBounds()) {
        if (comments.isNotEmpty()) {
            val trackHeight = 60.dp
            val startY = 100.dp
            val playOnce = comments.size <= 3
            val trackCount = if (playOnce) comments.size else minOf(5, comments.size)
            val nextIndex = remember(comments) { AtomicInteger(0) }

            for (i in 0 until trackCount) {
                DanmakuTrack(
                    comments = comments,
                    nextIndex = if (playOnce) null else nextIndex,
                    topOffset = startY + trackHeight * i,
                    playOnce = playOnce,
                    fixedIndex = if (playOnce) i else null
                )
            }
        }
    }
}

/**
 * 单条弹幕平移运动轨道。
 */
@Composable
private fun DanmakuTrack(
    comments: List<String>,
    nextIndex: AtomicInteger?,
    topOffset: Dp,
    playOnce: Boolean,
    fixedIndex: Int?
) {
    var text by remember { mutableStateOf("") }
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    val density = LocalDensity.current
    val startX = with(density) { screenWidth.toPx() }
    val endX = -1000f

    val anim = remember { Animatable(startX) }

    LaunchedEffect(comments, playOnce, fixedIndex) {
        if (playOnce) {
            val index = fixedIndex ?: 0
            if (index in comments.indices) {
                text = comments[index]
                anim.snapTo(startX)
                val duration = (8000..15000).random()
                anim.animateTo(endX, animationSpec = tween(duration, easing = LinearEasing))
            }
        } else {
            while (true) {
                val index = (nextIndex?.getAndIncrement() ?: 0) % comments.size
                text = comments[index]
                anim.snapTo(startX)
                val duration = (8000..15000).random()
                anim.animateTo(endX, animationSpec = tween(duration, easing = LinearEasing))
                delay((500L..2000L).random())
            }
        }
    }

    if (text.isNotEmpty()) {
        Text(
            text = text,
            color = ReliveWhite,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            modifier = Modifier
                .offset(y = topOffset)
                .offset { IntOffset(anim.value.roundToInt(), 0) }
                .background(ReliveBlack.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                .padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

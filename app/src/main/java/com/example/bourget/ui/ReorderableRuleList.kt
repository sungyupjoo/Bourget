package com.example.bourget.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.zIndex
import com.example.bourget.R
import com.example.bourget.data.Rule
import com.example.bourget.data.RuleKind
import kotlinx.coroutines.launch

/**
 * The rule list of the apps and websites tabs. Long-press a row and drag to change the order;
 * the other rows slide aside and the list scrolls near its edges. The order is kept on screen
 * while dragging and saved once ([onReorder]) when the row is dropped. Screen readers get
 * "Move up" and "Move down" actions instead.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ReorderableRuleList(
    kind: RuleKind,
    rules: List<Rule>,
    apps: AppDirectory,
    nameOf: (Rule) -> String,
    onEnabledChange: (Rule, Boolean) -> Unit,
    onOpenRule: (Rule) -> Unit,
    onReorder: (List<String>) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    val currentOnReorder by rememberUpdatedState(onReorder)

    // The order shown while dragging, and after the drop until the saved list catches up.
    var local by remember { mutableStateOf<List<Rule>?>(null) }
    var draggedKey by remember { mutableStateOf<String?>(null) }
    var dragStart by remember { mutableStateOf<LazyListItemInfo?>(null) }
    var distance by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(rules) {
        val pending = local ?: return@LaunchedEffect
        val saved = rules.map { it.target }
        val shown = pending.map { it.target }
        // Saved, or the rules changed underneath (added or removed): follow the stored list again.
        if (draggedKey == null && (saved == shown || saved.toSet() != shown.toSet())) local = null
    }
    // Rule contents (on/off, action) always come from the stored list; only the order is local.
    val shown = local?.let { order ->
        val byTarget = rules.associateBy { it.target }
        order.mapNotNull { byTarget[it.target] }
    } ?: rules
    val currentShown by rememberUpdatedState(shown)

    fun move(from: Int, to: Int) {
        val first = listState.firstVisibleItemIndex
        val firstOffset = listState.firstVisibleItemScrollOffset
        local = currentShown.toMutableList().apply { add(to, removeAt(from)) }
        // LazyColumn keeps the first visible item in place by key; when that item is the one
        // moving, pin the scroll position instead so the list does not jump.
        if (first == from || first == to) scope.launch { listState.scrollToItem(first, firstOffset) }
    }

    fun finishDrag() {
        if (draggedKey != null) local?.let { order -> currentOnReorder(order.map { it.target }) }
        draggedKey = null
        dragStart = null
        distance = 0f
    }

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize().pointerInput(Unit) {
            detectDragGesturesAfterLongPress(
                onDragStart = { offset ->
                    val item = listState.layoutInfo.visibleItemsInfo
                        .firstOrNull { offset.y.toInt() in it.offset..(it.offset + it.size) }
                        ?: return@detectDragGesturesAfterLongPress
                    local = currentShown
                    draggedKey = item.key as String
                    dragStart = item
                    distance = 0f
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                },
                onDrag = { change, amount ->
                    val start = dragStart ?: return@detectDragGesturesAfterLongPress
                    val key = draggedKey ?: return@detectDragGesturesAfterLongPress
                    change.consume()
                    distance += amount.y
                    val info = listState.layoutInfo
                    val current = info.visibleItemsInfo.firstOrNull { it.key == key }
                        ?: return@detectDragGesturesAfterLongPress
                    val top = start.offset + distance
                    val bottom = top + current.size
                    val index = currentShown.indexOfFirst { it.target == key }
                    val next = info.visibleItemsInfo.firstOrNull { it.index == index + 1 }
                    val previous = info.visibleItemsInfo.firstOrNull { it.index == index - 1 }
                    // Swap once the dragged row passes the middle of its neighbor.
                    if (next != null && bottom > next.offset + next.size / 2) {
                        move(index, index + 1)
                    } else if (previous != null && top < previous.offset + previous.size / 2) {
                        move(index, index - 1)
                    }
                    val overscroll = when {
                        bottom > info.viewportEndOffset -> bottom - info.viewportEndOffset
                        top < info.viewportStartOffset -> top - info.viewportStartOffset
                        else -> 0f
                    }
                    if (overscroll != 0f) scope.launch { listState.scrollBy(overscroll / 2) }
                },
                onDragEnd = { finishDrag() },
                onDragCancel = { finishDrag() },
            )
        },
    ) {
        itemsIndexed(shown, key = { _, rule -> rule.target }) { i, rule ->
            val dragged = rule.target == draggedKey
            val moveUp = stringResource(R.string.rule_move_up)
            val moveDown = stringResource(R.string.rule_move_down)
            Column(
                if (dragged) {
                    // Lifted: drawn above the others at the finger, on a tonal background.
                    Modifier
                        .zIndex(1f)
                        .graphicsLayer {
                            val now = listState.layoutInfo.visibleItemsInfo.firstOrNull { it.key == rule.target }
                            translationY = (dragStart?.offset ?: 0) + distance - (now?.offset ?: 0)
                        }
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                } else {
                    Modifier.animateItemPlacement()
                },
            ) {
                if (i > 0 && !dragged) RowRule()
                RuleRow(
                    kind = kind,
                    rule = rule,
                    name = nameOf(rule),
                    apps = apps,
                    onEnabledChange = { onEnabledChange(rule, it) },
                    onClick = { onOpenRule(rule) },
                    modifier = Modifier.semantics {
                        customActions = buildList {
                            if (i > 0) add(CustomAccessibilityAction(moveUp) {
                                onReorder(shown.map { it.target }.toMutableList().apply { add(i - 1, removeAt(i)) })
                                true
                            })
                            if (i < shown.lastIndex) add(CustomAccessibilityAction(moveDown) {
                                onReorder(shown.map { it.target }.toMutableList().apply { add(i + 1, removeAt(i)) })
                                true
                            })
                        }
                    },
                )
            }
        }
    }
}

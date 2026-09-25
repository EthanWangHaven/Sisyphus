package cn.wangce.lumi.ui.tasks

// 待办共享组件：首页与「备忘录和待办」页共用的筛选胶囊 / 快速新增 / 待办行 / 编辑弹层 / 删除确认

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cn.wangce.lumi.R
import cn.wangce.lumi.data.local.TodoEntity
import cn.wangce.lumi.ui.components.AppTextField
import cn.wangce.lumi.ui.components.DangerButton
import cn.wangce.lumi.ui.components.GlassCard
import cn.wangce.lumi.ui.components.LumiDialog
import cn.wangce.lumi.ui.components.LumiDialogButtons
import cn.wangce.lumi.ui.components.PrimaryPillButton
import cn.wangce.lumi.ui.components.SecondaryButton
import cn.wangce.lumi.ui.components.SelectableChip
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// 待办行尾注：创建日期（M月d日 / MMM d）
internal fun todoDayLabel(context: Context, createdAt: Long): String =
    SimpleDateFormat(context.getString(R.string.date_md), Locale.getDefault()).format(Date(createdAt))

// 筛选胶囊组：未完成 / 已完成 / 全部（统一 SelectableChip 主筛选模式）
@Composable
internal fun FilterTabs(
    current: TodoFilter,
    onSelect: (TodoFilter) -> Unit,
) {
    val options = listOf(
        TodoFilter.PENDING to stringResource(R.string.scf0a3e),
        TodoFilter.DONE to stringResource(R.string.sfad522),
        TodoFilter.ALL to stringResource(R.string.sa8b0c2),
    )
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        options.forEach { (value, label) ->
            SelectableChip(
                text = label,
                selected = value == current,
                onClick = { onSelect(value) },
            )
        }
    }
}

// 快速新增输入框已移除（需求新6：添加待办改为 NotesScreen 中央弹窗）

// 单条待办：圆圈勾选 + 标题（完成态动画删除线）+ 创建日期尾注，点击编辑、长按操作弹窗
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun TodoRow(
    todo: TodoEntity,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onRequestDelete: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .combinedClickable(
                onClick = onEdit,
                onLongClick = { menuOpen = true },
            ),
        cornerRadius = 12,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CircleCheckbox(checked = todo.done, onToggle = onToggle)
            Spacer(Modifier.width(12.dp))
            Text(
                text = todo.title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (todo.done) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                textDecoration = if (todo.done) TextDecoration.LineThrough else null,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = todoDayLabel(LocalContext.current, todo.createdAt),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            )
        }
    }
    // 长按操作弹窗：与「添加待办」同款 LumiDialog 中央卡片，统一风格
    if (menuOpen) {
        LumiDialog(
            onDismissRequest = { menuOpen = false },
            title = todo.title,
            actions = {
                LumiDialogButtons {
                    // 编辑：白底 hairline 胶囊
                    SecondaryButton(
                        text = stringResource(R.string.s95b351),
                        onClick = {
                            menuOpen = false
                            onEdit()
                        },
                        modifier = Modifier.weight(1f),
                    )
                    // 删除：破坏性操作，橙红底胶囊
                    DangerButton(
                        text = stringResource(R.string.s2f4aad),
                        onClick = {
                            menuOpen = false
                            onRequestDelete()
                        },
                        modifier = Modifier.weight(1f),
                    )
                }
            },
        ) {
        }
    }
}

// 圆圈勾选框：完成态填充主题色 + 打勾动画
@Composable
private fun CircleCheckbox(
    checked: Boolean,
    onToggle: () -> Unit,
) {
    val primary = MaterialTheme.colorScheme.primary
    val ringColor by animateColorAsState(
        targetValue = if (checked) {
            primary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
        },
        animationSpec = tween(250),
        label = "ringColor",
    )
    val fillAlpha by animateFloatAsState(
        targetValue = if (checked) 1f else 0f,
        animationSpec = tween(250),
        label = "fillAlpha",
    )
    Box(
        modifier = Modifier
            .size(24.dp)
            .clip(CircleShape)
            .background(primary.copy(alpha = fillAlpha))
            .border(2.dp, ringColor, CircleShape)
            .clickable(onClick = onToggle),
        contentAlignment = Alignment.Center,
    ) {
        AnimatedVisibility(
            visible = checked,
            enter = scaleIn(tween(200)) + fadeIn(tween(200)),
            exit = scaleOut(tween(200)) + fadeOut(tween(200)),
        ) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(14.dp),
            )
        }
    }
}

// 编辑待办弹窗：与「添加待办」同款 LumiDialog 中央卡片（取消/保存等宽胶囊）
@Composable
internal fun TodoEditDialog(
    todo: TodoEntity,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var text by remember(todo.id) { mutableStateOf(todo.title) }
    LumiDialog(
        onDismissRequest = onDismiss,
        title = stringResource(R.string.sb994c3),
        actions = {
            LumiDialogButtons {
                // 取消：白底 hairline 胶囊
                SecondaryButton(
                    text = stringResource(R.string.s625fb2),
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                )
                // 保存：主操作胶囊，内容为空时禁用
                PrimaryPillButton(
                    text = stringResource(R.string.sbe5fbb),
                    onClick = { onSave(text) },
                    enabled = text.isNotBlank(),
                    modifier = Modifier.weight(1f),
                )
            }
        },
    ) {
        AppTextField(
            value = text,
            onValueChange = { text = it },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { if (text.isNotBlank()) onSave(text) }),
        )
    }
}

// 删除确认弹窗
@Composable
internal fun TodoDeleteDialog(
    todo: TodoEntity,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    LumiDialog(
        onDismissRequest = onDismiss,
        title = stringResource(R.string.s329fde),
        actions = {
            LumiDialogButtons {
                // 取消：白底 hairline 胶囊
                SecondaryButton(
                    text = stringResource(R.string.s625fb2),
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                )
                // 删除：破坏性操作，橙红底胶囊
                DangerButton(
                    text = stringResource(R.string.s2f4aad),
                    onClick = onConfirm,
                    modifier = Modifier.weight(1f),
                )
            }
        },
    ) {
        Text(stringResource(R.string.del_confirm, todo.title))
    }
}

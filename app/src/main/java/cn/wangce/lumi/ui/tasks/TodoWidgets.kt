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
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cn.wangce.lumi.R
import cn.wangce.lumi.data.local.TodoEntity
import cn.wangce.lumi.ui.components.GlassCard
import cn.wangce.lumi.ui.theme.DarkSheetBg
import cn.wangce.lumi.ui.theme.LightSheetBg
import cn.wangce.lumi.ui.theme.LocalDarkTheme
import cn.wangce.lumi.ui.theme.MorandiPink
import cn.wangce.lumi.ui.theme.PillBgDark
import cn.wangce.lumi.ui.theme.PillBgLight
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// 待办行尾注：创建日期（M月d日 / MMM d）
internal fun todoDayLabel(context: Context, createdAt: Long): String =
    SimpleDateFormat(context.getString(R.string.date_md), Locale.getDefault()).format(Date(createdAt))

// 筛选胶囊组：未完成 / 已完成 / 全部（独立胶囊按钮，iOS 软 UI 风格）
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
            val selected = value == current
            // 选中态：玻璃底 + 深色描边 + 深色文字（参考图 chip 风格）
            val bg by animateColorAsState(
                targetValue = if (selected) {
                    MaterialTheme.colorScheme.surface.copy(alpha = 0.55f)
                } else {
                    Color.Transparent
                },
                animationSpec = tween(250),
                label = "pillBg",
            )
            val borderColor by animateColorAsState(
                targetValue = if (selected) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                },
                animationSpec = tween(250),
                label = "pillBorder",
            )
            Box(
                modifier = Modifier
                    .height(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(bg)
                    .border(1.dp, borderColor, RoundedCornerShape(12.dp))
                    .clickable { onSelect(value) }
                    .padding(horizontal = 22.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

// 快速新增输入框已移除（需求新6：添加待办改为 NotesScreen 中央弹窗）

// 单条待办：圆圈勾选 + 标题（完成态动画删除线）+ 创建日期尾注，点击编辑、长按菜单
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun TodoRow(
    todo: TodoEntity,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onRequestDelete: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    Box {
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .combinedClickable(
                    onClick = onEdit,
                    onLongClick = { menuOpen = true },
                ),
            cornerRadius = 18,
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
        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.s95b351)) },
                onClick = {
                    menuOpen = false
                    onEdit()
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.s2f4aad)) },
                onClick = {
                    menuOpen = false
                    onRequestDelete()
                },
            )
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

// 编辑弹层：底部 Sheet 修改标题
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TodoEditSheet(
    todo: TodoEntity,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var text by remember(todo.id) { mutableStateOf(todo.title) }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = if (LocalDarkTheme.current) DarkSheetBg else LightSheetBg,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
        ) {
            Text(
                text = stringResource(R.string.sb994c3),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.fillMaxWidth(),
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                ),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { if (text.isNotBlank()) onSave(text) }),
            )
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.s625fb2)) }
                Spacer(Modifier.width(8.dp))
                // 主操作黑胶囊：浅色黑底白字 / 深色浅底黑字
                val dark = LocalDarkTheme.current
                Button(
                    onClick = { if (text.isNotBlank()) onSave(text) },
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (dark) PillBgDark else PillBgLight,
                        contentColor = MaterialTheme.colorScheme.surface,
                    ),
                ) { Text(stringResource(R.string.sbe5fbb)) }
            }
        }
    }
}

// 删除确认弹窗
@Composable
internal fun TodoDeleteDialog(
    todo: TodoEntity,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.s329fde)) },
        text = { Text(stringResource(R.string.del_confirm, todo.title)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.s2f4aad), color = MorandiPink)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.s625fb2)) }
        },
    )
}

package com.listeik.familyapp.ui

import android.graphics.Color.parseColor
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.listeik.familyapp.data.model.ActivityEvent
import com.listeik.familyapp.data.model.FamilyItem
import com.listeik.familyapp.data.model.FamilyMember
import com.listeik.familyapp.data.model.FamilySession
import com.listeik.familyapp.data.model.ItemCategory
import com.listeik.familyapp.data.model.ItemStatus
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val PistachioStrong = Color(0xFF4F8160)

@Composable
internal fun DashboardScreen(
    session: FamilySession,
    members: List<FamilyMember>,
    items: List<FamilyItem>,
    events: List<ActivityEvent>,
    onAdjustFoodPortions: (FamilyItem, Int) -> Unit,
    onSetItemCompleted: (FamilyItem, Boolean) -> Unit,
    onMoveItemForward: (FamilyItem) -> Unit,
    onDeleteItem: (FamilyItem) -> Unit,
    contentPadding: PaddingValues,
) {
    val food = remember(items) { items.filter { it.category == ItemCategory.FOOD } }
    val shopping = remember(items) { items.filter { it.category == ItemCategory.BUY } }
    val tasks = remember(items) { items.filter { it.category == ItemCategory.TASK } }
    val wishes = remember(items) { items.filter { it.category == ItemCategory.WISH } }
    val memberById = remember(members) { members.associateBy(FamilyMember::uid) }
    val backgroundColors = if (isSystemInDarkTheme()) {
        listOf(Color(0xFF17231A), MaterialTheme.colorScheme.background, Color(0xFF231E18))
    } else {
        listOf(Color(0xFFF1F9F0), MaterialTheme.colorScheme.background, Color(0xFFFFFAF3))
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = backgroundColors,
                ),
            ),
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                top = contentPadding.calculateTopPadding() + 12.dp,
                end = 16.dp,
                bottom = contentPadding.calculateBottomPadding() + 96.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                DashboardHeader(
                    session = session,
                    members = members,
                    foodCount = food.count { (it.remainingPortions ?: 1) > 0 },
                    shoppingCount = shopping.count { it.status != ItemStatus.BOUGHT },
                    taskCount = tasks.count { it.status != ItemStatus.DONE },
                )
            }
            item {
                FoodWidget(
                    items = food,
                    session = session,
                    memberById = memberById,
                    onAdjustPortions = onAdjustFoodPortions,
                    onMoveForward = onMoveItemForward,
                    onDeleteItem = onDeleteItem,
                )
            }
            item {
                ChecklistWidget(
                    title = "Список покупок",
                    icon = Icons.Default.ShoppingCart,
                    items = shopping,
                    completeStatus = ItemStatus.BOUGHT,
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    accentColor = MaterialTheme.colorScheme.tertiary,
                    emptyText = "Список покупок пуст",
                    session = session,
                    onSetCompleted = onSetItemCompleted,
                    onDeleteItem = onDeleteItem,
                )
            }
            item {
                ChecklistWidget(
                    title = "Бытовые дела",
                    icon = Icons.Default.Checklist,
                    items = tasks,
                    completeStatus = ItemStatus.DONE,
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    accentColor = MaterialTheme.colorScheme.secondary,
                    emptyText = "На сегодня дел нет",
                    session = session,
                    onSetCompleted = onSetItemCompleted,
                    onDeleteItem = onDeleteItem,
                )
            }
            if (wishes.isNotEmpty()) {
                item {
                    ChecklistWidget(
                        title = "Хотелки",
                        icon = Icons.Default.Stars,
                        items = wishes,
                        completeStatus = ItemStatus.ARCHIVED,
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        accentColor = MaterialTheme.colorScheme.tertiary,
                        emptyText = "Пока без хотелок",
                        session = session,
                        onSetCompleted = onSetItemCompleted,
                        onDeleteItem = onDeleteItem,
                    )
                }
            }
            if (events.isNotEmpty()) {
                item { RecentActivityWidget(events.take(4)) }
            }
        }
    }
}

@Composable
private fun DashboardHeader(
    session: FamilySession,
    members: List<FamilyMember>,
    foodCount: Int,
    shoppingCount: Int,
    taskCount: Int,
) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Добрый день, ${session.userName.substringBefore(' ')}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = session.familyName,
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(12.dp))
            MemberAvatarRow(members = members, fallbackName = session.userName)
        }
        Spacer(Modifier.height(14.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SummaryMetric("Еда", foodCount, MaterialTheme.colorScheme.primary, Modifier.weight(1f))
            SummaryMetric("Купить", shoppingCount, MaterialTheme.colorScheme.tertiary, Modifier.weight(1f))
            SummaryMetric("Дела", taskCount, MaterialTheme.colorScheme.secondary, Modifier.weight(1f))
        }
        Spacer(Modifier.height(10.dp))
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
            tonalElevation = 1.dp,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Group,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(19.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Код семьи", style = MaterialTheme.typography.bodyMedium)
                }
                Text(
                    session.inviteCode,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun SummaryMetric(label: String, value: Int, accent: Color, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.height(62.dp),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.78f),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 7.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                value.toString(),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = accent,
            )
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun MemberAvatarRow(members: List<FamilyMember>, fallbackName: String) {
    val visibleMembers = members.take(4)
    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        if (visibleMembers.isEmpty()) {
            MemberAvatar(name = fallbackName, color = PistachioStrong, size = 38)
        } else {
            visibleMembers.forEach { member ->
                MemberAvatar(
                    name = member.name,
                    color = avatarColor(member.avatarColor),
                    size = 38,
                )
            }
        }
    }
}

@Composable
private fun FoodWidget(
    items: List<FamilyItem>,
    session: FamilySession,
    memberById: Map<String, FamilyMember>,
    onAdjustPortions: (FamilyItem, Int) -> Unit,
    onMoveForward: (FamilyItem) -> Unit,
    onDeleteItem: (FamilyItem) -> Unit,
) {
    val accent = MaterialTheme.colorScheme.primary
    WidgetCard(containerColor = MaterialTheme.colorScheme.primaryContainer) {
        WidgetHeader(
            title = "Готовая еда",
            count = items.count { (it.remainingPortions ?: 1) > 0 },
            icon = Icons.Default.Restaurant,
            accent = accent,
        )
        if (items.isEmpty()) {
            WidgetEmpty("Добавьте еду и количество порций")
        } else {
            items.forEachIndexed { index, item ->
                if (index > 0) WidgetDivider()
                FoodInventoryRow(
                    item = item,
                    updatedBy = memberById[item.updatedBy],
                    canDelete = item.createdBy == session.userId,
                    onAdjustPortions = onAdjustPortions,
                    onMoveForward = onMoveForward,
                    onDelete = onDeleteItem,
                    accent = accent,
                )
            }
        }
    }
}

@Composable
private fun FoodInventoryRow(
    item: FamilyItem,
    updatedBy: FamilyMember?,
    canDelete: Boolean,
    onAdjustPortions: (FamilyItem, Int) -> Unit,
    onMoveForward: (FamilyItem) -> Unit,
    onDelete: (FamilyItem) -> Unit,
    accent: Color,
) {
    val total = item.totalPortions
    val remaining = item.remainingPortions
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 13.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            MemberAvatar(
                name = updatedBy?.name ?: item.title,
                color = updatedBy?.let { avatarColor(it.avatarColor) } ?: accent,
                size = 32,
            )
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    item.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    updatedBy?.name?.let { "Обновил $it" } ?: item.status.label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (canDelete) {
                IconButton(onClick = { onDelete(item) }, modifier = Modifier.size(38.dp)) {
                    Icon(
                        Icons.Default.DeleteOutline,
                        contentDescription = "Удалить ${item.title}",
                        modifier = Modifier.size(19.dp),
                    )
                }
            }
        }
        if (total != null && remaining != null) {
            val progress = (remaining.toFloat() / total.toFloat()).coerceIn(0f, 1f)
            Spacer(Modifier.height(11.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(9.dp).clip(CircleShape),
                color = accent,
                trackColor = Color.White.copy(alpha = 0.62f),
            )
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PortionButton(
                    icon = Icons.Default.Remove,
                    contentDescription = "Уменьшить количество ${item.title}",
                    enabled = remaining > 0,
                    accent = accent,
                    onClick = { onAdjustPortions(item, -1) },
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "$remaining из $total",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = accent,
                    )
                    Text(
                        if (remaining == 1) "порция осталась" else "порций осталось",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                PortionButton(
                    icon = Icons.Default.Add,
                    contentDescription = "Увеличить количество ${item.title}",
                    enabled = remaining < total,
                    accent = accent,
                    onClick = { onAdjustPortions(item, 1) },
                )
            }
        } else {
            Spacer(Modifier.height(10.dp))
            Surface(
                onClick = { onMoveForward(item) },
                shape = RoundedCornerShape(16.dp),
                color = Color.White.copy(alpha = 0.55f),
            ) {
                Text(
                    item.status.label,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
                    color = accent,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
private fun PortionButton(
    icon: ImageVector,
    contentDescription: String,
    enabled: Boolean,
    accent: Color,
    onClick: () -> Unit,
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.size(42.dp).background(
            color = if (enabled) Color.White.copy(alpha = 0.82f) else Color.White.copy(alpha = 0.35f),
            shape = CircleShape,
        ),
    ) {
        Icon(
            icon,
            contentDescription = contentDescription,
            tint = if (enabled) accent else MaterialTheme.colorScheme.outline,
        )
    }
}

@Composable
private fun ChecklistWidget(
    title: String,
    icon: ImageVector,
    items: List<FamilyItem>,
    completeStatus: ItemStatus,
    containerColor: Color,
    accentColor: Color,
    emptyText: String,
    session: FamilySession,
    onSetCompleted: (FamilyItem, Boolean) -> Unit,
    onDeleteItem: (FamilyItem) -> Unit,
) {
    WidgetCard(containerColor = containerColor) {
        WidgetHeader(
            title = title,
            count = items.count { it.status != completeStatus },
            icon = icon,
            accent = accentColor,
        )
        if (items.isEmpty()) {
            WidgetEmpty(emptyText)
        } else {
            items.forEachIndexed { index, item ->
                if (index > 0) WidgetDivider()
                val complete = item.status == completeStatus
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(
                        checked = complete,
                        onCheckedChange = { onSetCompleted(item, it) },
                        colors = CheckboxDefaults.colors(
                            checkedColor = accentColor,
                            checkmarkColor = Color.White,
                        ),
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            item.title,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            textDecoration = if (complete) TextDecoration.LineThrough else null,
                            color = if (complete) {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (item.status !in setOf(item.category.defaultStatus, completeStatus)) {
                            Text(
                                item.status.label,
                                style = MaterialTheme.typography.labelSmall,
                                color = accentColor,
                            )
                        }
                    }
                    if (item.createdBy == session.userId) {
                        IconButton(onClick = { onDeleteItem(item) }, modifier = Modifier.size(38.dp)) {
                            Icon(
                                Icons.Default.DeleteOutline,
                                contentDescription = "Удалить ${item.title}",
                                modifier = Modifier.size(19.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RecentActivityWidget(events: List<ActivityEvent>) {
    WidgetCard(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)) {
        WidgetHeader(
            title = "Последние события",
            count = events.size,
            icon = Icons.Default.History,
            accent = MaterialTheme.colorScheme.primary,
        )
        events.forEachIndexed { index, event ->
            if (index > 0) WidgetDivider()
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier.size(32.dp).background(
                        MaterialTheme.colorScheme.primaryContainer,
                        CircleShape,
                    ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(17.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        event.text,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        formatDashboardTime(event.createdAtMillis),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun WidgetCard(
    containerColor: Color,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 17.dp)) {
            content()
        }
    }
}

@Composable
private fun WidgetHeader(
    title: String,
    count: Int,
    icon: ImageVector,
    accent: Color,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(40.dp).background(Color.White.copy(alpha = 0.72f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(21.dp))
        }
        Spacer(Modifier.width(11.dp))
        Text(
            title,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Surface(shape = CircleShape, color = Color.White.copy(alpha = 0.62f)) {
            Text(
                count.toString(),
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                style = MaterialTheme.typography.labelLarge,
                color = accent,
                fontWeight = FontWeight.Bold,
            )
        }
    }
    Spacer(Modifier.height(9.dp))
}

@Composable
private fun WidgetEmpty(text: String) {
    Text(
        text,
        modifier = Modifier.fillMaxWidth().padding(vertical = 18.dp),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun WidgetDivider() {
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f))
}

@Composable
private fun MemberAvatar(name: String, color: Color, size: Int) {
    Box(
        modifier = Modifier.size(size.dp).background(color, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = name.trim().firstOrNull()?.uppercase() ?: "?",
            style = MaterialTheme.typography.labelLarge,
            color = Color.White,
            fontWeight = FontWeight.Bold,
        )
    }
}

private fun avatarColor(value: String): Color =
    runCatching { Color(parseColor(value)) }.getOrDefault(PistachioStrong)

private fun formatDashboardTime(timestamp: Long): String =
    SimpleDateFormat("d MMM, HH:mm", Locale("ru")).format(Date(timestamp))

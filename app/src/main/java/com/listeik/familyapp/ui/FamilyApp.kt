package com.listeik.familyapp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Redeem
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.listeik.familyapp.data.model.ActivityEvent
import com.listeik.familyapp.data.model.FamilyItem
import com.listeik.familyapp.data.model.FamilyMessage
import com.listeik.familyapp.data.model.FamilySession
import com.listeik.familyapp.data.model.ItemCategory
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class HomeTab(val label: String, val icon: ImageVector) {
    BOARD("Главная", Icons.Default.Home),
    CHAT("Чат", Icons.Default.ChatBubbleOutline),
    ACTIVITY("История", Icons.Default.History),
}

private enum class OnboardingMode(val label: String) {
    CREATE("Создать семью"),
    JOIN("Войти по коду"),
}

@Composable
private fun familyBackgroundBrush(): Brush =
    Brush.verticalGradient(
        if (isSystemInDarkTheme()) {
            listOf(Color(0xFF17231A), MaterialTheme.colorScheme.background, Color(0xFF231E18))
        } else {
            listOf(Color(0xFFF1F9F0), MaterialTheme.colorScheme.background, Color(0xFFFFFAF3))
        },
    )

@Composable
fun FamilyApp(viewModel: FamilyViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            state.isLoading -> LoadingScreen()
            state.session == null -> OnboardingScreen(
                isWorking = state.isWorking,
                onCreateFamily = viewModel::createFamily,
                onJoinFamily = viewModel::joinFamily,
            )
            else -> FamilyHome(
                state = state,
                onCreateItem = viewModel::createItem,
                onMoveItemForward = viewModel::moveItemForward,
                onAdjustFoodPortions = viewModel::adjustFoodPortions,
                onSetItemCompleted = viewModel::setItemCompleted,
                onDeleteItem = viewModel::deleteItem,
                onSendMessage = viewModel::sendMessage,
            )
        }
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

@Composable
fun FirebaseSetupScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(72.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.NotificationsActive,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(34.dp),
                )
            }
        }
        Spacer(Modifier.height(20.dp))
        Text("Нужно подключить Firebase", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(10.dp))
        Text(
            text = "Добавьте Android-приложение com.listeik.familyapp в Firebase, скачайте google-services.json и поместите его в папку app.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.widthIn(max = 520.dp),
        )
    }
}

@Composable
private fun LoadingScreen() {
    Box(
        modifier = Modifier.fillMaxSize().background(familyBackgroundBrush()),
        contentAlignment = Alignment.Center,
    ) {
        LinearProgressIndicator(modifier = Modifier.width(180.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OnboardingScreen(
    isWorking: Boolean,
    onCreateFamily: (String, String) -> Unit,
    onJoinFamily: (String, String) -> Unit,
) {
    var mode by rememberSaveable { mutableStateOf(OnboardingMode.CREATE) }
    var userName by rememberSaveable { mutableStateOf("") }
    var familyName by rememberSaveable { mutableStateOf("") }
    var inviteCode by rememberSaveable { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(familyBackgroundBrush())
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(76.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Group,
                    contentDescription = null,
                    modifier = Modifier.size(38.dp),
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        Text("Домашний круг", style = MaterialTheme.typography.headlineMedium)
        Text(
            "Общие дела, покупки и семейный чат",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(28.dp))

        Column(modifier = Modifier.widthIn(max = 520.dp)) {
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                OnboardingMode.entries.forEachIndexed { index, item ->
                    SegmentedButton(
                        selected = mode == item,
                        onClick = { mode = item },
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = OnboardingMode.entries.size,
                        ),
                        label = { Text(item.label) },
                    )
                }
            }
            Spacer(Modifier.height(20.dp))
            OutlinedTextField(
                value = userName,
                onValueChange = { userName = it.take(80) },
                label = { Text("Ваше имя") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))
            if (mode == OnboardingMode.CREATE) {
                OutlinedTextField(
                    value = familyName,
                    onValueChange = { familyName = it.take(80) },
                    label = { Text("Название семьи") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                OutlinedTextField(
                    value = inviteCode,
                    onValueChange = { inviteCode = it.uppercase().take(6) },
                    label = { Text("Код приглашения") },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Spacer(Modifier.height(20.dp))
            Button(
                onClick = {
                    if (mode == OnboardingMode.CREATE) {
                        onCreateFamily(familyName, userName)
                    } else {
                        onJoinFamily(inviteCode, userName)
                    }
                },
                enabled = !isWorking,
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) {
                Text(if (mode == OnboardingMode.CREATE) "Создать" else "Войти")
            }
            if (isWorking) {
                Spacer(Modifier.height(12.dp))
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FamilyHome(
    state: FamilyUiState,
    onCreateItem: (String, ItemCategory, Int?) -> Unit,
    onMoveItemForward: (FamilyItem) -> Unit,
    onAdjustFoodPortions: (FamilyItem, Int) -> Unit,
    onSetItemCompleted: (FamilyItem, Boolean) -> Unit,
    onDeleteItem: (FamilyItem) -> Unit,
    onSendMessage: (String) -> Unit,
) {
    val session = requireNotNull(state.session)
    var selectedTab by rememberSaveable { mutableStateOf(HomeTab.BOARD) }
    var showCreateDialog by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = {
            Column {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            when (selectedTab) {
                                HomeTab.BOARD -> "Домашний круг"
                                HomeTab.CHAT -> "Семейный чат"
                                HomeTab.ACTIVITY -> "История"
                            },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontWeight = FontWeight.SemiBold,
                        )
                    },
                )
                if (state.isWorking) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
            }
        },
        bottomBar = {
            NavigationBar {
                HomeTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label) },
                    )
                }
            }
        },
        floatingActionButton = {
            if (selectedTab == HomeTab.BOARD) {
                FloatingActionButton(
                    onClick = { showCreateDialog = true },
                    shape = CircleShape,
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Добавить")
                }
            }
        },
    ) { padding ->
        when (selectedTab) {
            HomeTab.BOARD -> DashboardScreen(
                session = session,
                members = state.members,
                items = state.items,
                events = state.events,
                onAdjustFoodPortions = onAdjustFoodPortions,
                onSetItemCompleted = onSetItemCompleted,
                onMoveItemForward = onMoveItemForward,
                onDeleteItem = onDeleteItem,
                contentPadding = padding,
            )
            HomeTab.CHAT -> ChatScreen(
                session = session,
                messages = state.messages,
                onSendMessage = onSendMessage,
                contentPadding = padding,
            )
            HomeTab.ACTIVITY -> ActivityScreen(state.events, padding)
        }
    }

    if (showCreateDialog) {
        CreateItemDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { title, category, portions ->
                onCreateItem(title, category, portions)
                showCreateDialog = false
            },
        )
    }
}

@Composable
private fun ChatScreen(
    session: FamilySession,
    messages: List<FamilyMessage>,
    onSendMessage: (String) -> Unit,
    contentPadding: PaddingValues,
) {
    var text by rememberSaveable { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.lastIndex)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(familyBackgroundBrush())
            .padding(top = contentPadding.calculateTopPadding(), bottom = contentPadding.calculateBottomPadding())
            .imePadding(),
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (messages.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 64.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Icon(Icons.Default.ChatBubbleOutline, null, modifier = Modifier.size(44.dp))
                        Spacer(Modifier.height(12.dp))
                        Text("Начните семейный разговор")
                    }
                }
            }
            items(messages, key = { it.id }) { message ->
                MessageBubble(message, isMine = message.senderId == session.userId)
            }
        }
        Surface(
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.97f),
            shadowElevation = 4.dp,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it.take(500) },
                    placeholder = { Text("Сообщение") },
                    maxLines = 4,
                    shape = RoundedCornerShape(22.dp),
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        onSendMessage(text)
                        text = ""
                    },
                    enabled = text.isNotBlank(),
                    modifier = Modifier.size(48.dp).background(
                        if (text.isNotBlank()) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        },
                        CircleShape,
                    ),
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Отправить",
                        tint = if (text.isNotBlank()) Color.White else MaterialTheme.colorScheme.outline,
                    )
                }
            }
        }
    }
}

@Composable
private fun MessageBubble(message: FamilyMessage, isMine: Boolean) {
    Box(modifier = Modifier.fillMaxWidth()) {
        Card(
            modifier = Modifier
                .widthIn(max = 320.dp)
                .align(if (isMine) Alignment.CenterEnd else Alignment.CenterStart),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isMine) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceContainerHigh
                },
            ),
        ) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp)) {
                if (!isMine) {
                    Text(
                        message.senderName,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                Text(message.text)
                Text(
                    formatTime(message.createdAtMillis),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.End),
                )
            }
        }
    }
}

@Composable
private fun ActivityScreen(events: List<ActivityEvent>, contentPadding: PaddingValues) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(familyBackgroundBrush()),
        contentPadding = PaddingValues(
            start = 16.dp,
            top = contentPadding.calculateTopPadding() + 12.dp,
            end = 16.dp,
            bottom = contentPadding.calculateBottomPadding() + 16.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (events.isEmpty()) {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 64.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(Icons.Default.History, null, modifier = Modifier.size(44.dp))
                    Spacer(Modifier.height(12.dp))
                    Text("События появятся здесь")
                }
            }
        }
        items(events, key = { it.id }) { event ->
            EventRow(event)
        }
    }
}

@Composable
private fun EventRow(event: ActivityEvent) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier.size(40.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(19.dp))
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(event.text, style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(3.dp))
                Text(
                    formatDateTime(event.createdAtMillis),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun CreateItemDialog(
    onDismiss: () -> Unit,
    onCreate: (String, ItemCategory, Int?) -> Unit,
) {
    var title by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf(ItemCategory.TASK) }
    var portions by rememberSaveable { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Новое для семьи") },
        text = {
            Column {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(ItemCategory.entries) { item ->
                        AssistChip(
                            onClick = { category = item },
                            label = { Text(item.label) },
                            leadingIcon = {
                                Icon(
                                    categoryIcon(item),
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                )
                            },
                            colors = if (category == item) {
                                androidx.compose.material3.AssistChipDefaults.assistChipColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                )
                            } else {
                                androidx.compose.material3.AssistChipDefaults.assistChipColors()
                            },
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it.take(140) },
                    label = { Text("Название") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (category == ItemCategory.FOOD) {
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = portions,
                        onValueChange = { value -> portions = value.filter(Char::isDigit).take(3) },
                        label = { Text("Количество в холодильнике") },
                        placeholder = { Text("Например, 8") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onCreate(title, category, portions.toIntOrNull()) },
                enabled = title.isNotBlank() &&
                    (category != ItemCategory.FOOD || portions.toIntOrNull() != null),
            ) {
                Text("Добавить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        },
        shape = RoundedCornerShape(28.dp),
    )
}

private fun categoryIcon(category: ItemCategory): ImageVector =
    when (category) {
        ItemCategory.FOOD -> Icons.Default.Restaurant
        ItemCategory.BUY -> Icons.Default.ShoppingCart
        ItemCategory.TASK -> Icons.Default.Checklist
        ItemCategory.WISH -> Icons.Default.Redeem
    }

private fun formatTime(timestamp: Long): String =
    SimpleDateFormat("HH:mm", Locale("ru")).format(Date(timestamp))

private fun formatDateTime(timestamp: Long): String =
    SimpleDateFormat("d MMM, HH:mm", Locale("ru")).format(Date(timestamp))

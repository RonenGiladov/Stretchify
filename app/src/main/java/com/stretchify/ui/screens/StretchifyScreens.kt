package com.stretchify.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandVertically
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.findRootCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stretchify.data.ProgressSummary
import com.stretchify.data.DelightPresentation
import com.stretchify.ui.components.DelightCard
import com.stretchify.ui.components.HomeRewards
import com.stretchify.ui.components.SessionProgressCard
import com.stretchify.session.SessionProgressMoment
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.isActive
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.stretchify.data.GoalProgressCalculator
import com.stretchify.data.GoalWeek
import com.stretchify.data.GoalCatalog
import com.stretchify.model.DashboardCard
import com.stretchify.model.AlertMode
import com.stretchify.model.AlertTiming
import com.stretchify.model.DashboardCardType
import com.stretchify.model.StretchRoutine
import com.stretchify.model.GoalRoutine
import com.stretchify.model.LiquidPreset
import com.stretchify.model.GlassFinish
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.RadioButton
import com.stretchify.model.ThemePreference
import com.stretchify.ui.theme.LocalLiquidPreset
import com.stretchify.ui.components.liquidSurface
import androidx.activity.compose.BackHandler
import com.stretchify.session.SessionPhase
import com.stretchify.session.SessionState
import com.stretchify.ui.StretchifyEvent
import com.stretchify.ui.StretchifyUiState
import com.stretchify.ui.TopLevelDestination
import com.stretchify.ui.CustomRoutineStep
import com.stretchify.ui.components.GlassBackground
import com.stretchify.ui.components.GlassCard
import com.stretchify.ui.components.GlassTimerPanel
import com.stretchify.ui.components.TrainerMessageBubble
import com.stretchify.ui.components.navigationGlassBorderColor
import com.stretchify.ui.components.navigationHazeStyle
import com.stretchify.ui.theme.filledCardStyle
import com.stretchify.ui.theme.LocalFilledCard
import com.stretchify.ui.theme.LocalFilledCardAccentColor
import com.stretchify.ui.theme.LocalFilledCardSecondaryColor
import com.stretchify.ui.theme.LocalColorfulLight
import com.stretchify.ui.theme.LocalColorfulDark
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import dev.chrisbanes.haze.hazeChild
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import java.time.Instant
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private const val WEEKLY_SESSION_GOAL = 3

@Composable
fun TopLevelScreen(
    uiState: StretchifyUiState,
    onEvent: (StretchifyEvent) -> Unit,
    homeListState: LazyListState,
    libraryListState: LazyListState,
    progressListState: LazyListState
)
{
    val snackbarHostState = remember { SnackbarHostState() }
    val hazeState = remember { HazeState() }
    val coroutineScope = rememberCoroutineScope()
    val selectedListState = when (uiState.selectedDestination)
    {
        TopLevelDestination.Home -> homeListState
        TopLevelDestination.Library -> libraryListState
        TopLevelDestination.Progress -> progressListState
    }
    LaunchedEffect(uiState.removedCard)
    {
        if (uiState.removedCard != null)
        {
            val result = snackbarHostState.showSnackbar(
                message = "Card removed from Home",
                actionLabel = "Undo",
                withDismissAction = true,
                duration = SnackbarDuration.Long
            )
            if (result == SnackbarResult.ActionPerformed)
            {
                onEvent(StretchifyEvent.UndoRemoveCard)
            }
        }
    }

    GlassBackground {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val isExpanded = maxWidth >= 600.dp
            if (isExpanded)
            {
                Row(modifier = Modifier.fillMaxSize()) {
                    DestinationRail(uiState.selectedDestination, onEvent, hazeState)
                    TopLevelContent(
                        uiState = uiState,
                        onEvent = onEvent,
                        modifier = Modifier
                            .weight(1f)
                            .haze(hazeState),
                        snackbarHostState = snackbarHostState,
                        homeListState = homeListState,
                        libraryListState = libraryListState,
                        progressListState = progressListState
                    )
                }
            }
            else
            {
                TopLevelDestinationContent(
                    uiState = uiState,
                    onEvent = onEvent,
                    modifier = Modifier
                        .fillMaxSize()
                        .haze(hazeState),
                    homeListState = homeListState,
                    libraryListState = libraryListState,
                    progressListState = progressListState
                )
                SnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(start = 20.dp, end = 20.dp, bottom = 112.dp)
                )
                DestinationBar(
                    selected = uiState.selectedDestination,
                    onEvent = onEvent,
                    hazeState = hazeState,
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
            AnimatedVisibility(
                visible = selectedListState.canScrollBackward,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .navigationBarsPadding()
                    .padding(end = 24.dp, bottom = if (isExpanded) 24.dp else 112.dp)
            ) {
                FloatingActionButton(
                    onClick = { coroutineScope.launch { selectedListState.animateScrollToItem(0) } },
                    modifier = Modifier
                        .testTag("back-to-top-button")
                        .semantics { contentDescription = "Back to top" }
                ) {
                    Text("↑", style = MaterialTheme.typography.headlineMedium)
                }
            }
        }
    }
}

@Composable
private fun TopLevelContent(
    uiState: StretchifyUiState,
    onEvent: (StretchifyEvent) -> Unit,
    modifier: Modifier,
    snackbarHostState: SnackbarHostState,
    homeListState: LazyListState,
    libraryListState: LazyListState,
    progressListState: LazyListState
)
{
    Scaffold(
        modifier = modifier,
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        TopLevelDestinationContent(
            uiState,
            onEvent,
            Modifier.padding(paddingValues),
            homeListState,
            libraryListState,
            progressListState
        )
    }
}

@Composable
private fun TopLevelDestinationContent(
    uiState: StretchifyUiState,
    onEvent: (StretchifyEvent) -> Unit,
    modifier: Modifier,
    homeListState: LazyListState,
    libraryListState: LazyListState,
    progressListState: LazyListState
)
{
    val listState = when (uiState.selectedDestination)
    {
        TopLevelDestination.Home -> homeListState
        TopLevelDestination.Library -> libraryListState
        TopLevelDestination.Progress -> progressListState
    }
    val edgeFadeModifier = if (LocalColorfulLight.current || LocalColorfulDark.current)
    {
        Modifier
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
            .drawWithContent {
                drawContent()
                drawRect(
                    brush = Brush.verticalGradient(
                        0f to if (listState.canScrollBackward) Color.Transparent else Color.Black,
                        0.08f to Color.Black,
                        0.82f to Color.Black,
                        1f to if (listState.canScrollForward) Color.Transparent else Color.Black
                    ),
                    blendMode = BlendMode.DstIn
                )
            }
    }
    else
    {
        Modifier
    }
    AnimatedContent(
        targetState = uiState.selectedDestination,
        modifier = modifier.fillMaxSize().then(edgeFadeModifier),
        transitionSpec = {
            val direction = if (targetState.ordinal > initialState.ordinal) 1 else -1
            (fadeIn(tween(180)) + slideInHorizontally(tween(240)) { width -> direction * width / 12 }) togetherWith
                (fadeOut(tween(140)) + slideOutHorizontally(tween(200)) { width -> -direction * width / 16 })
        },
        label = "tab transition"
    ) { destination ->
        when (destination)
        {
            TopLevelDestination.Home -> HomeScreen(uiState, onEvent, Modifier.fillMaxSize(), homeListState)
            TopLevelDestination.Library -> LibraryScreen(uiState, onEvent, Modifier.fillMaxSize(), libraryListState)
            TopLevelDestination.Progress -> ProgressScreen(uiState, onEvent, Modifier.fillMaxSize(), progressListState)
        }
    }
}

@Composable
private fun DestinationBar(
    selected: TopLevelDestination,
    onEvent: (StretchifyEvent) -> Unit,
    hazeState: HazeState,
    modifier: Modifier = Modifier
)
{
    val shape = androidx.compose.foundation.shape.RoundedCornerShape(32.dp)
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(start = 18.dp, end = 18.dp, bottom = 10.dp)
            .shadow(20.dp, shape, ambientColor = Color.Black.copy(alpha = 0.22f))
            .clip(shape)
            .then(
                if (LocalLiquidPreset.current != null) Modifier
                    .hazeChild(hazeState, navigationHazeStyle())
                    .liquidSurface(32.dp, isGlass = true)
                else Modifier.hazeChild(hazeState, navigationHazeStyle())
            )
            .border(
                width = 1.dp,
                color = if (LocalLiquidPreset.current != null) Color.Transparent else navigationGlassBorderColor(),
                shape = shape
            )
            .testTag("bottom-navigation"),
        color = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = shape,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            TopLevelDestination.entries.forEach { destination ->
                val isSelected = selected == destination
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 56.dp)
                        .clip(androidx.compose.foundation.shape.RoundedCornerShape(24.dp))
                        .background(
                            if (isSelected)
                            {
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                            }
                            else
                            {
                                androidx.compose.ui.graphics.Color.Transparent
                            }
                        )
                        .clickable(role = Role.Button) {
                            onEvent(StretchifyEvent.SelectDestination(destination))
                        }
                        .semantics { contentDescription = "${destination.name} tab" }
                        .padding(vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        destination.symbol,
                        color = if (isSelected)
                        {
                            MaterialTheme.colorScheme.primary
                        }
                        else
                        {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                    Text(
                        destination.name,
                        color = if (isSelected)
                        {
                            MaterialTheme.colorScheme.onSurface
                        }
                        else
                        {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun DestinationRail(
    selected: TopLevelDestination,
    onEvent: (StretchifyEvent) -> Unit,
    hazeState: HazeState
)
{
    NavigationRail(
        modifier = Modifier
            .fillMaxHeight()
            .safeDrawingPadding()
            .then(
                if (LocalLiquidPreset.current != null) Modifier
                    .hazeChild(hazeState, navigationHazeStyle())
                    .liquidSurface(32.dp, isGlass = true)
                else Modifier.hazeChild(hazeState, navigationHazeStyle())
            )
            .border(
                width = 1.dp,
                color = if (LocalLiquidPreset.current != null) Color.Transparent else navigationGlassBorderColor()
            )
            .testTag("navigation-rail"),
        containerColor = Color.Transparent
    ) {
        Spacer(modifier = Modifier.height(24.dp))
        TopLevelDestination.entries.forEach { destination ->
            NavigationRailItem(
                selected = selected == destination,
                onClick = { onEvent(StretchifyEvent.SelectDestination(destination)) },
                icon = { Text(destination.symbol) },
                label = { Text(destination.name) },
                modifier = Modifier.semantics { contentDescription = "${destination.name} tab" }
            )
        }
    }
}

private val TopLevelDestination.symbol: String
    get() = when (this)
    {
        TopLevelDestination.Home -> "⌂"
        TopLevelDestination.Library -> "▦"
        TopLevelDestination.Progress -> "↗"
    }

@Composable
private fun ScreenHeader(title: String, subtitle: String, onSettings: () -> Unit)
{
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.semantics { heading() }
            )
            Text(
                text = subtitle,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.72f),
                style = MaterialTheme.typography.bodyLarge
            )
        }
        IconButton(
            onClick = onSettings,
            modifier = Modifier.semantics { contentDescription = "Open settings" }
        ) {
            Text("⚙", style = MaterialTheme.typography.titleLarge)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    uiState: StretchifyUiState,
    onEvent: (StretchifyEvent) -> Unit,
    modifier: Modifier = Modifier,
    listState: LazyListState
)
{
    var isRepeatSheetVisible by remember { mutableStateOf(false) }
    val lastCompletedRoutine = uiState.lastCompletedRoutine
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(lifecycle)
    {
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED)
        {
            onEvent(StretchifyEvent.CheckWelcomeBack)
            while (isActive)
            {
                onEvent(StretchifyEvent.RefreshRewards)
                val now = Instant.now()
                val midnight = now.atZone(ZoneId.systemDefault()).toLocalDate().plusDays(1)
                    .atStartOfDay(ZoneId.systemDefault()).toInstant()
                delay((midnight.toEpochMilli() - now.toEpochMilli()).coerceAtLeast(1000L))
            }
        }
    }
    LazyColumn(
        state = listState,
        modifier = modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .testTag("home-screen"),
        contentPadding = PaddingValues(start = 20.dp, top = 20.dp, end = 20.dp, bottom = 140.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            ScreenHeader(
                title = "Stretchify",
                subtitle = "Move better, one calm reset at a time.",
                onSettings = { onEvent(StretchifyEvent.OpenSettings) }
            )
        }
        if (uiState.welcomeBackRecordId != null)
        {
            item {
                var isWelcomeVisible by remember(uiState.welcomeBackRecordId) { mutableStateOf(false) }
                LaunchedEffect(uiState.welcomeBackRecordId, isWelcomeVisible)
                {
                    if (isWelcomeVisible)
                    {
                        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED)
                        {
                            onEvent(StretchifyEvent.PresentWelcomeBack(uiState.welcomeBackRecordId))
                        }
                    }
                }
                TrainerMessageBubble(
                    "Welcome back. A little time for yourself is a lovely place to begin.",
                    modifier = Modifier.onGloballyPositioned { coordinates ->
                        isWelcomeVisible = coordinates.boundsInWindow().overlaps(
                            coordinates.findRootCoordinates().boundsInWindow()
                        )
                    }
                )
            }
        }
        item(key = "home-rewards") {
            HomeRewards(uiState.rewards, uiState.screen == com.stretchify.ui.StretchifyScreen.TopLevel &&
                uiState.selectedDestination == TopLevelDestination.Home,
                onPresented = { onEvent(StretchifyEvent.PresentHomeRewards(it)) },
                onViewed = { onEvent(StretchifyEvent.ViewBadges(it)) })
        }
        item {
            if (lastCompletedRoutine != null)
            {
                AnimatedVisibility(
                    visible = !uiState.isDashboardEditing,
                    enter = fadeIn(tween(220)) + expandVertically(tween(220)),
                    exit = fadeOut(tween(150)) + shrinkVertically(tween(220))
                ) {
                    Column(modifier = Modifier.padding(bottom = 18.dp)) {
                        GlassCard(modifier = Modifier
                            .clip(androidx.compose.foundation.shape.RoundedCornerShape(24.dp))
                            .clickable(enabled = !uiState.isDashboardEditing) { isRepeatSheetVisible = true }
                            .testTag("repeat-last-routine-card")) {
                            Text("Pick up where you left off", style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold)
                            Text(lastCompletedRoutine.title, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Button(
                                onClick = { onEvent(StretchifyEvent.RepeatLastRoutine) },
                                enabled = !uiState.isDashboardEditing,
                                modifier = Modifier.fillMaxWidth().testTag("repeat-last-routine-button")
                            ) {
                                FilledButtonText("Repeat last routine")
                            }
                        }
                    }
                }
            }
            AnimatedContent(
                targetState = uiState.isDashboardEditing,
                transitionSpec = {
                    (fadeIn(tween(220)) + slideInVertically(tween(220)) { it / 4 }) togetherWith
                        (fadeOut(tween(150)) + slideOutVertically(tween(150)) { -it / 4 })
                },
                label = "Home edit actions"
            ) { isEditing ->
                if (isEditing)
                {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { onEvent(StretchifyEvent.OpenCardGallery) },
                            modifier = Modifier.weight(1f).testTag("add-card-button")
                        ) {
                            Text("Add")
                        }
                        OutlinedButton(
                            onClick = { onEvent(StretchifyEvent.ResetDashboard) },
                            modifier = Modifier.weight(1f).testTag("reset-dashboard-button")
                        ) {
                            Text("Reset")
                        }
                        Button(
                            onClick = { onEvent(StretchifyEvent.CloseDashboardEditor) },
                            modifier = Modifier.weight(1f).testTag("dashboard-done-button")
                        ) {
                            FilledButtonText("Done")
                        }
                    }
                }
                else
                {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        OutlinedButton(
                            onClick = { onEvent(StretchifyEvent.OpenDashboardEditor) },
                            modifier = Modifier.testTag("customize-home-button")
                        ) {
                            Text("Customize Home")
                        }
                    }
                }
            }
        }
        item {
            if (uiState.dashboardCards.isEmpty())
            {
                EmptyDashboard(onEvent)
            }
            else
            {
                DashboardGrid(
                    cards = uiState.dashboardCards,
                    uiState = uiState,
                    onSelectRoutine = { onEvent(StretchifyEvent.SelectRoutine(it)) },
                    onEvent = onEvent
                )
            }
        }
    }
    if (isRepeatSheetVisible && lastCompletedRoutine != null)
    {
        ModalBottomSheet(
            onDismissRequest = { isRepeatSheetVisible = false },
            modifier = Modifier.testTag("repeat-routine-sheet"),
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(start = 24.dp, end = 24.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(lastCompletedRoutine.title, style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold)
                Text(lastCompletedRoutine.goal, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    "${lastCompletedRoutine.estimatedDurationSeconds / 60} min · " +
                        "${lastCompletedRoutine.difficulty} · ${lastCompletedRoutine.targetAreas.joinToString()}"
                )
                Button(
                    onClick = {
                        isRepeatSheetVisible = false
                        onEvent(StretchifyEvent.RepeatLastRoutine)
                    },
                    modifier = Modifier.fillMaxWidth().testTag("sheet-start-routine")
                ) { FilledButtonText("Start routine") }
                OutlinedButton(
                    onClick = {
                        isRepeatSheetVisible = false
                        onEvent(StretchifyEvent.SelectRoutine(lastCompletedRoutine.id))
                    },
                    modifier = Modifier.fillMaxWidth().testTag("sheet-view-routine")
                ) { Text("View routine") }
            }
        }
    }
}

@Composable
private fun EmptyDashboard(onEvent: (StretchifyEvent) -> Unit)
{
    GlassCard {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Make this space yours", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("Add routines and progress cards for quick access.")
            Button(onClick = { onEvent(StretchifyEvent.OpenCardGallery) }) {
                FilledButtonText("Add card")
            }
        }
    }
}

@Composable
private fun DashboardGrid(
    cards: List<DashboardCard>,
    uiState: StretchifyUiState,
    onSelectRoutine: (String) -> Unit,
    onEvent: (StretchifyEvent) -> Unit
)
{
    AnimatedContent(
        targetState = cards,
        transitionSpec = {
            (fadeIn(animationSpec = tween(durationMillis = 280)) +
                scaleIn(initialScale = 0.98f, animationSpec = tween(durationMillis = 280))) togetherWith
                (fadeOut(animationSpec = tween(durationMillis = 180)) +
                    scaleOut(targetScale = 0.98f, animationSpec = tween(durationMillis = 180)))
        },
        label = "Dashboard layout"
    ) { displayedCards ->
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            var index = 0
            while (index < displayedCards.size)
            {
                val card = displayedCards[index]
                if (card.widthSpan == 2)
                {
                    DashboardCardContent(card, uiState, onSelectRoutine, onEvent, Modifier.fillMaxWidth())
                    index += 1
                }
                else
                {
                    val secondCard = displayedCards.getOrNull(index + 1)?.takeIf { it.widthSpan == 1 }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        DashboardCardContent(card, uiState, onSelectRoutine, onEvent, Modifier.weight(1f))
                        if (secondCard != null)
                        {
                            DashboardCardContent(secondCard, uiState, onSelectRoutine, onEvent, Modifier.weight(1f))
                        }
                        else
                        {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                    index += if (secondCard == null) 1 else 2
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DashboardCardContent(
    card: DashboardCard,
    uiState: StretchifyUiState,
    onSelectRoutine: (String) -> Unit,
    onEvent: (StretchifyEvent) -> Unit,
    modifier: Modifier
)
{
    val routine = uiState.catalog.firstOrNull { it.id == card.routineId }
    val goal = uiState.goals.firstOrNull { it.id == card.goalId }
    val goalWeek = goal?.let {
        GoalProgressCalculator.calculate(it, uiState.completionRecords, uiState.firstDayOfWeek).firstOrNull()
    }
    val routineToOpen = routine ?: uiState.catalog.firstOrNull().takeIf { card.type == DashboardCardType.Today }
    val cardShape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp)
    val cardPadding by animateDpAsState(
        targetValue = if (uiState.isDashboardEditing) 12.dp else 20.dp,
        animationSpec = tween(durationMillis = 220),
        label = "Dashboard card padding"
    )
    val cardInteractionModifier = if (uiState.isDashboardEditing)
    {
        Modifier
    }
    else
    {
        Modifier
            .clip(cardShape)
            .combinedClickable(
                role = Role.Button,
                onClick = {
                    if (goal != null) onEvent(StretchifyEvent.SelectGoal(goal.id))
                    else if (routineToOpen != null) onSelectRoutine(routineToOpen.id)
                },
                onLongClick = { onEvent(StretchifyEvent.OpenDashboardEditor) }
            )
    }
    val moveCardModifier = if (uiState.isDashboardEditing)
    {
        Modifier.pointerInput(card.id) {
            var dragX = 0f
            var dragY = 0f
            var moveOffset = 0
            val interactionThreshold = 8.dp.toPx()
            detectDragGestures(
                onDragStart = {
                    dragX = 0f
                    dragY = 0f
                    moveOffset = 0
                },
                onDragEnd = {
                    if (moveOffset != 0)
                    {
                        onEvent(StretchifyEvent.MoveCard(card.id, moveOffset))
                    }
                },
                onDragCancel = { moveOffset = 0 }
            ) { change, drag ->
                change.consume()
                dragX += drag.x
                dragY += drag.y
                if (moveOffset == 0 && (abs(dragX) > interactionThreshold || abs(dragY) > interactionThreshold))
                {
                    moveOffset = if (abs(dragX) > abs(dragY))
                    {
                        if (dragX > 0f) 1 else -1
                    }
                    else
                    {
                        if (dragY > 0f) 2 else -2
                    }
                }
            }
        }
    }
    else
    {
        Modifier
    }
    GlassCard(
        modifier = modifier
            .animateContentSize(animationSpec = tween(durationMillis = 280))
            .heightIn(min = (112 * card.heightSpan).dp)
            .testTag("dashboard-card-${card.id}")
            .then(cardInteractionModifier),
        contentPadding = PaddingValues(cardPadding),
        filledStyle = filledCardStyle(
            card.type == DashboardCardType.Routine || card.type == DashboardCardType.Goal,
            card.colorSeed ?: card.id.hashCode(),
            LocalColorfulDark.current
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("move-card-${card.id}")
                .then(moveCardModifier)
        ) {
            when (card.type)
            {
                DashboardCardType.Today -> SummaryCardText(
                    eyebrow = "TODAY",
                    title = if (uiState.rewards.hasCompletedToday) "You showed up today." else "Your next reset",
                    body = if (uiState.rewards.hasCompletedToday) "A moment for yourself, made."
                        else "Start with ${uiState.catalog.first().title}"
                )
                DashboardCardType.Routine -> SummaryCardText(
                    eyebrow = routine?.category?.uppercase() ?: "ROUTINE",
                    title = routine?.title ?: "Routine unavailable",
                    body = routine?.let { "${it.estimatedDurationSeconds / 60} min · ${it.difficulty}" } ?: ""
                )
                DashboardCardType.Goal ->
                {
                    val progress by animateFloatAsState(goalWeek?.progressFraction ?: 0f,
                        label = "Goal progress")
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        SummaryCardText(
                            eyebrow = "GOAL",
                            title = goal?.title ?: "Goal unavailable",
                            body = goalWeek?.let {
                                if (it.isMet) "✓ Met this week" else "${it.completed}/${it.target} this week"
                            } ?: ""
                        )
                        LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth()
                            .testTag("goal-progress-${goal?.id}"),
                            color = if (LocalFilledCard.current) Color.White else MaterialTheme.colorScheme.primary,
                            trackColor = if (LocalFilledCard.current) Color.White.copy(alpha = 0.25f)
                                else MaterialTheme.colorScheme.surfaceVariant)
                    }
                }
                DashboardCardType.WeeklyProgress -> {
                    val weeklySessions = uiState.progressSummary.weeklySessions
                    val progress by animateFloatAsState(
                        targetValue = (weeklySessions / WEEKLY_SESSION_GOAL.toFloat()).coerceIn(0f, 1f),
                        label = "Weekly session progress"
                    )
                    val summary: @Composable () -> Unit = {
                        SummaryCardText(
                            eyebrow = "THIS WEEK",
                            title = "$weeklySessions sessions",
                            body = "$WEEKLY_SESSION_GOAL-session goal"
                        )
                    }
                    val progressRing: @Composable () -> Unit = {
                        CircularProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .size(if (card.widthSpan == 1) 48.dp else 60.dp)
                                .semantics {
                                    contentDescription =
                                        "$weeklySessions of $WEEKLY_SESSION_GOAL sessions this week"
                                }
                                .testTag("weekly-progress-ring"),
                            color = if (LocalFilledCard.current) LocalFilledCardAccentColor.current
                            else MaterialTheme.colorScheme.primary,
                            trackColor = if (LocalFilledCard.current)
                                LocalFilledCardAccentColor.current.copy(alpha = 0.24f)
                            else MaterialTheme.colorScheme.surfaceVariant,
                            strokeWidth = 5.dp
                        )
                    }
                    if (card.widthSpan == 1)
                    {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            summary()
                            Spacer(modifier = Modifier.height(8.dp))
                            Box(modifier = Modifier.align(Alignment.End)) {
                                progressRing()
                            }
                        }
                    }
                    else
                    {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                summary()
                            }
                            progressRing()
                        }
                    }
                }
                DashboardCardType.Streak -> SummaryCardText(
                    eyebrow = "CURRENT STREAK",
                    title = "${uiState.progressSummary.currentStreak} days",
                    body = "Consistency over intensity."
                )
            }
        }
        AnimatedVisibility(
            visible = uiState.isDashboardEditing,
            enter = fadeIn(tween(220)) + expandVertically(tween(220)),
            exit = fadeOut(tween(150)) + shrinkVertically(tween(220))
        ) {
            InlineCardEditControls(card = card, uiState = uiState, onEvent = onEvent)
        }
    }
}

@Composable
private fun InlineCardEditControls(
    card: DashboardCard,
    uiState: StretchifyUiState,
    onEvent: (StretchifyEvent) -> Unit
)
{
    var isResizing by remember(card.id) { mutableStateOf(false) }
    val introductoryScale = remember(card.id) { Animatable(1f) }
    val activeScale by animateFloatAsState(
        targetValue = if (isResizing) 1.14f else 1f,
        animationSpec = tween(durationMillis = 140),
        label = "Resize handle scale"
    )
    val iconRotation by animateFloatAsState(
        targetValue = if (isResizing) 8f else 0f,
        animationSpec = tween(durationMillis = 140),
        label = "Resize icon rotation"
    )
    val handleColor by animateColorAsState(
        targetValue = if (isResizing)
        {
            if (LocalFilledCard.current) LocalFilledCardAccentColor.current
            else MaterialTheme.colorScheme.primary
        }
        else
        {
            if (LocalFilledCard.current) LocalFilledCardAccentColor.current.copy(alpha = 0.16f)
            else MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
        },
        animationSpec = tween(durationMillis = 140),
        label = "Resize handle color"
    )
    val iconColor by animateColorAsState(
        targetValue = if (isResizing)
        {
            if (LocalFilledCard.current && LocalColorfulLight.current) Color.White
            else if (LocalFilledCard.current) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onPrimary
        }
        else
        {
            if (LocalFilledCard.current) LocalFilledCardAccentColor.current
            else MaterialTheme.colorScheme.primary
        },
        animationSpec = tween(durationMillis = 140),
        label = "Resize icon color"
    )

    LaunchedEffect(card.id)
    {
        delay(180)
        introductoryScale.animateTo(1.12f, animationSpec = tween(durationMillis = 220))
        introductoryScale.animateTo(1f, animationSpec = tween(durationMillis = 280))
    }

    Spacer(modifier = Modifier.height(10.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "${card.widthSpan}×${card.heightSpan}",
            modifier = Modifier.weight(1f).testTag("resize-size-${card.id}"),
            color = if (LocalFilledCard.current) LocalFilledCardSecondaryColor.current
            else MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1
        )
        IconButton(
            onClick = { onEvent(StretchifyEvent.RemoveCard(card.id)) },
            modifier = Modifier.semantics {
                contentDescription = "Remove ${cardTitle(card, uiState)}"
            }
        ) {
            Text("×", style = MaterialTheme.typography.titleLarge)
        }
        Box(
            modifier = Modifier
                .size(48.dp)
                .graphicsLayer {
                    val scale = if (isResizing) activeScale else introductoryScale.value
                    scaleX = scale
                    scaleY = scale
                }
                .clip(androidx.compose.foundation.shape.CircleShape)
                .background(handleColor)
                .semantics {
                    contentDescription = "Drag to resize ${cardTitle(card, uiState)}"
                    stateDescription = if (isResizing) "Resizing" else "Resize handle"
                }
                .testTag("resize-card-${card.id}")
                .pointerInput(card.id) {
                    var resizeX = 0f
                    var resizeY = 0f
                    var widthDelta = 0
                    var heightDelta = 0
                    val interactionThreshold = 8.dp.toPx()
                    detectDragGestures(
                        onDragStart = {
                            resizeX = 0f
                            resizeY = 0f
                            widthDelta = 0
                            heightDelta = 0
                            isResizing = true
                        },
                        onDragEnd = {
                            if (widthDelta != 0 || heightDelta != 0)
                            {
                                onEvent(StretchifyEvent.ResizeCard(card.id, widthDelta, heightDelta))
                            }
                            isResizing = false
                        },
                        onDragCancel = {
                            widthDelta = 0
                            heightDelta = 0
                            isResizing = false
                        }
                    ) { change, drag ->
                        change.consume()
                        resizeX += drag.x
                        resizeY += drag.y
                        if (widthDelta == 0 && heightDelta == 0 &&
                            (abs(resizeX) > interactionThreshold || abs(resizeY) > interactionThreshold))
                        {
                            widthDelta = when
                            {
                                resizeX > interactionThreshold -> 1
                                resizeX < -interactionThreshold -> -1
                                else -> 0
                            }
                            heightDelta = when
                            {
                                resizeY > interactionThreshold -> 1
                                resizeY < -interactionThreshold -> -1
                                else -> 0
                            }
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "↘",
                color = iconColor,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.graphicsLayer { rotationZ = iconRotation }
            )
        }
    }
}

@Composable
private fun SummaryCardText(eyebrow: String, title: String, body: String)
{
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = eyebrow,
            color = if (LocalFilledCard.current) LocalFilledCardAccentColor.current
            else MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold
        )
        Text(text = title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(text = body, color = if (LocalFilledCard.current) LocalFilledCardSecondaryColor.current
            else MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun LibraryScreen(
    uiState: StretchifyUiState,
    onEvent: (StretchifyEvent) -> Unit,
    modifier: Modifier = Modifier,
    listState: LazyListState
)
{
    LazyColumn(
        state = listState,
        modifier = modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .testTag("library-screen"),
        contentPadding = PaddingValues(start = 20.dp, top = 20.dp, end = 20.dp, bottom = 140.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            ScreenHeader("Library", "Find the right reset for how you feel.") {
                onEvent(StretchifyEvent.OpenSettings)
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = !uiState.isGoalLibrarySelected,
                    onClick = { onEvent(StretchifyEvent.SelectLibrarySection(false)) },
                    label = { Text("Routines") })
                FilterChip(selected = uiState.isGoalLibrarySelected,
                    onClick = { onEvent(StretchifyEvent.SelectLibrarySection(true)) },
                    label = { Text("Goals") })
            }
        }
        if (uiState.isGoalLibrarySelected)
        {
            item {
                Button(onClick = { onEvent(StretchifyEvent.OpenGoalCreator) },
                    modifier = Modifier.fillMaxWidth().testTag("create-custom-goal")) {
                    FilledButtonText("Create custom goal")
                }
            }
            items(uiState.goals.size, key = { "goal-${uiState.goals[it].id}" }) { index ->
                val goal = uiState.goals[index]
                val isOnHome = uiState.dashboardCards.any { it.goalId == goal.id }
                GlassCard(modifier = Modifier.testTag("library-goal-${goal.id}")) {
                    Text(goal.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(goal.description)
                    Text("${goal.weeklyTargets.size} routines")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { onEvent(StretchifyEvent.SelectGoal(goal.id)) },
                            modifier = Modifier.weight(1f)) { Text("View") }
                        Button(onClick = { onEvent(StretchifyEvent.AddGoalCard(goal.id)) },
                            enabled = !isOnHome, modifier = Modifier.weight(1f)) {
                            FilledButtonText(if (isOnHome) "On Home" else "Add to Home")
                        }
                    }
                }
            }
        }
        else
        {
        item {
            Button(
                onClick = { onEvent(StretchifyEvent.OpenCustomRoutineCreator) },
                modifier = Modifier.fillMaxWidth().pulseOnFirstVisible()
                    .testTag("library-create-custom-routine-button")
            ) {
                FilledButtonText("Create custom routine")
            }
        }
        item {
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = { onEvent(StretchifyEvent.UpdateSearch(it)) },
                label = { Text("Search routines") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("routine-search")
            )
        }
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("All", "Posture", "Back", "Hips", "Neck", "Recovery", "Quick", "Custom").forEach { category ->
                    FilterChip(
                        selected = uiState.selectedCategory == category,
                        onClick = { onEvent(StretchifyEvent.SelectCategory(category)) },
                        label = { Text(category) }
                    )
                }
            }
        }
        if (uiState.searchQuery.isBlank() && uiState.selectedCategory == "All")
        {
            item { SectionTitle("Featured") }
            itemsWithCards("featured", uiState.catalog.filter { it.isFeatured }, uiState, onEvent)
            item { SectionTitle("All routines") }
        }
        if (uiState.filteredRoutines.isEmpty())
        {
            item {
                GlassCard {
                    SummaryCardText("NO RESULTS", "Try another search", "Clear a filter or search a body area.")
                }
            }
        }
        else
        {
            itemsWithCards("routines", uiState.filteredRoutines, uiState, onEvent)
        }
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.itemsWithCards(
    keyPrefix: String,
    routines: List<StretchRoutine>,
    uiState: StretchifyUiState,
    onEvent: (StretchifyEvent) -> Unit
)
{
    items(routines.size, key = { index -> "$keyPrefix-${routines[index].id}" }) { index ->
        val routine = routines[index]
        RoutineLibraryCard(
            routine = routine,
            isOnHome = uiState.dashboardCards.any { it.routineId == routine.id },
            onOpen = { onEvent(StretchifyEvent.SelectRoutine(routine.id)) },
            onAdd = { onEvent(StretchifyEvent.AddRoutineCard(routine.id)) }
        )
    }
}

@Composable
private fun SectionTitle(text: String)
{
    Text(text, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
}

@Composable
private fun RoutineLibraryCard(
    routine: StretchRoutine,
    isOnHome: Boolean,
    onOpen: () -> Unit,
    onAdd: () -> Unit
)
{
    GlassCard(
        modifier = Modifier.testTag("library-routine-${routine.id}"),
        filledStyle = filledCardStyle(true, routine.id.hashCode(), LocalColorfulDark.current)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                routine.category.uppercase(),
                color = if (LocalFilledCard.current) LocalFilledCardAccentColor.current
                else MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
            Text(routine.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(routine.goal, color = if (LocalFilledCard.current) LocalFilledCardSecondaryColor.current
                else MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                "${routine.estimatedDurationSeconds / 60} min · ${routine.difficulty} · " +
                    routine.targetAreas.joinToString(),
                style = MaterialTheme.typography.labelMedium
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = onOpen,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = if (LocalFilledCard.current) LocalFilledCardAccentColor.current
                        else MaterialTheme.colorScheme.primary
                    ),
                    border = if (LocalFilledCard.current)
                        BorderStroke(1.dp, LocalFilledCardAccentColor.current.copy(alpha = 0.72f))
                    else ButtonDefaults.outlinedButtonBorder(enabled = true)
                ) {
                    Text("View")
                }
                Button(
                    onClick = onAdd,
                    enabled = !isOnHome,
                    border = if (LocalFilledCard.current)
                        BorderStroke(1.dp, LocalFilledCardAccentColor.current.copy(alpha = 0.72f))
                    else null,
                    colors = ButtonDefaults.buttonColors(
                        disabledContainerColor = if (LocalFilledCard.current)
                            LocalFilledCardAccentColor.current.copy(alpha = 0.12f)
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                        disabledContentColor = if (LocalFilledCard.current)
                            LocalFilledCardAccentColor.current.copy(alpha = 0.60f)
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("add-home-${routine.id}")
                ) {
                    FilledButtonText(if (isOnHome) "On Home" else "Add to Home")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProgressScreen(
    uiState: StretchifyUiState,
    onEvent: (StretchifyEvent) -> Unit,
    modifier: Modifier = Modifier,
    listState: LazyListState
)
{
    val summary = uiState.progressSummary
    var selectedRecordId by remember { mutableStateOf<String?>(null) }
    var isConfirmingSessionDelete by remember { mutableStateOf(false) }
    val selectedRecord = summary.recentRecords.firstOrNull { it.id == selectedRecordId }
    LazyColumn(
        state = listState,
        modifier = modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .testTag("progress-screen"),
        contentPadding = PaddingValues(start = 20.dp, top = 20.dp, end = 20.dp, bottom = 140.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            ScreenHeader("Progress", "Small sessions add up to lasting mobility.") {
                onEvent(StretchifyEvent.OpenSettings)
            }
        }
        item { ProgressCards(summary) }
        if (uiState.startedGoals.isNotEmpty())
        {
            item { SectionTitle("Goals") }
            items(uiState.startedGoals.size, key = { "progress-goal-${uiState.startedGoals[it].id}" }) { index ->
                val goal = uiState.startedGoals[index]
                val week = GoalProgressCalculator.calculate(goal, uiState.completionRecords,
                    uiState.firstDayOfWeek).first()
                GlassCard(modifier = Modifier.testTag("progress-goal-${goal.id}")) {
                    Text(goal.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("${week.completed}/${week.target} this week · ${if (week.isMet) "Met" else "In progress"}")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { onEvent(StretchifyEvent.SelectGoal(goal.id)) },
                            modifier = Modifier.weight(1f)) { Text("History") }
                        OutlinedButton(onClick = { onEvent(StretchifyEvent.OpenGoalEditor(goal.id)) },
                            modifier = Modifier.weight(1f)) { Text("Edit") }
                    }
                }
            }
        }
        if (summary.recentRecords.isEmpty())
        {
            item {
                GlassCard {
                    SummaryCardText(
                        "YOUR FIRST STEP",
                        "No sessions yet",
                        "Complete a routine and your progress will appear here."
                    )
                }
            }
            item {
                Button(onClick = { onEvent(StretchifyEvent.OpenHistoryEditor()) },
                    modifier = Modifier.fillMaxWidth().pulseOnFirstVisible()) { FilledButtonText("Add session") }
            }
        }
        else
        {
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically) {
                    SectionTitle("Recent sessions")
                    Button(onClick = { onEvent(StretchifyEvent.OpenHistoryEditor()) },
                        modifier = Modifier.pulseOnFirstVisible()) { FilledButtonText("Add session") }
                }
            }
            items(summary.recentRecords.size) { index ->
                val record = summary.recentRecords[index]
                val routine = uiState.catalog.firstOrNull { it.id == record.routineId }
                GlassCard(modifier = Modifier
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(24.dp))
                    .clickable {
                        selectedRecordId = record.id
                    }
                    .testTag("history-${record.id}"),
                    filledStyle = filledCardStyle(false, record.id.hashCode(), LocalColorfulDark.current)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                record.routineTitle ?: routine?.title ?: "Stretch session",
                                fontWeight = FontWeight.Bold
                            )
                            Text("${record.completedStepCount} stretches")
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("${record.elapsedSeconds / 60} min", color =
                                if (LocalFilledCard.current) LocalFilledCardAccentColor.current
                                else MaterialTheme.colorScheme.primary)
                            Text(
                                Instant.ofEpochMilli(record.completedAtMillis)
                                    .atZone(ZoneId.systemDefault())
                                    .format(DateTimeFormatter.ofPattern("d MMM yyyy")),
                                style = MaterialTheme.typography.labelMedium,
                                color = if (LocalFilledCard.current) LocalFilledCardSecondaryColor.current
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
    if (selectedRecord != null)
    {
        val routine = uiState.catalog.firstOrNull { it.id == selectedRecord.routineId }
        ModalBottomSheet(
            onDismissRequest = { selectedRecordId = null },
            modifier = Modifier.testTag("session-actions-sheet"),
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(start = 24.dp, end = 24.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    selectedRecord.routineTitle ?: routine?.title ?: "Stretch session",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    Instant.ofEpochMilli(selectedRecord.completedAtMillis)
                        .atZone(ZoneId.systemDefault())
                        .format(DateTimeFormatter.ofPattern("d MMM yyyy · HH:mm")),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text("${selectedRecord.elapsedSeconds / 60} min · ${selectedRecord.completedStepCount} stretches")
                Button(
                    onClick = {
                        selectedRecordId = null
                        onEvent(StretchifyEvent.OpenHistoryEditor(selectedRecord.id))
                    },
                    modifier = Modifier.fillMaxWidth().testTag("sheet-edit-session")
                ) { FilledButtonText("Edit session") }
                OutlinedButton(
                    onClick = { isConfirmingSessionDelete = true },
                    modifier = Modifier.fillMaxWidth().testTag("sheet-delete-session")
                ) { Text("Delete session", color = MaterialTheme.colorScheme.error) }
            }
        }
    }
    if (isConfirmingSessionDelete && selectedRecord != null)
    {
        AlertDialog(
            onDismissRequest = { isConfirmingSessionDelete = false },
            title = { Text("Delete this session?") },
            text = { Text("This will update your progress totals and cannot be undone.") },
            confirmButton = {
                Button(onClick = {
                    isConfirmingSessionDelete = false
                    selectedRecordId = null
                    onEvent(StretchifyEvent.DeleteHistoryRecord(selectedRecord.id))
                }) { FilledButtonText("Delete") }
            },
            dismissButton = {
                OutlinedButton(onClick = { isConfirmingSessionDelete = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun ProgressCards(summary: ProgressSummary)
{
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        GlassCard(modifier = Modifier.weight(1f), contentPadding = PaddingValues(14.dp),
            filledStyle = filledCardStyle(false, "progress-streak".hashCode(), LocalColorfulDark.current)) {
            SummaryCardText("STREAK", "${summary.currentStreak}", "days")
        }
        GlassCard(modifier = Modifier.weight(1f), contentPadding = PaddingValues(14.dp),
            filledStyle = filledCardStyle(false, "progress-week".hashCode(), LocalColorfulDark.current)) {
            SummaryCardText("THIS WEEK", "${summary.weeklySessions}", "sessions")
        }
        GlassCard(modifier = Modifier.weight(1f), contentPadding = PaddingValues(14.dp),
            filledStyle = filledCardStyle(false, "progress-total".hashCode(), LocalColorfulDark.current)) {
            SummaryCardText("TOTAL", "${summary.totalMinutes}", "minutes")
        }
    }
}

@Composable
fun RoutinePreviewScreen(
    routine: StretchRoutine,
    onStartSession: () -> Unit,
    onBack: () -> Unit,
    onEdit: () -> Unit
)
{
    FocusedScreen {
        BackHeader("Routine details", onBack)
        Text(routine.title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(routine.goal, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.76f))
        GlassCard {
            Text(
                "${routine.estimatedDurationSeconds / 60} min · ${routine.difficulty} · ${routine.targetAreas.joinToString()}",
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )
        }
        Button(
            onClick = onStartSession,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp)
                .pulseOnFirstVisible()
                .testTag("start-session-top-button")
        ) {
            FilledButtonText("Start routine")
        }
        TrainerMessageBubble("I'll guide the timing and transitions. Move only through a comfortable range.")
        routine.steps.forEachIndexed { index, step ->
            GlassCard {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("${index + 1}. ${step.stretch.name}", style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold)
                    Text(step.stretch.description, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${step.durationSeconds}s stretch · ${step.restSeconds}s rest",
                        color = MaterialTheme.colorScheme.primary)
                }
            }
        }
        Button(
            onClick = onStartSession,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp)
                .pulseOnFirstVisible()
                .testTag("start-session-button")
        ) {
            FilledButtonText("Start routine")
        }
        OutlinedButton(onClick = onEdit, modifier = Modifier.fillMaxWidth().testTag("edit-routine-button")) {
            Text("Edit routine")
        }
    }
}

@Composable
fun CountdownScreen(remainingSeconds: Int, onCancel: () -> Unit, onStartNow: () -> Unit)
{
    FocusedScreen {
        Text("Get ready", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        GlassTimerPanel(
            remainingSeconds = remainingSeconds,
            phaseLabel = "Starting in",
            modifier = Modifier.testTag("countdown-timer")
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f)) { Text("Cancel") }
            Button(onClick = onStartNow, modifier = Modifier.weight(1f).testTag("start-now-button")) {
                FilledButtonText("Start now")
            }
        }
    }
}

@Composable
fun ActiveSessionScreen(
    sessionState: SessionState,
    isExitConfirmationVisible: Boolean,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onSkip: () -> Unit,
    onExit: () -> Unit,
    onKeepStretching: () -> Unit,
    onConfirmExit: () -> Unit,
    progressMoments: Flow<SessionProgressMoment> = emptyFlow()
)
{
    val currentStep = sessionState.currentStep
    var isUsingEasierVersion by rememberSaveable(sessionState.routine.id, sessionState.currentStepIndex) {
        mutableStateOf(false)
    }
    val isPaused = sessionState.phase == SessionPhase.Paused
    val phaseLabel = when (sessionState.phase)
    {
        SessionPhase.Stretching -> "Stretch"
        SessionPhase.Resting -> "Rest"
        SessionPhase.Paused -> "Paused"
        else -> "Session"
    }
    FocusedScreen {
        BackHeader("Active session", onExit)
        GlassTimerPanel(
            remainingSeconds = sessionState.remainingSeconds,
            phaseLabel = phaseLabel,
            modifier = Modifier
                .testTag("timer-panel")
                .semantics {
                    contentDescription = "$phaseLabel timer ${sessionState.remainingSeconds} seconds remaining"
                }
        )
        SessionProgressCard(sessionState, progressMoments)
        GlassCard(modifier = Modifier.testTag("current-stretch-card")) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(currentStep.stretch.name, style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold)
                Text(
                    if (isUsingEasierVersion) currentStep.stretch.easierDescription ?: currentStep.stretch.description
                    else currentStep.stretch.description,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (currentStep.stretch.easierDescription != null && sessionState.phase != SessionPhase.Resting &&
                    sessionState.previousActivePhase != SessionPhase.Resting)
                {
                    OutlinedButton(
                        onClick = { isUsingEasierVersion = !isUsingEasierVersion },
                        modifier = Modifier.testTag("easier-version-button")
                    ) {
                        Text(if (isUsingEasierVersion) "Use standard version" else "Use easier version")
                    }
                }
            }
        }
        TrainerMessageBubble(
            message = if (sessionState.phase == SessionPhase.Resting)
            {
                "Nice. Shake it out and get ready for ${sessionState.nextStep?.stretch?.name ?: "the finish"}."
            }
            else
            {
                currentStep.stretch.trainerCue
            },
            modifier = Modifier.testTag("trainer-guidance")
        )
        GlassCard(modifier = Modifier.testTag("next-stretch-card")) {
            Text("Next: ${sessionState.nextStep?.stretch?.name ?: "Complete"}", fontWeight = FontWeight.SemiBold)
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .testTag("session-controls"),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SecondaryActionButton("Exit", "Exit session", Modifier.weight(1f), onExit)
            PrimaryActionButton(
                if (isPaused) "Resume" else "Pause",
                if (isPaused) "Resume session" else "Pause session",
                Modifier.weight(1f).testTag("pause-resume-button"),
                if (isPaused) onResume else onPause
            )
            SecondaryActionButton("Skip", "Skip current step", Modifier.weight(1f).testTag("skip-button"), onSkip)
        }
    }
    if (isExitConfirmationVisible)
    {
        AlertDialog(
            onDismissRequest = onKeepStretching,
            title = { Text("Exit session?") },
            text = { Text("Your current session will end without saving progress.") },
            confirmButton = {
                Button(onClick = onConfirmExit, modifier = Modifier.testTag("confirm-session-exit")) {
                    FilledButtonText("Exit session")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = onKeepStretching, modifier = Modifier.testTag("keep-stretching")) {
                    Text("Keep stretching")
                }
            },
            modifier = Modifier.testTag("session-exit-dialog")
        )
    }
}

@Composable
fun CompletionScreen(
    sessionState: SessionState,
    onRestart: () -> Unit,
    onHome: () -> Unit,
    onProgress: () -> Unit,
    delight: DelightPresentation? = null,
    shouldAnimateDelight: Boolean = false,
    onDelightPresented: (String) -> Unit = { }
)
{
    FocusedScreen {
        if (delight != null)
        {
            DelightCard(delight, shouldAnimateDelight, onDelightPresented)
        }
        else
        {
            Text("Session complete", style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.tertiary)
        }
        Text(sessionState.routine.title, style = MaterialTheme.typography.titleLarge)
        Text("${sessionState.elapsedSeconds / 60} min ${sessionState.elapsedSeconds % 60} sec of time for yourself.",
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Button(onClick = onProgress, modifier = Modifier.fillMaxWidth()) { FilledButtonText("View progress") }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SecondaryActionButton("Home", "Return to home", Modifier.weight(1f), onHome)
            PrimaryActionButton("Restart", "Restart session", Modifier.weight(1f), onRestart)
        }
    }
}

@Composable
fun SettingsScreen(
    selectedTheme: ThemePreference,
    selectedLiquidPreset: LiquidPreset = LiquidPreset.Aurora,
    onLiquidPresetSelected: (LiquidPreset) -> Unit = { },
    selectedGlassFinish: GlassFinish = GlassFinish.Clear,
    onGlassFinishSelected: (GlassFinish) -> Unit = { },
    onThemeSelected: (ThemePreference) -> Unit,
    selectedAlertMode: AlertMode,
    onAlertModeSelected: (AlertMode) -> Unit,
    selectedAlertTiming: AlertTiming,
    onAlertTimingSelected: (AlertTiming) -> Unit,
    selectedCountdownSeconds: Int,
    onCountdownSelected: (Int) -> Unit,
    isRemindersEnabled: Boolean,
    onRemindersEnabledChanged: (Boolean) -> Unit,
    reminderHour: Int,
    onReminderHourSelected: (Int) -> Unit,
    firstDayOfWeek: DayOfWeek,
    onFirstDayOfWeekSelected: (DayOfWeek) -> Unit,
    onBack: () -> Unit
)
{
    var isPresetMenuVisible by rememberSaveable { mutableStateOf(false) }
    if (isPresetMenuVisible)
    {
        BackHandler { isPresetMenuVisible = false }
        GlassBackground {
            LazyColumn(
                modifier = Modifier.fillMaxSize().safeDrawingPadding().testTag("liquid-preset-menu"),
                contentPadding = PaddingValues(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item { BackHeader("Color presets") { isPresetMenuVisible = false } }
                items(LiquidPreset.entries.size) { index ->
                    val preset = LiquidPreset.entries[index]
                    val isSelected = selectedLiquidPreset == preset
                    GlassCard(
                        modifier = Modifier
                            .testTag("liquid-preset-${preset.name.lowercase()}")
                            .semantics { selected = isSelected }
                            .clickable(role = Role.RadioButton) { onLiquidPresetSelected(preset) }
                    ) {
                        Box(
                            Modifier.fillMaxWidth().height(88.dp)
                                .clip(androidx.compose.foundation.shape.RoundedCornerShape(16.dp))
                                .liquidSurface(previewPreset = preset)
                        )
                        Spacer(Modifier.height(10.dp))
                        Text(
                            if (isSelected) "${preset.name} ✓" else preset.name,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
            }
        }
        return
    }
    FocusedScreen {
        BackHeader("Settings", onBack)
        Text("Week starts on", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DayOfWeek.entries.forEach { day ->
                FilterChip(selected = day == firstDayOfWeek,
                    onClick = { onFirstDayOfWeekSelected(day) },
                    label = { Text(day.name.lowercase().replaceFirstChar(Char::titlecase)) },
                    modifier = Modifier.testTag("week-start-${day.name.lowercase()}")
                )
            }
        }
        Text("Appearance", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("Stretchify follows your phone unless you choose a theme here.")
        ThemePreference.entries.forEach { preference ->
            SettingsOptionButton(
                label = when (preference)
                {
                    ThemePreference.ColorfulLight -> "Colorful Light"
                    ThemePreference.ColorfulDark -> "Colorful Dark"
                    else -> preference.name
                },
                isSelected = preference == selectedTheme,
                onClick = { onThemeSelected(preference) },
                testTag = "theme-${preference.name.lowercase()}"
            )
        }
        if (selectedTheme == ThemePreference.Liquid)
        {
            SettingsOptionButton(
                label = "Color presets · ${selectedLiquidPreset.name}",
                isSelected = false,
                onClick = { isPresetMenuVisible = true },
                testTag = "liquid-color-presets"
            )
            Text("Glass finish", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Column(Modifier.selectableGroup())
            {
                GlassFinish.entries.forEach { finish ->
                    Row(
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                            .testTag("glass-finish-${finish.name.lowercase()}")
                            .selectable(
                                selected = finish == selectedGlassFinish,
                                role = Role.RadioButton,
                                onClick = { onGlassFinishSelected(finish) }
                            )
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    )
                    {
                        RadioButton(selected = finish == selectedGlassFinish, onClick = null)
                        Text(
                            when (finish)
                            {
                                GlassFinish.Clear -> "Clear · More transparent"
                                GlassFinish.Balanced -> "Balanced · Calmer background"
                            }
                        )
                    }
                }
            }
        }
        Text("Session alerts", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("Choose how Stretchify lets you know when timers and routines finish.")
        AlertMode.entries.forEach { alertMode ->
            val label = when (alertMode)
            {
                AlertMode.SoundAndVibration -> "Sound + vibration"
                AlertMode.SoundOnly -> "Sound only"
                AlertMode.VibrationOnly -> "Vibration only"
                AlertMode.Off -> "Off"
            }
            SettingsOptionButton(
                label = label,
                isSelected = alertMode == selectedAlertMode,
                onClick = { onAlertModeSelected(alertMode) },
                testTag = "alert-mode-${alertMode.name.lowercase()}"
            )
        }
        Text("Alert timing", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        AlertTiming.entries.forEach { alertTiming ->
            val label = when (alertTiming)
            {
                AlertTiming.EveryTransition -> "Every transition"
                AlertTiming.StretchAndFinish -> "Stretch and finish"
            }
            SettingsOptionButton(
                label = label,
                isSelected = alertTiming == selectedAlertTiming,
                onClick = { onAlertTimingSelected(alertTiming) },
                testTag = "alert-timing-${alertTiming.name.lowercase()}"
            )
        }
        Text("Start countdown", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("Choose how much preparation time appears before each routine.")
        Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(0, 3, 5, 10, 15).forEach { seconds ->
                FilterChip(
                    selected = selectedCountdownSeconds == seconds,
                    onClick = { onCountdownSelected(seconds) },
                    label = { Text(if (seconds == 0) "Off" else "${seconds}s") },
                    modifier = Modifier.testTag("countdown-$seconds")
                )
            }
        }
        Text("Daily reminder", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("Get a gentle nudge around your chosen time. Notifications must be allowed for Stretchify.")
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically) {
            Text("Remind me daily")
            Switch(
                checked = isRemindersEnabled,
                onCheckedChange = onRemindersEnabledChanged,
                modifier = Modifier.testTag("daily-reminder-switch")
            )
        }
        if (isRemindersEnabled)
        {
            Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(8, 12, 18, 21).forEach { hour ->
                    FilterChip(
                        selected = reminderHour == hour,
                        onClick = { onReminderHourSelected(hour) },
                        label = { Text("%02d:00".format(hour)) },
                        modifier = Modifier.testTag("reminder-hour-$hour")
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsOptionButton(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    testTag: String
)
{
    val modifier = Modifier
        .fillMaxWidth()
        .testTag(testTag)
        .semantics { selected = isSelected }
    if (isSelected)
    {
        Button(onClick = onClick, modifier = modifier) {
            FilledButtonText(label)
        }
    }
    else
    {
        OutlinedButton(onClick = onClick, modifier = modifier) {
            Text(label)
        }
    }
}

private fun cardTitle(card: DashboardCard, uiState: StretchifyUiState): String
{
    return when (card.type)
    {
        DashboardCardType.Today -> "Today"
        DashboardCardType.Routine -> uiState.catalog.firstOrNull { it.id == card.routineId }?.title ?: "Routine"
        DashboardCardType.Goal -> uiState.goals.firstOrNull { it.id == card.goalId }?.title ?: "Goal"
        DashboardCardType.WeeklyProgress -> "Weekly progress"
        DashboardCardType.Streak -> "Streak"
    }
}

@Composable
fun CardGalleryScreen(uiState: StretchifyUiState, onEvent: (StretchifyEvent) -> Unit)
{
    FocusedScreen {
        BackHeader("Add a card", { onEvent(StretchifyEvent.NavigateBack) })
        Button(
            onClick = { onEvent(StretchifyEvent.OpenCustomRoutineCreator) },
            modifier = Modifier.fillMaxWidth().testTag("create-custom-routine-button")
        ) {
            FilledButtonText("Create custom routine")
        }
        Text("Summary cards", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        listOf(DashboardCardType.Today, DashboardCardType.WeeklyProgress, DashboardCardType.Streak).forEach { type ->
            val isAdded = uiState.dashboardCards.any { it.type == type }
            val title = if (type == DashboardCardType.WeeklyProgress) "Weekly sessions (progress ring)"
            else type.name.replace(Regex("([a-z])([A-Z])"), "$1 $2")
            GalleryRow(title, isAdded) {
                onEvent(StretchifyEvent.AddSummaryCard(type))
            }
        }
        Text("Routine cards", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        uiState.catalog.forEach { routine ->
            val isAdded = uiState.dashboardCards.any { it.routineId == routine.id }
            GalleryRow(routine.title, isAdded) { onEvent(StretchifyEvent.AddRoutineCard(routine.id)) }
        }
        Text("Goal cards", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        uiState.goals.forEach { goal ->
            val isAdded = uiState.dashboardCards.any { it.goalId == goal.id }
            GalleryRow(goal.title, isAdded) { onEvent(StretchifyEvent.AddGoalCard(goal.id)) }
        }
    }
}

@Composable
fun HistoryEditorScreen(uiState: StretchifyUiState, onEvent: (StretchifyEvent) -> Unit)
{
    val record = uiState.editingHistoryRecord
    val formatter = remember { DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm") }
    val initialDateTime = remember(record?.id) {
        LocalDateTime.ofInstant(
            Instant.ofEpochMilli(record?.completedAtMillis ?: System.currentTimeMillis()),
            ZoneId.systemDefault()
        ).format(formatter)
    }
    var routineId by rememberSaveable(record?.id) {
        mutableStateOf(record?.routineId ?: uiState.catalog.firstOrNull()?.id.orEmpty())
    }
    var dateTimeText by rememberSaveable(record?.id) { mutableStateOf(initialDateTime) }
    var durationText by rememberSaveable(record?.id) {
        mutableStateOf((record?.elapsedSeconds ?: 60).toString())
    }
    var stretchCountText by rememberSaveable(record?.id) {
        mutableStateOf((record?.completedStepCount ?: 1).toString())
    }
    var isConfirmingDelete by remember { mutableStateOf(false) }
    val parsedDateTime = runCatching { LocalDateTime.parse(dateTimeText, formatter) }.getOrNull()
    val durationSeconds = durationText.toIntOrNull()
    val stretchCount = stretchCountText.toIntOrNull()
    val canSave = routineId.isNotBlank() && parsedDateTime != null && durationSeconds != null &&
        durationSeconds > 0 && stretchCount != null && stretchCount >= 0

    FocusedScreen {
        BackHeader(if (record == null) "Add session" else "Edit session") {
            onEvent(StretchifyEvent.NavigateBack)
        }
        Text("Routine", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (record != null && uiState.catalog.none { it.id == record.routineId })
            {
                FilterChip(selected = routineId == record.routineId, onClick = { routineId = record.routineId },
                    label = { Text(record.routineTitle ?: "Deleted routine") })
            }
            uiState.catalog.forEach { routine ->
                FilterChip(selected = routineId == routine.id, onClick = { routineId = routine.id },
                    label = { Text(routine.title) })
            }
        }
        OutlinedTextField(
            value = dateTimeText,
            onValueChange = { dateTimeText = it },
            label = { Text("Date and time (yyyy-MM-dd HH:mm)") },
            isError = dateTimeText.isNotBlank() && parsedDateTime == null,
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("history-date-time")
        )
        OutlinedTextField(
            value = durationText,
            onValueChange = { durationText = it.filter(Char::isDigit).take(6) },
            label = { Text("Duration in seconds") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("history-duration")
        )
        OutlinedTextField(
            value = stretchCountText,
            onValueChange = { stretchCountText = it.filter(Char::isDigit).take(4) },
            label = { Text("Completed stretches") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("history-stretch-count")
        )
        Button(
            onClick = {
                val selectedRoutine = uiState.catalog.firstOrNull { it.id == routineId }
                onEvent(
                    StretchifyEvent.SaveHistoryRecord(
                        recordId = record?.id,
                        routineId = routineId,
                        routineTitle = selectedRoutine?.title ?: record?.routineTitle,
                        completedAtMillis = parsedDateTime!!.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
                        elapsedSeconds = durationSeconds!!,
                        completedStepCount = stretchCount!!
                    )
                )
            },
            enabled = canSave,
            modifier = Modifier.fillMaxWidth().testTag("save-history")
        ) { FilledButtonText("Save session") }
        if (record != null)
        {
            OutlinedButton(onClick = { isConfirmingDelete = true }, modifier = Modifier.fillMaxWidth()) {
                Text("Delete session")
            }
        }
    }
    if (isConfirmingDelete && record != null)
    {
        AlertDialog(
            onDismissRequest = { isConfirmingDelete = false },
            title = { Text("Delete this session?") },
            text = { Text("This will update your progress totals and cannot be undone.") },
            confirmButton = {
                Button(onClick = {
                    isConfirmingDelete = false
                    onEvent(StretchifyEvent.DeleteHistoryRecord(record.id))
                    onEvent(StretchifyEvent.NavigateBack)
                }) { FilledButtonText("Delete") }
            },
            dismissButton = { OutlinedButton(onClick = { isConfirmingDelete = false }) { Text("Cancel") } }
        )
    }
}

@Composable
fun CustomRoutineCreatorScreen(
    onEvent: (StretchifyEvent) -> Unit,
    routine: StretchRoutine? = null,
    isBuiltIn: Boolean = false,
    goals: List<GoalRoutine> = emptyList()
)
{
    var title by rememberSaveable(routine?.id) { mutableStateOf(routine?.title ?: "") }
    var goal by rememberSaveable(routine?.id) { mutableStateOf(routine?.goal ?: "") }
    var linkedGoalIds by remember(routine?.id) {
        mutableStateOf(goals.filter { routine?.id in it.weeklyTargets }.map { it.id }.toSet())
    }
    var stretchName by rememberSaveable { mutableStateOf("") }
    var durationText by rememberSaveable { mutableStateOf("60") }
    var restText by rememberSaveable { mutableStateOf("10") }
    var steps by remember(routine?.id) {
        mutableStateOf<List<CustomRoutineStep>>(routine?.steps?.map { step ->
            CustomRoutineStep(step.stretch.name, step.durationSeconds, step.restSeconds)
        } ?: emptyList())
    }
    var isConfirmingDestructiveAction by remember { mutableStateOf(false) }
    val durationSeconds = durationText.toIntOrNull()
    val restSeconds = restText.toIntOrNull()
    val canAddStretch = stretchName.isNotBlank() && durationSeconds != null && durationSeconds in 10..600 &&
        restSeconds != null && restSeconds in 0..300
    val canCreate = title.isNotBlank() && goal.isNotBlank() && steps.isNotEmpty() &&
        steps.all { it.name.isNotBlank() && it.durationSeconds in 10..600 && it.restSeconds in 0..300 }

    FocusedScreen {
        BackHeader(if (routine == null) "Custom routine" else "Edit routine",
            { onEvent(StretchifyEvent.NavigateBack) })
        Text(if (routine == null) "Create a routine card" else "Update ${routine.title}",
            style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("Build a sequence of timed stretches. A routine needs at least one stretch.")
        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Routine name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("custom-routine-title")
        )
        OutlinedTextField(
            value = goal,
            onValueChange = { goal = it },
            label = { Text("Goal or instructions") },
            modifier = Modifier.fillMaxWidth().testTag("custom-routine-goal")
        )
        if (goals.isNotEmpty())
        {
            Text("Linked goals", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            goals.forEach { linkedGoal ->
                FilterChip(
                    selected = linkedGoal.id in linkedGoalIds,
                    enabled = routine?.id !in linkedGoal.weeklyTargets || linkedGoal.weeklyTargets.size > 1 ||
                        linkedGoal.id !in linkedGoalIds,
                    onClick = {
                        linkedGoalIds = if (linkedGoal.id in linkedGoalIds) linkedGoalIds - linkedGoal.id
                            else linkedGoalIds + linkedGoal.id
                    },
                    label = { Text(linkedGoal.title) },
                    modifier = Modifier.testTag("routine-goal-${linkedGoal.id}")
                )
            }
        }
        OutlinedTextField(
            value = stretchName,
            onValueChange = { stretchName = it },
            label = { Text("Stretch name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("custom-stretch-name")
        )
        OutlinedTextField(
            value = durationText,
            onValueChange = { value -> durationText = value.filter(Char::isDigit).take(3) },
            label = { Text("Duration in seconds (10–600)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("custom-routine-duration")
        )
        OutlinedTextField(
            value = restText,
            onValueChange = { value -> restText = value.filter(Char::isDigit).take(3) },
            label = { Text("Rest after stretch in seconds (0–300)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("custom-routine-rest")
        )
        OutlinedButton(
            onClick = {
                steps = steps + CustomRoutineStep(
                    name = stretchName.trim(),
                    durationSeconds = durationSeconds!!,
                    restSeconds = restSeconds!!
                )
                stretchName = ""
                durationText = "60"
                restText = "10"
            },
            enabled = canAddStretch,
            modifier = Modifier.fillMaxWidth().testTag("add-custom-stretch")
        ) {
            Text("Add stretch")
        }
        if (steps.isNotEmpty())
        {
            Text("Routine sequence", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            steps.forEachIndexed { index, step ->
                GlassCard(contentPadding = PaddingValues(14.dp)) {
                    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Stretch ${index + 1}", fontWeight = FontWeight.SemiBold)
                        OutlinedTextField(
                            value = step.name,
                            onValueChange = { value ->
                                steps = steps.mapIndexed { stepIndex, item ->
                                    if (stepIndex == index) item.copy(name = value) else item
                                }
                            },
                            label = { Text("Stretch name") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = step.durationSeconds.toString(),
                                onValueChange = { value ->
                                    value.filter(Char::isDigit).toIntOrNull()?.takeIf { it in 10..600 }?.let { seconds ->
                                        steps = steps.mapIndexed { stepIndex, item ->
                                            if (stepIndex == index) item.copy(durationSeconds = seconds) else item
                                        }
                                    }
                                },
                                label = { Text("Stretch sec") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = step.restSeconds.toString(),
                                onValueChange = { value ->
                                    value.filter(Char::isDigit).toIntOrNull()?.takeIf { it in 0..300 }?.let { seconds ->
                                        steps = steps.mapIndexed { stepIndex, item ->
                                            if (stepIndex == index) item.copy(restSeconds = seconds) else item
                                        }
                                    }
                                },
                                label = { Text("Rest sec") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = {
                                if (index > 0)
                                {
                                    val reordered = steps.toMutableList()
                                    val item = reordered.removeAt(index)
                                    reordered.add(index - 1, item)
                                    steps = reordered
                                }
                            },
                            enabled = index > 0
                        ) { Text("↑") }
                        IconButton(
                            onClick = {
                                if (index < steps.lastIndex)
                                {
                                    val reordered = steps.toMutableList()
                                    val item = reordered.removeAt(index)
                                    reordered.add(index + 1, item)
                                    steps = reordered
                                }
                            },
                            enabled = index < steps.lastIndex
                        ) { Text("↓") }
                        IconButton(
                            onClick = { steps = steps.filterIndexed { stepIndex, _ -> stepIndex != index } },
                            modifier = Modifier.semantics { contentDescription = "Remove ${step.name}" }
                        ) {
                            Text("×", style = MaterialTheme.typography.titleLarge)
                        }
                        }
                    }
                }
            }
        }
        Button(
            onClick = {
                onEvent(
                    StretchifyEvent.SaveRoutine(
                        routineId = routine?.id,
                        title = title,
                        goal = goal,
                        steps = steps,
                        goalIds = linkedGoalIds
                    )
                )
            },
            enabled = canCreate,
            modifier = Modifier.fillMaxWidth().testTag("save-custom-routine")
        ) {
            FilledButtonText(if (routine == null) "Create and add to Home" else "Save changes")
        }
        if (routine != null)
        {
            val isRequiredByGoal = goals.any { routine.id in it.weeklyTargets && it.weeklyTargets.size == 1 }
            if (isRequiredByGoal && !isBuiltIn)
            {
                Text("Add another routine to its goal before deleting this routine.")
            }
            OutlinedButton(
                onClick = { isConfirmingDestructiveAction = true },
                enabled = !isRequiredByGoal || isBuiltIn,
                modifier = Modifier.fillMaxWidth().testTag(if (isBuiltIn) "restore-routine" else "delete-routine")
            ) { Text(if (isBuiltIn) "Restore default" else "Delete routine") }
        }
    }
    if (isConfirmingDestructiveAction)
    {
        AlertDialog(
            onDismissRequest = { isConfirmingDestructiveAction = false },
            title = { Text(if (isBuiltIn) "Restore default routine?" else "Delete custom routine?") },
            text = { Text(if (isBuiltIn) "All changes to this routine will be replaced."
                else "The routine card will be removed, but its history will remain.") },
            confirmButton = {
                Button(onClick = {
                    isConfirmingDestructiveAction = false
                    onEvent(if (isBuiltIn) StretchifyEvent.RestoreRoutine else StretchifyEvent.DeleteRoutine)
                }) { FilledButtonText(if (isBuiltIn) "Restore" else "Delete") }
            },
            dismissButton = { OutlinedButton(onClick = { isConfirmingDestructiveAction = false }) {
                Text("Cancel")
            } }
        )
    }
}

@Composable
private fun GalleryRow(title: String, isAdded: Boolean, onAdd: () -> Unit)
{
    GlassCard(contentPadding = PaddingValues(14.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(title, modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
            Button(onClick = onAdd, enabled = !isAdded) {
                FilledButtonText(if (isAdded) "Added" else "Add")
            }
        }
    }
}

@Composable
fun GoalDetailsScreen(uiState: StretchifyUiState, onEvent: (StretchifyEvent) -> Unit)
{
    val goal = uiState.selectedGoal ?: return
    val weeks = GoalProgressCalculator.calculate(goal, uiState.completionRecords, uiState.firstDayOfWeek)
    val isStarted = GoalProgressCalculator.isStarted(goal, uiState.completionRecords)
    var isConfirmingReset by remember(goal.id) { mutableStateOf(false) }
    FocusedScreen {
        BackHeader("Goal details") { onEvent(StretchifyEvent.NavigateBack) }
        Text(goal.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(goal.description)
        if (!isStarted)
        {
            Button(onClick = { onEvent(StretchifyEvent.StartGoal(goal.id)) },
                modifier = Modifier.fillMaxWidth().testTag("start-goal")) { FilledButtonText("Start goal") }
        }
        else
        {
            OutlinedButton(onClick = { isConfirmingReset = true }, modifier = Modifier.fillMaxWidth()) {
                Text("Start clean")
            }
        }
        Button(onClick = { onEvent(StretchifyEvent.OpenGoalEditor(goal.id)) },
            modifier = Modifier.fillMaxWidth().testTag("edit-goal")) { FilledButtonText("Edit goal") }
        val currentWeek = weeks.first()
        Text("This week · ${currentWeek.completed}/${currentWeek.target}",
            style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(if (currentWeek.isMet) "All routine targets met" else "Keep practicing")
        goal.weeklyTargets.forEach { (routineId, target) ->
            val routine = uiState.catalog.firstOrNull { it.id == routineId }
            Text("${routine?.title ?: routineId}: ${currentWeek.countsByRoutine[routineId] ?: 0}/$target")
            if (routine != null)
            {
                OutlinedButton(onClick = { onEvent(StretchifyEvent.SelectRoutine(routineId)) }) {
                    Text("Open ${routine.title}")
                }
            }
        }
        Text("Weekly history", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        weeks.forEach { week ->
            GoalHistoryWeek(week, uiState)
        }
    }
    if (isConfirmingReset)
    {
        AlertDialog(onDismissRequest = { isConfirmingReset = false },
            title = { Text("Start this goal clean?") },
            text = { Text("Earlier goal progress will be hidden. Your routine history stays intact.") },
            confirmButton = { Button(onClick = {
                isConfirmingReset = false
                onEvent(StretchifyEvent.StartGoalClean(goal.id))
            }) { FilledButtonText("Start clean") } },
            dismissButton = { OutlinedButton(onClick = { isConfirmingReset = false }) { Text("Cancel") } })
    }
}

@Composable
private fun GoalHistoryWeek(week: GoalWeek, uiState: StretchifyUiState)
{
    GlassCard(modifier = Modifier.testTag("goal-week-${week.startDate}")) {
        Text("${week.startDate} · ${week.completed}/${week.target}", fontWeight = FontWeight.Bold)
        LinearProgressIndicator(progress = { if (week.target == 0) 0f
            else (week.completed / week.target.toFloat()).coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth())
        Text(if (week.isMet) "All targets met" else "In progress")
        week.targetsByRoutine.forEach { (routineId, target) ->
            val title = uiState.catalog.firstOrNull { it.id == routineId }?.title ?: routineId
            Text("$title: ${week.countsByRoutine[routineId] ?: 0}/$target")
        }
        week.records.forEach { record ->
            val title = record.routineTitle ?: uiState.catalog.firstOrNull { it.id == record.routineId }?.title
                ?: "Routine"
            val date = Instant.ofEpochMilli(record.completedAtMillis).atZone(ZoneId.systemDefault())
                .format(DateTimeFormatter.ofPattern("d MMM yyyy · HH:mm"))
            Text("$date · $title")
        }
    }
}

@Composable
fun GoalEditorScreen(uiState: StretchifyUiState, onEvent: (StretchifyEvent) -> Unit)
{
    val existing = uiState.editingGoal
    var title by rememberSaveable(existing?.id) { mutableStateOf(existing?.title ?: "") }
    var description by rememberSaveable(existing?.id) { mutableStateOf(existing?.description ?: "") }
    var targets by remember(existing?.id) { mutableStateOf(existing?.weeklyTargets.orEmpty()) }
    var isConfirmingDelete by remember(existing?.id) { mutableStateOf(false) }
    FocusedScreen {
        BackHeader(if (existing == null) "Create goal" else "Edit goal") {
            onEvent(StretchifyEvent.NavigateBack)
        }
        OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Goal name") },
            modifier = Modifier.fillMaxWidth().testTag("goal-title"))
        OutlinedTextField(value = description, onValueChange = { description = it },
            label = { Text("Description") }, modifier = Modifier.fillMaxWidth())
        Text("Routines and weekly targets", style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold)
        uiState.catalog.forEach { routine ->
            val target = targets[routine.id]
            GlassCard(contentPadding = PaddingValues(12.dp)) {
                FilterChip(selected = target != null,
                    onClick = { targets = if (target == null) targets + (routine.id to 1)
                        else targets - routine.id },
                    label = { Text(routine.title) },
                    modifier = Modifier.testTag("goal-routine-${routine.id}"))
                if (target != null)
                {
                    OutlinedTextField(value = target.toString(), onValueChange = { value ->
                        value.filter(Char::isDigit).toIntOrNull()?.takeIf { it in 1..99 }?.let {
                            targets = targets + (routine.id to it)
                        }
                    }, label = { Text("Sessions per week") }, modifier = Modifier.fillMaxWidth())
                }
            }
        }
        Button(onClick = { onEvent(StretchifyEvent.SaveGoal(existing?.id, title, description, targets)) },
            enabled = title.isNotBlank() && targets.isNotEmpty(),
            modifier = Modifier.fillMaxWidth().testTag("save-goal")) {
            FilledButtonText(if (existing == null) "Create goal" else "Save changes")
        }
        if (existing != null)
        {
            val isBuiltIn = GoalCatalog.defaults.any { it.id == existing.id }
            OutlinedButton(onClick = { isConfirmingDelete = true }, modifier = Modifier.fillMaxWidth()) {
                Text(if (isBuiltIn) "Restore default" else "Delete goal")
            }
        }
    }
    if (isConfirmingDelete && existing != null)
    {
        val isBuiltIn = GoalCatalog.defaults.any { it.id == existing.id }
        AlertDialog(onDismissRequest = { isConfirmingDelete = false },
            title = { Text(if (isBuiltIn) "Restore default goal?" else "Delete goal?") },
            text = { Text(if (isBuiltIn) "Your edits to this goal will be replaced."
                else "The goal and its Home card will be removed. Routine history stays intact.") },
            confirmButton = { Button(onClick = {
                isConfirmingDelete = false
                onEvent(if (isBuiltIn) StretchifyEvent.RestoreGoal else StretchifyEvent.DeleteGoal)
            }) { FilledButtonText(if (isBuiltIn) "Restore" else "Delete") } },
            dismissButton = { OutlinedButton(onClick = { isConfirmingDelete = false }) { Text("Cancel") } })
    }
}

@Composable
private fun BackHeader(title: String, onBack: () -> Unit)
{
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack, modifier = Modifier.semantics { contentDescription = "Back" }) {
            Text("‹", style = MaterialTheme.typography.headlineMedium)
        }
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun FocusedScreen(content: @Composable ColumnScope.() -> Unit)
{
    GlassBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .widthIn(max = 760.dp)
                .padding(20.dp)
                .verticalScroll(rememberScrollState())
                .testTag("screen-content"),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            content = content
        )
    }
}

@Composable
private fun Modifier.pulseOnFirstVisible(): Modifier
{
    val scale = remember { Animatable(1f) }
    var hasPulsed by remember { mutableStateOf(false) }
    LaunchedEffect(hasPulsed)
    {
        if (hasPulsed)
        {
            scale.animateTo(1.06f, animationSpec = tween(durationMillis = 180))
            scale.animateTo(1f, animationSpec = tween(durationMillis = 260))
        }
    }
    return this
        .onGloballyPositioned { coordinates ->
            if (!hasPulsed && coordinates.boundsInWindow().overlaps(
                    coordinates.findRootCoordinates().boundsInWindow()
                ))
            {
                hasPulsed = true
            }
        }
        .graphicsLayer {
            scaleX = scale.value
            scaleY = scale.value
        }
}

@Composable
private fun FilledButtonText(
    text: String,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip
)
{
    Text(text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = maxLines, overflow = overflow)
}

@Composable
private fun PrimaryActionButton(
    text: String,
    description: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
)
{
    Button(
        onClick = onClick,
        modifier = modifier.heightIn(min = 48.dp).semantics { contentDescription = description }
    ) {
        FilledButtonText(text, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun SecondaryActionButton(
    text: String,
    description: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
)
{
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = 48.dp).semantics { contentDescription = description }
    ) {
        Text(text, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

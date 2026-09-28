package com.stretchify.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.stretchify.StretchifyApplication
import com.stretchify.data.DashboardLayout
import com.stretchify.data.DelightEvaluator
import com.stretchify.data.DelightPresentation
import com.stretchify.data.GoalCatalog
import com.stretchify.data.GoalProgressCalculator
import com.stretchify.data.ProgressCalculator
import com.stretchify.data.ProgressSummary
import com.stretchify.data.RoutineCatalog
import com.stretchify.data.SampleRoutineProvider
import com.stretchify.data.StretchifyRepository
import com.stretchify.model.CompletionRecord
import com.stretchify.model.GoalRevision
import com.stretchify.model.GoalRoutine
import com.stretchify.model.AlertMode
import com.stretchify.model.AlertTiming
import com.stretchify.model.DashboardCard
import com.stretchify.model.DashboardCardType
import com.stretchify.model.RoutineStep
import com.stretchify.model.Stretch
import com.stretchify.model.StretchRoutine
import com.stretchify.model.LiquidPreset
import com.stretchify.model.GlassFinish
import com.stretchify.model.ThemePreference
import com.stretchify.reminders.ReminderReceiver
import com.stretchify.session.SessionPhase
import com.stretchify.session.SessionState
import com.stretchify.widget.StretchWidgetProvider
import java.util.UUID
import java.time.DayOfWeek
import kotlin.random.Random
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SessionViewModel(application: Application) : AndroidViewModel(application)
{
    private val repository = StretchifyRepository(application)
    private val sessionController = (application as StretchifyApplication).sessionController
    private var customRoutines = repository.loadCustomRoutines()
    private var routineOverrides = repository.loadRoutineOverrides()
    private var catalog = buildCatalog()
    private var customGoals = repository.loadCustomGoals()
    private var goalOverrides = repository.loadGoalOverrides()
    private var goals = GoalCatalog.build(customGoals, goalOverrides)
    private var selectedRoutine = sessionController.state.value.routine
    private var customRoutineReturnScreen = StretchifyScreen.CardGallery
    private var shouldResumeAfterExitDialog = false

    private val mutableUiState = MutableStateFlow(
        StretchifyUiState(
            screen = screenForSession(sessionController.state.value),
            selectedDestination = TopLevelDestination.Home,
            originDestination = TopLevelDestination.Home,
            catalog = catalog,
            selectedRoutine = selectedRoutine,
            sessionState = sessionController.state.value,
            dashboardCards = repository.loadDashboardCards(catalog.map { it.id }.toSet(), goals.map { it.id }.toSet()),
            goals = goals,
            firstDayOfWeek = repository.loadFirstDayOfWeek(),
            themePreference = repository.loadThemePreference(),
            liquidPreset = repository.loadLiquidPreset(),
            glassFinish = repository.loadGlassFinish(),
            completionRecords = sessionController.completionRecords.value,
            alertMode = repository.loadAlertMode(),
            alertTiming = repository.loadAlertTiming(),
            countdownSeconds = repository.loadCountdownSeconds(),
            isRemindersEnabled = repository.loadRemindersEnabled(),
            reminderHour = repository.loadReminderHour()
        )
    )

    val uiState: StateFlow<StretchifyUiState> = mutableUiState.asStateFlow()

    init
    {
        viewModelScope.launch {
            sessionController.savedCompletion.collect { completion ->
                val delight = completion?.let {
                    DelightEvaluator.evaluate(it.record, it.beforeRecords, it.afterRecords, it.goals,
                        it.firstDayOfWeek, it.awardedMilestoneKeys)
                }
                if (delight != null) repository.saveDelightMilestones(delight.milestoneKeys)
                mutableUiState.update { it.copy(delight = delight,
                    shouldAnimateDelight = delight != null && !repository.hasConsumedDelight(delight.completionId)) }
            }
        }
        viewModelScope.launch {
            sessionController.state.collect { sessionState ->
                selectedRoutine = sessionState.routine
                mutableUiState.update { currentState ->
                    val nextScreen = if (sessionState.phase == SessionPhase.Completed ||
                        sessionState.phase == SessionPhase.Paused || sessionState.phase.isActive)
                    {
                        screenForSession(sessionState)
                    }
                    else if (sessionState.phase == SessionPhase.NotStarted &&
                        (currentState.sessionState.phase == SessionPhase.Paused ||
                            currentState.sessionState.phase.isActive ||
                            currentState.sessionState.phase == SessionPhase.Completed))
                    {
                        StretchifyScreen.TopLevel
                    }
                    else
                    {
                        currentState.screen
                    }
                    currentState.copy(
                        screen = nextScreen,
                        selectedRoutine = sessionState.routine,
                        sessionState = sessionState
                    )
                }
            }
        }
        viewModelScope.launch {
            sessionController.completionRecords.collect { completionRecords ->
                mutableUiState.update { it.copy(completionRecords = completionRecords) }
            }
        }
    }

    fun onEvent(event: StretchifyEvent)
    {
        when (event)
        {
            is StretchifyEvent.PresentDelight ->
            {
                if (mutableUiState.value.delight?.completionId == event.completionId)
                {
                    repository.consumeDelight(event.completionId)
                    mutableUiState.update { it.copy(shouldAnimateDelight = false) }
                }
            }
            StretchifyEvent.CheckWelcomeBack -> mutableUiState.update {
                it.copy(welcomeBackRecordId = DelightEvaluator.findWelcomeBackRecordId(
                    it.completionRecords, repository.loadWelcomeBackAcknowledgment()))
            }
            is StretchifyEvent.PresentWelcomeBack -> repository.acknowledgeWelcomeBack(event.recordId)
            is StretchifyEvent.SelectDestination -> selectDestination(event.destination)
            is StretchifyEvent.SelectRoutine -> selectRoutine(event.routineId)
            StretchifyEvent.RepeatLastRoutine -> repeatLastRoutine()
            is StretchifyEvent.StartRoutineById -> startRoutineById(event.routineId)
            StretchifyEvent.StartSession -> startSession()
            StretchifyEvent.StartSessionImmediately -> sessionController.startImmediately()
            StretchifyEvent.CancelCountdown -> cancelCountdown()
            StretchifyEvent.PauseSession -> pauseSession()
            StretchifyEvent.ResumeSession -> resumeSession()
            StretchifyEvent.SkipCurrentStep -> skipCurrentStep()
            StretchifyEvent.RequestSessionExit -> requestSessionExit()
            StretchifyEvent.CancelSessionExit -> cancelSessionExit()
            StretchifyEvent.ConfirmSessionExit -> confirmSessionExit()
            StretchifyEvent.RestartSession -> restartSession()
            StretchifyEvent.ReturnHome -> returnTo(TopLevelDestination.Home)
            StretchifyEvent.ReturnProgress -> returnTo(TopLevelDestination.Progress)
            StretchifyEvent.NavigateBack -> navigateBack()
            StretchifyEvent.OpenSettings -> openScreen(StretchifyScreen.Settings)
            StretchifyEvent.OpenDashboardEditor -> enterDashboardEditor()
            StretchifyEvent.CloseDashboardEditor -> closeDashboardEditor()
            StretchifyEvent.OpenCardGallery -> openScreen(StretchifyScreen.CardGallery)
            StretchifyEvent.OpenCustomRoutineCreator -> openCustomRoutineCreator()
            is StretchifyEvent.OpenRoutineEditor -> openRoutineEditor(event.routineId)
            is StretchifyEvent.SelectGoal -> selectGoal(event.goalId)
            StretchifyEvent.OpenGoalCreator -> mutableUiState.update {
                it.copy(screen = StretchifyScreen.GoalEditor, editingGoal = null)
            }
            is StretchifyEvent.OpenGoalEditor -> openGoalEditor(event.goalId)
            is StretchifyEvent.SaveGoal -> saveGoal(event)
            StretchifyEvent.DeleteGoal -> deleteGoal()
            StretchifyEvent.RestoreGoal -> restoreGoal()
            is StretchifyEvent.StartGoal -> startGoal(event.goalId)
            is StretchifyEvent.StartGoalClean -> startGoalClean(event.goalId)
            is StretchifyEvent.SaveRoutine -> saveRoutine(event)
            StretchifyEvent.RestoreRoutine -> restoreRoutine()
            StretchifyEvent.DeleteRoutine -> deleteRoutine()
            is StretchifyEvent.OpenHistoryEditor -> openHistoryEditor(event.recordId)
            is StretchifyEvent.SaveHistoryRecord -> saveHistoryRecord(event)
            is StretchifyEvent.DeleteHistoryRecord -> sessionController.deleteCompletionRecord(event.recordId)
            StretchifyEvent.ResetDashboard -> updateCards(DashboardLayout.defaultCards())
            is StretchifyEvent.RemoveCard -> removeCard(event.cardId)
            StretchifyEvent.UndoRemoveCard -> undoRemoveCard()
            is StretchifyEvent.MoveCard -> moveCard(event.cardId, event.offset)
            is StretchifyEvent.ResizeCard -> resizeCard(event.cardId, event.widthDelta, event.heightDelta)
            is StretchifyEvent.AddRoutineCard -> addRoutineCard(event.routineId)
            is StretchifyEvent.AddGoalCard -> addGoalCard(event.goalId)
            is StretchifyEvent.AddSummaryCard -> addSummaryCard(event.type)
            is StretchifyEvent.UpdateSearch -> mutableUiState.update { it.copy(searchQuery = event.query) }
            is StretchifyEvent.SelectCategory -> mutableUiState.update { it.copy(selectedCategory = event.category) }
            is StretchifyEvent.SelectTheme -> selectTheme(event.themePreference)
            is StretchifyEvent.SelectLiquidPreset ->
            {
                repository.saveLiquidPreset(event.preset)
                mutableUiState.update { it.copy(liquidPreset = event.preset) }
            }
            is StretchifyEvent.SelectGlassFinish ->
            {
                repository.saveGlassFinish(event.finish)
                mutableUiState.update { it.copy(glassFinish = event.finish) }
            }
            is StretchifyEvent.SelectAlertMode -> selectAlertMode(event.alertMode)
            is StretchifyEvent.SelectAlertTiming -> selectAlertTiming(event.alertTiming)
            is StretchifyEvent.SelectCountdown -> selectCountdown(event.seconds)
            is StretchifyEvent.SetRemindersEnabled -> setRemindersEnabled(event.isEnabled)
            is StretchifyEvent.SelectReminderHour -> selectReminderHour(event.hour)
            is StretchifyEvent.SelectFirstDayOfWeek -> {
                repository.saveFirstDayOfWeek(event.day)
                mutableUiState.update { it.copy(firstDayOfWeek = event.day) }
            }
            is StretchifyEvent.SelectLibrarySection -> mutableUiState.update {
                it.copy(isGoalLibrarySelected = event.isGoals)
            }
        }
    }

    private fun selectDestination(destination: TopLevelDestination)
    {
        mutableUiState.update {
            it.copy(
                screen = StretchifyScreen.TopLevel,
                selectedDestination = destination,
                originDestination = destination,
                isDashboardEditing = false
            )
        }
    }

    private fun enterDashboardEditor()
    {
        mutableUiState.update {
            it.copy(
                screen = StretchifyScreen.TopLevel,
                selectedDestination = TopLevelDestination.Home,
                originDestination = TopLevelDestination.Home,
                isDashboardEditing = true
            )
        }
    }

    private fun closeDashboardEditor()
    {
        mutableUiState.update { it.copy(isDashboardEditing = false) }
    }

    private fun selectRoutine(routineId: String)
    {
        val routine = catalog.firstOrNull { it.id == routineId } ?: return
        selectedRoutine = routine
        sessionController.prepare(routine)
        mutableUiState.update { currentState ->
            currentState.copy(
                screen = StretchifyScreen.RoutineDetails,
                selectedRoutine = routine,
                sessionState = sessionController.state.value,
                originDestination = currentState.selectedDestination
            )
        }
    }

    private fun repeatLastRoutine()
    {
        val routine = mutableUiState.value.lastCompletedRoutine ?: return
        startRoutineById(routine.id)
    }

    private fun startRoutineById(routineId: String)
    {
        if (catalog.none { it.id == routineId } || sessionController.state.value.phase.isActive ||
            sessionController.state.value.phase == SessionPhase.Paused)
        {
            return
        }
        selectRoutine(routineId)
        startSession()
    }

    private fun startSession()
    {
        sessionController.start()
        mutableUiState.update { currentState ->
            currentState.copy(
                screen = screenForSession(sessionController.state.value),
                sessionState = sessionController.state.value
            )
        }
    }

    private fun cancelCountdown()
    {
        sessionController.stop()
        sessionController.prepare(selectedRoutine)
        mutableUiState.update {
            it.copy(screen = StretchifyScreen.RoutineDetails, sessionState = sessionController.state.value)
        }
    }

    private fun pauseSession()
    {
        sessionController.pause()
    }

    private fun resumeSession()
    {
        sessionController.resume()
    }

    private fun skipCurrentStep()
    {
        sessionController.skip()
    }

    private fun requestSessionExit()
    {
        if (mutableUiState.value.screen != StretchifyScreen.ActiveSession ||
            mutableUiState.value.isExitConfirmationVisible)
        {
            return
        }
        shouldResumeAfterExitDialog = sessionController.state.value.phase.isActive
        if (shouldResumeAfterExitDialog)
        {
            sessionController.pause()
        }
        mutableUiState.update { it.copy(isExitConfirmationVisible = true) }
    }

    private fun cancelSessionExit()
    {
        if (!mutableUiState.value.isExitConfirmationVisible)
        {
            return
        }
        mutableUiState.update { it.copy(isExitConfirmationVisible = false) }
        if (shouldResumeAfterExitDialog)
        {
            sessionController.resume()
        }
        shouldResumeAfterExitDialog = false
    }

    private fun confirmSessionExit()
    {
        if (!mutableUiState.value.isExitConfirmationVisible)
        {
            return
        }
        shouldResumeAfterExitDialog = false
        returnTo(mutableUiState.value.originDestination)
    }

    private fun restartSession()
    {
        sessionController.restart()
    }

    private fun returnTo(destination: TopLevelDestination)
    {
        sessionController.stop()
        mutableUiState.update {
            it.copy(
                screen = StretchifyScreen.TopLevel,
                selectedDestination = destination,
                originDestination = destination,
                isDashboardEditing = false,
                isExitConfirmationVisible = false,
                sessionState = sessionController.state.value
            )
        }
    }

    private fun navigateBack()
    {
        when (mutableUiState.value.screen)
        {
            StretchifyScreen.CardGallery -> enterDashboardEditor()
            StretchifyScreen.CustomRoutineCreator -> openScreen(customRoutineReturnScreen)
            StretchifyScreen.RoutineEditor -> openScreen(StretchifyScreen.RoutineDetails)
            StretchifyScreen.HistoryEditor -> returnTo(TopLevelDestination.Progress)
            StretchifyScreen.GoalEditor -> if (mutableUiState.value.editingGoal != null)
                openScreen(StretchifyScreen.GoalDetails) else returnTo(TopLevelDestination.Library)
            StretchifyScreen.Settings -> returnTo(mutableUiState.value.originDestination)
            StretchifyScreen.TopLevel -> closeDashboardEditor()
            StretchifyScreen.ActiveSession -> requestSessionExit()
            else -> returnTo(mutableUiState.value.originDestination)
        }
    }

    private fun openScreen(screen: StretchifyScreen)
    {
        mutableUiState.update { it.copy(screen = screen) }
    }

    private fun openCustomRoutineCreator()
    {
        customRoutineReturnScreen = mutableUiState.value.screen
        mutableUiState.update { it.copy(screen = StretchifyScreen.RoutineEditor, editingRoutine = null) }
    }

    private fun openRoutineEditor(routineId: String)
    {
        val routine = catalog.firstOrNull { it.id == routineId } ?: return
        mutableUiState.update { it.copy(screen = StretchifyScreen.RoutineEditor, editingRoutine = routine) }
    }

    private fun updateCards(cards: List<DashboardCard>)
    {
        val normalizedCards = DashboardLayout.normalize(
            cards, catalog.map { it.id }.toSet(), goals.map { it.id }.toSet()
        )
        repository.saveDashboardCards(normalizedCards)
        mutableUiState.update { it.copy(dashboardCards = normalizedCards, removedCard = null) }
    }

    private fun removeCard(cardId: String)
    {
        val removedCard = mutableUiState.value.dashboardCards.firstOrNull { it.id == cardId } ?: return
        val cards = DashboardLayout.normalize(
            mutableUiState.value.dashboardCards.filterNot { it.id == cardId },
            catalog.map { it.id }.toSet(), goals.map { it.id }.toSet()
        )
        repository.saveDashboardCards(cards)
        mutableUiState.update { it.copy(dashboardCards = cards, removedCard = removedCard) }
    }

    private fun undoRemoveCard()
    {
        val removedCard = mutableUiState.value.removedCard ?: return
        updateCards(mutableUiState.value.dashboardCards + removedCard)
    }

    private fun moveCard(cardId: String, offset: Int)
    {
        val cards = mutableUiState.value.dashboardCards.sortedBy { it.position }.toMutableList()
        val currentIndex = cards.indexOfFirst { it.id == cardId }
        if (currentIndex < 0)
        {
            return
        }
        val targetIndex = (currentIndex + offset).coerceIn(0, cards.lastIndex)
        val card = cards.removeAt(currentIndex)
        cards.add(targetIndex, card)
        updateCards(cards.mapIndexed { index, dashboardCard -> dashboardCard.copy(position = index) })
    }

    private fun resizeCard(cardId: String, widthDelta: Int, heightDelta: Int)
    {
        updateCards(mutableUiState.value.dashboardCards.map { card ->
            if (card.id == cardId)
            {
                card.copy(
                    widthSpan = (card.widthSpan + widthDelta).coerceIn(1, 2),
                    heightSpan = (card.heightSpan + heightDelta).coerceIn(1, 3)
                )
            }
            else
            {
                card
            }
        })
    }

    private fun addRoutineCard(routineId: String)
    {
        if (mutableUiState.value.dashboardCards.any { it.routineId == routineId })
        {
            return
        }
        updateCards(mutableUiState.value.dashboardCards + DashboardCard(
            id = "routine-$routineId",
            type = DashboardCardType.Routine,
            routineId = routineId,
            position = mutableUiState.value.dashboardCards.size,
            widthSpan = 1,
            heightSpan = 2,
            colorSeed = Random.nextInt()
        ))
    }

    private fun addSummaryCard(type: DashboardCardType)
    {
        if (mutableUiState.value.dashboardCards.any { it.type == type })
        {
            return
        }
        updateCards(mutableUiState.value.dashboardCards + DashboardCard(
            id = type.name.lowercase(),
            type = type,
            position = mutableUiState.value.dashboardCards.size,
            widthSpan = 2,
            heightSpan = 1,
            colorSeed = Random.nextInt()
        ))
    }

    private fun addGoalCard(goalId: String)
    {
        if (goals.none { it.id == goalId } || mutableUiState.value.dashboardCards.any { it.goalId == goalId }) return
        updateCards(mutableUiState.value.dashboardCards + DashboardCard(
            id = "goal-$goalId", type = DashboardCardType.Goal, position = mutableUiState.value.dashboardCards.size,
            widthSpan = 1, heightSpan = 2, colorSeed = Random.nextInt(), goalId = goalId
        ))
    }

    private fun selectGoal(goalId: String)
    {
        if (goals.none { it.id == goalId }) return
        mutableUiState.update { it.copy(screen = StretchifyScreen.GoalDetails, selectedGoalId = goalId,
            originDestination = it.selectedDestination) }
    }

    private fun openGoalEditor(goalId: String)
    {
        val goal = goals.firstOrNull { it.id == goalId } ?: return
        mutableUiState.update { it.copy(screen = StretchifyScreen.GoalEditor, selectedGoalId = goalId,
            editingGoal = goal) }
    }

    private fun persistGoal(goal: GoalRoutine)
    {
        if (GoalCatalog.defaults.any { it.id == goal.id })
        {
            goalOverrides = goalOverrides.filterNot { it.id == goal.id } + goal
            repository.saveGoalOverrides(goalOverrides)
        }
        else
        {
            customGoals = customGoals.filterNot { it.id == goal.id } + goal
            repository.saveCustomGoals(customGoals)
        }
        goals = GoalCatalog.build(customGoals, goalOverrides)
        mutableUiState.update { it.copy(goals = goals) }
    }

    private fun saveGoal(event: StretchifyEvent.SaveGoal)
    {
        if (event.title.isBlank() || event.weeklyTargets.isEmpty() ||
            event.weeklyTargets.any { (id, target) -> catalog.none { it.id == id } || target < 1 }) return
        val existing = event.goalId?.let { id -> goals.firstOrNull { it.id == id } }
        val targets = event.weeklyTargets
        val revisions = if (existing == null) listOf(GoalRevision(Long.MIN_VALUE, targets))
            else if (existing.weeklyTargets == targets) existing.revisions
            else existing.revisions + GoalRevision(maxOf(System.currentTimeMillis(),
                existing.revisions.maxOf { it.effectiveAtMillis } + 1), targets)
        val goal = GoalRoutine(existing?.id ?: "custom-${UUID.randomUUID()}", event.title.trim(),
            event.description.trim(), revisions,
            existing?.let { GoalProgressCalculator.isStarted(it, mutableUiState.value.completionRecords) } ?: false,
            existing?.baselineAtMillis, existing?.startedAtMillis)
        persistGoal(goal)
        mutableUiState.update { it.copy(screen = StretchifyScreen.GoalDetails, selectedGoalId = goal.id,
            editingGoal = null) }
    }

    private fun startGoal(goalId: String)
    {
        val goal = goals.firstOrNull { it.id == goalId } ?: return
        persistGoal(goal.copy(isStarted = true, startedAtMillis = goal.startedAtMillis ?: System.currentTimeMillis()))
    }

    private fun startGoalClean(goalId: String)
    {
        val goal = goals.firstOrNull { it.id == goalId } ?: return
        val now = System.currentTimeMillis()
        persistGoal(goal.copy(isStarted = true, baselineAtMillis = now, startedAtMillis = now))
    }

    private fun restoreGoal()
    {
        val id = mutableUiState.value.editingGoal?.id ?: return
        val default = GoalCatalog.defaults.firstOrNull { it.id == id } ?: return
        val current = goals.first { it.id == id }
        val revisions = if (current.weeklyTargets == default.weeklyTargets) current.revisions
            else current.revisions + GoalRevision(maxOf(System.currentTimeMillis(),
                current.revisions.maxOf { it.effectiveAtMillis } + 1), default.weeklyTargets)
        persistGoal(default.copy(revisions = revisions,
            isStarted = GoalProgressCalculator.isStarted(current, mutableUiState.value.completionRecords),
            baselineAtMillis = current.baselineAtMillis, startedAtMillis = current.startedAtMillis))
        mutableUiState.update { it.copy(goals = goals, screen = StretchifyScreen.GoalDetails,
            selectedGoalId = default.id, editingGoal = null) }
    }

    private fun deleteGoal()
    {
        val goal = mutableUiState.value.editingGoal ?: return
        if (GoalCatalog.defaults.any { it.id == goal.id }) return
        customGoals = customGoals.filterNot { it.id == goal.id }
        repository.saveCustomGoals(customGoals)
        goals = GoalCatalog.build(customGoals, goalOverrides)
        updateCards(mutableUiState.value.dashboardCards.filterNot { it.goalId == goal.id })
        mutableUiState.update { it.copy(goals = goals, screen = StretchifyScreen.TopLevel,
            selectedDestination = TopLevelDestination.Library, isGoalLibrarySelected = true, editingGoal = null) }
    }

    private fun saveRoutine(event: StretchifyEvent.SaveRoutine)
    {
        val existingRoutine = event.routineId?.let { id -> catalog.firstOrNull { it.id == id } }
        val routineId = existingRoutine?.id ?: "custom-${UUID.randomUUID()}"
        val routine = StretchRoutine(
            id = routineId,
            title = event.title.trim(),
            goal = event.goal.trim(),
            category = existingRoutine?.category ?: "Custom",
            difficulty = existingRoutine?.difficulty ?: "Beginner",
            targetAreas = existingRoutine?.targetAreas ?: listOf("Custom"),
            isFeatured = existingRoutine?.isFeatured ?: false,
            steps = event.steps.mapIndexed { index, step ->
                val existingStep = existingRoutine?.steps?.firstOrNull { it.stretch.name == step.name } ?:
                    existingRoutine?.steps?.getOrNull(index)
                RoutineStep(
                    stretch = Stretch(
                        id = existingStep?.stretch?.id ?: "$routineId-stretch-$index",
                        name = step.name.trim(),
                        description = existingStep?.stretch?.description ?: event.goal.trim(),
                        trainerCue = existingStep?.stretch?.trainerCue ?:
                            "Move gently and stay within a comfortable range.",
                        easierDescription = existingStep?.stretch?.easierDescription
                    ),
                    durationSeconds = step.durationSeconds,
                    restSeconds = step.restSeconds
                )
            }
        )
        if (SampleRoutineProvider.routines.any { it.id == routineId })
        {
            routineOverrides = routineOverrides.filterNot { it.id == routineId } + routine
            repository.saveRoutineOverrides(routineOverrides)
        }
        else
        {
            customRoutines = customRoutines.filterNot { it.id == routineId } + routine
            repository.saveCustomRoutines(customRoutines)
        }
        rebuildCatalog()
        goals.toList().forEach { goal ->
            val isLinked = routineId in goal.weeklyTargets
            val shouldLink = goal.id in event.goalIds
            if (isLinked != shouldLink && (shouldLink || goal.weeklyTargets.size > 1))
            {
                val targets = if (shouldLink) goal.weeklyTargets + (routineId to 1)
                    else goal.weeklyTargets - routineId
                persistGoal(goal.copy(revisions = goal.revisions + GoalRevision(maxOf(System.currentTimeMillis(),
                    goal.revisions.maxOf { it.effectiveAtMillis } + 1), targets),
                    isStarted = GoalProgressCalculator.isStarted(goal, mutableUiState.value.completionRecords)))
            }
        }
        selectedRoutine = routine
        sessionController.prepare(routine)
        if (existingRoutine == null)
        {
            addRoutineCard(routine.id)
        }
        mutableUiState.update {
            it.copy(
                screen = StretchifyScreen.RoutineDetails,
                selectedRoutine = routine,
                sessionState = sessionController.state.value,
                editingRoutine = null
            )
        }
    }

    private fun restoreRoutine()
    {
        val routineId = mutableUiState.value.editingRoutine?.id ?: return
        val defaultRoutine = SampleRoutineProvider.routines.firstOrNull { it.id == routineId } ?: return
        routineOverrides = routineOverrides.filterNot { it.id == routineId }
        repository.saveRoutineOverrides(routineOverrides)
        rebuildCatalog()
        selectedRoutine = defaultRoutine
        sessionController.prepare(defaultRoutine)
        mutableUiState.update {
            it.copy(screen = StretchifyScreen.RoutineDetails, selectedRoutine = defaultRoutine,
                sessionState = sessionController.state.value, editingRoutine = null)
        }
    }

    private fun deleteRoutine()
    {
        val routine = mutableUiState.value.editingRoutine ?: return
        if (SampleRoutineProvider.routines.any { it.id == routine.id })
        {
            return
        }
        if (goals.any { routine.id in it.weeklyTargets && it.weeklyTargets.size == 1 }) return
        customRoutines = customRoutines.filterNot { it.id == routine.id }
        repository.saveCustomRoutines(customRoutines)
        rebuildCatalog()
        goals.toList().filter { routine.id in it.weeklyTargets }.forEach { goal ->
            if (goal.weeklyTargets.size > 1)
            {
                persistGoal(goal.copy(revisions = goal.revisions + GoalRevision(maxOf(System.currentTimeMillis(),
                    goal.revisions.maxOf { it.effectiveAtMillis } + 1), goal.weeklyTargets - routine.id),
                    isStarted = GoalProgressCalculator.isStarted(goal, mutableUiState.value.completionRecords)))
            }
        }
        updateCards(mutableUiState.value.dashboardCards.filterNot { it.routineId == routine.id })
        mutableUiState.update {
            it.copy(screen = StretchifyScreen.TopLevel, selectedDestination = TopLevelDestination.Library,
                originDestination = TopLevelDestination.Library, editingRoutine = null)
        }
    }

    private fun openHistoryEditor(recordId: String?)
    {
        val record = recordId?.let { id -> mutableUiState.value.completionRecords.firstOrNull { it.id == id } }
        mutableUiState.update { it.copy(screen = StretchifyScreen.HistoryEditor, editingHistoryRecord = record) }
    }

    private fun saveHistoryRecord(event: StretchifyEvent.SaveHistoryRecord)
    {
        val routine = catalog.firstOrNull { it.id == event.routineId }
        sessionController.saveCompletionRecord(
            CompletionRecord(
                id = event.recordId ?: UUID.randomUUID().toString(),
                routineId = event.routineId,
                completedAtMillis = event.completedAtMillis,
                elapsedSeconds = event.elapsedSeconds,
                completedStepCount = event.completedStepCount,
                routineTitle = routine?.title ?: event.routineTitle
            )
        )
        mutableUiState.update {
            it.copy(screen = StretchifyScreen.TopLevel, selectedDestination = TopLevelDestination.Progress,
                originDestination = TopLevelDestination.Progress, editingHistoryRecord = null)
        }
    }

    private fun rebuildCatalog()
    {
        catalog = buildCatalog()
        mutableUiState.update { it.copy(catalog = catalog) }
        StretchWidgetProvider.updateAll(getApplication<Application>())
    }

    private fun buildCatalog(): List<StretchRoutine>
    {
        return RoutineCatalog.build(customRoutines, routineOverrides)
    }

    private fun selectTheme(themePreference: ThemePreference)
    {
        repository.saveThemePreference(themePreference)
        mutableUiState.update { it.copy(themePreference = themePreference) }
    }

    private fun selectAlertMode(alertMode: AlertMode)
    {
        repository.saveAlertMode(alertMode)
        mutableUiState.update { it.copy(alertMode = alertMode) }
        if (!alertMode.isSoundEnabled)
        {
            sessionController.releaseAlerts()
        }
    }

    private fun selectAlertTiming(alertTiming: AlertTiming)
    {
        repository.saveAlertTiming(alertTiming)
        mutableUiState.update { it.copy(alertTiming = alertTiming) }
    }

    private fun selectCountdown(seconds: Int)
    {
        repository.saveCountdownSeconds(seconds)
        mutableUiState.update { it.copy(countdownSeconds = seconds) }
    }

    private fun setRemindersEnabled(isEnabled: Boolean)
    {
        repository.saveRemindersEnabled(isEnabled)
        ReminderReceiver.schedule(getApplication<Application>(), repository)
        mutableUiState.update { it.copy(isRemindersEnabled = isEnabled) }
    }

    private fun selectReminderHour(hour: Int)
    {
        repository.saveReminderHour(hour)
        ReminderReceiver.schedule(getApplication<Application>(), repository)
        mutableUiState.update { it.copy(reminderHour = hour) }
    }

    private fun screenForSession(sessionState: SessionState): StretchifyScreen
    {
        return when
        {
            sessionState.phase == SessionPhase.Completed -> StretchifyScreen.Complete
            sessionState.phase == SessionPhase.Countdown -> StretchifyScreen.Countdown
            sessionState.phase == SessionPhase.Paused || sessionState.phase.isActive ->
                StretchifyScreen.ActiveSession
            else -> StretchifyScreen.TopLevel
        }
    }
}

data class StretchifyUiState(
    val screen: StretchifyScreen,
    val selectedDestination: TopLevelDestination,
    val originDestination: TopLevelDestination,
    val catalog: List<StretchRoutine>,
    val selectedRoutine: StretchRoutine,
    val sessionState: SessionState,
    val dashboardCards: List<DashboardCard>,
    val goals: List<GoalRoutine>,
    val firstDayOfWeek: DayOfWeek,
    val themePreference: ThemePreference,
    val completionRecords: List<CompletionRecord>,
    val alertMode: AlertMode,
    val alertTiming: AlertTiming,
    val countdownSeconds: Int,
    val isRemindersEnabled: Boolean,
    val reminderHour: Int,
    val liquidPreset: LiquidPreset = LiquidPreset.Aurora,
    val glassFinish: GlassFinish = GlassFinish.Clear,
    val searchQuery: String = "",
    val selectedCategory: String = "All",
    val removedCard: DashboardCard? = null,
    val isDashboardEditing: Boolean = false,
    val isExitConfirmationVisible: Boolean = false,
    val editingRoutine: StretchRoutine? = null,
    val editingHistoryRecord: CompletionRecord? = null,
    val selectedGoalId: String? = null,
    val editingGoal: GoalRoutine? = null,
    val isGoalLibrarySelected: Boolean = false,
    val delight: DelightPresentation? = null,
    val shouldAnimateDelight: Boolean = false,
    val welcomeBackRecordId: String? = null
)
{
    val filteredRoutines: List<StretchRoutine>
        get() = RoutineCatalog.filter(catalog, searchQuery, selectedCategory)

    val progressSummary: ProgressSummary
        get() = ProgressCalculator.calculate(completionRecords, firstDayOfWeek = firstDayOfWeek)

    val startedGoals: List<GoalRoutine>
        get() = goals.filter { GoalProgressCalculator.isStarted(it, completionRecords) }

    val selectedGoal: GoalRoutine?
        get() = goals.firstOrNull { it.id == selectedGoalId }

    val lastCompletedRoutine: StretchRoutine?
        get() = completionRecords.sortedByDescending { it.completedAtMillis }
            .firstNotNullOfOrNull { record -> catalog.firstOrNull { it.id == record.routineId } }
}

enum class TopLevelDestination
{
    Home,
    Library,
    Progress
}

enum class StretchifyScreen
{
    TopLevel,
    RoutineDetails,
    ActiveSession,
    Complete,
    Countdown,
    Settings,
    CardGallery,
    CustomRoutineCreator,
    RoutineEditor,
    HistoryEditor,
    GoalDetails,
    GoalEditor
}

sealed interface StretchifyEvent
{
    data class PresentDelight(val completionId: String) : StretchifyEvent
    data object CheckWelcomeBack : StretchifyEvent
    data class PresentWelcomeBack(val recordId: String) : StretchifyEvent
    data class SelectDestination(val destination: TopLevelDestination) : StretchifyEvent
    data class SelectRoutine(val routineId: String) : StretchifyEvent
    data object RepeatLastRoutine : StretchifyEvent
    data class StartRoutineById(val routineId: String) : StretchifyEvent
    data object StartSession : StretchifyEvent
    data object StartSessionImmediately : StretchifyEvent
    data object CancelCountdown : StretchifyEvent
    data object PauseSession : StretchifyEvent
    data object ResumeSession : StretchifyEvent
    data object SkipCurrentStep : StretchifyEvent
    data object RequestSessionExit : StretchifyEvent
    data object CancelSessionExit : StretchifyEvent
    data object ConfirmSessionExit : StretchifyEvent
    data object RestartSession : StretchifyEvent
    data object ReturnHome : StretchifyEvent
    data object ReturnProgress : StretchifyEvent
    data object NavigateBack : StretchifyEvent
    data object OpenSettings : StretchifyEvent
    data object OpenDashboardEditor : StretchifyEvent
    data object CloseDashboardEditor : StretchifyEvent
    data object OpenCardGallery : StretchifyEvent
    data object OpenCustomRoutineCreator : StretchifyEvent
    data class OpenRoutineEditor(val routineId: String) : StretchifyEvent
    data class SaveRoutine(
        val routineId: String?,
        val title: String,
        val goal: String,
        val steps: List<CustomRoutineStep>,
        val goalIds: Set<String> = emptySet()
    ) : StretchifyEvent
    data object RestoreRoutine : StretchifyEvent
    data object DeleteRoutine : StretchifyEvent
    data class SelectGoal(val goalId: String) : StretchifyEvent
    data object OpenGoalCreator : StretchifyEvent
    data class OpenGoalEditor(val goalId: String) : StretchifyEvent
    data class SaveGoal(val goalId: String?, val title: String, val description: String,
        val weeklyTargets: Map<String, Int>) : StretchifyEvent
    data object DeleteGoal : StretchifyEvent
    data object RestoreGoal : StretchifyEvent
    data class StartGoal(val goalId: String) : StretchifyEvent
    data class StartGoalClean(val goalId: String) : StretchifyEvent
    data class OpenHistoryEditor(val recordId: String? = null) : StretchifyEvent
    data class SaveHistoryRecord(
        val recordId: String?,
        val routineId: String,
        val routineTitle: String?,
        val completedAtMillis: Long,
        val elapsedSeconds: Int,
        val completedStepCount: Int
    ) : StretchifyEvent
    data class DeleteHistoryRecord(val recordId: String) : StretchifyEvent
    data object ResetDashboard : StretchifyEvent
    data class RemoveCard(val cardId: String) : StretchifyEvent
    data object UndoRemoveCard : StretchifyEvent
    data class MoveCard(val cardId: String, val offset: Int) : StretchifyEvent
    data class ResizeCard(val cardId: String, val widthDelta: Int, val heightDelta: Int) : StretchifyEvent
    data class AddRoutineCard(val routineId: String) : StretchifyEvent
    data class AddGoalCard(val goalId: String) : StretchifyEvent
    data class AddSummaryCard(val type: DashboardCardType) : StretchifyEvent
    data class UpdateSearch(val query: String) : StretchifyEvent
    data class SelectCategory(val category: String) : StretchifyEvent
    data class SelectTheme(val themePreference: ThemePreference) : StretchifyEvent
    data class SelectLiquidPreset(val preset: LiquidPreset) : StretchifyEvent
    data class SelectGlassFinish(val finish: GlassFinish) : StretchifyEvent
    data class SelectAlertMode(val alertMode: AlertMode) : StretchifyEvent
    data class SelectAlertTiming(val alertTiming: AlertTiming) : StretchifyEvent
    data class SelectCountdown(val seconds: Int) : StretchifyEvent
    data class SetRemindersEnabled(val isEnabled: Boolean) : StretchifyEvent
    data class SelectReminderHour(val hour: Int) : StretchifyEvent
    data class SelectFirstDayOfWeek(val day: DayOfWeek) : StretchifyEvent
    data class SelectLibrarySection(val isGoals: Boolean) : StretchifyEvent
}

data class CustomRoutineStep(
    val name: String,
    val durationSeconds: Int,
    val restSeconds: Int
)

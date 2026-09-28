package com.stretchify.data

import com.stretchify.model.RoutineStep
import com.stretchify.model.Stretch
import com.stretchify.model.StretchRoutine

object SampleRoutineProvider
{
    val roundedShouldersRoutine = StretchRoutine(
        id = "rounded-shoulders",
        title = "Fix Rounded Shoulders",
        goal = "Open the chest, wake up the upper back, and reset desk posture.",
        category = "Posture",
        difficulty = "Beginner",
        targetAreas = listOf("Chest", "Upper back", "Shoulders"),
        isFeatured = true,
        steps = listOf(
            RoutineStep(
                stretch = Stretch(
                    id = "doorway-chest-opener",
                    name = "Doorway Chest Opener",
                    description = "Place your forearm on a doorway and gently rotate away until your chest opens.",
                    trainerCue = "Keep your ribs quiet and let the shoulder relax down.",
                    easierDescription = "Stand in the doorway with your forearm lower and take a smaller step forward."
                ),
                durationSeconds = 30,
                restSeconds = 10
            ),
            RoutineStep(
                stretch = Stretch(
                    id = "wall-angels",
                    name = "Wall Angels",
                    description = "Stand against a wall and slowly slide your arms up and down with control.",
                    trainerCue = "Move slowly. Quality beats range here.",
                    easierDescription = "Keep your elbows below shoulder height and slide only as far as feels easy."
                ),
                durationSeconds = 35,
                restSeconds = 10
            ),
            RoutineStep(
                stretch = Stretch(
                    id = "thread-the-needle",
                    name = "Thread the Needle",
                    description = "From all fours, slide one arm under your body and breathe into the upper back.",
                    trainerCue = "Let your upper spine rotate instead of forcing the shoulder.",
                    easierDescription = "Sit upright and gently turn your chest side to side with your arms relaxed."
                ),
                durationSeconds = 40,
                restSeconds = 0
            )
        )
    )

    private fun createRoutine(
        id: String,
        title: String,
        goal: String,
        category: String,
        targetAreas: List<String>,
        stretchName: String,
        stretchDescription: String,
        trainerCue: String,
        easierDescription: String,
        durationSeconds: Int,
        difficulty: String = "Beginner",
        isFeatured: Boolean = false
    ): StretchRoutine
    {
        return StretchRoutine(
            id = id,
            title = title,
            goal = goal,
            category = category,
            difficulty = difficulty,
            targetAreas = targetAreas,
            isFeatured = isFeatured,
            steps = listOf(
                RoutineStep(
                    stretch = Stretch(
                        id = "$id-stretch",
                        name = stretchName,
                        description = stretchDescription,
                    trainerCue = trainerCue,
                    easierDescription = easierDescription
                    ),
                    durationSeconds = durationSeconds,
                    restSeconds = 0
                )
            )
        )
    }

    val lowerBackReset = createRoutine(
        id = "lower-back-reset",
        title = "Lower Back Reset",
        goal = "Release stiffness after long periods of sitting.",
        category = "Back",
        targetAreas = listOf("Lower back", "Spine"),
        stretchName = "Knee to Chest",
        stretchDescription = "Draw one knee toward your chest while the other leg stays long.",
        trainerCue = "Keep your shoulders soft and breathe into your lower back.",
        easierDescription = "Lie on your back with both knees bent and draw one knee in only a little.",
        durationSeconds = 45,
        isFeatured = true
    )

    val hipMobility = createRoutine(
        id = "hip-mobility",
        title = "Hip Mobility Flow",
        goal = "Restore comfortable movement through tight hips.",
        category = "Hips",
        targetAreas = listOf("Hips", "Glutes"),
        stretchName = "90/90 Hip Switch",
        stretchDescription = "Sit tall and rotate both knees side to side with control.",
        trainerCue = "Move from the hips and keep the motion smooth.",
        easierDescription = "Sit on a cushion and let your knees move through a smaller, comfortable arc.",
        durationSeconds = 60,
        difficulty = "Intermediate"
    )

    val neckRelief = createRoutine(
        id = "neck-relief",
        title = "Neck & Shoulder Relief",
        goal = "Ease screen-time tension in the neck and shoulders.",
        category = "Neck",
        targetAreas = listOf("Neck", "Shoulders"),
        stretchName = "Upper Trapezius Release",
        stretchDescription = "Tilt one ear toward the shoulder while keeping both shoulders low.",
        trainerCue = "Use only the weight of your head; never pull.",
        easierDescription = "Keep your head nearly upright and make a small, gentle tilt without using your hands.",
        durationSeconds = 40
    )

    val recoveryFlow = createRoutine(
        id = "recovery-flow",
        title = "Full Body Recovery",
        goal = "Unwind gently after training or a demanding day.",
        category = "Recovery",
        targetAreas = listOf("Full body", "Breathing"),
        stretchName = "Child's Pose Reach",
        stretchDescription = "Sit toward your heels and reach both hands forward.",
        trainerCue = "Let every exhale soften your back and shoulders.",
        easierDescription = "Stay seated and reach your hands forward on a table while keeping your hips comfortable.",
        durationSeconds = 75,
        isFeatured = true
    )

    val quickReset = createRoutine(
        id = "quick-reset",
        title = "Two-Minute Reset",
        goal = "A fast mobility break that fits between tasks.",
        category = "Quick",
        targetAreas = listOf("Spine", "Shoulders", "Hips"),
        stretchName = "Standing Reach",
        stretchDescription = "Reach overhead, lengthen both sides, then fold forward softly.",
        trainerCue = "Breathe slowly and avoid forcing the fold.",
        easierDescription = "Stay standing, reach only as high as comfortable, and skip the forward fold.",
        durationSeconds = 120
    )

    val morningPosture = createRoutine(
        id = "morning-posture",
        title = "Morning Posture Wake-Up",
        goal = "Start upright with gentle chest and spine mobility.",
        category = "Posture",
        targetAreas = listOf("Chest", "Spine"),
        stretchName = "Standing Chest Sweep",
        stretchDescription = "Sweep your arms wide and gently lift through your chest.",
        trainerCue = "Keep the ribs stacked and make the movement feel spacious.",
        easierDescription = "Keep your arms low and sweep them outward through a smaller range.",
        durationSeconds = 50
    )

    val hipFlexorStretch = StretchRoutine(
        id = "hip-flexor-stretch",
        title = "Hip Flexor Stretch",
        goal = "Practice gentle movement at the front of each hip.",
        category = "Hips",
        targetAreas = listOf("Hip flexors"),
        steps = listOf("Left", "Right").map { side ->
            RoutineStep(
                stretch = Stretch(
                    id = "hip-flexor-${side.lowercase()}",
                    name = "$side hip flexor stretch",
                    description = "Stand with your $side leg behind you and hold a firm support. Bend the front " +
                        "knee gently until you feel a stretch at the front of the back hip.",
                    trainerCue = "Keep your back upright and move only through a comfortable range.",
                    easierDescription = "Use a shorter step and a smaller bend while holding a firm support."
                ),
                durationSeconds = 30,
                restSeconds = if (side == "Left") 10 else 0
            )
        }
    )

    val neckPostureReset = createRoutine(
        id = "neck-posture-reset",
        title = "Neck Posture Reset",
        goal = "Practice gentle neck alignment and control.",
        category = "Neck",
        targetAreas = listOf("Neck", "Posture"),
        stretchName = "Gentle chin nod",
        stretchDescription = "Lie on your back with a small towel under your head. Repeat subtle chin nods " +
            "without tensing the sides of your neck.",
        trainerCue = "Make a subtle movement and breathe normally.",
        easierDescription = "Use a smaller nod and stop if it feels uncomfortable.",
        durationSeconds = 60
    )

    val routines = listOf(
        roundedShouldersRoutine,
        lowerBackReset,
        hipMobility,
        neckRelief,
        recoveryFlow,
        quickReset,
        morningPosture,
        hipFlexorStretch,
        neckPostureReset
    )
}

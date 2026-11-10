package com.example.gymtracker.data.program

data class ProgramExercise(
    val id: String,
    val name: String,
    val repRange: String,
    val restTime: String,
    val setsPerWeek: List<Int>,
    val executionCues: String,
    val isTimedHold: Boolean = false
)

data class ProgramDay(
    val dayNumber: Int,
    val name: String,
    val focus: String,
    val exercises: List<ProgramExercise>
)

object GymProgram {

    val days: List<ProgramDay> = listOf(
        day1PushA(),
        day2PullA(),
        day3LegsA(),
        day4PushB(),
        day5PullB(),
        day6LegsB()
    )

    fun getDayByNumber(dayNumber: Int): ProgramDay? = days.find { it.dayNumber == dayNumber }

    const val WEEKS_PER_BLOCK = 6

    // Weeks are numbered continuously (1, 2, ... 7, 8, ...). The DB stores them as
    // block + week-in-cycle, where week 6 of every block is the deload.
    fun blockOf(absoluteWeek: Int): Int = (absoluteWeek - 1) / WEEKS_PER_BLOCK + 1

    fun cycleWeekOf(absoluteWeek: Int): Int = (absoluteWeek - 1) % WEEKS_PER_BLOCK + 1

    fun absoluteWeek(blockNumber: Int, weekNumber: Int): Int =
        (blockNumber - 1) * WEEKS_PER_BLOCK + weekNumber

    fun absoluteWeekOf(session: com.example.gymtracker.data.db.WorkoutSession): Int =
        absoluteWeek(session.blockNumber, session.weekNumber)

    fun getSetsForWeek(exercise: ProgramExercise, weekInCycle: Int): Int {
        val index = (weekInCycle - 1).coerceIn(0, exercise.setsPerWeek.lastIndex)
        return exercise.setsPerWeek[index]
    }

    private fun day1PushA() = ProgramDay(
        dayNumber = 1,
        name = "Push A",
        focus = "Chest-Emphasis",
        exercises = listOf(
            ProgramExercise(
                id = "sv_d1",
                name = "Stomach Vacuum",
                repRange = "20-30s hold",
                restTime = "30-45 sec",
                setsPerWeek = listOf(4, 4, 4, 5, 5, 4),
                executionCues = "Exhale all air, pull your navel toward your spine, hold for 20-30 seconds. Breathe shallowly through your chest while holding.",
                isTimedHold = true
            ),
            ProgramExercise(
                id = "incline_bb_bench",
                name = "Incline Barbell Bench Press",
                repRange = "6-8",
                restTime = "3 min",
                setsPerWeek = listOf(3, 4, 4, 4, 4, 2),
                executionCues = "Set bench at 30° — not 45°. Lower angle maximizes upper pec, limits front-delt takeover. Full range of motion, controlled eccentric."
            ),
            ProgramExercise(
                id = "incline_db_press_d1",
                name = "Incline Dumbbell Press",
                repRange = "8-12",
                restTime = "2-3 min",
                setsPerWeek = listOf(3, 3, 3, 3, 4, 2),
                executionCues = "30° incline. Dumbbells at shoulder width, press up and slightly inward. Full stretch at bottom, squeeze at top."
            ),
            ProgramExercise(
                id = "low_high_cable_flye",
                name = "Low-to-High Cable Flye",
                repRange = "12-15",
                restTime = "90 sec",
                setsPerWeek = listOf(2, 2, 3, 3, 3, 1),
                executionCues = "Set cables at the lowest pulley, arc the handles upward and inward. Squeeze 1 second at the top. Control the eccentric."
            ),
            ProgramExercise(
                id = "cable_lat_raise_d1",
                name = "Cable Lateral Raise",
                repRange = "12-20",
                restTime = "90 sec",
                setsPerWeek = listOf(5, 5, 5, 6, 6, 3),
                executionCues = "Stand sideways, handle in far hand, raise to ear height with a slight forward lean (10-15°). Control the eccentric for 2-3 seconds."
            ),
            ProgramExercise(
                id = "oh_cable_tri_ext",
                name = "Overhead Cable Triceps Extension",
                repRange = "10-15",
                restTime = "90 sec",
                setsPerWeek = listOf(3, 3, 3, 3, 4, 2),
                executionCues = "Face away from cable, rope behind head. Extend arms overhead fully. Focus on the stretch at the bottom and lockout at top."
            ),
            ProgramExercise(
                id = "rev_pec_deck_d1",
                name = "Reverse Pec-Deck Flye",
                repRange = "15-20",
                restTime = "90 sec",
                setsPerWeek = listOf(3, 3, 3, 3, 3, 2),
                executionCues = "Chest against pad, arms slightly below shoulder height. Squeeze rear delts at full contraction, 1-second hold."
            )
        )
    )

    private fun day2PullA() = ProgramDay(
        dayNumber = 2,
        name = "Pull A",
        focus = "Lat-Width",
        exercises = listOf(
            ProgramExercise(
                id = "sv_d2",
                name = "Stomach Vacuum",
                repRange = "20-30s hold",
                restTime = "30-45 sec",
                setsPerWeek = listOf(4, 4, 4, 5, 5, 4),
                executionCues = "Exhale all air, pull your navel toward your spine, hold for 20-30 seconds.",
                isTimedHold = true
            ),
            ProgramExercise(
                id = "wide_pullup",
                name = "Wide-Grip Pull-Up",
                repRange = "6-10",
                restTime = "3 min",
                setsPerWeek = listOf(4, 4, 4, 4, 4, 2),
                executionCues = "Full dead hang at bottom, chin over bar at top. Grip at 1.5× shoulder width. Once you can do 4×10 at bodyweight, add 2.5 kg with a belt."
            ),
            ProgramExercise(
                id = "single_arm_lat_pd",
                name = "Single-Arm Lat Pulldown",
                repRange = "10-15",
                restTime = "90 sec",
                setsPerWeek = listOf(3, 4, 4, 5, 5, 2),
                executionCues = "Lean your torso slightly toward the working side (10-15°) and pull the handle toward your hip, not your shoulder. Maximizes lower lat sweep."
            ),
            ProgramExercise(
                id = "wide_cable_upright_row_d2",
                name = "Wide-Grip Cable Upright Row",
                repRange = "12-15",
                restTime = "2-3 min",
                setsPerWeek = listOf(4, 4, 4, 5, 5, 2),
                executionCues = "Grip at 1.5-2× shoulder width. Pull only to the xiphoid level — never above the collarbone. Wide grip shifts load from traps to lateral delts."
            ),
            ProgramExercise(
                id = "cable_face_pull_d2",
                name = "Cable Face Pull",
                repRange = "15-20",
                restTime = "90 sec",
                setsPerWeek = listOf(2, 2, 3, 3, 3, 1),
                executionCues = "High cable, rope attachment. Pull toward face, externally rotate at the end — thumbs pointing back. Squeeze rear delts and external rotators."
            ),
            ProgramExercise(
                id = "incline_db_curl",
                name = "Incline Dumbbell Curl",
                repRange = "8-12",
                restTime = "90 sec",
                setsPerWeek = listOf(3, 3, 4, 4, 4, 2),
                executionCues = "Bench at 45-60°. Arms hang straight at the bottom — maximum long-head stretch at the start of each rep. No swinging."
            )
        )
    )

    private fun day3LegsA() = ProgramDay(
        dayNumber = 3,
        name = "Legs A",
        focus = "Quad-Sweep",
        exercises = listOf(
            ProgramExercise(
                id = "sv_d3",
                name = "Stomach Vacuum",
                repRange = "20-30s hold",
                restTime = "30-45 sec",
                setsPerWeek = listOf(5, 5, 5, 5, 5, 4),
                executionCues = "Exhale all air, pull your navel toward your spine, hold for 20-30 seconds.",
                isTimedHold = true
            ),
            ProgramExercise(
                id = "front_squat",
                name = "Front Squat",
                repRange = "6-8",
                restTime = "3 min",
                setsPerWeek = listOf(3, 3, 3, 4, 4, 2),
                executionCues = "Hip-width stance, toes slightly out. Upright torso shifts stress onto quads. Cross-arm grip if wrists are limited."
            ),
            ProgramExercise(
                id = "narrow_leg_press",
                name = "Narrow-Stance Leg Press",
                repRange = "10-15",
                restTime = "2-3 min",
                setsPerWeek = listOf(2, 3, 3, 3, 3, 1),
                executionCues = "Feet narrow (inside hip width) and LOW on the platform. Maximizes knee flexion and vastus lateralis recruitment."
            ),
            ProgramExercise(
                id = "standing_calf_raise_d3",
                name = "Standing Calf Raise",
                repRange = "8-12",
                restTime = "2 min",
                setsPerWeek = listOf(3, 4, 4, 4, 4, 2),
                executionCues = "Pause 2-3 seconds at the deep bottom stretch. Full contraction at top with 1-2 second hold. Lengthened-position emphasis drives gastrocnemius growth."
            ),
            ProgramExercise(
                id = "seated_calf_raise",
                name = "Seated Calf Raise",
                repRange = "15-20",
                restTime = "90 sec",
                setsPerWeek = listOf(3, 3, 3, 4, 4, 2),
                executionCues = "Full range of motion. Pause at the bottom stretch. Targets the soleus muscle underneath the gastrocnemius."
            ),
            ProgramExercise(
                id = "lying_leg_curl",
                name = "Lying Leg Curl",
                repRange = "10-15",
                restTime = "90 sec",
                setsPerWeek = listOf(3, 3, 3, 3, 3, 2),
                executionCues = "Keep hips pressed into the pad. Control the eccentric for 2-3 seconds. Don't let the weight stack slam."
            )
        )
    )

    private fun day4PushB() = ProgramDay(
        dayNumber = 4,
        name = "Push B",
        focus = "Delt-Emphasis",
        exercises = listOf(
            ProgramExercise(
                id = "sv_d4",
                name = "Stomach Vacuum",
                repRange = "20-30s hold",
                restTime = "30-45 sec",
                setsPerWeek = listOf(4, 4, 4, 5, 5, 4),
                executionCues = "Exhale all air, pull your navel toward your spine, hold for 20-30 seconds.",
                isTimedHold = true
            ),
            ProgramExercise(
                id = "cable_lat_raise_d4",
                name = "Cable Lateral Raise",
                repRange = "12-20",
                restTime = "90 sec",
                setsPerWeek = listOf(4, 4, 5, 5, 5, 2),
                executionCues = "Lateral delts go FIRST. Do not pre-exhaust with pressing. Maximum neural drive for the highest-priority V-taper muscle."
            ),
            ProgramExercise(
                id = "btb_cable_lat_raise",
                name = "Behind-the-Back Cable Lateral Raise",
                repRange = "12-20",
                restTime = "90 sec",
                setsPerWeek = listOf(3, 4, 4, 4, 4, 1),
                executionCues = "Stand sideways, reach behind your back for the low-pulley handle, raise laterally. Places the lateral delt under stretch at the bottom — lengthened-position stimulus."
            ),
            ProgramExercise(
                id = "incline_db_press_d4",
                name = "Incline Dumbbell Press",
                repRange = "8-12",
                restTime = "2-3 min",
                setsPerWeek = listOf(3, 3, 3, 3, 3, 1),
                executionCues = "30° incline. Same form as Day 1. Controlled reps, full range of motion."
            ),
            ProgramExercise(
                id = "weighted_dip",
                name = "Weighted Dip",
                repRange = "8-12",
                restTime = "3 min",
                setsPerWeek = listOf(2, 2, 2, 3, 3, 1),
                executionCues = "Lean forward 30-45° from vertical to target chest. Upright dips shift stress to triceps. Full depth, elbows to 90°."
            ),
            ProgramExercise(
                id = "flat_db_flye",
                name = "Flat Dumbbell Flye",
                repRange = "12-15",
                restTime = "90 sec",
                setsPerWeek = listOf(1, 1, 1, 1, 1, 1),
                executionCues = "Single set each week to supplement chest volume. Light weight, deep stretch at the bottom, 2-second hold."
            ),
            ProgramExercise(
                id = "close_grip_bench",
                name = "Close-Grip Bench Press",
                repRange = "8-12",
                restTime = "2-3 min",
                setsPerWeek = listOf(3, 3, 3, 3, 3, 1),
                executionCues = "Hands shoulder-width or slightly narrower. Elbows tucked. Focus on triceps lockout."
            ),
            ProgramExercise(
                id = "tri_rope_pushdown",
                name = "Triceps Rope Pushdown",
                repRange = "12-15",
                restTime = "90 sec",
                setsPerWeek = listOf(2, 2, 3, 3, 3, 1),
                executionCues = "Keep elbows pinned at your sides. Spread the rope at the bottom for peak contraction. Control the eccentric."
            ),
            ProgramExercise(
                id = "rev_pec_deck_d4",
                name = "Reverse Pec-Deck Flye",
                repRange = "15-20",
                restTime = "90 sec",
                setsPerWeek = listOf(3, 3, 3, 3, 3, 2),
                executionCues = "Same as Day 1. Chest against pad, squeeze rear delts at full contraction."
            )
        )
    )

    private fun day5PullB() = ProgramDay(
        dayNumber = 5,
        name = "Pull B",
        focus = "Lower Lats / Sweep",
        exercises = listOf(
            ProgramExercise(
                id = "sv_d5",
                name = "Stomach Vacuum",
                repRange = "20-30s hold",
                restTime = "30-45 sec",
                setsPerWeek = listOf(4, 4, 4, 5, 5, 4),
                executionCues = "Exhale all air, pull your navel toward your spine, hold for 20-30 seconds.",
                isTimedHold = true
            ),
            ProgramExercise(
                id = "meadows_row",
                name = "Meadows Row",
                repRange = "8-12",
                restTime = "2-3 min",
                setsPerWeek = listOf(4, 4, 4, 4, 4, 2),
                executionCues = "Barbell wedged in a landmine or corner. Stand perpendicular, staggered stance, overhand grip, row one end. Targets lower/outer lats and teres major — the muscles that create sweep visible from behind."
            ),
            ProgramExercise(
                id = "straight_arm_lat_pd",
                name = "Straight-Arm Lat Pulldown",
                repRange = "12-20",
                restTime = "90 sec",
                setsPerWeek = listOf(3, 3, 4, 4, 5, 2),
                executionCues = "Face the cable, slight forward hip hinge (30°), arms nearly straight. Arc the bar from eye level to thighs. Keep elbows locked — any bend shifts load to biceps."
            ),
            ProgramExercise(
                id = "wide_cable_upright_row_d5",
                name = "Wide-Grip Cable Upright Row",
                repRange = "12-15",
                restTime = "2-3 min",
                setsPerWeek = listOf(4, 4, 4, 5, 5, 2),
                executionCues = "Same form as Day 2. Wide grip, pull to xiphoid level only."
            ),
            ProgramExercise(
                id = "cable_face_pull_d5",
                name = "Cable Face Pull",
                repRange = "15-20",
                restTime = "90 sec",
                setsPerWeek = listOf(2, 2, 2, 2, 3, 1),
                executionCues = "High cable, rope attachment. Pull toward face with external rotation. Thumbs pointing back at the end."
            ),
            ProgramExercise(
                id = "barbell_curl",
                name = "Barbell Curl",
                repRange = "8-12",
                restTime = "90 sec",
                setsPerWeek = listOf(3, 3, 3, 3, 4, 2),
                executionCues = "Shoulder-width grip. No swinging. Controlled eccentric. Squeeze at the top."
            )
        )
    )

    private fun day6LegsB() = ProgramDay(
        dayNumber = 6,
        name = "Legs B",
        focus = "Calf + Quad",
        exercises = listOf(
            ProgramExercise(
                id = "sv_d6",
                name = "Stomach Vacuum",
                repRange = "20-30s hold",
                restTime = "30-45 sec",
                setsPerWeek = listOf(5, 5, 5, 5, 5, 4),
                executionCues = "Exhale all air, pull your navel toward your spine, hold for 20-30 seconds.",
                isTimedHold = true
            ),
            ProgramExercise(
                id = "standing_calf_raise_d6",
                name = "Standing Calf Raise",
                repRange = "8-12",
                restTime = "2 min",
                setsPerWeek = listOf(3, 3, 4, 4, 4, 2),
                executionCues = "Calves lead this session. Pause 2-3 seconds at the deep bottom stretch. Full contraction at top."
            ),
            ProgramExercise(
                id = "leg_press_calf_raise",
                name = "Leg-Press Calf Raise",
                repRange = "12-20",
                restTime = "90 sec",
                setsPerWeek = listOf(3, 3, 3, 3, 4, 2),
                executionCues = "Balls of feet on the bottom edge of the platform. Emphasize the bottom stretch — 2-3 second hold before driving up."
            ),
            ProgramExercise(
                id = "hack_squat",
                name = "Hack Squat",
                repRange = "8-12",
                restTime = "3 min",
                setsPerWeek = listOf(3, 3, 3, 3, 4, 2),
                executionCues = "Narrow stance, feet low, for outer quad (vastus lateralis) bias. Full depth, controlled eccentric."
            ),
            ProgramExercise(
                id = "leg_extension",
                name = "Leg Extension",
                repRange = "15-20",
                restTime = "90 sec",
                setsPerWeek = listOf(2, 2, 3, 3, 3, 1),
                executionCues = "Toes turned slightly inward for marginal vastus lateralis emphasis. Slow, controlled reps. Squeeze at the top."
            ),
            ProgramExercise(
                id = "romanian_deadlift",
                name = "Romanian Deadlift",
                repRange = "8-12",
                restTime = "3 min",
                setsPerWeek = listOf(3, 3, 3, 3, 3, 2),
                executionCues = "Hip hinge, slight knee bend. Bar stays close to legs. Feel the hamstring stretch. Don't round your back."
            )
        )
    )
}

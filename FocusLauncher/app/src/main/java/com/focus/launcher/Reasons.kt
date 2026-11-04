package com.focus.launcher

/**
 * One answer to "why are you opening it?". [idle] answers ("bored", "just browsing") earn a
 * longer wait; [ask] makes you name the exact thing first ("What do you need to buy?").
 */
data class Reason(val label: String, val idle: Boolean = false, val ask: String? = null)

private val bored = Reason("Bored", idle = true)
private val habit = Reason("Habit", idle = true)

/** Reasons that make sense for this kind of app: nobody "replies to someone" in Chrome. */
fun reasonsFor(kind: AppKind): List<Reason> = when (kind) {
    AppKind.BROWSER -> listOf(
        Reason("Look something up", ask = "What are you looking up?"),
        Reason("Read something specific", ask = "What are you reading?"),
        bored, habit,
    )
    AppKind.SHOPPING -> listOf(
        Reason("Buy something I need", ask = "What do you need to buy?"),
        Reason("Track an order"),
        Reason("Just browsing", idle = true),
        Reason("Saw a deal", idle = true),
    )
    AppKind.MESSAGING -> listOf(
        Reason("Reply to someone"),
        Reason("Message someone", ask = "Who are you messaging?"),
        Reason("Check the groups", idle = true),
        habit,
    )
    AppKind.SOCIAL -> listOf(
        Reason("Reply to someone"),
        Reason("Post something"),
        Reason("Check on someone", ask = "Whose update?"),
        bored, habit,
    )
    AppKind.VIDEO -> listOf(
        Reason("Watch something specific", ask = "What will you watch?"),
        Reason("A planned break"),
        bored, habit,
    )
    AppKind.GAME -> listOf(Reason("A planned break"), bored, habit)
    AppKind.NEWS -> listOf(Reason("Read one story", ask = "Which story?"), bored, habit)
    AppKind.OTHER -> listOf(
        Reason("Something specific", ask = "What do you need it for?"),
        Reason("Reply to someone"),
        bored, habit,
    )
}

/** Words for what an idle answer means for this kind of app. */
fun idleNudge(kind: AppKind): String = when (kind) {
    AppKind.SHOPPING -> "Browsing is how unplanned orders happen. Ten more seconds to be sure."
    AppKind.BROWSER -> "Aimless browsing rarely ends quickly. Ten more seconds to be sure."
    AppKind.VIDEO -> "Autoplay will happily take the evening. Ten more seconds to be sure."
    else -> "Totally human. Ten more seconds to see if you still want it."
}

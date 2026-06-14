package com.braindump.app.data

import java.util.Calendar

/** A small bundled set of quotes shown on the home screen. Fully offline. */
object Quotes {

    private val quotes: List<Pair<String, String>> = listOf(
        "The future belongs to those who believe in the beauty of their dreams." to "Eleanor Roosevelt",
        "Write it on your heart that every day is the best day in the year." to "Ralph Waldo Emerson",
        "The secret of getting ahead is getting started." to "Mark Twain",
        "You are never too old to set another goal or to dream a new dream." to "C.S. Lewis",
        "Your mind is for having ideas, not holding them." to "David Allen",
        "Clear your mind of can't." to "Samuel Johnson",
        "An idea that is not dangerous is unworthy of being called an idea at all." to "Oscar Wilde",
        "The mind is everything. What you think, you become." to "Buddha",
        "Creativity takes courage." to "Henri Matisse",
        "Done is better than perfect." to "Sheryl Sandberg",
        "Either write something worth reading or do something worth writing." to "Benjamin Franklin",
        "Journaling is like whispering to one's self and listening at the same time." to "Mina Murray",
        "Fill your paper with the breathings of your heart." to "William Wordsworth",
        "What we plant in the soil of contemplation, we shall reap in the harvest of action." to "Meister Eckhart",
        "The unexamined life is not worth living." to "Socrates",
        "A thought which does not result in an action is nothing much, and an action which does not proceed from a thought is nothing at all." to "Georges Bernanos",
        "Worry never robs tomorrow of its sorrow, it only saps today of its joy." to "Leo Buscaglia",
        "Start where you are. Use what you have. Do what you can." to "Arthur Ashe",
        "Small daily improvements are the key to staggering long-term results." to "James Clear",
        "The best way to predict your future is to create it." to "Abraham Lincoln",
        "Every accomplishment starts with the decision to try." to "John F. Kennedy",
        "Notice the difference between what you write and what you say." to "Anonymous",
        "The act of writing is the act of discovering what you believe." to "David Hare",
        "You don't have to see the whole staircase, just take the first step." to "Martin Luther King Jr.",
        "Wherever you go, go with all your heart." to "Confucius",
        "Quiet the mind and the soul will speak." to "Ma Jaya Sati Bhagavati",
        "Today's accomplishments were yesterday's impossibilities." to "Robert H. Schuller",
        "Be curious, not judgmental." to "Walt Whitman",
        "Almost everything will work again if you unplug it for a few minutes, including you." to "Anne Lamott",
        "Progress, not perfection." to "Anonymous",
        "Out of clutter, find simplicity." to "Albert Einstein",
        "The only way to do great work is to love what you do." to "Steve Jobs",
        "Gratitude turns what we have into enough." to "Anonymous",
        "Inhale courage, exhale fear." to "Anonymous",
        "Take care of your body. It's the only place you have to live." to "Jim Rohn"
    )

    fun ofToday(): Pair<String, String> {
        val dayOfYear = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
        return quotes[dayOfYear % quotes.size]
    }

    fun random(): Pair<String, String> = quotes.random()
}

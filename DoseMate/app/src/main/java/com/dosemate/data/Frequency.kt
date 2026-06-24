package com.dosemate.data

enum class Frequency {
    DAILY,
    SPECIFIC_DAYS,
    ALTERNATE_DAYS,
    EVERY_X_HOURS,
    /** As-needed dose: no fixed reminder time. The user logs each dose manually when taken. */
    SOS
}

enum class LogStatus {
    PENDING,
    TAKEN,
    MISSED,
    SKIPPED
}

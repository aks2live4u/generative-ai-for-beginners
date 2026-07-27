/// Truncates a [DateTime] down to the calendar day (local time, midnight).
DateTime dayKey(DateTime dt) => DateTime(dt.year, dt.month, dt.day);

/// The Monday that starts the week containing [dt].
DateTime startOfWeek(DateTime dt) {
  final d = dayKey(dt);
  return d.subtract(Duration(days: d.weekday - 1));
}

DateTime startOfMonth(DateTime dt) => DateTime(dt.year, dt.month, 1);

bool isSameDay(DateTime a, DateTime b) => a.year == b.year && a.month == b.month && a.day == b.day;

/// True if [b] is exactly one calendar day before [a].
bool isPreviousDay(DateTime a, DateTime b) {
  final prev = dayKey(a).subtract(const Duration(days: 1));
  return isSameDay(prev, b);
}

import 'package:flutter/material.dart';
import '../../../core/utils/date_utils.dart';

/// A simple month-grid heatmap: one cell per day, colored by how many
/// focus minutes were logged that day relative to the busiest day shown.
class CalendarHeatmap extends StatelessWidget {
  final DateTime month;
  final Map<DateTime, int> minutesPerDay;
  final Color color;

  const CalendarHeatmap({super.key, required this.month, required this.minutesPerDay, required this.color});

  @override
  Widget build(BuildContext context) {
    final first = DateTime(month.year, month.month, 1);
    final daysInMonth = DateTime(month.year, month.month + 1, 0).day;
    final leadingBlanks = first.weekday - 1;
    final maxMinutes = minutesPerDay.values.fold<int>(0, (m, v) => v > m ? v : m);
    final theme = Theme.of(context);

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text('${_monthName(month.month)} ${month.year}', style: theme.textTheme.titleMedium),
        const SizedBox(height: 10),
        GridView.builder(
          shrinkWrap: true,
          physics: const NeverScrollableScrollPhysics(),
          gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
            crossAxisCount: 7,
            mainAxisSpacing: 5,
            crossAxisSpacing: 5,
          ),
          itemCount: leadingBlanks + daysInMonth,
          itemBuilder: (context, index) {
            if (index < leadingBlanks) return const SizedBox.shrink();
            final day = index - leadingBlanks + 1;
            final date = dayKey(DateTime(month.year, month.month, day));
            final minutes = minutesPerDay[date] ?? 0;
            final intensity = maxMinutes == 0 ? 0.0 : (minutes / maxMinutes).clamp(0.12, 1.0);
            final isToday = isSameDay(date, DateTime.now());
            return Container(
              decoration: BoxDecoration(
                color: minutes == 0 ? color.withValues(alpha: 0.08) : color.withValues(alpha: intensity),
                borderRadius: BorderRadius.circular(6),
                border: isToday ? Border.all(color: color, width: 1.5) : null,
              ),
              alignment: Alignment.center,
              child: Text(
                '$day',
                style: TextStyle(
                  fontSize: 10,
                  color: minutes > (maxMinutes * 0.55)
                      ? Colors.white
                      : theme.colorScheme.onSurface.withValues(alpha: 0.6),
                ),
              ),
            );
          },
        ),
      ],
    );
  }

  String _monthName(int m) => const [
    'January',
    'February',
    'March',
    'April',
    'May',
    'June',
    'July',
    'August',
    'September',
    'October',
    'November',
    'December',
  ][m - 1];
}

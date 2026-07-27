import 'package:fl_chart/fl_chart.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../core/utils/date_utils.dart';
import '../../data/models/session_model.dart';
import '../../domain/stats/stats_calculator.dart';
import '../providers/repositories_providers.dart';
import '../widgets/common/calendar_heatmap.dart';

enum _Period { today, week, month }

class StatsScreen extends ConsumerStatefulWidget {
  const StatsScreen({super.key});

  @override
  ConsumerState<StatsScreen> createState() => _StatsScreenState();
}

class _StatsScreenState extends ConsumerState<StatsScreen> {
  _Period _period = _Period.today;

  @override
  Widget build(BuildContext context) {
    ref.watch(refreshTickProvider);
    final theme = Theme.of(context);
    final sessions = ref.read(sessionRepositoryProvider).getAll();

    final stats = switch (_period) {
      _Period.today => StatsCalculator.today(sessions),
      _Period.week => StatsCalculator.thisWeek(sessions),
      _Period.month => StatsCalculator.thisMonth(sessions),
    };
    final streaks = StatsCalculator.streaks(sessions);

    final now = DateTime.now();
    final weekStart = startOfWeek(now);
    final weekStats = StatsCalculator.thisWeek(sessions);
    final monthStats = StatsCalculator.thisMonth(sessions);

    final weekMinutesByType = StatsCalculator.minutesByType(
      sessions,
      weekStart,
      weekStart.add(const Duration(days: 7)),
    );

    return Scaffold(
      appBar: AppBar(title: const Text('Statistics')),
      body: ListView(
        padding: const EdgeInsets.all(20),
        children: [
          SegmentedButton<_Period>(
            segments: const [
              ButtonSegment(value: _Period.today, label: Text('Today')),
              ButtonSegment(value: _Period.week, label: Text('This Week')),
              ButtonSegment(value: _Period.month, label: Text('This Month')),
            ],
            selected: {_period},
            onSelectionChanged: (s) => setState(() => _period = s.first),
          ),
          const SizedBox(height: 20),
          GridView.count(
            crossAxisCount: 2,
            shrinkWrap: true,
            physics: const NeverScrollableScrollPhysics(),
            mainAxisSpacing: 12,
            crossAxisSpacing: 12,
            childAspectRatio: 1.6,
            children: [
              _StatCard(label: 'Focus Hours', value: _formatDuration(stats.totalFocusTime)),
              _StatCard(label: 'Sessions', value: '${stats.completedSessions}'),
              _StatCard(label: 'Longest Streak', value: '${streaks.longest} days'),
              _StatCard(label: 'Avg Focus', value: '${stats.averageFocusMinutes.round()} min'),
            ],
          ),
          const SizedBox(height: 28),
          Text('This Week', style: theme.textTheme.titleLarge),
          const SizedBox(height: 12),
          SizedBox(
            height: 180,
            child: _WeekBarChart(
              sessionsPerDay: weekStats.sessionsPerDay,
              weekStart: weekStart,
              color: theme.colorScheme.primary,
            ),
          ),
          const SizedBox(height: 28),
          if (weekMinutesByType.isNotEmpty) ...[
            Text('Time Split This Week', style: theme.textTheme.titleLarge),
            const SizedBox(height: 12),
            SizedBox(
              height: 160,
              child: _TypePieChart(minutesByType: weekMinutesByType, theme: theme),
            ),
            const SizedBox(height: 28),
          ],
          Text('This Month', style: theme.textTheme.titleLarge),
          const SizedBox(height: 12),
          CalendarHeatmap(month: now, minutesPerDay: monthStats.focusMinutesPerDay, color: theme.colorScheme.primary),
          const SizedBox(height: 20),
        ],
      ),
    );
  }

  String _formatDuration(Duration d) {
    final h = d.inHours;
    final m = d.inMinutes % 60;
    if (h > 0) return '${h}h ${m}m';
    return '${m}m';
  }
}

class _StatCard extends StatelessWidget {
  final String label;
  final String value;
  const _StatCard({required this.label, required this.value});

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Text(value, style: theme.textTheme.headlineMedium),
            const SizedBox(height: 4),
            Text(label, style: theme.textTheme.bodyMedium),
          ],
        ),
      ),
    );
  }
}

class _WeekBarChart extends StatelessWidget {
  final Map<DateTime, int> sessionsPerDay;
  final DateTime weekStart;
  final Color color;

  const _WeekBarChart({required this.sessionsPerDay, required this.weekStart, required this.color});

  static const _labels = ['M', 'T', 'W', 'T', 'F', 'S', 'S'];

  @override
  Widget build(BuildContext context) {
    final maxY = (sessionsPerDay.values.fold<int>(0, (m, v) => v > m ? v : m)).clamp(4, 999).toDouble();
    return BarChart(
      BarChartData(
        maxY: maxY,
        alignment: BarChartAlignment.spaceAround,
        gridData: const FlGridData(show: false),
        borderData: FlBorderData(show: false),
        titlesData: FlTitlesData(
          leftTitles: const AxisTitles(sideTitles: SideTitles(showTitles: false)),
          rightTitles: const AxisTitles(sideTitles: SideTitles(showTitles: false)),
          topTitles: const AxisTitles(sideTitles: SideTitles(showTitles: false)),
          bottomTitles: AxisTitles(
            sideTitles: SideTitles(
              showTitles: true,
              getTitlesWidget: (value, meta) =>
                  Padding(padding: const EdgeInsets.only(top: 6), child: Text(_labels[value.toInt().clamp(0, 6)])),
            ),
          ),
        ),
        barGroups: List.generate(7, (i) {
          final date = dayKey(weekStart.add(Duration(days: i)));
          final count = sessionsPerDay[date] ?? 0;
          return BarChartGroupData(
            x: i,
            barRods: [
              BarChartRodData(toY: count.toDouble(), color: color, width: 18, borderRadius: BorderRadius.circular(6)),
            ],
          );
        }),
      ),
    );
  }
}

class _TypePieChart extends StatelessWidget {
  final Map<SessionType, int> minutesByType;
  final ThemeData theme;

  const _TypePieChart({required this.minutesByType, required this.theme});

  @override
  Widget build(BuildContext context) {
    final colors = {
      SessionType.focus: theme.colorScheme.primary,
      SessionType.shortBreak: Colors.tealAccent.shade400,
      SessionType.longBreak: Colors.deepPurpleAccent.shade100,
    };
    final labels = {SessionType.focus: 'Focus', SessionType.shortBreak: 'Break', SessionType.longBreak: 'Long Break'};
    final total = minutesByType.values.fold<int>(0, (a, b) => a + b);

    return Row(
      children: [
        Expanded(
          child: PieChart(
            PieChartData(
              sectionsSpace: 3,
              centerSpaceRadius: 30,
              sections: minutesByType.entries.map((e) {
                final pct = total == 0 ? 0 : (e.value / total * 100);
                return PieChartSectionData(
                  color: colors[e.key],
                  value: e.value.toDouble(),
                  title: '${pct.round()}%',
                  radius: 50,
                  titleStyle: const TextStyle(fontSize: 12, color: Colors.white, fontWeight: FontWeight.bold),
                );
              }).toList(),
            ),
          ),
        ),
        const SizedBox(width: 12),
        Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          mainAxisAlignment: MainAxisAlignment.center,
          children: minutesByType.keys
              .map(
                (type) => Padding(
                  padding: const EdgeInsets.symmetric(vertical: 4),
                  child: Row(
                    children: [
                      Container(
                        width: 12,
                        height: 12,
                        decoration: BoxDecoration(color: colors[type], shape: BoxShape.circle),
                      ),
                      const SizedBox(width: 8),
                      Text(labels[type] ?? '', style: theme.textTheme.bodyMedium),
                    ],
                  ),
                ),
              )
              .toList(),
        ),
      ],
    );
  }
}

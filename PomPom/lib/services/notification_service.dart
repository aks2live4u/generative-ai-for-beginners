import 'package:flutter_local_notifications/flutter_local_notifications.dart';
import '../core/constants/app_constants.dart';

/// One-shot, non-ongoing notifications ("Ready to focus?", "Break time!",
/// "Session completed!", "Time to relax"). The persistent countdown
/// notification while a timer is running is owned separately by the
/// foreground task service, not this class.
class NotificationService {
  static final NotificationService instance = NotificationService._();
  NotificationService._();

  final FlutterLocalNotificationsPlugin _plugin = FlutterLocalNotificationsPlugin();
  bool _initialized = false;
  int _idCounter = 1000;

  Future<void> init() async {
    if (_initialized) return;
    const androidInit = AndroidInitializationSettings('@mipmap/ic_launcher');
    const settings = InitializationSettings(android: androidInit);
    await _plugin.initialize(settings: settings);
    _initialized = true;
  }

  Future<void> _show(String title, String body) async {
    if (!_initialized) await init();
    const details = NotificationDetails(
      android: AndroidNotificationDetails(
        kNotifChannelId,
        kNotifChannelName,
        channelDescription: 'Cute reminders from your PomPom companion.',
        importance: Importance.high,
        priority: Priority.high,
        playSound: false,
        styleInformation: BigTextStyleInformation(''),
      ),
    );
    await _plugin.show(id: _idCounter++, title: title, body: body, notificationDetails: details);
  }

  Future<void> readyToFocus() => _show('🌸 Ready to focus?', 'Your PomPom friend is waiting for you.');

  Future<void> breakStarted() => _show('🌿 Break time!', 'Stretch, sip some water, and relax for a bit.');

  Future<void> sessionCompleted() => _show('🎉 Session completed!', 'Great job! One more focus session done.');

  Future<void> timeToRelax() => _show('💤 Time to relax', 'You\'ve earned a longer break. Well done today!');
}

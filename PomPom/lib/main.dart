import 'package:flutter/material.dart';
import 'package:flutter_foreground_task/flutter_foreground_task.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:home_widget/home_widget.dart';

import 'app.dart';
import 'core/constants/app_constants.dart';
import 'data/repositories/hive_boxes.dart';
import 'services/home_widget_service.dart';
import 'services/notification_service.dart';
import 'services/sound_service.dart';

void main() async {
  WidgetsFlutterBinding.ensureInitialized();

  await HiveBoxes.initAll();
  await NotificationService.instance.init();
  await SoundService.instance.init();

  await HomeWidget.registerInteractivityCallback(pompomWidgetBackgroundCallback);

  FlutterForegroundTask.initCommunicationPort();
  FlutterForegroundTask.init(
    androidNotificationOptions: AndroidNotificationOptions(
      channelId: kForegroundNotifChannelId,
      channelName: kForegroundNotifChannelName,
      channelDescription: 'Keeps your focus timer running in the background.',
      channelImportance: NotificationChannelImportance.LOW,
      priority: NotificationPriority.LOW,
      visibility: NotificationVisibility.VISIBILITY_PUBLIC,
      onlyAlertOnce: true,
    ),
    iosNotificationOptions: const IOSNotificationOptions(),
    foregroundTaskOptions: ForegroundTaskOptions(
      eventAction: ForegroundTaskEventAction.repeat(1000),
      autoRunOnBoot: false,
      allowWakeLock: true,
    ),
  );

  runApp(const ProviderScope(child: PomPomApp()));
}

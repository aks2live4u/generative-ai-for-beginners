import 'hive_boxes.dart';

/// A single pending remote-control command written by the home screen
/// widget's background isolate and polled by the running foreground task.
/// Kept as a tiny Hive box (rather than a direct isolate call) because the
/// widget's tap handler runs in its own short-lived headless isolate that
/// has no direct channel to the foreground task isolate.
class WidgetCommandRepository {
  Future<void> pushCommand(String command) => HiveBoxes.widgetCommandsBox.put(HiveBoxes.widgetCommandKey, command);

  String? takeCommand() {
    final box = HiveBoxes.widgetCommandsBox;
    final value = box.get(HiveBoxes.widgetCommandKey);
    if (value != null) box.delete(HiveBoxes.widgetCommandKey);
    return value;
  }
}

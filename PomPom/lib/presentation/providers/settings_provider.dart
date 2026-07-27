import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../data/models/settings_model.dart';
import '../../data/repositories/settings_repository.dart';
import 'repositories_providers.dart';

class SettingsController extends StateNotifier<SettingsModel> {
  final SettingsRepository _repository;
  SettingsController(this._repository) : super(_repository.get());

  void update(SettingsModel Function(SettingsModel current) updater) {
    state = updater(state);
    _repository.save(state);
  }

  void addReward(int amount) => update((s) => s.copyWith(rewardBalance: s.rewardBalance + amount));

  void reload() => state = _repository.get();

  Future<void> resetAllData() async {
    await _repository.save(const SettingsModel());
    state = const SettingsModel();
  }
}

final settingsControllerProvider = StateNotifierProvider<SettingsController, SettingsModel>(
  (ref) => SettingsController(ref.read(settingsRepositoryProvider)),
);

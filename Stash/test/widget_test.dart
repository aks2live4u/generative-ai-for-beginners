import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:stash/main.dart';

void main() {
  testWidgets('Stash app launches to the home screen', (tester) async {
    await tester.pumpWidget(const ProviderScope(child: StashApp()));
    await tester.pump();

    expect(find.text('Stash'), findsWidgets);
  });
}

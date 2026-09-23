import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'package:mobile/main.dart';

void main() {
  testWidgets('renders the mobile CASE shell', (WidgetTester tester) async {
    // Provide a fake SharedPreferences so OfflineSyncService can initialize
    SharedPreferences.setMockInitialValues({});

    await tester.pumpWidget(const UmlMobileApp());
    // The app bar title should be visible immediately
    expect(find.text('UML CASE Mobile'), findsOneWidget);
    // Wait for the async init to settle
    await tester.pumpAndSettle(const Duration(seconds: 2));
    // After init the "Proyecto:" line should appear
    expect(find.textContaining('Proyecto:'), findsOneWidget);
  });
}

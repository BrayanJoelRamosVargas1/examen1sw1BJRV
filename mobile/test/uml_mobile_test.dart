import 'dart:convert';
import 'package:flutter_test/flutter_test.dart';
import 'package:http/http.dart' as http;
import 'package:http/testing.dart';
import 'package:mobile/models/uml_models.dart';
import 'package:mobile/services/uml_api_service.dart';
import 'package:mobile/services/uml_realtime_service.dart';

void main() {
  test('parses model classes and members', () {
    final model = UmlModel.fromJson({
      'projectId': 'p1',
      'version': 4,
      'classes': [
        {
          'id': 'c1',
          'name': 'Cliente',
          'attributes': [
            {'id': 'a1', 'name': 'nombre', 'type': 'String'},
          ],
          'operations': [],
        },
      ],
      'relationships': [],
    });
    expect(model.version, 4);
    expect(model.classes.single.attributes.single.name, 'nombre');
  });

  test(
    'sends create class with expected version and refreshes model',
    () async {
      final client = MockClient((request) async {
        if (request.method == 'POST') {
          final body = jsonDecode(request.body) as Map<String, dynamic>;
          expect(body['expectedVersion'], 4);
          expect(body['participantId'], startsWith('mobile-'));
          return http.Response(
            jsonEncode({'modelVersion': 5, 'classId': 'c1'}),
            200,
          );
        }
        return http.Response(
          jsonEncode({
            'projectId': 'p1',
            'version': 5,
            'classes': [],
            'relationships': [],
          }),
          200,
        );
      });
      final service = UmlApiService(
        client: client,
        baseUrl: 'http://test/api',
        projectId: 'p1',
      );
      final result = await service.createClass('Cliente', 4);
      expect(result.version, 5);
    },
  );

  test('maps 409 as a conflict without retrying', () async {
    final client = MockClient(
      (request) async => http.Response('conflict', 409),
    );
    final service = UmlApiService(
      client: client,
      baseUrl: 'http://test/api',
      projectId: 'p1',
    );
    expect(
      () => service.createClass('Cliente', 4),
      throwsA(
        isA<UmlApiException>().having((e) => e.statusCode, 'status', 409),
      ),
    );
  });

  test('ignores stale realtime events and resyncs on a version gap', () {
    var resyncs = 0;
    var events = 0;
    final service = UmlRealtimeService(
      onResyncRequired: () => resyncs++,
      onEvent: (_) => events++,
    );
    service.currentVersion = 3;
    service.handleMessageForTest({'modelVersion': 2});
    service.handleMessageForTest({'modelVersion': 5});
    expect(events, 0);
    expect(resyncs, 1);
  });
}


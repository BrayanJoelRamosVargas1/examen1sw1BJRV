import 'dart:async';
import 'dart:convert';
import 'package:stomp_dart_client/stomp_dart_client.dart';

class UmlRealtimeEvent {
  final int modelVersion;
  final Map<String, dynamic> payload;
  const UmlRealtimeEvent(this.modelVersion, this.payload);
}

class UmlRealtimeService {
  int currentVersion = 0;
  StompClient? _client;
  final void Function()? onResyncRequired;
  final void Function(UmlRealtimeEvent event)? onEvent;

  UmlRealtimeService({this.onResyncRequired, this.onEvent});

  Future<void> connect(String wsUrl, String projectId) async {
    final connected = Completer<void>();
    _client = StompClient(
      config: StompConfig(
        url: wsUrl,
        onConnect: (_) {
          _client!.subscribe(
            destination: '/topic/projects/$projectId',
            callback: (frame) {
              final body = frame.body;
              if (body != null) _handleMessage(body);
            },
          );
          if (!connected.isCompleted) connected.complete();
        },
        onWebSocketError: (_) {
          if (!connected.isCompleted) {
            connected.completeError('WebSocket error');
          }
          onResyncRequired?.call();
        },
      ),
    );
    _client!.activate();
    await connected.future;
  }

  void _handleMessage(dynamic raw) {
    final payload = jsonDecode(raw as String) as Map<String, dynamic>;
    _processPayload(payload);
  }

  /// Exposed for unit tests: inject a decoded payload directly.
  // ignore: avoid_redundant_argument_values
  void handleMessageForTest(Map<String, dynamic> payload) => _processPayload(payload);

  void _processPayload(Map<String, dynamic> payload) {
    final version = (payload['modelVersion'] as num?)?.toInt();
    if (version == null || version <= currentVersion) return;
    if (version > currentVersion + 1) {
      onResyncRequired?.call();
      return;
    }
    currentVersion = version;
    onEvent?.call(UmlRealtimeEvent(version, payload));
  }

  Future<void> dispose() async => _client?.deactivate();
}

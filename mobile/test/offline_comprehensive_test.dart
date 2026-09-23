import 'dart:async';
import 'dart:convert';
import 'dart:io';

import 'package:flutter_test/flutter_test.dart';
import 'package:http/http.dart' as http;
import 'package:http/testing.dart';
import 'package:mobile/models/pending_uml_command.dart';
import 'package:mobile/models/uml_models.dart';
import 'package:mobile/services/offline_store.dart';
import 'package:mobile/services/offline_sync_service.dart';
import 'package:mobile/services/participant_identity_service.dart';
import 'package:mobile/services/uml_api_service.dart';
import 'package:mobile/services/uml_realtime_service.dart';

// ─── In-memory OfflineStore for unit tests ────────────────────────────────────
class MemoryStore implements OfflineStore {
  UmlModel? model;
  List<PendingUmlCommand> commands = [];
  String? participant;
  Map<String, String> mapping = {};

  @override Future<UmlModel?> loadCachedModel() async => model;
  @override Future<void> saveCachedModel(UmlModel v) async => model = v;
  @override Future<List<PendingUmlCommand>> loadPendingCommands() async => List.from(commands);
  @override Future<void> savePendingCommands(List<PendingUmlCommand> v) async => commands = List.from(v);
  @override Future<String?> loadParticipantId() async => participant;
  @override Future<void> saveParticipantId(String v) async => participant = v;
  @override Future<Map<String, String>> loadLocalIdMap() async => Map.from(mapping);
  @override Future<void> saveLocalIdMap(Map<String, String> v) async => mapping = Map.from(v);
}

// ─── Helpers ──────────────────────────────────────────────────────────────────
UmlModel emptyModel({int version = 0}) => UmlModel(
      projectId: 'p1',
      version: version,
      classes: [],
      relationships: [],
    );

UmlModel modelWithClass({int version = 1, String id = 'c1', String name = 'Cliente'}) =>
    UmlModel(
      projectId: 'p1',
      version: version,
      classes: [UmlClass(id: id, name: name, attributes: [], operations: [])],
      relationships: [],
    );

http.Response jsonResponse(Map<String, dynamic> body, {int status = 200}) =>
    http.Response(jsonEncode(body), status, headers: {'content-type': 'application/json'});

OfflineSyncService makeSyncService({
  required MemoryStore store,
  required http.Client client,
  String participantId = 'mobile-fixed',
  String baseUrl = 'http://test/api',
  String projectId = 'p1',
}) {
  final api = UmlApiService(
    client: client,
    baseUrl: baseUrl,
    projectId: projectId,
    participantId: participantId,
  );
  return OfflineSyncService(store: store, api: api, participantId: participantId);
}

void main() {
  // ══════════════════════════════════════════════════════════════════════════
  // GROUP 1 — PendingUmlCommand serialization
  // ══════════════════════════════════════════════════════════════════════════
  group('PendingUmlCommand serialization', () {
    test('round-trips all fields including status', () {
      final cmd = PendingUmlCommand(
        localId: 'l1',
        commandId: 'c1-uuid',
        type: 'CREATE_CLASS',
        payload: {'name': 'Factura', 'tempClassId': 'local:abc'},
        createdAt: DateTime.utc(2026, 1, 15, 10, 0, 0),
        baseVersion: 7,
        status: PendingCommandStatus.conflict,
      );
      final restored = PendingUmlCommand.fromJson(cmd.toJson());
      expect(restored.localId, 'l1');
      expect(restored.commandId, 'c1-uuid');
      expect(restored.type, 'CREATE_CLASS');
      expect(restored.payload['name'], 'Factura');
      expect(restored.baseVersion, 7);
      expect(restored.status, PendingCommandStatus.conflict);
      expect(restored.createdAt, cmd.createdAt);
    });

    test('defaults to pending status when missing from json', () {
      final json = {
        'localId': 'l2',
        'commandId': 'c2',
        'type': 'ADD_ATTRIBUTE',
        'payload': {'classRef': 'c1', 'name': 'total', 'type': 'Decimal'},
        'createdAt': '2026-01-15T10:00:00.000Z',
        'baseVersion': 3,
        // no 'status' key
      };
      final cmd = PendingUmlCommand.fromJson(json);
      expect(cmd.status, PendingCommandStatus.pending);
    });

    test('copyWith preserves unchanged fields', () {
      final original = PendingUmlCommand(
        localId: 'l3',
        commandId: 'c3',
        type: 'CREATE_CLASS',
        payload: {'name': 'X'},
        createdAt: DateTime.utc(2026),
        baseVersion: 0,
      );
      final updated = original.copyWith(status: PendingCommandStatus.failed);
      expect(updated.commandId, 'c3');
      expect(updated.status, PendingCommandStatus.failed);
      expect(updated.baseVersion, 0);
    });
  });

  // ══════════════════════════════════════════════════════════════════════════
  // GROUP 2 — Cache model save/load
  // ══════════════════════════════════════════════════════════════════════════
  group('Cache model save/load', () {
    test('saves and loads model from store', () async {
      final store = MemoryStore();
      final model = modelWithClass(version: 5);
      await store.saveCachedModel(model);
      final loaded = await store.loadCachedModel();
      expect(loaded?.version, 5);
      expect(loaded?.classes.single.name, 'Cliente');
    });

    test('returns null when no cached model', () async {
      final store = MemoryStore();
      expect(await store.loadCachedModel(), isNull);
    });

    test('refreshCache updates in-memory cache from server', () async {
      final store = MemoryStore();
      final client = MockClient((_) async => jsonResponse({
            'projectId': 'p1',
            'version': 10,
            'classes': [],
            'relationships': [],
          }));
      final svc = makeSyncService(store: store, client: client);
      await svc.initialize();
      final result = await svc.refreshCache();
      expect(result?.version, 10);
      expect(svc.cachedModel?.version, 10);
      final persisted = await store.loadCachedModel();
      expect(persisted?.version, 10);
    });

    test('refreshCache on transport error returns cached model', () async {
      final store = MemoryStore();
      await store.saveCachedModel(emptyModel(version: 3));
      final client = MockClient((_) async => throw const SocketException('network down'));
      final svc = makeSyncService(store: store, client: client);
      await svc.initialize();
      final result = await svc.refreshCache();
      expect(result?.version, 3);
      expect(svc.state, OfflineState.offline);
    });
  });

  // ══════════════════════════════════════════════════════════════════════════
  // GROUP 3 — ParticipantIdentityService
  // ══════════════════════════════════════════════════════════════════════════
  group('ParticipantIdentityService', () {
    test('creates and persists a new UUID-based participant id', () async {
      final store = MemoryStore();
      final svc = ParticipantIdentityService(store);
      final id = await svc.loadOrCreate();
      expect(id, startsWith('mobile-'));
      expect(id.length, greaterThan(10));
      // Verify persisted
      expect(await store.loadParticipantId(), id);
    });

    test('returns same id on subsequent calls (stable)', () async {
      final store = MemoryStore();
      final svc = ParticipantIdentityService(store);
      final id1 = await svc.loadOrCreate();
      final id2 = await svc.loadOrCreate();
      expect(id1, id2);
    });

    test('loads existing participant id without overwriting', () async {
      final store = MemoryStore();
      await store.saveParticipantId('mobile-existing-uuid');
      final svc = ParticipantIdentityService(store);
      final id = await svc.loadOrCreate();
      expect(id, 'mobile-existing-uuid');
    });
  });

  // ══════════════════════════════════════════════════════════════════════════
  // GROUP 4 — Stable commandId after reload (restart persistence)
  // ══════════════════════════════════════════════════════════════════════════
  group('Restart persistence', () {
    test('commandId and participantId survive service recreation', () async {
      final store = MemoryStore();
      final client = MockClient((_) async => throw const SocketException('offline'));
      final svc1 = makeSyncService(store: store, client: client);
      await svc1.initialize();
      final cmd = await svc1.enqueueCreateClass('Factura');
      final originalCommandId = cmd.commandId;

      // Simulate app restart → recreate service with same store
      final svc2 = makeSyncService(store: store, client: client);
      await svc2.initialize();

      expect(svc2.queue.single.commandId, originalCommandId);
      expect(svc2.participantId, 'mobile-fixed');
      expect(svc2.queue.single.status, PendingCommandStatus.pending);
    });

    test('queued add-attribute command survives restart', () async {
      final store = MemoryStore();
      final client = MockClient((_) async => throw const SocketException('offline'));
      final svc1 = makeSyncService(store: store, client: client);
      await svc1.initialize();
      // Use a known class reference
      final attrCmd = await svc1.enqueueAddAttribute('c1', 'total', 'Decimal');
      final attrCmdId = attrCmd.commandId;

      final svc2 = makeSyncService(store: store, client: client);
      await svc2.initialize();

      final restored = svc2.queue.firstWhere((c) => c.type == 'ADD_ATTRIBUTE');
      expect(restored.commandId, attrCmdId);
      expect(restored.payload['name'], 'total');
    });
  });

  // ══════════════════════════════════════════════════════════════════════════
  // GROUP 5 — Create class offline
  // ══════════════════════════════════════════════════════════════════════════
  group('Create class offline', () {
    test('enqueues CREATE_CLASS with UUID commandId and tempClassId', () async {
      final store = MemoryStore();
      final client = MockClient((_) async => throw const SocketException('offline'));
      final svc = makeSyncService(store: store, client: client);
      await svc.initialize();
      await store.saveCachedModel(emptyModel(version: 5));
      svc.cachedModel = emptyModel(version: 5);

      final cmd = await svc.enqueueCreateClass('Factura');

      expect(cmd.type, 'CREATE_CLASS');
      expect(cmd.payload['name'], 'Factura');
      expect(cmd.commandId, isNotEmpty);
      expect(cmd.commandId, isNot(contains('timestamp')));
      final tempId = cmd.payload['tempClassId'] as String;
      expect(tempId, startsWith('local:'));
      expect(cmd.baseVersion, 5);
      expect(svc.state, OfflineState.offline);
    });

    test('does not increment server modelVersion when offline', () async {
      final store = MemoryStore();
      final client = MockClient((_) async => throw const SocketException('offline'));
      final svc = makeSyncService(store: store, client: client);
      await svc.initialize();
      svc.cachedModel = emptyModel(version: 3);

      await svc.enqueueCreateClass('Orden');

      // Server version stays at 3 — only local queue grows
      expect(svc.cachedModel?.version, 3);
      expect(svc.queue.length, 1);
    });

    test('multiple enqueued classes accumulate in order', () async {
      final store = MemoryStore();
      final client = MockClient((_) async => throw const SocketException('offline'));
      final svc = makeSyncService(store: store, client: client);
      await svc.initialize();
      await svc.enqueueCreateClass('Clase1');
      await svc.enqueueCreateClass('Clase2');
      expect(svc.queue.length, 2);
      expect(svc.queue[0].payload['name'], 'Clase1');
      expect(svc.queue[1].payload['name'], 'Clase2');
    });
  });

  // ══════════════════════════════════════════════════════════════════════════
  // GROUP 6 — Add attribute offline
  // ══════════════════════════════════════════════════════════════════════════
  group('Add attribute offline', () {
    test('enqueues ADD_ATTRIBUTE with server classId reference', () async {
      final store = MemoryStore();
      final client = MockClient((_) async => throw const SocketException('offline'));
      final svc = makeSyncService(store: store, client: client);
      await svc.initialize();
      svc.cachedModel = modelWithClass(version: 2);

      final cmd = await svc.enqueueAddAttribute('c1', 'precio', 'Decimal');

      expect(cmd.type, 'ADD_ATTRIBUTE');
      expect(cmd.payload['classRef'], 'c1');
      expect(cmd.payload['name'], 'precio');
      expect(cmd.payload['type'], 'Decimal');
    });

    test('enqueues ADD_ATTRIBUTE with local class reference', () async {
      final store = MemoryStore();
      final client = MockClient((_) async => throw const SocketException('offline'));
      final svc = makeSyncService(store: store, client: client);
      await svc.initialize();

      // First create class offline, then add attribute referencing it
      final classCmd = await svc.enqueueCreateClass('Factura');
      final tempId = classCmd.payload['tempClassId'] as String;
      final attrCmd = await svc.enqueueAddAttribute(tempId, 'total', 'Decimal');

      expect(attrCmd.payload['classRef'], tempId);
      expect(svc.queue.length, 2);
    });
  });

  // ══════════════════════════════════════════════════════════════════════════
  // GROUP 7 — Sync empty queue
  // ══════════════════════════════════════════════════════════════════════════
  group('Sync empty queue', () {
    test('sync with empty queue just refreshes the cache', () async {
      final store = MemoryStore();
      final client = MockClient((_) async => jsonResponse({
            'projectId': 'p1',
            'version': 8,
            'classes': [],
            'relationships': [],
          }));
      final svc = makeSyncService(store: store, client: client);
      await svc.initialize();
      await svc.sync();
      expect(svc.state, OfflineState.online);
      expect(svc.cachedModel?.version, 8);
      expect(svc.queue, isEmpty);
    });
  });

  // ══════════════════════════════════════════════════════════════════════════
  // GROUP 8 — Sequential replay with correct expectedVersion
  // ══════════════════════════════════════════════════════════════════════════
  group('Sequential replay', () {
    test('expectedVersion increments N→N+1→N+2 across two commands', () async {
      final store = MemoryStore();
      final receivedVersions = <int>[];
      final receivedCommandIds = <String>[];

      var callCount = 0;
      final client = MockClient((request) async {
        if (request.method == 'GET') {
          return jsonResponse({
            'projectId': 'p1',
            'version': 10 + callCount,
            'classes': [],
            'relationships': [],
          });
        }
        final body = jsonDecode(request.body) as Map<String, dynamic>;
        receivedVersions.add(body['expectedVersion'] as int);
        receivedCommandIds.add(body['commandId'] as String);
        callCount++;
        return jsonResponse({'modelVersion': 10 + callCount, 'classId': 'server-$callCount'});
      });

      final svc = makeSyncService(store: store, client: client);
      await svc.initialize();
      svc.cachedModel = emptyModel(version: 10);

      final cmd1 = await svc.enqueueCreateClass('Clase1');
      final cmd2 = await svc.enqueueCreateClass('Clase2');
      await svc.sync();

      expect(receivedVersions[0], 10);
      expect(receivedVersions[1], 11);
      expect(receivedCommandIds[0], cmd1.commandId);
      expect(receivedCommandIds[1], cmd2.commandId);
      expect(svc.queue, isEmpty);
      expect(svc.state, OfflineState.online);
    });

    test('same commandId sent during replay (idempotence)', () async {
      final store = MemoryStore();
      final sentCommandIds = <String>[];

      final client = MockClient((request) async {
        if (request.method == 'POST') {
          final body = jsonDecode(request.body) as Map<String, dynamic>;
          sentCommandIds.add(body['commandId'] as String);
          return jsonResponse({'modelVersion': 5, 'classId': 'srv1'});
        }
        return jsonResponse({'projectId': 'p1', 'version': 5, 'classes': [], 'relationships': []});
      });

      final svc = makeSyncService(store: store, client: client);
      await svc.initialize();
      svc.cachedModel = emptyModel(version: 4);
      final original = await svc.enqueueCreateClass('Ordenes');
      await svc.sync();

      expect(sentCommandIds.single, original.commandId);
    });
  });

  // ══════════════════════════════════════════════════════════════════════════
  // GROUP 9 — Rebase from stale baseVersion
  // ══════════════════════════════════════════════════════════════════════════
  group('Rebase from stale baseVersion', () {
    test('sync starts from server version, not stale baseVersion', () async {
      final store = MemoryStore();
      final receivedVersions = <int>[];

      final client = MockClient((request) async {
        if (request.method == 'GET') {
          return jsonResponse({'projectId': 'p1', 'version': 15, 'classes': [], 'relationships': []});
        }
        final body = jsonDecode(request.body) as Map<String, dynamic>;
        receivedVersions.add(body['expectedVersion'] as int);
        return jsonResponse({'modelVersion': 16, 'classId': 'srv1'});
      });

      final svc = makeSyncService(store: store, client: client);
      await svc.initialize();
      // Command was queued when model was at version 10, but server is now at 15
      svc.cachedModel = emptyModel(version: 10);
      await svc.enqueueCreateClass('RebasedClass');
      await svc.sync();

      // Must use server version 15, not stale baseVersion 10
      expect(receivedVersions.single, 15);
    });
  });

  // ══════════════════════════════════════════════════════════════════════════
  // GROUP 10 — 409 stops replay
  // ══════════════════════════════════════════════════════════════════════════
  group('409 stops replay', () {
    test('409 on first command marks conflict and stops further commands', () async {
      final store = MemoryStore();
      var postCount = 0;

      final client = MockClient((request) async {
        if (request.method == 'GET') {
          return jsonResponse({'projectId': 'p1', 'version': 5, 'classes': [], 'relationships': []});
        }
        postCount++;
        return http.Response('conflict', 409);
      });

      final svc = makeSyncService(store: store, client: client);
      await svc.initialize();
      svc.cachedModel = emptyModel(version: 4);
      await svc.enqueueCreateClass('Clase1');
      await svc.enqueueCreateClass('Clase2');
      await svc.sync();

      expect(postCount, 1); // Only first command was attempted
      expect(svc.state, OfflineState.conflict);
      expect(svc.queue.length, 2); // Both commands preserved
      expect(svc.queue[0].status, PendingCommandStatus.conflict);
    });

    test('conflict message preserved in queue', () async {
      final store = MemoryStore();
      final client = MockClient((request) async {
        if (request.method == 'GET') {
          return jsonResponse({'projectId': 'p1', 'version': 5, 'classes': [], 'relationships': []});
        }
        return http.Response('conflict', 409);
      });

      final svc = makeSyncService(store: store, client: client);
      await svc.initialize();
      svc.cachedModel = emptyModel(version: 4);
      await svc.enqueueCreateClass('Conflicted');
      await svc.sync();

      final persisted = await store.loadPendingCommands();
      expect(persisted.single.status, PendingCommandStatus.conflict);
    });
  });

  // ══════════════════════════════════════════════════════════════════════════
  // GROUP 11 — Failed domain command stays queued
  // ══════════════════════════════════════════════════════════════════════════
  group('Failed domain command', () {
    test('HTTP 400 marks command as failed and stops replay', () async {
      final store = MemoryStore();
      final client = MockClient((request) async {
        if (request.method == 'GET') {
          return jsonResponse({'projectId': 'p1', 'version': 5, 'classes': [], 'relationships': []});
        }
        return http.Response('{"error":"Class name already exists"}', 400,
            headers: {'content-type': 'application/json'});
      });

      final svc = makeSyncService(store: store, client: client);
      await svc.initialize();
      svc.cachedModel = emptyModel(version: 4);
      await svc.enqueueCreateClass('DuplicateClass');
      await svc.sync();

      expect(svc.queue.single.status, PendingCommandStatus.failed);
      expect(svc.state, OfflineState.conflict);
    });
  });

  // ══════════════════════════════════════════════════════════════════════════
  // GROUP 12 — Successful commands removed from queue
  // ══════════════════════════════════════════════════════════════════════════
  group('Successful commands removed', () {
    test('successful commands are removed after sync', () async {
      final store = MemoryStore();
      final client = MockClient((request) async {
        if (request.method == 'GET') {
          return jsonResponse({'projectId': 'p1', 'version': 5, 'classes': [], 'relationships': []});
        }
        return jsonResponse({'modelVersion': 5, 'classId': 'c1'});
      });

      final svc = makeSyncService(store: store, client: client);
      await svc.initialize();
      svc.cachedModel = emptyModel(version: 4);
      await svc.enqueueCreateClass('Success');
      await svc.sync();

      expect(svc.queue, isEmpty);
      final persisted = await store.loadPendingCommands();
      expect(persisted, isEmpty);
    });
  });

  // ══════════════════════════════════════════════════════════════════════════
  // GROUP 13 — Local-to-server ID mapping
  // ══════════════════════════════════════════════════════════════════════════
  group('Local ID mapping', () {
    test('local temp class id mapped to server id after sync', () async {
      final store = MemoryStore();
      final client = MockClient((request) async {
        if (request.method == 'GET') {
          return jsonResponse({'projectId': 'p1', 'version': 6, 'classes': [], 'relationships': []});
        }
        return jsonResponse({'modelVersion': 6, 'classId': 'server-uuid-abc'});
      });

      final svc = makeSyncService(store: store, client: client);
      await svc.initialize();
      svc.cachedModel = emptyModel(version: 5);
      final classCmd = await svc.enqueueCreateClass('Factura');
      final tempId = classCmd.payload['tempClassId'] as String;
      await svc.sync();

      expect(svc.localToServerId[tempId], 'server-uuid-abc');
      final mapping = await store.loadLocalIdMap();
      expect(mapping[tempId], 'server-uuid-abc');
    });

    test('add-attribute referencing local class resolves during sync', () async {
      final store = MemoryStore();
      String? capturedClassId;
      var callCount = 0;

      final client = MockClient((request) async {
        if (request.method == 'GET') {
          return jsonResponse({'projectId': 'p1', 'version': 10 + callCount, 'classes': [], 'relationships': []});
        }
        callCount++;
        final url = request.url.toString();
        if (url.contains('/classes') && !url.contains('/attributes')) {
          return jsonResponse({'modelVersion': 11, 'classId': 'server-class-1'});
        }
        // Attribute creation — capture the classId in URL
        final segments = url.split('/');
        final classIdIndex = segments.indexOf('classes') + 1;
        if (classIdIndex > 0 && classIdIndex < segments.length) {
          capturedClassId = segments[classIdIndex];
        }
        return jsonResponse({'modelVersion': 12, 'classId': null});
      });

      final svc = makeSyncService(store: store, client: client);
      await svc.initialize();
      svc.cachedModel = emptyModel(version: 10);

      final classCmd = await svc.enqueueCreateClass('FacturaLocal');
      final tempId = classCmd.payload['tempClassId'] as String;
      await svc.enqueueAddAttribute(tempId, 'total', 'Decimal');
      await svc.sync();

      // The attribute request must use the server class ID, not the local temp ID
      expect(capturedClassId, 'server-class-1');
      expect(svc.queue, isEmpty);
    });
  });

  // ══════════════════════════════════════════════════════════════════════════
  // GROUP 14 — Final cache refresh after sync
  // ══════════════════════════════════════════════════════════════════════════
  group('Final cache refresh', () {
    test('cache is updated after successful sync', () async {
      final store = MemoryStore();
      var getCount = 0;

      final client = MockClient((request) async {
        if (request.method == 'GET') {
          getCount++;
          final version = getCount == 1 ? 5 : 6; // First GET for rebase, last for cache
          return jsonResponse({'projectId': 'p1', 'version': version, 'classes': [], 'relationships': []});
        }
        return jsonResponse({'modelVersion': 6, 'classId': 'c1'});
      });

      final svc = makeSyncService(store: store, client: client);
      await svc.initialize();
      svc.cachedModel = emptyModel(version: 4);
      await svc.enqueueCreateClass('AfterSync');
      await svc.sync();

      expect(svc.cachedModel?.version, 6);
      final persisted = await store.loadCachedModel();
      expect(persisted?.version, 6);
    });
  });

  // ══════════════════════════════════════════════════════════════════════════
  // GROUP 15 — STOMP version guard
  // ══════════════════════════════════════════════════════════════════════════
  group('STOMP version guard', () {
    test('stale events are ignored (version <= current)', () {
      var resyncs = 0;
      var applied = 0;
      final svc = UmlRealtimeService(
        onResyncRequired: () => resyncs++,
        onEvent: (_) => applied++,
      );
      svc.currentVersion = 10;
      svc.handleMessageForTest({'modelVersion': 9});
      svc.handleMessageForTest({'modelVersion': 10});
      expect(applied, 0);
      expect(resyncs, 0);
    });

    test('gap in version triggers resync', () {
      var resyncs = 0;
      final svc = UmlRealtimeService(onResyncRequired: () => resyncs++);
      svc.currentVersion = 5;
      svc.handleMessageForTest({'modelVersion': 8}); // Gap: expected 6
      expect(resyncs, 1);
    });

    test('consecutive event increments apply correctly', () {
      final events = <UmlRealtimeEvent>[];
      final svc = UmlRealtimeService(onEvent: events.add);
      svc.currentVersion = 3;
      svc.handleMessageForTest({'modelVersion': 4});
      svc.handleMessageForTest({'modelVersion': 5});
      expect(events.length, 2);
      expect(svc.currentVersion, 5);
    });
  });

  // ══════════════════════════════════════════════════════════════════════════
  // GROUP 16 — Transport error does NOT enqueue
  // ══════════════════════════════════════════════════════════════════════════
  group('Transport error detection', () {
    test('SocketException on sync sets state to offline but keeps queue', () async {
      final store = MemoryStore();
      final client = MockClient((request) async {
        if (request.method == 'GET') throw const SocketException('connection refused');
        return jsonResponse({'modelVersion': 5});
      });

      final svc = makeSyncService(store: store, client: client);
      await svc.initialize();
      svc.cachedModel = emptyModel(version: 4);
      await svc.enqueueCreateClass('PendingClass');
      await svc.sync();

      expect(svc.state, OfflineState.offline);
      expect(svc.queue.single.status, PendingCommandStatus.pending); // not marked as failed
    });

    test('TimeoutException on sync sets state to offline', () async {
      final store = MemoryStore();
      final client = MockClient((request) async {
        if (request.method == 'GET') throw TimeoutException('timed out');
        return jsonResponse({'modelVersion': 5});
      });

      final svc = makeSyncService(store: store, client: client);
      await svc.initialize();
      svc.cachedModel = emptyModel(version: 4);
      await svc.enqueueCreateClass('Pending');
      await svc.sync();

      expect(svc.state, OfflineState.offline);
      expect(svc.queue.single.status, PendingCommandStatus.pending);
    });
  });
}

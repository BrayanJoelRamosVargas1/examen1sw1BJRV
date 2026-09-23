import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/models/pending_uml_command.dart';
import 'package:mobile/models/uml_models.dart';
import 'package:mobile/services/offline_store.dart';
import 'package:mobile/services/offline_sync_service.dart';
import 'package:mobile/services/uml_api_service.dart';

class MemoryStore implements OfflineStore {
  UmlModel? model;
  List<PendingUmlCommand> commands = [];
  String? participant;
  Map<String, String> mapping = {};
  @override
  Future<UmlModel?> loadCachedModel() async => model;
  @override
  Future<void> saveCachedModel(UmlModel value) async => model = value;
  @override
  Future<List<PendingUmlCommand>> loadPendingCommands() async => commands;
  @override
  Future<void> savePendingCommands(List<PendingUmlCommand> value) async =>
      commands = value;
  @override
  Future<String?> loadParticipantId() async => participant;
  @override
  Future<void> saveParticipantId(String value) async => participant = value;
  @override
  Future<Map<String, String>> loadLocalIdMap() async => mapping;
  @override
  Future<void> saveLocalIdMap(Map<String, String> value) async =>
      mapping = value;
  @override
  Future<OfflineSyncState?> loadSyncState() async => OfflineSyncState(pendingCommands: commands, localIdMap: mapping);
  @override
  Future<void> saveSyncState(OfflineSyncState state) async {
    commands = state.pendingCommands;
    mapping = state.localIdMap;
  }
}

void main() {
  test('pending command round trips with stable UUIDs', () {
    final command = PendingUmlCommand(
      localId: 'l1',
      commandId: 'c1',
      type: 'CREATE_CLASS',
      payload: {'name': 'Factura'},
      createdAt: DateTime.utc(2026),
      baseVersion: 4,
    );
    final restored = PendingUmlCommand.fromJson(command.toJson());
    expect(restored.commandId, 'c1');
    expect(restored.baseVersion, 4);
  });

  test(
    'queue survives service recreation and keeps participant identity',
    () async {
      final store = MemoryStore();
      final api = UmlApiService(
        baseUrl: 'http://test/api',
        projectId: 'p1',
        participantId: 'mobile-fixed',
      );
      final first = OfflineSyncService(
        store: store,
        api: api,
        participantId: 'mobile-fixed',
      );
      await first.initialize();
      final pending = await first.enqueueCreateClass('Factura');
      final second = OfflineSyncService(
        store: store,
        api: api,
        participantId: 'mobile-fixed',
      );
      await second.initialize();
      expect(second.queue.single.commandId, pending.commandId);
      expect(second.participantId, 'mobile-fixed');
    },
  );
}

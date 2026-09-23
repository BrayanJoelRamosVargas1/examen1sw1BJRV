import 'dart:async';
import 'dart:io';
import 'package:http/http.dart' as http;
import 'package:uuid/uuid.dart';
import '../models/pending_uml_command.dart';
import '../models/uml_models.dart';
import '../config/app_config.dart';
import 'offline_store.dart';
import 'participant_identity_service.dart';
import 'uml_api_service.dart';

enum OfflineState { online, offline, syncing, conflict }

class OfflineSyncService {
  final OfflineStore store;
  final UmlApiService api;
  final String participantId;
  final Uuid uuid;
  List<PendingUmlCommand> queue = [];
  Map<String, String> localToServerId = {};
  OfflineState state = OfflineState.offline;
  UmlModel? cachedModel;

  OfflineSyncService({
    required this.store,
    required this.api,
    required this.participantId,
    Uuid? uuidGenerator,
  }) : uuid = uuidGenerator ?? const Uuid();

  static Future<OfflineSyncService> create({
    required OfflineStore store,
    String baseUrl = '',
    String projectId = '',
    http.Client? client,
  }) async {
    final participantId = await ParticipantIdentityService(
      store,
    ).loadOrCreate();
    final api = UmlApiService(
      client: client,
      participantId: participantId,
      baseUrl: baseUrl.isEmpty ? AppConfig.apiBaseUrl : baseUrl,
      projectId: projectId.isEmpty ? AppConfig.projectId : projectId,
    );
    final service = OfflineSyncService(
      store: store,
      api: api,
      participantId: participantId,
    );
    await service.initialize();
    return service;
  }

  Future<void> initialize() async {
    queue = await store.loadPendingCommands();
    localToServerId = await store.loadLocalIdMap();
    cachedModel = await store.loadCachedModel();
  }

  Future<UmlModel?> refreshCache() async {
    try {
      final model = await api.getModel();
      cachedModel = model;
      await store.saveCachedModel(model);
      state = OfflineState.online;
      return model;
    } on Object catch (error) {
      if (!_isTransportError(error)) rethrow;
      state = OfflineState.offline;
      return cachedModel;
    }
  }

  Future<PendingUmlCommand> enqueueCreateClass(String name) async {
    final localId = 'local:${uuid.v4()}';
    final command = PendingUmlCommand(
      localId: uuid.v4(),
      commandId: uuid.v4(),
      type: 'CREATE_CLASS',
      payload: {'name': name, 'tempClassId': localId},
      createdAt: DateTime.now().toUtc(),
      baseVersion: cachedModel?.version ?? 0,
    );
    queue = [...queue, command];
    await store.savePendingCommands(queue);
    state = OfflineState.offline;
    return command;
  }

  Future<PendingUmlCommand> enqueueAddAttribute(
    String classReference,
    String name,
    String type,
  ) async {
    final command = PendingUmlCommand(
      localId: uuid.v4(),
      commandId: uuid.v4(),
      type: 'ADD_ATTRIBUTE',
      payload: {'classRef': classReference, 'name': name, 'type': type},
      createdAt: DateTime.now().toUtc(),
      baseVersion: cachedModel?.version ?? 0,
    );
    queue = [...queue, command];
    await store.savePendingCommands(queue);
    state = OfflineState.offline;
    return command;
  }

  Future<void> sync() async {
    if (queue.isEmpty) {
      await refreshCache();
      return;
    }
    state = OfflineState.syncing;
    UmlModel serverModel;
    try {
      serverModel = await api.getModel();
    } on Object catch (error) {
      if (_isTransportError(error)) {
        state = OfflineState.offline;
        return;
      }
      rethrow;
    }
    cachedModel = serverModel;
    var serverVersion = serverModel.version;
    for (var index = 0; index < queue.length; index++) {
      final command = queue[index];
      try {
        final result = command.type == 'CREATE_CLASS'
            ? await api.executeCreateClass(
                command.commandId,
                command.payload['name'] as String,
                serverVersion,
              )
            : await api.executeAddAttribute(
                command.commandId,
                _resolveClass(command.payload['classRef'] as String),
                command.payload['name'] as String,
                command.payload['type'] as String,
                serverVersion,
              );
        serverVersion = result.modelVersion;
        final tempClassId = command.payload['tempClassId'] as String?;
        if (tempClassId != null && result.classId != null) {
          localToServerId[tempClassId] = result.classId!;
        }
        queue.removeAt(index--);
        await store.saveSyncState(OfflineSyncState(pendingCommands: queue, localIdMap: localToServerId));
      } on UmlApiException catch (error) {
        queue[index] = command.copyWith(
          status: error.statusCode == 409
              ? PendingCommandStatus.conflict
              : PendingCommandStatus.failed,
        );
        await store.saveSyncState(OfflineSyncState(pendingCommands: queue, localIdMap: localToServerId));
        final conflictState = error.statusCode == 409
            ? OfflineState.conflict
            : OfflineState.conflict;
        await refreshCache();
        state = conflictState; // Restore after refreshCache which sets state=online on success
        return;
      } on Object catch (error) {
        if (_isTransportError(error)) {
          state = OfflineState.offline;
          return;
        }
        rethrow;
      }
    }
    await refreshCache();
    state = OfflineState.online;
  }

  String _resolveClass(String reference) =>
      localToServerId[reference] ?? reference;

  bool _isTransportError(Object error) =>
      error is SocketException ||
      error is TimeoutException ||
      error is http.ClientException;
}

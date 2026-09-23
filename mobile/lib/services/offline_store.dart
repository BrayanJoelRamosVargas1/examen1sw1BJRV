import 'dart:convert';
import 'package:shared_preferences/shared_preferences.dart';
import '../models/pending_uml_command.dart';
import '../models/uml_models.dart';

abstract class OfflineStore {
  Future<UmlModel?> loadCachedModel();
  Future<void> saveCachedModel(UmlModel model);
  Future<List<PendingUmlCommand>> loadPendingCommands();
  Future<void> savePendingCommands(List<PendingUmlCommand> commands);
  Future<String?> loadParticipantId();
  Future<void> saveParticipantId(String participantId);
  Future<Map<String, String>> loadLocalIdMap();
  Future<void> saveLocalIdMap(Map<String, String> mapping);
}

class SharedPreferencesOfflineStore implements OfflineStore {
  final SharedPreferences preferences;
  static const schemaVersion = 1;
  SharedPreferencesOfflineStore(this.preferences);

  @override
  Future<UmlModel?> loadCachedModel() async {
    final raw = preferences.getString('offline.cachedModel');
    if (raw == null) return null;
    final value = jsonDecode(raw) as Map<String, dynamic>;
    if (value['schemaVersion'] != schemaVersion) return null;
    return UmlModel.fromJson(value['model'] as Map<String, dynamic>);
  }

  @override
  Future<void> saveCachedModel(UmlModel model) async => preferences.setString(
    'offline.cachedModel',
    jsonEncode({'schemaVersion': schemaVersion, 'model': _modelJson(model)}),
  );

  @override
  Future<List<PendingUmlCommand>> loadPendingCommands() async {
    final raw = preferences.getString('offline.pendingCommands');
    if (raw == null) return [];
    return (jsonDecode(raw) as List)
        .map(
          (item) => PendingUmlCommand.fromJson(
            Map<String, dynamic>.from(item as Map),
          ),
        )
        .toList();
  }

  @override
  Future<void> savePendingCommands(List<PendingUmlCommand> commands) async =>
      preferences.setString(
        'offline.pendingCommands',
        jsonEncode(commands.map((command) => command.toJson()).toList()),
      );

  @override
  Future<String?> loadParticipantId() async =>
      preferences.getString('offline.participantId');
  @override
  Future<void> saveParticipantId(String participantId) async =>
      preferences.setString('offline.participantId', participantId);

  @override
  Future<Map<String, String>> loadLocalIdMap() async {
    final raw = preferences.getString('offline.localIdMap');
    return raw == null ? {} : Map<String, String>.from(jsonDecode(raw) as Map);
  }

  @override
  Future<void> saveLocalIdMap(Map<String, String> mapping) async =>
      preferences.setString('offline.localIdMap', jsonEncode(mapping));

  Map<String, dynamic> _modelJson(UmlModel model) => {
    'projectId': model.projectId,
    'version': model.version,
    'classes': model.classes
        .map(
          (umlClass) => {
            'id': umlClass.id,
            'name': umlClass.name,
            'attributes': umlClass.attributes
                .map(
                  (attribute) => {
                    'id': attribute.id,
                    'name': attribute.name,
                    'type': attribute.type,
                    'visibility': attribute.visibility,
                  },
                )
                .toList(),
            'operations': umlClass.operations
                .map(
                  (operation) => {
                    'id': operation.id,
                    'name': operation.name,
                    'returnType': operation.returnType,
                  },
                )
                .toList(),
          },
        )
        .toList(),
    'relationships': model.relationships
        .map(
          (relationship) => {
            'type': relationship.type,
            'sourceClassId': relationship.sourceClassId,
            'targetClassId': relationship.targetClassId,
            'sourceMultiplicity': relationship.sourceMultiplicity,
            'targetMultiplicity': relationship.targetMultiplicity,
          },
        )
        .toList(),
  };
}

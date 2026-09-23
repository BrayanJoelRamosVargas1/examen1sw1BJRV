import 'dart:convert';
import 'package:http/http.dart' as http;
import 'package:uuid/uuid.dart';
import '../config/app_config.dart';
import '../models/uml_models.dart';

class UmlApiException implements Exception {
  final int statusCode;
  final String message;
  const UmlApiException(this.statusCode, this.message);
}

class MutationResult {
  final int modelVersion;
  final String? classId;
  const MutationResult({required this.modelVersion, this.classId});
}

class UmlApiService {
  final http.Client client;
  final String baseUrl;
  final String projectId;
  final String participantId;

  UmlApiService({
    http.Client? client,
    this.baseUrl = AppConfig.apiBaseUrl,
    this.projectId = AppConfig.projectId,
    String? participantId,
  }) : client = client ?? http.Client(),
       participantId =
           participantId ?? 'mobile-${DateTime.now().millisecondsSinceEpoch}';

  Future<UmlModel> getModel() async {
    final response = await client.get(
      Uri.parse('$baseUrl/projects/$projectId/model'),
    );
    return _decodeModel(response);
  }

  Future<UmlModel> createClass(String name, int expectedVersion) async {
    await executeCreateClass(_commandId(), name, expectedVersion);
    return getModel();
  }

  Future<MutationResult> executeCreateClass(
    String commandId,
    String name,
    int expectedVersion,
  ) async {
    final response = await client.post(
      Uri.parse('$baseUrl/projects/$projectId/classes'),
      headers: {'Content-Type': 'application/json'},
      body: jsonEncode({
        'commandId': commandId,
        'participantId': participantId,
        'expectedVersion': expectedVersion,
        'name': name,
      }),
    );
    return _decodeMutation(response);
  }

  Future<UmlModel> addAttribute(
    String classId,
    String name,
    String type,
    int expectedVersion,
  ) async {
    await executeAddAttribute(
      _commandId(),
      classId,
      name,
      type,
      expectedVersion,
    );
    return getModel();
  }

  Future<MutationResult> executeAddAttribute(
    String commandId,
    String classId,
    String name,
    String type,
    int expectedVersion,
  ) async {
    final response = await client.post(
      Uri.parse('$baseUrl/projects/$projectId/classes/$classId/attributes'),
      headers: {'Content-Type': 'application/json'},
      body: jsonEncode({
        'commandId': commandId,
        'participantId': participantId,
        'expectedVersion': expectedVersion,
        'attributeName': name,
        'attributeType': type,
        'visibility': 'PRIVATE',
      }),
    );
    return _decodeMutation(response);
  }

  UmlModel _decodeModel(http.Response response) {
    if (response.statusCode != 200) {
      throw UmlApiException(response.statusCode, response.body);
    }
    return UmlModel.fromJson(jsonDecode(response.body) as Map<String, dynamic>);
  }

  MutationResult _decodeMutation(http.Response response) {
    if (response.statusCode == 409) {
      throw const UmlApiException(409, 'El modelo cambió en otro cliente.');
    }
    if (response.statusCode < 200 || response.statusCode >= 300) {
      throw UmlApiException(response.statusCode, response.body);
    }
    final json = jsonDecode(response.body) as Map<String, dynamic>;
    return MutationResult(
      modelVersion: (json['modelVersion'] as num).toInt(),
      classId: json['classId'] as String?,
    );
  }

  String _commandId() => const Uuid().v4();
}

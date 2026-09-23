import 'dart:convert';
import 'package:http/http.dart' as http;
import '../config/app_config.dart';
import '../models/uml_models.dart';

class UmlApiException implements Exception {
  final int statusCode;
  final String message;
  const UmlApiException(this.statusCode, this.message);
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
    final response = await client.post(
      Uri.parse('$baseUrl/projects/$projectId/classes'),
      headers: {'Content-Type': 'application/json'},
      body: jsonEncode({
        'commandId': _commandId(),
        'participantId': participantId,
        'expectedVersion': expectedVersion,
        'name': name,
      }),
    );
    return _mutationResult(response);
  }

  Future<UmlModel> addAttribute(
    String classId,
    String name,
    String type,
    int expectedVersion,
  ) async {
    final response = await client.post(
      Uri.parse('$baseUrl/projects/$projectId/classes/$classId/attributes'),
      headers: {'Content-Type': 'application/json'},
      body: jsonEncode({
        'commandId': _commandId(),
        'participantId': participantId,
        'expectedVersion': expectedVersion,
        'attributeName': name,
        'attributeType': type,
        'visibility': 'PRIVATE',
      }),
    );
    return _mutationResult(response);
  }

  UmlModel _decodeModel(http.Response response) {
    if (response.statusCode != 200)
      throw UmlApiException(response.statusCode, response.body);
    return UmlModel.fromJson(jsonDecode(response.body) as Map<String, dynamic>);
  }

  Future<UmlModel> _mutationResult(http.Response response) async {
    if (response.statusCode == 409)
      throw const UmlApiException(409, 'El modelo cambió en otro cliente.');
    if (response.statusCode < 200 || response.statusCode >= 300)
      throw UmlApiException(response.statusCode, response.body);
    return getModel();
  }

  String _commandId() => DateTime.now().microsecondsSinceEpoch.toString();
}

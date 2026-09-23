class AppConfig {
  static const apiBaseUrl = String.fromEnvironment(
    'API_BASE_URL',
    defaultValue: 'http://10.0.2.2:8080/api',
  );
  static const projectId = String.fromEnvironment(
    'PROJECT_ID',
    defaultValue: '00000000-0000-0000-0000-000000000001',
  );
}

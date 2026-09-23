import 'package:flutter/material.dart';
import 'config/app_config.dart';
import 'models/uml_models.dart';
import 'services/uml_api_service.dart';

void main() => runApp(const UmlMobileApp());

class UmlMobileApp extends StatelessWidget {
  const UmlMobileApp({super.key});
  @override
  Widget build(BuildContext context) => MaterialApp(
    title: 'UML CASE Mobile',
    theme: ThemeData(
      colorScheme: ColorScheme.fromSeed(seedColor: Colors.indigo),
      useMaterial3: true,
    ),
    home: const ModelScreen(),
  );
}

class ModelScreen extends StatefulWidget {
  const ModelScreen({super.key});
  @override
  State<ModelScreen> createState() => _ModelScreenState();
}

class _ModelScreenState extends State<ModelScreen> {
  final UmlApiService api = UmlApiService();
  UmlModel? model;
  String status = 'Conectando...';
  String? error;

  @override
  void initState() {
    super.initState();
    _loadModel();
  }

  Future<void> _loadModel() async {
    setState(() {
      status = 'Sincronizando...';
      error = null;
    });
    try {
      final loaded = await api.getModel();
      if (!mounted) return;
      setState(() {
        model = loaded;
        status = 'Conectado';
      });
    } on UmlApiException catch (exception) {
      if (!mounted) return;
      setState(() {
        status = 'Desconectado';
        error = 'HTTP ${exception.statusCode}: ${exception.message}';
      });
    } catch (exception) {
      if (!mounted) return;
      setState(() {
        status = 'Desconectado';
        error = exception.toString();
      });
    }
  }

  Future<void> _createClass() async {
    final name = await _ask('Nueva clase', 'Nombre');
    if (name == null || name.trim().isEmpty || model == null) return;
    try {
      await api.createClass(name.trim(), model!.version);
      await _loadModel();
    } on UmlApiException catch (exception) {
      if (exception.statusCode == 409) {
        await _conflictResync();
      } else {
        setState(() => error = exception.message);
      }
    }
  }

  Future<void> _addAttribute(UmlClass umlClass) async {
    final name = await _ask('Nuevo atributo', 'Nombre');
    if (name == null || name.trim().isEmpty || model == null) return;
    final type = await _ask('Tipo', 'String, Integer, Decimal o Boolean');
    if (type == null || type.trim().isEmpty) return;
    try {
      await api.addAttribute(
        umlClass.id,
        name.trim(),
        type.trim(),
        model!.version,
      );
      await _loadModel();
    } on UmlApiException catch (exception) {
      if (exception.statusCode == 409) {
        await _conflictResync();
      } else {
        setState(() => error = exception.message);
      }
    }
  }

  Future<void> _conflictResync() async {
    setState(
      () => status =
          'El modelo cambió en otro cliente. Se sincronizó la versión más reciente.',
    );
    await _loadModel();
  }

  Future<String?> _ask(String title, String label) async {
    final controller = TextEditingController();
    return showDialog<String>(
      context: context,
      builder: (context) => AlertDialog(
        title: Text(title),
        content: TextField(
          controller: controller,
          decoration: InputDecoration(labelText: label),
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context),
            child: const Text('Cancelar'),
          ),
          FilledButton(
            onPressed: () => Navigator.pop(context, controller.text),
            child: const Text('Aceptar'),
          ),
        ],
      ),
    );
  }

  @override
  Widget build(BuildContext context) => Scaffold(
    appBar: AppBar(
      title: const Text('UML CASE Mobile'),
      actions: [
        IconButton(onPressed: _loadModel, icon: const Icon(Icons.sync)),
      ],
    ),
    floatingActionButton: FloatingActionButton.extended(
      onPressed: _createClass,
      icon: const Icon(Icons.add),
      label: const Text('Clase'),
    ),
    body: RefreshIndicator(
      onRefresh: _loadModel,
      child: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          Text(
            'Proyecto: ${AppConfig.projectId}',
            style: Theme.of(context).textTheme.bodySmall,
          ),
          Text('Version: ${model?.version ?? '-'}  -  $status'),
          if (error != null)
            Padding(
              padding: const EdgeInsets.symmetric(vertical: 8),
              child: Text(
                error!,
                style: TextStyle(color: Theme.of(context).colorScheme.error),
              ),
            ),
          const SizedBox(height: 12),
          if (model == null)
            const Center(child: CircularProgressIndicator())
          else
            ...model!.classes.map(
              (umlClass) => Card(
                child: Padding(
                  padding: const EdgeInsets.all(12),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Row(
                        children: [
                          Expanded(
                            child: Text(
                              'class ${umlClass.name}',
                              style: Theme.of(context).textTheme.titleMedium,
                            ),
                          ),
                          IconButton(
                            onPressed: () => _addAttribute(umlClass),
                            icon: const Icon(Icons.playlist_add),
                          ),
                        ],
                      ),
                      const Divider(),
                      ...umlClass.attributes.map(
                        (attribute) =>
                            Text('${attribute.name}: ${attribute.type}'),
                      ),
                      ...umlClass.operations.map(
                        (operation) => Text(
                          '${operation.name}(): ${operation.returnType}',
                        ),
                      ),
                    ],
                  ),
                ),
              ),
            ),
          if (model != null && model!.relationships.isNotEmpty) ...[
            const SizedBox(height: 12),
            const Text(
              'Relaciones',
              style: TextStyle(fontWeight: FontWeight.bold),
            ),
            ...model!.relationships.map(
              (relationship) => Text(
                '${relationship.type} ${relationship.sourceMultiplicity} -> ${relationship.targetMultiplicity}',
              ),
            ),
          ],
        ],
      ),
    ),
  );
}

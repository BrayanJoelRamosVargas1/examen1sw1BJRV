import 'package:flutter/material.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'config/app_config.dart';
import 'models/pending_uml_command.dart';
import 'models/uml_models.dart';
import 'services/offline_store.dart';
import 'services/offline_sync_service.dart';
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

// ─── Status badge ─────────────────────────────────────────────────────────────
class _StatusBadge extends StatelessWidget {
  final OfflineState state;
  final int pendingCount;
  const _StatusBadge({required this.state, required this.pendingCount});

  @override
  Widget build(BuildContext context) {
    final (icon, color, label) = switch (state) {
      OfflineState.online => ('🟢', Colors.green, 'Online'),
      OfflineState.syncing => ('🟠', Colors.orange, 'Sincronizando...'),
      OfflineState.offline => (
          '🔴',
          Colors.red,
          pendingCount > 0 ? 'Offline — $pendingCount cambios pendientes' : 'Offline',
        ),
      OfflineState.conflict => (
          '⚠',
          Colors.deepOrange,
          'Conflicto — $pendingCount pendientes',
        ),
    };
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
      decoration: BoxDecoration(
        color: color.withValues(alpha: 0.12),
        borderRadius: BorderRadius.circular(20),
        border: Border.all(color: color.withValues(alpha: 0.4)),
      ),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          Text(icon, style: const TextStyle(fontSize: 12)),
          const SizedBox(width: 4),
          Text(label, style: TextStyle(fontSize: 12, color: color, fontWeight: FontWeight.w600)),
        ],
      ),
    );
  }
}

// ─── Pending queue drawer ─────────────────────────────────────────────────────
class _PendingQueueCard extends StatelessWidget {
  final List<PendingUmlCommand> queue;
  final Map<String, String> localIdMap;
  const _PendingQueueCard({required this.queue, required this.localIdMap});

  String _describe(PendingUmlCommand cmd) {
    if (cmd.type == 'CREATE_CLASS') {
      return 'Crear clase "${cmd.payload['name']}"';
    }
    if (cmd.type == 'ADD_ATTRIBUTE') {
      final ref = cmd.payload['classRef'] as String;
      final resolvedRef = localIdMap[ref] ?? ref;
      final name = cmd.payload['name'];
      final type = cmd.payload['type'];
      return 'Agregar $name:$type a $resolvedRef';
    }
    return cmd.type;
  }

  Color _statusColor(PendingCommandStatus s) => switch (s) {
        PendingCommandStatus.pending => Colors.blue,
        PendingCommandStatus.syncing => Colors.orange,
        PendingCommandStatus.conflict => Colors.deepOrange,
        PendingCommandStatus.failed => Colors.red,
      };

  String _statusLabel(PendingCommandStatus s) => switch (s) {
        PendingCommandStatus.pending => 'pendiente',
        PendingCommandStatus.syncing => 'sincronizando',
        PendingCommandStatus.conflict => 'conflicto',
        PendingCommandStatus.failed => 'fallido',
      };

  @override
  Widget build(BuildContext context) {
    if (queue.isEmpty) return const SizedBox.shrink();
    return Card(
      margin: const EdgeInsets.only(bottom: 12),
      color: Colors.orange.shade50,
      child: Padding(
        padding: const EdgeInsets.all(12),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              'Cambios pendientes (${queue.length})',
              style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 13),
            ),
            const SizedBox(height: 8),
            ...queue.map(
              (cmd) => Padding(
                padding: const EdgeInsets.symmetric(vertical: 2),
                child: Row(
                  children: [
                    Expanded(child: Text('• ${_describe(cmd)}', style: const TextStyle(fontSize: 13))),
                    Container(
                      padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 2),
                      decoration: BoxDecoration(
                        color: _statusColor(cmd.status).withValues(alpha: 0.15),
                        borderRadius: BorderRadius.circular(8),
                      ),
                      child: Text(
                        _statusLabel(cmd.status),
                        style: TextStyle(fontSize: 11, color: _statusColor(cmd.status)),
                      ),
                    ),
                  ],
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }
}

// ─── Main screen ──────────────────────────────────────────────────────────────
class ModelScreen extends StatefulWidget {
  const ModelScreen({super.key});
  @override
  State<ModelScreen> createState() => _ModelScreenState();
}

class _ModelScreenState extends State<ModelScreen> {
  OfflineSyncService? _svc;
  UmlModel? _model;
  String? _error;
  bool _initializing = true;

  @override
  void initState() {
    super.initState();
    _initialize();
  }

  Future<void> _initialize() async {
    final prefs = await SharedPreferences.getInstance();
    final store = SharedPreferencesOfflineStore(prefs);
    _svc = await OfflineSyncService.create(store: store);
    if (!mounted) return;
    setState(() => _initializing = false);
    await _refreshModel();
  }

  Future<void> _refreshModel() async {
    if (_svc == null) return;
    setState(() => _error = null);
    try {
      final result = await _svc!.refreshCache();
      if (!mounted) return;
      setState(() => _model = result ?? _svc!.cachedModel);
    } catch (e) {
      if (!mounted) return;
      setState(() {
        _error = e.toString();
        _model = _svc!.cachedModel;
      });
    }
  }

  Future<void> _sync() async {
    if (_svc == null) return;
    setState(() {});
    await _svc!.sync();
    if (!mounted) return;
    setState(() => _model = _svc!.cachedModel);
  }

  Future<void> _createClass() async {
    final name = await _ask('Nueva clase', 'Nombre');
    if (name == null || name.trim().isEmpty) return;

    final svc = _svc!;
    if (svc.state == OfflineState.offline || svc.state == OfflineState.conflict) {
      // Queue offline
      await svc.enqueueCreateClass(name.trim());
      if (!mounted) return;
      setState(() {});
      _showSnack('Clase "${name.trim()}" en cola — se enviará al reconectar.');
      return;
    }

    try {
      final model = _model;
      if (model == null) return;
      await svc.api.createClass(name.trim(), model.version);
      await _refreshModel();
    } on UmlApiException catch (e) {
      if (e.statusCode == 409) {
        await _refreshModel();
        _showSnack('Conflicto resuelto. Inténtalo de nuevo.');
      } else {
        setState(() => _error = 'HTTP ${e.statusCode}: ${e.message}');
      }
    } catch (e) {
      // Transport error — enqueue instead
      await svc.enqueueCreateClass(name.trim());
      if (!mounted) return;
      setState(() {});
      _showSnack('Sin conexión. Clase "${name.trim()}" en cola.');
    }
  }

  Future<void> _addAttribute(UmlClass umlClass) async {
    final svc = _svc!;

    // Check if this class is a local-only (pending) class
    final isLocalClass = umlClass.id.startsWith('local:');

    if (isLocalClass && (svc.state == OfflineState.online || svc.state == OfflineState.syncing)) {
      _showSnack('Esta clase aún no se ha sincronizado. Conéctate primero.');
      return;
    }

    final name = await _ask('Nuevo atributo', 'Nombre');
    if (name == null || name.trim().isEmpty) return;
    final type = await _ask('Tipo', 'String, Integer, Decimal o Boolean');
    if (type == null || type.trim().isEmpty) return;

    final classRef = isLocalClass ? umlClass.id : umlClass.id;

    if (svc.state == OfflineState.offline || svc.state == OfflineState.conflict || isLocalClass) {
      await svc.enqueueAddAttribute(classRef, name.trim(), type.trim());
      if (!mounted) return;
      setState(() {});
      _showSnack('Atributo "${name.trim()}" en cola.');
      return;
    }

    try {
      final model = _model;
      if (model == null) return;
      await svc.api.addAttribute(umlClass.id, name.trim(), type.trim(), model.version);
      await _refreshModel();
    } on UmlApiException catch (e) {
      if (e.statusCode == 409) {
        await _refreshModel();
        _showSnack('Conflicto resuelto. Inténtalo de nuevo.');
      } else {
        setState(() => _error = 'HTTP ${e.statusCode}: ${e.message}');
      }
    } catch (e) {
      await svc.enqueueAddAttribute(classRef, name.trim(), type.trim());
      if (!mounted) return;
      setState(() {});
      _showSnack('Sin conexión. Atributo en cola.');
    }
  }

  Future<String?> _ask(String title, String label) async {
    final controller = TextEditingController();
    return showDialog<String>(
      context: context,
      builder: (ctx) => AlertDialog(
        title: Text(title),
        content: TextField(
          controller: controller,
          decoration: InputDecoration(labelText: label),
          autofocus: true,
        ),
        actions: [
          TextButton(onPressed: () => Navigator.pop(ctx), child: const Text('Cancelar')),
          FilledButton(
            onPressed: () => Navigator.pop(ctx, controller.text),
            child: const Text('Aceptar'),
          ),
        ],
      ),
    );
  }

  void _showSnack(String msg) {
    if (!mounted) return;
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(content: Text(msg), behavior: SnackBarBehavior.floating),
    );
  }

  Widget _buildOfflineClassCard(PendingUmlCommand cmd, BuildContext context) {
    final name = cmd.payload['name'] as String;
    final tempId = cmd.payload['tempClassId'] as String? ?? '';
    // Show pending class as a virtual class card
    final virtualClass = UmlClass(id: tempId, name: name, attributes: [], operations: []);
    return _buildClassCard(virtualClass, context, isPending: true);
  }

  Widget _buildClassCard(UmlClass umlClass, BuildContext context, {bool isPending = false}) {
    // Find pending attributes for this class
    final pendingAttrs = _svc?.queue
            .where(
              (c) => c.type == 'ADD_ATTRIBUTE' && c.payload['classRef'] == umlClass.id,
            )
            .toList() ??
        [];

    return Card(
      margin: const EdgeInsets.only(bottom: 8),
      color: isPending ? Colors.blue.shade50 : null,
      child: Padding(
        padding: const EdgeInsets.all(12),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                Expanded(
                  child: Row(
                    children: [
                      Text(
                        'class ${umlClass.name}',
                        style: Theme.of(context).textTheme.titleMedium,
                      ),
                      if (isPending) ...[
                        const SizedBox(width: 8),
                        Container(
                          padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 2),
                          decoration: BoxDecoration(
                            color: Colors.blue.withAlpha((255 * 0.15).round()),
                            borderRadius: BorderRadius.circular(8),
                          ),
                          child: const Text('pendiente', style: TextStyle(fontSize: 11, color: Colors.blue)),
                        ),
                      ],
                    ],
                  ),
                ),
                if (!isPending)
                  IconButton(
                    onPressed: () => _addAttribute(umlClass),
                    icon: const Icon(Icons.playlist_add),
                    tooltip: 'Añadir atributo',
                  )
                else
                  Tooltip(
                    message: 'Esta acción requiere conexión.',
                    child: Icon(Icons.lock_outline, size: 18, color: Colors.grey.shade400),
                  ),
              ],
            ),
            if (umlClass.attributes.isNotEmpty || pendingAttrs.isNotEmpty) const Divider(),
            ...umlClass.attributes.map(
              (attr) => Text('${attr.name}: ${attr.type}', style: const TextStyle(fontSize: 13)),
            ),
            // Pending attributes (queued offline)
            ...pendingAttrs.map(
              (cmd) => Row(
                children: [
                  Expanded(
                    child: Text(
                      '${cmd.payload['name']}: ${cmd.payload['type']}',
                      style: TextStyle(fontSize: 13, color: Colors.blue.shade700),
                    ),
                  ),
                  Container(
                    padding: const EdgeInsets.symmetric(horizontal: 4, vertical: 1),
                    decoration: BoxDecoration(
                      color: Colors.blue.shade50,
                      borderRadius: BorderRadius.circular(6),
                    ),
                    child: const Text('pendiente', style: TextStyle(fontSize: 10, color: Colors.blue)),
                  ),
                ],
              ),
            ),
            ...umlClass.operations.map(
              (op) => Text('${op.name}(): ${op.returnType}', style: const TextStyle(fontSize: 13)),
            ),
          ],
        ),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final svc = _svc;
    final queue = svc?.queue ?? [];
    final state = svc?.state ?? OfflineState.offline;
    final pendingCreateClasses = queue.where((c) => c.type == 'CREATE_CLASS').toList();

    return Scaffold(
      appBar: AppBar(
        title: const Text('UML CASE Mobile'),
        actions: [
          if (svc != null)
            IconButton(
              onPressed: _sync,
              icon: const Icon(Icons.sync),
              tooltip: 'Sincronizar ahora',
            ),
          IconButton(
            onPressed: _refreshModel,
            icon: const Icon(Icons.refresh),
            tooltip: 'Recargar modelo',
          ),
        ],
      ),
      floatingActionButton: FloatingActionButton.extended(
        onPressed: _createClass,
        icon: const Icon(Icons.add),
        label: const Text('Clase'),
      ),
      body: _initializing
          ? const Center(child: CircularProgressIndicator())
          : RefreshIndicator(
              onRefresh: _refreshModel,
              child: ListView(
                padding: const EdgeInsets.all(16),
                children: [
                  // Status header
                  Row(
                    children: [
                      Text(
                        'Proyecto: ${AppConfig.projectId}',
                        style: Theme.of(context).textTheme.bodySmall,
                      ),
                      const Spacer(),
                      _StatusBadge(state: state, pendingCount: queue.length),
                    ],
                  ),
                  const SizedBox(height: 4),
                  Text(
                    'Versión servidor: ${_model?.version ?? '-'}',
                    style: Theme.of(context).textTheme.bodySmall,
                  ),

                  if (_error != null)
                    Padding(
                      padding: const EdgeInsets.symmetric(vertical: 8),
                      child: Text(
                        _error!,
                        style: TextStyle(color: Theme.of(context).colorScheme.error),
                      ),
                    ),

                  if (state == OfflineState.conflict)
                    Padding(
                      padding: const EdgeInsets.symmetric(vertical: 8),
                      child: Card(
                        color: Colors.deepOrange.shade50,
                        child: Padding(
                          padding: const EdgeInsets.all(12),
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              const Text(
                                '⚠ El modelo cambió durante la sincronización.',
                                style: TextStyle(fontWeight: FontWeight.bold, fontSize: 13),
                              ),
                              const Text(
                                'Tus cambios pendientes se conservaron.',
                                style: TextStyle(fontSize: 12),
                              ),
                              const SizedBox(height: 8),
                              FilledButton.tonal(
                                onPressed: _sync,
                                child: const Text('Reintentar sincronización'),
                              ),
                            ],
                          ),
                        ),
                      ),
                    ),

                  if (_model == null && state != OfflineState.online)
                    const Padding(
                      padding: EdgeInsets.symmetric(vertical: 24),
                      child: Center(
                        child: Text(
                          'Sin conexión y todavía no existe\nuna copia local del modelo.',
                          textAlign: TextAlign.center,
                          style: TextStyle(color: Colors.grey),
                        ),
                      ),
                    ),

                  const SizedBox(height: 12),

                  // Pending queue card
                  _PendingQueueCard(
                    queue: queue,
                    localIdMap: svc?.localToServerId ?? {},
                  ),

                  // Server classes
                  ...?_model?.classes.map((cls) => _buildClassCard(cls, context)),

                  // Pending (offline-only) classes that don't exist on server yet
                  ...pendingCreateClasses.map((cmd) => _buildOfflineClassCard(cmd, context)),

                  // Relationships
                  if (_model != null && _model!.relationships.isNotEmpty) ...[
                    const SizedBox(height: 12),
                    const Text('Relaciones', style: TextStyle(fontWeight: FontWeight.bold)),
                    ..._model!.relationships.map(
                      (rel) => Text(
                        '${rel.type}  ${rel.sourceMultiplicity} → ${rel.targetMultiplicity}',
                      ),
                    ),
                  ],

                  const SizedBox(height: 80), // FAB padding
                ],
              ),
            ),
    );
  }
}

enum PendingCommandStatus { pending, syncing, conflict, failed }

class PendingUmlCommand {
  final String localId;
  final String commandId;
  final String type;
  final Map<String, dynamic> payload;
  final DateTime createdAt;
  final int baseVersion;
  final PendingCommandStatus status;

  const PendingUmlCommand({
    required this.localId,
    required this.commandId,
    required this.type,
    required this.payload,
    required this.createdAt,
    required this.baseVersion,
    this.status = PendingCommandStatus.pending,
  });

  PendingUmlCommand copyWith({
    PendingCommandStatus? status,
    Map<String, dynamic>? payload,
  }) => PendingUmlCommand(
    localId: localId,
    commandId: commandId,
    type: type,
    payload: payload ?? this.payload,
    createdAt: createdAt,
    baseVersion: baseVersion,
    status: status ?? this.status,
  );

  Map<String, dynamic> toJson() => {
    'localId': localId,
    'commandId': commandId,
    'type': type,
    'payload': payload,
    'createdAt': createdAt.toIso8601String(),
    'baseVersion': baseVersion,
    'status': status.name,
  };

  factory PendingUmlCommand.fromJson(Map<String, dynamic> json) =>
      PendingUmlCommand(
        localId: json['localId'] as String,
        commandId: json['commandId'] as String,
        type: json['type'] as String,
        payload: Map<String, dynamic>.from(json['payload'] as Map),
        createdAt: DateTime.parse(json['createdAt'] as String),
        baseVersion: (json['baseVersion'] as num).toInt(),
        status: PendingCommandStatus.values.firstWhere(
          (value) => value.name == json['status'],
          orElse: () => PendingCommandStatus.pending,
        ),
      );
}

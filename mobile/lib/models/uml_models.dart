class UmlAttribute {
  final String id;
  final String name;
  final String type;
  final String visibility;

  const UmlAttribute({
    required this.id,
    required this.name,
    required this.type,
    required this.visibility,
  });

  factory UmlAttribute.fromJson(Map<String, dynamic> json) => UmlAttribute(
    id: json['id'] as String,
    name: json['name'] as String,
    type: json['type'] as String,
    visibility: json['visibility'] as String? ?? 'PRIVATE',
  );
}

class UmlOperation {
  final String id;
  final String name;
  final String returnType;

  const UmlOperation({
    required this.id,
    required this.name,
    required this.returnType,
  });

  factory UmlOperation.fromJson(Map<String, dynamic> json) => UmlOperation(
    id: json['id'] as String,
    name: json['name'] as String,
    returnType: json['returnType'] as String? ?? 'void',
  );
}

class UmlClass {
  final String id;
  final String name;
  final List<UmlAttribute> attributes;
  final List<UmlOperation> operations;

  const UmlClass({
    required this.id,
    required this.name,
    required this.attributes,
    required this.operations,
  });

  factory UmlClass.fromJson(Map<String, dynamic> json) => UmlClass(
    id: json['id'] as String,
    name: json['name'] as String,
    attributes: ((json['attributes'] as List<dynamic>?) ?? [])
        .map((item) => UmlAttribute.fromJson(item as Map<String, dynamic>))
        .toList(),
    operations: ((json['operations'] as List<dynamic>?) ?? [])
        .map((item) => UmlOperation.fromJson(item as Map<String, dynamic>))
        .toList(),
  );
}

class UmlRelationship {
  final String type;
  final String sourceClassId;
  final String targetClassId;
  final String sourceMultiplicity;
  final String targetMultiplicity;

  const UmlRelationship({
    required this.type,
    required this.sourceClassId,
    required this.targetClassId,
    required this.sourceMultiplicity,
    required this.targetMultiplicity,
  });

  factory UmlRelationship.fromJson(Map<String, dynamic> json) =>
      UmlRelationship(
        type: json['type'] as String,
        sourceClassId: json['sourceClassId'] as String,
        targetClassId: json['targetClassId'] as String,
        sourceMultiplicity: json['sourceMultiplicity'] as String? ?? '',
        targetMultiplicity: json['targetMultiplicity'] as String? ?? '',
      );
}

class UmlModel {
  final String projectId;
  final int version;
  final List<UmlClass> classes;
  final List<UmlRelationship> relationships;

  const UmlModel({
    required this.projectId,
    required this.version,
    required this.classes,
    required this.relationships,
  });

  factory UmlModel.fromJson(Map<String, dynamic> json) => UmlModel(
    projectId: json['projectId'] as String,
    version: (json['version'] as num).toInt(),
    classes: ((json['classes'] as List<dynamic>?) ?? [])
        .map((item) => UmlClass.fromJson(item as Map<String, dynamic>))
        .toList(),
    relationships: ((json['relationships'] as List<dynamic>?) ?? [])
        .map((item) => UmlRelationship.fromJson(item as Map<String, dynamic>))
        .toList(),
  );
}

import 'package:uuid/uuid.dart';
import 'offline_store.dart';

class ParticipantIdentityService {
  final OfflineStore store;
  const ParticipantIdentityService(this.store);

  Future<String> loadOrCreate() async {
    final existing = await store.loadParticipantId();
    if (existing != null && existing.isNotEmpty) return existing;
    final value = 'mobile-${const Uuid().v4()}';
    await store.saveParticipantId(value);
    return value;
  }
}

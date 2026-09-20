-- Seed de desarrollo temporal para la Fase 1
-- ID fijo del proyecto: 00000000-0000-0000-0000-000000000001
-- UUIDs generados para la inserción

INSERT INTO uml_models (id, project_id, version)
VALUES ('00000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000000001', 0)
ON CONFLICT (project_id) DO NOTHING;

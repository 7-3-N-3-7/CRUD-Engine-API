-- ── Keycloak database ──────────────────────────────────────────────────────
CREATE DATABASE crudapp_keycloak_db;

-- ── Product seed data ──────────────────────────────────────────────────────
-- Runs on first boot only (postgres initdb.d scripts are skipped if the
-- data directory already exists).  Hibernate creates the schema via
-- Liquibase before this runs, so the tables are guaranteed to exist.
-- We wrap everything in a DO block so we can use ON CONFLICT to stay
-- idempotent if someone runs it twice.
-- ---------------------------------------------------------------------------
\c crudapp_db

-- Required for the tenant-isolation RLS policy on baseentity
SET app.current_tenant = 'default';

DO $$
BEGIN

  -- BaseEntity rows (identity column, so we supply explicit IDs)
  INSERT INTO baseentity (id, version, created_by, created_date, last_modified_by, last_modified_date, tenant_id)
  VALUES
    (1, 0, 'seed', NOW(), 'seed', NOW(), 'crud-realm'),
    (2, 0, 'seed', NOW(), 'seed', NOW(), 'crud-realm'),
    (3, 0, 'seed', NOW(), 'seed', NOW(), 'crud-realm'),
    (4, 0, 'seed', NOW(), 'seed', NOW(), 'crud-realm'),
    (5, 0, 'seed', NOW(), 'seed', NOW(), 'crud-realm')
  ON CONFLICT (id) DO NOTHING;

  -- Product rows
  INSERT INTO product (id, name, description, price)
  VALUES
    (1, 'CRUD Engine Core',           'The reactive engine powering dynamic REST APIs with zero boilerplate.',              0.00),
    (2, 'Security Module (Keycloak)', 'JWT-based authentication and RBAC via Keycloak integration.',                      49.99),
    (3, 'Audit Log Plugin',           'Automatic audit trail for every create, update and delete operation.',              19.99),
    (4, 'Rate Limiter Plugin',        'Per-tenant request throttling to protect your APIs under load.',                    29.99),
    (5, 'Weaviate Vector Engine',     'Semantic search and AI-powered vector storage for your entities.',                  99.99)
  ON CONFLICT (id) DO NOTHING;

END $$;

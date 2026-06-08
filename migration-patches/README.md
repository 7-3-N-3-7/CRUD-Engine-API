# Security-into-Core Migration — submodule patches

`crud-engine-security-keycloak` has been promoted from a separable submodule into
`crud-engine-core`, because **security is foundational, not an afterthought**.

`crud-engine-core` and `crud-engine-spring-boot-starter` live in their **own Git
repositories** (they are submodules of this parent). The changes for those repos
**cannot be pushed from the parent repository**, so they are provided here as
ready-to-apply patches.

## What this parent PR already contains
- `pom.xml` — removed the `crud-engine-security-keycloak` `<module>` and its
  `dependencyManagement` entry.
- `.gitmodules` + submodule removal of `crud-engine-security-keycloak`.
- Documentation updates (`README.md`, `documentation.md`, `OPTIMIZATIONS.md`).
- `crud-app-sample` hardening:
  - removed the `keycloak.test.public-key` backdoor from the shipped
    `application.properties`;
  - flipped `app.mode` to `PRODUCTION`;
  - externalized the datasource credentials behind environment variables;
  - the integration test now opts into `app.mode=DEVELOPMENT` so the test-only
    static key path stays valid.

## Apply order (must be sequential across repos)
The parent build/CI will stay red until the two submodule PRs below are merged
and this parent PR is updated to point at the new submodule commits.

1. **`crud-engine-core`** (adds the security classes + dependencies + deny-by-default):
   ```bash
   cd crud-engine-core
   git switch -c security-into-core
   git apply --3way ../migration-patches/0001-crud-engine-core-make-security-foundational.patch
   git add -A && git commit -m "feat(security): make security foundational in core"
   git push
   ```
2. **`crud-engine-spring-boot-starter`** (drops the now-transitive dependency):
   ```bash
   cd crud-engine-spring-boot-starter
   git switch -c drop-security-keycloak-dep
   git apply --3way ../migration-patches/0002-crud-engine-spring-boot-starter-drop-security-keycloak-dep.patch
   git add -A && git commit -m "build: drop crud-engine-security-keycloak dependency (security now in core)"
   git push
   ```
3. **Parent**: bump the `crud-engine-core` and `crud-engine-spring-boot-starter`
   submodule pointers to the new commits, then verify:
   ```bash
   git submodule update --remote crud-engine-core crud-engine-spring-boot-starter
   git add crud-engine-core crud-engine-spring-boot-starter
   mvn -q -DskipTests install && mvn -q test   # requires Postgres on :5433
   ```
4. **Archive** the old `crud-engine-security-keycloak` repository (do not delete —
   keep history).

## Verification performed locally
With patches 1 & 2 applied to the submodules and the parent changes in place, the
full reactor builds and the sample integration suite passes:

```
crud-engine-core ............ SUCCESS
crud-engine-spring-boot-starter SUCCESS
crud-app-sample ............. SUCCESS
Tests run: 14, Failures: 0, Errors: 0, Skipped: 0
```

These patch files and this directory can be deleted once steps 1–3 are merged.

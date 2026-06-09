# Context Map: demo-service

Canonical task-type template: [templates/context-map.md](../../context-map.md).

Use that document for Open/Close/Strategy patterns. This file lists
**project-specific paths** for `demo-service` only.

## Path overrides

| Task type | Open (demo-service paths) |
|-----------|---------------------------|
| Feature implementation | `domain/Person.java`, `controller/PersonController.java`, `dto/` |
| Database migration | `db/changelog/db.changelog-master.xml`, `db/changelog/changes/` |
| Bug fix / debugging | Failing test file + code under test; paste `git log --oneline -5` if regression |
| Code review | Diff via `git diff main...feature-branch` |
| Test generation | Class under test (e.g. `service/impl/PersonServiceImpl.java`), pattern test (e.g. `PersonControllerTest.java`) |
| CI/CD pipeline fix | `.gitlab-ci.yml`, relevant `.helm/` files |

Skills in this project: `code-review`, `generate-tests`, `db-migration`, `schema-review`, `ci-fix`.

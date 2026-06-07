# Sample Project

This directory contains a complete, working reference implementation.

## What's Included

```
sample-project/
├── AGENTS.md                        # Project context for AI assistants
├── .cursorignore                    # Files excluded from AI context
├── context-map.md                   # Which files to open per task
├── pom.xml                          # Maven config with Java 21
├── .gitlab-ci.yml                   # GitLab CI pipeline
│
├── skills/                          # Reusable AI skills
│   ├── code-review/SKILL.md
│   ├── generate-tests/SKILL.md
│   ├── db-migration/SKILL.md
│   ├── schema-review/SKILL.md       # Database schema reviewer
│   └── ci-fix/SKILL.md              # CI pipeline debugger
│
├── .cursor/                         # Cursor configuration
│   ├── mcp.json                     # MCP server config
│   └── commands/                    # Slash commands
│       ├── review.md
│       ├── test.md
│       ├── endpoint.md
│       ├── migration.md
│       ├── explain.md
│       ├── ci-fix.md                # CI pipeline debugging
│       └── k8s-debug.md             # Kubernetes debugging
│
├── .opencode/                       # OpenCode configuration
│   ├── mcp.json                     # MCP server config
│   └── commands/                    # OpenCode commands
│       ├── review.md
│       ├── test.md
│       ├── migration.md
│       ├── ci-fix.md
│       └── k8s-debug.md
│
├── .codeassistant/                  # SourceCraft configuration
│   ├── mcp.json                     # MCP server config
│   └── commands/                    # SourceCraft commands
│       ├── review.md
│       ├── test.md
│       ├── migration.md
│       ├── ci-fix.md
│       └── k8s-debug.md
│
├── src/main/java/com/example/demo/  # Java source
│   ├── DemoServiceApplication.java
│   ├── controller/PersonController.java
│   ├── service/PersonService.java
│   ├── service/impl/PersonServiceImpl.java
│   ├── repository/PersonRepository.java
│   ├── domain/Person.java
│   ├── dto/CreatePersonRequest.java
│   ├── dto/PersonResponse.java
│   └── mapper/PersonMapper.java
│
├── src/main/resources/
│   ├── application.yaml
│   └── db/changelog/               # Liquibase
│       ├── db.changelog-master.xml
│       └── changes/2026-04-04-001-create-persons-table.xml
│
├── src/test/java/                   # Tests
│   └── com/example/demo/PersonControllerTest.java
│
└── .helm/                           # Helm chart
    ├── Chart.yaml
    ├── values.yaml
    └── templates/deployment.yaml
```

## How to Use

1. Copy the `sample-project/` structure as a starting point for your project.
2. Update `AGENTS.md` with your project-specific details.
3. Update `context-map.md` with your project's file structure.
4. Customize the skills and commands for your team's conventions.
5. Configure `.cursorignore` for your sensitive files.
6. Set up MCP servers in `mcp.json` with your database credentials.
7. Delete any assistant-specific directories you don't need (e.g., keep
   only `.cursor/` if you only use Cursor).

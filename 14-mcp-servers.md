# Section 15: MCP Servers

MCP (Model Context Protocol) is an open standard (created by Anthropic,
adopted across the industry) for connecting **external capabilities** to
your AI assistant without modifying the IDE itself.

### The Problem MCP Solves

Built-in tools are fixed by the IDE vendor. If you need the assistant to:
- Query your GitLab merge requests
- Read symbols from a language server
- Look up rows in a production database
- Fetch a Jira ticket description

...you cannot add those as built-in tools. MCP provides a **standard
protocol** for plugging in external tool servers.

This section covers architecture, agent-loop integration, tool resolution,
configuration in each assistant, useful servers for Java developers, custom
server development, security, and troubleshooting.

| Section | Topic |
|---------|-------|
| [1. MCP Architecture](#1-mcp-architecture) | Host, client, server roles, transports |
| [2. MCP in the Agent Loop](#2-mcp-in-the-agent-loop) | Integration, MCP vs built-in tools |
| [3. Tool Resolution](#3-tool-resolution-with-multiple-mcp-servers) | Heuristics, parallel calls, failure modes, AGENTS.md |
| [4. Configuring MCP Servers](#4-configuring-mcp-servers) | Cursor, OpenCode config, quick lab |
| [5. Useful Servers for Java](#5-useful-mcp-servers-for-java-developers) | Recommended servers, selection guide |
| [6. Writing a Custom Server](#6-writing-a-custom-mcp-server) | Node.js and Python examples |
| [7. Security Considerations](#7-mcp-security-considerations) | Permissions, trust, risks |
| [8. Troubleshooting](#8-troubleshooting-mcp) | Common failures and fixes |

---

## 1. MCP Architecture

MCP defines three roles:

```text
┌─────────────────────────────────────────────────────────────┐
│                        MCP Host                              │
│                  (IDE: Cursor, OpenCode)                     │
│                                                             │
│  ┌───────────────────────────────────────────────────────┐   │
│  │                     MCP Client                        │   │
│  │              (AI Assistant / Agent)                   │   │
│  │                                                       │   │
│  │  Sees MCP tools in its tool list alongside            │   │
│  │  built-in tools (Read, Grep, Shell, etc.)             │   │
│  └──────┬────────────────┬──────────────┬────────────────┘   │
│         │                │              │                    │
│         ▼                ▼              ▼                    │
│  ┌────────────┐  ┌────────────┐  ┌───────────────┐          │
│  │ MCP Server │  │ MCP Server │  │ MCP Server    │          │
│  │ (GitLens)  │  │ (Serena)   │  │ (Database)    │          │
│  │            │  │            │  │               │          │
│  │ Tools:     │  │ Tools:     │  │ Tools:        │          │
│  │ - git_log  │  │ - find_sym │  │ - run_query   │          │
│  │ - blame    │  │ - get_refs │  │ - list_tables │          │
│  │ - diff     │  │ - search   │  │ - describe    │          │
│  └────────────┘  └────────────┘  └───────────────┘          │
└─────────────────────────────────────────────────────────────┘
```

| Component   | What It Does                                         |
|-------------|------------------------------------------------------|
| **Host**    | The IDE application that runs the assistant. Manages MCP clients. |
| **Client**  | One per server. Maintains session, routes tool calls from the agent. |
| **Server**  | A separate process (local or remote) that exposes tools, resources, and prompts via JSON-RPC (stdio or SSE). |

### Communication Transports

| Transport | How It Works                              | Use When                  |
|-----------|-------------------------------------------|---------------------------|
| `stdio`   | Server runs as a child process. JSON over stdin/stdout. | Local servers, fastest.  |
| `SSE`     | Server runs as an HTTP service. Server-Sent Events.     | Remote / shared servers. |

### Tools, Resources, and Prompts

| MCP Capability | Purpose                          | Example                                |
|----------------|----------------------------------|----------------------------------------|
| **Tools**      | Actions the model can invoke     | `find_symbol`, `run_query`, `git_log`  |
| **Resources**  | Read-only data for context       | Project README, API schema, DB schema  |
| **Prompts**    | Reusable prompt templates        | Code review template, migration guide  |

Resources have URIs and are useful for static context like documentation,
configuration schemas, or API specifications — data the assistant can fetch
without executing a function.

---

## 2. MCP in the Agent Loop

MCP tools appear in the model's tool list **exactly like built-in tools**.
The model does not know or care whether a tool is built-in or comes from
an MCP server. It simply picks the best tool for the job.

```mermaid
sequenceDiagram
    participant User
    participant Agent as Agent Runtime
    participant LLM
    participant BuiltIn as Built-in Tools
    participant MCP as MCP Server (Serena)

    User->>Agent: "Find all classes that reference PersonService"
    Agent->>LLM: Context + tool list (built-in + MCP tools)
    
    Note over LLM: Model sees both Grep (built-in)<br/>and find_referencing_symbols (MCP).<br/>Picks the MCP tool for precision.

    LLM-->>Agent: Tool call: CallMcpTool("serena", "find_referencing_symbols", {name: "PersonService"})
    Agent->>MCP: JSON-RPC: find_referencing_symbols({name: "PersonService"})
    MCP-->>Agent: 4 references with code snippets and file locations
    Agent->>LLM: Inject MCP result into context
    LLM-->>Agent: Text: "PersonService is referenced by PersonController, PersonControllerTest, ..."
    Agent-->>User: Display response
```

### When Does MCP Get Called vs Built-In Tools?

The model chooses based on the tool description in the schema. The decision
is not hardcoded — it emerges from the LLM's training and the tool
descriptions you provide.

| Scenario                                      | Likely Tool Choice         | Why                                     |
|-----------------------------------------------|----------------------------|-----------------------------------------|
| Read a known file path                        | Built-in Read              | Direct, no overhead                     |
| Search for a text pattern                     | Built-in Grep              | Fast regex search                       |
| Find all references to a symbol               | MCP (Serena)               | Semantic understanding, not just text   |
| Get git blame for a file                      | MCP (GitLens)              | Git-specific operation                  |
| Run a database query                          | MCP (Database)             | External system access                  |
| List files matching a pattern                 | Built-in Glob              | Filesystem operation                    |
| Find code by meaning ("where is auth done?")  | Built-in SemanticSearch    | Vector index is built-in                |

See [Section 2: How AI Assistants Work](02-how-ai-assistants-work.md) for
the full agent loop, built-in tool selection, and how MCP fits into the
end-to-end request flow.

---

## 3. Tool Resolution with Multiple MCP Servers

When you connect several MCP servers, the model's tool list can grow to
dozens or even hundreds of tools. A typical setup might look like this:

```text
Tool list seen by the LLM (in system prompt):
┌─────────────────────────────────────────────────────────────────┐
│  Built-in tools (10):                                           │
│    Read, Grep, Glob, SemanticSearch, StrReplace, Write,         │
│    Shell, ReadLints, Task, CallMcpTool                          │
│                                                                 │
│  MCP: GitLens (5 tools):                                        │
│    get_file_blame, get_file_changes, get_commit_details,        │
│    get_working_changes, search_commits                          │
│                                                                 │
│  MCP: Serena (8 tools):                                         │
│    find_symbol, get_symbols_overview, find_referencing_symbols,  │
│    replace_symbol_body, insert_before_symbol,                   │
│    insert_after_symbol, search_for_pattern, list_dir            │
│                                                                 │
│  MCP: Database (4 tools):                                       │
│    run_query, list_tables, describe_table, get_schema           │
│                                                                 │
│  Total: 27 tools                                                │
└─────────────────────────────────────────────────────────────────┘
```

The model must pick the right tool from all 27 on every iteration.

### The Decision Is Description-Driven, Not Server-Driven

The model does **not** think "I should use the Serena MCP server."
It thinks "I need to find all references to PersonService" and then
scans all 27 tool descriptions for the best match. The fact that
`find_referencing_symbols` comes from Serena is irrelevant to the
model — it only cares about the tool's description and parameters.

This means **tool description quality is the primary factor** in
whether an MCP tool gets selected:

| Description Quality                                    | Will the model pick it?           |
|--------------------------------------------------------|-----------------------------------|
| `"find_referencing_symbols: Find all symbols that reference the given symbol, including call sites, imports, and type references. Returns code snippets and file locations."` | Yes — clear, specific, matches many intents |
| `"find_refs: Find refs"` | Rarely — too vague, the model cannot tell what it does |
| `"tool_7: Execute operation type 3 on the codebase"` | Never — incomprehensible description |

### How the Model Resolves Overlapping Tools

Multiple tools can serve similar purposes. When the model sees
overlapping capabilities, it uses several heuristics to choose:

```mermaid
sequenceDiagram
    participant LLM
    participant Runtime as Agent Runtime
    participant Grep as Built-in Grep
    participant Serena as MCP: Serena
    participant GitLens as MCP: GitLens

    Note over LLM: User asked: "Who last modified the<br/>PersonService validation logic and<br/>find all places that call validate()?"
    
    Note over LLM: DECISION 1: "Who last modified" →<br/>git history → get_file_blame (GitLens)<br/><br/>DECISION 2: "find all places that call" →<br/>reference search → find_referencing_symbols (Serena)<br/><br/>Both are independent → emit parallel calls

    LLM->>Runtime: Two tool calls (parallel)
    
    par Tool call 1
        Runtime->>GitLens: get_file_blame("PersonServiceImpl.java")
        GitLens-->>Runtime: Blame data: last modified by dev@team.com, 3 days ago
    and Tool call 2
        Runtime->>Serena: find_referencing_symbols("validate", depth=1)
        Serena-->>Runtime: Called from PersonController.create(), PersonControllerTest
    end

    Runtime->>LLM: Both results injected
    Note over LLM: Synthesize: "The validation logic was<br/>last modified by dev@team.com 3 days ago.<br/>validate() is called from PersonController<br/>and PersonControllerTest."
```

The model chose GitLens for git history and Serena for code references
because each tool's description matched the specific sub-task. Built-in
Grep was **not** chosen even though it could find the text "validate"
because the model recognized that semantic reference search is more
precise than text matching for this request.

### Resolution Heuristics

When multiple tools could handle the same intent, the model applies
these implicit priorities:

| Heuristic                          | Example                                    | Effect                                    |
|------------------------------------|--------------------------------------------|-------------------------------------------|
| **Specificity wins**               | `find_referencing_symbols` vs `Grep`       | Specialized tool beats general-purpose     |
| **Description match**              | "find symbol" matches `find_symbol` over `search_for_pattern` | Closer semantic match wins |
| **Minimize round-trips**           | One tool returning structured data vs two tools returning raw text | Fewer iterations preferred |
| **Prefer read-only first**         | `get_symbols_overview` before `replace_symbol_body` | Gather context before acting |
| **Built-in for simple tasks**      | `Read` for reading a known file path       | No MCP overhead for basic operations      |
| **MCP for domain-specific tasks**  | `run_query` for database operations        | Only MCP can reach external systems       |

### When Tool Selection Goes Wrong

The model can make suboptimal tool choices. Common failure modes:

**Problem: Vague tool descriptions**

If two MCP tools have similar vague descriptions, the model may pick the
wrong one or hesitate between them. Fix this by writing precise, distinct
descriptions for each tool.

**Problem: Too many tools**

With 50+ tools, the tool schemas alone consume 5K-10K tokens of the context
window, and the model has more options to get confused by. Each MCP server
you add increases system prompt size and selection complexity.

**Problem: Missing tool**

If no tool matches the intent, the model will improvise — often by using
Shell to run a command, or by using Grep as a fallback for any search task.
This produces lower-quality results than a purpose-built tool.

**Mitigation strategies:**

1. **Write descriptive tool descriptions** when building MCP servers.
   Include the use case, not just the function signature.
2. **Limit connected MCP servers** to those you actually use. Disconnect
   servers you are not actively working with.
3. **Use AGENTS.md to guide tool preference** — see below and
   [Section 9: AGENTS.md](09-agents-md.md).

### Guiding Tool Selection via AGENTS.md

You can influence the model's tool selection by adding hints in AGENTS.md.
Because AGENTS.md sits early in the context window, these hints are seen on
every iteration of the agent loop:

```markdown
## Tool Usage Preferences

- For finding symbol references and call sites, use Serena's
  `find_referencing_symbols` tool instead of Grep
- For git blame and commit history, use GitLens MCP tools
- For database schema questions, use the Database MCP `describe_table`
  tool before writing SQL queries
- Use built-in Grep only for literal text pattern matching
```

This is not a hard rule — the model can still choose differently if
the context suggests it — but it provides a strong default preference
that resolves ambiguity in most cases.

---

## 4. Configuring MCP Servers

### Cursor

Config file: `.cursor/mcp.json` (project-level) or `~/.cursor/mcp.json` (global).

```json
{
  "mcpServers": {
    "serena": {
      "command": "uvx",
      "args": ["serena-mcp", "--project-root", "."],
      "description": "Semantic code analysis"
    },
    "postgres": {
      "command": "npx",
      "args": ["-y", "@modelcontextprotocol/server-postgres"],
      "env": {
        "POSTGRES_URL": "postgresql://dev:dev@localhost:5432/mydb"
      }
    },
    "filesystem": {
      "command": "npx",
      "args": ["-y", "@modelcontextprotocol/server-filesystem", "/path/to/project"]
    }
  }
}
```

### OpenCode

Config file: `.opencode/mcp.json` or `~/.config/opencode/mcp.json`.

```json
{
  "mcpServers": {
    "postgres": {
      "command": "npx",
      "args": ["-y", "@modelcontextprotocol/server-postgres"],
      "env": {
        "POSTGRES_URL": "postgresql://dev:dev@localhost:5432/mydb"
      }
    }
  }
}
```

Once registered, the MCP server's tools appear in the model's tool list
automatically. No prompt changes are needed.

### Verifying the Connection

After adding a server, restart the assistant and check that the new tools
appear. In Cursor, open the MCP tools panel or ask:

> "What MCP tools do you have available?"

The assistant should list the server's tools (e.g., `postgres.run_query`,
`postgres.describe_table`).

### Quick Lab Preview

Here is what it looks like when an MCP server is configured and the agent
uses it during a real task.

**Configuration** (in `.cursor/mcp.json`):
```json
{
  "mcpServers": {
    "postgres": {
      "command": "npx",
      "args": ["-y", "@modelcontextprotocol/server-postgres", "postgresql://localhost:5432/persons_db"]
    }
  }
}
```

**What the developer sees in chat:**

```
Developer: What columns does the persons table have?

Agent:     [calls MCP tool: postgres.describe_table("persons")]
           → The persons table has 5 columns:
             id         BIGINT       PRIMARY KEY
             first_name VARCHAR(100) NOT NULL
             last_name  VARCHAR(100) NOT NULL
             email      VARCHAR(255) UNIQUE
             created_at TIMESTAMP    DEFAULT now()
```

The agent chose `describe_table` from the MCP server because its
description matched the intent better than any built-in tool. No prompt
engineering was needed — MCP tool descriptions drive selection
automatically.

---

## 5. Useful MCP Servers for Java Developers

| Server                   | Package / Repo                                    | Key Tools                             | Use Case                            |
|--------------------------|---------------------------------------------------|---------------------------------------|--------------------------------------|
| **PostgreSQL**           | `@modelcontextprotocol/server-postgres`           | `run_query`, `describe_table`, `list_tables` | Query schemas, test SQL, explore data |
| **Filesystem**           | `@modelcontextprotocol/server-filesystem`         | `read_file`, `write_file`, `list_directory`  | Extra file access outside workspace  |
| **Git**                  | `@modelcontextprotocol/server-git`                | `log`, `diff`, `blame`, `branch_list`        | Git history analysis                 |
| **GitLab**               | Community servers                                  | `list_issues`, `create_mr`, `get_pipeline`   | Issue/MR management from chat        |
| **Jira**                 | Community servers                                  | `get_issue`, `search_issues`, `add_comment`  | Ticket context in prompts            |
| **Docker**               | Community servers                                  | `list_containers`, `container_logs`          | Debug containerized services         |
| **Kubernetes**           | Community servers                                  | `get_pods`, `describe_pod`, `get_logs`       | K8s cluster troubleshooting          |

### Choosing Between MCP and Built-In Tools

| Scenario                                    | Use MCP?  | Why                                          |
|---------------------------------------------|-----------|----------------------------------------------|
| Read/write project files                    | No        | Built-in Read/Write tools are faster         |
| Search code in the project                  | No        | Built-in Grep/Glob is optimized              |
| Query a running PostgreSQL database         | **Yes**   | No built-in DB tool exists                   |
| Get GitLab pipeline status                  | **Yes**   | No built-in CI tool exists                   |
| Interact with Jira tickets                  | **Yes**   | No built-in issue tracker tool exists        |
| Read files outside the workspace            | **Yes**   | Built-in Read is scoped to workspace         |

---

## 6. Writing a Custom MCP Server

When no existing server covers your need, you can write your own.
MCP servers are simple programs that speak JSON-RPC over stdin/stdout.

### Minimal Example (Node.js / TypeScript)

This server exposes a single tool: `get_app_health` that checks a Spring Boot
actuator endpoint.

```typescript
import { McpServer } from "@modelcontextprotocol/sdk/server/mcp.js";
import { StdioServerTransport } from "@modelcontextprotocol/sdk/server/stdio.js";
import { z } from "zod";

const server = new McpServer({
  name: "spring-actuator",
  version: "1.0.0",
});

server.tool(
  "get_app_health",
  "Check Spring Boot actuator /health endpoint",
  {
    url: z.string().describe("Base URL of the Spring Boot app, e.g. http://localhost:8080"),
  },
  async ({ url }) => {
    const res = await fetch(`${url}/actuator/health`);
    const body = await res.json();
    return {
      content: [{ type: "text", text: JSON.stringify(body, null, 2) }],
    };
  }
);

const transport = new StdioServerTransport();
await server.connect(transport);
```

### Minimal Example (Python)

```python
from mcp.server.fastmcp import FastMCP
import httpx

mcp = FastMCP("spring-actuator")

@mcp.tool()
async def get_app_health(url: str) -> str:
    """Check Spring Boot actuator /health endpoint."""
    async with httpx.AsyncClient() as client:
        response = await client.get(f"{url}/actuator/health")
        return response.text

mcp.run()
```

### Registering Your Custom Server

```json
{
  "mcpServers": {
    "spring-actuator": {
      "command": "node",
      "args": ["path/to/actuator-server.js"]
    }
  }
}
```

### Design Guidelines

1. **Keep servers focused** — one server per data source or concern
2. **Use descriptive tool names** — the LLM picks tools by name and description
3. **Include parameter descriptions** — the LLM needs to know what to pass
4. **Handle errors gracefully** — return error text, don't crash the server
5. **Never expose destructive operations without safeguards** — confirm before DELETE/DROP

---

## 7. MCP Security Considerations

| Risk                                  | Mitigation                                          |
|---------------------------------------|-----------------------------------------------------|
| Database credentials in config        | Use environment variables, not hardcoded values     |
| Destructive SQL via `run_query`       | Use a read-only database user for MCP               |
| Server has network access             | Run servers in isolated environments when possible  |
| LLM can call tools automatically      | Review tool calls in the agent loop before confirming |
| Config files committed to Git         | Add `mcp.json` to `.gitignore` or use env vars      |

---

## 8. Troubleshooting MCP

| Symptom                           | Likely Cause                          | Fix                                     |
|-----------------------------------|---------------------------------------|------------------------------------------|
| "No MCP tools available"         | Config file not found or invalid JSON | Check path and JSON syntax               |
| "Connection refused"             | Server not running or wrong port      | Start the server, check the URL          |
| "Tool call failed"               | Missing env vars or wrong arguments   | Check server logs, verify env vars       |
| Server starts but no tools show  | Tools not registered properly         | Verify `server.tool()` calls             |
| Slow responses                   | Network latency to remote server      | Use stdio transport for local servers    |

## Quick-Reference Cheat Sheet

| Concept           | Summary                                                    |
|--------------------|------------------------------------------------------------|
| MCP Host           | The AI assistant (Cursor, OpenCode)                        |
| MCP Client         | One per server, managed by the host                        |
| MCP Server         | External process exposing tools/resources                  |
| stdio transport    | Server as child process, JSON over stdin/stdout            |
| SSE transport      | Server as HTTP service, Server-Sent Events                 |
| Tools              | Functions the LLM can call (e.g., run_query)               |
| Resources          | Read-only data (e.g., schema files)                        |
| Config file        | `.cursor/mcp.json`, `.opencode/mcp.json`, etc.             |
| Custom server      | Any process that speaks MCP protocol (Node.js, Python, Go) |
| Tool selection     | Description-driven — quality of tool descriptions determines which tool the model picks |

---

## Next Section

Proceed to [Section 15: Java Production Stack](15-java-production-stack.md)
to apply AI assistants to Java 21, Spring Boot, database, and DevOps workflows.

### Further Reading

- [MCP Specification](https://spec.modelcontextprotocol.io/)
- [MCP Server Registry](https://github.com/modelcontextprotocol/servers)
- [Section 2: How AI Assistants Work](02-how-ai-assistants-work.md) for the agent loop and built-in tools
- [Section 9: AGENTS.md](09-agents-md.md) for project context that complements MCP

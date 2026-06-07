# Section 12: MCP Servers

MCP (Model Context Protocol) extends your AI assistant with external tools and
data sources. Instead of the assistant being limited to built-in tools like
file read/write and shell, MCP lets it query databases, call APIs, interact
with issue trackers, and more — all through a standardized protocol.

This section explains the architecture, shows how to configure MCP servers in
each assistant, lists the most useful servers for Java developers, and walks
through writing a custom MCP server.

## 1. MCP Architecture

MCP follows a client-server model with three layers:

```text
┌─────────────────────────────────────────────────────┐
│  MCP Host (Cursor / OpenCode / SourceCraft)         │
│                                                     │
│  ┌──────────────┐   ┌──────────────┐                │
│  │  MCP Client  │   │  MCP Client  │   ...          │
│  └──────┬───────┘   └──────┬───────┘                │
│         │                  │                         │
└─────────┼──────────────────┼─────────────────────────┘
          │ stdio / SSE      │ stdio / SSE
          ▼                  ▼
   ┌──────────────┐   ┌──────────────┐
   │  MCP Server  │   │  MCP Server  │
   │  (postgres)  │   │  (gitlab)    │
   └──────────────┘   └──────────────┘
```

### Roles

| Component   | What It Does                                         |
|-------------|------------------------------------------------------|
| **Host**    | The AI assistant application. Manages clients.       |
| **Client**  | One per server. Maintains session, routes tool calls. |
| **Server**  | Exposes tools and/or resources via the MCP protocol.  |

### Communication Transports

| Transport | How It Works                              | Use When                  |
|-----------|-------------------------------------------|---------------------------|
| `stdio`   | Server runs as a child process. JSON over stdin/stdout. | Local servers, fastest.  |
| `SSE`     | Server runs as an HTTP service. Server-Sent Events.     | Remote / shared servers. |

### What a Server Can Expose

| Capability   | Description                                   | Example                         |
|--------------|-----------------------------------------------|----------------------------------|
| **Tools**    | Functions the LLM can call                    | `postgres.run_query(sql)`       |
| **Resources**| Read-only data the LLM can fetch              | `file:///schema.sql`            |
| **Prompts**  | Predefined prompt templates                   | `explain_query(sql)`            |

## 2. Configuring MCP Servers

### Cursor

Config file: `.cursor/mcp.json` (project-level) or `~/.cursor/mcp.json` (global).

```json
{
  "mcpServers": {
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

### Yandex SourceCraft

Config file: `.codeassistant/mcp.json`.

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

### Verifying the Connection

After adding a server, restart the assistant and check that the new tools
appear. In Cursor, open the MCP tools panel or ask:

> "What MCP tools do you have available?"

The assistant should list the server's tools (e.g., `postgres.run_query`,
`postgres.describe_table`).

## 3. Useful MCP Servers for Java Developers

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

## 4. Writing a Custom MCP Server

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

## 5. MCP Security Considerations

| Risk                                  | Mitigation                                          |
|---------------------------------------|-----------------------------------------------------|
| Database credentials in config        | Use environment variables, not hardcoded values     |
| Destructive SQL via `run_query`       | Use a read-only database user for MCP               |
| Server has network access             | Run servers in isolated environments when possible  |
| LLM can call tools automatically      | Review tool calls in the agent loop before confirming |
| Config files committed to Git         | Add `mcp.json` to `.gitignore` or use env vars      |

## 6. Troubleshooting MCP

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
| MCP Host           | The AI assistant (Cursor, OpenCode, SourceCraft)           |
| MCP Client         | One per server, managed by the host                        |
| MCP Server         | External process exposing tools/resources                  |
| stdio transport    | Server as child process, JSON over stdin/stdout            |
| SSE transport      | Server as HTTP service, Server-Sent Events                 |
| Tools              | Functions the LLM can call (e.g., run_query)               |
| Resources          | Read-only data (e.g., schema files)                        |
| Config file        | `.cursor/mcp.json`, `.opencode/mcp.json`, etc.             |
| Custom server      | Any process that speaks MCP protocol (Node.js, Python, Go) |

---

Proceed to [Section 6: Context](06-context.md)
to learn how to manage what goes into the AI's context window.

### Further Reading

- [MCP Specification](https://spec.modelcontextprotocol.io/)
- [MCP Server Registry](https://github.com/modelcontextprotocol/servers)
- [Section 2: How AI Assistants Work](02-how-ai-assistants-work.md) for the agent loop and tool model
- [Section 8: AGENTS.md](08-agents-md.md) for project context that complements MCP

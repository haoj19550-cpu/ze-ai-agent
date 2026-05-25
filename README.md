[README.md](https://github.com/user-attachments/files/28224232/README.md)
# ZE AI Agent

基于 Spring Boot 3 + Spring AI Alibaba 构建的 AI 智能体应用，集成 DashScope（通义千问）大模型、RAG 知识库、MCP 工具调用与多轮对话记忆，并配套 Vue 3 前端聊天室。

## ✨ 特性

- **双应用模式**
  - 💞 **AI 恋爱大师（小汐）**：基于角色提示词的领域聊天助手，配合 RAG 知识库回答恋爱场景问题
  - 🤖 **ZeManus 超级智能体**：基于 ReAct 模式的工具调用智能体，可联网搜索、抓取网页、操作文件、生成 PDF、执行终端命令等
- **流式输出**：基于 SSE（Server-Sent Events）实现打字机效果
- **多轮记忆**：会话历史持久化到 MySQL，支持按 `chatId` 续聊
- **RAG 检索增强**：支持 PgVector 向量数据库，加载 Markdown 知识库
- **MCP 协议**：通过 Model Context Protocol 接入外部工具服务（如图片搜索）
- **API 文档**：集成 Knife4j（Swagger UI）

## 🛠️ 技术栈

### 后端
- Java 21、Spring Boot 3.5
- Spring AI 1.1.2 + Spring AI Alibaba 1.1.2
- DashScope SDK（通义千问 / DeepSeek 模型）
- MyBatis-Plus + MySQL（会话存储）
- PgVector（向量存储，可选）
- Hutool、Jsoup、iText（工具依赖）
- Knife4j OpenAPI 3

### 前端
- Vue 3 + Vue Router 4
- Vite 6
- Axios

## 📁 项目结构

```
ze-ai-agent/
├── src/main/java/com/zegao/zeaiagent/
│   ├── ZeAiAgentApplication.java     # 启动类
│   ├── Controller/                   # REST 接口（健康检查、Love/Manus 聊天）
│   ├── agent/                        # 智能体抽象（BaseAgent → ReActAgent → ToolCallAgent → ZeManus）
│   ├── tools/                        # 工具集（WebSearch、WebScraping、File、PDF、Terminal 等）
│   ├── rag/                          # RAG 配置（文档读取、PgVector）
│   ├── chatMemory/                   # 文件型记忆实现
│   ├── repository/                   # MySQL 记忆 / 消息存储
│   ├── Advisor/                      # Spring AI 拦截器
│   ├── Config/                       # ChatClient、CORS、工具注册
│   ├── entity/                       # 实体类
│   └── demo/                         # LoveApp 等示例应用
├── src/main/resources/
│   ├── application.yml               # 主配置
│   ├── application-local.yml         # 本地 profile
│   ├── schema.sql                    # MySQL 建表脚本
│   ├── mcp-servers.json              # MCP 服务配置
│   └── document/                     # RAG 知识库（Markdown）
├── ze-agent-frontend/                # Vue3 前端
└── ze-images-search/                 # MCP 子模块（图片搜索服务）
```

## 🚀 快速开始

### 环境要求

- JDK 21+
- Maven 3.9+
- MySQL 8.0+
- Node.js 18+（前端）
- （可选）PostgreSQL + pgvector 扩展
- DashScope API Key（[阿里云百炼](https://bailian.console.aliyun.com/)）

### 1. 初始化数据库

```sql
-- 执行 src/main/resources/schema.sql
mysql -uroot -p < src/main/resources/schema.sql
```

### 2. 配置环境变量

```bash
# Windows PowerShell
$env:BAILIAN_API_KEY="你的-DashScope-API-Key"
$env:MYSQL_URL="jdbc:mysql://localhost:3306/ze_ai_agent?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true"
$env:MYSQL_USERNAME="root"
$env:MYSQL_PASSWORD="123456"

# Linux / macOS
export BAILIAN_API_KEY="你的-DashScope-API-Key"
export MYSQL_USERNAME="root"
export MYSQL_PASSWORD="123456"
```

### 3. 启动后端

```bash
./mvnw spring-boot:run
```

服务默认运行在 `http://localhost:8080/api`，Swagger UI 地址：
`http://localhost:8080/api/doc.html`

### 4. 启动前端

```bash
cd ze-agent-frontend
npm install
npm run dev
```

前端默认运行在 `http://localhost:5173`，并将 `/api` 反向代理至后端 `8080`。

### 5. （可选）启动 MCP 图片搜索服务

```bash
cd ze-images-search
./mvnw clean package -DskipTests
```

`mcp-servers.json` 已配置该服务的 stdio 启动方式，主应用会自动接入。

## 🌐 主要接口

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/ai/Love_app/Chat/sse` | 恋爱大师 SSE 流式对话 |
| GET | `/api/ai/manus/chat` | ZeManus 智能体 SSE 流式对话 |
| GET | `/api/ai/chats?appType=love\|manus` | 列出指定应用的会话列表 |
| GET | `/api/ai/chats/{chatId}/messages` | 查询某次会话的全部消息 |
| GET | `/api/health` | 健康检查 |

请求示例：

```bash
curl -N "http://localhost:8080/api/ai/Love_app/Chat/sse?message=你好&chatId=love-001"
```

## 🧰 智能体工具

ZeManus 默认注册的工具包括：

- `WebSearchTool`：联网搜索
- `WebScrapingTool`：网页抓取（Jsoup）
- `FileOperationTool`：本地文件读写
- `ResourceDownloadTool`：资源下载
- `PDFGenerationTool`：PDF 生成（iText）
- `TerminalOperationTool`：终端命令执行
- `TerminateTool`：结束推理循环
- 通过 MCP 接入的外部工具（如 `ze-images-search`）

## 🗂️ 前端页面

| 路由 | 页面 |
| --- | --- |
| `/` | 应用入口（切换 Love / Manus） |
| `/love` | AI 恋爱大师聊天室 |
| `/manus` | AI 超级智能体聊天室 |

## 📦 打包发布

```bash
# 后端
./mvnw clean package -DskipTests

# 前端
cd ze-agent-frontend
npm run build
```

后端产物：`target/ze-ai-agent-0.0.1-SNAPSHOT.jar`
前端产物：`ze-agent-frontend/dist/`

## 📝 License

本项目仅用于学习与研究目的。

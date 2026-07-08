# 智旅云图 Java 后端

这是把原 FastAPI 后端逐步迁移到 Java/Spring Boot 的第一版。

## 当前已完成

- Spring Boot 3 + Java 17 项目结构
- 前端兼容接口：
  - `GET /`
  - `GET /health`
  - `POST /trip/generate`
  - `POST /trip/edit`
  - `POST /trip/save`
  - `GET /trip`
  - `GET /trip/stats`
  - `GET /trip/{tripId}`
  - `DELETE /trip/{tripId}`
  - `GET /weather/forecast`
  - `GET /export/{tripId}/markdown`
  - `GET /export/{tripId}/pdf`
- 使用 DashScope/OpenAI-compatible `/chat/completions` 调用 `qwen-max`
- 使用内存 Map 保存演示行程
- `.env` 不上传 GitHub，但 Java 后端会尝试读取：
  - `backend-java/.env`
  - `../backend/.env`
  - `backend/.env`

## 运行方式

在项目根目录执行：

```bash
cd backend-java
mvn spring-boot:run
```

启动后访问：

```text
http://127.0.0.1:8080/health
```

前端要改成连接 Java 后端：

```env
VITE_API_BASE_URL=http://127.0.0.1:8080
```

改完前端 `.env` 后重启：

```bash
cd frontend
npm run dev
```

## 配置

可以复用原 Python 后端的 `backend/.env`：

```env
LLM_PROVIDER=openai_compatible
LLM_API_KEY=你的 DashScope Key
LLM_MODEL=qwen-max
LLM_BASE_URL=https://dashscope.aliyuncs.com/compatible-mode/v1
LLM_TIMEOUT_SECONDS=60
```

## 后续迁移计划

当前版本先把后端语言切到 Java，并保证前端能调用。后续可以继续迁移：

1. RAG：用 LangChain4j + 向量库替代 Python Chroma 调用链
2. Rerank：接入 qwen3-rerank
3. 高德地图：用 Java RestClient 调用 Web 服务 API
4. SQLite/MySQL：替换当前内存 Map 保存
5. Redis：迁移缓存层
6. PDF：完善中文字体和完整行程导出

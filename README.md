# 极客聊天室

一个基于 Spring Boot 的简易在线聊天室项目，提供用户注册登录、好友管理、实时私聊、历史消息、未读消息提醒、个人资料维护和文件上传功能。项目同时包含一套由 Spring Boot 静态资源托管的网页端界面。

## 功能特性

- 用户注册、登录和 BCrypt 密码加密
- 用户列表、用户搜索和个人资料编辑
- 发送、同意、拒绝好友申请
- 基于 WebSocket 的实时单聊
- 聊天消息持久化到 MySQL
- 历史消息分页查询
- 未读消息数量统计和会话已读处理
- 图片或其他文件上传
- 登录页、聊天页和个人资料页
- 支持移动端聊天侧边栏和表情输入

## 技术栈

- Java 26
- Spring Boot 4.1.1
- Spring MVC / WebSocket
- Spring Data JPA
- Spring Security
- MySQL
- Redis Starter（当前配置中未启用）
- Maven
- HTML、CSS、JavaScript
- Lombok

## 项目结构

```text
chat/
├── src/main/java/com/qqtongxun/chat/
│   ├── auth/                  # 注册、登录
│   ├── common/                # 统一返回结果、文件上传
│   ├── config/                # Security、MVC、WebSocket 配置
│   ├── friend/                # 好友关系和好友申请
│   ├── message/               # 消息、历史记录、未读数
│   ├── user/                  # 用户实体、查询和资料维护
│   └── websocket/             # 实时聊天处理器
├── src/main/resources/
│   ├── application.yaml       # 服务端口和数据库配置
│   └── static/                 # 网页端静态资源
│       ├── login.html          # 登录和注册页
│       ├── chat.html           # 聊天页
│       └── profile.html        # 个人资料页
├── uploads/                    # 文件上传目录，运行时自动创建文件
├── pom.xml
└── mvnw / mvnw.cmd
```

## 环境要求

运行项目之前，请准备：

1. JDK 26，并确保 `java -version` 可以正常执行。
2. MySQL 服务，并创建名为 `chat_db` 的数据库。
3. Maven，或者直接使用项目自带的 Maven Wrapper。

创建数据库示例：

```sql
CREATE DATABASE chat_db
	DEFAULT CHARACTER SET utf8mb4
	COLLATE utf8mb4_unicode_ci;
```

项目使用 JPA 的 `ddl-auto: update`，首次启动时会根据实体自动创建或更新数据表，包括用户、好友关系和聊天消息相关表。

## 配置数据库

默认配置位于 `src/main/resources/application.yaml`：

```yaml
spring:
	datasource:
		url: jdbc:mysql://localhost:3306/chat_db?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
		username: root
		password: 123456
```

请根据本机 MySQL 用户名和密码修改配置。不要在生产环境中使用仓库中的默认密码，建议通过环境变量或外部配置注入数据库凭据。

文件上传限制为单个文件最大 10 MB，文件默认保存到项目运行目录下的 `uploads/` 文件夹。

## 启动项目

在项目根目录 `JAVA/chat` 下执行：

Windows：

```powershell
./mvnw.cmd spring-boot:run
```

Linux 或 macOS：

```bash
./mvnw spring-boot:run
```

也可以先打包再运行：

```bash
./mvnw clean package
java -jar target/chat-0.0.1-SNAPSHOT.jar
```

启动成功后访问：

- 登录/注册：<http://localhost:8080/login.html>
- 聊天页面：<http://localhost:8080/chat.html>
- 个人资料：<http://localhost:8080/profile.html>

## REST API

接口统一返回以下结构：

```json
{
	"code": 200,
	"message": "操作成功",
	"data": {}
}
```

### 认证

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/api/auth/register` | 注册，提交 `username`、`nickname`、`password` |
| POST | `/api/auth/login` | 登录，提交 `username`、`password` |

用户名只允许英文、数字和下划线，长度为 4 至 20 位。登录成功后返回 `token`、`userId`、`nickname` 和 `avatar`。

### 用户和资料

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/users?currentUserId={userId}` | 获取用户列表，并排除当前用户 |
| GET | `/api/users/{userId}` | 获取指定用户资料 |
| GET | `/api/users/search?keyword={keyword}&currentUserId={userId}` | 按用户名或昵称搜索用户 |
| PUT | `/api/users/profile` | 更新昵称、头像、签名、性别和邮箱 |

### 好友

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/friends?userId={userId}` | 获取好友列表 |
| POST | `/api/friends/request` | 发送好友申请，提交 `fromUserId`、`toUserId` |
| GET | `/api/friends/requests?userId={userId}` | 获取待处理申请 |
| POST | `/api/friends/accept` | 同意申请，提交 `userId`、`requesterId` |
| POST | `/api/friends/reject` | 拒绝申请，提交 `userId`、`requesterId` |

### 消息和文件

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/messages/history?sessionId={sessionId}&page=0&size=30` | 分页获取历史消息 |
| GET | `/api/unread?userId={userId}` | 获取各好友的未读数量 |
| POST | `/api/unread/read` | 清除未读，提交 `userId`、`fromUserId` |
| POST | `/api/files/upload` | 使用 `multipart/form-data` 的 `file` 字段上传文件 |

## WebSocket 协议

连接地址：

```text
ws://localhost:8080/ws/chat?userId={userId}
```

发送私聊消息示例：

```json
{
	"fromUserId": "发送者ID",
	"toUserId": "接收者ID",
	"content": "你好",
	"type": "1",
	"sessionId": "按两个用户ID排序后用下划线连接"
}
```

服务端会把消息保存到数据库；接收方在线时实时推送，离线时只保存消息并增加未读数。当前群聊广播分支已保留，但网页端主要使用单聊。

## 会话 ID

前端使用两个用户 ID 按字典序排序后拼接生成会话 ID，例如：

```text
user-a_user-b
```

这样同一对用户无论谁发起聊天，都会查询到同一个历史消息会话。

## 开发说明

- 当前登录接口生成的是随机字符串 Token，后续请求不会校验该 Token；项目现阶段主要依靠前端保存的 `userId` 工作。
- `SecurityConfig` 对用户、好友、消息、文件和 WebSocket 路径开放访问，正式部署前应补充真正的 Token/JWT 校验和接口权限控制。
- 未读消息暂存在应用内存中，服务重启后会丢失；如果需要持久化或支持多实例部署，应启用 Redis 或将未读状态保存到数据库。
- WebSocket 允许任意来源跨域连接，生产环境应限制 `setAllowedOrigins` 的来源范围。
- `application.yaml` 开启了 SQL 输出，生产环境建议关闭 `spring.jpa.show-sql`。

## 测试

执行项目测试：

```bash
./mvnw test
```

当前测试目录包含 Spring Boot 应用启动测试。涉及数据库、WebSocket 和文件上传的完整业务验证仍建议补充集成测试。

## License

当前项目未声明单独的开源许可证。

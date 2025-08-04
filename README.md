# Anti-Politically-Related-Content 插件

![Minecraft](https://img.shields.io/badge/Minecraft-1.21.4-green) ![Java](https://img.shields.io/badge/Java-17-blue)

## 项目简介

这是一个Minecraft Bukkit插件，用于检测和过滤游戏中的政治相关内容。插件使用OpenRouter AI API对玩家聊天内容、命令和玩家ID进行实时审核。
4.0-RELEASE版本已更新，请查看分支

## 功能特点

- 实时监控玩家聊天内容
- 检测玩家命令中的政治内容
- 新玩家加入时检查玩家ID
- 自动处理违规内容：
  - 发送政治内容的玩家会被踢出
  - 使用违规ID的玩家会被封禁
- 异步API调用不影响服务器性能

## 安装要求

- Minecraft Paper服务器 1.21.4+
- Java 17或更高版本
- 有效的OpenRouter API密钥（请自行去openrouter.ai申请）

## 安装方法

1. 使用Maven构建项目：
   依次执行
   ```bash
   mvn clean
   ```
   
   ```bash
   mvn package
   ```
2. 将生成的`target/anti-politically-related-content-1.0-SNAPSHOT.jar`复制到服务器的`plugins`文件夹
3. 重启服务器

## 配置说明

1. 在Main.java中修改以下常量：
   （不建议更换model，默认模型是免费的而且效果不错）
   ```java
   private static final String API_KEY = "你的OpenRouter API密钥";
   private static final String MODEL = "deepseek/deepseek-chat-v3-0324:free";
   ```

2. 确保服务器可以访问OpenRouter API:
   （不要换这里的URL啊啊啊啊啊啊啊）
   ```java
   private static final String API_URL = "https://openrouter.ai/api/v1/chat/completions";
   ```

## 注意事项

1. 插件需要网络连接才能调用OpenRouter API
2. API调用可能会有延迟，请根据服务器性能调整异步任务设置
3. 默认使用deepseek-chat-v3-0324模型，可以替换为其他支持的模型（不建议R1，思考时间太长了）

## 开发者

- 项目名称: anti-politically-related-content
- 组ID: com.example
- 版本: 1.0-SNAPSHOT
- 依赖: PaperAPI, OkHttp, Gson

## 许可证

本项目使用MIT许可证。详情请查看LICENSE文件。

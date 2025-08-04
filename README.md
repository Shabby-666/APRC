# Anti-Politically-Related-Content 插件

![Minecraft](https://img.shields.io/badge/Minecraft-1.21.4-green) ![Java](https://img.shields.io/badge/Java-17-blue)

## 项目简介

这是一个Minecraft Bukkit插件，用于检测和过滤游戏中的政治相关内容。插件使用OpenRouter AI API对玩家聊天内容、命令和玩家ID进行实时审核。

## 特别鸣谢

感谢 [抖音-Pisoft](https://www.douyin.com/user/MS4wLjABAAAAueJ9tO_7lvvx4c-n-L0X_zKgqu8aQfIBySLvRmOlQv46MGafyyxHSMCQ-e2rqcgT?from_tab_name=main) 帮助测试。[他的github](https://github.com/PISOFT-Studio)

## 功能特点

- 实时监控玩家聊天内容
- 检测玩家命令中的政治内容
- 新玩家加入时检查玩家ID
- 自动处理违规内容：
  - 发送政治内容的玩家会被踢出
  - 使用违规ID的玩家会被封禁
- 异步API调用不影响服务器性能
- 【2.0版本更新】管理员可通过命令启用/禁用插件
- 【4.0-RELEASE版本更新】可通过/aprc test测试API地址连接
- 【4.0-RELEASE版本更新】可通过配置文件更改API相关配置（也就是说作者可以上传提前打包好的jar了）
- 【4.0-RELEASE版本更新】/aprc reload可重载配置文件
- 【4.0-RELEASE版本更新】增加一个备用API地址位，防止其中一个API平台突然爆炸

## 安装要求

- Minecraft Paper服务器 1.21.4+
- Java 17或更高版本
- 有效的OpenRouter API密钥（请访问[openrouter.ai](https://openrouter.ai)创建）
- （可选）有效的硅基流动API密钥（请访问[硅基流动官网](https://siliconflow.cn/)创建）
- （可选）其它API开放平台的API密钥（需要一定编程基础，需要改模型名称和API地址）

## 安装方法

1. 使用Maven构建项目：
   ```bash
   mvn clean package
   ```
2. 将生成的`target/anti-politically-related-content-1.0-SNAPSHOT.jar`复制到服务器的`plugins`文件夹
3. 重启服务器

## 配置说明

### API设置（修改Main.java文件）

   ```yaml
   primary:
     api_key: 主API密钥
     model: deepseek/deepseek-chat-v3-0324:free
     api_url: https://openrouter.ai/api/v1/chat/completions
   backup:
     api_key: 备用API密钥
     model: deepseek-ai/DeepSeek-R1-0528-Qwen3-8B
     api_url: https://api.siliconflow.cn/v1/chat/completions
   ```

### 响应自定义

修改Main.java中的以下方法来改变违规处理方式：
- `handleChatViolation()` - 处理聊天违规
- `handleNameViolation()` - 处理名称违规

## 使用示例

1. 安装并配置插件
2. 插件将自动：
   - 扫描所有聊天消息
   - 检查新玩家名称
   - 检测到违规时自动采取行动
3. 拥有aprc.admin权限的玩家可以通过 /aprc <test|on|off> 命令来测试API地址/启用/禁用插件

## 注意事项

1. 需要网络连接才能访问API
2. API调用可能有1-3秒延迟
3. 免费版有速率限制（高流量服务器建议升级）
4. 在生产服务器部署前请充分测试（作者也不确定这东西到底有多少bug）

## 开发者信息

- 项目名称: Anti-Politically-Related-Content
- 版本: 4.0-RELEASE
- 依赖库:
  - PaperAPI
  - OkHttp 4.12.0
  - Gson 2.10.1

## 许可证

MIT许可证 - 详情请查看[LICENSE](LICENSE)文件。

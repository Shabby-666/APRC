package com.example.anti_politically_related_content;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.CommandExecutor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;

import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import okhttp3.*;

public class Main extends JavaPlugin implements Listener, CommandExecutor {
    private boolean aiEnabled = true;
    private String primaryApiKey;
    private String primaryModel;
    private String primaryApiUrl;
    private String backupApiKey;
    private String backupModel;
    private String backupApiUrl;
    private final OkHttpClient httpClient = new OkHttpClient();
    private FileConfiguration config;
    
    @Override
    public void onEnable() {
        // 创建配置文件夹和文件
        if (!getDataFolder().exists()) {
            getDataFolder().mkdir();
        }
        File configFile = new File(getDataFolder(), "config.yml");
        
        // 初始化配置文件
        if (!configFile.exists()) {
            try {
                configFile.createNewFile();
                config = YamlConfiguration.loadConfiguration(configFile);
                config.options().header("这是APRC插件配置文件\n" +
                    "默认主API地址为openrouter，备用API为硅基流动\n" +
                    "默认模型都是免费模型\n" +
                    "请按要求填写API Key和必要配置");
                config.set("primary.api_key", "在此输入主API Key");
                config.set("primary.model", "deepseek/deepseek-chat-v3-0324:free");
                config.set("primary.api_url", "https://openrouter.ai/api/v1/chat/completions");
                config.set("backup.api_key", "在此输入备用API Key");
                config.set("backup.model", "deepseek-ai/DeepSeek-R1-0528-Qwen3-8B");
                config.set("backup.api_url", "https://api.siliconflow.cn/v1/chat/completions");
                config.save(configFile);
            } catch (IOException e) {
                getLogger().log(Level.SEVERE, "无法创建配置文件", e);
            }
        }
        
        // 加载配置
        reloadConfig();
        
        getServer().getPluginManager().registerEvents(this, this);
        getCommand("aprc").setExecutor(this);
        getLogger().info("Anti-Politically Related Content插件已启用");
    }

    @Override
    public void reloadConfig() {
        File configFile = new File(getDataFolder(), "config.yml");
        config = YamlConfiguration.loadConfiguration(configFile);
        primaryApiKey = config.getString("primary.api_key");
        primaryModel = config.getString("primary.model");
        primaryApiUrl = config.getString("primary.api_url");
        backupApiKey = config.getString("backup.api_key");
        backupModel = config.getString("backup.model");
        backupApiUrl = config.getString("backup.api_url");
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        if (!aiEnabled) return;
        handleContentCheck(event.getMessage(), event.getPlayer());
    }

    @EventHandler
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        if (!aiEnabled) return;
        handleContentCheck(event.getMessage(), event.getPlayer());
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (!aiEnabled) return;
        Player player = event.getPlayer();
        getServer().getScheduler().runTaskAsynchronously(this, () -> {
            try {
                boolean isPolitical = checkPoliticalContent(player.getName());
                if (isPolitical) {
                    Bukkit.getScheduler().runTask(this, () -> {
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), 
                            "say " + player.getName() + " 因ID违规被APRC封禁");
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), 
                            "ban " + player.getName() + " 您已被封禁：违规的ID");
                        getLogger().log(Level.INFO, "已封禁玩家 " + player.getName() + " 因违规ID");
                    });
            }
        } catch (Exception e) {
                getLogger().log(Level.SEVERE, "调用OpenRouter API失败", e);
            }
        });
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!sender.hasPermission("aprc.admin")) {
            sender.sendMessage("§c你没有权限使用此命令");
            return true;
        }
        
        if (args.length == 0) {
            sender.sendMessage("§aAPRC状态: " + (aiEnabled ? "§a启用" : "§c禁用"));
            return true;
        }
        
        if (args[0].equalsIgnoreCase("on")) {
            aiEnabled = true;
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "say APRC AI审核已开启");
            sender.sendMessage("§a已启用APRC AI审核");
            getLogger().info(sender.getName() + " 已启用APRC AI审核");
            return true;
        } else if (args[0].equalsIgnoreCase("off")) {
            aiEnabled = false;
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "say APRC AI审核已关闭");
            sender.sendMessage("§c已禁用APRC AI审核");
            getLogger().info(sender.getName() + " 已禁用APRC AI审核");
            return true;
        } else if (args[0].equalsIgnoreCase("test")) {
            testAPI(sender);
            return true;
        } else if (args[0].equalsIgnoreCase("reload")) {
            reloadConfig();
            sender.sendMessage("§a配置已重新加载");
            getLogger().info(sender.getName() + " 重新加载了配置文件");
            return true;
        }
        
        sender.sendMessage("§c用法: /aprc [on|off|test|reload]");
        return true;
    }

    private String getStatusMessage(int code) {
        switch(code) {
            case 200: return "OK";
            case 201: return "Created";
            case 204: return "No Content";
            case 400: return "Bad Request";
            case 401: return "Unauthorized";
            case 403: return "Forbidden";
            case 404: return "Not Found";
            case 500: return "Internal Server Error";
            case 503: return "Service Unavailable";
            default: return "Unknown";
        }
    }

    private void testAPI(CommandSender sender) {
        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
            try {
                // 测试主API
                JsonObject requestBody = new JsonObject();
                requestBody.addProperty("model", primaryModel);
                
                JsonArray messages = new JsonArray();
                JsonObject systemMessage = new JsonObject();
                systemMessage.addProperty("role", "system");
                systemMessage.addProperty("content", "请回复'测试成功'");
                messages.add(systemMessage);
                
                JsonObject userMessage = new JsonObject();
                userMessage.addProperty("role", "user");
                userMessage.addProperty("content", "测试连接");
                messages.add(userMessage);
                
                requestBody.add("messages", messages);
                requestBody.addProperty("max_tokens", 10);
                
                Request request = new Request.Builder()
                    .url(primaryApiUrl)
                    .header("Authorization", "Bearer " + primaryApiKey)
                    .header("Content-Type", "application/json")
                    .post(RequestBody.create(requestBody.toString(), MediaType.get("application/json")))
                    .build();
                
                try (Response response = httpClient.newCall(request).execute()) {
                    if (response.isSuccessful()) {
                        String responseBody = response.body().string();
                        JsonObject jsonResponse = JsonParser.parseString(responseBody).getAsJsonObject();
                        String result = jsonResponse.getAsJsonArray("choices")
                            .get(0).getAsJsonObject()
                            .getAsJsonObject("message")
                            .get("content").getAsString()
                            .trim();
                        
                        if (result.contains("测试成功")) {
                            Bukkit.getScheduler().runTask(this, () -> sender.sendMessage("§a主API测试成功: 200 OK"));
                            getLogger().info("主API测试成功");
                        } else {
                            Bukkit.getScheduler().runTask(this, () -> 
                                sender.sendMessage("§c主API测试失败: 预期'测试成功'但得到'" + result + "'"));
                            getLogger().warning("主API测试失败: " + result);
                        }
                    } else {
                        Bukkit.getScheduler().runTask(this, () -> 
                            sender.sendMessage("§c主API测试失败: " + response.code() + " " + getStatusMessage(response.code())));
                        getLogger().warning("主API测试失败: " + response.code());
                    }
                }
                
                // 测试备用API
                requestBody = new JsonObject();
                requestBody.addProperty("model", backupModel);
                messages = new JsonArray();
                systemMessage = new JsonObject();
                systemMessage.addProperty("role", "system");
                systemMessage.addProperty("content", "请回复'测试成功'");
                messages.add(systemMessage);
                
                userMessage = new JsonObject();
                userMessage.addProperty("role", "user");
                userMessage.addProperty("content", "测试连接");
                messages.add(userMessage);
                
                requestBody.add("messages", messages);
                requestBody.addProperty("max_tokens", 10);
                
                request = new Request.Builder()
                    .url(backupApiUrl)
                    .header("Authorization", "Bearer " + backupApiKey)
                    .header("Content-Type", "application/json")
                    .post(RequestBody.create(requestBody.toString(), MediaType.get("application/json")))
                    .build();
                
                try (Response response = httpClient.newCall(request).execute()) {
                    if (response.isSuccessful()) {
                        String responseBody = response.body().string();
                        JsonObject jsonResponse = JsonParser.parseString(responseBody).getAsJsonObject();
                        String result = jsonResponse.getAsJsonArray("choices")
                            .get(0).getAsJsonObject()
                            .getAsJsonObject("message")
                            .get("content").getAsString()
                            .trim();
                        
                        if (result.contains("测试成功")) {
                            Bukkit.getScheduler().runTask(this, () -> sender.sendMessage("§a备用API测试成功: 200 OK"));
                            getLogger().info("备用API测试成功");
                        } else {
                            Bukkit.getScheduler().runTask(this, () -> 
                                sender.sendMessage("§c备用API测试失败: 预期'测试成功'但得到'" + result + "'"));
                            getLogger().warning("备用API测试失败: " + result);
                        }
                    } else {
                        Bukkit.getScheduler().runTask(this, () -> 
                            sender.sendMessage("§c备用API测试失败: " + response.code() + " " + getStatusMessage(response.code())));
                        getLogger().warning("备用API测试失败: " + response.code());
                    }
                }
            } catch (Exception e) {
                Bukkit.getScheduler().runTask(this, () -> sender.sendMessage("§cAPI测试失败: " + e.getMessage()));
                getLogger().log(Level.SEVERE, "API测试失败", e);
            }
        });
    }

    private void handleContentCheck(String content, Player player) {
        if (!aiEnabled || content == null || player == null) {
            return;
        }
        
        final String message = content;
        final Player targetPlayer = player;
        
        // 异步调用API审核内容
        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
            try {
                boolean isPolitical = checkPoliticalContent(message);
                if (isPolitical) {
                    Bukkit.getScheduler().runTask(this, () -> {
                        if (targetPlayer.isOnline()) {
                            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), 
                                "say " + targetPlayer.getName() + " 由于发送涉政消息被APRC踢出");
                            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), 
                                "kick " + targetPlayer.getName() + " 您已被踢出：发送违规消息");
                            getLogger().log(Level.INFO, "已踢出玩家 " + targetPlayer.getName() + " 的涉政消息: " + message);
                        }
                    });
                }
            } catch (Exception e) {
                getLogger().log(Level.SEVERE, "调用OpenRouter API失败", e);
            }
        });
    }

    private boolean checkPoliticalContent(String message) {
        if (message == null || message.isEmpty()) {
            return false;
        }
        
        // 先尝试主API
        try {
            JsonObject requestBody = new JsonObject();
            requestBody.addProperty("model", primaryModel);
            
            JsonArray messages = new JsonArray();
            JsonObject systemMessage = new JsonObject();
            systemMessage.addProperty("role", "system");
            systemMessage.addProperty("content", "请严格判断以下内容是否包含涉政言论(只回答Yes或No)。不要判断是否有脏话或粗口，只判断是否涉及政治内容。如果内容违规请回答Yes，否则回答No。");
            messages.add(systemMessage);
            
            JsonObject userMessage = new JsonObject();
            userMessage.addProperty("role", "user");
            userMessage.addProperty("content", message);
            messages.add(userMessage);
            
            requestBody.add("messages", messages);
            requestBody.addProperty("max_tokens", 3);
            
            Request request = new Request.Builder()
                .url(primaryApiUrl)
                .header("Authorization", "Bearer " + primaryApiKey)
                .header("Content-Type", "application/json")
                .post(RequestBody.create(requestBody.toString(), MediaType.get("application/json")))
                .build();
            
            try (Response response = httpClient.newCall(request).execute()) {
                if (response.isSuccessful()) {
                    String responseBody = response.body().string();
                    JsonObject jsonResponse = JsonParser.parseString(responseBody).getAsJsonObject();
                    String result = jsonResponse.getAsJsonArray("choices")
                        .get(0).getAsJsonObject()
                        .getAsJsonObject("message")
                        .get("content").getAsString()
                        .trim();
                    
                    return result.equalsIgnoreCase("Yes");
                }
            }
        } catch (Exception e) {
            getLogger().log(Level.WARNING, "主API调用失败，尝试备用API", e);
        }
        
        // 主API失败时调用备用API
        try {
            JsonObject requestBody = new JsonObject();
            requestBody.addProperty("model", backupModel);
            
            JsonArray messages = new JsonArray();
            JsonObject systemMessage = new JsonObject();
            systemMessage.addProperty("role", "system");
            systemMessage.addProperty("content", "请严格判断以下内容是否包含涉政言论(只回答Yes或No)。不要判断是否有脏话或粗口，只判断是否涉及政治内容。如果内容违规请回答Yes，否则回答No。");
            messages.add(systemMessage);
            
            JsonObject userMessage = new JsonObject();
            userMessage.addProperty("role", "user");
            userMessage.addProperty("content", message);
            messages.add(userMessage);
            
            requestBody.add("messages", messages);
            requestBody.addProperty("max_tokens", 3);
            
            Request request = new Request.Builder()
                .url(backupApiUrl)
                .header("Authorization", "Bearer " + backupApiKey)
                .header("Content-Type", "application/json")
                .post(RequestBody.create(requestBody.toString(), MediaType.get("application/json")))
                .build();
            
            try (Response response = httpClient.newCall(request).execute()) {
                if (response.isSuccessful()) {
                    String responseBody = response.body().string();
                    JsonObject jsonResponse = JsonParser.parseString(responseBody).getAsJsonObject();
                    String result = jsonResponse.getAsJsonArray("choices")
                        .get(0).getAsJsonObject()
                        .getAsJsonObject("message")
                        .get("content").getAsString()
                        .trim();
                    
                    return result.equalsIgnoreCase("Yes");
                }
            }
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "备用API调用失败", e);
        }
        
        return false;
    }
}

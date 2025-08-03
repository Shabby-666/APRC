package com.example.anti_politically_related_content;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import okhttp3.*;
import java.io.IOException;
import java.util.logging.Level;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.CommandExecutor;

public class Main extends JavaPlugin implements Listener, CommandExecutor {
    private boolean aiEnabled = true;
    private static final String API_KEY = "填入你的openrouter.ai API Key（不填的我敬你是个人物）";
    private static final String MODEL = "deepseek/deepseek-chat-v3-0324:free";
    private static final String API_URL = "https://openrouter.ai/api/v1/chat/completions";
    private final OkHttpClient httpClient = new OkHttpClient();
    
    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        getCommand("aprc").setExecutor(this);
        getLogger().info("Anti-Politically Related Content插件已启用");
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
        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
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
            } catch (IOException e) {
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
            sender.sendMessage("§a已启用APRC AI审核");
            getLogger().info(sender.getName() + " 已启用APRC AI审核");
            return true;
        } else if (args[0].equalsIgnoreCase("off")) {
            aiEnabled = false;
            sender.sendMessage("§c已禁用APRC AI审核");
            getLogger().info(sender.getName() + " 已禁用APRC AI审核");
            return true;
        }
        
        sender.sendMessage("§c用法: /aprc [on|off]");
        return true;
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
            } catch (IOException e) {
                getLogger().log(Level.SEVERE, "调用OpenRouter API失败", e);
            }
        });
    }
    private boolean checkPoliticalContent(String message) throws IOException {
        if (message == null || message.isEmpty()) {
            return false;
        }
        // 构建符合OpenRouter API要求的请求体
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("model", MODEL);
        
        JsonArray messages = new JsonArray();
        
        // 系统提示
        JsonObject systemMessage = new JsonObject();
        systemMessage.addProperty("role", "system");
        systemMessage.addProperty("content", "请严格判断以下内容是否包含涉政言论(只回答Yes或No)。不要判断是否有脏话或粗口，只判断是否涉及政治内容。如果内容违规请回答Yes，否则回答No。");
        messages.add(systemMessage);
        
        // 用户消息
        JsonObject userMessage = new JsonObject();
        userMessage.addProperty("role", "user");
        userMessage.addProperty("content", message);
        messages.add(userMessage);
        
        requestBody.add("messages", messages);
        requestBody.addProperty("max_tokens", 3);
        
        Request request = new Request.Builder()
            .url(API_URL)
            .header("Authorization", "Bearer " + API_KEY)
            .header("Content-Type", "application/json")
            .post(RequestBody.create(requestBody.toString(), MediaType.get("application/json")))
            .build();
        
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);
            
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
}

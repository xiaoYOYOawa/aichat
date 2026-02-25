// File: AIService.java
package comaichat.service;

import comaichat.config.ModConfig;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import comaichat.Aichat;
import net.minecraft.server.command.ServerCommandSource;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.IOException;

public class AIService {

    public static String chatWithAI(String userMessage, ServerCommandSource source, boolean commandMode) throws IOException {
        String apiKey = ModConfig.apiKey;
        String apiUrl = ModConfig.apiUrl;

        if ("YOUR_API_KEY_HERE".equals(apiKey) || apiKey.isEmpty()) {
            return "❌ 错误：请在配置文件中设置正确的 API Key。";
        }

        // 构建请求的JSON数据
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("model", ModConfig.modelName);
        requestBody.addProperty("temperature", ModConfig.temperature);
        requestBody.addProperty("max_tokens", ModConfig.maxTokens);

        JsonArray messages = new JsonArray();

        // 动态系统提示词
        JsonObject systemMessage = new JsonObject();
        systemMessage.addProperty("role", "system");

        String systemPrompt = "你是一个叫xiaoYOYOawa，正在Minecraft游戏中与玩家对话。";
        if (commandMode && ModConfig.allowCommandExecution) {
            systemPrompt += SafeCommandService.getSystemPromptCommandsPart();
        } else {
            systemPrompt += "你不能执行游戏指令，只能进行聊天对话。";
        }
        systemPrompt += "回复要简洁明了，适合游戏内聊天。";

        systemMessage.addProperty("content", systemPrompt);
        messages.add(systemMessage);

        // 用户消息
        JsonObject userMessageObj = new JsonObject();
        userMessageObj.addProperty("role", "user");
        userMessageObj.addProperty("content", userMessage);
        messages.add(userMessageObj);

        requestBody.add("messages", messages);

        // 创建HTTP请求
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(10000)
                .setSocketTimeout(30000)
                .build();

        HttpPost httpPost = new HttpPost(apiUrl);
        httpPost.setHeader("Authorization", "Bearer " + apiKey);
        httpPost.setHeader("Content-Type", "application/json");
        httpPost.setEntity(new StringEntity(requestBody.toString()));

        // 发送请求并获取响应
        try (CloseableHttpClient client = HttpClients.custom()
                .setDefaultRequestConfig(requestConfig)
                .build();
             CloseableHttpResponse response = client.execute(httpPost)) {

            String responseBody = EntityUtils.toString(response.getEntity());
            int statusCode = response.getStatusLine().getStatusCode();

            Aichat.LOGGER.info("API响应状态码: {}, 响应体: {}", statusCode, responseBody);

            if (statusCode != 200) {
                return "❌ API请求失败，错误码：" + statusCode + "。请检查配置和网络连接。";
            }

            try {
                JsonObject jsonResponse = JsonParser.parseString(responseBody).getAsJsonObject();

                if (jsonResponse.has("choices") && jsonResponse.get("choices").isJsonArray()) {
                    JsonArray choices = jsonResponse.getAsJsonArray("choices");

                    if (!choices.isEmpty()) {
                        JsonObject firstChoice = choices.get(0).getAsJsonObject();

                        String responseText = "";

                        // OpenAI格式：检查message->content
                        if (firstChoice.has("message") && firstChoice.get("message").isJsonObject()) {
                            JsonObject messageObj = firstChoice.getAsJsonObject("message");
                            if (messageObj.has("content") && messageObj.get("content").isJsonPrimitive()) {
                                responseText = messageObj.get("content").getAsString().trim();
                            }
                        }

                        // 兼容格式：直接检查text
                        else if (firstChoice.has("text") && firstChoice.get("text").isJsonPrimitive()) {
                            responseText = firstChoice.get("text").getAsString().trim();
                        }

                        // 处理指令执行（如果启用了指令模式）
                        if (commandMode && ModConfig.allowCommandExecution) {
                            return processCommandResponse(responseText, source);
                        }

                        return responseText.isEmpty() ? "❌ 未收到有效响应" : responseText;

                    }
                }

                return "❌ AI响应格式异常。响应：" + responseBody.substring(0, Math.min(200, responseBody.length()));

            } catch (Exception e) {
                Aichat.LOGGER.error("JSON解析错误: {}", responseBody, e);
                return "❌ 解析AI响应时出错：" + e.getMessage();
            }

        } catch (Exception e) {
            Aichat.LOGGER.error("调用AI服务时发生错误", e);
            return "❌ 发生内部错误：" + e.getMessage();
        }
    }

    private static String processCommandResponse(String response, ServerCommandSource source) {
        // 检查是否包含指令标记
        if (response.contains("{{COMMAND:")) {
            try {
                // 提取指令内容
                int start = response.indexOf("{{COMMAND:") + "{{COMMAND:".length();
                int end = response.indexOf("}}", start);

                if (end > start) {
                    String command = response.substring(start, end).trim();

                    // 执行指令
                    boolean success = SafeCommandService.executeSafeCommand(source, command);

                    if (success) {
                        // 移除指令标记，返回剩余内容
                        String chatResponse = response.replace("{{COMMAND:" + command + "}}", "").trim();
                        return chatResponse.isEmpty() ? "✅ 指令已执行" : chatResponse;
                    } else {
                        String chatResponse = response.replace("{{COMMAND:" + command + "}}", "").trim();
                        return chatResponse.isEmpty() ? "❌ 指令执行失败" : chatResponse + " (指令执行失败)";
                    }
                }
            } catch (Exception e) {
                return "❌ 处理指令时出错: " + e.getMessage();
            }
        }

        return response;
    }

    // 简化测试连接方法
    public static String testConnection() throws IOException {
        String apiKey = ModConfig.apiKey;

        if ("YOUR_API_KEY_HERE".equals(apiKey) || apiKey.isEmpty()) {
            return "❌ API Key未设置";
        }

        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpPost httpPost = new HttpPost(ModConfig.apiUrl);
            httpPost.setHeader("Authorization", "Bearer " + apiKey);
            httpPost.setHeader("Content-Type", "application/json");

            JsonObject requestBody = new JsonObject();
            requestBody.addProperty("model", ModConfig.modelName);
            requestBody.addProperty("max_tokens", 10);

            JsonArray messages = new JsonArray();
            JsonObject message = new JsonObject();
            message.addProperty("role", "user");
            message.addProperty("content", "test");
            messages.add(message);
            requestBody.add("messages", messages);

            httpPost.setEntity(new StringEntity(requestBody.toString()));

            try (CloseableHttpResponse response = client.execute(httpPost)) {
                int statusCode = response.getStatusLine().getStatusCode();
                if (statusCode == 200) {
                    return "✅ 连接测试成功";
                } else {
                    return "❌ 连接失败，状态码: " + statusCode;
                }
            }
        } catch (Exception e) {
            return "❌ 连接测试异常: " + e.getMessage();
        }
    }

    // 重载原有方法，保持兼容性
    public static String chatWithAI(String userMessage) throws IOException {
        return chatWithAI(userMessage, null, false);
    }
}

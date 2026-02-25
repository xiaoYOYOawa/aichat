package comaichat.command;


import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import comaichat.config.ModConfig;
import comaichat.service.AIService;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.io.IOException;

import static net.minecraft.server.command.CommandManager.*;

public class AIChatCommand {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(
                literal("aichat")
                        .executes(context -> {
                            return showHelp((ServerCommandSource) context.getSource());
                        })

                        .then(argument("message", StringArgumentType.greedyString())
                                .executes(context -> {
                                    ServerCommandSource source = (ServerCommandSource) context.getSource();
                                    String message = StringArgumentType.getString(context, "message");
                                    return executeChat(source, message);
                                }))

                        .then(literal("config")
                                .executes(context -> {
                                    ServerCommandSource source = (ServerCommandSource) context.getSource();
                                    return showConfigHelp(source);
                                })

                                .then(literal("reload")
                                        .executes(context -> {
                                            ServerCommandSource source = (ServerCommandSource) context.getSource();
                                            return reloadConfig(source);
                                        }))

                                .then(literal("set")
                                        .then(argument("key", StringArgumentType.word())
                                                .then(argument("value", StringArgumentType.greedyString())
                                                        .executes(context -> {
                                                            ServerCommandSource source = (ServerCommandSource) context.getSource();
                                                            String key = StringArgumentType.getString(context, "key");
                                                            String value = StringArgumentType.getString(context, "value");
                                                            return setConfig(source, key, value);
                                                        })))
                                )

                                .then(literal("view")
                                        .executes(context -> {
                                            ServerCommandSource source = (ServerCommandSource) context.getSource();
                                            return viewConfig(source);
                                        }))

                                .then(literal("test")
                                        .executes(context -> {
                                            ServerCommandSource source = (ServerCommandSource) context.getSource();
                                            return testConnection(source);
                                        }))
                        )
        );
    }

    private static int executeChat(ServerCommandSource source, String message) {
        source.sendFeedback(() -> Text.literal("🤖 AI思考中...").formatted(Formatting.GRAY), false);

        new Thread(() -> {
            try {
                String response = AIService.chatWithAI(message);
                source.getServer().execute(() -> {
                    Text responseText = Text.literal("🤖 AI: ").formatted(Formatting.GREEN)
                            .append(Text.literal(response).formatted(Formatting.WHITE));
                    source.sendFeedback(() -> responseText, false);
                });
            } catch (IOException e) {
                source.getServer().execute(() -> {
                    source.sendError(Text.literal("❌ 调用AI时出错: " + e.getMessage()));
                });
            }
        }).start();

        return 1;
    }

    private static int reloadConfig(ServerCommandSource source) {
        try {
            ModConfig.loadConfig();
            source.sendFeedback(() -> Text.literal("✅ 配置已重载").formatted(Formatting.GREEN), true);
            return 1;
        } catch (Exception e) {
            source.sendError(Text.literal("❌ 重载配置失败: " + e.getMessage()));
            return 0;
        }
    }

    private static int setConfig(ServerCommandSource source, String key, String value) {
        try {
            switch (key.toLowerCase()) {
                case "apikey":
                    ModConfig.apiKey = value;
                    source.sendFeedback(() -> Text.literal("✅ API Key 已更新").formatted(Formatting.GREEN), true);
                    break;
                case "apiurl":
                    ModConfig.apiUrl = value;
                    source.sendFeedback(() -> Text.literal("✅ API URL 已更新: " + value).formatted(Formatting.GREEN), true);
                    break;
                case "model":
                    ModConfig.modelName = value;
                    source.sendFeedback(() -> Text.literal("✅ 模型名称已更新: " + value).formatted(Formatting.GREEN), true);
                    break;
                case "temperature":
                    try {
                        double temp = Double.parseDouble(value);
                        if (temp < 0 || temp > 2) {
                            source.sendError(Text.literal("❌ temperature 必须在 0-2 之间"));
                            return 0;
                        }
                        ModConfig.temperature = temp;
                        source.sendFeedback(() -> Text.literal("✅ Temperature 已更新: " + temp).formatted(Formatting.GREEN), true);
                    } catch (NumberFormatException e) {
                        source.sendError(Text.literal("❌ temperature 必须是数字"));
                        return 0;
                    }
                    break;
                case "maxtokens":
                    try {
                        int maxTokens = Integer.parseInt(value);
                        if (maxTokens < 1 || maxTokens > 4000) {
                            source.sendError(Text.literal("❌ maxTokens 必须在 1-4000 之间"));
                            return 0;
                        }
                        ModConfig.maxTokens = maxTokens;
                        source.sendFeedback(() -> Text.literal("✅ Max Tokens 已更新: " + maxTokens).formatted(Formatting.GREEN), true);
                    } catch (NumberFormatException e) {
                        source.sendError(Text.literal("❌ maxTokens 必须是整数"));
                        return 0;
                    }
                    break;
                default:
                    source.sendError(Text.literal("❌ 未知配置项。可用项: apikey, apiurl, model, temperature, maxtokens"));
                    return 0;
            }

            ModConfig.saveConfig();
            return 1;

        } catch (Exception e) {
            source.sendError(Text.literal("❌ 设置配置失败: " + e.getMessage()));
            return 0;
        }
    }

    private static int viewConfig(ServerCommandSource source) {
        source.sendFeedback(() -> Text.literal("=== 🤖 AI Chat Mod 配置 ===").formatted(Formatting.GOLD), false);
        source.sendFeedback(() -> Text.literal("API Key: " +
                        (ModConfig.apiKey.length() > 8 ? "***" + ModConfig.apiKey.substring(ModConfig.apiKey.length() - 4) : "未设置"))
                .formatted(Formatting.YELLOW), false);
        source.sendFeedback(() -> Text.literal("API URL: " + ModConfig.apiUrl).formatted(Formatting.YELLOW), false);
        source.sendFeedback(() -> Text.literal("模型: " + ModConfig.modelName).formatted(Formatting.YELLOW), false);
        source.sendFeedback(() -> Text.literal("Temperature: " + ModConfig.temperature).formatted(Formatting.YELLOW), false);
        source.sendFeedback(() -> Text.literal("Max Tokens: " + ModConfig.maxTokens).formatted(Formatting.YELLOW), false);
        source.sendFeedback(() -> Text.literal(" "), false);
        source.sendFeedback(() -> Text.literal("使用 /aichat config set <key> <value> 修改配置").formatted(Formatting.GRAY), false);
        source.sendFeedback(() -> Text.literal("使用 /aichat config reload 重载配置文件").formatted(Formatting.GRAY), false);
        source.sendFeedback(() -> Text.literal("使用 /aichat config test 测试连接").formatted(Formatting.GRAY), false);

        return 1;
    }

    private static int testConnection(ServerCommandSource source) {
        source.sendFeedback(() -> Text.literal("🔍 测试AI服务连接...").formatted(Formatting.GRAY), false);

        new Thread(() -> {
            try {
                String result = AIService.testConnection();
                source.getServer().execute(() -> {
                    source.sendFeedback(() -> Text.literal(result), false);
                });
            } catch (IOException e) {
                source.getServer().execute(() -> {
                    source.sendError(Text.literal("❌ 连接测试失败: " + e.getMessage()));
                });
            }
        }).start();

        return 1;
    }

    private static int showHelp(ServerCommandSource source) {
        source.sendFeedback(() -> Text.literal("=== 🤖 AI Chat Mod 帮助 ===").formatted(Formatting.GOLD), false);
        source.sendFeedback(() -> Text.literal("/aichat <消息> - 与AI聊天").formatted(Formatting.WHITE), false);
        source.sendFeedback(() -> Text.literal("/aichat config view - 查看配置").formatted(Formatting.WHITE), false);
        source.sendFeedback(() -> Text.literal("/aichat config set <key> <value> - 修改配置").formatted(Formatting.WHITE), false);
        source.sendFeedback(() -> Text.literal("/aichat config reload - 重载配置").formatted(Formatting.WHITE), false);
        source.sendFeedback(() -> Text.literal("/aichat config test - 测试连接").formatted(Formatting.WHITE), false);
        source.sendFeedback(() -> Text.literal(" "), false);
        source.sendFeedback(() -> Text.literal("配置项: apikey, apiurl, model, temperature, maxtokens").formatted(Formatting.GRAY), false);
        return 1;
    }

    private static int showConfigHelp(ServerCommandSource source) {
        source.sendFeedback(() -> Text.literal("=== 🤖 AI Chat Mod 配置帮助 ===").formatted(Formatting.GOLD), false);
        source.sendFeedback(() -> Text.literal("/aichat config view - 查看当前配置").formatted(Formatting.WHITE), false);
        source.sendFeedback(() -> Text.literal("/aichat config set <key> <value> - 修改配置").formatted(Formatting.WHITE), false);
        source.sendFeedback(() -> Text.literal("/aichat config reload - 重载配置文件").formatted(Formatting.WHITE), false);
        source.sendFeedback(() -> Text.literal("/aichat config test - 测试API连接").formatted(Formatting.WHITE), false);
        source.sendFeedback(() -> Text.literal(" "), false);
        source.sendFeedback(() -> Text.literal("配置项: apikey, apiurl, model, temperature, maxtokens").formatted(Formatting.GRAY), false);
        return 1;
    }
}

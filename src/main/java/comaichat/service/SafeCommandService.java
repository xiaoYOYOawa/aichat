// File: SafeCommandService.java
package comaichat.service;

import comaichat.config.ModConfig;
import comaichat.Aichat;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class SafeCommandService {
    // 安全指令白名单
    private static final Set<String> SAFE_COMMANDS = new HashSet<>(Arrays.asList(
            "time", "weather", "gamemode", "tp", "teleport", "give",
            "effect", "enchant", "kill", "summon", "setblock", "fill",
            "say", "me", "tell", "msg", "w", "tellraw", "title",
            "gamerule", "particle", "playsound", "clear", "spawnpoint"
    ));

    // 危险指令黑名单
    private static final Set<String> DANGEROUS_COMMANDS = new HashSet<>(Arrays.asList(
            "op", "deop", "stop", "ban", "pardon", "kick", "whitelist",
            "execute", "function", "scoreboard", "datapack", "save-all",
            "save-on", "save-off", "ban-ip", "pardon-ip", "banlist",
            "whitelist", "reload", "stop", "restart"
    ));

    // 需要特定权限的指令
    private static final Set<String> OP_COMMANDS = new HashSet<>(Arrays.asList(
            "gamemode", "tp", "teleport", "give", "effect", "enchant",
            "summon", "setblock", "fill", "gamerule", "clear", "spawnpoint"
    ));

    // 指令执行模式状态
    private static final Set<UUID> commandModePlayers = new HashSet<>();

    public static boolean executeSafeCommand(ServerCommandSource source, String command) {
        // 检查全局开关
        if (!ModConfig.allowCommandExecution) {
            source.sendError(Text.literal("❌ 指令执行功能已被禁用").formatted(Formatting.RED));
            return false;
        }

        // 检查权限等级
        if (source.getPermissionLevel() < ModConfig.minPermissionLevel) {
            source.sendError(Text.literal("❌ 你需要至少 " + ModConfig.minPermissionLevel + " 级权限才能执行指令")
                    .formatted(Formatting.RED));
            return false;
        }

        try {
            // 清理指令
            String cleanCommand = command.trim();
            if (cleanCommand.startsWith("/")) {
                cleanCommand = cleanCommand.substring(1);
            }

            if (cleanCommand.isEmpty()) {
                source.sendError(Text.literal("❌ 指令不能为空").formatted(Formatting.RED));
                return false;
            }

            // 提取指令名称
            String[] parts = cleanCommand.split(" ", 2);
            String commandName = parts[0].toLowerCase();

            // 安全检查
            if (!isCommandSafe(source, commandName, cleanCommand)) {
                return false;
            }

            // 记录日志
            if (ModConfig.enableCommandLogging) {
                String playerName = source.getPlayer() != null ?
                        source.getPlayer().getName().getString() : "CONSOLE";
                Aichat.LOGGER.info("AI执行指令 - 玩家: {}, 指令: /{}", playerName, cleanCommand);
            }

            // 执行指令
            int result = source.getServer().getCommandManager().executeWithPrefix(source, cleanCommand);

            if (result > 0) {
                source.sendFeedback(() -> Text.literal("✅ AI已执行指令: /" + cleanCommand)
                        .formatted(Formatting.GREEN), false);
                return true;
            } else {
                source.sendError(Text.literal("❌ 指令执行失败: /" + cleanCommand).formatted(Formatting.RED));
                return false;
            }

        } catch (Exception e) {
            source.sendError(Text.literal("❌ 执行指令时出错: " + e.getMessage()).formatted(Formatting.RED));
            return false;
        }
    }

    private static boolean isCommandSafe(ServerCommandSource source, String commandName, String fullCommand) {
        // 检查黑名单
        if (DANGEROUS_COMMANDS.contains(commandName)) {
            source.sendError(Text.literal("❌ 该指令已被禁止执行").formatted(Formatting.RED));
            return false;
        }

        // 检查白名单
        if (!SAFE_COMMANDS.contains(commandName)) {
            source.sendError(Text.literal("❌ 该指令不在安全白名单中").formatted(Formatting.RED));
            return false;
        }

        // 检查需要OP权限的指令
        if (OP_COMMANDS.contains(commandName) && source.getPermissionLevel() < 2) {
            source.sendError(Text.literal("❌ 执行该指令需要OP权限").formatted(Formatting.RED));
            return false;
        }

        // 额外的安全检查
        if (isPotentiallyDangerous(fullCommand)) {
            source.sendError(Text.literal("❌ 该指令参数可能不安全").formatted(Formatting.RED));
            return false;
        }

        return true;
    }

    private static boolean isPotentiallyDangerous(String command) {
        String lowerCommand = command.toLowerCase();

        // 检查是否有危险模式
        if (lowerCommand.contains("@a") || lowerCommand.contains("@e") || lowerCommand.contains("@r")) {
            return true;
        }

        // 限制某些指令的参数
        if (lowerCommand.startsWith("kill ") &&
                (lowerCommand.contains("@e") || lowerCommand.contains("@a"))) {
            return true;
        }

        if (lowerCommand.startsWith("give ") &&
                (lowerCommand.contains("command_block") || lowerCommand.contains("barrier"))) {
            return true;
        }

        if (lowerCommand.startsWith("fill ") && lowerCommand.contains("-")) {
            // 防止大范围填充
            String[] coords = lowerCommand.split(" ");
            if (coords.length >= 7) {
                try {
                    int volume = Math.abs(Integer.parseInt(coords[1]) - Integer.parseInt(coords[4])) *
                            Math.abs(Integer.parseInt(coords[2]) - Integer.parseInt(coords[5])) *
                            Math.abs(Integer.parseInt(coords[3]) - Integer.parseInt(coords[6]));
                    if (volume > 10000) { // 限制填充体积
                        return true;
                    }
                } catch (NumberFormatException e) {
                    // 参数解析失败，谨慎处理
                    return true;
                }
            }
        }

        return false;
    }

    // 指令模式管理
    public static void enableCommandMode(ServerPlayerEntity player) {
        commandModePlayers.add(player.getUuid());
    }

    public static void disableCommandMode(ServerPlayerEntity player) {
        commandModePlayers.remove(player.getUuid());
    }

    public static boolean isInCommandMode(ServerPlayerEntity player) {
        return commandModePlayers.contains(player.getUuid());
    }

    // 获取可用的安全指令列表
    public static String getAvailableCommands() {
        return "可用MC指令: time set <时间>, weather <clear/rain/thunder>, gamemode <模式> [玩家], " +
                "tp <目标玩家> <目的地玩家>, give <玩家> <物品> [数量], effect <玩家> <效果> [时长] [强度], " +
                "say <消息>, tell <玩家> <消息>, kill [实体], summon <实体类型> [位置], setblock <位置> <方块>, " +
                "fill <开始> <结束> <方块>, gamerule <规则> <值>。注意：带*的指令需要OP权限。";
    }

    // 获取系统提示词中的指令部分
    public static String getSystemPromptCommandsPart() {
        if (!ModConfig.allowCommandExecution) {
            return "你不能执行任何游戏指令。";
        }

        return "你可以执行安全的游戏指令来帮助玩家。" + getAvailableCommands() +
                "如果你要执行指令，请使用以下格式：{{COMMAND: /指令内容}}。例如：{{COMMAND: /time set day}}。" +
                "其他情况请正常聊天回复。指令执行需要玩家有相应权限。";
    }
}

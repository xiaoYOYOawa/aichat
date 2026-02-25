// File: ChatListener.java
package comaichat.listener;

import comaichat.service.ConversationManager;
import comaichat.service.AIService;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class ChatListener {

    public static void register() {
        ServerMessageEvents.CHAT_MESSAGE.register((message, sender, params) -> {
            String content = message.getContent().getString();

            // 检查是否应该由AI处理（例如以特定前缀开头）
            if (content.startsWith("ai ")) {
                // 取消原消息
                // 注意：这需要谨慎处理，可能会影响其他mod

                String aiMessage = content.substring(3).trim();
                if (!aiMessage.isEmpty()) {
                    processAIChat(sender, aiMessage);
                }
                return; // 阻止原消息发送
            }

            // 或者检查对话模式
            if (ConversationManager.isInConversationMode(sender)) {
                processAIChat(sender, content);
                // 可以选择是否阻止原消息发送
            }
        });
    }

    private static void processAIChat(ServerPlayerEntity player, String message) {
        player.sendMessage(Text.literal("🤖 AI思考中...").formatted(Formatting.GRAY), false);

        new Thread(() -> {
            try {
                String response = AIService.chatWithAI(message);
                player.getServer().execute(() -> {
                    Text responseText = Text.literal("🤖 AI: ").formatted(Formatting.GREEN)
                            .append(Text.literal(response).formatted(Formatting.WHITE));
                    player.sendMessage(responseText, false);
                });
            } catch (Exception e) {
                player.getServer().execute(() -> {
                    player.sendMessage(Text.literal("❌ AI处理出错: " + e.getMessage())
                            .formatted(Formatting.RED), false);
                });
            }
        }).start();
    }
}

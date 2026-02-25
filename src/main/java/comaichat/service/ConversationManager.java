// File: ConversationManager.java
package comaichat.service;

import net.minecraft.server.network.ServerPlayerEntity;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ConversationManager {
    private static final Map<UUID, Boolean> conversationMode = new HashMap<>();

    public static void enableConversationMode(ServerPlayerEntity player) {
        conversationMode.put(player.getUuid(), true);
    }

    public static void disableConversationMode(ServerPlayerEntity player) {
        conversationMode.remove(player.getUuid());
    }

    public static boolean isInConversationMode(ServerPlayerEntity player) {
        return conversationMode.getOrDefault(player.getUuid(), false);
    }
}

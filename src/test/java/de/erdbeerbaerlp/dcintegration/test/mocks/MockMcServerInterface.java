package de.erdbeerbaerlp.dcintegration.test.mocks;

import de.erdbeerbaerlp.dcintegration.common.util.McServerInterface;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.emoji.EmojiUnion;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.requests.RestAction;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Mock implementation of McServerInterface for testing.
 * Captures all messages sent to Minecraft and provides verification methods.
 */
public class MockMcServerInterface implements McServerInterface {
    private final List<SentMessage> sentMessages = new ArrayList<>();
    private final HashMap<UUID, String> players = new HashMap<>();
    private final HashMap<UUID, List<String>> playerPermissions = new HashMap<>();
    private int maxPlayers = 20;
    private int onlinePlayers = 0;
    private boolean onlineMode = true;
    private String loaderName = "TestLoader";
    
    /**
     * Represents a message sent to Minecraft
     */
    public static class SentMessage {
        public final Component componentMessage;
        public final String stringMessage;
        public final UUID targetPlayer;
        public final long timestamp;
        public final boolean isComponent;
        
        public SentMessage(Component msg, UUID target) {
            this.componentMessage = msg;
            this.stringMessage = null;
            this.targetPlayer = target;
            this.timestamp = System.currentTimeMillis();
            this.isComponent = true;
        }
        
        public SentMessage(String msg, UUID target) {
            this.componentMessage = null;
            this.stringMessage = msg;
            this.targetPlayer = target;
            this.timestamp = System.currentTimeMillis();
            this.isComponent = false;
        }
        
        public String getPlainText() {
            if (isComponent && componentMessage != null) {
                return LegacyComponentSerializer.legacySection().serialize(componentMessage);
            }
            return stringMessage;
        }
    }
    
    @Override
    public int getMaxPlayers() {
        return maxPlayers;
    }
    
    public void setMaxPlayers(int maxPlayers) {
        this.maxPlayers = maxPlayers;
    }
    
    @Override
    public int getOnlinePlayers() {
        return onlinePlayers;
    }
    
    public void setOnlinePlayers(int onlinePlayers) {
        this.onlinePlayers = onlinePlayers;
    }
    
    @Override
    public void sendIngameMessage(Component msg) {
        sentMessages.add(new SentMessage(msg, null));
    }
    
    @Override
    public void sendIngameMessage(String msg, UUID player) {
        sentMessages.add(new SentMessage(msg, player));
    }
    
    @Override
    public void sendIngameReaction(Member member, RestAction<Message> retrieveMessage, UUID targetUUID, EmojiUnion reactionEmote) {
        // Store reaction info if needed for testing
    }
    
    @Override
    public void runMcCommand(String cmd, CompletableFuture<InteractionHook> cmdMsg, User user) {
        // Store command for verification
        sentMessages.add(new SentMessage("[COMMAND] " + cmd, null));
    }
    
    @Override
    public HashMap<UUID, String> getPlayers() {
        return new HashMap<>(players);
    }
    
    public void addPlayer(UUID uuid, String name) {
        players.put(uuid, name);
        onlinePlayers = players.size();
    }
    
    public void removePlayer(UUID uuid) {
        players.remove(uuid);
        onlinePlayers = players.size();
    }
    
    @Override
    public boolean isOnlineMode() {
        return onlineMode;
    }
    
    public void setOnlineMode(boolean onlineMode) {
        this.onlineMode = onlineMode;
    }
    
    @Override
    public String getNameFromUUID(UUID uuid) {
        return players.get(uuid);
    }
    
    @Override
    public String getLoaderName() {
        return loaderName;
    }
    
    public void setLoaderName(String loaderName) {
        this.loaderName = loaderName;
    }
    
    @Override
    public boolean playerHasPermissions(UUID player, String... permissions) {
        List<String> playerPerms = playerPermissions.get(player);
        if (playerPerms == null) return false;
        for (String perm : permissions) {
            if (playerPerms.contains(perm)) return true;
        }
        return false;
    }
    
    public void setPlayerPermissions(UUID player, String... permissions) {
        List<String> perms = new ArrayList<>();
        for (String perm : permissions) {
            perms.add(perm);
        }
        playerPermissions.put(player, perms);
    }
    
    @Override
    public String runMCCommand(String cmdString) {
        return "Command executed: " + cmdString;
    }
    
    @Override
    public boolean isPlayerVanish(UUID player) {
        return false;
    }
    
    // Verification methods
    
    /**
     * Gets all messages sent to Minecraft
     */
    public List<SentMessage> getSentMessages() {
        return new ArrayList<>(sentMessages);
    }
    
    /**
     * Clears all sent messages
     */
    public void clearMessages() {
        sentMessages.clear();
    }
    
    /**
     * Gets the number of messages sent
     */
    public int getMessageCount() {
        return sentMessages.size();
    }
    
    /**
     * Asserts that at least one message contains the given text
     */
    public void assertMessageContains(String text) {
        for (SentMessage msg : sentMessages) {
            if (msg.getPlainText().contains(text)) {
                return;
            }
        }
        throw new AssertionError("No message contains: " + text);
    }
    
    /**
     * Asserts that no messages were sent
     */
    public void assertNoMessages() {
        if (!sentMessages.isEmpty()) {
            throw new AssertionError("Expected no messages, but got " + sentMessages.size());
        }
    }
    
    /**
     * Gets the last message sent
     */
    public SentMessage getLastMessage() {
        if (sentMessages.isEmpty()) {
            return null;
        }
        return sentMessages.get(sentMessages.size() - 1);
    }
}


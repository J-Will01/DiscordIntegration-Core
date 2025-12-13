package de.erdbeerbaerlp.dcintegration.test.e2e;

import de.erdbeerbaerlp.dcintegration.test.IntegrationTestBase;
import de.erdbeerbaerlp.dcintegration.test.util.TestConfigHelper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.UUID;

/**
 * Full end-to-end flow tests covering complete message flows in both directions.
 */
public class FullFlowE2ETest extends IntegrationTestBase {
    
    @Test
    public void testPlayerJoinFlow() throws Exception {
        // Simulate player join
        UUID playerUUID = UUID.randomUUID();
        String playerName = "NewPlayer";
        getMCEventSimulator().simulatePlayerJoin(playerUUID, playerName);
        
        // Wait for async operations
        waitForAsyncOperations();
        
        // Verify message sent to Discord
        var messages = getMockJDAFactory().getSentMessages(defaultChannelID);
        Assertions.assertFalse(messages.isEmpty(), "Join message should be sent to Discord");
        
        var lastMessage = getMockJDAFactory().getLastMessage(defaultChannelID);
        Assertions.assertNotNull(lastMessage);
        // Should contain player name
        boolean containsPlayer = false;
        if (lastMessage.content != null) {
            containsPlayer = lastMessage.content.contains(playerName);
        }
        if (lastMessage.embed != null && lastMessage.embed.getDescription() != null) {
            containsPlayer = containsPlayer || lastMessage.embed.getDescription().contains(playerName);
        }
        Assertions.assertTrue(containsPlayer, "Message should contain player name");
    }
    
    @Test
    public void testPlayerDeathFlow() throws Exception {
        // Simulate player death
        UUID playerUUID = UUID.randomUUID();
        String playerName = "DeadPlayer";
        String deathMessage = "was slain by a zombie";
        
        getMCEventSimulator().simulateDeath(playerUUID, playerName, deathMessage);
        
        // Wait for async operations
        waitForAsyncOperations();
        
        // Verify message sent to Discord
        var messages = getMockJDAFactory().getSentMessages(defaultChannelID);
        Assertions.assertFalse(messages.isEmpty(), "Death message should be sent to Discord");
        
        var lastMessage = getMockJDAFactory().getLastMessage(defaultChannelID);
        Assertions.assertNotNull(lastMessage);
        // Should contain death message
        boolean containsDeath = false;
        if (lastMessage.content != null) {
            containsDeath = lastMessage.content.contains(deathMessage) || lastMessage.content.contains(playerName);
        }
        if (lastMessage.embed != null) {
            if (lastMessage.embed.getDescription() != null) {
                containsDeath = containsDeath || lastMessage.embed.getDescription().contains(deathMessage);
            }
            if (lastMessage.embed.getTitle() != null) {
                containsDeath = containsDeath || lastMessage.embed.getTitle().contains(playerName);
            }
        }
        Assertions.assertTrue(containsDeath, "Message should contain death information");
    }
    
    @Test
    public void testBackupPatternFlow() throws Exception {
        // Setup backup pattern
        TestConfigHelper.setupMessagePattern(
            "(?i).*backup.*complete.*",
            "",
            true,
            "🧰 World Backup Completed",
            "Backup finished successfully",
            "#00FF00"
        );
        getDiscordIntegration().getMessagePatternMatcher().reloadPatterns();
        
        // Simulate backup completion log
        getMCEventSimulator().simulateConsoleLog("[INFO] World backup completed in 2.5s");
        
        // Wait for async operations
        waitForAsyncOperations();
        
        // Verify pattern matched and embed sent
        var lastMessage = getMockJDAFactory().getLastMessage(defaultChannelID);
        Assertions.assertNotNull(lastMessage);
        Assertions.assertNotNull(lastMessage.embed, "Should send embed for backup completion");
        Assertions.assertEquals("🧰 World Backup Completed", lastMessage.embed.getTitle());
    }
    
    @Test
    public void testChatBidirectionalFlow() throws Exception {
        // Simulate Minecraft chat message
        UUID playerUUID = UUID.randomUUID();
        String playerName = "ChatPlayer";
        String chatMessage = "Hello everyone!";
        
        getMCEventSimulator().simulateChatMessage(playerUUID, playerName, chatMessage);
        
        // Wait for async operations
        waitForAsyncOperations();
        
        // Verify message sent to Discord
        var messages = getMockJDAFactory().getSentMessages(defaultChannelID);
        Assertions.assertFalse(messages.isEmpty(), "Chat message should be sent to Discord");
        
        var lastMessage = getMockJDAFactory().getLastMessage(defaultChannelID);
        Assertions.assertNotNull(lastMessage);
        // Should contain chat message
        boolean containsChat = false;
        if (lastMessage.content != null) {
            containsChat = lastMessage.content.contains(chatMessage) || lastMessage.content.contains(playerName);
        }
        if (lastMessage.embed != null && lastMessage.embed.getDescription() != null) {
            containsChat = containsChat || lastMessage.embed.getDescription().contains(chatMessage);
        }
        Assertions.assertTrue(containsChat, "Message should contain chat content");
    }
}


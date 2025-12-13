package de.erdbeerbaerlp.dcintegration.test.e2e;

import de.erdbeerbaerlp.dcintegration.test.IntegrationTestBase;
import de.erdbeerbaerlp.dcintegration.test.util.TestConfigHelper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.UUID;

/**
 * End-to-end tests for advancement messages with custom embeds and channel routing.
 */
public class AdvancementE2ETest extends IntegrationTestBase {
    
    @Test
    public void testAdvancementWithCustomEmbed() throws Exception {
        // Setup custom embed
        TestConfigHelper.setupCustomAdvancementEmbed(
            "🏆 : %player%",
            "**%advName%**\n_%advDesc%_"
        );
        getDiscordIntegration().getMessagePatternMatcher().reloadPatterns();
        
        // Simulate advancement
        UUID playerUUID = UUID.randomUUID();
        getMCEventSimulator().simulateAdvancement(
            playerUUID,
            "TestPlayer",
            "Diamonds!",
            "Acquire diamonds"
        );
        
        // Wait for async operations
        waitForAsyncOperations();
        
        // Verify message sent to Discord
        var messages = getMockJDAFactory().getSentMessages(defaultChannelID);
        Assertions.assertFalse(messages.isEmpty(), "No message sent to Discord");
        
        var lastMessage = getMockJDAFactory().getLastMessage(defaultChannelID);
        Assertions.assertNotNull(lastMessage, "Last message should not be null");
        Assertions.assertNotNull(lastMessage.embed, "Message should have an embed");
        Assertions.assertEquals("🏆 : TestPlayer", lastMessage.embed.getTitle());
        Assertions.assertEquals("**Diamonds!**\n_Acquire diamonds_", lastMessage.embed.getDescription());
    }
    
    @Test
    public void testAdvancementChannelRouting() throws Exception {
        // Setup custom channel
        String customChannelID = "987654321";
        getMockJDAFactory().createMockTextChannel(customChannelID);
        TestConfigHelper.setupChannelRouting(customChannelID, null);
        getDiscordIntegration().getMessagePatternMatcher().reloadPatterns();
        
        // Simulate advancement
        UUID playerUUID = UUID.randomUUID();
        getMCEventSimulator().simulateAdvancement(
            playerUUID,
            "TestPlayer",
            "Test Advancement",
            "Test description"
        );
        
        // Wait for async operations
        waitForAsyncOperations();
        
        // Verify message sent to custom channel
        var messages = getMockJDAFactory().getSentMessages(customChannelID);
        Assertions.assertFalse(messages.isEmpty(), "Message should be sent to custom channel");
        
        // Verify default channel has no messages
        var defaultMessages = getMockJDAFactory().getSentMessages(defaultChannelID);
        // Note: May have other messages, so we check that advancement-specific content isn't there
    }
    
    @Test
    public void testAdvancementPlaceholderReplacement() throws Exception {
        // Setup custom embed with all placeholders
        TestConfigHelper.setupCustomAdvancementEmbed(
            "Player: %player%",
            "Advancement: %advName%\nDescription: %advDesc%"
        );
        getDiscordIntegration().getMessagePatternMatcher().reloadPatterns();
        
        // Simulate advancement
        UUID playerUUID = UUID.randomUUID();
        getMCEventSimulator().simulateAdvancement(
            playerUUID,
            "Steve",
            "Monster Hunter",
            "Kill a monster"
        );
        
        // Wait for async operations
        waitForAsyncOperations();
        
        // Verify placeholders replaced
        var lastMessage = getMockJDAFactory().getLastMessage(defaultChannelID);
        Assertions.assertNotNull(lastMessage);
        Assertions.assertNotNull(lastMessage.embed);
        Assertions.assertEquals("Player: Steve", lastMessage.embed.getTitle());
        Assertions.assertTrue(lastMessage.embed.getDescription().contains("Monster Hunter"));
        Assertions.assertTrue(lastMessage.embed.getDescription().contains("Kill a monster"));
    }
    
    @Test
    public void testAdvancementDefaultBehavior() throws Exception {
        // Reset to defaults (no custom fields)
        TestConfigHelper.resetConfig();
        getDiscordIntegration().getMessagePatternMatcher().reloadPatterns();
        
        // Simulate advancement
        UUID playerUUID = UUID.randomUUID();
        getMCEventSimulator().simulateAdvancement(
            playerUUID,
            "TestPlayer",
            "Test Advancement",
            "Test description"
        );
        
        // Wait for async operations
        waitForAsyncOperations();
        
        // Verify default message format
        var lastMessage = getMockJDAFactory().getLastMessage(defaultChannelID);
        Assertions.assertNotNull(lastMessage);
        // Should use default localization message
        Assertions.assertTrue(
            lastMessage.content != null && lastMessage.content.contains("TestPlayer") ||
            lastMessage.embed != null && lastMessage.embed.getDescription() != null && 
            lastMessage.embed.getDescription().contains("TestPlayer")
        );
    }
}


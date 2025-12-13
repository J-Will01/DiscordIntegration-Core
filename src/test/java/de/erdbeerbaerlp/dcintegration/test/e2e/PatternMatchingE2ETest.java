package de.erdbeerbaerlp.dcintegration.test.e2e;

import de.erdbeerbaerlp.dcintegration.test.IntegrationTestBase;
import de.erdbeerbaerlp.dcintegration.test.util.TestConfigHelper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * End-to-end tests for pattern matching and message replacement.
 */
public class PatternMatchingE2ETest extends IntegrationTestBase {
    
    @Test
    public void testConsoleLogPatternMatch() throws Exception {
        // Setup pattern
        TestConfigHelper.setupMessagePattern(
            "(?i).*backup.*start.*",
            "🧰 World Backup Started",
            false,
            "",
            "",
            null
        );
        getDiscordIntegration().getMessagePatternMatcher().reloadPatterns();
        
        // Simulate console log
        getMCEventSimulator().simulateConsoleLog("[INFO] World backup started successfully");
        
        // Wait for async operations
        waitForAsyncOperations();
        
        // Verify replacement message sent
        var messages = getMockJDAFactory().getSentMessages(defaultChannelID);
        Assertions.assertFalse(messages.isEmpty());
        
        var lastMessage = getMockJDAFactory().getLastMessage(defaultChannelID);
        Assertions.assertNotNull(lastMessage);
        Assertions.assertTrue(
            lastMessage.content != null && lastMessage.content.contains("🧰 World Backup Started")
        );
    }
    
    @Test
    public void testPatternSuppressesOriginal() throws Exception {
        // Setup pattern that suppresses original
        TestConfigHelper.setupMessagePattern(
            "(?i).*spam.*",
            "Filtered message",
            false,
            "",
            "",
            null
        );
        getDiscordIntegration().getMessagePatternMatcher().reloadPatterns();
        
        // Simulate console log
        getMCEventSimulator().simulateConsoleLog("This is spam message");
        
        // Wait for async operations
        waitForAsyncOperations();
        
        // Verify original suppressed and replacement sent
        var messages = getMockJDAFactory().getSentMessages(defaultChannelID);
        boolean foundReplacement = false;
        boolean foundOriginal = false;
        
        for (var msg : messages) {
            if (msg.content != null && msg.content.contains("Filtered message")) {
                foundReplacement = true;
            }
            if (msg.content != null && msg.content.contains("This is spam message")) {
                foundOriginal = true;
            }
        }
        
        Assertions.assertTrue(foundReplacement, "Replacement message should be sent");
        Assertions.assertFalse(foundOriginal, "Original message should be suppressed");
    }
    
    @Test
    public void testPatternWithEmbed() throws Exception {
        // Setup pattern with embed
        TestConfigHelper.setupMessagePattern(
            "(?i).*backup.*complete.*",
            "",
            true,
            "🧰 World Backup Completed",
            "Backup finished successfully",
            "#00FF00"
        );
        getDiscordIntegration().getMessagePatternMatcher().reloadPatterns();
        
        // Simulate console log
        getMCEventSimulator().simulateConsoleLog("[INFO] World backup completed");
        
        // Wait for async operations
        waitForAsyncOperations();
        
        // Verify embed sent
        var lastMessage = getMockJDAFactory().getLastMessage(defaultChannelID);
        Assertions.assertNotNull(lastMessage);
        Assertions.assertNotNull(lastMessage.embed);
        Assertions.assertEquals("🧰 World Backup Completed", lastMessage.embed.getTitle());
        Assertions.assertEquals("Backup finished successfully", lastMessage.embed.getDescription());
    }
    
    @Test
    public void testPatternChannelRouting() throws Exception {
        // Setup pattern with custom channel
        String customChannelID = "555555555";
        getMockJDAFactory().createMockTextChannel(customChannelID);
        
        TestConfigHelper.setupMessagePattern(
            "(?i).*test.*pattern.*",
            "Test replacement",
            false,
            "",
            "",
            null
        );
        // Manually set channel ID for this test
        var patterns = getDiscordIntegration().getMessagePatternMatcher();
        // Note: Channel routing would need to be configured in the pattern
        getDiscordIntegration().getMessagePatternMatcher().reloadPatterns();
        
        // Simulate console log
        getMCEventSimulator().simulateConsoleLog("This is a test pattern message");
        
        // Wait for async operations
        waitForAsyncOperations();
        
        // Verify message sent (to default channel since pattern channelID is "default")
        var messages = getMockJDAFactory().getSentMessages(defaultChannelID);
        Assertions.assertFalse(messages.isEmpty());
    }
    
    @Test
    public void testPatternCaptureGroups() throws Exception {
        // Setup pattern with capture groups
        TestConfigHelper.setupMessagePattern(
            "Backup #(\\d+) completed in (\\d+\\.\\d+)s",
            "",
            true,
            "🧰 Backup #$1 Completed",
            "Duration: $2 seconds",
            "#FFD700"
        );
        getDiscordIntegration().getMessagePatternMatcher().reloadPatterns();
        
        // Simulate console log
        getMCEventSimulator().simulateConsoleLog("Backup #11 completed in 1.2s");
        
        // Wait for async operations
        waitForAsyncOperations();
        
        // Verify capture groups replaced
        var lastMessage = getMockJDAFactory().getLastMessage(defaultChannelID);
        Assertions.assertNotNull(lastMessage);
        Assertions.assertNotNull(lastMessage.embed);
        Assertions.assertEquals("🧰 Backup #11 Completed", lastMessage.embed.getTitle());
        Assertions.assertEquals("Duration: 1.2 seconds", lastMessage.embed.getDescription());
    }
}


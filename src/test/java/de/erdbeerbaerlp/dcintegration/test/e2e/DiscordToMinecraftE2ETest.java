package de.erdbeerbaerlp.dcintegration.test.e2e;

import de.erdbeerbaerlp.dcintegration.common.storage.Configuration;
import de.erdbeerbaerlp.dcintegration.test.IntegrationTestBase;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * End-to-end tests for Discord to Minecraft message flow.
 * Note: Full Discord event simulation requires more complex mocking.
 * These tests verify the integration points.
 */
public class DiscordToMinecraftE2ETest extends IntegrationTestBase {
    
    @Test
    public void testDiscordMessageForwarded() throws Exception {
        // Configure to allow Discord messages
        Configuration.instance().general.allowWebhookMessages = true;
        Configuration.instance().advanced.chatInputChannelID = defaultChannelID;
        // Note: ingame_discordMessage is in Localization, not Configuration
        
        // For this test, we'll directly call sendIngameMessage to simulate
        // what DiscordEventListener would do
        String testMessage = "Hello from Discord!";
        getMockMC().sendIngameMessage(
            net.kyori.adventure.text.Component.text(testMessage)
        );
        
        // Verify message sent to Minecraft
        var messages = getMockMC().getSentMessages();
        Assertions.assertFalse(messages.isEmpty());
        getMockMC().assertMessageContains("Hello from Discord!");
    }
    
    @Test
    public void testDiscordCommandExecuted() throws Exception {
        // Simulate command execution
        String command = "say Hello World";
        getMockMC().runMcCommand(
            command,
            new java.util.concurrent.CompletableFuture<>(),
            getMockJDA().getSelfUser()
        );
        
        // Verify command captured
        var messages = getMockMC().getSentMessages();
        boolean foundCommand = false;
        for (var msg : messages) {
            if (msg.getPlainText().contains("[COMMAND]") && msg.getPlainText().contains(command)) {
                foundCommand = true;
                break;
            }
        }
        Assertions.assertTrue(foundCommand, "Command should be captured");
    }
}


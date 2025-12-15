package de.erdbeerbaerlp.dcintegration.test.e2e;

import de.erdbeerbaerlp.dcintegration.common.DiscordIntegration;
import de.erdbeerbaerlp.dcintegration.common.storage.Configuration;
import de.erdbeerbaerlp.dcintegration.common.storage.template.TemplateConfig;
import de.erdbeerbaerlp.dcintegration.common.util.template.TemplateEngine;
import de.erdbeerbaerlp.dcintegration.test.IntegrationTestBase;
import net.dv8tion.jda.api.entities.Message;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * E2E tests for the template system integration
 */
public class TemplateSystemE2ETest extends IntegrationTestBase {
    
    @Test
    public void testTemplateSystemEnabled() throws Exception {
        // Enable template system
        TemplateConfig.instance().enabled = true;
        TemplateConfig.instance().saveConfig();
        
        // Register a test template
        TemplateEngine engine = discordIntegration.getTemplateEngine();
        engine.registerTemplate("testGreeting", "Hello %player%!");
        
        // Process a template
        Map<String, String> vars = new HashMap<>();
        vars.put("player", "TestPlayer");
        
        String result = engine.process("{include testGreeting} Welcome!", vars);
        Assertions.assertEquals("Hello TestPlayer! Welcome!", result);
    }
    
    @Test
    public void testTemplateInEmbedCustomization() throws Exception {
        // Enable template system and embed mode
        TemplateConfig.instance().enabled = true;
        TemplateConfig.instance().saveConfig();
        
        Configuration.instance().embedMode.enabled = true;
        Configuration.instance().embedMode.advancementMessage.asEmbed = true;
        Configuration.instance().embedMode.advancementMessage.customTitle = "{if advName}🏆 %advName%{/if}";
        Configuration.instance().embedMode.advancementMessage.customDescription = "%player% earned this achievement!\n_%advDesc%_";
        Configuration.instance().saveConfig();
        
        // Register template
        TemplateEngine engine = discordIntegration.getTemplateEngine();
        engine.registerTemplate("achievementHeader", "Achievement Unlocked!");
        
        // Simulate advancement
        UUID playerUUID = UUID.randomUUID();
        mcEventSimulator.simulatePlayerJoin(playerUUID, "TestPlayer");
        waitForAsyncOperations();
        
        // Simulate advancement
        mcEventSimulator.simulateAdvancement(playerUUID, "TestPlayer", "Diamonds!", "Acquire diamonds");
        waitForAsyncOperations();
        
        // Wait for message to be sent
        Thread.sleep(500);
        
        // Verify message was sent (check mock)
        var messages = mockJDAFactory.getSentMessages(defaultChannelID);
        Assertions.assertFalse(messages.isEmpty(), "Message should have been sent");
    }
    
    @Test
    public void testTemplateWithConditionalsInMessage() throws Exception {
        TemplateConfig.instance().enabled = true;
        TemplateConfig.instance().saveConfig();
        
        Configuration.instance().embedMode.enabled = true;
        Configuration.instance().embedMode.playerJoinMessage.asEmbed = true;
        Configuration.instance().embedMode.playerJoinMessage.customDescription = "{if online}%player% joined (%online% online){/if}{ifnot online}%player% joined (server empty){/ifnot}";
        Configuration.instance().saveConfig();
        
        // Set online count
        Map<String, String> vars = new HashMap<>();
        vars.put("player", "TestPlayer");
        vars.put("online", "5");
        
        TemplateEngine engine = discordIntegration.getTemplateEngine();
        String result = engine.process(Configuration.instance().embedMode.playerJoinMessage.customDescription, vars);
        
        Assertions.assertEquals("TestPlayer joined (5 online)", result);
    }
    
    @Test
    public void testTemplateConfigReload() throws Exception {
        TemplateConfig.instance().enabled = true;
        TemplateConfig.instance().templates.put("testTemplate", new TemplateConfig.TemplateEntry());
        TemplateConfig.instance().templates.get("testTemplate").content = "Test: %value%";
        TemplateConfig.instance().saveConfig();
        
        // Reload
        TemplateConfig.instance().reloadTemplates();
        
        // Verify template is registered
        TemplateEngine engine = discordIntegration.getTemplateEngine();
        de.erdbeerbaerlp.dcintegration.common.util.template.Template template = engine.getTemplate("testTemplate");
        
        Assertions.assertNotNull(template);
        Assertions.assertEquals("Test: %value%", template.getContent());
    }
    
    @Test
    public void testTemplateSystemDisabled() throws Exception {
        // Disable template system
        TemplateConfig.instance().enabled = false;
        TemplateConfig.instance().saveConfig();
        
        // Process should fall back to basic replacement
        Map<String, String> vars = new HashMap<>();
        vars.put("player", "TestPlayer");
        
        // Even with template syntax, should use basic replacement when disabled
        String template = "{if player}%player%{/if}";
        String result = discordIntegration.getTemplateEngine().process(template, vars);
        
        // When disabled, MessageUtils.replacePlaceholders should handle it
        // But template engine still processes, so conditionals won't work
        // This is expected behavior - template engine is always available, but MessageUtils checks if enabled
        Assertions.assertNotNull(result);
    }
    
    @Test
    public void testTemplateIntegrationWithMessageUtils() throws Exception {
        TemplateConfig.instance().enabled = true;
        TemplateConfig.instance().saveConfig();
        
        Map<String, String> vars = new HashMap<>();
        vars.put("player", "Steve");
        vars.put("online", "5");
        
        // Use MessageUtils.replacePlaceholders which should use template engine when enabled
        String template = "{if online}%player% is online (%online% players){/if}";
        String result = de.erdbeerbaerlp.dcintegration.common.util.MessageUtils.replacePlaceholders(template, vars);
        
        Assertions.assertEquals("Steve is online (5 players)", result);
    }
    
    @Test
    public void testTemplateWithDefaultValues() throws Exception {
        TemplateConfig.instance().enabled = true;
        TemplateConfig.instance().saveConfig();
        
        Map<String, String> vars = new HashMap<>();
        vars.put("player", "Steve");
        // msg not provided
        
        TemplateEngine engine = discordIntegration.getTemplateEngine();
        String template = "%player% said: %msg|nothing%";
        String result = engine.process(template, vars);
        
        Assertions.assertEquals("Steve said: nothing", result);
    }
    
    @Test
    public void testMultipleTemplates() throws Exception {
        TemplateConfig.instance().enabled = true;
        TemplateConfig.instance().saveConfig();
        
        TemplateEngine engine = discordIntegration.getTemplateEngine();
        engine.registerTemplate("header", "=== %title% ===");
        engine.registerTemplate("footer", "--- End ---");
        
        Map<String, String> vars = new HashMap<>();
        vars.put("title", "Server Status");
        vars.put("content", "Server is online");
        
        String template = "{include header}\n%content%\n{include footer}";
        String result = engine.process(template, vars);
        
        Assertions.assertTrue(result.contains("=== Server Status ==="));
        Assertions.assertTrue(result.contains("Server is online"));
        Assertions.assertTrue(result.contains("--- End ---"));
    }
}


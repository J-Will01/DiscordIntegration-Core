package de.erdbeerbaerlp.dcintegration.test;

import de.erdbeerbaerlp.dcintegration.common.DiscordIntegration;
import de.erdbeerbaerlp.dcintegration.common.storage.Configuration;
import net.dv8tion.jda.api.EmbedBuilder;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class EmbedCustomizationTests {

    @BeforeEach
    public void setup() throws IOException {
        // Create temp config file
        DiscordIntegration.configFile = File.createTempFile("testEmbedCustomization", "config");
        DiscordIntegration.configFile.deleteOnExit();
        Configuration.instance().loadConfig();
    }

    @Test
    public void testCustomTitleOnly() {
        Configuration.EmbedMode.EmbedEntry entry = Configuration.instance().embedMode.advancementMessage;
        entry.asEmbed = true;
        entry.customTitle = "🏆 : %player%";
        entry.customDescription = ""; // Empty
        
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("player", "TestPlayer");
        
        EmbedBuilder builder = entry.toEmbed();
        boolean applied = entry.applyCustomFields(builder, placeholders);
        
        Assertions.assertTrue(applied);
        Assertions.assertEquals("🏆 : TestPlayer", builder.build().getTitle());
        Assertions.assertNull(builder.build().getDescription()); // Should be null since customDescription is empty
    }

    @Test
    public void testCustomDescriptionOnly() {
        Configuration.EmbedMode.EmbedEntry entry = Configuration.instance().embedMode.advancementMessage;
        entry.asEmbed = true;
        entry.customTitle = ""; // Empty
        entry.customDescription = "**%advName%**\n_%advDesc%_";
        
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("advName", "Diamonds!");
        placeholders.put("advDesc", "Acquire diamonds");
        
        EmbedBuilder builder = entry.toEmbed();
        boolean applied = entry.applyCustomFields(builder, placeholders);
        
        Assertions.assertTrue(applied);
        Assertions.assertNull(builder.build().getTitle()); // Should be null since customTitle is empty
        Assertions.assertEquals("**Diamonds!**\n_Acquire diamonds_", builder.build().getDescription());
    }

    @Test
    public void testCustomTitleAndDescription() {
        Configuration.EmbedMode.EmbedEntry entry = Configuration.instance().embedMode.advancementMessage;
        entry.asEmbed = true;
        entry.customTitle = "🏆 : %player%";
        entry.customDescription = "**%advName%**\n_%advDesc%_";
        
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("player", "Steve");
        placeholders.put("advName", "Diamonds!");
        placeholders.put("advDesc", "Acquire diamonds");
        
        EmbedBuilder builder = entry.toEmbed();
        boolean applied = entry.applyCustomFields(builder, placeholders);
        
        Assertions.assertTrue(applied);
        Assertions.assertEquals("🏆 : Steve", builder.build().getTitle());
        Assertions.assertEquals("**Diamonds!**\n_Acquire diamonds_", builder.build().getDescription());
    }

    @Test
    public void testCustomFieldsWithAsEmbedFalse() {
        Configuration.EmbedMode.EmbedEntry entry = Configuration.instance().embedMode.advancementMessage;
        entry.asEmbed = false;
        entry.customTitle = "🏆 : %player%";
        entry.customDescription = "Test description";
        
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("player", "TestPlayer");
        
        EmbedBuilder builder = entry.toEmbed();
        boolean applied = entry.applyCustomFields(builder, placeholders);
        
        // Should not apply and log warning
        Assertions.assertFalse(applied);
        Assertions.assertNull(builder.build().getTitle());
        Assertions.assertNull(builder.build().getDescription());
    }

    @Test
    public void testNoCustomFields() {
        Configuration.EmbedMode.EmbedEntry entry = Configuration.instance().embedMode.advancementMessage;
        entry.asEmbed = true;
        entry.customTitle = "";
        entry.customDescription = "";
        
        Map<String, String> placeholders = new HashMap<>();
        
        EmbedBuilder builder = entry.toEmbed();
        boolean applied = entry.applyCustomFields(builder, placeholders);
        
        Assertions.assertFalse(applied);
    }

    @Test
    public void testTitleLengthTruncation() {
        Configuration.EmbedMode.EmbedEntry entry = Configuration.instance().embedMode.advancementMessage;
        entry.asEmbed = true;
        // Create a title longer than 256 chars
        entry.customTitle = "A".repeat(300);
        
        Map<String, String> placeholders = new HashMap<>();
        
        EmbedBuilder builder = entry.toEmbed();
        entry.applyCustomFields(builder, placeholders);
        
        // Should be truncated to 256 chars
        Assertions.assertEquals(256, builder.build().getTitle().length());
    }

    @Test
    public void testDescriptionLengthTruncation() {
        Configuration.EmbedMode.EmbedEntry entry = Configuration.instance().embedMode.advancementMessage;
        entry.asEmbed = true;
        // Create a description longer than 4096 chars
        entry.customDescription = "B".repeat(5000);
        
        Map<String, String> placeholders = new HashMap<>();
        
        EmbedBuilder builder = entry.toEmbed();
        entry.applyCustomFields(builder, placeholders);
        
        // Should be truncated to 4096 chars
        Assertions.assertEquals(4096, builder.build().getDescription().length());
    }
}


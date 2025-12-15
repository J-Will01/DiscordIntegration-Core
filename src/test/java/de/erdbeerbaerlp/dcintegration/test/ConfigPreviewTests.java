package de.erdbeerbaerlp.dcintegration.test;

import de.erdbeerbaerlp.dcintegration.common.discordCommands.CommandConfigPreview;
import de.erdbeerbaerlp.dcintegration.common.storage.Configuration;
import net.dv8tion.jda.api.EmbedBuilder;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.util.Map;

/**
 * Unit tests for config preview functionality
 */
public class ConfigPreviewTests {
    
    @BeforeEach
    public void setUp() {
        // Reset config to defaults
        Configuration.instance().embedMode.enabled = true;
        Configuration.instance().embedMode.advancementMessage.asEmbed = true;
        Configuration.instance().embedMode.advancementMessage.colorHexCode = "#FFD700";
        Configuration.instance().embedMode.advancementMessage.customTitle = "";
        Configuration.instance().embedMode.advancementMessage.customDescription = "";
    }
    
    @Test
    public void testGetEmbedEntry() {
        // Test all embed types
        Assertions.assertNotNull(CommandConfigPreview.getEmbedEntry("join"));
        Assertions.assertNotNull(CommandConfigPreview.getEmbedEntry("leave"));
        Assertions.assertNotNull(CommandConfigPreview.getEmbedEntry("death"));
        Assertions.assertNotNull(CommandConfigPreview.getEmbedEntry("advancement"));
        Assertions.assertNotNull(CommandConfigPreview.getEmbedEntry("chat"));
        Assertions.assertNotNull(CommandConfigPreview.getEmbedEntry("start"));
        Assertions.assertNotNull(CommandConfigPreview.getEmbedEntry("stop"));
        Assertions.assertNull(CommandConfigPreview.getEmbedEntry("invalid"));
    }
    
    @Test
    public void testGetSamplePlaceholders() {
        Map<String, String> placeholders;
        
        placeholders = CommandConfigPreview.getSamplePlaceholders("join");
        Assertions.assertTrue(placeholders.containsKey("player"));
        Assertions.assertTrue(placeholders.containsKey("msg"));
        
        placeholders = CommandConfigPreview.getSamplePlaceholders("advancement");
        Assertions.assertTrue(placeholders.containsKey("player"));
        Assertions.assertTrue(placeholders.containsKey("advName"));
        Assertions.assertTrue(placeholders.containsKey("advDesc"));
        
        placeholders = CommandConfigPreview.getSamplePlaceholders("death");
        Assertions.assertTrue(placeholders.containsKey("player"));
        Assertions.assertTrue(placeholders.containsKey("deathMessage"));
    }
    
    @Test
    public void testGeneratePreview() {
        Configuration.EmbedMode.EmbedEntry entry = Configuration.instance().embedMode.advancementMessage;
        entry.asEmbed = true;
        entry.customTitle = "🏆 %advName%";
        entry.customDescription = "%player% earned this achievement!";
        
        EmbedBuilder preview = CommandConfigPreview.generatePreview("advancement", entry);
        Assertions.assertNotNull(preview);
        
        // Check that title and description were applied
        Assertions.assertNotNull(preview.build().getTitle());
        Assertions.assertNotNull(preview.build().getDescription());
    }
    
    @Test
    public void testGeneratePreviewWithDefaultMessage() {
        Configuration.EmbedMode.EmbedEntry entry = Configuration.instance().embedMode.playerJoinMessage;
        entry.asEmbed = true;
        entry.customTitle = "";
        entry.customDescription = "";
        
        EmbedBuilder preview = CommandConfigPreview.generatePreview("join", entry);
        Assertions.assertNotNull(preview);
        // Should have description with default message
        Assertions.assertNotNull(preview.build().getDescription());
    }
    
    @Test
    public void testSaveField() {
        Configuration.EmbedMode.EmbedEntry entry = Configuration.instance().embedMode.advancementMessage;
        
        // Test saving title
        String originalTitle = entry.customTitle;
        CommandConfigPreview.saveField("advancement", "title", "Test Title");
        Assertions.assertEquals("Test Title", entry.customTitle);
        
        // Test saving description
        String originalDesc = entry.customDescription;
        CommandConfigPreview.saveField("advancement", "desc", "Test Description");
        Assertions.assertEquals("Test Description", entry.customDescription);
        
        // Test saving color
        String originalColor = entry.colorHexCode;
        CommandConfigPreview.saveField("advancement", "color", "#FF0000");
        Assertions.assertEquals("#FF0000", entry.colorHexCode);
        
        // Test invalid color (should not save)
        CommandConfigPreview.saveField("advancement", "color", "invalid");
        Assertions.assertEquals("#FF0000", entry.colorHexCode); // Should remain unchanged
        
        // Restore original values
        entry.customTitle = originalTitle;
        entry.customDescription = originalDesc;
        entry.colorHexCode = originalColor;
    }
    
    @Test
    public void testToggleEmbed() {
        Configuration.EmbedMode.EmbedEntry entry = Configuration.instance().embedMode.advancementMessage;
        boolean originalValue = entry.asEmbed;
        
        CommandConfigPreview.toggleEmbed("advancement");
        Assertions.assertEquals(!originalValue, entry.asEmbed);
        
        // Toggle back
        CommandConfigPreview.toggleEmbed("advancement");
        Assertions.assertEquals(originalValue, entry.asEmbed);
    }
    
    @Test
    public void testColorValidation() {
        Configuration.EmbedMode.EmbedEntry entry = Configuration.instance().embedMode.advancementMessage;
        String originalColor = entry.colorHexCode;
        
        // Valid color should save
        CommandConfigPreview.saveField("advancement", "color", "#00FF00");
        Assertions.assertEquals("#00FF00", entry.colorHexCode);
        
        // Invalid colors should not save
        CommandConfigPreview.saveField("advancement", "color", "not a color");
        Assertions.assertEquals("#00FF00", entry.colorHexCode); // Unchanged
        
        CommandConfigPreview.saveField("advancement", "color", "#GGG");
        Assertions.assertEquals("#00FF00", entry.colorHexCode); // Unchanged
        
        // Restore
        entry.colorHexCode = originalColor;
    }
}


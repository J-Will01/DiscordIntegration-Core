package de.erdbeerbaerlp.dcintegration.test.util;

import de.erdbeerbaerlp.dcintegration.common.storage.Configuration;

/**
 * Utilities to set up test configurations.
 */
public class TestConfigHelper {
    
    /**
     * Sets up custom advancement embed configuration
     */
    public static void setupCustomAdvancementEmbed(String title, String description) {
        Configuration.instance().embedMode.advancementMessage.asEmbed = true;
        Configuration.instance().embedMode.advancementMessage.customTitle = title;
        Configuration.instance().embedMode.advancementMessage.customDescription = description;
    }
    
    /**
     * Sets up a message pattern for testing
     */
    public static void setupMessagePattern(String pattern, String replacement, boolean asEmbed, 
                                          String embedTitle, String embedDescription, String embedColor) {
        Configuration.instance().messagePatterns.enabled = true;
        Configuration.MessagePatterns.MessagePattern newPattern = new Configuration.MessagePatterns.MessagePattern();
        newPattern.pattern = pattern;
        newPattern.replacement = replacement;
        newPattern.suppressOriginal = true;
        newPattern.asEmbed = asEmbed;
        newPattern.embedTitle = embedTitle;
        newPattern.embedDescription = embedDescription;
        newPattern.embedColor = embedColor != null ? embedColor : "#808080";
        newPattern.channelID = "default";
        
        Configuration.MessagePatterns.MessagePattern[] patterns = Configuration.instance().messagePatterns.patterns;
        Configuration.MessagePatterns.MessagePattern[] newPatterns = new Configuration.MessagePatterns.MessagePattern[patterns.length + 1];
        System.arraycopy(patterns, 0, newPatterns, 0, patterns.length);
        newPatterns[patterns.length] = newPattern;
        Configuration.instance().messagePatterns.patterns = newPatterns;
    }
    
    /**
     * Sets up channel routing configuration
     */
    public static void setupChannelRouting(String advancementChannel, String backupChannel) {
        if (advancementChannel != null) {
            Configuration.instance().advanced.advancementChannelID = advancementChannel;
        }
        // Note: backupChannelID doesn't exist in Configuration yet
        // It would be used if pattern matching channel routing is needed
    }
    
    /**
     * Resets configuration to defaults
     */
    public static void resetConfig() {
        Configuration.instance().embedMode.advancementMessage.customTitle = "";
        Configuration.instance().embedMode.advancementMessage.customDescription = "";
        Configuration.instance().messagePatterns.patterns = new Configuration.MessagePatterns.MessagePattern[0];
        Configuration.instance().messagePatterns.enabled = true;
        Configuration.instance().advanced.advancementChannelID = "default";
    }
}


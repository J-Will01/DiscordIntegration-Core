package de.erdbeerbaerlp.dcintegration.test;

import de.erdbeerbaerlp.dcintegration.common.DiscordIntegration;
import de.erdbeerbaerlp.dcintegration.common.storage.Configuration;
import de.erdbeerbaerlp.dcintegration.common.util.MessagePatternMatcher;
import de.erdbeerbaerlp.dcintegration.common.util.PatternMatchResult;
import de.erdbeerbaerlp.dcintegration.common.util.DiscordMessage;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;

public class PatternMatchingTests {

    @BeforeEach
    public void setup() throws IOException {
        // Create temp config file
        DiscordIntegration.configFile = File.createTempFile("testPatternMatching", "config");
        DiscordIntegration.configFile.deleteOnExit();
        Configuration.instance().loadConfig();
        Configuration.instance().messagePatterns.enabled = true;
    }

    @Test
    public void testPatternMatchingDisabled() {
        Configuration.instance().messagePatterns.enabled = false;
        MessagePatternMatcher matcher = new MessagePatternMatcher();
        matcher.reloadPatterns();
        
        PatternMatchResult result = matcher.processMessage("backup started");
        
        Assertions.assertFalse(result.isMatched());
    }

    @Test
    public void testBasicPatternMatch() {
        Configuration.MessagePatterns.MessagePattern pattern = new Configuration.MessagePatterns.MessagePattern();
        pattern.pattern = "(?i).*backup.*start.*";
        pattern.replacement = "🧰 World Backup Started";
        pattern.suppressOriginal = true;
        pattern.asEmbed = false;
        pattern.channelID = "default";
        
        Configuration.instance().messagePatterns.patterns = new Configuration.MessagePatterns.MessagePattern[]{pattern};
        
        MessagePatternMatcher matcher = new MessagePatternMatcher();
        matcher.reloadPatterns();
        
        PatternMatchResult result = matcher.processMessage("[INFO] World backup started successfully");
        
        Assertions.assertTrue(result.isMatched());
        Assertions.assertTrue(result.shouldSuppressOriginal());
        Assertions.assertTrue(result.hasReplacement());
        
        DiscordMessage replacement = result.buildReplacementMessage();
        Assertions.assertNotNull(replacement);
        Assertions.assertEquals("🧰 World Backup Started", replacement.getMessage());
    }

    @Test
    public void testPatternNoMatch() {
        Configuration.MessagePatterns.MessagePattern pattern = new Configuration.MessagePatterns.MessagePattern();
        pattern.pattern = "(?i).*backup.*start.*";
        pattern.replacement = "🧰 Backup Started";
        
        Configuration.instance().messagePatterns.patterns = new Configuration.MessagePatterns.MessagePattern[]{pattern};
        
        MessagePatternMatcher matcher = new MessagePatternMatcher();
        matcher.reloadPatterns();
        
        PatternMatchResult result = matcher.processMessage("Player joined the server");
        
        Assertions.assertFalse(result.isMatched());
    }

    @Test
    public void testCaptureGroupReplacement() {
        Configuration.MessagePatterns.MessagePattern pattern = new Configuration.MessagePatterns.MessagePattern();
        pattern.pattern = "Backup #(\\d+) completed";
        pattern.replacement = "🧰 Backup ID #$1 done";
        pattern.suppressOriginal = true;
        pattern.asEmbed = false;
        
        Configuration.instance().messagePatterns.patterns = new Configuration.MessagePatterns.MessagePattern[]{pattern};
        
        MessagePatternMatcher matcher = new MessagePatternMatcher();
        matcher.reloadPatterns();
        
        PatternMatchResult result = matcher.processMessage("Backup #42 completed");
        
        Assertions.assertTrue(result.isMatched());
        DiscordMessage replacement = result.buildReplacementMessage();
        Assertions.assertEquals("🧰 Backup ID #42 done", replacement.getMessage());
    }

    @Test
    public void testEmbedPattern() {
        Configuration.MessagePatterns.MessagePattern pattern = new Configuration.MessagePatterns.MessagePattern();
        pattern.pattern = "(?i).*backup.*complete.*";
        pattern.replacement = "";
        pattern.suppressOriginal = true;
        pattern.asEmbed = true;
        pattern.embedTitle = "🧰 World Backup Completed";
        pattern.embedDescription = "Backup finished successfully";
        pattern.embedColor = "#00FF00";
        pattern.channelID = "default";
        
        Configuration.instance().messagePatterns.patterns = new Configuration.MessagePatterns.MessagePattern[]{pattern};
        
        MessagePatternMatcher matcher = new MessagePatternMatcher();
        matcher.reloadPatterns();
        
        PatternMatchResult result = matcher.processMessage("[INFO] World backup completed");
        
        Assertions.assertTrue(result.isMatched());
        DiscordMessage replacement = result.buildReplacementMessage();
        Assertions.assertNotNull(replacement);
        Assertions.assertNotNull(replacement.getEmbed());
        Assertions.assertEquals("🧰 World Backup Completed", replacement.getEmbed().getTitle());
        Assertions.assertEquals("Backup finished successfully", replacement.getEmbed().getDescription());
        Assertions.assertEquals("", replacement.getMessage()); // Should be empty (embed-only)
    }

    @Test
    public void testCaptureGroupsInEmbed() {
        Configuration.MessagePatterns.MessagePattern pattern = new Configuration.MessagePatterns.MessagePattern();
        pattern.pattern = "Backup #(\\d+) completed in (\\d+\\.\\d+)s";
        pattern.replacement = "";
        pattern.suppressOriginal = true;
        pattern.asEmbed = true;
        pattern.embedTitle = "🧰 Backup #$1 Completed";
        pattern.embedDescription = "Duration: $2 seconds";
        pattern.embedColor = "#FFD700";
        
        Configuration.instance().messagePatterns.patterns = new Configuration.MessagePatterns.MessagePattern[]{pattern};
        
        MessagePatternMatcher matcher = new MessagePatternMatcher();
        matcher.reloadPatterns();
        
        PatternMatchResult result = matcher.processMessage("Backup #11 completed in 1.2s");
        
        Assertions.assertTrue(result.isMatched());
        DiscordMessage replacement = result.buildReplacementMessage();
        Assertions.assertNotNull(replacement);
        Assertions.assertEquals("🧰 Backup #11 Completed", replacement.getEmbed().getTitle());
        Assertions.assertEquals("Duration: 1.2 seconds", replacement.getEmbed().getDescription());
    }

    @Test
    public void testChannelRouting() {
        Configuration.MessagePatterns.MessagePattern pattern = new Configuration.MessagePatterns.MessagePattern();
        pattern.pattern = "(?i).*test.*";
        pattern.replacement = "Test message";
        pattern.channelID = "123456789"; // Specific channel
        
        Configuration.instance().messagePatterns.patterns = new Configuration.MessagePatterns.MessagePattern[]{pattern};
        
        MessagePatternMatcher matcher = new MessagePatternMatcher();
        matcher.reloadPatterns();
        
        PatternMatchResult result = matcher.processMessage("This is a test message");
        
        Assertions.assertEquals("123456789", result.getTargetChannelID("originalChannelID"));
    }

    @Test
    public void testChannelRoutingDefault() {
        Configuration.MessagePatterns.MessagePattern pattern = new Configuration.MessagePatterns.MessagePattern();
        pattern.pattern = "(?i).*test.*";
        pattern.replacement = "Test message";
        pattern.channelID = "default"; // Should preserve original
        
        Configuration.instance().messagePatterns.patterns = new Configuration.MessagePatterns.MessagePattern[]{pattern};
        
        MessagePatternMatcher matcher = new MessagePatternMatcher();
        matcher.reloadPatterns();
        
        PatternMatchResult result = matcher.processMessage("This is a test message");
        
        Assertions.assertEquals("originalChannelID", result.getTargetChannelID("originalChannelID"));
    }

    @Test
    public void testInvalidRegexPattern() {
        Configuration.MessagePatterns.MessagePattern pattern = new Configuration.MessagePatterns.MessagePattern();
        pattern.pattern = "[invalid regex (unclosed bracket";
        pattern.replacement = "Should not work";
        
        Configuration.instance().messagePatterns.patterns = new Configuration.MessagePatterns.MessagePattern[]{pattern};
        
        MessagePatternMatcher matcher = new MessagePatternMatcher();
        // Should not throw, should log warning and skip
        matcher.reloadPatterns();
        
        Assertions.assertEquals(0, matcher.getPatternCount());
    }

    @Test
    public void testPatternOrder() {
        // First pattern (more specific)
        Configuration.MessagePatterns.MessagePattern pattern1 = new Configuration.MessagePatterns.MessagePattern();
        pattern1.pattern = "(?i).*backup.*start.*";
        pattern1.replacement = "Pattern 1 matched";
        pattern1.suppressOriginal = true;
        
        // Second pattern (less specific)
        Configuration.MessagePatterns.MessagePattern pattern2 = new Configuration.MessagePatterns.MessagePattern();
        pattern2.pattern = "(?i).*backup.*";
        pattern2.replacement = "Pattern 2 matched";
        pattern2.suppressOriginal = true;
        
        Configuration.instance().messagePatterns.patterns = new Configuration.MessagePatterns.MessagePattern[]{pattern1, pattern2};
        
        MessagePatternMatcher matcher = new MessagePatternMatcher();
        matcher.reloadPatterns();
        
        PatternMatchResult result = matcher.processMessage("World backup started");
        
        // First pattern should match first
        Assertions.assertTrue(result.isMatched());
        DiscordMessage replacement = result.buildReplacementMessage();
        Assertions.assertEquals("Pattern 1 matched", replacement.getMessage());
    }

    @Test
    public void testSuppressOriginalFalse() {
        Configuration.MessagePatterns.MessagePattern pattern = new Configuration.MessagePatterns.MessagePattern();
        pattern.pattern = "(?i).*test.*";
        pattern.replacement = "Additional message";
        pattern.suppressOriginal = false; // Don't suppress
        
        Configuration.instance().messagePatterns.patterns = new Configuration.MessagePatterns.MessagePattern[]{pattern};
        
        MessagePatternMatcher matcher = new MessagePatternMatcher();
        matcher.reloadPatterns();
        
        PatternMatchResult result = matcher.processMessage("This is a test");
        
        Assertions.assertTrue(result.isMatched());
        Assertions.assertFalse(result.shouldSuppressOriginal()); // Should NOT suppress
    }

    @Test
    public void testEmptyReplacementSuppresses() {
        Configuration.MessagePatterns.MessagePattern pattern = new Configuration.MessagePatterns.MessagePattern();
        pattern.pattern = "(?i).*spam.*";
        pattern.replacement = ""; // Empty replacement
        pattern.suppressOriginal = true;
        
        Configuration.instance().messagePatterns.patterns = new Configuration.MessagePatterns.MessagePattern[]{pattern};
        
        MessagePatternMatcher matcher = new MessagePatternMatcher();
        matcher.reloadPatterns();
        
        PatternMatchResult result = matcher.processMessage("This is spam");
        
        Assertions.assertTrue(result.isMatched());
        Assertions.assertTrue(result.shouldSuppressOriginal());
        // Note: hasReplacement() checks for embedTitle, so this might return false
        // This test verifies the suppression behavior
    }

    @Test
    public void testNullAndEmptyMessages() {
        Configuration.MessagePatterns.MessagePattern pattern = new Configuration.MessagePatterns.MessagePattern();
        pattern.pattern = ".*";
        pattern.replacement = "Matched";
        
        Configuration.instance().messagePatterns.patterns = new Configuration.MessagePatterns.MessagePattern[]{pattern};
        
        MessagePatternMatcher matcher = new MessagePatternMatcher();
        matcher.reloadPatterns();
        
        // Should handle null and empty gracefully
        PatternMatchResult result1 = matcher.processMessage(null);
        PatternMatchResult result2 = matcher.processMessage("");
        
        Assertions.assertFalse(result1.isMatched());
        Assertions.assertFalse(result2.isMatched());
    }
}


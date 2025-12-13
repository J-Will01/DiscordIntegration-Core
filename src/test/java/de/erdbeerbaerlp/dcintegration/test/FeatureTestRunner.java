package de.erdbeerbaerlp.dcintegration.test;

import de.erdbeerbaerlp.dcintegration.common.DiscordIntegration;
import de.erdbeerbaerlp.dcintegration.common.storage.Configuration;
import de.erdbeerbaerlp.dcintegration.common.util.MessagePatternMatcher;
import de.erdbeerbaerlp.dcintegration.common.util.PatternMatchResult;
import de.erdbeerbaerlp.dcintegration.common.util.MessageUtils;
import net.dv8tion.jda.api.EmbedBuilder;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Standalone test runner for manual verification of new features.
 * Run this to quickly verify functionality before deploying to server.
 */
public class FeatureTestRunner {
    public static void main(String[] args) {
        System.out.println("=== Testing New Features ===\n");
        
        try {
            // Setup config
            DiscordIntegration.configFile = File.createTempFile("testRunner", "config");
            DiscordIntegration.configFile.deleteOnExit();
            Configuration.instance().loadConfig();
            
            // Test 1: Placeholder Replacement
            System.out.println("1. Testing Placeholder Replacement...");
            testPlaceholderReplacement();
            
            // Test 2: Embed Customization
            System.out.println("2. Testing Embed Customization...");
            testEmbedCustomization();
            
            // Test 3: Pattern Matching
            System.out.println("3. Testing Pattern Matching...");
            testPatternMatching();
            
            System.out.println("\n=== All Tests Complete ===");
            System.out.println("✓ All features appear to be working correctly!");
            
        } catch (Exception e) {
            System.err.println("\n✗ Test failed with error:");
            e.printStackTrace();
            System.exit(1);
        }
    }
    
    private static void testPlaceholderReplacement() {
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("player", "TestPlayer");
        placeholders.put("advName", "Diamonds!");
        placeholders.put("advDesc", "Acquire diamonds");
        
        String template = "🏆 : %player%\n**%advName%**\n_%advDesc%_";
        String result = MessageUtils.replacePlaceholders(template, placeholders);
        
        System.out.println("   Template: " + template);
        System.out.println("   Result: " + result);
        
        if (result.contains("TestPlayer") && result.contains("Diamonds!") && result.contains("Acquire diamonds")) {
            System.out.println("   ✓ Pass\n");
        } else {
            System.out.println("   ✗ Fail - Placeholders not replaced correctly\n");
            throw new RuntimeException("Placeholder replacement test failed");
        }
    }
    
    private static void testEmbedCustomization() throws IOException {
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
        
        System.out.println("   Custom fields applied: " + applied);
        System.out.println("   Title: " + builder.build().getTitle());
        System.out.println("   Description: " + builder.build().getDescription());
        
        if (applied && 
            "🏆 : Steve".equals(builder.build().getTitle()) &&
            builder.build().getDescription() != null && 
            builder.build().getDescription().contains("Diamonds!")) {
            System.out.println("   ✓ Pass\n");
        } else {
            System.out.println("   ✗ Fail - Custom fields not applied correctly\n");
            throw new RuntimeException("Embed customization test failed");
        }
    }
    
    private static void testPatternMatching() {
        Configuration.instance().messagePatterns.enabled = true;
        
        Configuration.MessagePatterns.MessagePattern pattern = new Configuration.MessagePatterns.MessagePattern();
        pattern.pattern = "(?i).*backup.*complete.*";
        pattern.replacement = "";
        pattern.suppressOriginal = true;
        pattern.asEmbed = true;
        pattern.embedTitle = "🧰 Backup #$1 Completed";
        pattern.embedDescription = "Backup finished successfully";
        pattern.embedColor = "#00FF00";
        
        Configuration.instance().messagePatterns.patterns = new Configuration.MessagePatterns.MessagePattern[]{pattern};
        
        MessagePatternMatcher matcher = new MessagePatternMatcher();
        matcher.reloadPatterns();
        System.out.println("   Loaded " + matcher.getPatternCount() + " pattern(s)");
        
        PatternMatchResult matchResult = matcher.processMessage("World backup completed");
        System.out.println("   Pattern matched: " + matchResult.isMatched());
        System.out.println("   Should suppress: " + matchResult.shouldSuppressOriginal());
        System.out.println("   Has replacement: " + matchResult.hasReplacement());
        
        if (matchResult.hasReplacement()) {
            var replacement = matchResult.buildReplacementMessage();
            if (replacement != null && replacement.getEmbed() != null) {
                System.out.println("   Replacement embed title: " + replacement.getEmbed().getTitle());
                System.out.println("   Replacement embed description: " + replacement.getEmbed().getDescription());
            }
        }
        
        if (matchResult.isMatched() && matchResult.shouldSuppressOriginal() && matchResult.hasReplacement()) {
            System.out.println("   ✓ Pass\n");
        } else {
            System.out.println("   ✗ Fail - Pattern matching not working correctly\n");
            throw new RuntimeException("Pattern matching test failed");
        }
    }
}


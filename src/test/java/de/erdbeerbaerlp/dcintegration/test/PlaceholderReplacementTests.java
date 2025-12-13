package de.erdbeerbaerlp.dcintegration.test;

import de.erdbeerbaerlp.dcintegration.common.util.MessageUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

public class PlaceholderReplacementTests {

    @Test
    public void testBasicPlaceholderReplacement() {
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("player", "TestPlayer");
        placeholders.put("msg", "Hello World");
        
        String template = "%player% said: %msg%";
        String result = MessageUtils.replacePlaceholders(template, placeholders);
        
        Assertions.assertEquals("TestPlayer said: Hello World", result);
    }

    @Test
    public void testAdvancementPlaceholders() {
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("player", "Steve");
        placeholders.put("advName", "Diamonds!");
        placeholders.put("advDesc", "Acquire diamonds");
        
        String template = "🏆 : %player%\n**%advName%**\n_%advDesc%_";
        String result = MessageUtils.replacePlaceholders(template, placeholders);
        
        Assertions.assertEquals("🏆 : Steve\n**Diamonds!**\n_Acquire diamonds_", result);
    }

    @Test
    public void testUnknownPlaceholder() {
        // This should log a warning but leave the placeholder as-is
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("player", "TestPlayer");
        
        String template = "%player% has %unknownPlaceholder% items";
        String result = MessageUtils.replacePlaceholders(template, placeholders);
        
        // Unknown placeholder should remain as literal text
        Assertions.assertTrue(result.contains("%unknownPlaceholder%"));
        Assertions.assertTrue(result.contains("TestPlayer"));
    }

    @Test
    public void testEmptyTemplate() {
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("player", "TestPlayer");
        
        String result1 = MessageUtils.replacePlaceholders("", placeholders);
        String result2 = MessageUtils.replacePlaceholders(null, placeholders);
        
        Assertions.assertEquals("", result1);
        Assertions.assertNull(result2);
    }

    @Test
    public void testMultipleSamePlaceholder() {
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("player", "Alice");
        
        String template = "%player% and %player% are friends";
        String result = MessageUtils.replacePlaceholders(template, placeholders);
        
        Assertions.assertEquals("Alice and Alice are friends", result);
    }
}


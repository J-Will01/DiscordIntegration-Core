package de.erdbeerbaerlp.dcintegration.test;

import de.erdbeerbaerlp.dcintegration.common.util.template.Template;
import de.erdbeerbaerlp.dcintegration.common.util.template.TemplateEngine;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * Unit tests for TemplateEngine
 */
public class TemplateEngineTests {
    private TemplateEngine engine;
    
    @BeforeEach
    public void setUp() {
        engine = new TemplateEngine();
    }
    
    @Test
    public void testBasicVariableReplacement() {
        Map<String, String> vars = new HashMap<>();
        vars.put("player", "Steve");
        vars.put("msg", "Hello World");
        
        String template = "%player% said: %msg%";
        String result = engine.process(template, vars);
        
        Assertions.assertEquals("Steve said: Hello World", result);
    }
    
    @Test
    public void testVariableWithDefaultValue() {
        Map<String, String> vars = new HashMap<>();
        vars.put("player", "Steve");
        // msg not provided
        
        String template = "%player% said: %msg|nothing%";
        String result = engine.process(template, vars);
        
        Assertions.assertEquals("Steve said: nothing", result);
    }
    
    @Test
    public void testVariableWithoutDefault() {
        Map<String, String> vars = new HashMap<>();
        vars.put("player", "Steve");
        
        String template = "%player% said: %msg%";
        String result = engine.process(template, vars);
        
        Assertions.assertEquals("Steve said: ", result);
    }
    
    @Test
    public void testIfConditionalTrue() {
        Map<String, String> vars = new HashMap<>();
        vars.put("online", "5");
        vars.put("player", "Steve");
        
        String template = "{if online}%player% is online (%online% players){/if}";
        String result = engine.process(template, vars);
        
        Assertions.assertEquals("Steve is online (5 players)", result);
    }
    
    @Test
    public void testIfConditionalFalse() {
        Map<String, String> vars = new HashMap<>();
        vars.put("online", "");
        vars.put("player", "Steve");
        
        String template = "{if online}%player% is online{/if}";
        String result = engine.process(template, vars);
        
        Assertions.assertEquals("", result);
    }
    
    @Test
    public void testIfConditionalWithFalseValue() {
        Map<String, String> vars = new HashMap<>();
        vars.put("enabled", "false");
        
        String template = "{if enabled}Enabled{/if}";
        String result = engine.process(template, vars);
        
        Assertions.assertEquals("", result);
    }
    
    @Test
    public void testIfNotConditional() {
        Map<String, String> vars = new HashMap<>();
        vars.put("online", "");
        
        String template = "{ifnot online}No one is online{/ifnot}";
        String result = engine.process(template, vars);
        
        Assertions.assertEquals("No one is online", result);
    }
    
    @Test
    public void testIfNotConditionalFalse() {
        Map<String, String> vars = new HashMap<>();
        vars.put("online", "5");
        
        String template = "{ifnot online}No one is online{/ifnot}";
        String result = engine.process(template, vars);
        
        Assertions.assertEquals("", result);
    }
    
    @Test
    public void testConditionalEquality() {
        Map<String, String> vars = new HashMap<>();
        vars.put("status", "online");
        
        String template = "{if status == \"online\"}Server is online{/if}";
        String result = engine.process(template, vars);
        
        Assertions.assertEquals("Server is online", result);
    }
    
    @Test
    public void testConditionalInequality() {
        Map<String, String> vars = new HashMap<>();
        vars.put("status", "offline");
        
        String template = "{if status != \"online\"}Server is not online{/if}";
        String result = engine.process(template, vars);
        
        Assertions.assertEquals("Server is not online", result);
    }
    
    @Test
    public void testTemplateInclude() {
        engine.registerTemplate("greeting", "Hello %player%!");
        
        Map<String, String> vars = new HashMap<>();
        vars.put("player", "Steve");
        
        String template = "{include greeting} Welcome to the server!";
        String result = engine.process(template, vars);
        
        Assertions.assertEquals("Hello Steve! Welcome to the server!", result);
    }
    
    @Test
    public void testNestedIncludes() {
        engine.registerTemplate("header", "=== %title% ===");
        engine.registerTemplate("message", "{include header}\n%content%");
        
        Map<String, String> vars = new HashMap<>();
        vars.put("title", "Server Status");
        vars.put("content", "Server is online");
        
        String template = "{include message}";
        String result = engine.process(template, vars);
        
        Assertions.assertEquals("=== Server Status ===\nServer is online", result);
    }
    
    @Test
    public void testComplexTemplate() {
        engine.registerTemplate("playerInfo", "Player: %player%");
        
        Map<String, String> vars = new HashMap<>();
        vars.put("player", "Steve");
        vars.put("online", "5");
        vars.put("max", "20");
        
        String template = "{if online}{include playerInfo} (%online%/%max% online){/if}";
        String result = engine.process(template, vars);
        
        Assertions.assertEquals("Player: Steve (5/20 online)", result);
    }
    
    @Test
    public void testMultipleConditionals() {
        Map<String, String> vars = new HashMap<>();
        vars.put("hasAdvancement", "true");
        vars.put("player", "Steve");
        vars.put("advName", "Diamonds!");
        
        String template = "{if hasAdvancement}%player% earned %advName%{/if}{ifnot hasAdvancement}No advancement{/ifnot}";
        String result = engine.process(template, vars);
        
        Assertions.assertEquals("Steve earned Diamonds!", result);
    }
    
    @Test
    public void testEmptyTemplate() {
        Map<String, String> vars = new HashMap<>();
        
        String result1 = engine.process("", vars);
        String result2 = engine.process(null, vars);
        
        Assertions.assertEquals("", result1);
        Assertions.assertNull(result2);
    }
    
    @Test
    public void testTemplateRegistration() {
        engine.registerTemplate("test", "Hello World");
        Template template = engine.getTemplate("test");
        
        Assertions.assertNotNull(template);
        Assertions.assertEquals("test", template.getName());
        Assertions.assertEquals("Hello World", template.getContent());
    }
    
    @Test
    public void testTemplateUnregistration() {
        engine.registerTemplate("test", "Hello World");
        engine.unregisterTemplate("test");
        
        Template template = engine.getTemplate("test");
        Assertions.assertNull(template);
    }
    
    @Test
    public void testCaseInsensitiveTemplateNames() {
        engine.registerTemplate("Test", "Hello World");
        Template template1 = engine.getTemplate("test");
        Template template2 = engine.getTemplate("TEST");
        
        Assertions.assertNotNull(template1);
        Assertions.assertNotNull(template2);
        Assertions.assertEquals(template1, template2);
    }
    
    @Test
    public void testMultilineConditional() {
        Map<String, String> vars = new HashMap<>();
        vars.put("online", "5");
        
        String template = "{if online}\n" +
                         "Players online: %online%\n" +
                         "Server is active\n" +
                         "{/if}";
        String result = engine.process(template, vars);
        
        Assertions.assertTrue(result.contains("Players online: 5"));
        Assertions.assertTrue(result.contains("Server is active"));
    }
    
    @Test
    public void testNestedConditionals() {
        Map<String, String> vars = new HashMap<>();
        vars.put("online", "5");
        vars.put("player", "Steve");
        
        String template = "{if online}{if player}%player% is online (%online% players){/if}{/if}";
        String result = engine.process(template, vars);
        
        Assertions.assertEquals("Steve is online (5 players)", result);
    }
}


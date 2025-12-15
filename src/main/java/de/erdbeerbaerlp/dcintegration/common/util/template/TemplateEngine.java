package de.erdbeerbaerlp.dcintegration.common.util.template;

import de.erdbeerbaerlp.dcintegration.common.DiscordIntegration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Advanced template engine supporting variables, conditionals, and reusable templates.
 * 
 * <p>Features:
 * <ul>
 *   <li>Variable replacement: %variable%</li>
 *   <li>Conditionals: {if variable}...{/if}, {ifnot variable}...{/ifnot}</li>
 *   <li>Template includes: {include templateName}</li>
 *   <li>Default values: %variable|default%</li>
 * </ul>
 * 
 * <p>Example:
 * <pre>{@code
 * TemplateEngine engine = new TemplateEngine();
 * Map<String, String> vars = new HashMap<>();
 * vars.put("player", "Steve");
 * vars.put("online", "5");
 * 
 * String template = "{if online}%player% is online ({online} players){/if}";
 * String result = engine.process(template, vars);
 * // Result: "Steve is online (5 players)"
 * }</pre>
 */
public class TemplateEngine {
    private static final Pattern VARIABLE_PATTERN = Pattern.compile("%([^%|]+)(?:\\|([^%]+))?%");
    // Match {if condition}...{/if} - condition is everything until first }, content is everything until {/if}
    private static final Pattern CONDITIONAL_PATTERN = Pattern.compile("\\{if\\s+([^}]+)\\}(.*?)\\{/if\\}", Pattern.DOTALL);
    private static final Pattern CONDITIONAL_NOT_PATTERN = Pattern.compile("\\{ifnot\\s+([^}]+)\\}(.*?)\\{/ifnot\\}", Pattern.DOTALL);
    private static final Pattern INCLUDE_PATTERN = Pattern.compile("\\{include\\s+([^}]+)\\}");
    
    private final Map<String, Template> registeredTemplates = new HashMap<>();
    
    /**
     * Registers a template for reuse via {include templateName}
     * 
     * @param name Template name
     * @param content Template content
     */
    public void registerTemplate(@NotNull String name, @NotNull String content) {
        registeredTemplates.put(name.toLowerCase(), new Template(name, content));
    }
    
    /**
     * Registers a template object
     * 
     * @param template Template to register
     */
    public void registerTemplate(@NotNull Template template) {
        registeredTemplates.put(template.getName().toLowerCase(), template);
    }
    
    /**
     * Unregisters a template
     * 
     * @param name Template name
     */
    public void unregisterTemplate(@NotNull String name) {
        registeredTemplates.remove(name.toLowerCase());
    }
    
    /**
     * Gets a registered template
     * 
     * @param name Template name
     * @return Template or null if not found
     */
    @Nullable
    public Template getTemplate(@NotNull String name) {
        return registeredTemplates.get(name.toLowerCase());
    }
    
    /**
     * Processes a template string with the given variables
     * 
     * @param template Template string
     * @param variables Variable map
     * @return Processed string
     */
    @NotNull
    public String process(@NotNull String template, @NotNull Map<String, String> variables) {
        if (template == null || template.isEmpty()) {
            return template;
        }
        
        String result = template;
        
        // Process includes first (before other processing)
        result = processIncludes(result, variables);
        
        // Process conditionals
        result = processConditionals(result, variables);
        
        // Process variables (with default values)
        result = processVariables(result, variables);
        
        return result;
    }
    
    /**
     * Processes template includes: {include templateName}
     */
    @NotNull
    private String processIncludes(@NotNull String template, @NotNull Map<String, String> variables) {
        String result = template;
        Matcher matcher = INCLUDE_PATTERN.matcher(result);
        
        while (matcher.find()) {
            String templateName = matcher.group(1).trim().toLowerCase();
            Template includedTemplate = registeredTemplates.get(templateName);
            
            if (includedTemplate != null) {
                // Recursively process the included template
                String includedContent = process(includedTemplate.getContent(), variables);
                result = result.replace(matcher.group(0), includedContent);
            } else {
                DiscordIntegration.LOGGER.warn("Template '{}' not found for include, leaving as-is", templateName);
            }
        }
        
        return result;
    }
    
    /**
     * Processes conditionals: {if variable}...{/if} and {ifnot variable}...{/ifnot}
     * Processes iteratively to handle nested conditionals
     */
    @NotNull
    private String processConditionals(@NotNull String template, @NotNull Map<String, String> variables) {
        String result = template;
        String previousResult;
        int maxIterations = 10; // Prevent infinite loops
        int iterations = 0;
        
        // Keep processing until no more conditionals are found (handles nesting)
        do {
            previousResult = result;
            
            // Process {if variable}...{/if} - use StringBuffer for proper replacement
            Matcher ifMatcher = CONDITIONAL_PATTERN.matcher(result);
            StringBuffer ifBuffer = new StringBuffer();
            while (ifMatcher.find()) {
                String condition = ifMatcher.group(1).trim();
                String content = ifMatcher.group(2);
                
                String replacement = evaluateCondition(condition, variables) ? content : "";
                ifMatcher.appendReplacement(ifBuffer, Matcher.quoteReplacement(replacement));
            }
            ifMatcher.appendTail(ifBuffer);
            result = ifBuffer.toString();
            
            // Process {ifnot variable}...{/ifnot}
            Matcher ifNotMatcher = CONDITIONAL_NOT_PATTERN.matcher(result);
            StringBuffer ifNotBuffer = new StringBuffer();
            while (ifNotMatcher.find()) {
                String condition = ifNotMatcher.group(1).trim();
                String content = ifNotMatcher.group(2);
                
                String replacement = !evaluateCondition(condition, variables) ? content : "";
                ifNotMatcher.appendReplacement(ifNotBuffer, Matcher.quoteReplacement(replacement));
            }
            ifNotMatcher.appendTail(ifNotBuffer);
            result = ifNotBuffer.toString();
            
            iterations++;
        } while (!result.equals(previousResult) && iterations < maxIterations);
        
        return result;
    }
    
    /**
     * Evaluates a conditional expression
     * 
     * @param condition Condition to evaluate (variable name or expression)
     * @param variables Variable map
     * @return true if condition is true
     */
    private boolean evaluateCondition(@NotNull String condition, @NotNull Map<String, String> variables) {
        condition = condition.trim();
        
        // Check if it's a simple variable check
        if (variables.containsKey(condition)) {
            String value = variables.get(condition);
            // Consider empty string, "false", "0", "no" as false
            return value != null && 
                   !value.isEmpty() && 
                   !value.equalsIgnoreCase("false") && 
                   !value.equalsIgnoreCase("0") && 
                   !value.equalsIgnoreCase("no");
        }
        
        // Check for comparison operators: variable == value, variable != value
        if (condition.contains("==")) {
            String[] parts = condition.split("==", 2);
            if (parts.length == 2) {
                String var = parts[0].trim();
                String expected = parts[1].trim().replaceAll("^[\"']|[\"']$", ""); // Remove quotes
                String actual = variables.getOrDefault(var, "");
                return actual.equals(expected);
            }
        }
        
        if (condition.contains("!=")) {
            String[] parts = condition.split("!=", 2);
            if (parts.length == 2) {
                String var = parts[0].trim();
                String expected = parts[1].trim().replaceAll("^[\"']|[\"']$", "");
                String actual = variables.getOrDefault(var, "");
                return !actual.equals(expected);
            }
        }
        
        // Default: check if variable exists and is not empty
        return variables.containsKey(condition) && 
               variables.get(condition) != null && 
               !variables.get(condition).isEmpty();
    }
    
    /**
     * Processes variables: %variable% or %variable|default%
     */
    @NotNull
    private String processVariables(@NotNull String template, @NotNull Map<String, String> variables) {
        String result = template;
        Matcher matcher = VARIABLE_PATTERN.matcher(result);
        
        while (matcher.find()) {
            String varName = matcher.group(1).trim();
            String defaultValue = matcher.group(2); // May be null
            
            String value = variables.get(varName);
            if (value == null || value.isEmpty()) {
                value = defaultValue != null ? defaultValue : "";
            }
            
            result = result.replace(matcher.group(0), value);
        }
        
        return result;
    }
    
    /**
     * Gets all registered template names
     * 
     * @return Set of template names
     */
    @NotNull
    public Set<String> getRegisteredTemplateNames() {
        return new HashSet<>(registeredTemplates.keySet());
    }
    
    /**
     * Clears all registered templates
     */
    public void clearTemplates() {
        registeredTemplates.clear();
    }
}


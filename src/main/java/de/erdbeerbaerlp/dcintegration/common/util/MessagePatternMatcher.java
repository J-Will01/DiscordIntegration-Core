package de.erdbeerbaerlp.dcintegration.common.util;

import de.erdbeerbaerlp.dcintegration.common.DiscordIntegration;
import de.erdbeerbaerlp.dcintegration.common.storage.Configuration;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Utility class for matching messages against configured regex patterns.
 * Patterns are compiled once on load for performance.
 * 
 * <p>Usage in platform-specific code (console log forwarding):</p>
 * <pre>{@code
 * String logLine = ...; // raw console/log output
 * PatternMatchResult result = messagePatternMatcher.processMessage(logLine);
 * 
 * if (result.isMatched()) {
 *     if (result.shouldSuppressOriginal()) {
 *         // Skip forwarding original message
 *     }
 *     if (result.hasReplacement()) {
 *         String targetChannelID = result.getTargetChannelID(originalChannelID);
 *         DiscordMessage replacement = result.buildReplacementMessage();
 *         // Send replacement to targetChannelID
 *     }
 * } else {
 *     // Forward original message normally
 * }
 * }</pre>
 */
public class MessagePatternMatcher {
    private final List<CompiledPattern> compiledPatterns = new ArrayList<>();

    /**
     * Internal class to hold compiled regex pattern with its configuration.
     */
    private static class CompiledPattern {
        final Pattern regex;
        final Configuration.MessagePatterns.MessagePattern config;

        CompiledPattern(Pattern regex, Configuration.MessagePatterns.MessagePattern config) {
            this.regex = regex;
            this.config = config;
        }
    }

    /**
     * Processes a log/console message against all configured patterns.
     * Returns first matching pattern result.
     *
     * @param originalMessage The original message line to check
     * @return PatternMatchResult indicating if message was matched and what to do
     */
    public PatternMatchResult processMessage(String originalMessage) {
        if (originalMessage == null || originalMessage.isEmpty()) {
            return PatternMatchResult.noMatch();
        }

        if (!Configuration.instance().messagePatterns.enabled) {
            return PatternMatchResult.noMatch();
        }

        for (CompiledPattern compiled : compiledPatterns) {
            try {
                java.util.regex.Matcher matcher = compiled.regex.matcher(originalMessage);
                // Try both matches() (full match) and find() (partial match)
                if (matcher.matches() || matcher.find()) {
                    // Reset matcher to start to allow replaceAll to work properly
                    matcher.reset();
                    matcher.find();
                    return PatternMatchResult.match(compiled.config, originalMessage, matcher);
                }
            } catch (Exception e) {
                DiscordIntegration.LOGGER.warn("Error matching pattern against message", e);
            }
        }

        return PatternMatchResult.noMatch();
    }

    /**
     * Reloads patterns from configuration.
     * Should be called after configuration is loaded or reloaded.
     */
    public void reloadPatterns() {
        compiledPatterns.clear();

        if (!Configuration.instance().messagePatterns.enabled) {
            return;
        }

        Configuration.MessagePatterns.MessagePattern[] patterns = Configuration.instance().messagePatterns.patterns;
        if (patterns == null) {
            return;
        }

        for (Configuration.MessagePatterns.MessagePattern pattern : patterns) {
            if (pattern == null || pattern.pattern == null || pattern.pattern.isEmpty()) {
                continue;
            }

            try {
                // Compile pattern with case-insensitive flag
                Pattern regex = Pattern.compile(pattern.pattern, Pattern.CASE_INSENSITIVE);
                compiledPatterns.add(new CompiledPattern(regex, pattern));
            } catch (PatternSyntaxException e) {
                DiscordIntegration.LOGGER.warn("Invalid regex pattern '{}', skipping: {}", pattern.pattern, e.getMessage());
            } catch (Exception e) {
                DiscordIntegration.LOGGER.warn("Error compiling pattern '{}'", pattern.pattern, e);
            }
        }

        DiscordIntegration.LOGGER.info("Loaded {} message pattern(s)", compiledPatterns.size());
    }

    /**
     * Gets the number of currently loaded patterns.
     *
     * @return Number of compiled patterns
     */
    public int getPatternCount() {
        return compiledPatterns.size();
    }
}


package de.erdbeerbaerlp.dcintegration.common.util;

import de.erdbeerbaerlp.dcintegration.common.DiscordIntegration;
import de.erdbeerbaerlp.dcintegration.common.storage.Configuration;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;

import java.awt.Color;
import java.util.regex.Matcher;

/**
 * Represents the result of processing a message against pattern matchers.
 */
public class PatternMatchResult {
    private final boolean matched;
    private final Configuration.MessagePatterns.MessagePattern pattern;
    private final String originalMessage;
    private final Matcher matcher;

    private PatternMatchResult(boolean matched, Configuration.MessagePatterns.MessagePattern pattern,
                              String originalMessage, Matcher matcher) {
        this.matched = matched;
        this.pattern = pattern;
        this.originalMessage = originalMessage;
        this.matcher = matcher;
    }

    /**
     * Creates a result indicating no match was found.
     *
     * @return PatternMatchResult with matched = false
     */
    public static PatternMatchResult noMatch() {
        return new PatternMatchResult(false, null, null, null);
    }

    /**
     * Creates a result indicating a match was found.
     *
     * @param pattern         The matching pattern configuration
     * @param originalMessage The original message that was matched
     * @param matcher         The Matcher that found the match
     * @return PatternMatchResult with matched = true
     */
    public static PatternMatchResult match(Configuration.MessagePatterns.MessagePattern pattern,
                                          String originalMessage, Matcher matcher) {
        return new PatternMatchResult(true, pattern, originalMessage, matcher);
    }

    /**
     * Whether a match was found.
     *
     * @return true if matched, false otherwise
     */
    public boolean isMatched() {
        return matched;
    }

    /**
     * Whether the original message should be suppressed.
     *
     * @return true if original should be suppressed, false otherwise
     */
    public boolean shouldSuppressOriginal() {
        return matched && pattern != null && pattern.suppressOriginal;
    }

    /**
     * Whether this result has a replacement message to send.
     *
     * @return true if replacement exists, false otherwise
     */
    public boolean hasReplacement() {
        return matched && pattern != null &&
                (pattern.replacement != null && !pattern.replacement.isEmpty() ||
                 (pattern.asEmbed && ((pattern.embedTitle != null && !pattern.embedTitle.isEmpty()) ||
                                      (pattern.embedDescription != null && !pattern.embedDescription.isEmpty()))));
    }

    /**
     * Gets the target channel ID for the replacement message.
     * If pattern's channelID is "default", returns the originalChannelID.
     * Otherwise returns the pattern's channelID.
     *
     * @param originalChannelID The original message's intended channel ID
     * @return Target channel ID for replacement message
     */
    public String getTargetChannelID(String originalChannelID) {
        if (!matched || pattern == null) {
            return originalChannelID;
        }
        if ("default".equals(pattern.channelID)) {
            return originalChannelID;
        }
        return pattern.channelID;
    }

    /**
     * Builds the replacement DiscordMessage based on the pattern configuration.
     * Applies capture group replacement using Matcher.replaceAll().
     *
     * @return DiscordMessage to send, or null if no replacement configured
     */
    public DiscordMessage buildReplacementMessage() {
        if (!matched || pattern == null || !hasReplacement()) {
            return null;
        }

        try {
            if (pattern.asEmbed) {
                // Build embed message
                EmbedBuilder embedBuilder = new EmbedBuilder();
                
                // Set color
                try {
                    embedBuilder.setColor(Color.decode(pattern.embedColor));
                } catch (NumberFormatException e) {
                    DiscordIntegration.LOGGER.warn("Invalid embed color '{}' in pattern, using default gray", pattern.embedColor);
                    embedBuilder.setColor(Color.GRAY);
                }

                // Apply title with capture group replacement
                if (pattern.embedTitle != null && !pattern.embedTitle.isEmpty()) {
                    String title = matcher.replaceAll(pattern.embedTitle);
                    // Discord API limit: title max 256 chars
                    if (title.length() > 256) {
                        DiscordIntegration.LOGGER.warn("Pattern embed title exceeds 256 character limit, truncating");
                        title = title.substring(0, 256);
                    }
                    embedBuilder.setTitle(title);
                }

                // Apply description with capture group replacement
                if (pattern.embedDescription != null && !pattern.embedDescription.isEmpty()) {
                    String description = matcher.replaceAll(pattern.embedDescription);
                    // Discord API limit: description max 4096 chars
                    if (description.length() > 4096) {
                        DiscordIntegration.LOGGER.warn("Pattern embed description exceeds 4096 character limit, truncating");
                        description = description.substring(0, 4096);
                    }
                    embedBuilder.setDescription(description);
                }

                MessageEmbed embed = embedBuilder.build();
                // Embed-only message (empty string content)
                return new DiscordMessage(embed, "");
            } else {
                // Plain text message with capture group replacement
                String replacement = pattern.replacement != null ? pattern.replacement : "";
                if (!replacement.isEmpty()) {
                    replacement = matcher.replaceAll(replacement);
                }
                return new DiscordMessage(replacement);
            }
        } catch (Exception e) {
            DiscordIntegration.LOGGER.error("Error building replacement message from pattern", e);
            return null;
        }
    }

    /**
     * Gets the original message that was matched.
     *
     * @return Original message, or null if no match
     */
    public String getOriginalMessage() {
        return originalMessage;
    }
}


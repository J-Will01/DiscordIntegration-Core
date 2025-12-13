package de.erdbeerbaerlp.dcintegration.test.util;

import de.erdbeerbaerlp.dcintegration.common.DiscordIntegration;
import de.erdbeerbaerlp.dcintegration.common.storage.Configuration;
import de.erdbeerbaerlp.dcintegration.common.storage.Localization;
import de.erdbeerbaerlp.dcintegration.common.util.DiscordMessage;
import de.erdbeerbaerlp.dcintegration.common.util.MessageUtils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Utility to simulate Minecraft events as if triggered by platform code.
 * These methods call the same DiscordIntegration methods that platform code would call.
 */
public class MinecraftEventSimulator {
    private final DiscordIntegration discordIntegration;
    
    public MinecraftEventSimulator(DiscordIntegration discordIntegration) {
        this.discordIntegration = discordIntegration;
    }
    
    /**
     * Simulates a player joining the server
     */
    public void simulatePlayerJoin(UUID playerUUID, String playerName) {
        String message = Localization.instance().playerJoin.replace("%player%", playerName);
        sendMessageToDiscord(message, playerUUID, playerName);
    }
    
    /**
     * Simulates a player leaving the server
     */
    public void simulatePlayerLeave(UUID playerUUID, String playerName) {
        String message = Localization.instance().playerLeave.replace("%player%", playerName);
        sendMessageToDiscord(message, playerUUID, playerName);
    }
    
    /**
     * Simulates a player timeout
     */
    public void simulatePlayerTimeout(UUID playerUUID, String playerName) {
        String message = Localization.instance().playerTimeout.replace("%player%", playerName);
        sendMessageToDiscord(message, playerUUID, playerName);
    }
    
    /**
     * Simulates a player advancement
     */
    public void simulateAdvancement(UUID playerUUID, String playerName, String advName, String advDesc) {
        Configuration.EmbedMode.EmbedEntry entry = Configuration.instance().embedMode.advancementMessage;
        
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("player", playerName);
        placeholders.put("advName", advName);
        placeholders.put("advDesc", advDesc);
        placeholders.put("advNameURL", java.net.URLEncoder.encode(advName, java.nio.charset.StandardCharsets.UTF_8));
        placeholders.put("advDescURL", java.net.URLEncoder.encode(advDesc, java.nio.charset.StandardCharsets.UTF_8));
        
        String channelID = Configuration.instance().advanced.advancementChannelID;
        if (channelID == null || channelID.isEmpty() || "default".equals(channelID)) {
            channelID = Configuration.instance().general.botChannel;
        }
        
        if (entry.asEmbed) {
            EmbedBuilder builder = entry.toEmbed();
            boolean customFieldsApplied = entry.applyCustomFields(builder, placeholders);
            
            MessageEmbed embed = builder.build();
            DiscordMessage discordMessage;
            
            if (customFieldsApplied) {
                // Custom fields applied, send embed-only
                discordMessage = new DiscordMessage(embed, "");
            } else {
                // Use default message
                String defaultMsg = Localization.instance().advancementMessage;
                defaultMsg = MessageUtils.replacePlaceholders(defaultMsg, placeholders);
                discordMessage = new DiscordMessage(embed, defaultMsg);
            }
            
            discordIntegration.sendMessage(playerName, playerUUID.toString(), discordMessage, 
                discordIntegration.getChannel(channelID));
        } else {
            String message = Localization.instance().advancementMessage;
            message = MessageUtils.replacePlaceholders(message, placeholders);
            discordIntegration.sendMessage(playerName, playerUUID.toString(), message, 
                discordIntegration.getChannel(channelID));
        }
    }
    
    /**
     * Simulates a player death
     */
    public void simulateDeath(UUID playerUUID, String playerName, String deathMessage) {
        Configuration.EmbedMode.EmbedEntry entry = Configuration.instance().embedMode.deathMessage;
        
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("player", playerName);
        placeholders.put("msg", deathMessage);
        
        String message = Localization.instance().playerDeath.replace("%player%", playerName).replace("%msg%", deathMessage);
        
        if (entry.asEmbed) {
            EmbedBuilder builder = entry.toEmbed();
            boolean customFieldsApplied = entry.applyCustomFields(builder, placeholders);
            
            MessageEmbed embed = builder.build();
            DiscordMessage discordMessage;
            
            if (customFieldsApplied) {
                discordMessage = new DiscordMessage(embed, "");
            } else {
                discordMessage = new DiscordMessage(embed, message);
            }
            
            discordIntegration.sendMessage(playerName, playerUUID.toString(), discordMessage, 
                discordIntegration.getChannel());
        } else {
            discordIntegration.sendMessage(playerName, playerUUID.toString(), message, 
                discordIntegration.getChannel());
        }
    }
    
    /**
     * Simulates a chat message from a player
     */
    public void simulateChatMessage(UUID playerUUID, String playerName, String message) {
        String formattedMessage = Localization.instance().discordChatMessage
            .replace("%player%", playerName)
            .replace("%msg%", message);
        discordIntegration.sendMessage(formattedMessage, "", playerName);
    }
    
    /**
     * Simulates a console log line (for pattern matching tests)
     */
    public void simulateConsoleLog(String logLine) {
        // This would be called by platform code that intercepts console logs
        // For testing, we'll process it through the pattern matcher
        // and then send to Discord if not suppressed
        var result = discordIntegration.getMessagePatternMatcher().processMessage(logLine);
        
        if (result.isMatched()) {
            if (result.shouldSuppressOriginal()) {
                // Original suppressed, check if replacement should be sent
                if (result.hasReplacement()) {
                    String targetChannelID = result.getTargetChannelID(Configuration.instance().general.botChannel);
                    DiscordMessage replacement = result.buildReplacementMessage();
                    if (replacement != null) {
                        discordIntegration.sendMessage(replacement, discordIntegration.getChannel(targetChannelID));
                    }
                }
            } else {
                // Original not suppressed, send both original and replacement if exists
                String defaultChannel = Configuration.instance().general.botChannel;
                discordIntegration.sendMessage(logLine, discordIntegration.getChannel(defaultChannel));
                
                if (result.hasReplacement()) {
                    String targetChannelID = result.getTargetChannelID(defaultChannel);
                    DiscordMessage replacement = result.buildReplacementMessage();
                    if (replacement != null) {
                        discordIntegration.sendMessage(replacement, discordIntegration.getChannel(targetChannelID));
                    }
                }
            }
        } else {
            // No match, send original
            discordIntegration.sendMessage(logLine, discordIntegration.getChannel());
        }
    }
    
    /**
     * Helper method to send a message to Discord
     */
    private void sendMessageToDiscord(String message, UUID playerUUID, String playerName) {
        Configuration.EmbedMode.EmbedEntry entry;
        if (message.contains("joined")) {
            entry = Configuration.instance().embedMode.playerJoinMessage;
        } else {
            entry = Configuration.instance().embedMode.playerLeaveMessages;
        }
        
        if (entry.asEmbed) {
            EmbedBuilder builder = entry.toEmbed();
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("player", playerName);
            boolean customFieldsApplied = entry.applyCustomFields(builder, placeholders);
            
            MessageEmbed embed = builder.build();
            DiscordMessage discordMessage;
            
            if (customFieldsApplied) {
                discordMessage = new DiscordMessage(embed, "");
            } else {
                discordMessage = new DiscordMessage(embed, message);
            }
            
            discordIntegration.sendMessage(playerName, playerUUID.toString(), discordMessage, 
                discordIntegration.getChannel());
        } else {
            discordIntegration.sendMessage(playerName, playerUUID.toString(), message, 
                discordIntegration.getChannel());
        }
    }
}


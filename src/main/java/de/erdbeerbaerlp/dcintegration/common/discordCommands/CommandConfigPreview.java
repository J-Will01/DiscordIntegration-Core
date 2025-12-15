package de.erdbeerbaerlp.dcintegration.common.discordCommands;

import de.erdbeerbaerlp.dcintegration.common.DiscordIntegration;
import de.erdbeerbaerlp.dcintegration.common.storage.Configuration;
import de.erdbeerbaerlp.dcintegration.common.storage.Localization;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;

/**
 * Discord command for previewing and editing embed configurations with live previews.
 * Similar to Carl.gg's embed preview feature.
 */
public class CommandConfigPreview extends DiscordCommand {
    
    public CommandConfigPreview() {
        super("config-preview", "Preview and edit embed configurations with live previews");
    }
    
    @Override
    public void execute(SlashCommandInteractionEvent ev, ReplyCallbackAction reply) {
        if (!DiscordIntegration.INSTANCE.hasAdminRole(ev.getMember().getRoles())) {
            reply.setContent("❌ You need admin permissions to use this command.").setEphemeral(true).queue();
            return;
        }
        
        // Show main menu with embed type selection
        EmbedBuilder menuEmbed = new EmbedBuilder()
            .setTitle("📋 Config Preview & Editor")
            .setDescription("Select an embed type to preview and edit its configuration.")
            .setColor(Color.CYAN)
            .addField("Available Embed Types", 
                "• **Join** - Player join messages\n" +
                "• **Leave** - Player leave messages\n" +
                "• **Death** - Player death messages\n" +
                "• **Advancement** - Advancement messages\n" +
                "• **Chat** - Chat messages\n" +
                "• **Start** - Server start messages\n" +
                "• **Stop** - Server stop messages", false)
            .setFooter("Use the dropdown below to select an embed type");
        
        StringSelectMenu.Builder selectMenu = StringSelectMenu.create("config_preview:select_type")
            .setPlaceholder("Select embed type to preview...")
            .addOption("Player Join", "join", "Preview player join messages")
            .addOption("Player Leave", "leave", "Preview player leave messages")
            .addOption("Player Death", "death", "Preview player death messages")
            .addOption("Advancement", "advancement", "Preview advancement messages")
            .addOption("Chat", "chat", "Preview chat messages")
            .addOption("Server Start", "start", "Preview server start messages")
            .addOption("Server Stop", "stop", "Preview server stop messages");
        
        reply.setEmbeds(menuEmbed.build())
            .addComponents(ActionRow.of(selectMenu.build()))
            .setEphemeral(true)
            .queue();
    }
    
    /**
     * Shows preview and edit options for a specific embed type
     */
    public static void showEmbedPreview(StringSelectInteractionEvent selectEv, String embedType, ButtonInteractionEvent buttonEv) {
        // Determine which event we're handling
        if (selectEv == null && buttonEv == null) return;
        
        ReplyCallbackAction reply = selectEv != null ? 
            selectEv.reply("").setEphemeral(true) : 
            buttonEv.reply("").setEphemeral(true);
        Configuration.EmbedMode.EmbedEntry entry = getEmbedEntry(embedType);
        if (entry == null) {
            reply.setContent("❌ Invalid embed type: " + embedType).queue();
            return;
        }
        
        // Generate preview with sample data
        EmbedBuilder previewEmbed = generatePreview(embedType, entry);
        
        // Build info embed
        EmbedBuilder infoEmbed = new EmbedBuilder()
            .setTitle("⚙️ " + capitalize(embedType) + " Embed Configuration")
            .setColor(Color.decode(entry.colorHexCode))
            .addField("Enabled as Embed", entry.asEmbed ? "✅ Yes" : "❌ No", true)
            .addField("Color", "`" + entry.colorHexCode + "`", true)
            .addField("Custom Title", entry.customTitle.isEmpty() ? "*Not set*" : "`" + truncate(entry.customTitle, 50) + "`", false)
            .addField("Custom Description", entry.customDescription.isEmpty() ? "*Not set*" : "`" + truncate(entry.customDescription, 50) + "`", false)
            .addField("Custom JSON", entry.customJSON.isEmpty() ? "*Not set*" : "`Set (hidden)`", false)
            .setFooter("Preview below shows how this embed will look");
        
        // Build action buttons
        Button previewBtn = Button.primary("config_preview:preview_" + embedType, "🔄 Refresh Preview");
        Button editTitleBtn = Button.secondary("config_preview:edit_title_" + embedType, "✏️ Edit Title");
        Button editDescBtn = Button.secondary("config_preview:edit_desc_" + embedType, "✏️ Edit Description");
        Button editColorBtn = Button.secondary("config_preview:edit_color_" + embedType, "🎨 Edit Color");
        Button toggleEmbedBtn = Button.success("config_preview:toggle_" + embedType, entry.asEmbed ? "❌ Disable Embed" : "✅ Enable Embed");
        Button backBtn = Button.danger("config_preview:back", "◀️ Back to Menu");
        
        reply.setEmbeds(infoEmbed.build(), previewEmbed.build())
            .addComponents(
                ActionRow.of(previewBtn, toggleEmbedBtn),
                ActionRow.of(editTitleBtn, editDescBtn, editColorBtn),
                ActionRow.of(backBtn)
            )
            .queue();
    }
    
    /**
     * Shows main menu (for back button)
     */
    public static void showMainMenu(ButtonInteractionEvent ev) {
        EmbedBuilder menuEmbed = new EmbedBuilder()
            .setTitle("📋 Config Preview & Editor")
            .setDescription("Select an embed type to preview and edit its configuration.")
            .setColor(Color.CYAN)
            .addField("Available Embed Types", 
                "• **Join** - Player join messages\n" +
                "• **Leave** - Player leave messages\n" +
                "• **Death** - Player death messages\n" +
                "• **Advancement** - Advancement messages\n" +
                "• **Chat** - Chat messages\n" +
                "• **Start** - Server start messages\n" +
                "• **Stop** - Server stop messages", false)
            .setFooter("Use the dropdown below to select an embed type");
        
        StringSelectMenu.Builder selectMenu = StringSelectMenu.create("config_preview:select_type")
            .setPlaceholder("Select embed type to preview...")
            .addOption("Player Join", "join", "Preview player join messages")
            .addOption("Player Leave", "leave", "Preview player leave messages")
            .addOption("Player Death", "death", "Preview player death messages")
            .addOption("Advancement", "advancement", "Preview advancement messages")
            .addOption("Chat", "chat", "Preview chat messages")
            .addOption("Server Start", "start", "Preview server start messages")
            .addOption("Server Stop", "stop", "Preview server stop messages");
        
        ev.replyEmbeds(menuEmbed.build())
            .addComponents(ActionRow.of(selectMenu.build()))
            .setEphemeral(true)
            .queue();
    }
    
    /**
     * Generates a preview embed with sample data
     */
    public static EmbedBuilder generatePreview(String embedType, Configuration.EmbedMode.EmbedEntry entry) {
        Map<String, String> samplePlaceholders = getSamplePlaceholders(embedType);
        
        EmbedBuilder builder = entry.toEmbed();
        
        // Apply custom fields if set
        if (entry.asEmbed) {
            boolean customApplied = entry.applyCustomFields(builder, samplePlaceholders);
            
            if (!customApplied) {
                // Use default message based on embed type
                String defaultMessage = getDefaultMessage(embedType, samplePlaceholders);
                builder.setDescription(defaultMessage);
            }
        }
        
        return builder;
    }
    
    /**
     * Gets sample placeholders for preview
     */
    public static Map<String, String> getSamplePlaceholders(String embedType) {
        Map<String, String> placeholders = new HashMap<>();
        
        switch (embedType) {
            case "join":
                placeholders.put("player", "Steve");
                placeholders.put("msg", "Steve joined the game");
                break;
            case "leave":
                placeholders.put("player", "Alex");
                placeholders.put("msg", "Alex left the game");
                break;
            case "death":
                placeholders.put("player", "Notch");
                placeholders.put("msg", "Notch was slain by Zombie");
                placeholders.put("deathMessage", "was slain by Zombie");
                break;
            case "advancement":
                placeholders.put("player", "Dinnerbone");
                placeholders.put("advName", "Diamonds!");
                placeholders.put("advDesc", "Acquire diamonds");
                placeholders.put("msg", "Dinnerbone has made the advancement [Diamonds!]");
                break;
            case "chat":
                placeholders.put("player", "jeb_");
                placeholders.put("msg", "Hello, world!");
                break;
            case "start":
                placeholders.put("msg", "Server is starting...");
                break;
            case "stop":
                placeholders.put("msg", "Server stopped");
                break;
        }
        
        return placeholders;
    }
    
    /**
     * Gets default message text for embed type
     */
    private static String getDefaultMessage(String embedType, Map<String, String> placeholders) {
        switch (embedType) {
            case "join":
                return Localization.instance().playerJoin.replace("%player%", placeholders.get("player"));
            case "leave":
                return Localization.instance().playerLeave.replace("%player%", placeholders.get("player"));
            case "death":
                return Localization.instance().playerDeath.replace("%player%", placeholders.get("player"))
                    .replace("%deathMessage%", placeholders.getOrDefault("deathMessage", "died"));
            case "advancement":
                return Localization.instance().advancementMessage.replace("%player%", placeholders.get("player"))
                    .replace("%advName%", placeholders.get("advName"))
                    .replace("%advDesc%", placeholders.get("advDesc"));
            case "chat":
                return placeholders.get("msg");
            case "start":
                return Localization.instance().serverStarting;
            case "stop":
                return Localization.instance().serverStopped;
            default:
                return "Preview message";
        }
    }
    
    /**
     * Gets the EmbedEntry for a given embed type
     */
    public static Configuration.EmbedMode.EmbedEntry getEmbedEntry(String embedType) {
        Configuration.EmbedMode embedMode = Configuration.instance().embedMode;
        
        switch (embedType) {
            case "join":
                return embedMode.playerJoinMessage;
            case "leave":
                return embedMode.playerLeaveMessages;
            case "death":
                return embedMode.deathMessage;
            case "advancement":
                return embedMode.advancementMessage;
            case "chat":
                return embedMode.chatMessages;
            case "start":
                return embedMode.startMessages;
            case "stop":
                return embedMode.stopMessages;
            default:
                return null;
        }
    }
    
    /**
     * Shows modal for editing a field
     */
    public static void showEditModal(ButtonInteractionEvent ev, String embedType, String fieldType) {
        Configuration.EmbedMode.EmbedEntry entry = getEmbedEntry(embedType);
        if (entry == null) {
            ev.reply("❌ Invalid embed type.").setEphemeral(true).queue();
            return;
        }
        
        String currentValue = "";
        String label = "";
        String placeholder = "";
        TextInputStyle style = TextInputStyle.SHORT;
        
        switch (fieldType) {
            case "title":
                currentValue = entry.customTitle;
                label = "Custom Title";
                placeholder = "Enter custom title (supports placeholders like %player%)";
                style = TextInputStyle.SHORT;
                break;
            case "desc":
                currentValue = entry.customDescription;
                label = "Custom Description";
                placeholder = "Enter custom description (supports placeholders like %player%)";
                style = TextInputStyle.PARAGRAPH;
                break;
            case "color":
                currentValue = entry.colorHexCode;
                label = "Embed Color";
                placeholder = "Enter hex color code (e.g., #FF0000)";
                style = TextInputStyle.SHORT;
                break;
        }
        
        TextInput input = TextInput.create("value", label, style)
            .setPlaceholder(placeholder)
            .setValue(currentValue)
            .setRequired(false)
            .setMaxLength(fieldType.equals("desc") ? 4096 : (fieldType.equals("color") ? 7 : 256))
            .build();
        
        Modal modal = Modal.create("config_preview:save_" + fieldType + "_" + embedType, "Edit " + label)
            .addComponents(ActionRow.of(input))
            .build();
        
        ev.replyModal(modal).queue();
    }
    
    /**
     * Saves edited field value
     */
    public static void saveField(String embedType, String fieldType, String value) {
        Configuration.EmbedMode.EmbedEntry entry = getEmbedEntry(embedType);
        if (entry == null) return;
        
        switch (fieldType) {
            case "title":
                entry.customTitle = value != null ? value : "";
                break;
            case "desc":
                entry.customDescription = value != null ? value : "";
                break;
            case "color":
                // Validate hex color
                if (value != null && !value.isEmpty()) {
                    if (value.startsWith("#") && value.length() == 7) {
                        try {
                            Color.decode(value); // Validate it's a valid color
                            entry.colorHexCode = value;
                        } catch (NumberFormatException e) {
                            // Invalid color, don't save
                        }
                    }
                }
                break;
        }
        
        // Save config
        try {
            Configuration.instance().saveConfig();
        } catch (Exception e) {
            DiscordIntegration.LOGGER.error("Failed to save config", e);
        }
    }
    
    /**
     * Toggles embed mode for an embed type
     */
    public static void toggleEmbed(String embedType) {
        Configuration.EmbedMode.EmbedEntry entry = getEmbedEntry(embedType);
        if (entry != null) {
            entry.asEmbed = !entry.asEmbed;
            try {
                Configuration.instance().saveConfig();
            } catch (Exception e) {
                DiscordIntegration.LOGGER.error("Failed to save config", e);
            }
        }
    }
    
    private static String capitalize(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }
    
    private static String truncate(String str, int maxLength) {
        if (str == null) return "";
        return str.length() > maxLength ? str.substring(0, maxLength) + "..." : str;
    }
    
    @Override
    public boolean adminOnly() {
        return true;
    }
}


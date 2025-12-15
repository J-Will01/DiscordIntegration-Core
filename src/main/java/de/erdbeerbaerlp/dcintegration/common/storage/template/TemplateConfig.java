package de.erdbeerbaerlp.dcintegration.common.storage.template;

import com.moandjiezana.toml.Toml;
import com.moandjiezana.toml.TomlComment;
import com.moandjiezana.toml.TomlIgnore;
import com.moandjiezana.toml.TomlWriter;
import de.erdbeerbaerlp.dcintegration.common.DiscordIntegration;
import de.erdbeerbaerlp.dcintegration.common.util.template.Template;
import de.erdbeerbaerlp.dcintegration.common.util.template.TemplateEngine;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static de.erdbeerbaerlp.dcintegration.common.DiscordIntegration.discordDataDir;

/**
 * Configuration for message templates.
 * Templates can be reused across different message types.
 */
public class TemplateConfig {
    @TomlIgnore
    private static TemplateConfig INSTANCE;
    
    @TomlIgnore
    private static final File templateFile = new File(discordDataDir, "Templates.toml");
    
    static {
        INSTANCE = new TemplateConfig();
    }
    
    @TomlComment({
        "Enable the template system",
        "When enabled, you can use templates in embed customization and message patterns"
    })
    public boolean enabled = false;
    
    @TomlComment({
        "Registered templates that can be reused via {include templateName}",
        "Example:",
        "[templates.advancement]",
        "content = \"🏆 %player% earned %advName%\"",
        "",
        "Then use it with: {include advancement}"
    })
    public Map<String, TemplateEntry> templates = new HashMap<>();
    
    public static class TemplateEntry {
        @TomlComment("Template content (supports variables, conditionals, and includes)")
        public String content = "";
        
        @TomlComment("Template description (for documentation purposes)")
        public String description = "";
    }
    
    public static TemplateConfig instance() {
        return INSTANCE;
    }
    
    /**
     * Loads templates from file and registers them with the template engine
     */
    public void loadConfig() throws IOException {
        if (!templateFile.exists()) {
            INSTANCE = new TemplateConfig();
            INSTANCE.saveConfig();
            return;
        }
        
        INSTANCE = new Toml().read(templateFile).to(TemplateConfig.class);
        if (INSTANCE.templates == null) {
            INSTANCE.templates = new HashMap<>();
        }
        
        // Register templates with the template engine
        registerTemplates();
        
        INSTANCE.saveConfig(); // Re-write to add new values
    }
    
    /**
     * Registers all templates with the template engine
     */
    private void registerTemplates() {
        if (DiscordIntegration.INSTANCE == null) {
            return;
        }
        
        TemplateEngine engine = DiscordIntegration.INSTANCE.getTemplateEngine();
        if (engine == null) {
            return;
        }
        
        engine.clearTemplates();
        
        for (Map.Entry<String, TemplateEntry> entry : templates.entrySet()) {
            String name = entry.getKey();
            TemplateEntry templateEntry = entry.getValue();
            if (templateEntry != null && templateEntry.content != null && !templateEntry.content.isEmpty()) {
                engine.registerTemplate(name, templateEntry.content);
                DiscordIntegration.LOGGER.debug("Registered template: {}", name);
            }
        }
    }
    
    /**
     * Saves the configuration to file
     */
    @SuppressWarnings("ResultOfMethodCallIgnored")
    public void saveConfig() throws IOException {
        if (!discordDataDir.exists()) {
            discordDataDir.mkdirs();
        }
        
        if (!templateFile.exists()) {
            templateFile.getParentFile().mkdirs();
            templateFile.createNewFile();
        }
        
        final TomlWriter w = new TomlWriter.Builder()
                .indentValuesBy(2)
                .indentTablesBy(4)
                .padArrayDelimitersBy(2)
                .build();
        w.write(this, templateFile);
    }
    
    /**
     * Reloads templates from config
     */
    public void reloadTemplates() {
        try {
            loadConfig();
        } catch (IOException e) {
            DiscordIntegration.LOGGER.error("Failed to reload templates", e);
        }
    }
}


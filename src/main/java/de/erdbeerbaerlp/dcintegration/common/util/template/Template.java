package de.erdbeerbaerlp.dcintegration.common.util.template;

import org.jetbrains.annotations.NotNull;

/**
 * Represents a reusable template with a name and content.
 */
public class Template {
    private final String name;
    private final String content;
    
    /**
     * Creates a new template
     * 
     * @param name Template name
     * @param content Template content
     */
    public Template(@NotNull String name, @NotNull String content) {
        this.name = name;
        this.content = content;
    }
    
    /**
     * Gets the template name
     * 
     * @return Template name
     */
    @NotNull
    public String getName() {
        return name;
    }
    
    /**
     * Gets the template content
     * 
     * @return Template content
     */
    @NotNull
    public String getContent() {
        return content;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Template template = (Template) o;
        return name.equals(template.name);
    }
    
    @Override
    public int hashCode() {
        return name.hashCode();
    }
    
    @Override
    public String toString() {
        return "Template{name='" + name + "', content='" + content + "'}";
    }
}


package de.erdbeerbaerlp.dcintegration.test;

import de.erdbeerbaerlp.dcintegration.common.DiscordIntegration;
import de.erdbeerbaerlp.dcintegration.common.storage.Configuration;
import de.erdbeerbaerlp.dcintegration.common.storage.Localization;
import de.erdbeerbaerlp.dcintegration.test.mocks.MockJDAFactory;
import de.erdbeerbaerlp.dcintegration.test.mocks.MockMcServerInterface;
import de.erdbeerbaerlp.dcintegration.test.util.DiscordEventSimulator;
import de.erdbeerbaerlp.dcintegration.test.util.MinecraftEventSimulator;
import de.erdbeerbaerlp.dcintegration.test.util.TestConfigHelper;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import java.io.File;
import java.lang.reflect.Field;
import java.util.UUID;

/**
 * Base class for all e2e integration tests.
 * Provides common setup and teardown with mocked interfaces.
 */
public abstract class IntegrationTestBase {
    protected MockMcServerInterface mockMC;
    protected MockJDAFactory mockJDAFactory;
    protected JDA mockJDA;
    protected DiscordIntegration discordIntegration;
    protected MinecraftEventSimulator mcEventSimulator;
    protected DiscordEventSimulator discordEventSimulator;
    protected String defaultChannelID = "123456789";
    
    @BeforeEach
    public void setUp() throws Exception {
        // Create temp config file
        DiscordIntegration.configFile = File.createTempFile("e2eTest", "config");
        DiscordIntegration.configFile.deleteOnExit();
        
        // Load configuration
        Configuration.instance().loadConfig();
        Localization.instance().loadConfig();
        
        // Set up default channel ID in config
        Configuration.instance().general.botChannel = defaultChannelID;
        Configuration.instance().advanced.serverChannelID = defaultChannelID;
        Configuration.instance().advanced.chatInputChannelID = "default";
        Configuration.instance().webhook.enable = false; // Disable webhooks for simpler testing
        
        // Create mock interfaces
        mockMC = new MockMcServerInterface();
        mockMC.addPlayer(UUID.randomUUID(), "TestPlayer");
        
        mockJDAFactory = new MockJDAFactory();
        mockJDA = mockJDAFactory.createMockJDA();
        
        // Create mock channel
        mockJDAFactory.createMockTextChannel(defaultChannelID);
        
        // Create DiscordIntegration instance
        discordIntegration = new DiscordIntegration(mockMC);
        DiscordIntegration.INSTANCE = discordIntegration;
        
        // Inject mock JDA using reflection (since jda is private)
        injectMockJDA(mockJDA);
        
        // Wait a bit for initialization
        Thread.sleep(100);
        
        // Create event simulators
        mcEventSimulator = new MinecraftEventSimulator(discordIntegration);
        discordEventSimulator = new DiscordEventSimulator(discordIntegration, mockJDA);
        
        // Reload message patterns
        discordIntegration.getMessagePatternMatcher().reloadPatterns();
        
        // Reset config to defaults
        TestConfigHelper.resetConfig();
    }
    
    @AfterEach
    public void tearDown() throws Exception {
        // Clean up
        if (discordIntegration != null) {
            try {
                discordIntegration.kill(true);
            } catch (Exception e) {
                // Ignore cleanup errors
            }
        }
        DiscordIntegration.INSTANCE = null;
        
        // Clear mocks
        if (mockMC != null) {
            mockMC.clearMessages();
        }
        if (mockJDAFactory != null) {
            mockJDAFactory.clearMessages();
        }
    }
    
    /**
     * Injects the mock JDA into DiscordIntegration using reflection
     */
    private void injectMockJDA(JDA mockJDA) throws Exception {
        Field jdaField = DiscordIntegration.class.getDeclaredField("jda");
        jdaField.setAccessible(true);
        jdaField.set(discordIntegration, mockJDA);
    }
    
    /**
     * Gets the mock Minecraft server interface
     */
    protected MockMcServerInterface getMockMC() {
        return mockMC;
    }
    
    /**
     * Gets the mock JDA factory
     */
    protected MockJDAFactory getMockJDAFactory() {
        return mockJDAFactory;
    }
    
    /**
     * Gets the mock JDA instance
     */
    protected JDA getMockJDA() {
        return mockJDA;
    }
    
    /**
     * Gets the DiscordIntegration instance
     */
    protected DiscordIntegration getDiscordIntegration() {
        return discordIntegration;
    }
    
    /**
     * Gets the Minecraft event simulator
     */
    protected MinecraftEventSimulator getMCEventSimulator() {
        return mcEventSimulator;
    }
    
    /**
     * Gets the Discord event simulator
     */
    protected DiscordEventSimulator getDiscordEventSimulator() {
        return discordEventSimulator;
    }
    
    /**
     * Helper to wait for async operations to complete
     */
    protected void waitForAsyncOperations() throws InterruptedException {
        Thread.sleep(200); // Give time for WorkThread jobs to complete
    }
    
    /**
     * Gets the default channel for testing
     */
    protected GuildMessageChannel getDefaultChannel() {
        return discordIntegration.getChannel(defaultChannelID);
    }
}


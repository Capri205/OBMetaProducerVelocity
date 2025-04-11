package net.obmc.OBMetaProducerVelocity;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import org.slf4j.Logger;
import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;

@Plugin(
	id = "obmetaproducervelocity",
	name = "OBMetaProducerVelocity",
	version = "1.0",
    url = "https://ob-mc.net",
	description = "Produces event meta data for external tools, utilities and monitoring",
	authors = {"Fahrenheit451/Capri205"}
)
public class OBMetaProducer {

    private final Logger logger;

	private final ProxyServer server;

	public static OBMetaProducer instance;

	private ConfigurationNode config;
	private final Path configPath;

	public static String metafile;
	public static String program = "OBMetaProducerVelocity";
	
	@Inject
    public OBMetaProducer(ProxyServer server, Logger logger, @DataDirectory Path dataDirectory) {
        this.server = server;
	    this.logger = logger;
		instance = this;
		this.configPath = dataDirectory.resolve("config.yml");
	}

	@Subscribe
	public void onProxyInitialization(ProxyInitializeEvent event) {

		copyDefaultConfig(configPath);
		loadConfig();
		setupMetaFile();

		// Do some operation demanding access to the Velocity API here.
		server.getEventManager().register(this, new OBMetaEvent());

		logger.info("[" + program + "] Plugin loaded");
	}

	@Subscribe
    public void onProxyShutdown(ProxyShutdownEvent event) {
		logger.info("[" + program + "] Plugin unloaded");
    }

	public void loadConfig() {
		YamlConfigurationLoader loader = YamlConfigurationLoader.builder()
            .path(configPath)
            .build();
        try {
            config = loader.load();
        } catch (ConfigurateException ex) {
            logger.error("[" + program + "] Failed to load configuration");
        }
		logger.info("[" + program + "] Configuration loaded");
	}
	
	private void copyDefaultConfig(Path configPath) {
logger.info("looking for : " + configPath.toString());
		try (InputStream in = getClass().getClassLoader().getResourceAsStream("config.yml")) {
			if (in == null) {
				throw new IllegalStateException("Default config.yml not found in resources");
			}

			Files.createDirectories(configPath.getParent());

			if (Files.notExists(configPath)) {
logger.info("config file doesn't exist... copy from resources");
				Files.copy(in, configPath);
			} else {
logger.info("config file exists!");				
			}
		} catch (IOException e) {
			logger.error("[" + program + "] Failed to create default config");
		}
	}
	public void setupMetaFile() {
		
		metafile = config.node("MetaFile", "Filename").getString();
logger.info("[" + program + "] metafile: " + metafile);
		File file = new File( metafile );
		if (!file.exists()) {
			try {
				file.createNewFile();
			} catch ( IOException e ) {
				logger.error("[" + program + "] Failed to create metafile");
			}
		}
	}

	public static OBMetaProducer getInstance() {
		return instance;
	}
	public ProxyServer getServer() {
		return server;
	}
	public Logger getLogger() {
		return logger;
	}
    public String getProgram() {
        return program;
    }
}
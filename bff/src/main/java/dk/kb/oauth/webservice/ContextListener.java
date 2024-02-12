package dk.kb.oauth.webservice;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.joran.util.ConfigurationWatchListUtil;
import ch.qos.logback.core.util.StatusPrinter;
import dk.kb.oauth.config.ServiceConfig;
import dk.kb.util.BuildInfoManager;
import dk.kb.util.Files;
import dk.kb.util.Resolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Listener to handle the various setups and configuration sanity checks that can be carried out at when the
 * context is deployed/initalized.
 */

public class ContextListener implements ServletContextListener {
    private final Logger log = LoggerFactory.getLogger(getClass());

    public static final String CONFIG_ENV = "java:/comp/env/application-config";

    // Having a key that contains the artifact ID is not necessary as the logback config file location
    // it is always stated in a Tomcat context file that is only used for the project.
    // The old way was to state the application ID in the key.
    // The new way is to state the logback config file using a generic key, just as for the application config.
    // The LOGBACK_ENV_LEGACY is used secondarily and is only kept to support existing setups.
    public static final String LOGBACK_ENV = "java:/comp/env/logback-config";
    public static final String LOGBACK_ENV_LEGACY = "java:/comp/env/bff-logback-config";

    /**
     * On context initialisation this
     * i) Initialises the logging framework (logback).
     * ii) Initialises the configuration class.
     * @param sce context provided by the web server upon initialization.
     * @throws java.lang.RuntimeException if anything at all goes wrong.
     */
    @Override
    public void contextInitialized(ServletContextEvent sce) {
        // Workaround for logback problem. This should be called before any logging takes place
        initLogging();
        BuildInfoManager.loadBuildInfo("bff.build.properties");

        logServiceInfo();
        initConfig();
    }

    /**
     * Log service name, version, heap etc. for help with debugging of service problems.
     */
    private void logServiceInfo() {
        RuntimeMXBean mxBean = ManagementFactory.getRuntimeMXBean();
        if (mxBean.getInputArguments().stream().noneMatch(arg -> arg.startsWith("-Xmx"))) {
            log.warn("Java heap size (-Xmx option) is not specified. " +
                     "In stage or production this is almost always an error");
        }

        String hostname;
        try {
            hostname = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            log.warn("Unable to resolve hostname for localhost");
            hostname = "N/A";
        }

        log.info("Initializing service {} {} build {} using Java {} with max heap {}MB on machine {}. The service " +
                "has been build from git branch: '{}' with commit checksum: '{}' and newest commit was made " +
                "at: '{}'. The service has the git tag: '{}' and is closest to the following git tag: '{}'.",
                 BuildInfoManager.getName(), BuildInfoManager.getVersion(), BuildInfoManager.getBuildTime(),
                 System.getProperty("java.version"), Runtime.getRuntime().maxMemory()/1048576, hostname,
                 BuildInfoManager.getGitBranch(), BuildInfoManager.getGitCommitChecksum(),
                 BuildInfoManager.getGitCommitTime(), BuildInfoManager.getGitCurrentTag(),
                 BuildInfoManager.getGitClosestTag());
    }

    /**
     * Initialize the configuration for the service.
     */
    private void initConfig() {
        InitialContext ctx;
        try {
            ctx = new InitialContext();
        } catch (NamingException e) {
            String message = "Failed to create InitialContext. Config loading not possible";
            log.error(message, e);
            throw new RuntimeException(message, e);
        }

        String configSource;
        try {
            configSource = (String) ctx.lookup(CONFIG_ENV);
        } catch (NamingException e) {
            String message = "Fatal error: Failed to lookup config from '" + CONFIG_ENV + "'";
            log.error(message, e);
            throw new RuntimeException(message, e);
        }

        try {
            //TODO this should not refer to something in template. Should we perhaps use reflection here?
            ServiceConfig.getInstance().initialize(configSource);

            String logConfig = "N/A";
            try {
                LoggerContext loggerContext = ((ch.qos.logback.classic.Logger)log).getLoggerContext();
                URL mainURL = ConfigurationWatchListUtil.getMainWatchURL(loggerContext);
                logConfig = mainURL == null ? "N/A" : mainURL.toString();
            } catch (Exception e) {
                log.warn("Unable to determing Logback configuration. Probable causes are missing specification of " +
                    "logbag config in '{}' or third party library include of slf4j-simple", LOGBACK_ENV);
            }

            log.info("ServiceConfig initialized with autoupdate={} from config glob '{}', Logback config in '{}'",
                     ServiceConfig.getInstance().isAutoUpdating(), configSource, logConfig);
        } catch (IOException e) {
            String message = "Failed to load settings from '" + configSource + "' resolved from '" + CONFIG_ENV + "'";
            log.error(message, e);
            throw new RuntimeException(message , e);
        }
    }

    /**
     * For unfathomable reasons, logback 1.4.14 does not support the construction
     * <pre>
     * &lt;configuration scan="true" scanPeriod="5 minutes"&gt;
     *     &lt;insertFromJNDI env-entry-name="java:comp/env/ds-image-logback-config" as="logbackConfiguration"/&gt;
     *     &lt;include file="${logbackConfiguration}"/&gt;
     * &lt;/configuration&gt;
     * </pre>
     * as the JNDI injection is performed <strong>after</strong> the {@code include}.
     * <p>
     * The workaround is to programatically perform the same environment lookup and reconfigure logback to use
     * the right logback setup file.
     * <p>
     * To complicate matters further, logback require included files to encapsulate the concrete setup in
     * {@code <included>} instead of {@code <configuration>} so in order to stay backwards compatible (and forward
     * compatible as the JNDI-problem is probably solved at some point in the future), a tiny configuration is
     * created at runtime, including the real configuration.
     */
    private void initLogging() {
        try {
            if (Resolver.resolveURL("logback-test.xml") != null) {
                // No need to explicitly reinit with logback-test.xml as Logback will already have done that
                log.info("Logback config 'logback-test.xml' found. Running in test mode");
                return;
            }

        } catch (Exception e) {
            // We might want to skip this logging as it will log to the unconfigured logback at this point
            log.debug("Logback config 'logback-test.xml' not found. Attempting explicit logback configuration");
        }

        InitialContext ctx;
        try {
            ctx = new InitialContext();
        } catch (NamingException e) {
            log.warn("Failed to create InitialContext. Logback config resolving not possible", e);
            return;
        }

        // It would be possible (and easy) to look for logback setups in different predefined locations,
        // such as the user's home directory. This is easier for a random person installing the application,
        // but also leads to inconsistent installations and harder debugging. Current choice is to require
        // the logback config to be explicitly stated.
        String logbackConfigFile;
        try {
            logbackConfigFile = (String) ctx.lookup(LOGBACK_ENV);
        } catch (NamingException e) {
            try {
                logbackConfigFile = (String) ctx.lookup(LOGBACK_ENV_LEGACY);
            } catch (NamingException ex) {
                log.warn("Unable to resolve logback config location from either '{}' or '{}'. " +
                    "Unable to reinitialize logback", LOGBACK_ENV, LOGBACK_ENV_LEGACY);
                return;
            }
        }

        try {
            // Check for logback setup file existence (throws an exception if it fails)
            Resolver.resolveURL(logbackConfigFile);
        } catch (MalformedURLException | FileNotFoundException e) {
            log.warn("Resolved '{}' to path '{}', which does not exist. " +
                "Unable to reinitialize logback", LOGBACK_ENV, logbackConfigFile);
            return;
        }

        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        try {
            // Create a temporary file that includes the real config file (see JavaDoc for this method for details)
            File redirect = createRedirect(logbackConfigFile);

            // https://dennis-xlc.gitbooks.io/the-logback-manual/content/en/chapter-3-configuration/configuration-in-logback/invoking-joranconfigurator-directly.html
            JoranConfigurator configurator = new JoranConfigurator();
            configurator.setContext(context);
            context.reset();
            configurator.doConfigure(redirect);

            log.info("Successfully reinitialized logback with '{}'", logbackConfigFile);
        } catch (JoranException e) {
            StatusPrinter.printInCaseOfErrorsOrWarnings(context);
            throw new RuntimeException("Failed to reinitialize logback from '" + logbackConfigFile + "'", e);
        }
    }

    /**
     * Create a logback redirection file that points to the true logback configuration.
     * See {@link #initLogging()} for details.
     * @param logbackFile the real configuration for logback as a file on the local filesystem.
     * @return a logback config that includes {@code logbackFile}.
     */
    private static File createRedirect(String logbackFile) {
        File redirectFile;
        try {
            redirectFile = File.createTempFile("logback-loader_", ".xml");
        } catch (IOException e) {
            throw new RuntimeException(
                    "Unable to create temporary file for logback configuration of '" + logbackFile + "'", e);
        }
        redirectFile.deleteOnExit();

        try {
            String redirectContent =
                    "<configuration>\n" +
                            "<include file=\"" + logbackFile + "\"/>\n" +
                            "</configuration>\n";
            Files.saveString(redirectContent, redirectFile);
        } catch (IOException e) {
            throw new RuntimeException(
                    "Unable to save redirect logback for '" + logbackFile + "' to temp file '" + redirectFile + "'", e);
        }

        return redirectFile;
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        log.debug("contextDestroyed called: Shutting down ServiceConfig");
        ServiceConfig.getInstance().shutdown();
        log.info("Service destroyed");
    }

}

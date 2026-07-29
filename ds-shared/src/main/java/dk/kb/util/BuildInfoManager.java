package dk.kb.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Parses the property file 'build.properties' which has been populated with application name, version and build time.
 * The information is typically logged upon application start to make it easier for Operations an other log users
 * to determine what is running.
 *
 * As of 2024-01-29 this requires that the project pom.xml contains the following sections
 * <pre>
 *   <properties>
 *     ...
 *     <!-- Needed to populate build.properties -->
 *     <timestamp>${maven.build.timestamp}</timestamp>
 *     <maven.build.timestamp.format>yyyy-MM-dd HH:mm</maven.build.timestamp.format>
 *     ...
 *   </properties>
 * </pre>
 * and
 * <pre>
 *   <build>
 *     <resources>
 *       <resource>
 *         <directory>src/main/resources/</directory>
 *         <includes>
 *           <include>build.properties</include>
 *         </includes>
 *         <filtering>true</filtering>
 *       </resource>
 *     </resources>
 *     ...
 *   </build>
 * </pre>
 *
 * <pre>
 *   <plugin>
 *     <groupId>io.github.git-commit-id</groupId>
 *     <artifactId>git-commit-id-maven-plugin</artifactId>
 *     <version>7.0.0</version>
 *     <executions>
 *       <execution>
 *         <id>get-the-git-infos</id>
 *         <goals>
 *           <goal>revision</goal>
 *         </goals>
 *         <phase>initialize</phase>
 *       </execution>
 *     </executions>
 *     <configuration>
 *       <includeOnlyProperties>
 *         <includeOnlyProperty>git.commit.id</includeOnlyProperty>
 *         <includeOnlyProperty>git.branch</includeOnlyProperty>
 *         <includeOnlyProperty>git.tag</includeOnlyProperty>
 *         <includeOnlyProperty>git.closest.tag.name</includeOnlyProperty>
 *         <includeOnlyProperty>git.commit.author.time</includeOnlyProperty>
 *       </includeOnlyProperties>
 *       <replacementProperties>
 *         <replacementProperty>
 *           <property>git.tag</property>
 *           <regex>false</regex>
 *           <token>git.tag</token>
 *           <value>unknown</value>
 *        </replacementProperty>
 *      </replacementProperties>
 *    </configuration>
 *  </plugin>
 * </pre>
 * The file {@code src/main/resources/build.properties} should contain at least
 * <pre>
 *   # Build Time Information
 *   APPLICATION.NAME=${pom.name}
 *   APPLICATION.VERSION=${pom.version}
 *   APPLICATION.BUILDTIME=${timestamp}
 *   GIT.COMMIT.CHECKSUM=${git.commit.id}
 *   GIT.BRANCH=${git.branch}
 *   GIT.CURRENT.TAG=${git.tag}
 *   GIT.CLOSEST.TAG=${git.closest.tag.name}
 *   GIT.COMMIT.TIME=${git.commit.author.time}
 * </pre>
 */
public class BuildInfoManager {
    static String BUILD_PROPERTY_FILE = "build.properties";

    private static final Logger log = LoggerFactory.getLogger(BuildInfoManager.class);

    private static String name = null;
    private static String version = null;
    private static String buildTime = null;
    private static String gitBranch = null;
    private static String gitCommitChecksum = null;
    private static String gitCommitTime = null;
    private static String gitCurrentTag = null;
    private static String gitClosestTag = null;


    /**
     * @return the human readable name of the application, as defined in pom.xml.
     */
    public static String getName() {
        loadBuildInfo();
        return name;
    }

    /**
     * @return the version of the application, as defined in pom.xml.
     */
    public static String getVersion() {
        loadBuildInfo();
        return version;
    }

    /**
     * @return the build time of the application.
     */
    public static String getBuildTime() {
        loadBuildInfo();
        return buildTime;
    }

    /**
     * @return name of the built branch.
     */
    public static String getGitBranch(){
        loadBuildInfo();
        return gitBranch;
    }


    public static String getGitCommitChecksum(){
        loadBuildInfo();
        return gitCommitChecksum;
    }

    public static String getGitCommitTime(){
        loadBuildInfo();
        return gitCommitTime;
    }

    public static String getGitCurrentTag(){
        loadBuildInfo();
        return gitCurrentTag;
    }

    public static String getGitClosestTag(){
        loadBuildInfo();
        return gitClosestTag;
    }

    /**
     * Load build info from an inputted property file.
     * @param propertyFile the file to extract the properties from.
     */
    public static synchronized void loadBuildInfo(String propertyFile) {
        BUILD_PROPERTY_FILE = propertyFile;
        getBuildInfo();
    }

    /**
     * Load build information from {@link #BUILD_PROPERTY_FILE} on the path.
     */
    private static synchronized void loadBuildInfo() {
        getBuildInfo();
    }

    private static void getBuildInfo() {
        if (name != null) { // Already resolved
            return;
        }

        Properties properties = new Properties();
        try (InputStream is = Resolver.resolveStream(BUILD_PROPERTY_FILE)) {
            if (is == null) {
                log.warn("Unable to load '" + BUILD_PROPERTY_FILE + "' from the classpath. " +
                        "Build information will be unavailable");
            } else {
                properties.load(is);
                log.debug("Properties were loaded from: '{}'", BUILD_PROPERTY_FILE);
            }
        } catch (IOException e) {
            log.warn("Could not load build information from:" + BUILD_PROPERTY_FILE, e);
        }

        name = properties.getProperty("APPLICATION.NAME", "unknown");
        version = properties.getProperty("APPLICATION.VERSION", "unknown");
        buildTime = properties.getProperty("APPLICATION.BUILDTIME", "unknown");
        gitCommitChecksum = properties.getProperty("GIT.COMMIT.CHECKSUM", "unknown");
        gitBranch = properties.getProperty("GIT.BRANCH", "unknown");
        gitClosestTag = properties.getProperty("GIT.CLOSEST.TAG", "unknown");
        gitCurrentTag = properties.getProperty("GIT.CURRENT.TAG", "unknown");
        gitCommitTime = properties.getProperty("GIT.COMMIT.TIME", "unknown");
    }
}

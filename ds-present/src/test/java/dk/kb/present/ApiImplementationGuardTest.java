package dk.kb.present;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Guards the gap between the contract (ds-present-api) and this service.
 * <p>
 * The contract generates one JAX-RS interface per OpenAPI tag. Adding a NEW tag to the spec
 * compiles fine even if nobody implements it: the endpoint is simply missing at runtime.
 * This test makes that a build failure instead. For every {@code *Api} interface in
 * {@code dk.kb.present.api.v1} it checks that
 * <ol>
 *     <li>{@code dk.kb.present.api.v1.impl.<Name>ServiceImpl} exists and implements it, and</li>
 *     <li>that class is registered in {@code Application_v1#getClasses()}.</li>
 * </ol>
 * The skeleton generator in the ds-present pom writes a stub for a new tag on every build
 * (unless {@code -DskipImplGen} is set); registering it in Application_v1 is the manual step.
 * <p>
 * The check reads Application_v1's source rather than calling {@code getClasses()}, because
 * that method loads the service configuration and this test must run without one.
 */
class ApiImplementationGuardTest {
    private static final String API_PACKAGE = "dk.kb.present.api.v1";
    private static final String IMPL_PACKAGE = API_PACKAGE + ".impl";
    private static final Path APPLICATION_SOURCE =
            Paths.get("src", "main", "java", "dk", "kb", "present", "webservice", "Application_v1.java");
    private static final String HINT =
            " Build ds-present without -DskipImplGen and the skeleton generator writes a stub for it.";

    @Test
    void everyContractInterfaceIsImplementedAndRegistered() throws Exception {
        Set<String> apis = findApiInterfaces();
        assertFalse(apis.isEmpty(),
                "Found no *Api interfaces in " + API_PACKAGE + " on the test classpath. " +
                "Is ds-present-api a dependency, and has it been built?");

        assertTrue(Files.isRegularFile(APPLICATION_SOURCE),
                "Cannot find " + APPLICATION_SOURCE.toAbsolutePath() + " (the test expects to run from the module directory)");
        String application = new String(Files.readAllBytes(APPLICATION_SOURCE), StandardCharsets.UTF_8);

        List<String> problems = new ArrayList<>();
        for (String api : apis) {
            String simpleName = api.substring(api.lastIndexOf('.') + 1);
            String implName = IMPL_PACKAGE + "." + simpleName + "ServiceImpl";
            Class<?> iface = Class.forName(api);
            Class<?> impl;
            try {
                impl = Class.forName(implName);
            } catch (ClassNotFoundException e) {
                problems.add(simpleName + ": no implementation " + implName + "." + HINT);
                continue;
            }
            if (!iface.isAssignableFrom(impl)) {
                problems.add(simpleName + ": " + implName + " exists but does not implement " + api);
                continue;
            }
            Pattern registered = Pattern.compile("\\b" + Pattern.quote(impl.getSimpleName() + ".class"));
            if (!registered.matcher(application).find()) {
                problems.add(simpleName + ": " + impl.getSimpleName() +
                        ".class is not listed in Application_v1#getClasses(), so the endpoint would not be served");
            }
        }
        if (!problems.isEmpty()) {
            fail("Contract/implementation mismatch:\n  " + String.join("\n  ", problems));
        }
    }

    /**
     * All top-level interfaces named {@code *Api} in {@link #API_PACKAGE}, whether the contract
     * is on the classpath as a directory (reactor build) or as a jar (standalone build).
     */
    private static Set<String> findApiInterfaces() throws IOException, URISyntaxException, ClassNotFoundException {
        String path = API_PACKAGE.replace('.', '/');
        Set<String> candidates = new TreeSet<>();
        Enumeration<URL> roots = ApiImplementationGuardTest.class.getClassLoader().getResources(path);
        while (roots.hasMoreElements()) {
            URL root = roots.nextElement();
            if ("jar".equals(root.getProtocol())) {
                URLConnection connection = root.openConnection();
                connection.setUseCaches(false);
                try (JarFile jar = ((JarURLConnection) connection).getJarFile()) {
                    Enumeration<JarEntry> entries = jar.entries();
                    while (entries.hasMoreElements()) {
                        String name = entries.nextElement().getName();
                        if (name.startsWith(path + "/") && name.indexOf('/', path.length() + 1) < 0) {
                            addIfCandidate(candidates, name.substring(path.length() + 1));
                        }
                    }
                }
            } else if ("file".equals(root.getProtocol())) {
                File[] files = new File(root.toURI()).listFiles();
                if (files != null) {
                    for (File file : files) {
                        if (file.isFile()) {
                            addIfCandidate(candidates, file.getName());
                        }
                    }
                }
            }
        }
        Set<String> apis = new TreeSet<>();
        for (String candidate : candidates) {
            if (Class.forName(candidate).isInterface()) {
                apis.add(candidate);
            }
        }
        return apis;
    }

    private static void addIfCandidate(Set<String> candidates, String fileName) {
        if (fileName.endsWith("Api.class") && fileName.indexOf('$') < 0) {
            candidates.add(API_PACKAGE + "." + fileName.substring(0, fileName.length() - ".class".length()));
        }
    }
}

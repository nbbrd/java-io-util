package nbbrd.io.sys;

import lombok.NonNull;
import org.jspecify.annotations.Nullable;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * https://docs.oracle.com/javase/tutorial/essential/environment/sysprop.html
 */
@lombok.RequiredArgsConstructor(staticName = "of")
public final class SystemProperties {

    public static final String FILE_SEPARATOR = "file.separator";
    public static final String JAVA_CLASS_PATH = "java.class.path";
    public static final String JAVA_CLASS_VERSION = "java.class.version";
    public static final String JAVA_HOME = "java.home";
    public static final String JAVA_LIBRARY_PATH = "java.library.path";
    public static final String JAVA_VENDOR = "java.vendor";
    public static final String JAVA_VENDOR_URL = "java.vendor.url";
    public static final String JAVA_VERSION = "java.version";
    public static final String JAVA_IO_TMPDIR = "java.io.tmpdir";
    public static final String JAVA_COMPILER = "java.compiler";
    public static final String LINE_SEPARATOR = "line.separator";
    public static final String OS_ARCH = "os.arch";
    public static final String OS_NAME = "os.name";
    public static final String OS_VERSION = "os.version";
    public static final String PATH_SEPARATOR = "path.separator";
    public static final String USER_DIR = "user.dir";
    public static final String USER_HOME = "user.home";
    public static final String USER_NAME = "user.name";
    public static final String JAVA_VM_SPECIFICATION_VERSION = "java.vm.specification.version";
    public static final String JAVA_VM_SPECIFICATION_VENDOR = "java.vm.specification.vendor";
    public static final String JAVA_VM_SPECIFICATION_NAME = "java.vm.specification.name";
    public static final String JAVA_VM_VERSION = "java.vm.version";
    public static final String JAVA_VM_VENDOR = "java.vm.vendor";
    public static final String JAVA_VM_NAME = "java.vm.name";
    public static final String JAVA_SPECIFICATION_VERSION = "java.specification.version";
    public static final String JAVA_SPECIFICATION_VENDOR = "java.specification.vendor";
    public static final String JAVA_SPECIFICATION_NAME = "java.specification.name";

    public static final SystemProperties DEFAULT = of(System.getProperties(), FileSystems.getDefault());

    @NonNull
    private final Properties source;

    @NonNull
    private final FileSystem fileSystem;

    /**
     * Character that separates components of a file path.
     * This is "/" on UNIX and "\" on Windows.
     *
     * @return
     */
    public @Nullable Character getFileSeparator() {
        return asChar(get(FILE_SEPARATOR));
    }

    /**
     * Path used to find directories and JAR archives containing class files.
     * Elements of the class path are separated by a platform-specific character specified in the path separator property.
     *
     * @return
     */
    public @NonNull List<Path> getJavaClassPath() {
        return asPaths(get(JAVA_CLASS_PATH));
    }

    public @Nullable String getJavaClassVersion() {
        return get(JAVA_CLASS_VERSION);
    }

    /**
     * Installation directory for Java Runtime Environment (JRE)
     *
     * @return
     */
    public @Nullable Path getJavaHome() {
        return asPath(get(JAVA_HOME));
    }

    public @NonNull List<Path> getJavaLibraryPath() {
        return asPaths(get(JAVA_LIBRARY_PATH));
    }

    /**
     * JRE vendor name
     *
     * @return
     */
    public @Nullable String getJavaVendor() {
        return get(JAVA_VENDOR);
    }

    /**
     * JRE vendor URL
     *
     * @return
     */
    public @Nullable URL getJavaVendorUrl() {
        return asURL(get(JAVA_VENDOR_URL));
    }

    /**
     * JRE version number
     *
     * @return
     */
    public @Nullable String getJavaVersion() {
        return get(JAVA_VERSION);
    }

    public @Nullable Path getJavaIoTmpdir() {
        return asPath(get(JAVA_IO_TMPDIR));
    }

    public @Nullable String getJavaCompiler() {
        return get(JAVA_COMPILER);
    }

    /**
     * Sequence used by operating system to separate lines in text files
     *
     * @return
     */
    public @Nullable String getLineSeparator() {
        return get(LINE_SEPARATOR);
    }

    /**
     * Operating system architecture
     *
     * @return
     */
    public @Nullable String getOsArch() {
        return get(OS_ARCH);
    }

    /**
     * Operating system name
     *
     * @return
     */
    public @Nullable String getOsName() {
        return get(OS_NAME);
    }

    /**
     * Operating system version
     *
     * @return
     */
    public @Nullable String getOsVersion() {
        return get(OS_VERSION);
    }

    /**
     * Path separator character used in java.class.path
     *
     * @return
     */
    public @Nullable Character getPathSeparator() {
        return asChar(get(PATH_SEPARATOR));
    }

    /**
     * User working directory
     *
     * @return
     */
    public @Nullable Path getUserDir() {
        return asPath(get(USER_DIR));
    }

    /**
     * User home directory
     *
     * @return
     */
    public @Nullable Path getUserHome() {
        return asPath(get(USER_HOME));
    }

    /**
     * User account name
     *
     * @return
     */
    public @Nullable String getUserName() {
        return get(USER_NAME);
    }

    public @Nullable String getJavaVmSpecificationVersion() {
        return get(JAVA_VM_SPECIFICATION_VERSION);
    }

    public @Nullable String getJavaVmSpecificationVendor() {
        return get(JAVA_VM_SPECIFICATION_VENDOR);
    }

    public @Nullable String getJavaVmSpecificationName() {
        return get(JAVA_VM_SPECIFICATION_NAME);
    }

    public @Nullable String getJavaVmVersion() {
        return get(JAVA_VM_VERSION);
    }

    public @Nullable String getJavaVmVendor() {
        return get(JAVA_VM_VENDOR);
    }

    public @Nullable String getJavaVmName() {
        return get(JAVA_VM_NAME);
    }

    public @Nullable String getJavaSpecificationVersion() {
        return get(JAVA_SPECIFICATION_VERSION);
    }

    public @Nullable String getJavaSpecificationVendor() {
        return get(JAVA_SPECIFICATION_VENDOR);
    }

    public @Nullable String getJavaSpecificationName() {
        return get(JAVA_SPECIFICATION_NAME);
    }

    private String get(String key) {
        return source.getProperty(key);
    }

    private Path asPath(String input) {
        return input != null ? fileSystem.getPath(input) : null;
    }

    private Character asChar(String input) {
        return input != null && input.length() == 1 ? input.charAt(0) : null;
    }

    private List<Path> asPaths(String input) {
        if (input != null && !input.isEmpty()) {
            Character pathSeparator = getPathSeparator();
            if (pathSeparator != null) {
                return Stream.of(input.split(pathSeparator.toString(), -1))
                        .map(this::asPath)
                        .collect(Collectors.toList());
            }
        }
        return Collections.emptyList();
    }

    private URL asURL(String input) {
        try {
            return input != null ? new URL(input) : null;
        } catch (MalformedURLException ex) {
            return null;
        }
    }
}

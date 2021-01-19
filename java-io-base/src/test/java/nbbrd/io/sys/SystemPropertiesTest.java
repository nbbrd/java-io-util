package nbbrd.io.sys;

import com.google.common.base.StandardSystemProperty;
import org.junit.Test;

import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

public class SystemPropertiesTest {

    @Test
    public void testFactories() {
        assertThatNullPointerException()
                .isThrownBy(() -> SystemProperties.of(null, FileSystems.getDefault()));

        assertThatNullPointerException()
                .isThrownBy(() -> SystemProperties.of(System.getProperties(), null));

        assertThat(SystemProperties.DEFAULT)
                .isNotNull();
    }

    @Test
    public void testGetUserHome() {
        Properties p = new Properties();
        SystemProperties x = SystemProperties.of(p, FileSystems.getDefault());

        assertThat(x.getUserHome())
                .isNull();

        p.put(SystemProperties.USER_HOME, "C:\\Temp");
        assertThat(x.getUserHome())
                .isEqualByComparingTo(Paths.get("C:\\Temp"));
    }

    @Test
    public void testGetUserDir() {
        Properties p = new Properties();
        SystemProperties x = SystemProperties.of(p, FileSystems.getDefault());

        assertThat(x.getUserDir())
                .isNull();

        p.put(SystemProperties.USER_DIR, "C:\\Temp");
        assertThat(x.getUserDir())
                .isEqualByComparingTo(Paths.get("C:\\Temp"));
    }

    @Test
    public void testGetJavaClassPath() {
        Properties p = new Properties();
        SystemProperties x = SystemProperties.of(p, FileSystems.getDefault());

        assertThat(x.getJavaClassPath())
                .isEmpty();

        p.put(SystemProperties.PATH_SEPARATOR, ";");
        assertThat(x.getJavaClassPath())
                .isEmpty();

        p.put(SystemProperties.JAVA_CLASS_PATH, "C:\\Temp\\x.jar");
        assertThat(x.getJavaClassPath())
                .containsExactly(Paths.get("C:\\Temp\\x.jar"));

        p.put(SystemProperties.JAVA_CLASS_PATH, "C:\\Temp\\x.jar;C:\\Temp\\y.jar");
        assertThat(x.getJavaClassPath())
                .containsExactly(
                        Paths.get("C:\\Temp\\x.jar"),
                        Paths.get("C:\\Temp\\y.jar")
                );
    }

    @Test
    public void testGetPathSeparator() {
        Properties p = new Properties();
        SystemProperties x = SystemProperties.of(p, FileSystems.getDefault());

        assertThat(x.getPathSeparator()).isNull();

        p.put(SystemProperties.PATH_SEPARATOR, "");
        assertThat(x.getPathSeparator()).isNull();

        p.put(SystemProperties.PATH_SEPARATOR, "; ");
        assertThat(x.getPathSeparator()).isNull();

        p.put(SystemProperties.PATH_SEPARATOR, ";");
        assertThat(x.getPathSeparator()).isEqualTo(';');
    }

    @Test
    public void testDEFAULT() {
        SystemProperties p = SystemProperties.DEFAULT;

        assertThat(p.getJavaVersion()).isEqualTo(StandardSystemProperty.JAVA_VERSION.value());
        assertThat(p.getJavaVendor()).isEqualTo(StandardSystemProperty.JAVA_VENDOR.value());
        assertThat(p.getJavaVendorUrl()).hasToString(StandardSystemProperty.JAVA_VENDOR_URL.value());
        assertThat(p.getJavaHome()).isEqualTo(Paths.get(StandardSystemProperty.JAVA_HOME.value()));
        assertThat(p.getJavaVmSpecificationVersion()).isEqualTo(StandardSystemProperty.JAVA_VM_SPECIFICATION_VERSION.value());
        assertThat(p.getJavaVmSpecificationVendor()).isEqualTo(StandardSystemProperty.JAVA_VM_SPECIFICATION_VENDOR.value());
        assertThat(p.getJavaVmSpecificationName()).isEqualTo(StandardSystemProperty.JAVA_VM_SPECIFICATION_NAME.value());
        assertThat(p.getJavaVmVersion()).isEqualTo(StandardSystemProperty.JAVA_VM_VERSION.value());
        assertThat(p.getJavaVmVendor()).isEqualTo(StandardSystemProperty.JAVA_VM_VENDOR.value());
        assertThat(p.getJavaVmName()).isEqualTo(StandardSystemProperty.JAVA_VM_NAME.value());
        assertThat(p.getJavaSpecificationVersion()).isEqualTo(StandardSystemProperty.JAVA_SPECIFICATION_VERSION.value());
        assertThat(p.getJavaSpecificationVendor()).isEqualTo(StandardSystemProperty.JAVA_SPECIFICATION_VENDOR.value());
        assertThat(p.getJavaSpecificationName()).isEqualTo(StandardSystemProperty.JAVA_SPECIFICATION_NAME.value());
        assertThat(p.getJavaClassVersion()).isEqualTo(StandardSystemProperty.JAVA_CLASS_VERSION.value());
        assertThat(p.getJavaClassPath()).containsExactlyElementsOf(splitPath(StandardSystemProperty.JAVA_CLASS_PATH.value()));
        assertThat(p.getJavaLibraryPath()).containsExactlyElementsOf(splitPath(StandardSystemProperty.JAVA_LIBRARY_PATH.value()));
        assertThat(p.getJavaIoTmpdir()).isEqualTo(Paths.get(StandardSystemProperty.JAVA_IO_TMPDIR.value()));
        assertThat(p.getJavaCompiler()).isEqualTo(StandardSystemProperty.JAVA_COMPILER.value());
        assertThat(p.getOsName()).isEqualTo(StandardSystemProperty.OS_NAME.value());
        assertThat(p.getOsArch()).isEqualTo(StandardSystemProperty.OS_ARCH.value());
        assertThat(p.getOsVersion()).isEqualTo(StandardSystemProperty.OS_VERSION.value());
        assertThat(p.getFileSeparator()).hasToString(StandardSystemProperty.FILE_SEPARATOR.value());
        assertThat(p.getPathSeparator()).hasToString(StandardSystemProperty.PATH_SEPARATOR.value());
        assertThat(p.getLineSeparator()).hasToString(StandardSystemProperty.LINE_SEPARATOR.value());
        assertThat(p.getUserName()).isEqualTo(StandardSystemProperty.USER_NAME.value());
        assertThat(p.getUserHome()).isEqualTo(Paths.get(StandardSystemProperty.USER_HOME.value()));
        assertThat(p.getUserDir()).isEqualTo(Paths.get(StandardSystemProperty.USER_DIR.value()));
    }

    private static List<Path> splitPath(String input) {
        return Stream.of(input.split(StandardSystemProperty.PATH_SEPARATOR.value(), -1))
                .map(Paths::get)
                .collect(Collectors.toList());
    }
}

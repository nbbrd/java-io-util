package _test;

import nbbrd.io.sys.SystemProperties;

public class Util {

    public static boolean isJDK8() {
        return SystemProperties.DEFAULT.getJavaVersion().contains("1.8");
    }
}

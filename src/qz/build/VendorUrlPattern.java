package qz.build;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Locale;

/**
 * Each JDK provider uses their own url format
 */
public enum VendorUrlPattern {
    ADOPT("https://github.com/adoptium/temurin%s-binaries/releases/download/jdk-%s/OpenJDK%sU-jdk_%s_%s_%s_%s.%s"),
    SEMERU("https://github.com/ibmruntimes/semeru%s-binaries/releases/download/jdk-%s_%s-%s/ibm-semeru-open-jdk_%s_%s_%s_%s-%s.%s"),
    BELL("https://download.bell-sw.com/java/%s/bellsoft-jdk%s-%s-%s.%s");

    private static final VendorUrlPattern DEFAULT_VENDOR = ADOPT;
    private static final Logger log = LogManager.getLogger(VendorUrlPattern.class);

    String pattern;
    VendorUrlPattern(String pattern) {
        this.pattern = pattern;
    }

    public static VendorUrlPattern getVendor(String vendor) {
        if(vendor != null) {
            for(VendorUrlPattern pattern : values()) {
                if (vendor.toUpperCase(Locale.ROOT).startsWith(pattern.name())) {
                    return pattern;
                }
            }
        }
        log.warn("Vendor provided couldn't be matched: {} will fallback to default: {}", vendor, DEFAULT_VENDOR);
        return null;
    }

    public static String format(String vendor, String arch, String platform, String gcEngine, String javaMajor, String javaVersion, String javaVersionFormatted, String gcVer, String fileExt) {
        VendorUrlPattern pattern = VendorUrlPattern.getVendor(vendor);
        switch(pattern) {
            case BELL:
                return String.format(pattern.pattern, javaVersion, javaVersion, platform, arch, fileExt);
            case SEMERU:
                return String.format(pattern.pattern, javaMajor, javaVersion, gcEngine, gcVer, arch, platform, javaVersionFormatted, gcEngine, gcVer, fileExt);
            case ADOPT:
            default:
                return String.format(pattern.pattern, javaMajor, javaVersion, javaMajor, arch, platform, gcEngine, javaVersionFormatted, fileExt);
        }
    }

}

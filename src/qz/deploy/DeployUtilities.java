/**
 * @author Tres Finocchiaro
 *
 * Copyright (C) 2016 Tres Finocchiaro, QZ Industries, LLC
 *
 * LGPL 2.1 This is free software.  This software and source code are released under
 * the "LGPL 2.1 License".  A copy of this license should be distributed with
 * this software. http://www.gnu.org/licenses/lgpl-2.1.html
 */

package qz.deploy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qz.common.Constants;
import qz.utils.SystemUtilities;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.Properties;

/**
 * Utility class for creating, querying and removing startup shortcuts and
 * desktop shortcuts.
 *
 * @author Tres Finocchiaro
 */
public abstract class DeployUtilities {

    // System logger
    protected static final Logger log = LoggerFactory.getLogger(DeployUtilities.class);

    // Default shortcut name to create
    static private final String DEFAULT_SHORTCUT_NAME = "Java Shortcut";

    private String jarPath;
    private String shortcutName;

    public boolean setAutostart(boolean autostart) {
        try {
            return writeStartupFile(autostart ? "1": "0");
        }
        catch(IOException e) {
            return false;
        }
    }

    public boolean isAutostart() {
        try {
            return readStartupFile().equals("1");
        }
        catch(IOException e) {
            return false;
        }
    }

    public abstract boolean hasStartupShortcut();

    /**
     * Creates a startup for the current OS. Automatically detects the OS and
     * places the shortcut item on the user's Desktop.
     *
     * @return Returns <code>true</code> if the startup item was created
     */
    public abstract boolean createDesktopShortcut();

    /**
     * Parses the parent directory from an absolute file URL. This will not work
     * with relative paths.<code>
     * // Good:
     * getWorkingPath("C:\Folder\MyFile.jar");
     * <p/>
     * // Bad:
     * getWorkingPath("C:\Folder\SubFolder\..\MyFile.jar");
     * </code>
     *
     * @param filePath Absolute path to a jar file
     * @return The calculated working path value, or an empty string if one
     * could not be determined
     */
    private static String getParentDirectory(String filePath) {
        // Working path should always default to the JARs parent folder
        int lastSlash = filePath.lastIndexOf(File.separator);
        return lastSlash < 0? "":filePath.substring(0, lastSlash);
    }

    public String getParentDirectory() {
        return getParentDirectory(getJarPath());
    }

    public void setShortcutName(String shortcutName) {
        if (shortcutName != null) {
            this.shortcutName = shortcutName;
        }
    }

    public String getShortcutName() {
        return shortcutName == null? DEFAULT_SHORTCUT_NAME:shortcutName;
    }

    /**
     * Detects the OS and creates the appropriate shortcut creator
     *
     * @return The appropriate shortcut creator for the currently running OS
     */
    public static DeployUtilities getSystemShortcutCreator() {
        if (SystemUtilities.isWindows()) {
            return new WindowsDeploy();
        } else if (SystemUtilities.isMac()) {
            return new MacDeploy();
        } else {
            return new LinuxDeploy();
        }
    }

    private static boolean writeStartupFile(String mode) throws IOException {
        Path autostartFile = Paths.get(SystemUtilities.getDataDirectory() , "autostart");
        Files.write(autostartFile, mode.getBytes(), StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
        return readStartupFile().equals(mode);
    }

    private static String readStartupFile() throws IOException {
        Path autostartFile = Paths.get(SystemUtilities.getDataDirectory() ,"autostart");
        if (!Files.exists(autostartFile)) {
            autostartFile = Paths.get(SystemUtilities.getSharedDataDirectory() ,"autostart");
            //If no autostart files are found, the program will autostart anyway as per requested business logic
            if (!Files.exists(autostartFile)) return "1";
        }
        return Files.readAllLines(autostartFile).get(0);
    }

    /**
     * Sets the executable permission flag for a file. This only works on
     * Linux/Unix.
     *
     * @param filePath The full file path to set the execute flag on
     * @return <code>true</code> if successful, <code>false</code> otherwise
     */
    @SuppressWarnings("ResultOfMethodCallIgnored")
    private static boolean setExecutable(String filePath) {
        if (!SystemUtilities.isWindows()) {
            try {
                File f = new File(filePath);
                f.setExecutable(true);
                return true;
            }
            catch(SecurityException e) {
                log.error("Unable to set file as executable: {}", filePath, e);
            }
        } else {
            return true;
        }
        return false;
    }

    /**
     * Gets the path to qz-tray.properties
     */
    private static String detectPropertiesPath() {
        // Use supplied path from IDE or command line
        // i.e  -DsslPropertiesFile=C:\qz-tray.properties
        String override = System.getProperty("sslPropertiesFile");
        if (override != null) {
            return override;
        }

        String jarPath = detectJarPath();
        String propFile = Constants.PROPS_FILE + ".properties";

        // Use relative path based on qz-tray.jar, fix %20
        return fixWhitespaces(getParentDirectory(jarPath) + File.separator + propFile);
    }

    /**
     * Returns a properties object containing the SSL properties infor
     */
    public static Properties loadTrayProperties() {
        Properties trayProps = new Properties();
        String trayPropsPath = DeployUtilities.detectPropertiesPath();
        log.info("Main properties file " + trayPropsPath);

        File propsFile = new File(trayPropsPath);
        try(FileInputStream inputStream = new FileInputStream(propsFile)) {
            trayProps.load(inputStream);
            return trayProps;
        }
        catch(IOException e) {
            e.printStackTrace();
            log.warn("Failed to load properties file!");
            return null;
        }
    }

    /**
     * Determines the currently running Jar's absolute path on the local filesystem
     *
     * @return A String value representing the absolute path to the currently running
     * jar
     */
    private static String detectJarPath() {
        try {
            return new File(DeployUtilities.class.getProtectionDomain()
                                    .getCodeSource().getLocation().getPath()).getCanonicalPath();
        }
        catch(IOException ex) {
            log.error("Unable to determine Jar path", ex);
        }
        return null;
    }

    /**
     * Returns the jar which we will create a shortcut for
     *
     * @return The path to the jar path which has been set
     */
    public String getJarPath() {
        if (jarPath == null) {
            jarPath = detectJarPath();
        }
        return jarPath;
    }

    /**
     * Small Enum for differentiating "desktop" and "startup"
     */
    public enum ToggleType {
        STARTUP, DESKTOP;

        /**
         * Returns the English description of this object
         *
         * @return The string "startup" or "desktop"
         */
        @Override
        public String toString() {
            return getName();
        }

        /**
         * Returns the English description of this object
         *
         * @return The string "startup" or "desktop"
         */
        public String getName() {
            return this.name() == null? null:this.name().toLowerCase();
        }
    }

    /**
     * Attempts to correct URL path conversions that occur on old JREs and older
     * Windows versions.  For now, just addresses %20 spaces, but
     * there could be other URLs which will need special consideration.
     *
     * @param filePath The absolute file path to convert
     * @return The converted path
     */
    public static String fixWhitespaces(String filePath) {
        return filePath.replace("%20", " ");
    }

}

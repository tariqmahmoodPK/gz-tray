package qz.installer;
/**
 * @author Tres Finocchiaro
 *
 * Copyright (C) 2019 Tres Finocchiaro, QZ Industries, LLC
 *
 * LGPL 2.1 This is free software.  This software and source code are released under
 * the "LGPL 2.1 License".  A copy of this license should be distributed with
 * this software. http://www.gnu.org/licenses/lgpl-2.1.html
 */

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qz.utils.FileUtilities;
import qz.utils.ShellUtilities;
import qz.utils.SystemUtilities;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;

import static qz.common.Constants.*;

public class MacInstaller extends Installer {
    protected static final Logger log = LoggerFactory.getLogger(MacInstaller.class);
    public static final String PACKAGE_NAME = getPackageName();
    private String destination = "/Applications/" + ABOUT_TITLE + ".app";

    public Installer addAppLauncher() {
        // not needed; registered when "QZ Tray.app" is copied
        return this;
    }

    public Installer addStartupEntry() {
        File dest = new File(String.format("/Library/LaunchAgents/%s.plist", PACKAGE_NAME));
        HashMap<String, String> fieldMap = new HashMap<>();
        // Dynamic fields
        fieldMap.put("%PACKAGE_NAME%", PACKAGE_NAME);
        fieldMap.put("%COMMAND%", String.format("%s/Contents/MacOS/%s", destination, ABOUT_TITLE));
        fieldMap.put("%PARAM%", "--honorautostart");

        try {
            FileUtilities.configureAssetFile("assets/mac-launchagent.plist.in", dest, fieldMap, MacInstaller.class);
        } catch(IOException e) {
            log.warn("Unable to write startup file: {}", dest, e);
        }

        return this;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public String getDestination() {
        return destination;
    }

    public Installer addSystemSettings() {
        // Chrome protocol handler
        String plist = "/Library/Preferences/com.google.Chrome.plist";
        if(ShellUtilities.execute(new String[] { "/usr/bin/defaults", "write", plist }, new String[] { "qz://*" }).isEmpty()) {
            ShellUtilities.execute("/usr/bin/defaults", "write", plist, "URLWhitelist", "-array-add", "qz://*");
        }
        return this;
    }
    public Installer removeSystemSettings() {
        // Remove startup entry
        File dest = new File(String.format("/Library/LaunchAgents/%s.plist", PACKAGE_NAME));
        dest.delete();
        return this;
    }

    /**
     * Removes legacy (<= 2.0) startup entries
     */
    public Installer removeLegacyStartup() {
        log.info("Removing startup entries for all users matching " + ABOUT_TITLE);
        String script = "tell application \"System Events\" to delete "
                + "every login item where name is \"" + ABOUT_TITLE + "\"";

        // Handle edge-case for when running from IDE
        File jar = new File(SystemUtilities.getJarPath());
        if(jar.getName().endsWith(".jar")) {
            script += " or name is \"" + jar.getName() + "\"";
        }

        // Run on background thread in case System Events is hung or slow to respond
        final String finalScript = script;
        new Thread(() -> {
            ShellUtilities.executeAppleScript(finalScript);
        }).run();
        return this;
    }

    public static String getAppPath() {
        // Return the Mac ".app" location
        String target = SystemUtilities.getJarPath();
        int appIndex = target.indexOf(".app/");
        if (appIndex > 0) {
            return target.substring(0, appIndex -1);
        }
        // Fallback on the ".jar" location
        return target;
    }

    public static String getPackageName() {
        String packageName;
        String[] parts = ABOUT_URL.split("\\W");
        if (parts.length >= 2) {
            // Parse io.qz.qz-print from Constants
            packageName = String.format("%s.%s.%s", parts[parts.length - 1], parts[parts.length - 2], PROPS_FILE);
        } else {
            // Fallback on something sane
            packageName = "local." + PROPS_FILE;
        }
        return packageName;
    }

    public void spawn(List<String> args) throws Exception {
        if(SystemUtilities.isAdmin()) {
            // macOS unconventionally uses "$USER" during its install process
            String whoami = System.getenv("USER");
            if(whoami == null || whoami.isEmpty() || whoami.equals("root")) {
                // Fallback, should only fire via Terminal + sudo
                whoami = ShellUtilities.executeRaw("logname").trim();
            }
            // Just in case any re-spawned
            TaskKiller.killAll();
            ShellUtilities.execute("su", whoami, "-c", "\"" + StringUtils.join(args, "\" \"") + "\"");
        } else {
            ShellUtilities.execute(args.toArray(new String[args.size()]));
        }
    }
}

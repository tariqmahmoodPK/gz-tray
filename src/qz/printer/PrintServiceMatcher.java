/**
 * @author Tres Finocchiaro
 *
 * Copyright (C) 2016 Tres Finocchiaro, QZ Industries, LLC
 *
 * LGPL 2.1 This is free software.  This software and source code are released under
 * the "LGPL 2.1 License".  A copy of this license should be distributed with
 * this software. http://www.gnu.org/licenses/lgpl-2.1.html
 */

package qz.printer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import qz.printer.info.MediaSizeHashSet;
import qz.printer.info.NativePrinter;
import qz.printer.info.NativePrinterMap;
import qz.utils.SystemUtilities;

import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import javax.print.attribute.ResolutionSyntax;
import javax.print.attribute.standard.*;
import java.util.*;

public class PrintServiceMatcher {
    private static final Logger log = LogManager.getLogger(PrintServiceMatcher.class);

    public static NativePrinterMap getNativePrinterList(boolean silent, boolean withAttributes) {
        NativePrinterMap printers = NativePrinterMap.getInstance();
        printers.putAll(PrintServiceLookup.lookupPrintServices(null, null));
        if (withAttributes) { printers.values().forEach(NativePrinter::getDriverAttributes); }
        if (!silent) { log.debug("Found {} printers", printers.size()); }
        return printers;
    }

    public static NativePrinterMap getNativePrinterList(boolean silent) {
        return getNativePrinterList(silent, false);
    }

    public static NativePrinterMap getNativePrinterList() {
        return getNativePrinterList(false);
    }

    public static NativePrinter getDefaultPrinter() {
        PrintService defaultService = PrintServiceLookup.lookupDefaultPrintService();

        if(defaultService == null) {
            return null;
        }

        NativePrinterMap printers = NativePrinterMap.getInstance();
        if (!printers.contains(defaultService)) {
            printers.putAll(defaultService);
        }

        return printers.get(defaultService);
    }

    public static String findPrinterName(String query) throws JSONException {
        NativePrinter printer = PrintServiceMatcher.matchPrinter(query);

        if (printer != null) {
            return printer.getPrintService().value().getName();
        } else {
            return null;
        }
    }

    /**
     * Finds {@code PrintService} by looking at any matches to {@code printerSearch}.
     *
     * @param printerSearch Search query to compare against service names.
     */
    public static NativePrinter matchPrinter(String printerSearch, boolean silent) {
        NativePrinter exact = null;
        NativePrinter begins = null;
        NativePrinter partial = null;

        if (!silent) { log.debug("Searching for PrintService matching {}", printerSearch); }

        NativePrinter defaultPrinter = getDefaultPrinter();
        if (defaultPrinter != null && printerSearch.equals(defaultPrinter.getName())) {
            if (!silent) { log.debug("Matched default printer, skipping further search"); }
            return defaultPrinter;
        }

        printerSearch = printerSearch.toLowerCase(Locale.ENGLISH);

        // Search services for matches
        for(NativePrinter printer : getNativePrinterList(silent).values()) {
            if (printer.getName() == null) {
                continue;
            }
            String printerName = printer.getName().toLowerCase(Locale.ENGLISH);
            if (printerName.equals(printerSearch)) {
                exact = printer;
                break;
            }
            if (printerName.startsWith(printerSearch)) {
                begins = printer;
                continue;
            }
            if (printerName.contains(printerSearch)) {
                partial = printer;
                continue;
            }

            if (SystemUtilities.isMac()) {
                // 1.9 compat: fallback for old style names
                PrinterName name = printer.getLegacyName();
                if (name == null || name.getValue() == null) { continue; }
                printerName = name.getValue().toLowerCase(Locale.ENGLISH);
                if (printerName.equals(printerSearch)) {
                    exact = printer;
                    continue;
                }
                if (printerName.startsWith(printerSearch)) {
                    begins = printer;
                    continue;
                }
                if (printerName.contains(printerSearch)) {
                    partial = printer;
                }
            }
        }

        // Return closest match
        NativePrinter use = null;
        if (exact != null) {
            use = exact;
        } else if (begins != null) {
            use = begins;
        } else if (partial != null) {
            use = partial;
        }

        if (use != null) {
            if(!silent) log.debug("Found match: {}", use.getPrintService().value().getName());
        } else {
            log.warn("Printer not found: {}", printerSearch);
        }

        return use;
    }

    public static NativePrinter matchPrinter(String printerSearch) {
        return matchPrinter(printerSearch, false);
    }

    public static JSONArray getPrintersJSON(boolean includeDetails) throws JSONException {
        JSONArray list = new JSONArray();

        PrintService defaultService = PrintServiceLookup.lookupDefaultPrintService();

        boolean mediaTrayCrawled = false;

        for(NativePrinter printer : getNativePrinterList().values()) {
            PrintService ps = printer.getPrintService().value();
            JSONObject jsonService = new JSONObject();
            jsonService.put("name", ps.getName());

            if (includeDetails) {
                jsonService.put("driver", printer.getDriver().value());
                jsonService.put("connection", printer.getConnection());
                jsonService.put("default", ps == defaultService);

                if (!mediaTrayCrawled) {
                    log.info("Gathering printer MediaTray information...");
                    mediaTrayCrawled = true;
                }

                MediaSizeHashSet sizes = new MediaSizeHashSet();

                for(Media m : (Media[])ps.getSupportedAttributeValues(Media.class, null, null)) {
                    if (m instanceof MediaTray) { jsonService.accumulate("trays", m.toString()); }
                    if (m instanceof MediaSizeName) {
                        sizes.add((MediaSizeName)m);
                    }
                }

                List<MediaSizeHashSet.UnitPair> sortedList = new ArrayList<>(sizes);
                Collections.sort(sortedList);

                for(MediaSizeHashSet.UnitPair pair : sortedList) {
                    // First, list as inches
                    JSONObject in = new JSONObject();
                    in.put("units", "in");
                    in.put("width", pair.getIn().getWidth());
                    in.put("height", pair.getIn().getHeight());
                    jsonService.accumulate("sizes", in);
                }

                for(MediaSizeHashSet.UnitPair pair : sortedList) {
                    // Second, list as millimeters
                    JSONObject mm = new JSONObject();
                    mm.put("units", "mm");
                    mm.put("width", pair.getMm().getWidth());
                    mm.put("height", pair.getMm().getHeight());
                    jsonService.accumulate("sizes", mm);
                }

                PrinterResolution res = printer.getResolution().value();
                int density = -1; if (res != null) { density = res.getFeedResolution(ResolutionSyntax.DPI); }
                jsonService.put("density", density);
            }

            list.put(jsonService);
        }

        return list;
    }

}

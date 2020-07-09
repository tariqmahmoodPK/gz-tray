package qz.printer.status;

import qz.printer.info.NativePrinterMap;
import qz.utils.SystemUtilities;

import java.util.Locale;

import static qz.printer.status.PrinterStatusType.*;
import static qz.utils.SystemUtilities.isMac;

/**
 * Created by kyle on 7/7/17.
 */
public class PrinterStatus {

    public PrinterStatusType type;
    public String issuingPrinterName;
    public String cupsString;


    public PrinterStatus(PrinterStatusType type, String issuingPrinterName) {
        this(type, issuingPrinterName, "");
    }

    public PrinterStatus(PrinterStatusType type, String issuingPrinterName, String cupsString) {
        this.type = type;
        this.issuingPrinterName = issuingPrinterName;
        this.cupsString = cupsString;
    }

    public static PrinterStatus[] getFromWMICode(int code, String issuingPrinterName) {
        if (code == 0) {
            return new PrinterStatus[] {new PrinterStatus(OK, issuingPrinterName)};
        }

        int bitPopulation = Integer.bitCount(code);
        PrinterStatus[] statusArray = new PrinterStatus[bitPopulation];
        int mask = 1;

        while(bitPopulation > 0) {
            if ((mask & code) > 0) {
                statusArray[--bitPopulation] = new PrinterStatus(codeLookupTable.get(mask), issuingPrinterName);
            }
            mask <<= 1;
        }
        return statusArray;
    }

    public static PrinterStatus getFromCupsString(String reason, String issuingPrinterName) {
        if (reason == null) { return null; }

        reason = reason.toLowerCase(Locale.ENGLISH).replaceAll("-(error|warning|report)", "");

        PrinterStatusType statusType = cupsLookupTable.get(reason);
        if (statusType == null) { statusType = UNKNOWN_STATUS; }

        return new PrinterStatus(statusType, issuingPrinterName, reason);
    }

    public String getPrinterName() {
        String name;
        //if (SystemUtilities.isMac()) {
        //    name = NativePrinterMap.getInstance().getPrinterIdByDescription(issuingPrinterName);
        //
        //} else {
            name = issuingPrinterName;
        //}
        return name;
    }

    public String toString() {
        String returnString = type.getName() + ": Level " + type.getSeverity() + ", StatusCode " + type.getCode() + ", From " + getPrinterName();
        if (!cupsString.isEmpty()) {
            returnString += ", CUPS string " + cupsString;
        }
        return returnString;
    }
}

package qz.printer.info;

import com.sun.jna.platform.win32.Advapi32Util;

import javax.print.PrintService;

import static com.sun.jna.platform.win32.WinReg.*;

public class WinWrapper extends Wrapper {
    public NativePrinterList wrapServices(PrintService[] services, NativePrinterList existing) {
        NativePrinterList printers = existing != null ? existing : new NativePrinterList();
        // TODO: Find way to NOT may system calls when a printer's already been added
        for (PrintService service : services) {
            String name = service.getName();
            NativePrinter printer = new NativePrinter(name);
            printer.setDescription(name);
            printer.setPrintService(service);
            printers.put(printer.getPrinterId(), printer);
        }
        return printers;
    }

    public void fillAttributes(NativePrinter printer) {
        String keyName = printer.getPrinterId().replaceAll("\\\\", ",");
        String key = "SYSTEM\\CurrentControlSet\\Control\\Print\\Printers\\" + keyName;
        String driver = getRegString(HKEY_LOCAL_MACHINE, key, "Printer Driver");
        if (driver == null) {
            key = "Printers\\Connections\\" + keyName;
            String guid = getRegString(HKEY_CURRENT_USER, key, "GuidPrinter");
            if (guid != null) {
                String serverName = keyName.replaceAll(",,(.+),.+", "$1");
                key = "Software\\Microsoft\\Windows NT\\CurrentVersion\\Print\\Providers\\Client Side Rendering Print Provider\\Servers\\" + serverName + "\\Printers\\" + guid;
                driver = getRegString(HKEY_LOCAL_MACHINE, key, "Printer Driver");
            }
        }
        printer.setDriver(driver);
    }

    private static String getRegString(HKEY root, String key, String value) {
        if (Advapi32Util.registryKeyExists(root, key) && Advapi32Util.registryValueExists(root, key, value)) {
            return Advapi32Util.registryGetStringValue(root, key, value);
        }
        return null;
    }
}

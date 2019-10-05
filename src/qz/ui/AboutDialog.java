package qz.ui;

import com.github.zafarkhaja.semver.Version;
import org.eclipse.jetty.server.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qz.common.AboutInfo;
import qz.common.Constants;
import qz.ui.component.IconCache;
import qz.ui.component.LinkLabel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.font.TextAttribute;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Tres on 2/26/2015.
 * Displays a basic about dialog
 */
public class AboutDialog extends BasicDialog implements Themeable {

    private static final Logger log = LoggerFactory.getLogger(AboutDialog.class);

    private Server server;

    private boolean limitedDisplay;

    private JLabel lblUpdate;
    private JButton updateButton;

    public AboutDialog(JMenuItem menuItem, IconCache iconCache) {
        super(menuItem, iconCache);

        //noinspection ConstantConditions - white label support
        limitedDisplay = Constants.VERSION_CHECK_URL.isEmpty();
    }

    public void setServer(Server server) {
        this.server = server;

        initComponents();
    }

    public void initComponents() {
        setIconImage(getImage(IconCache.Icon.ABOUT_ICON));

        JLabel lblAbout = new JLabel(Constants.ABOUT_TITLE);
        lblAbout.setFont(new Font(null, Font.PLAIN, 36));

        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
        infoPanel.setPreferredSize(new Dimension(320, 260));

        LinkLabel linkLibrary = new LinkLabel("Detailed library information");
        linkLibrary.setLinkLocation(String.format("%s://%s:%s", server.getURI().getScheme(), AboutInfo.getPreferredHostname(), server.getURI().getPort()));

        if (!limitedDisplay) {
            LinkLabel linkNew = new LinkLabel("What's New?");
            linkNew.setLinkLocation(Constants.VERSION_DOWNLOAD_URL);

            lblUpdate = new JLabel();
            updateButton = new JButton();
            updateButton.setVisible(false);
            updateButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent evt) {
                    try { Desktop.getDesktop().browse(new URL(Constants.ABOUT_URL + "/download").toURI()); }
                    catch(Exception e) { log.error("", e); }
                }
            });
            checkForUpdate();

            Box versionBox = Box.createHorizontalBox();
            versionBox.setAlignmentX(Component.LEFT_ALIGNMENT);
            versionBox.add(new JLabel(String.format("%s (Java)", Constants.VERSION.toString())));
            versionBox.add(Box.createHorizontalStrut(12));
            versionBox.add(linkNew);

            infoPanel.add(lblAbout);
            infoPanel.add(Box.createVerticalGlue());
            infoPanel.add(versionBox);
            infoPanel.add(Box.createVerticalGlue());
            infoPanel.add(lblUpdate);
            infoPanel.add(updateButton);
            infoPanel.add(Box.createVerticalGlue());
            infoPanel.add(new JLabel(String.format("<html>%s is written and supported by %s.</html>", Constants.ABOUT_TITLE, Constants.ABOUT_COMPANY)));
            infoPanel.add(Box.createVerticalGlue());
            infoPanel.add(new JLabel(String.format("<html>If using %s commercially, please first reach out to the website publisher for support issues.</html>", Constants.ABOUT_TITLE)));
            infoPanel.add(Box.createVerticalGlue());
            infoPanel.add(linkLibrary);
        } else {
            LinkLabel linkLabel = new LinkLabel(Constants.ABOUT_URL);
            linkLabel.setLinkLocation(Constants.ABOUT_URL);

            infoPanel.add(Box.createVerticalGlue());
            infoPanel.add(lblAbout);
            infoPanel.add(Box.createVerticalStrut(16));
            infoPanel.add(linkLabel);
            infoPanel.add(Box.createVerticalStrut(8));
            infoPanel.add(linkLibrary);
            infoPanel.add(Box.createVerticalGlue());
            infoPanel.add(Box.createHorizontalStrut(16));
        }


        JPanel aboutPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        aboutPanel.add(new JLabel(getIcon(IconCache.Icon.LOGO_ICON)));
        aboutPanel.add(infoPanel);

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));
        panel.add(aboutPanel);
        panel.add(new JToolBar.Separator());

        if (!limitedDisplay) {
            //override font to remove underline for these links
            Font lblFont = new Font(null, Font.PLAIN, 12);
            Map<TextAttribute,Object> attributes = new HashMap<>(lblFont.getAttributes());
            attributes.remove(TextAttribute.UNDERLINE);
            lblFont = lblFont.deriveFont(attributes);

            LinkLabel lblLicensing = new LinkLabel("Licensing Information");
            lblLicensing.setLinkLocation(Constants.ABOUT_URL + "/licensing");
            lblLicensing.setFont(lblFont);

            LinkLabel lblSupport = new LinkLabel("Support Information");
            lblSupport.setLinkLocation(Constants.ABOUT_URL + "/support");
            lblSupport.setFont(lblFont);

            LinkLabel lblPrivacy = new LinkLabel("Privacy Policy");
            lblPrivacy.setLinkLocation(Constants.ABOUT_URL + "/privacy");
            lblPrivacy.setFont(lblFont);

            JPanel supportPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 80, 10));
            supportPanel.add(lblLicensing);
            supportPanel.add(lblSupport);
            supportPanel.add(lblPrivacy);

            panel.add(supportPanel);
        }

        setContent(panel, true);
    }

    private void checkForUpdate() {
        Version latestVersion = AboutInfo.findLatestVersion();
        if (latestVersion.greaterThan(Constants.VERSION)) {
            lblUpdate.setText("An update is available:");

            updateButton.setText("Download " + latestVersion.toString());
            updateButton.setVisible(true);
        } else if (latestVersion.lessThan(Constants.VERSION)) {
            lblUpdate.setText("You are on a beta release.");

            updateButton.setText("Revert to stable " + latestVersion.toString());
            updateButton.setVisible(true);
        } else {
            lblUpdate.setText("You have the latest version.");

            updateButton.setVisible(false);
        }
    }


    @Override
    public void setVisible(boolean visible) {
        if (visible && !limitedDisplay) {
            checkForUpdate();
        }

        super.setVisible(visible);
    }


}

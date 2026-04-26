package org.apache.ant.cyclonedx;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.CodeSource;
import java.util.Collections;
import java.util.Properties;

import org.cyclonedx.Version;
import org.cyclonedx.model.Component;
import org.cyclonedx.model.License;
import org.cyclonedx.model.LicenseChoice;
import org.cyclonedx.model.OrganizationalEntity;
import org.cyclonedx.model.metadata.ToolInformation;
import org.cyclonedx.util.BomUtils;

/**
 * Provides tool information for BOM's metadata section.
 */
public class ToolData {
    private static ToolInformation cachedToolInformation;

    /**
     * Tool Information needed for BOM's metadata section.
     */
    public static ToolInformation getToolInformation() throws IOException {
        return cachedToolInformation != null ? cachedToolInformation
            : (cachedToolInformation = cacheToolInformation());
    }

    private static ToolInformation cacheToolInformation() throws IOException {
        ToolInformation tool = new ToolInformation();
        Component antlibComponent = new Component();

        antlibComponent.setType(Component.Type.LIBRARY);
        antlibComponent.setGroup("org.apache.ant");
        antlibComponent.setName("ant-cyclonedx");
        antlibComponent.setVersion(getVersion());
        antlibComponent.setDescription("Apache CycloneDX Antlib");

        OrganizationalEntity manufacturer = new OrganizationalEntity();
        manufacturer.setName("Apache Ant Development Team");
        manufacturer.setUrls(Collections.singletonList("https://ant.apache.org/"));
        antlibComponent.setManufacturer(manufacturer);

        LicenseChoice lc = new LicenseChoice();
        License license = new License();
        license.setId("Apache-2.0");
        lc.setLicenses(Collections.singletonList(license));
        antlibComponent.setLicenses(lc);

        File antlib = findAntlib();
        if (antlib != null) {
            antlibComponent.setHashes(BomUtils.calculateHashes(antlib, Version.VERSION_16));
        }

        tool.setComponents(Collections.singletonList(antlibComponent));
        return tool;
    }

    private static String getVersion() {
        String version = null;
        try (InputStream in =
             ToolData.class.getResourceAsStream("/org/apache/ant/cyclonedx/version.properties")) {
            Properties props = new Properties();
            props.load(in);
            version = props.getProperty("artifact.version");
        } catch (Exception ex) {
            // silently fall back to unknown version
        }
        return version == null ? "unknown" : version;
    }

    private static File findAntlib() {
        CodeSource antlibSource = ToolData.class.getProtectionDomain().getCodeSource();
        if (antlibSource == null) {
            return null;
        }
        URL location = antlibSource.getLocation();
        if (location.getProtocol() == "file") {
            return new File(location.getPath());
        }
        return null;
    }

}

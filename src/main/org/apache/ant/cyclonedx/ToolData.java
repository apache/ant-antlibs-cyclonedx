package org.apache.ant.cyclonedx;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.CodeSource;
import java.util.Collections;
import java.util.Properties;

import org.apache.tools.ant.types.resources.FileResource;
import org.apache.tools.ant.types.resources.URLResource;

import org.cyclonedx.Version;
import org.cyclonedx.model.ExternalReference;
import org.cyclonedx.model.metadata.ToolInformation;

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

        antlibComponent.setGroup("org.apache.ant");
        antlibComponent.setName("ant-cyclonedx");
        antlibComponent.setVersion(getVersion());
        antlibComponent.setDescription("Apache CycloneDX Antlib");

        Component.Organization manufacturer = antlibComponent.createManufacturer();
        manufacturer.setName("Apache Ant Development Team");
        manufacturer.addConfiguredUrl(new URLResource("https://ant.apache.org/"));
        antlibComponent.setManufacturerIsSupplier(true);

        Component.License license = new Component.License();
        license.setLicenseId("Apache-2.0");
        antlibComponent.addConfiguredLicense(license);

        Component.ExternalReference repo = new Component.ExternalReference();
        repo.setUrl("https://github.com/apache/ant-antlibs-cyclonedx");
        repo.setType(ExternalReference.Type.VCS);
        antlibComponent.addConfiguredExternalReference(repo);

        File antlib = findAntlib();
        if (antlib != null) {
            antlibComponent.add(new FileResource(antlib));
        }

        org.cyclonedx.model.Component cdxComponent =
            antlibComponent.toMainCycloneDxComponent(Version.VERSION_16);
        cdxComponent.setBomRef(null);
        tool.setComponents(Collections.singletonList(cdxComponent));
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

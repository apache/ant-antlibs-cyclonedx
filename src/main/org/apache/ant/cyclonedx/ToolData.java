package org.apache.ant.cyclonedx;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.CodeSource;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.tools.ant.types.resources.FileResource;
import org.apache.tools.ant.types.resources.URLResource;

import org.cyclonedx.Version;
import org.cyclonedx.model.ExternalReference;
import org.cyclonedx.model.metadata.ToolInformation;

/**
 * Provides tool information for BOM's metadata section.
 */
class ToolData {
    private static Map<Version, ToolInformation> toolInformationCache = new HashMap<>();

    /**
     * Tool Information needed for BOM's metadata section.
     */
    static ToolInformation getToolInformation(Version specVersion) throws IOException {
        ToolInformation cachedToolInformation = toolInformationCache.get(specVersion);
        if (cachedToolInformation == null) {
            cachedToolInformation = cacheToolInformation(specVersion);
            toolInformationCache.put(specVersion, cachedToolInformation);
        }
        return cachedToolInformation;
    }

    private static ToolInformation cacheToolInformation(Version specVersion) throws IOException {
        ToolInformation tool = new ToolInformation();
        Component antlibComponent = new Component();

        antlibComponent.setGroup("org.apache.ant");
        antlibComponent.setName("ant-cyclonedx");
        antlibComponent.setVersion(getVersion());
        antlibComponent.setDescription("Apache CycloneDX Antlib");
        antlibComponent.setPublisher("The Apache Software Foundation");

        Organization supplier = new Organization();
        supplier.setName("Apache Ant Project Management Committee");
        supplier.addConfiguredUrl(new URLResource("https://ant.apache.org/"));
        antlibComponent.addSupplier(supplier);
        antlibComponent.setSupplierIsManufacturer(true);

        License license = new License();
        license.setLicenseId("Apache-2.0");
        license.addConfiguredUrl(new URLResource("https://www.apache.org/licenses/LICENSE-2.0.txt"));
        antlibComponent.addConfiguredLicense(license);

        antlibComponent.addConfiguredExternalReference(
           createExternalReference(ExternalReference.Type.VCS,
                                   "https://gitbox.apache.org/repos/asf/ant-antlibs-cyclonedx.git"));
        antlibComponent.addConfiguredExternalReference(
           createExternalReference(ExternalReference.Type.LICENSE,
                                   "https://www.apache.org/licenses/LICENSE-2.0.txt"));
        antlibComponent.addConfiguredExternalReference(
           createExternalReference(ExternalReference.Type.BUILD_SYSTEM,
                                   "https://ci-builds.apache.org/job/Ant/job/CycloneDX%20Antlib/"));
        antlibComponent.addConfiguredExternalReference(
           createExternalReference(ExternalReference.Type.MAILING_LIST,
                                   "https://ant.apache.org/mail.html"));
        antlibComponent.addConfiguredExternalReference(
           createExternalReference(ExternalReference.Type.ISSUE_TRACKER,
                                   "https://bz.apache.org/bugzilla/buglist.cgi?component=CycloneDX%20Antlib&product=Ant"));
        antlibComponent.addConfiguredExternalReference(
           createExternalReference(ExternalReference.Type.WEBSITE,
                                   "https://ant.apache.org/antlibs/cyclonedx/"));
        antlibComponent.addConfiguredExternalReference(
           createExternalReference(ExternalReference.Type.DISTRIBUTION,
                                   "https://ant.apache.org/antlibs/bindownload.cgi"));
        antlibComponent.addConfiguredExternalReference(
           createExternalReference(ExternalReference.Type.SOURCE_DISTRIBUTION,
                                   "https://ant.apache.org/antlibs/srcdownload.cgi"));
        antlibComponent.addConfiguredExternalReference(
           createExternalReference(ExternalReference.Type.SECURITY_CONTACT,
                                   "https://www.apache.org/security/"));

        File antlib = findAntlib();
        if (antlib != null) {
            antlibComponent.add(new FileResource(antlib));
        }

        org.cyclonedx.model.Component cdxComponent =
            antlibComponent.toMainCycloneDxComponent(specVersion);
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

    private static org.apache.ant.cyclonedx.ExternalReference
        createExternalReference(ExternalReference.Type type, String url) {
        org.apache.ant.cyclonedx.ExternalReference e = new org.apache.ant.cyclonedx.ExternalReference();
        e.setUrl(url);
        e.setType(type.name());
        return e;
    }
}

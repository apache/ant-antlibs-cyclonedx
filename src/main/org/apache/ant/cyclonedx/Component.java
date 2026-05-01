package org.apache.ant.cyclonedx;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.types.Resource;
import org.apache.tools.ant.types.resources.FileProvider;
import org.apache.tools.ant.types.resources.URLResource;

import org.cyclonedx.Version;
import org.cyclonedx.model.LicenseChoice;
import org.cyclonedx.model.OrganizationalEntity;
import org.cyclonedx.util.BomUtils;

public class Component {
    private Resource resource;
    private org.cyclonedx.model.Component.Type type = org.cyclonedx.model.Component.Type.LIBRARY;
    private String name;
    private String group;
    private String version;
    private String description;
    private Manufacturer manufacturer = null;
    private List<org.cyclonedx.model.License> licenses = new ArrayList<>();
    private String purl;
    private String bomRef;

    public void add(Resource resource) {
        if (this.resource != null) {
            throw new BuildException("component can only be defined for a single asset");
        }
        this.resource = resource;
    }

    public void setType(org.cyclonedx.model.Component.Type type) {
        this.type = type;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Manufacturer createManufacturer() {
        if (manufacturer != null) {
            throw new BuildException("component can only have one manufacturer");
        }
        manufacturer = new Manufacturer();
        return manufacturer;
    }

    public void addConfiguredLicense(License l) {
        licenses.add(l.toCycloneDxLicense());
    }

    public void setPurl(String purl) {
        this.purl = purl;
    }

    public String getPurl() {
        if (purl != null) {
            return purl;
        }
        if (group != null && name != null && version != null) {
            return "pkg:maven/" + group + "/" + name + "@" + version + "?type=jar";
        }
        return null;
    }

    public void setBomRef(String bomRef) {
        this.bomRef = bomRef;
    }

    public String getBomRef() {
        if (bomRef == null) {
            return getPurl();
        }
        return bomRef;
    }

    public org.cyclonedx.model.Component toCycloneDxComponent(Version bomVersion)
        throws IOException {
        if (name == null) {
            throw new BuildException("component name is required");
        }

        org.cyclonedx.model.Component component = new org.cyclonedx.model.Component();

        component.setType(type);
        component.setName(name);
        if (group != null) {
            component.setGroup(group);
        }
        if (version != null) {
            component.setVersion(version);
        }
        if (description != null) {
            component.setDescription(description);
        }
        if (manufacturer != null) {
            component.setManufacturer(manufacturer.toOrganizationalEntity());
        }
        if (!licenses.isEmpty()) {
            LicenseChoice lc = new LicenseChoice();
            lc.setLicenses(licenses);
            component.setLicenses(lc);
        }
        String purl = getPurl();
        if (purl != null) {
            component.setPurl(purl);
        }
        String bomRef = getBomRef();
        if (bomRef != null) {
            component.setBomRef(bomRef);
        }
        addHashes(component, bomVersion);
        return component;
    }

    private void addHashes(org.cyclonedx.model.Component component, Version bomVersion)
        throws IOException {
        if (resource == null) {
            return;
        }

        if (!resource.isFilesystemOnly()) {
            throw new BuildException("component resource must be a file system resource");
        }

        File file = null;
        FileProvider fp = resource.as(FileProvider.class);
        if (fp != null) {
            file = fp.getFile();
        }
        if (file == null || !file.isFile()) {
            throw new BuildException("component resource doesn't provide a file");
        }

        component.setHashes(BomUtils.calculateHashes(file, bomVersion));
    }

    public static class Manufacturer {
        private String name;
        private List<String> urls = new ArrayList<>();

        public void setName(String name) {
            this.name = name;
        }

        public void addConfiguredUrl(URLResource url) {
            urls.add(url.getURL().toExternalForm());
        }

        public OrganizationalEntity toOrganizationalEntity() {
            OrganizationalEntity oe = new OrganizationalEntity();
            if (name != null) {
                oe.setName(name);
            }
            if (!urls.isEmpty()) {
                oe.setUrls(urls);
            }
            return oe;
        }
    }

    public static class License {
        private String id;
        private String name;

        public void setLicenseId(String id) {
            this.id = id;
        }

        public void setName(String name) {
            this.name = name;
        }

        public org.cyclonedx.model.License toCycloneDxLicense() {
            if (name == null && id == null) {
                throw new BuildException("license name or id is required");
            }
            org.cyclonedx.model.License l = new org.cyclonedx.model.License();
            if (name != null) {
                l.setName(name);
            }
            if (id != null) {
                l.setId(id);
            }
            return l;
        }
    }
}

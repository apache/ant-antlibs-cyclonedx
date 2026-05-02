package org.apache.ant.cyclonedx;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.ProjectComponent;
import org.apache.tools.ant.types.DataType;
import org.apache.tools.ant.types.Resource;
import org.apache.tools.ant.types.resources.FileProvider;

import org.cyclonedx.Version;
import org.cyclonedx.model.LicenseChoice;
import org.cyclonedx.model.OrganizationalEntity;
import org.cyclonedx.util.BomUtils;

public class Component extends DataType {
    private Resource resource;
    private org.cyclonedx.model.Component.Type type = org.cyclonedx.model.Component.Type.LIBRARY;
    private String name;
    private String group;
    private String version;
    private String description;
    private Organization manufacturer = null;
    private Organization supplier = null;
    private boolean manufacturerIsSupplier = false;
    private List<org.cyclonedx.model.License> licenses = new ArrayList<>();
    private String purl;
    private String bomRef;
    private List<org.cyclonedx.model.ExternalReference> externalReferences = new ArrayList<>();
    private org.cyclonedx.model.Component.Scope scope;
    private boolean isExternal = false;
    private List<Dependency> dependencies = new ArrayList<>();
    private boolean unknownDependencies = false;

    public void add(Resource resource) {
        checkChildrenAllowed();
        if (this.resource != null) {
            throw new BuildException("component can only be defined for a single asset");
        }
        this.resource = resource;
    }

    public void setType(org.cyclonedx.model.Component.Type type) {
        checkAttributesAllowed();
        this.type = type;
    }

    public void setName(String name) {
        checkAttributesAllowed();
        this.name = name;
    }

    public void setGroup(String group) {
        checkAttributesAllowed();
        this.group = group;
    }

    public void setVersion(String version) {
        checkAttributesAllowed();
        this.version = version;
    }

    public void setDescription(String description) {
        checkAttributesAllowed();
        this.description = description;
    }

    public void addManufacturer(Organization manufacturer) {
        checkChildrenAllowed();
        if (this.manufacturer != null) {
            throw new BuildException("component can only have one manufacturer");
        }
        this.manufacturer = manufacturer;
    }

    public void addSupplier(Organization supplier) {
        checkChildrenAllowed();
        if (this.supplier != null) {
            throw new BuildException("component can only have one supplier");
        }
        this.supplier = supplier;
    }

    public void setManufacturerIsSupplier(boolean manufacturerIsSupplier) {
        checkAttributesAllowed();
        this.manufacturerIsSupplier = manufacturerIsSupplier;
    }

    public void addConfiguredLicense(License l) {
        checkChildrenAllowed();
        licenses.add(l.toCycloneDxLicense());
    }

    public void setPurl(String purl) {
        checkAttributesAllowed();
        this.purl = purl;
    }

    public String getPurl() {
        if (isReference()) {
            return getRef().getPurl();
        }
        if (purl != null) {
            return purl;
        }
        if (group != null && name != null && version != null) {
            return "pkg:maven/" + group + "/" + name + "@" + version + "?type=jar";
        }
        return null;
    }

    public void setBomRef(String bomRef) {
        checkAttributesAllowed();
        this.bomRef = bomRef;
    }

    public String getBomRef() {
        if (isReference()) {
            return getRef().getBomRef();
        }
        if (bomRef == null) {
            return getPurl();
        }
        return bomRef;
    }

    public void addConfiguredExternalReference(ExternalReference ref) {
        checkChildrenAllowed();
        externalReferences.add(ref.toCycloneDxExternalReference());
    }

    public void setScope(org.cyclonedx.model.Component.Scope scope) {
        checkAttributesAllowed();
        this.scope = scope;
    }

    public void setIsExternal(boolean isExternal) {
        checkAttributesAllowed();
        this.isExternal = isExternal;
    }

    public void addDependency(Dependency d) {
        checkChildrenAllowed();
        dependencies.add(d);
    }

    public Iterable<Dependency> getDependencies() {
        if (isReference()) {
            return getRef().getDependencies();
        }
        return dependencies;
    }

    public void setUnknownDependencies(boolean unknownDependencies) {
        checkAttributesAllowed();
        this.unknownDependencies = unknownDependencies;
    }

    public boolean areDependenciesUnknown() {
        if (isReference()) {
            return getRef().areDependenciesUnknown();
        }
        return unknownDependencies;
    }

    public org.cyclonedx.model.Component toMainCycloneDxComponent(Version bomVersion)
        throws IOException {
        if (isReference()) {
            return getRef().toMainCycloneDxComponent(bomVersion);
        }
        if (isExternal) {
            throw new BuildException("isExternal can not be true for the main bom component");
        }
        return toCycloneDxComponent(bomVersion);
    }

    public org.cyclonedx.model.Component toAdditionalCycloneDxComponent(Version bomVersion)
        throws IOException {
        if (isReference()) {
            return getRef().toAdditionalCycloneDxComponent(bomVersion);
        }
        org.cyclonedx.model.Component component = toCycloneDxComponent(bomVersion);
        if (scope != null) {
            component.setScope(scope);
        }
        return component;
    }

    private org.cyclonedx.model.Component toCycloneDxComponent(Version bomVersion)
        throws IOException {
        if (name == null) {
            throw new BuildException("component name is required");
        }
        if (manufacturerIsSupplier) {
            if (manufacturer == null) {
                throw new BuildException("component without manufacturer can't use manufacturer as supplier");
            }
            if (supplier != null) {
                throw new BuildException("component with supplier can't use manufacturer as supplier");
            }
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
            OrganizationalEntity oe = manufacturer.toOrganizationalEntity();
            component.setManufacturer(oe);
            if (manufacturerIsSupplier) {
                component.setSupplier(oe);
            }
        }
        if (supplier != null) {
            component.setSupplier(supplier.toOrganizationalEntity());
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
        } else if (!dependencies.isEmpty()) {
            throw new BuildException("a component with dependencies must provide a bomRef");
        }
        if (!externalReferences.isEmpty()) {
            component.setExternalReferences(externalReferences);
        }
        // add isExternal once VERSION_17 is supported by cyclonedx-java-core
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

    public static class ExternalReference {
        private String url;
        private org.cyclonedx.model.ExternalReference.Type type;

        public void setUrl(String url) {
            this.url = url;
        }

        public void setType(org.cyclonedx.model.ExternalReference.Type type) {
            this.type = type;
        }

        public org.cyclonedx.model.ExternalReference toCycloneDxExternalReference() {
            if (url == null) {
                throw new BuildException("external references must have an url");
            }
            if (type == null) {
                throw new BuildException("external references must have a type");
            }
            org.cyclonedx.model.ExternalReference r = new org.cyclonedx.model.ExternalReference();
            r.setUrl(url);
            r.setType(type);
            return r;
        }
    }

    public static class Dependency extends ProjectComponent {
        private String bomRef;
        private String componentRef;

        public void setBomRef(String bomRef) {
            this.bomRef = bomRef;
        }

        public void setComponentRef(String componentRef) {
            this.componentRef = componentRef;
        }

        public String getBomRef() {
            if (bomRef == null && componentRef == null) {
                throw new BuildException("bomRef or componentRef is required");
            }
            if (bomRef != null && componentRef != null) {
                throw new BuildException("only one of bomRef and componentRef are permitted");
            }
            if (bomRef != null) {
                return bomRef;
            }

            Object component = getProject().getReference(componentRef);
            if (component == null) {
                throw new BuildException("componentRef '" + componentRef + "' is unknown");
            }
            if (component instanceof Component) {
                String b = ((Component) component).getBomRef();
                if (b == null) {
                    throw new BuildException("component with id '" + componentRef + "' doesn't provide a bomRef");
                }
                return b;
            }
            throw new BuildException("componentRef '" + componentRef + "' doesn't refer to a component");
        }
    }

    /**
     * Perform the check for circular references and return the
     * referenced Resource.
     * @return <code>Component</code>.
     */
    protected Component getRef() {
        return getCheckedRef(Component.class);
    }
}

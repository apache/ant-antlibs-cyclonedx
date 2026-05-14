package org.apache.ant.cyclonedx;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Stack;
import java.util.stream.Collectors;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.ProjectComponent;
import org.apache.tools.ant.types.DataType;
import org.apache.tools.ant.types.Resource;
import org.apache.tools.ant.types.resources.FileProvider;
import org.apache.tools.ant.types.resources.Union;

import org.cyclonedx.Version;
import org.cyclonedx.exception.ParseException;
import org.cyclonedx.model.Bom;
import org.cyclonedx.model.LicenseChoice;
import org.cyclonedx.model.OrganizationalContact;
import org.cyclonedx.model.OrganizationalEntity;
import org.cyclonedx.model.Property;
import org.cyclonedx.model.component.Tags;
import org.cyclonedx.parsers.BomParserFactory;
import org.cyclonedx.parsers.Parser;
import org.cyclonedx.util.BomUtils;

public class Component extends DataType {
    private Resource resource;
    private org.cyclonedx.model.Component.Type type = org.cyclonedx.model.Component.Type.LIBRARY;
    private String name;
    private String group;
    private String publisher;
    private String version;
    private String description;
    private String copyright;
    private Organization manufacturer = null;
    private Organization supplier = null;
    private boolean manufacturerIsSupplier = false;
    private List<org.cyclonedx.model.License> licenses = new ArrayList<>();
    private String purl;
    private String bomRef;
    private List<org.cyclonedx.model.ExternalReference> externalReferences = new ArrayList<>();
    private org.cyclonedx.model.Component.Scope scope;
    private boolean isExternal = false;
    private List<Component> nestedComponents = new ArrayList<>();
    private List<Dependency> dependencies = new ArrayList<>();
    private boolean unknownDependencies = false;
    private boolean sbomLinkResolved = false;
    private List<OrganizationalContact> authors = new ArrayList<>();
    private List<Tag> tags = new ArrayList<>();
    private List<Property> properties = new ArrayList<>();
    private String mimeType;
    private Union sbomLink;

    public void add(Resource resource) {
        checkChildrenAllowed();
        if (this.resource != null) {
            throw new BuildException("component can only be defined for a single asset");
        }
        this.resource = resource;
    }

    public void setType(ComponentType type) {
        checkAttributesAllowed();
        this.type = type.getType();
    }

    public void setName(String name) {
        checkAttributesAllowed();
        this.name = name;
    }

    public String getName() {
        if (isReference()) {
            return getRef().getName();
        }
        dieOnCircularReference();
        return name;
    }

    public void setGroup(String group) {
        checkAttributesAllowed();
        this.group = group;
    }

    public String getGroup() {
        if (isReference()) {
            return getRef().getGroup();
        }
        dieOnCircularReference();
        return group;
    }

    public void setVersion(String version) {
        checkAttributesAllowed();
        this.version = version;
    }

    public void setDescription(String description) {
        checkAttributesAllowed();
        this.description = description;
    }

    public void setPublisher(String publisher) {
        checkAttributesAllowed();
        this.publisher = publisher;
    }

    public void setCopyright(String copyright) {
        checkAttributesAllowed();
        this.copyright = copyright;
    }

    public void setMimeType(String mimeType) {
        checkAttributesAllowed();
        this.mimeType = mimeType;
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

    public void addAuthor(OrganizationalContact author) {
        checkChildrenAllowed();
        authors.add(author);
    }

    public void addTag(Tag tag) {
        checkChildrenAllowed();
        tags.add(tag);
    }

    public void addProperty(Property property) {
        checkChildrenAllowed();
        properties.add(property);
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
        dieOnCircularReference();
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
        dieOnCircularReference();
        if (bomRef == null) {
            return getPurl();
        }
        return bomRef;
    }

    public void addConfiguredExternalReference(ExternalReference ref) {
        checkChildrenAllowed();
        externalReferences.add(ref.toCycloneDxExternalReference());
    }

    public void addConfiguredExternalReferenceSet(ExternalReferenceSet set) {
        checkChildrenAllowed();
        externalReferences.addAll(set.getExternalReferences());
    }

    public void setScope(ComponentScope scope) {
        checkAttributesAllowed();
        this.scope = scope.getScope();
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
        dieOnCircularReference();
        return dependencies;
    }

    public void addComponent(Component c) {
        checkChildrenAllowed();
        nestedComponents.add(c);
        setChecked(false);
    }

    public List<Component> getNestedComponents() {
        if (isReference()) {
            return getRef().getNestedComponents();
        }
        dieOnCircularReference();
        List<Component> result = new ArrayList<>();
        result.addAll(nestedComponents);
        result.addAll(nestedComponents
                      .stream()
                      .flatMap(c -> c.getNestedComponents().stream())
                      .collect(Collectors.toList()));
        return result;
    }

    public void setUnknownDependencies(boolean unknownDependencies) {
        checkAttributesAllowed();
        this.unknownDependencies = unknownDependencies;
    }

    public Union createSbomLink() {
        checkChildrenAllowed();
        return sbomLink == null ? (sbomLink = new Union()) : sbomLink;
    }

    public boolean hasSbomLink() {
        if (isReference()) {
            return getRef().hasSbomLink();
        }
        dieOnCircularReference();
        return sbomLink != null;
    }

    public boolean areDependenciesUnknown() {
        if (isReference()) {
            return getRef().areDependenciesUnknown();
        }
        dieOnCircularReference();
        return unknownDependencies;
    }

    public Collection<Component> resolve() throws IOException {
        if (isReference()) {
            return getRef().resolve();
        }
        dieOnCircularReference();

        if (sbomLink != null && !sbomLinkResolved) {
            if (sbomLink.size() != 1) {
                throw new BuildException("sbomLink requires exactly one nested resource");
            }
            Resource sbom = sbomLink.iterator().next();
            try (InputStream data = sbom.getInputStream();
                 ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                byte[] buf = new byte[4096];
                int count = data.read(buf, 0, buf.length);
                while (count >= 0) {
                    baos.write(buf, 0, count);
                    count = data.read(buf, 0, buf.length);
                }
                byte[] content = baos.toByteArray();
                try {
                    Parser parser = BomParserFactory.createParser(content);
                    Bom bom = parser.parse(content);
                    if (bom.getMetadata() == null) {
                        throw new BuildException("referenced SBOM file lacks metadata");
                    }
                    org.cyclonedx.model.Component real = bom.getMetadata().getComponent();
                    if (real == null) {
                        throw new BuildException("referenced SBOM file lacks component");
                    }
                    fillFrom(real);

                    List<org.cyclonedx.model.Dependency> allDependencies = bom.getDependencies();
                    if (allDependencies != null) {
                        setUnknownDependencies(true);
                        org.cyclonedx.model.Dependency myDependencies = allDependencies
                            .stream()
                            .filter(d -> Objects.equals(d.getRef(), getBomRef()))
                            .findAny()
                            .orElse(null);
                        if (myDependencies != null && myDependencies.getDependencies() != null) {
                            setUnknownDependencies(false);
                            dependencies.clear();
                            dependencies
                                .addAll(myDependencies.getDependencies()
                                        .stream()
                                        .map(Dependency::from)
                                        .collect(Collectors.toList()));
                        }
                    }

                    List<org.cyclonedx.model.Component> additionalComponents = bom.getComponents();
                    if (additionalComponents != null && !areDependenciesUnknown()) {
                        List<Component> toReturn = new ArrayList<>();
                        for (org.cyclonedx.model.Component c : additionalComponents) {
                            Component dep = from(c);
                            if (dependencies.stream().anyMatch(d -> Objects.equals(dep.getBomRef(), d.getBomRef()))) {
                                // we don't want to resolve transitive dependencies automatically
                                dep.setUnknownDependencies(true);
                                toReturn.add(dep);
                            }
                        }
                        return toReturn;
                    }

                } catch (ParseException ex) {
                    throw new BuildException("failed to parse sbomlink " + sbom.getName());
                }
            }
            sbomLinkResolved = true;
        }

        return Collections.emptyList();
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

    public static Component from(org.cyclonedx.model.Component real) {
        Component c = new Component();
        c.fillFrom(real);
        return c;
    }

    private org.cyclonedx.model.Component toCycloneDxComponent(Version bomVersion)
        throws IOException {
        dieOnCircularReference();
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
        if (publisher != null) {
            component.setPublisher(publisher);
        }
        if (copyright != null) {
            component.setCopyright(copyright);
        }
        if (mimeType != null) {
            component.setMimeType(mimeType);
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
        if (!authors.isEmpty()) {
            component.setAuthors(authors);
        }
        if (!properties.isEmpty()) {
            component.setProperties(properties);
        }
        if (!tags.isEmpty()) {
            Tags ts = new Tags();
            ts.setTags(tags.stream().map(t -> t.getTag()).collect(Collectors.toList()));
            component.setTags(ts);
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
        for (Component c : nestedComponents) {
            component.addComponent(c.toAdditionalCycloneDxComponent(bomVersion));
        }
        // add isExternal once VERSION_17 is supported by cyclonedx-java-core
        addHashes(component, bomVersion);
        return component;
    }

    private void fillFrom(org.cyclonedx.model.Component real) {
        setType(ComponentType.from(real.getType()));
        setName(real.getName());
        setGroup(real.getGroup());
        setVersion(real.getVersion());
        setDescription(real.getDescription());
        setPublisher(real.getPublisher());
        setCopyright(real.getCopyright());
        setMimeType(real.getMimeType());
        setPurl(real.getPurl());
        setBomRef(real.getBomRef());
        if (real.getScope() != null) {
            setScope(ComponentScope.from(real.getScope()));
        }
        OrganizationalEntity manufacturer = real.getManufacturer();
        if (manufacturer != null) {
            this.manufacturer = Organization.from(manufacturer);
        }
        OrganizationalEntity supplier = real.getSupplier();
        if (supplier != null) {
            this.supplier = Organization.from(supplier);
        }
        LicenseChoice licenses = real.getLicenses();
        if (licenses != null) {
            this.licenses.clear();
            this.licenses.addAll(licenses.getLicenses());
        }
        if (real.getExternalReferences() != null) {
            this.externalReferences.clear();
            this.externalReferences.addAll(real.getExternalReferences());
        }
        if (real.getAuthors() != null) {
            authors.clear();
            authors.addAll(real.getAuthors());
        }
        if (real.getProperties() != null) {
            properties.clear();
            properties.addAll(real.getProperties());
        }
        if (real.getTags() != null && real.getTags().getTags() != null) {
            tags.clear();
            tags.addAll(real.getTags().getTags()
                        .stream()
                        .map(t -> {
                                Tag tag = new Tag();
                                tag.addText(t);
                                return tag;
                            })
                        .collect(Collectors.toList()));
        }
        if (real.getComponents() != null) {
            nestedComponents.clear();
            nestedComponents.addAll(real.getComponents()
                                    .stream()
                                    .map(Component::from)
                                    .collect(Collectors.toList()));
        }
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
            throw new BuildException("component resource " + resource + " doesn't provide a file");
        }

        component.setHashes(BomUtils.calculateHashes(file, bomVersion));
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

        public static Dependency from(org.cyclonedx.model.Dependency dependency) {
            Dependency d = new Dependency();
            d.setBomRef(dependency.getRef());
            return d;
        }
    }

    public static class Tag {
        private String tag;

        public void addText(String text) {
            tag = text;
        }

        public String getTag() {
            return tag;
        }
    }

    /**
     * Perform the check for circular references and return the
     * referenced Component.
     * @return <code>Component</code>.
     */
    protected Component getRef() {
        return getCheckedRef(Component.class);
    }

    @Override
    protected synchronized void dieOnCircularReference(Stack<Object> stk, Project p)
        throws BuildException {
        if (isChecked()) {
            return;
        }
        if (isReference()) {
            super.dieOnCircularReference(stk, p);
        } else {
            for (Component c : nestedComponents) {
                pushAndInvokeCircularReferenceCheck(c, stk, p);
            }
            setChecked(true);
        }
    }
}

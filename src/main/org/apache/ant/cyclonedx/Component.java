package org.apache.ant.cyclonedx;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.Stack;
import java.util.stream.Collectors;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.DataType;
import org.apache.tools.ant.types.Reference;
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

/**
 * The central concept of an SBOM which describes components and their dependencies.
 *
 * <p>The CycloneDX specification supports more information for a
 * component than this type currently exposes.</p>
 *
 * <p>This class is a type exposed by this Ant Library. When using the
 * inherited {@code refid} attribute it can reference an instance
 * defined previously - in which case no child elements or other
 * attributes are allowed.</p>
 */
public class Component extends DataType {
    private Resource resource;
    private org.cyclonedx.model.Component.Type type;
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
    private Set<String> tags = new HashSet<>();
    private List<Property> properties = new ArrayList<>();
    private String mimeType;
    private Union sbomLink;

    /**
     * Sets the resource the component is about.
     *
     * <p>At most one resource can be set. Without a nested resource
     * the component will not have any "hashes" when written to the
     * SBOM.</p>
     */
    public void add(Resource resource) {
        checkChildrenAllowed();
        if (this.resource != null) {
            throw new BuildException("component can only be defined for a single asset");
        }
        this.resource = resource;
    }

    /**
     * Sets the type of the component.
     *
     * <p>Defaults to "library".</p>
     */
    public void setType(ComponentType type) {
        checkAttributesAllowed();
        this.type = type.getType();
    }

    /**
     * Sets the name of the component.
     */
    public void setName(String name) {
        checkAttributesAllowed();
        this.name = name;
    }

    /**
     * Sets the group of the component.
     */
    public void setGroup(String group) {
        checkAttributesAllowed();
        this.group = group;
    }

    /**
     * Sets the version of the component.
     */
    public void setVersion(String version) {
        checkAttributesAllowed();
        this.version = version;
    }

    /**
     * Sets the Package-URL (purl) of the component.
     */
    public void setPurl(String purl) {
        checkAttributesAllowed();
        this.purl = purl;
    }

    /**
     * Sets the bom-ref of the component.
     */
    public void setBomRef(String bomRef) {
        checkAttributesAllowed();
        this.bomRef = bomRef;
    }

    /**
     * Sets the decription of the component.
     */
    public void setDescription(String description) {
        checkAttributesAllowed();
        this.description = description;
    }

    /**
     * Sets the publisher of the component.
     */
    public void setPublisher(String publisher) {
        checkAttributesAllowed();
        this.publisher = publisher;
    }

    /**
     * Sets the copyright of the component.
     */
    public void setCopyright(String copyright) {
        checkAttributesAllowed();
        this.copyright = copyright;
    }

    /**
     * Sets the mime-type of the component.
     */
    public void setMimeType(String mimeType) {
        checkAttributesAllowed();
        this.mimeType = mimeType;
    }

    /**
     * Sets the manufacturer of the component.
     *
     * <p>At most one manufacturer can be set.</p>
     */
    public void addManufacturer(Organization manufacturer) {
        checkChildrenAllowed();
        if (this.manufacturer != null) {
            throw new BuildException("component can only have one manufacturer");
        }
        this.manufacturer = manufacturer;
    }

    /**
     * Sets the supplier of the component.
     *
     * <p>At most one supplier can be set.</p>
     */
    public void addSupplier(Organization supplier) {
        checkChildrenAllowed();
        if (this.supplier != null) {
            throw new BuildException("component can only have one supplier");
        }
        this.supplier = supplier;
    }

    /**
     * Adds an author to the component.
     */
    public void addAuthor(OrganizationalContact author) {
        checkChildrenAllowed();
        authors.add(author);
    }

    /**
     * Adds a tag to the component.
     */
    public void addConfiguredTag(Tag tag) {
        checkChildrenAllowed();
        tags.add(tag.getTag());
    }

    /**
     * Adds a property to the component.
     */
    public void addProperty(Property property) {
        checkChildrenAllowed();
        properties.add(property);
    }

    /**
     * If set to {@code true} the manufacturer will also be used to
     * provide the supplier information.
     */
    public void setManufacturerIsSupplier(boolean manufacturerIsSupplier) {
        checkAttributesAllowed();
        this.manufacturerIsSupplier = manufacturerIsSupplier;
    }

    /**
     * Adds a license to this component.
     */
    public void addConfiguredLicense(License l) {
        checkChildrenAllowed();
        licenses.add(l.toCycloneDxLicense());
    }

    /**
     * Adds an external reference to the component.
     */
    public void addConfiguredExternalReference(ExternalReference ref) {
        checkChildrenAllowed();
        externalReferences.add(ref.toCycloneDxExternalReference());
    }

    /**
     * Adds a set of external references to the component.
     */
    public void addConfiguredExternalReferenceSet(ExternalReferenceSet set) {
        checkChildrenAllowed();
        externalReferences.addAll(set.getExternalReferences());
    }

    /**
     * Sets the scope of this component.
     *
     * <p>Must not be set for the main component of the SBOM.</p>
     */
    public void setScope(ComponentScope scope) {
        checkAttributesAllowed();
        this.scope = scope.getScope();
    }

    /**
     * Sets whether the component is external.
     *
     * <p>The CycloneDX Specification says: An external component is
     * one that is not part of an assembly, but is expected to be
     * provided by the environment, regardless of the component's
     * scope.</p>
     *
     * <p>Must not be set to {@code true} for the main component of
     * the SBOM.</p>
     *
     * <p>Right now this attribute has no effect until the CycloneDX
     * core library supports the specification version 1.7.</p>
     */
    public void setIsExternal(boolean isExternal) {
        checkAttributesAllowed();
        this.isExternal = isExternal;
    }

    /**
     * Adds a dependency to this component.
     */
    public void addDependency(Dependency d) {
        checkChildrenAllowed();
        dependencies.add(d);
    }

    /**
     * Sets whether the dependencies of this component are unknown.
     *
     * <p>This flag is needed to be able to tell dependencies with
     * unknown dependencies from components without any
     * dependencies.</p>
     */
    public void setUnknownDependencies(boolean unknownDependencies) {
        checkAttributesAllowed();
        this.unknownDependencies = unknownDependencies;
    }

    /**
     * Adds a nested component.
     */
    public void addComponent(Component c) {
        checkChildrenAllowed();
        nestedComponents.add(c);
        setChecked(false);
    }

    public Union createSbomLink() {
        checkChildrenAllowed();
        return sbomLink == null ? (sbomLink = new Union()) : sbomLink;
    }

    /**
     * Gets the name of the component.
     */
    public String getName() {
        if (isReference()) {
            return getRef().getName();
        }
        dieOnCircularReference();
        return name;
    }

    /**
     * Gets the group of the component.
     */
    public String getGroup() {
        if (isReference()) {
            return getRef().getGroup();
        }
        dieOnCircularReference();
        return group;
    }

    /**
     * Gets the Package-URL (purl) of the component.
     *
     * @return the value set with {@see #setPurl} or a Maven purl
     * derived from name, group and version if all three or set - or
     * {@code null otherwise}.
     */
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

    /**
     * Gets the bom-ref of the component.
     *
     * @return the value set with {@see #setBomRef} or the result of {@see #getPurl}.
     */
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

    /**
     * Gets the dependencies of the component.
     */
    public Iterable<Dependency> getDependencies() {
        if (isReference()) {
            return getRef().getDependencies();
        }
        dieOnCircularReference();
        return dependencies;
    }

    /**
     * @return the value set with {@link #setUnknownDependencies}
     * or {@code false}.
     */
    public boolean areDependenciesUnknown() {
        if (isReference()) {
            return getRef().areDependenciesUnknown();
        }
        dieOnCircularReference();
        return unknownDependencies;
    }

    /**
     * Recursively returns the nested components of this component.
     */
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

    public boolean hasSbomLink() {
        if (isReference()) {
            return getRef().hasSbomLink();
        }
        dieOnCircularReference();
        return sbomLink != null;
    }

    public Collection<Component> resolve() throws IOException {
        if (isReference()) {
            return getRef().resolve();
        }
        dieOnCircularReference();

        if (sbomLink != null && !sbomLinkResolved) {
            Bom bom = readLinkedSbom();
            if (bom.getMetadata() == null) {
                throw new BuildException("referenced SBOM file lacks metadata");
            }
            org.cyclonedx.model.Component real = bom.getMetadata().getComponent();
            if (real == null) {
                throw new BuildException("referenced SBOM file lacks component");
            }
            List<org.cyclonedx.model.Dependency> allDependencies = bom.getDependencies();
            fillFrom(real, allDependencies);

            List<org.cyclonedx.model.Component> additionalComponents = bom.getComponents();
            if (additionalComponents != null && !areDependenciesUnknown()) {
                List<Component> toReturn = new ArrayList<>();
                for (org.cyclonedx.model.Component c : additionalComponents) {
                    Component dep = from(c, Collections.emptyList());
                    if (dependencies.stream().anyMatch(d -> Objects.equals(dep.getBomRef(), d.getBomRef()))) {
                        // only include "additional components" this component depends on directly.
                        // we don't want to resolve transitive dependencies automatically
                        dep.setUnknownDependencies(true);
                        toReturn.add(dep);
                    }
                }
                return toReturn;
            }
            sbomLinkResolved = true;
        }

        return Collections.emptyList();
    }

    org.cyclonedx.model.Component toMainCycloneDxComponent(Version bomVersion)
        throws IOException {
        if (isReference()) {
            return getRef().toMainCycloneDxComponent(bomVersion);
        }
        if (isExternal) {
            throw new BuildException("isExternal can not be true for the main bom component");
        }
        if (scope != null) {
            throw new BuildException("scope must not be set for the main bom component");
        }
        return toCycloneDxComponent(bomVersion);
    }

    org.cyclonedx.model.Component toAdditionalCycloneDxComponent(Version bomVersion)
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

    private static Component from(org.cyclonedx.model.Component real,
                                  List<org.cyclonedx.model.Dependency> dependencies) {
        Component c = new Component();
        c.fillFrom(real, dependencies);
        return c;
    }

    private org.cyclonedx.model.Component toCycloneDxComponent(Version bomVersion)
        throws IOException {
        dieOnCircularReference();
        if (name == null) {
            throw new BuildException("component name is required");
        }
        if (!dependencies.isEmpty() && getBomRef() == null) {
            throw new BuildException("components without bomRef cannot have dependencies");
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

        if (type != null) {
            component.setType(type);
        } else {
            component.setType(org.cyclonedx.model.Component.Type.LIBRARY);
        }
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
            component.setTags(new Tags(tags.stream().sorted().collect(Collectors.toList())));
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

    private void fillFrom(org.cyclonedx.model.Component real,
                          List<org.cyclonedx.model.Dependency> allDependencies) {
        if (type == null) {
            setType(ComponentType.from(real.getType()));
        }
        if (getBomRef() == null) {
            setBomRef(real.getBomRef());
        }
        if (getPurl() == null) {
            setPurl(real.getPurl());
        }
        if (name == null) {
            setName(real.getName());
        }
        if (group == null) {
            setGroup(real.getGroup());
        }
        if (version == null) {
            setVersion(real.getVersion());
        }
        if (scope == null && real.getScope() != null) {
            setScope(ComponentScope.from(real.getScope()));
        }
        // copy isExternal once CycloneDX Core supports it
        if (description == null) {
            setDescription(real.getDescription());
        }
        if (publisher == null) {
            setPublisher(real.getPublisher());
        }
        if (copyright == null) {
            setCopyright(real.getCopyright());
        }
        if (mimeType == null) {
            setMimeType(real.getMimeType());
        }
        if (manufacturer == null) {
            OrganizationalEntity realManufacturer = real.getManufacturer();
            if (realManufacturer != null) {
                manufacturer = Organization.from(realManufacturer);
            }
        }
        if (supplier == null && !manufacturerIsSupplier) {
            OrganizationalEntity realSupplier = real.getSupplier();
            if (realSupplier != null) {
                supplier = Organization.from(realSupplier);
            }
        }
        if (licenses.isEmpty()) {
            LicenseChoice realLicenses = real.getLicenses();
            if (realLicenses != null) {
                licenses.addAll(realLicenses.getLicenses());
            }
        }
        if (externalReferences.isEmpty()) {
            List<org.cyclonedx.model.ExternalReference> realExternalReferences =
                real.getExternalReferences();
            if (realExternalReferences != null) {
                externalReferences.addAll(realExternalReferences);
            }
        }
        if (authors.isEmpty()) {
            List<OrganizationalContact> realAuthors = real.getAuthors();
            if (realAuthors != null) {
                authors.addAll(realAuthors);
            }
        }
        if (properties.isEmpty()) {
            List<Property> realProperties = real.getProperties();
            if (realProperties != null) {
                properties.addAll(realProperties);
            }
        }
        if (real.getTags() != null && real.getTags().getTags() != null) {
            tags.addAll(real.getTags().getTags());
        }
        if (nestedComponents.isEmpty()) {
            List<org.cyclonedx.model.Component> realComponents = real.getComponents();
            if (realComponents != null) {
                nestedComponents.addAll(realComponents.stream()
                                        .map(c -> Component.from(c, allDependencies))
                                        .collect(Collectors.toList()));
            }
        }
        if (dependencies.isEmpty() && allDependencies != null) {
            fillDependencies(allDependencies);
        }
    }

    private void fillDependencies(List<org.cyclonedx.model.Dependency> allDependencies) {
        setUnknownDependencies(true);
        org.cyclonedx.model.Dependency myDependencies = allDependencies
            .stream()
            .filter(d -> Objects.equals(d.getRef(), getBomRef()))
            .findAny()
            .orElse(null);
        if (myDependencies != null && myDependencies.getDependencies() != null) {
            setUnknownDependencies(false);
            dependencies
                .addAll(myDependencies.getDependencies()
                        .stream()
                        .map(Dependency::from)
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

    private Bom readLinkedSbom() throws IOException {
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
                return parser.parse(content);
            } catch (ParseException ex) {
                throw new BuildException("failed to parse sbomlink " + sbom.getName(), ex);
            }
        }
    }

    /**
     * Represents a dependency of a component.
     */
    public static class Dependency {
        private String bomRef;
        private Reference componentRef;

        /**
         * Identifies the dependency by its bom-ref.
         */
        public void setBomRef(String bomRef) {
            this.bomRef = bomRef;
        }

        /**
         * Identifies the dependency by its Ant {@code id} attribute.
         */
        public void setComponentRef(Reference componentRef) {
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

            Object component = componentRef.getReferencedObject();
            if (component instanceof Component) {
                String b = ((Component) component).getBomRef();
                if (b == null) {
                    throw new BuildException("component with id '" + componentRef.getRefId() + "' doesn't provide a bomRef");
                }
                return b;
            }
            throw new BuildException("componentRef '" + componentRef.getRefId() + "' doesn't refer to a component");
        }

        static Dependency from(org.cyclonedx.model.Dependency dependency) {
            Dependency d = new Dependency();
            d.setBomRef(dependency.getRef());
            return d;
        }
    }

    /**
     * Represents a tag.
     */
    public static class Tag {
        private String tag;

        /**
         * Sets the tag value.
         */
        public void addText(String text) {
            tag = text;
        }

        String getTag() {
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

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
import org.apache.tools.ant.types.resources.URLProvider;

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
    private boolean supplierIsManufacturer = false;
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
     *
     * @param resource the resource holding the component's content
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
     *
     * @param type component type
     */
    public void setType(ComponentType type) {
        checkAttributesAllowed();
        this.type = type.getType();
    }

    /**
     * Sets the name of the component.
     *
     * @param name component name
     */
    public void setName(String name) {
        checkAttributesAllowed();
        this.name = name;
    }

    /**
     * Sets the group of the component.
     *
     * @param group component group
     */
    public void setGroup(String group) {
        checkAttributesAllowed();
        this.group = group;
    }

    /**
     * Sets the version of the component.
     *
     * @param version component version
     */
    public void setVersion(String version) {
        checkAttributesAllowed();
        this.version = version;
    }

    /**
     * Sets the Package-URL (purl) of the component.
     *
     * @param purl component Package URL
     */
    public void setPurl(String purl) {
        checkAttributesAllowed();
        this.purl = purl;
    }

    /**
     * Sets the bom-ref of the component.
     *
     * @param bomRef component bom-ref
     */
    public void setBomRef(String bomRef) {
        checkAttributesAllowed();
        this.bomRef = bomRef;
    }

    /**
     * Sets the decription of the component.
     *
     * @param description component description
     */
    public void setDescription(String description) {
        checkAttributesAllowed();
        this.description = description;
    }

    /**
     * Sets the publisher of the component.
     *
     * @param publisher component publisher
     */
    public void setPublisher(String publisher) {
        checkAttributesAllowed();
        this.publisher = publisher;
    }

    /**
     * Sets the copyright of the component.
     *
     * @param copyright component copyright
     */
    public void setCopyright(String copyright) {
        checkAttributesAllowed();
        this.copyright = copyright;
    }

    /**
     * Sets the mime-type of the component.
     *
     * @param mimeType component mime-type
     */
    public void setMimeType(String mimeType) {
        checkAttributesAllowed();
        this.mimeType = mimeType;
    }

    /**
     * Sets the manufacturer of the component.
     *
     * <p>At most one manufacturer can be set.</p>
     *
     * @param manufacturer component manufacturer
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
     *
     * @param supplier compoment supplier
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
     *
     * @param author component author
     */
    public void addAuthor(OrganizationalContact author) {
        checkChildrenAllowed();
        authors.add(author);
    }

    /**
     * Adds a tag to the component.
     *
     * @param tag component tag
     */
    public void addConfiguredTag(Tag tag) {
        checkChildrenAllowed();
        tags.add(tag.getTag());
    }

    /**
     * Adds a property to the component.
     *
     * @param property component property
     */
    public void addConfiguredProperty(Property property) {
        checkChildrenAllowed();
        if (property.getName() == null) {
            throw new BuildException("properties must have a name");
        }
        properties.add(property);
    }

    /**
     * Adds a set of properties to the component.
     *
     * @param set set of properties of component
     * @since CycloneDX Antlib 0.2
     */
    public void addConfiguredPropertySet(PropertySet set) {
        checkChildrenAllowed();
        properties.addAll(set.getProperties());
    }

    /**
     * If set to {@code true} the supplier will also be used to
     * provide the manufacturer information.
     *
     * @param supplierIsManufacturer whether to use supplier as
     * manufacturer as well
     */
    public void setSupplierIsManufacturer(boolean supplierIsManufacturer) {
        checkAttributesAllowed();
        this.supplierIsManufacturer = supplierIsManufacturer;
    }

    /**
     * Adds a license to this component.
     *
     * @param l compoment license
     */
    public void addConfiguredLicense(License l) {
        checkChildrenAllowed();
        licenses.add(l.toCycloneDxLicense());
    }

    /**
     * Adds an external reference to the component.
     *
     * @param ref external reference of component
     */
    public void addConfiguredExternalReference(ExternalReference ref) {
        checkChildrenAllowed();
        externalReferences.add(ref.toCycloneDxExternalReference());
    }

    /**
     * Adds a set of external references to the component.
     *
     * @param set set of external references of component
     */
    public void addConfiguredExternalReferenceSet(ExternalReferenceSet set) {
        checkChildrenAllowed();
        externalReferences.addAll(set.getExternalReferences());
    }

    /**
     * Sets the scope of this component.
     *
     * <p>Must not be set for the main component of the SBOM.</p>
     *
     * @param scope component scope
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
     *
     * @param isExternal whether the component is external
     */
    public void setIsExternal(boolean isExternal) {
        checkAttributesAllowed();
        this.isExternal = isExternal;
    }

    /**
     * Adds a dependency to this component.
     *
     * @param d component dependency
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
     *
     * @param unknownDependencies whether dependencies are unknown
     */
    public void setUnknownDependencies(boolean unknownDependencies) {
        checkAttributesAllowed();
        this.unknownDependencies = unknownDependencies;
    }

    /**
     * Adds a nested component.
     *
     * @param c nested component
     */
    public void addComponent(Component c) {
        checkChildrenAllowed();
        nestedComponents.add(c);
        // the newly added component may cause a circular dependency
        setChecked(false);
    }

    /**
     * Container for SBOM link resource.
     *
     * @return container for SBOM link resource
     */
    public Union createSbomLink() {
        checkChildrenAllowed();
        return sbomLink == null ? (sbomLink = new Union()) : sbomLink;
    }

    /**
     * Gets the name of the component.
     *
     * @return component name
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
     *
     * @return component group
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
     *
     * @return component's dependencies
     */
    public Iterable<Dependency> getDependencies() {
        if (isReference()) {
            return getRef().getDependencies();
        }
        dieOnCircularReference();
        return dependencies;
    }

    /**
     * Whether dependencies are unknoown.
     *
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
     *
     * @return nested components of this component
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

    /**
     * Read the linked SBOM (if any) and merge its content with the
     * one already defined for this component.
     *
     * @return the "addtional" components defined in the linked SBOM
     * that are dependencies of this component.
     * @throws IOException if reading the SBOM links fails
     */
    public synchronized Collection<Component> resolve() throws IOException {
        if (isReference()) {
            return getRef().resolve();
        }
        dieOnCircularReference();

        if (sbomLink != null && !sbomLinkResolved) {
            sbomLinkResolved = true;

            Bom bom = readLinkedSbom();
            if (bom.getMetadata() == null) {
                throw new BuildException("referenced SBOM file lacks metadata");
            }
            org.cyclonedx.model.Component real = bom.getMetadata().getComponent();
            if (real == null) {
                throw new BuildException("referenced SBOM file lacks component");
            }
            List<org.cyclonedx.model.Dependency> allDependencies = bom.getDependencies();
            fillFromBomLink(real, allDependencies);

            if (!areDependenciesUnknown() && !dependencies.isEmpty()) {
                List<org.cyclonedx.model.Component> additionalComponents = bom.getComponents();
                if (additionalComponents != null) {
                    return extractComponentsThatAreDirectDependencies(additionalComponents);
                }
            }
        }

        return Collections.emptyList();
    }

    /**
     * Creates a new "file" type component for a resource.
     *
     * @param project project to use when resolving paths
     * @param r the resource holding the file
     * @return the component
     */
    public static Component createFileComponent(Project project, Resource r) {
        Component c = new Component();
        c.setProject(project);
        c.add(r);
        c.setName(r.getName());
        c.setType(ComponentType.from(org.cyclonedx.model.Component.Type.FILE));
        return c;
    }

    /**
     * Translates this component to a CycloneDX component suitable for the metadata.component.
     *
     * @param bomVersion specification version
     * @return mapped component
     * @throws IOException if calculating component hashes fails
     */
    public org.cyclonedx.model.Component toMainCycloneDxComponent(Version bomVersion)
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

    /**
     * Translates this component to a CycloneDX component suitable for the components.component.
     *
     * @param bomVersion specification version
     * @return mapped component
     * @throws IOException if calculating component hashes fails
     */
    public org.cyclonedx.model.Component toAdditionalCycloneDxComponent(Version bomVersion)
        throws IOException {
        if (isReference()) {
            return getRef().toAdditionalCycloneDxComponent(bomVersion);
        }
        org.cyclonedx.model.Component component = toCycloneDxComponent(bomVersion);
        if (scope != null) {
            component.setScope(scope);
        }
        // add isExternal once VERSION_17 is supported by cyclonedx-java-core
        return component;
    }

    /**
     * Maps all common data except for <code>scope</code> and <code>isExternal</code>.
     *
     * @param bomVersion specification version
     * @return mapped component
     * @throws IOException if calculating component hashes fails
     */
    protected org.cyclonedx.model.Component toCycloneDxComponent(Version bomVersion)
        throws IOException {
        dieOnCircularReference();
        if (name == null) {
            throw new BuildException("component name is required");
        }
        if (!dependencies.isEmpty() && getBomRef() == null) {
            throw new BuildException("components without bomRef cannot have dependencies");
        }
        if (supplierIsManufacturer) {
            if (supplier == null) {
                throw new BuildException("component without supplier can't use supplier as manufacturer");
            }
            if (manufacturer != null) {
                throw new BuildException("component with manufacturer can't use supplier as manufacturer");
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
        if (supplier != null) {
            OrganizationalEntity oe = supplier.toOrganizationalEntity();
            component.setSupplier(oe);
            if (supplierIsManufacturer) {
                component.setManufacturer(oe);
            }
        }
        if (manufacturer != null) {
            component.setManufacturer(manufacturer.toOrganizationalEntity());
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
        component.setAuthors(authors);
        component.setProperties(properties);
        component.setTags(new Tags(tags.stream().sorted().collect(Collectors.toList())));
        component.setExternalReferences(externalReferences);
        if (!licenses.isEmpty()) {
            // would create an empty licenses node otherwise
            LicenseChoice lc = new LicenseChoice();
            lc.setLicenses(licenses);
            component.setLicenses(lc);
        }
        for (Component c : nestedComponents) {
            component.addComponent(c.toAdditionalCycloneDxComponent(bomVersion));
        }
        addHashes(component, bomVersion);
        return component;
    }

    private List<Component> extractComponentsThatAreDirectDependencies(List<org.cyclonedx.model.Component> cs) {
        List<Component> toReturn = new ArrayList<>();
        for (org.cyclonedx.model.Component c : cs) {
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

    private static Component from(
        org.cyclonedx.model.Component real,
        List<org.cyclonedx.model.Dependency> dependencies) {
        Component c = new Component();
        c.fillFromBomLink(real, dependencies);
        return c;
    }

    private void fillFromBomLink(
        org.cyclonedx.model.Component real,
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
        if (supplier == null) {
            OrganizationalEntity realSupplier = real.getSupplier();
            if (realSupplier != null) {
                supplier = Organization.from(realSupplier);
            }
        }
        if (manufacturer == null && !supplierIsManufacturer) {
            OrganizationalEntity realManufacturer = real.getManufacturer();
            if (realManufacturer != null) {
                manufacturer = Organization.from(realManufacturer);
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
            fillDependenciesFromBomLink(allDependencies);
        }
    }

    private void fillDependenciesFromBomLink(List<org.cyclonedx.model.Dependency> allDependencies) {
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

    /**
     * If this component has a nested resource child, all hashes
     * supported by the CycloneDX Core library for the spec version are
     * created and added to the given component.
     *
     * @param component component to add hashes to
     * @param bomVersion specification version
     * @throws IOException if calculating component hashes fails
     */
    protected void addHashes(org.cyclonedx.model.Component component, Version bomVersion)
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
        logSbom(sbom);
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

    private void logSbom(Resource r) {
        String name = r.getName();
        FileProvider fp = r.as(FileProvider.class);
        if (fp != null) {
            name = fp.getFile().getAbsolutePath();
        } else {
            URLProvider up = r.as(URLProvider.class);
            if (up != null) {
                name = up.getURL().toExternalForm();
            }
        }
        log("reading SBOM from " + name, Project.MSG_VERBOSE);
    }

    /**
     * Represents a dependency of a component.
     */
    public static class Dependency {
        private String bomRef;
        private Reference componentRef;

        /**
         * Identifies the dependency by its bom-ref.
         *
         * @param bomRef dependency's bom-ref
         */
        public void setBomRef(String bomRef) {
            this.bomRef = bomRef;
        }

        /**
         * Identifies the dependency by its Ant {@code id} attribute.
         *
         * @param componentRef reference to component that constitutes
         * the dependency
         */
        public void setComponentRef(Reference componentRef) {
            this.componentRef = componentRef;
        }

        /**
         * Looks up the bom-ref of the dependency.
         * @return bom-ref of the dependency.
         */
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

            String refid = componentRef.getRefId();
            Object component = componentRef.getReferencedObject();
            if (component instanceof Component) {
                String b = ((Component) component).getBomRef();
                if (b == null) {
                    throw new BuildException("component with id '" + refid + "' doesn't provide a bomRef");
                }
                return b;
            }
            throw new BuildException("componentRef '" + refid + "' doesn't refer to a component");
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
         *
         * @param text the tag
         */
        public void addText(String text) {
            tag = text;
        }

        /**
         * Obtains the tag.
         *
         * @return tag
         */
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

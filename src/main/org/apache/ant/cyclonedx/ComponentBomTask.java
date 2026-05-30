package org.apache.ant.cyclonedx;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.Resource;
import org.apache.tools.ant.types.ResourceCollection;
import org.apache.tools.ant.types.resources.Union;

import org.cyclonedx.Format;
import org.cyclonedx.exception.GeneratorException;
import org.cyclonedx.generators.BomGeneratorFactory;
import org.cyclonedx.generators.json.BomJsonGenerator;
import org.cyclonedx.generators.xml.BomXmlGenerator;
import org.cyclonedx.model.Bom;
import org.cyclonedx.model.Dependency;
import org.cyclonedx.model.LicenseChoice;
import org.cyclonedx.model.LifecycleChoice;
import org.cyclonedx.model.Lifecycles;
import org.cyclonedx.model.Metadata;
import org.cyclonedx.model.OrganizationalEntity;
import org.cyclonedx.model.metadata.ToolInformation;

/**
 * Task that creates CycloneDX BOMs for a single component.
 */
public class ComponentBomTask extends Task {

    private File outputDirectory;
    private String bomName = "bom";
    private SpecVersion specVersion = SpecVersion.DEFAULT;
    private OutputFormat format = OutputFormat.JSON;
    private Component component;
    private List<Component> toolComponents = new ArrayList<>();
    private List<Component> additionalComponents = new ArrayList<>();
    private Organization manufacturer = null;
    private Organization supplier = null;
    private boolean useComponentSupplier = false;
    private boolean useComponentManufacturer = false;
    private Union pureFileComponents = new Union();
    private List<org.cyclonedx.model.License> licenses = new ArrayList<>();

    /**
     * Specifies the CycloneDX version to use.
     *
     * <p>Defaults to 1.6.</p>
     *
     * @param specVersion specification version
     */
    public void setSpecVersion(SpecVersion specVersion) {
        this.specVersion = specVersion;
    }

    /**
     * Which serialization format of CycloneDX SBOM to use.
     *
     * @param format output format
     */
    public void setFormat(OutputFormat format) {
        this.format = format;
    }

    /**
     * Sets the base name of the generated BOM.
     *
     * <p>The file name will be the base name plus the extension of
     * the {@see #setFormat format}.
     *
     * @param bomName base name of generated file
     */
    public void setBomName(String bomName) {
        this.bomName = bomName;
    }

    /**
     * Sets the output directory for the generated SBOM.
     *
     * @param f output directory
     */
    public void setOutputDirectory(File f) {
        outputDirectory = f;
    }

    /**
     * Whether to use the supplier of the main component as supplier for the BOM as well.
     *
     * @param useComponentSupplier whether to use supplier of component for the BOM
     */
    public void setUseComponentSupplier(boolean useComponentSupplier) {
        this.useComponentSupplier = useComponentSupplier;
    }

    /**
     * Whether to use the manufacturer of the main component as manufacturer for the BOM as well.
     *
     * @param useComponentManufacturer whether to use manufacturer of component for the BOM
     */
    public void setUseComponentManufacturer(boolean useComponentManufacturer) {
        this.useComponentManufacturer = useComponentManufacturer;
    }

    /**
     * Sets the component for the SBOM.
     *
     * @return container for main component
     */
    public Component createComponent() {
        if (component != null) {
            throw new BuildException("only one nested component element is permitted");
        }
        component = new Component();
        return component;
    }

    /**
     * Sets the manufacturer of the SBOM.
     *
     * <p>At most one manufacturer can be set.</p>
     *
     * @return manufaturer of SBOM
     */
    public Organization createManufacturer() {
        if (manufacturer != null) {
            throw new BuildException("can only have one manufacturer");
        }
        manufacturer = new Organization();
        return manufacturer;
    }

    /**
     * Sets the supplier of the SBOM.
     *
     * <p>At most one supplier can be set.</p>
     *
     * @return supplier of SBOM
     */
    public Organization createSupplier() {
        if (supplier != null) {
            throw new BuildException("can only have one supplier");
        }
        supplier = new Organization();
        return supplier;
    }

    /**
     * Adds a license to the SBOM's metadata.
     *
     * @param l SBOM's license
     */
    public void addConfiguredLicense(License l) {
        licenses.add(l.toCycloneDxLicense());
    }

    /**
     * Adds another component to the SBOM.
     *
     * @param c component to be added to SBOM
     */
    public void addAdditionalComponent(Component c) {
        additionalComponents.add(c);
    }

    /**
     * Adds component to be added to the metadata.tools section of the
     * SBOM.
     *
     * <p>This is meant to be used by tools that have also taken part
     * in the generation of this SBOM.</p>
     *
     * @param c component to be added to tools
     */
    public void addToolComponent(Component c) {
        toolComponents.add(c);
    }

    /**
     * Accepts arbitrary file-system only resources that will be added
     * as components of type file.
     *
     * @return container for pure file components
     */
    public Union createPureFileComponents() {
        return pureFileComponents;
    }

    public void execute() {
        if (supplier != null && useComponentSupplier) {
            throw new BuildException("can't use component's supplier when there is an explicit supplier");
        }
        if (manufacturer != null && useComponentManufacturer) {
            throw new BuildException("can't use component's manufacturer when there is an explicit manufacturer");
        }
        if (outputDirectory != null && !outputDirectory.isDirectory()) {
            throw new BuildException("outputDirectory must point to a directory");
        }
        if (pureFileComponents.size() > 0 && !pureFileComponents.isFilesystemOnly()) {
            throw new BuildException("only file system resources are supported for pureFileComponents");
        }
        if (component == null) {
            throw new BuildException("nested component element is required");
        }

        try {
            File dir = outputDirectory != null ? outputDirectory : getProject().getBaseDir();
            Bom bom = createBom();
            for (Format f : format.getCycloneDxFormats(specVersion.getVersion())) {
                writeBom(bom, f, new File(dir, bomName + "." + f.getExtension()));
            }
        } catch (IOException | GeneratorException ex) {
            throw new BuildException("failed to write BOM", ex);
        }
    }

    private Bom createBom() throws IOException {
        Bom bom = new Bom();
        bom.setSerialNumber("urn:uuid:" + UUID.randomUUID());

        Metadata meta = createMetadata();

        Map<String, String> knownComponents = new HashMap<>();
        List<Component> resolvedComponents = new ArrayList<>();
        visitAllComponents(c -> {
                try {
                    resolvedComponents.addAll(c.resolve());
                } catch (IOException ex) {
                    throw new BuildException("failed to resolve component", ex);
                }
                String unversionedKey = getUnversionedCoordinates(c);
                if (unversionedKey != null) {
                    knownComponents.put(unversionedKey, c.getBomRef());
                }
            });
        meta.setComponent(component.toMainCycloneDxComponent(specVersion.getVersion()));

        if (useComponentSupplier) {
            OrganizationalEntity componentSupplier = meta.getComponent().getSupplier();
            if (componentSupplier == null) {
                throw new BuildException("useComponentSupplier is true but component supplier is null");
            }
            meta.setSupplier(componentSupplier);
        }

        if (useComponentManufacturer) {
            OrganizationalEntity componentManufacturer = meta.getComponent().getManufacturer();
            if (componentManufacturer == null) {
                throw new BuildException("useComponentManufacturer is true but component manufacturer is null");
            }
            meta.setManufacturer(componentManufacturer);
        }

        bom.setMetadata(meta);

        List<org.cyclonedx.model.Component> cs = new ArrayList<>();
        for (Component c : additionalComponents) {
            cs.add(c.toAdditionalCycloneDxComponent(specVersion.getVersion()));
        }

        for (Component c : resolvedComponents) {
            String unversionedKey = getUnversionedCoordinates(c);
            if (unversionedKey == null) {
                cs.add(c.toAdditionalCycloneDxComponent(specVersion.getVersion()));
            } else if (!knownComponents.containsKey(unversionedKey)) {
                knownComponents.put(unversionedKey, c.getBomRef());
                cs.add(c.toAdditionalCycloneDxComponent(specVersion.getVersion()));
            }
        }

        for (Resource r : pureFileComponents) {
            Component c = Component.createFileComponent(getProject(), r);
            cs.add(c.toAdditionalCycloneDxComponent(specVersion.getVersion()));
        }

        bom.setComponents(cs);
        addDependencies(bom, knownComponents);

        return bom;
    }

    private Metadata createMetadata() throws IOException {
        Metadata meta = new Metadata();
        meta.setTimestamp(new Date());
        ToolInformation antlibToolInformation = ToolData.getToolInformation(specVersion.getVersion());
        if (!toolComponents.isEmpty()) {
            List<org.cyclonedx.model.Component> tools =
                new ArrayList(antlibToolInformation.getComponents());
            for (Component c : toolComponents) {
                tools.add(c.toAdditionalCycloneDxComponent(specVersion.getVersion()));
            }
            ToolInformation ti = new ToolInformation();
            ti.setComponents(tools);
            ti.setServices(antlibToolInformation.getServices());
            meta.setToolChoice(ti);
        } else {
            meta.setToolChoice(antlibToolInformation);
        }
        if (!licenses.isEmpty()) {
            LicenseChoice lc = new LicenseChoice();
            lc.setLicenses(licenses);
            meta.setLicenses(lc);
        }

        Lifecycles l = new Lifecycles();
        LifecycleChoice lc = new LifecycleChoice();
        lc.setPhase(LifecycleChoice.Phase.BUILD);
        l.setLifecycleChoice(Collections.singletonList(lc));
        meta.setLifecycles(l);

        if (supplier != null) {
            meta.setSupplier(supplier.toOrganizationalEntity());
        }
        if (manufacturer != null) {
            meta.setManufacturer(manufacturer.toOrganizationalEntity());
        }

        return meta;
    }

    private void addDependencies(Bom bom, Map<String, String> unversionedToVersioned) {
        final Set<String> bomRefs = new HashSet<>();
        visitAllBomComponents(bom, c -> {
                String bomRef = c.getBomRef();
                if (bomRef != null) {
                    if (!bomRefs.add(bomRef)) {
                        throw new BuildException("BOM contains multiple components with bom-ref " + bomRef);
                    }
                }
            });

        final List<Dependency> dependencies = new ArrayList<>();
        visitAllComponents(c -> {
                String bomRef = c.getBomRef();
                if (bomRef != null && !c.areDependenciesUnknown()) {
                    Dependency dep = new Dependency(bomRef);
                    for (Component.Dependency d : c.getDependencies()) {
                        String br = d.getBomRef();
                        if (!bomRefs.contains(br)) {
                            String mappedRef = null;
                            String unversionedKey = getUnversionedCoordinates(d);
                            if (unversionedKey != null) {
                                mappedRef = unversionedToVersioned.get(unversionedKey);
                            }
                            if (mappedRef == null) {
                                throw new BuildException("dependency '" + br + "' is unknown");
                            }
                            br = mappedRef;
                        }
                        dep.addDependency(new Dependency(br));
                    }
                    dependencies.add(dep);
                }
            });

        bom.setDependencies(dependencies);
    }

    private void visitAllComponents(Consumer<Component> visitor) {
        visitAllComponents(component, visitor);
        visitAllComponents(additionalComponents, visitor);
    }

    private void visitAllComponents(Component c,
                                    Consumer<Component> visitor) {
        visitor.accept(c);
        List<Component> cs = c.getNestedComponents();
        if (cs != null) {
            // getNestedComponents() has already traversed the whole hierarchy recursively
            cs.forEach(visitor);
        }
    }

    private void visitAllComponents(List<Component> cs,
                                    Consumer<Component> visitor) {
        if (cs != null) {
            cs.forEach(c -> visitAllComponents(c, visitor));
        }
    }

    private void visitAllBomComponents(Bom bom, Consumer<org.cyclonedx.model.Component> visitor) {
        visitAllBomComponents(bom.getMetadata().getComponent(), visitor);
        visitAllBomComponents(bom.getComponents(), visitor);
    }

    private void visitAllBomComponents(org.cyclonedx.model.Component c,
                                       Consumer<org.cyclonedx.model.Component> visitor) {
        visitor.accept(c);
        visitAllBomComponents(c.getComponents(), visitor);
    }

    private void visitAllBomComponents(List<org.cyclonedx.model.Component> cs,
                                       Consumer<org.cyclonedx.model.Component> visitor) {
        if (cs != null) {
            cs.forEach(c -> visitAllBomComponents(c, visitor));
        }
    }

    private void writeBom(Bom bom, Format format, File bomFile)
        throws IOException, GeneratorException {
        log("creating CycloneDX SBOM " + bomFile);
        switch (format) {
        case JSON:
            writeJsonBom(bom, bomFile);
            break;
        case XML:
            writeXmlBom(bom, bomFile);
            break;
        default:
            throw new BuildException("unsupported format " + format);
        }
    }

    private void writeJsonBom(Bom bom, File bomFile) throws IOException, GeneratorException {
        BomJsonGenerator generator = BomGeneratorFactory.createJson(specVersion.getVersion(), bom);
        try (FileOutputStream fos = new FileOutputStream(bomFile);
             OutputStreamWriter writer = new OutputStreamWriter(fos, StandardCharsets.UTF_8)) {
            writer.write(generator.toJsonString(true));
        }
    }

    private void writeXmlBom(Bom bom, File bomFile) throws IOException, GeneratorException {
        BomXmlGenerator generator = BomGeneratorFactory.createXml(specVersion.getVersion(), bom);
        try (FileOutputStream fos = new FileOutputStream(bomFile);
             OutputStreamWriter writer = new OutputStreamWriter(fos, StandardCharsets.UTF_8)) {
            writer.write(generator.toXmlString());
        }
    }

    private static String getUnversionedCoordinates(Component c) {
        Map.Entry<String, String> mavenCoordinates = extractMavenCoordinates(c.getBomRef());
        if (mavenCoordinates == null) {
            return null;
        }
        return mavenCoordinates.getKey() + ":" + mavenCoordinates.getValue();
    }

    private static String getUnversionedCoordinates(Component.Dependency d) {
        Map.Entry<String, String> mavenCoordinates = extractMavenCoordinates(d.getBomRef());
        if (mavenCoordinates == null) {
            return null;
        }
        return mavenCoordinates.getKey() + ":" + mavenCoordinates.getValue();
    }

    private static Pattern MAVEN_PURL_PATTERN = Pattern.compile("pkg:maven/([^/]+)/([^/]+)@.+\\?type=jar");

    private static Map.Entry<String, String> extractMavenCoordinates(String bomRef) {
        if (bomRef == null) {
            return null;
        }
        Matcher m = MAVEN_PURL_PATTERN.matcher(bomRef);
        if (m.matches()) {
            return new AbstractMap.SimpleImmutableEntry(m.group(1), m.group(2));
        }
        return null;
    }
}

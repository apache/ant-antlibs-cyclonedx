package org.apache.ant.cyclonedx;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

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

/**
 * Task that creates CycloneDX BOM for a single component.
 */
public class ComponentBomTask extends Task {

    private File outputDirectory;
    private String bomName = "bom";
    private SpecVersion specVersion = SpecVersion.DEFAULT;
    private OutputFormat format = OutputFormat.JSON;
    private Component component;
    private List<Component> additionalComponents = new ArrayList<>();
    private Organization manufacturer = null;
    private Organization supplier = null;
    private boolean useComponentSupplier = false;
    private Union pureFileComponents = new Union();
    private List<org.cyclonedx.model.License> licenses = new ArrayList<>();

    public void setOutputDirectory(File f) {
        outputDirectory = f;
    }

    public void setBomName(String bomName) {
        this.bomName = bomName;
    }

    public void setFormat(OutputFormat format) {
        this.format = format;
    }

    public void setSpecVersion(SpecVersion specVersion) {
        this.specVersion = specVersion;
    }

    public Component createComponent() {
        if (component != null) {
            throw new BuildException("only one nested component element is permitted");
        }
        component = new Component();
        return component;
    }

    public void addAdditionalComponent(Component c) {
        additionalComponents.add(c);
    }

    public Organization createManufacturer() {
        if (manufacturer != null) {
            throw new BuildException("can only have one manufacturer");
        }
        manufacturer = new Organization();
        return manufacturer;
    }

    public Organization createSupplier() {
        if (supplier != null) {
            throw new BuildException("can only have one supplier");
        }
        supplier = new Organization();
        return supplier;
    }

    public void setUseComponentSupplier(boolean useComponentSupplier) {
        this.useComponentSupplier = useComponentSupplier;
    }

    public Union createPureFileComponents() {
        return pureFileComponents;
    }

    /**
     * Adds a license to the SBOM's metadata.
     */
    public void addConfiguredLicense(License l) {
        licenses.add(l.toCycloneDxLicense());
    }

    public void execute() {
        if (supplier != null && useComponentSupplier) {
            throw new BuildException("can't use component's supplier when there is an explicit supplier");
        }
        if (outputDirectory == null || !outputDirectory.isDirectory()) {
            throw new BuildException("outputDirectory must point to a directory");
        }
        if (pureFileComponents.size() > 0 && !pureFileComponents.isFilesystemOnly()) {
            throw new BuildException("only file system resources are supported for pureFileComponents");
        }

        try {
            Bom bom = createBom();
            for (Format f : format.getCycloneDxFormats(specVersion.getVersion())) {
                writeBom(bom, f,
                         new File(outputDirectory,
                                  bomName + "." + f.name().toLowerCase(Locale.ENGLISH)));
            }
        } catch (IOException | GeneratorException ex) {
            throw new BuildException("failed to write BOM", ex);
        }
    }

    private Bom createBom() throws IOException {
        Bom bom = new Bom();
        bom.setSerialNumber("urn:uuid:" + UUID.randomUUID());

        Metadata meta = new Metadata();
        meta.setTimestamp(new Date());
        meta.setToolChoice(ToolData.getToolInformation(specVersion.getVersion()));
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

        if (component == null) {
            throw new BuildException("nested component element is required");
        }
        Set<String> knownComponents = new HashSet<>();
        List<Component> resolvedComponents = new ArrayList<>();
        visitAllComponents(c -> {
                try {
                    resolvedComponents.addAll(c.resolve());
                } catch (IOException ex) {
                    throw new BuildException("failed to resolve component", ex);
                }
                knownComponents.add(getUnversionedCoordinates(c));
            });
        meta.setComponent(component.toMainCycloneDxComponent(specVersion.getVersion()));
        if (useComponentSupplier) {
            OrganizationalEntity componentSupplier = meta.getComponent().getSupplier();
            if (componentSupplier == null) {
                throw new BuildException("useComponentSupplier is true but component supplier is null");
            }
            meta.setSupplier(componentSupplier);
        }
        if (supplier != null) {
            meta.setSupplier(supplier.toOrganizationalEntity());
        }
        if (manufacturer != null) {
            meta.setManufacturer(manufacturer.toOrganizationalEntity());
        }

        bom.setMetadata(meta);

        List<org.cyclonedx.model.Component> cs = new ArrayList<>();
        if (!additionalComponents.isEmpty()) {
            for (Component c : additionalComponents) {
                cs.add(c.toAdditionalCycloneDxComponent(specVersion.getVersion()));
            }
        }

        if (!resolvedComponents.isEmpty()) {
            for (Component c : resolvedComponents) {
                String componentKey = getUnversionedCoordinates(c);
                if (!knownComponents.contains(componentKey)) {
                    knownComponents.add(componentKey);
                    cs.add(c.toAdditionalCycloneDxComponent(specVersion.getVersion()));
                }
            }
        }

        if (pureFileComponents.size() > 0) {
            for (Resource r : pureFileComponents) {
                Component c = new Component();
                c.setProject(getProject());
                c.add(r);
                c.setName(r.getName());
                c.setType(ComponentType.from(org.cyclonedx.model.Component.Type.FILE));
                cs.add(c.toAdditionalCycloneDxComponent(specVersion.getVersion()));
            }
        }

        bom.setComponents(cs);
        addDependencies(bom);

        return bom;
    }

    private void addDependencies(Bom bom) {
        final Set<String> bomRefs = new HashSet<>();
        visitAllBomComponents(bom, c -> {
                String bomRef = c.getBomRef();
                if (bomRef != null) {
                    bomRefs.add(bomRef);
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
                            throw new BuildException("dependency '" + br + "' is unknown");
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
        switch (format) {
        case JSON:
            writeJsonBom(bom, bomFile);
            break;
        case XML:
            writeXmlBom(bom, bomFile);
            break;
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
        return c.getGroup() + ":" + c.getName();
    }
}

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
import java.util.Set;
import java.util.UUID;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

import org.cyclonedx.Format;
import org.cyclonedx.Version;
import org.cyclonedx.exception.GeneratorException;
import org.cyclonedx.generators.BomGeneratorFactory;
import org.cyclonedx.generators.json.BomJsonGenerator;
import org.cyclonedx.generators.xml.BomXmlGenerator;
import org.cyclonedx.model.Bom;
import org.cyclonedx.model.Dependency;
import org.cyclonedx.model.LifecycleChoice;
import org.cyclonedx.model.Lifecycles;
import org.cyclonedx.model.Metadata;
import org.cyclonedx.model.OrganizationalEntity;

/**
 * Task that creates CycloneDX BOM for a single component.
 */
public class ComponentBomTask extends Task {

    private File bomFile;
    private Format format = Format.JSON;
    private Component component;
    private List<Component> additionalComponents = new ArrayList<>();
    private Organization manufacturer = null;
    private Organization supplier = null;
    private boolean useComponentSupplier = false;

    public void setBomFile(File f) {
        bomFile = f;
    }

    public void setFormat(Format format) {
        this.format = format;
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

    public void execute() {
        if (supplier != null && useComponentSupplier) {
            throw new BuildException("can't use component's supplier when there is an explicit supplier");
        }

        try {
            Bom bom = createBom();
            writeBom(bom, bomFile);
        } catch (IOException | GeneratorException ex) {
            throw new BuildException("failed to write BOM", ex);
        }
    }

    private Bom createBom() throws IOException {
        Bom bom = new Bom();
        bom.setSerialNumber("urn:uuid:" + UUID.randomUUID());

        Metadata meta = new Metadata();
        meta.setTimestamp(new Date());
        meta.setToolChoice(ToolData.getToolInformation());

        Lifecycles l = new Lifecycles();
        LifecycleChoice lc = new LifecycleChoice();
        lc.setPhase(LifecycleChoice.Phase.BUILD);
        l.setLifecycleChoice(Collections.singletonList(lc));
        meta.setLifecycles(l);

        if (component == null) {
            throw new BuildException("nested component element is required");
        }
        meta.setComponent(component.toMainCycloneDxComponent(Version.VERSION_16));
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

        if (!additionalComponents.isEmpty()) {
            List<org.cyclonedx.model.Component> cs = new ArrayList<>();
            for (Component c : additionalComponents) {
                cs.add(c.toAdditionalCycloneDxComponent(Version.VERSION_16));
            }
            bom.setComponents(cs);
        }

        addDependencies(bom);

        return bom;
    }

    private void addDependencies(Bom bom) {
        List<Dependency> dependencies = new ArrayList<>();
        Set<String> bomRefs = new HashSet<>();
        if (component.getBomRef() != null) {
            bomRefs.add(component.getBomRef());
        }
        for (Component c : additionalComponents) {
            if (c.getBomRef() != null) {
                bomRefs.add(c.getBomRef());
            }
        }

        if (component.getBomRef() != null) {
            Dependency dep = new Dependency(component.getBomRef());
            for (Component.Dependency d : component.getDependencies()) {
                String br = d.getBomRef();
                if (!bomRefs.contains(br)) {
                    throw new BuildException("dependency '" + br + "' is unknown");
                }
                dep.addDependency(new Dependency(br));
            }
            dependencies.add(dep);
        }
        for (Component c : additionalComponents) {
            if (!c.areDependenciesUnknown() && c.getBomRef() != null) {
                Dependency dep = new Dependency(c.getBomRef());
                for (Component.Dependency d : c.getDependencies()) {
                    String br = d.getBomRef();
                    if (!bomRefs.contains(br)) {
                        throw new BuildException("dependency '" + br + "' is unknown");
                    }
                    dep.addDependency(new Dependency(br));
                }
                dependencies.add(dep);
            }
        }

        bom.setDependencies(dependencies);
    }

    private void writeBom(Bom bom, File bomFile) throws IOException, GeneratorException {
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
        BomJsonGenerator generator = BomGeneratorFactory.createJson(Version.VERSION_16, bom);
        try (FileOutputStream fos = new FileOutputStream(bomFile);
             OutputStreamWriter writer = new OutputStreamWriter(fos, StandardCharsets.UTF_8)) {
            writer.write(generator.toJsonString(true));
        }
    }

    private void writeXmlBom(Bom bom, File bomFile) throws IOException, GeneratorException {
        BomXmlGenerator generator = BomGeneratorFactory.createXml(Version.VERSION_16, bom);
        try (FileOutputStream fos = new FileOutputStream(bomFile);
             OutputStreamWriter writer = new OutputStreamWriter(fos, StandardCharsets.UTF_8)) {
            writer.write(generator.toXmlString());
        }
    }
}

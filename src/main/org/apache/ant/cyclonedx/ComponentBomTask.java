package org.apache.ant.cyclonedx;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Date;
import java.util.UUID;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

import org.cyclonedx.Version;
import org.cyclonedx.exception.GeneratorException;
import org.cyclonedx.generators.BomGeneratorFactory;
import org.cyclonedx.generators.json.BomJsonGenerator;
import org.cyclonedx.model.Bom;
import org.cyclonedx.model.LifecycleChoice;
import org.cyclonedx.model.Lifecycles;
import org.cyclonedx.model.Metadata;

/**
 * Task that creates CycloneDX BOM for a single component.
 */
public class ComponentBomTask extends Task {

    private File bomFile;

    public void setBomFile(File f) {
        bomFile = f;
    }

    public void execute() {
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

        bom.setMetadata(meta);

        BomJsonGenerator generator = BomGeneratorFactory.createJson(Version.VERSION_16, bom);
        try (FileOutputStream fos = new FileOutputStream(bomFile);
             OutputStreamWriter writer = new OutputStreamWriter(fos, StandardCharsets.UTF_8)) {
            writer.write(generator.toJsonString(true));
        } catch (IOException | GeneratorException ex) {
            throw new BuildException("failed to write BOM", ex);
        }
    }
}

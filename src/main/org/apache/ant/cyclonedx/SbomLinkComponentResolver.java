/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package org.apache.ant.cyclonedx;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.Resource;
import org.apache.tools.ant.types.resources.FileProvider;
import org.apache.tools.ant.types.resources.URLProvider;

import org.cyclonedx.exception.ParseException;
import org.cyclonedx.model.Bom;
import org.cyclonedx.parsers.BomParserFactory;
import org.cyclonedx.parsers.Parser;

/**
 * Reads a linked SBOM and configures the component containing the link.
 *
 * @since CycloneDX Antlib 0.2
 */
class SbomLinkComponentResolver {
    private final Project project;
    private final Component.SbomLink sbomLink;

    SbomLinkComponentResolver(Project project, Component.SbomLink sbomLink) {
        this.project = project;
        this.sbomLink = sbomLink;
    }

    /**
     * Configures the given component and only adds dependencies if the component doesn't already know about any
     * dependencies itself.
     *
     * @param parent component the link applies to
     * @return Components that are direct dependencies of the parent component
     */
    Collection<Component> resolve(Component parent) throws IOException {
        Bom bom = readLinkedSbom();
        if (bom.getMetadata() == null) {
            throw new BuildException("referenced SBOM file lacks metadata");
        }
        org.cyclonedx.model.Component real = bom.getMetadata().getComponent();
        if (real == null) {
            throw new BuildException("referenced SBOM file lacks component");
        }
        List<org.cyclonedx.model.Dependency> allDependencies = bom.getDependencies();

        // only add dependencies if the component doesn't already have any dependency configuration itself
        if (!parent.areDependenciesUnknown()
            && !parent.getDependencies().iterator().hasNext()
            && allDependencies != null) {
            parent.fillFrom(real, allDependencies);
        } else {
            parent.fillFrom(real, Collections.emptyList());
        }

        if (sbomLink.getCreateBomExternalReference()
            && !parent.getExternalReferences().stream()
            .anyMatch(e -> e.getType().equals(org.cyclonedx.model.ExternalReference.Type.BOM))) {
            Resource sbom = sbomLink.iterator().next();
            URLProvider up = sbom.as(URLProvider.class);
            if (up != null) {
                ExternalReference e = new ExternalReference();
                e.setUrl(up.getURL().toExternalForm());
                e.setType(org.cyclonedx.model.ExternalReference.Type.BOM.name());
                parent.addConfiguredExternalReference(e);
            }
        }

        if (!parent.areDependenciesUnknown() && parent.getDependencies().iterator().hasNext()) {
            List<org.cyclonedx.model.Component> additionalComponents = bom.getComponents();
            if (additionalComponents != null) {
                return extractComponentsThatAreDirectDependencies(parent, additionalComponents);
            }
        }

        return Collections.emptyList();
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
        project.log("reading SBOM from " + name, Project.MSG_VERBOSE);
    }

    private List<Component> extractComponentsThatAreDirectDependencies(Component parent,
            List<org.cyclonedx.model.Component> cs) {
        List<Component> toReturn = new ArrayList<>();
        List<Component.Dependency> dependencies = new ArrayList<>();
        parent.getDependencies().iterator().forEachRemaining(dependencies::add);
        for (org.cyclonedx.model.Component c : cs) {
            Component dep = Component.from(c, Collections.emptyList());
            if (dependencies.stream().anyMatch(d -> Objects.equals(dep.getBomRef(), d.getBomRef()))) {
                // only include "additional components" the parent component depends on directly.
                // we don't want to resolve transitive dependencies automatically here
                dep.setUnknownDependencies(true);
                toReturn.add(dep);
            }
        }
        return toReturn;
    }
}

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

import java.io.IOException;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.ivy.Ivy;
import org.apache.ivy.ant.IvyAntSettings;
import org.apache.ivy.core.module.descriptor.License;
import org.apache.ivy.core.module.descriptor.ModuleDescriptor;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.core.report.ResolveReport;
import org.apache.ivy.core.resolve.IvyNode;
import org.apache.ivy.core.resolve.IvyNodeCallers.Caller;
import org.apache.ivy.core.settings.IvySettings;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.Reference;
import org.apache.tools.ant.types.resources.URLResource;

import org.cyclonedx.model.Component.Scope;

/**
 * Resolver that populates a Component from Ivy module data.
 *
 * <p>Assumes the Ivy file has already been resolved (i.e., the project
 * has run ivy:resolve already). Uses the Ivy engine to resolve dependencies
 * from the Ivy cache.</p>
 *
 * <p>Strongly influenced by Ivy's dependencytree task.</p>
 *
 * @since CycloneDX Antlib 0.2
 */
class IvyModuleComponentResolver {

    private final Component.IvyModule ivyModule;
    private final Project project;
    private boolean includeAllConfigurations;
    private Set<String> includedConfigurations;
    private Set<String> optionalConfigurations;
    private Set<String> externalConfigurations;

    IvyModuleComponentResolver(Component.IvyModule ivyModule, Project project) {
        this.ivyModule = ivyModule;
        this.project = project;
    }

    /**
     * Resolves the Ivy module and populates the component with its data.
     *
     * @param component the component to populate
     * @return additional components that are (transitive) dependencies of this component
     * @throws IOException if resolution fails
     */
    public Collection<Component> resolve(Component component) throws IOException, BuildException {
        Ivy ivy = createIvyInstance(component);
        IvySettings settings = ivy.getSettings();

        parseConfigurations(settings);

        ResolveReport report = loadResolveReport(settings);
        ModuleDescriptor root = report.getModuleDescriptor();

        Set<ModuleRevisionId> optionalModules = new HashSet<>();
        Set<ModuleRevisionId> externalModules = new HashSet<>();
        Map<ModuleRevisionId, Set<IvyNode>> dependencyTree =
            populateDependencyTree(settings, report, optionalModules, externalModules);
        fillFromModuleDescriptor(component, root, dependencyTree);

        Collection<ModuleDescriptor> allDependencies = getDependencies(dependencyTree, root);

        return allDependencies.stream()
            .map(d -> toComponent(d, dependencyTree, optionalModules, externalModules))
            .collect(Collectors.toList());
    }

    private Ivy createIvyInstance(Component component) {
        Reference settingRef = ivyModule.getSettingsRef();
        IvyAntSettings engine;
        if (settingRef == null) {
            engine = IvyAntSettings.getDefaultInstance(component);
        }
        else {
            engine = settingRef.getReferencedObject(project);
        }
        return engine.getConfiguredIvyInstance(component);
    }

    private void parseConfigurations(IvySettings settings) {
        String conf = ivyModule.getConf();
        if (conf == null || "*".equals(conf)) {
            conf = settings.getVariable("ivy.resolved.configurations");
        }
        if (conf == null) {
            throw new BuildException("no conf provided, you need to call to <resolve/> before using this task");
        }
        includeAllConfigurations = "*".equals(conf);
        if (includeAllConfigurations) {
            includedConfigurations = new HashSet<>();
        } else {
            includedConfigurations = confAsSet(conf);
        }
        optionalConfigurations = confAsSet(ivyModule.getOptionalConf());
        externalConfigurations = confAsSet(ivyModule.getExternalConf());
    }

    private ResolveReport loadResolveReport(IvySettings settings) {
        // explicit values for organisation and module would come in here, once supported
        String organisation = settings.getVariable("ivy.organisation");
        if (organisation == null) {
            throw new BuildException("no organisation provided, you need to call to <resolve/> before using this task");
        }

        String module = settings.getVariable("ivy.module");
        if (organisation == null) {
            throw new BuildException("no module provided, you need to call to <resolve/> before using this task");
        }

        ResolveReport report = getResolvedReport(organisation, module);
        if (report == null) {
            throw new BuildException("No resolution report was available," +
                                     " you need to call to <resolve/> before using this task");
        }
        return report;
    }

    private ResolveReport getResolvedReport(String org, String module) {
        String resolveId = ivyModule.getResolveId();
        ResolveReport report;
        if (resolveId != null) {
            report = project.getReference("ivy.resolved.report."+ org + "." + resolveId);
        } else {
            report = project.getReference("ivy.resolved.report."+ org + "." + module);
        }
        if (report == null) {
            report = project.getReference("ivy.resolved.report");
        }
        return report;
    }

    private Component toComponent(ModuleDescriptor md,
                                  Map<ModuleRevisionId, Set<IvyNode>> dependencyTree,
                                  Set<ModuleRevisionId> optionalModules,
                                  Set<ModuleRevisionId> externalModules) {
        Component c = new Component();
        c.setProject(project);
        fillFromModuleDescriptor(c, md, dependencyTree);

        ModuleRevisionId mrid = md.getModuleRevisionId();
        if (optionalModules.contains(mrid)) {
            c.setScope(ComponentScope.from(Scope.OPTIONAL));
        }
        c.setIsExternal(externalModules.contains(mrid));
        return c;
    }

    private static void fillFromModuleDescriptor(Component component,
                                                 ModuleDescriptor md,
                                                 Map<ModuleRevisionId, Set<IvyNode>> dependencyTree) {
        ModuleRevisionId mrid = md.getModuleRevisionId();
        if (component.getName() == null) {
            component.setName(mrid.getName());
        }
        if (component.getGroup() == null) {
            component.setGroup(mrid.getOrganisation());
        }
        if (component.getVersion() == null) {
            component.setVersion(mrid.getRevision());
        }
        if (component.getDescription() == null && md.getDescription() != null && md.getDescription().length() > 0) {
            component.setDescription(md.getDescription());
        }

        if (!component.hasLicenses()) {
            License[] ivyLicenses = md.getLicenses();
            if (ivyLicenses != null) {
                for (License ivyLicense : ivyLicenses) {
                    org.apache.ant.cyclonedx.License license = new org.apache.ant.cyclonedx.License();
                    license.setName(ivyLicense.getName());
                    if (ivyLicense.getUrl() != null) {
                        license.addConfiguredUrl(new URLResource(ivyLicense.getUrl()));
                    }
                    component.addConfiguredLicense(license);
                }
            }
        }

        // only add dependencies if the component doesn't already have any dependency configuration itself
        if (!component.areDependenciesUnknown() && !component.getDependencies().iterator().hasNext()) {
            Set<IvyNode> dependencies = dependencyTree.get(mrid);
            if (dependencies != null) {
                for (IvyNode n : dependencies) {
                    Component.Dependency d = new Component.Dependency();
                    d.setBomRef(getBomRef(n));
                    component.addDependency(d);
                }
            }
        }

        String homePage = md.getHomePage();
        if (homePage != null && !component.getExternalReferences().stream()
            .anyMatch(e -> e.getType().equals(org.cyclonedx.model.ExternalReference.Type.WEBSITE))) {
            ExternalReference e = new ExternalReference();
            e.setUrl(homePage);
            e.setType(org.cyclonedx.model.ExternalReference.Type.WEBSITE.name());
            component.addConfiguredExternalReference(e);
        }
    }

    private Map<ModuleRevisionId, Set<IvyNode>> populateDependencyTree(IvySettings settings,
                                                                       ResolveReport report,
                                                                       Set<ModuleRevisionId> optionalModules,
                                                                       Set<ModuleRevisionId> externalModules) {
        Map<ModuleRevisionId, Set<IvyNode>> tree = new HashMap<>();
        for (IvyNode dependency : report.getDependencies()) {
            populateDependencyTree(dependency, tree, optionalModules, externalModules);
        }
        return tree;
    }

    private void populateDependencyTree(IvyNode node,
                                        Map<ModuleRevisionId, Set<IvyNode>> tree,
                                        Set<ModuleRevisionId> optionalModules,
                                        Set<ModuleRevisionId> externalModules) {
        if (!isIncluded(node)) {
            return;
        }
        ModuleRevisionId mrid = node.getId();
        if (isOptional(node)) {
            optionalModules.add(mrid);
        }
        if (isExternal(node)) {
            externalModules.add(mrid);
        }

        tree.computeIfAbsent(mrid, _ignored -> new HashSet<>());
        for (Caller caller : node.getAllCallers()) {
            addDependency(caller.getModuleRevisionId(), node, tree);
        }
    }

    private void addDependency(ModuleRevisionId caller, IvyNode dependency, Map<ModuleRevisionId, Set<IvyNode>> tree) {
        Set<IvyNode> deps = tree.computeIfAbsent(caller, _ignored -> new HashSet<>());
        deps.add(dependency);
    }

    private Collection<ModuleDescriptor> getDependencies(Map<ModuleRevisionId, Set<IvyNode>> tree,
                                                         ModuleDescriptor root) {
        Set<ModuleRevisionId> seen = new HashSet<>();
        ModuleRevisionId rootId = root.getModuleRevisionId();
        seen.add(rootId);
        List<ModuleDescriptor> deps = new ArrayList<>();
        appendDependencies(tree, rootId, deps, seen);
        return deps;
    }

    private void appendDependencies(Map<ModuleRevisionId, Set<IvyNode>> tree, ModuleRevisionId mrid,
                                    List<ModuleDescriptor> deps, Set<ModuleRevisionId> seen) {
        Set<IvyNode> thisDeps = tree.get(mrid);
        if (thisDeps != null) {
            for (IvyNode d : thisDeps) {
                ModuleRevisionId depId = d.getId();
                if (!seen.contains(depId)) {
                    seen.add(depId);
                    deps.add(d.getDescriptor());
                    appendDependencies(tree, depId, deps, seen);
                }
            }
        }
    }

    private Set<String> confAsSet(String conf) {
        if (conf == null) {
            return Collections.emptySet();
        }
        return Arrays.stream(conf.split(","))
            .map(c -> c.trim())
            .filter(c -> c.length() > 0)
            .collect(Collectors.toSet());
    }

    private boolean isIncluded(IvyNode node) {
        return includeAllConfigurations
            || node.getRootModuleConfigurationsSet().stream().anyMatch(c -> includedConfigurations.contains(c));
    }

    private boolean isOptional(IvyNode node) {
        return isSpecial(node, optionalConfigurations);
    }

    private boolean isExternal(IvyNode node) {
        return isSpecial(node, externalConfigurations);
    }

    private boolean isSpecial(IvyNode node, Set<String> specialConfigurations) {
        if (specialConfigurations.isEmpty()) {
            return false;
        }
        Set<String> rootConfs = node.getRootModuleConfigurationsSet();
        Stream<String> includedBecauseOf = includeAllConfigurations ? rootConfs.stream()
            : rootConfs.stream().filter(c -> includedConfigurations.contains(c));
        return includedBecauseOf.allMatch(c -> specialConfigurations.contains(c));
    }

    private static String getBomRef(IvyNode n) {
        ModuleRevisionId mrid = n.getId();
        return "pkg:maven/" + mrid.getOrganisation() + "/" + mrid.getName() + "@" + mrid.getRevision() + "?type=jar";
    }
}

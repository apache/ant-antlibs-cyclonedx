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
import java.util.Collection;

import org.apache.tools.ant.BuildException;

/**
 * Common interface for "resolvers" that read data of a component form external sources and enrich an existing component
 * withe data read - and may even identify additional components.
 *
 * <p>Any additional component detected may be added to the SBOM. If another component already exists with the same
 * group and name coordinates the additional component detected by this instance is ignored.</p>
 */
public interface ComponentResolver {
    /**
     * Resolves the external data and enriches the component with it.
     *
     * @param component the component to enrich
     * @return additional components identified while reading external data
     * @throws IOException if resolution fails
     */
    Collection<Component> resolve(Component component) throws IOException, BuildException;
}

package org.apache.ant.cyclonedx;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.types.EnumeratedAttribute;

import org.cyclonedx.model.Component.Scope;

/**
 * CycloneDX component type.
 *
 * <p>Accepts the enum constants like {@code LIBRARY} as well as the
 * lowercase version {@code library}. The values are directly provided
 * by CycloneDX Core's enum.</p>
 */
public class ComponentScope extends EnumeratedAttribute {

    @Override
    public String[] getValues() {
        return EnumUtils.valuesPlus(Scope.class, Scope::getScopeName);
    }

    /**
     * Translates this instance to a {@link Scope}.
     *
     * @throws BuildException if the value can not be translated.
     */
    public Scope getScope() {
        return EnumUtils.valueOf(Scope.class, getValue(), Scope::getScopeName);
    }

    public static ComponentScope from(Scope scope) {
        ComponentScope s = new ComponentScope();
        s.setValue(scope.name());
        return s;
    }
}

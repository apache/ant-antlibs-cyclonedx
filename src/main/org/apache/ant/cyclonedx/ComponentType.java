package org.apache.ant.cyclonedx;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.types.EnumeratedAttribute;

import org.cyclonedx.model.Component.Type;

/**
 * CycloneDX component type.
 *
 * <p>Accepts the enum constants like {@code LIBRARY} as well as the
 * lowercase version {@code library}. The values are directly provided
 * by CycloneDX Core's enum.</p>
 */
public class ComponentType extends EnumeratedAttribute {

    @Override
    public String[] getValues() {
        return EnumUtils.valuesPlus(Type.class, Type::getTypeName);
    }

    /**
     * Translates this instance to a {@link Type}.
     *
     * @throws BuildException if the value can not be translated.
     * @return CycloneDX type of this instance
     */
    public Type getType() {
        return EnumUtils.valueOf(Type.class, getValue(), Type::getTypeName);
    }

    /**
     * Maps a CycloneDX type.
     *
     * @param type CycloneDX type.
     * @return translated type
     */
    public  static ComponentType from(Type type) {
        ComponentType t = new ComponentType();
        t.setValue(type.name());
        return t;
    }
}

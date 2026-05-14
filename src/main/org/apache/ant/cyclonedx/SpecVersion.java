package org.apache.ant.cyclonedx;

import java.util.Objects;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.types.EnumeratedAttribute;

import org.cyclonedx.Version;

/**
 * CycloneDX specification version to use for the SBOM.
 *
 * <p>Accepts the enum constants like {@code VERSION_16} as well as
 * the human readable version {@code 1.6}. The values are directly
 * provided by CycloneDX Core's enum.</p>
 */
public class SpecVersion extends EnumeratedAttribute {

    public static final SpecVersion DEFAULT;

    static {
        DEFAULT = new SpecVersion();
        DEFAULT.setValue(Version.VERSION_16.name());
    }

    @Override
    public String[] getValues() {
        return EnumUtils.valuesPlus(Version.class, Version::getVersionString);
    }

    /**
     * Translates this instance to a {@link Version}.
     *
     * @throws BuildException if the value can not be translated.
     */
    public Version getVersion() {
        Version version = Version.fromVersionString(getValue());
        if (version == null) {
            try {
                version = Version.valueOf(getValue());
            } catch (IllegalArgumentException ex) {
                throw new BuildException(getValue() + " is not a supported version");
            }
        }
        return version;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        return other != null && other.getClass().equals(getClass())
            && Objects.equals(((SpecVersion) other).getValue(), getValue());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getValue());
    }
}

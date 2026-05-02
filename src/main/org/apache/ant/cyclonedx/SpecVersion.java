package org.apache.ant.cyclonedx;

import java.util.Objects;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.types.EnumeratedAttribute;

import org.cyclonedx.Version;

public class SpecVersion extends EnumeratedAttribute {

    public static final SpecVersion VERSION_16;

    static {
        VERSION_16 = new SpecVersion();
        VERSION_16.setValue("1.6");
    }

    @Override
    public String[] getValues() {
        return new String[] { "1.0", "1.1", "1.2", "1.3", "1.4", "1.5", "1.6" };
    }

    public Version getVersion() {
        switch (getValue()) {
        case "1.0":
            return Version.VERSION_10;
        case "1.1":
            return Version.VERSION_11;
        case "1.2":
            return Version.VERSION_12;
        case "1.3":
            return Version.VERSION_13;
        case "1.4":
            return Version.VERSION_14;
        case "1.5":
            return Version.VERSION_15;
        case "1.6":
            return Version.VERSION_16;
        default:
            throw new BuildException("version '" + getValue() + "' is not supported");
        }
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

package org.apache.ant.cyclonedx;

import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Stream;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.types.EnumeratedAttribute;

import org.cyclonedx.Version;
import org.cyclonedx.Format;

/**
 * CycloneDX format to use for the SBOM.
 *
 * <p>Accepts the enum constants like {@code JSON} as well as the
 * lowercase version {@code json} and the special value {@code
 * all}. The values other than {@code all} are directly provided by
 * CycloneDX Core's enum.</p>
 *
 * <p>{@code all} means the task will emit SBOMs in all formats
 * supported by the selected {@link SpecVersion} - i.e. only XML for
 * versions 1.1 and 1.2 and both JSON and XML afterwards.</p>
 */
public class OutputFormat extends EnumeratedAttribute {

    private static final String ALL = "ALL";

    public static final OutputFormat JSON;

    static {
        JSON = new OutputFormat();
        JSON.setValue(Format.JSON.name());
    }

    @Override
    public String[] getValues() {
        return Stream
            .concat(Arrays.stream(EnumUtils.valuesPlus(Format.class, Format::getExtension)),
                    Stream.of(ALL, ALL.toLowerCase(Locale.ENGLISH)))
            .toArray(String[]::new);
    }

    /**
     * Translates this instance to {@link Format}s.
     *
     * @throws BuildException if the value can not be translated.
     */
    public Iterable<Format> getCycloneDxFormats(Version version) {
        String value = getValue();
        if (value.equalsIgnoreCase("all")) {
            return version.getFormats();
        }
        Format format = Format.fromExtension(value);
        if (format == null) {
            try {
                format = Format.valueOf(value);
            } catch (IllegalArgumentException ex) {
                throw new BuildException(getValue() + " is not a supported format");
            }
        }
        return Arrays.asList(format);
    }
}

package org.apache.ant.cyclonedx;

import java.util.Arrays;

import org.cyclonedx.Format;

public enum OutputFormat {
    json(Format.JSON),
    xml(Format.XML),
    all(Format.JSON, Format.XML);

    private Format[] formats;

    private OutputFormat(Format... formats) {
        this.formats = formats;
    }

    public Iterable<Format> getCycloneDxFormats() {
        return Arrays.asList(formats);
    }
}

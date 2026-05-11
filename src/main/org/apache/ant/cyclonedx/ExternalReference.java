package org.apache.ant.cyclonedx;

import org.apache.tools.ant.BuildException;

public class ExternalReference {
    private String url;
    private org.cyclonedx.model.ExternalReference.Type type;

    public void setUrl(String url) {
        this.url = url;
    }

    public void setType(org.cyclonedx.model.ExternalReference.Type type) {
        this.type = type;
    }

    public org.cyclonedx.model.ExternalReference toCycloneDxExternalReference() {
        if (url == null) {
            throw new BuildException("external references must have an url");
        }
        if (type == null) {
            throw new BuildException("external references must have a type");
        }
        org.cyclonedx.model.ExternalReference r = new org.cyclonedx.model.ExternalReference();
        r.setUrl(url);
        r.setType(type);
        return r;
    }
}

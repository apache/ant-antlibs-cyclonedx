package org.apache.ant.cyclonedx;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.types.DataType;
import org.apache.tools.ant.types.resources.URLResource;

public class License extends DataType {
    private String id;
    private String name;
    private String url;

    public void setLicenseId(String id) {
        checkAttributesAllowed();
        this.id = id;
    }

    public void setName(String name) {
        checkAttributesAllowed();
        this.name = name;
    }

    public void addConfiguredUrl(URLResource url) {
        checkAttributesAllowed();
        if (this.url != null) {
            throw new BuildException("only one URL is allowed");
        }
        this.url = url.getURL().toExternalForm();
    }

    public org.cyclonedx.model.License toCycloneDxLicense() {
        if (isReference()) {
            return getRef().toCycloneDxLicense();
        }
        if (name == null && id == null) {
            throw new BuildException("license name or id is required");
        }
        org.cyclonedx.model.License l = new org.cyclonedx.model.License();
        if (name != null) {
            l.setName(name);
        }
        if (id != null) {
            l.setId(id);
        }
        if (url != null) {
            l.setUrl(url);
        }
        return l;
    }

    /**
     * Perform the check for circular references and return the
     * referenced License.
     * @return <code>License</code>.
     */
    protected License getRef() {
        return getCheckedRef(License.class);
    }
}

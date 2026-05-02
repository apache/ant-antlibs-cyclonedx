package org.apache.ant.cyclonedx;

import java.util.ArrayList;
import java.util.List;

import org.apache.tools.ant.types.DataType;
import org.apache.tools.ant.types.resources.URLResource;

import org.cyclonedx.model.OrganizationalEntity;

public class Organization extends DataType {
    private String name;
    private List<String> urls = new ArrayList<>();

    public void setName(String name) {
        checkAttributesAllowed();
        this.name = name;
    }

    public void addConfiguredUrl(URLResource url) {
        checkAttributesAllowed();
        urls.add(url.getURL().toExternalForm());
    }

    public OrganizationalEntity toOrganizationalEntity() {
        if (isReference()) {
            return getRef().toOrganizationalEntity();
        }
        OrganizationalEntity oe = new OrganizationalEntity();
        if (name != null) {
            oe.setName(name);
        }
        if (!urls.isEmpty()) {
            oe.setUrls(urls);
        }
        return oe;
    }

    /**
     * Perform the check for circular references and return the
     * referenced Organization.
     * @return <code>Organization</code>.
     */
    protected Organization getRef() {
        return getCheckedRef(Organization.class);
    }
}

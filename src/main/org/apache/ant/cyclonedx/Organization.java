package org.apache.ant.cyclonedx;

import java.util.ArrayList;
import java.util.List;

import org.apache.tools.ant.types.resources.URLResource;

import org.cyclonedx.model.OrganizationalEntity;

public class Organization {
    private String name;
    private List<String> urls = new ArrayList<>();

    public void setName(String name) {
        this.name = name;
    }

    public void addConfiguredUrl(URLResource url) {
        urls.add(url.getURL().toExternalForm());
    }

    public OrganizationalEntity toOrganizationalEntity() {
        OrganizationalEntity oe = new OrganizationalEntity();
        if (name != null) {
            oe.setName(name);
        }
        if (!urls.isEmpty()) {
            oe.setUrls(urls);
        }
        return oe;
    }
}

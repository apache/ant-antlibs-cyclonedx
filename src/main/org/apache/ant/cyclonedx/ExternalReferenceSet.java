package org.apache.ant.cyclonedx;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.tools.ant.types.DataType;

public class ExternalReferenceSet extends DataType {
    private List<org.cyclonedx.model.ExternalReference> externalReferences = new ArrayList<>();

    public void addConfiguredExternalReference(ExternalReference ref) {
        checkChildrenAllowed();
        externalReferences.add(ref.toCycloneDxExternalReference());
    }

    public Collection<org.cyclonedx.model.ExternalReference> getExternalReferences() {
        if (isReference()) {
            return getRef().getExternalReferences();
        }
        dieOnCircularReference();
        return externalReferences;
    }

    /**
     * Perform the check for circular references and return the
     * referenced Organization.
     * @return <code>Organization</code>.
     */
    protected ExternalReferenceSet getRef() {
        return getCheckedRef(ExternalReferenceSet.class);
    }
}

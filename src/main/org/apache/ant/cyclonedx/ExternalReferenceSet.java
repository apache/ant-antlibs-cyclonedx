package org.apache.ant.cyclonedx;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.tools.ant.types.DataType;

/**
 * A container for a collection of {@link ExternalReference}s.
 *
 * <p>This class is a type exposed by this Ant Library. When using the
 * inherited {@code refid} attribute it can reference an instance
 * defined previously - in which case no child elements are
 * allowed.</p>
 */
public class ExternalReferenceSet extends DataType {
    private List<org.cyclonedx.model.ExternalReference> externalReferences = new ArrayList<>();

    /**
     * Adds an external reference to this set.
     */
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
     * referenced ExternalReferenceSet.
     * @return <code>ExternalReferenceSet</code>.
     */
    protected ExternalReferenceSet getRef() {
        return getCheckedRef(ExternalReferenceSet.class);
    }
}

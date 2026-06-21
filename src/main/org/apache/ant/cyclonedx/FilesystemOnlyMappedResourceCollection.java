package org.apache.ant.cyclonedx;

import java.util.Collections;
import java.util.Iterator;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.types.DataType;
import org.apache.tools.ant.types.Mapper;
import org.apache.tools.ant.types.Resource;
import org.apache.tools.ant.types.ResourceCollection;
import org.apache.tools.ant.types.resources.MappedResourceCollection;
import org.apache.tools.ant.types.resources.ResourceDecorator;
import org.apache.tools.ant.types.resources.Union;
import org.apache.tools.ant.util.FileNameMapper;
import org.apache.tools.ant.util.IdentityMapper;
import org.apache.tools.ant.util.MergingMapper;

/**
 * A variant of {@link MappedResourceCollection} that only accepts
 * file-system resource collections as children and which returns
 * resources that provide access to the underlying files (unlike
 * {@link MappedResourceCollection} which prevents direct access.
 *
 * <p>It wraps another resource collection and maps their names using
 * the provided mapper when returning the resources.</p>
 *
 * <p>This type should not be used with arbitrary Ant tasks as the
 * tasks may use the underlying File's name instead of the mapped name
 * when iterating over the resources. It is useful when used with
 * {@link ComponentBomTask}'s pure file components as all that is used
 * there is the mapped name and the content of the actual file (to
 * calculate hashes).</p>
 *
 * @since CycloneDX Antlib 0.2
 */
public final class FilesystemOnlyMappedResourceCollection
    extends DataType implements ResourceCollection {

    private Union nested = null;
    private Mapper mapper = null;

    /**
     * Adds the resources to map.
     */
    public void add(ResourceCollection c) throws BuildException {
        checkChildrenAllowed();
        if (!c.isFilesystemOnly()) {
            throw new BuildException("only file-system resources allowed as children",
                                     getLocation());
        }
        if (nested == null) {
            nested = new Union(getProject());
        }
        nested.add(c);
    }

    /**
     * Creates a nested mapper.
     */
    public Mapper createMapper() throws BuildException {
        checkChildrenAllowed();
        if (mapper != null) {
            throw new BuildException("Cannot define more than one mapper",
                                     getLocation());
        }
        mapper = new Mapper(getProject());
        return mapper;
    }

    /**
     * Creates a nested mapper by directly providing the implementation.
     */
    public void add(FileNameMapper fileNameMapper) {
        createMapper().add(fileNameMapper);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isFilesystemOnly() {
        if (isReference()) {
            return getRef().isFilesystemOnly();
        }
        return true;
    }

    @Override
    public Iterator<Resource> iterator() {
        if (isReference()) {
            return getRef().iterator();
        }
        if (nested == null) {
            return Collections.emptyIterator();
        }
        FileNameMapper m =
            mapper == null ? new IdentityMapper() : mapper.getImplementation();
        return nested.stream().map(r -> (Resource) new MappedResource(r, m)).iterator();
    }

    @Override
    public int size() {
        if (isReference()) {
            return getRef().size();
        }
        if (nested == null) {
            return 0;
        }
        return nested.size();
    }

    private FilesystemOnlyMappedResourceCollection getRef() {
        return getCheckedRef(FilesystemOnlyMappedResourceCollection.class);
    }

    private static class MappedResource extends ResourceDecorator {
        private final FileNameMapper mapper;

        private MappedResource(Resource r, FileNameMapper m) {
            super(r);
            mapper = m;
        }

        @Override
        public String getName() {
            String name = getResource().getName();
            String[] mapped = mapper.mapFileName(name);
            return mapped != null && mapped.length == 1 ? mapped[0] : null;
        }
    }
}

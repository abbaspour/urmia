package io.urmia.md.mock;

import com.google.common.base.Optional;
import io.urmia.md.repo.MetadataRepository;
import io.urmia.md.exception.MetadataException;
import io.urmia.md.model.storage.Etag;
import io.urmia.md.model.storage.FullObjectName;
import io.urmia.md.model.storage.ObjectName;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class MockMetadataRepositoryImpl implements MetadataRepository {

    final List<FullObjectName> objectNames;
    final List<String> storageNodes;

    public MockMetadataRepositoryImpl(final List<FullObjectName> objectNames, final List<String> storageNodes) {
        this.objectNames = objectNames;
        this.storageNodes = storageNodes;
    }

    @Override
    public Optional<FullObjectName> selectByName(ObjectName on) throws MetadataException {
        for(FullObjectName fon : objectNames)
            if(fon.toSimpleString().equalsIgnoreCase(on.toString()))
                    return Optional.of(fon);
        return Optional.absent();
    }

    @Override
    public void insert(FullObjectName fon) throws MetadataException {
        objectNames.add(fon);
    }

    @Override
    public void insertStored(/*StorageNode*/String storageNode, Etag etag) throws MetadataException { }

    @Override
    public void delete(ObjectName on) throws MetadataException {
        Iterator<FullObjectName> fonItr = objectNames.iterator();

        while(fonItr.hasNext()) {
            FullObjectName fon = fonItr.next();
            if(fon.toSimpleString().equalsIgnoreCase(on.toString()))
                fonItr.remove();
        }
    }

    @Override
    public List<FullObjectName> listDir(ObjectName on, int limit) throws MetadataException {
        return objectNames.subList(0, limit);
    }

    @Override
    public boolean deletable(ObjectName dir) throws MetadataException {
        throw new RuntimeException("not implemented");
    }

    /*
    @Override
    public List<String> listStorageNodes() throws MetadataException {
        return storageNodes;
    }
    */

    @Override
    public List<String> findStorageByName(ObjectName on) throws MetadataException {
        Optional<FullObjectName> fon = selectByName(on);
        return fon.isPresent() ? findStorageNameByEtag(fon.get().attributes.etag) : Collections.<String>emptyList();
    }

    @Override
    public List<String> findStorageNameByEtag(String etag) throws MetadataException {
        throw new RuntimeException("not implemented");
    }
}

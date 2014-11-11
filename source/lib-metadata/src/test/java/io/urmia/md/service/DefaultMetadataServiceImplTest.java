package io.urmia.md.service;

import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import io.netty.util.concurrent.ImmediateEventExecutor;
import io.urmia.md.repo.MetadataRepository;
import io.urmia.md.mock.MockMetadataRepositoryImpl;
import io.urmia.md.model.*;
import io.urmia.md.model.storage.ExtendedObjectAttributes;
import io.urmia.md.model.storage.FullObjectName;
import io.urmia.md.model.storage.ObjectName;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class DefaultMetadataServiceImplTest {

    private static final Logger log = LoggerFactory.getLogger(DefaultMetadataServiceImplTest.class);

    private static HttpRequest httpRequest(HttpMethod method, String uri) {
        return new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, method, uri);
    }

    private static MetadataRepository mockRepository() {
        List<FullObjectName> objectNames = new ArrayList<FullObjectName>();
        ExtendedObjectAttributes eoa = new ExtendedObjectAttributes(false, 1, "==md5==", "--etag--", 2, System.currentTimeMillis());
        objectNames.add(new FullObjectName(ObjectName.of("/a/stor/c").get(), eoa));

        List<String> storageNodes = new ArrayList<String>();
        //StorageNode node01 = new StorageNode("some-storage-location", 1, "some-host", 0);
        storageNodes.add("some-storage-location");

        return new MockMetadataRepositoryImpl(objectNames, storageNodes);
    }

    @Test
    public void testList_01() throws InterruptedException {
        MetadataRepository repository = mockRepository();
        MetadataService mds = new DefaultMetadataServiceImpl(repository);

        ObjectRequest request = new ObjectRequest(httpRequest(HttpMethod.GET, "/owner/index/parent/path"));

        mds.list(ImmediateEventExecutor.INSTANCE, request).addListener(
                new GenericFutureListener<Future<? super ObjectResponse>>() {
                    @Override
                    public void operationComplete(Future<? super ObjectResponse> future) throws Exception {
                        if (future.isSuccess()) {
                            log.info("fetched: {}", future.get());
                        } else {
                            log.debug("failed: {}", future.cause());
                        }
                    }
                }
        ).sync();

    }
}

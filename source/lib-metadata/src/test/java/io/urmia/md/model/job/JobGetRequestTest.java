package io.urmia.md.model.job;

import io.urmia.md.model.ObjectRequest;
import io.urmia.md.model.storage.ObjectName;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class JobGetRequestTest {
    @Test
    public void testParseUri() {
        String uri = "/abbaspour/jobs/d081adc6-c193-48ef-a99a-85c4e5934cfb/stor/abbaspour/stor/treasure_island.txt.0.c8598f52-3c14-451a-9b2f-354579a2d5e8";
        ObjectRequest objectRequest = JobGetRequest.fromJobGetHttpRequest(uri);

        assertNotNull(objectRequest);

        ObjectName on = objectRequest.objectName;

        assertNotNull(on);

        assertEquals("abbaspour", on.owner);
        assertEquals(ObjectName.Namespace.STOR, on.ns);
        assertEquals("treasure_island.txt.0.c8598f52-3c14-451a-9b2f-354579a2d5e8", on.name);
    }

    @Test
    public void testStorageId_01() {
        String uri = "/abbaspour/jobs/d081adc6-c193-48ef-a99a-85c4e5934cfb/stor/abbaspour/stor/treasure_island.txt.0.c8598f52-3c14-451a-9b2f-354579a2d5e8";
        String storageId = JobGetRequest.getStorageNodeId(uri);
        assertEquals("c8598f52-3c14-451a-9b2f-354579a2d5e8", storageId);
    }

    @Test
    public void testStorageId_02() {
        String uri = "/abbaspour/jobs/d081adc6-c193-48ef-a99a-85c4e5934cfb/stor/abbaspour/jobs/c8598f52-3c14-451a-9b2f-354579a2d5e8";
        String storageId = JobGetRequest.getStorageNodeId(uri);
        assertEquals("c8598f52-3c14-451a-9b2f-354579a2d5e8", storageId);
    }
}

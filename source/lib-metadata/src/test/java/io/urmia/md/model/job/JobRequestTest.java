package io.urmia.md.model.job;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class JobRequestTest {
    @Test
    public void testGetJobId() {
        String id = JobRequest.getId("/jobs/506b260b-3e97-4f23-b175-66154889e8ad/live/in");
        assertEquals("506b260b-3e97-4f23-b175-66154889e8ad", id);
    }
}

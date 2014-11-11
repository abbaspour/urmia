package io.urmia.job;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class JobExecutorFactoryTest {

    private static final Logger log = LoggerFactory.getLogger(JobExecutorFactoryTest.class);

    @Test
    public void testLookup01() {
        JobExecutorFactory jef = JobExecutorFactory.lookup();
        log.info("JobExecutorFactory from lookup is: {}", jef);
        assertNotNull(jef);
        assertTrue(jef instanceof XargsJobExecutor.Factory);
    }
}

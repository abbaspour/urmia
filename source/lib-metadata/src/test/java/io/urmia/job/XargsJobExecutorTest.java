package io.urmia.job;

import com.google.common.collect.Lists;
import com.google.gson.JsonElement;
import io.urmia.md.model.job.JobDefinition;
import io.urmia.md.model.job.JobExec;
import io.urmia.md.model.job.LineJobInput;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import static io.urmia.md.model.job.JobDefinition.Phase.Type.MAP;
import static org.junit.Assert.assertEquals;

public class XargsJobExecutorTest {

    private static final Logger log = LoggerFactory.getLogger(XargsJobExecutorTest.class);

    private static final String TEST_MOUNT_POINT = "/tmp/manta/xargsTest/";

    @BeforeClass
    public static void setup() {
        File d = new File(TEST_MOUNT_POINT);
        //noinspection ResultOfMethodCallIgnored
        d.mkdirs();
    }

    @Test
    public void test_ls() throws IOException, InterruptedException {
        JobExec exec = new JobExec.Shell("/bin/ls -1 %");
        JobDefinition.Phase phase = new JobDefinition.Phase(exec);
        JobDefinition jd = new JobDefinition("tck", Lists.newArrayList(phase));
        JobExecutor je = new XargsJobExecutor("testHostId", jd, "1", TEST_MOUNT_POINT, MAP);
        je.run();

        je.addInput(new LineJobInput("/"));
        je.addInput(new LineJobInput("/usr"));
        je.terminateInput();

        assertEquals(0, je.exitCode());
    }


    @Test
    public void test_ls2pipe() throws IOException, InterruptedException {
        JobDefinition.Phase phase1 = new JobDefinition.Phase(new JobExec.Shell("/bin/ls -1 | wc -l"));

        JobDefinition jd = new JobDefinition("tck", Lists.newArrayList(phase1));
        JobExecutor je = new XargsJobExecutor("testHostId", jd, "2", TEST_MOUNT_POINT, MAP);
        je.run();

        je.addInput(new LineJobInput("/"));
        je.addInput(new LineJobInput("/usr"));
        je.terminateInput();

        assertEquals(0, je.exitCode());
    }

    @Test
    public void test_ls2phase() throws IOException, InterruptedException {
        JobDefinition.Phase phase1 = new JobDefinition.Phase(new JobExec.Shell("/bin/ls -1"));
        JobDefinition.Phase phase2 = new JobDefinition.Phase(new JobExec.Shell("wc -l"));

        JobDefinition jd = new JobDefinition("tck", Lists.newArrayList(phase1, phase2));
        JobExecutor je = new XargsJobExecutor("testHostId", jd, "3", TEST_MOUNT_POINT, MAP);
        je.run();

        je.addInput(new LineJobInput("/"));
        je.addInput(new LineJobInput("/usr"));
        je.terminateInput();

        assertEquals(0, je.exitCode());
    }

    @Test
    public void test_lsROOT() throws IOException, InterruptedException {
        JobDefinition.Phase phase1 = new JobDefinition.Phase(new JobExec.Shell("/bin/ls -1d"));

        JobDefinition jd = new JobDefinition("tck", Lists.newArrayList(phase1));
        JobExecutor je = new XargsJobExecutor("testHostId", jd, "4", TEST_MOUNT_POINT, MAP);
        je.run();

        //Thread.sleep(500);

        log.info("adding input: /");
        je.addInput(new LineJobInput("/tck/"));
        //Thread.sleep(500);

        log.info("terminate input");
        je.terminateInput();
        //Thread.sleep(500);

        assertEquals(0, je.exitCode());
        //Thread.sleep(2000);

        log.info("checking output file: {}", je.getOutputPath());
        Scanner s = new Scanner(new File(je.getOutputPath()));
        assertEquals("/tmp/manta/xargsTest/tck/", s.nextLine());
    }

    @Test
    public void test_lsROOTgrep() throws IOException, InterruptedException {
        JobDefinition.Phase phase1 = new JobDefinition.Phase("/bin/ls -1 | grep jobs");

        JobDefinition jd = new JobDefinition("tck", Lists.newArrayList(phase1));
        JobExecutor je = new XargsJobExecutor("testHostId", jd, "5", TEST_MOUNT_POINT, MAP);
        je.run();

        //Thread.sleep(500);

        je.addInput(new LineJobInput("/tck"));
        je.terminateInput();

        assertEquals(0, je.exitCode());

        //Thread.sleep(1000);

        log.info("checking output file: {}", je.getOutputPath());

        Scanner s = new Scanner(new File(je.getOutputPath()));
        assertEquals("jobs", s.nextLine());
    }

    @Test
    public void test_lsROOTgrep2phase() throws IOException, InterruptedException {
        JobDefinition.Phase phase1 = new JobDefinition.Phase("/bin/ls -1");
        JobDefinition.Phase phase2 = new JobDefinition.Phase("grep jobs");

        JobDefinition jd = new JobDefinition("tck", Lists.newArrayList(phase1, phase2));
        JobExecutor je = new XargsJobExecutor("testHostId", jd, "6", TEST_MOUNT_POINT, MAP);
        je.run();

        //Thread.sleep(200);

        je.addInput(new LineJobInput("/tck/"));
        je.terminateInput();

        assertEquals(0, je.exitCode());

        //Thread.sleep(500);

        log.info("checking output file: {}", je.getOutputPath());
        Scanner s = new Scanner(new File(je.getOutputPath()));
        assertEquals("jobs", s.nextLine());
    }

    @Test
    public void test_jsonWordCount() throws IOException, InterruptedException {
        File d = new File(TEST_MOUNT_POINT + "tck/jobs/7/");
        //noinspection ResultOfMethodCallIgnored
        d.mkdirs();
        File f = File.createTempFile("json_wc", ".tmp", d);

        String objName = "/tck/jobs/7/" + f.getName();
        log.info("temp file path: {}", objName);

        PrintWriter in = new PrintWriter(f);

        final int lineCount = 5;
        for(int i = 1; i <= lineCount; i++) in.println("line: " + i);
        in.flush();

        JobDefinition jd = new JobDefinition(makeJsonJob("tck", "wc -l"));
        JobExecutor je = new XargsJobExecutor("testHostId", jd, "7", TEST_MOUNT_POINT, MAP);
        je.run();

        je.addInput(new LineJobInput(objName));
        je.terminateInput();

        assertEquals(0, je.exitCode());

        log.info("output path: {}", je.getOutputPath());
        assertJobOutputEquals(je, lineCount + " " + f.getAbsolutePath());
    }

    private static JsonElement makeJsonJob(String owner, String... execs) {
        List<JobDefinition.Phase> phases = new ArrayList<JobDefinition.Phase>(execs.length);
        for(String exec : execs)
            phases.add(new JobDefinition.Phase(exec));
        return new JobDefinition(owner, phases).toJson();
    }

    private static void assertJobOutputEquals(JobExecutor je, String result) throws FileNotFoundException, InterruptedException {
        //Thread.sleep(1000);
        Scanner s = new Scanner(new File(je.getOutputPath()));
        assertEquals(result.trim(), s.nextLine().trim());
    }
}

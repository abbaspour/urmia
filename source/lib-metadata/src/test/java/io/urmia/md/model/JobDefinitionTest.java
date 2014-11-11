package io.urmia.md.model;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import io.urmia.md.model.job.JobDefinition;
import org.junit.Assert;
import org.junit.Test;

public class JobDefinitionTest {

    private static final Gson gson = new Gson();

    // http://apidocs.joyent.com/manta/jobs-reference.html
    @Test
    public void testParseJson_01() {

        String json = "{" +
                "\"phases\":  [ " +
                "{\"exec\": \"grep -w main\"}, " +
                "{" +
                " \"type\": \"reduce\"," +
                " \"exec\": \"/var/tmp/stackvis/bin/stackvis\"," +
                " \"count\": 2," +
                " \"image\": \">=13.1.0\"," +
                " \"assets\": [ \"/:login/stor/stackvis.tgz\" ]," +
                " \"init\": \"cd /var/tmp && tar xzf /assets/:login/stor/stackvis.tgz\"," +
                " \"memory\": 2048," +
                " \"disk\": 16" +
                "} ]\n" +
                "}";

        System.out.println("json = " + json);

        JsonParser parser = new JsonParser();
        JsonElement root = parser.parse(json);

        System.out.println("root = " + root);

        JsonElement phases = root.getAsJsonObject().get("phases");

        System.out.println("phases = " + phases);
        System.out.println("phases size = " + phases.getAsJsonArray().size());
    }

    @Test
    public void testParseJob_01() {
        String json = "{phases: [{exec:\"ls\"}]}";

        JobDefinition job = new JobDefinition("u1", json);

        Assert.assertFalse(job.getPhases().isEmpty());
        Assert.assertEquals(1, job.getPhases().size());
        JobDefinition.Phase phase = job.getPhases().get(0);

        Assert.assertEquals(phase.type, JobDefinition.Phase.Type.MAP);
        Assert.assertNotNull(phase.exec);

        Assert.assertEquals(job.getOwner(), "u1");
    }

    @Test
    public void testParseJob_02() {

        String json = "{" +
                "\"phases\":  [ " +
                "{\"exec\": \"grep -w main\"}, " +
                "{" +
                " \"type\": \"reduce\"," +
                " \"exec\": \"/var/tmp/stackvis/bin/stackvis\"," +
                " \"count\": 2," +
                " \"image\": \">=13.1.0\"," +
                " \"assets\": [ \"/:login/stor/stackvis.tgz\" ]," +
                " \"init\": \"cd /var/tmp && tar xzf /assets/:login/stor/stackvis.tgz\"," +
                " \"memory\": 2048," +
                " \"disk\": 16" +
                "} ]\n" +
                "}";

        JobDefinition job = new JobDefinition("u2", json);

        Assert.assertFalse(job.getPhases().isEmpty());
        Assert.assertEquals(2, job.getPhases().size());
        JobDefinition.Phase phase1 = job.getPhases().get(0);

        Assert.assertEquals(phase1.type, JobDefinition.Phase.Type.MAP);
        Assert.assertNotNull(phase1.exec);

        Assert.assertEquals(job.getOwner(), "u2");
    }
}
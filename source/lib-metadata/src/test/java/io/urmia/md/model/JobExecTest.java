package io.urmia.md.model;

import io.urmia.md.model.job.JobExec;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

public class JobExecTest {

    @Test
    public void commandParseTest_01() {
        JobExec j = new JobExec.Shell("ls /");
        List<String> c = j.getCommands();
        Assert.assertNotNull(c);
        Assert.assertEquals(2, c.size());
        Assert.assertEquals("ls", c.get(0));
        Assert.assertEquals("/", c.get(1));
    }

    @Test
    public void commandParseTest_02() {
        JobExec j = new JobExec.Shell("a b c ");
        List<String> c = j.getCommands();
        Assert.assertNotNull(c);
        Assert.assertEquals(3, c.size());
        Assert.assertEquals("a", c.get(0));
        Assert.assertEquals("b", c.get(1));
        Assert.assertEquals("c", c.get(2));
    }

    @Test
    public void commandParseTest_03() {
        JobExec j = new JobExec.Shell(" a b c ");
        List<String> c = j.getCommands();
        Assert.assertNotNull(c);
        Assert.assertEquals(3, c.size());
        Assert.assertEquals("a", c.get(0));
        Assert.assertEquals("b", c.get(1));
        Assert.assertEquals("c", c.get(2));
    }

    @Test
    public void commandParseTest_04() {
        JobExec j = new JobExec.Shell(" a b \"cd\"");
        List<String> c = j.getCommands();
        Assert.assertNotNull(c);
        Assert.assertEquals(3, c.size());
        Assert.assertEquals("a", c.get(0));
        Assert.assertEquals("b", c.get(1));
        Assert.assertEquals("cd", c.get(2));
    }

    @Test
    public void commandParseTest_05() {
        JobExec j = new JobExec.Shell(" a b \"c d\"");
        List<String> c = j.getCommands();
        Assert.assertNotNull(c);
        Assert.assertEquals(3, c.size());
        Assert.assertEquals("a", c.get(0));
        Assert.assertEquals("b", c.get(1));
        Assert.assertEquals("c d", c.get(2));
    }
}

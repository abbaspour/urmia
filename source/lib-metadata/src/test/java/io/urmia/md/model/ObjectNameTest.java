package io.urmia.md.model;

import com.google.common.base.Optional;
import io.urmia.md.model.storage.ObjectName;
import org.junit.Assert;
import org.junit.Test;

public class ObjectNameTest {

    @Test
    public void testParseObjectName_01() {
        final String uri = "/abbaspour/stor/somefile";

        Optional<ObjectName> objectNameOptional = ObjectName.of(uri);

        Assert.assertTrue(objectNameOptional.isPresent());

        ObjectName objectName = objectNameOptional.get();

        Assert.assertEquals("abbaspour", objectName.owner);
        Assert.assertEquals(ObjectName.Namespace.STOR, objectName.ns);
        Assert.assertEquals("/", objectName.parent);
        Assert.assertEquals("somefile", objectName.name);
        Assert.assertEquals("/somefile", objectName.path);
    }

    @Test
    public void testParseObjectName_root_01() {
        final String uri = "/abbaspour/stor/";

        Optional<ObjectName> objectNameOptional = ObjectName.of(uri);

        Assert.assertTrue(objectNameOptional.isPresent());

        ObjectName objectName = objectNameOptional.get();

        Assert.assertEquals("abbaspour", objectName.owner);
        Assert.assertEquals(ObjectName.Namespace.STOR, objectName.ns);
        Assert.assertEquals("/", objectName.parent);
        Assert.assertEquals("", objectName.name);
        Assert.assertEquals("/", objectName.path);
    }

    @Test
    public void testParseObjectName_subdir_01() {
        final String uri = "/abbaspour/stor/a/b/c";

        Optional<ObjectName> objectNameOptional = ObjectName.of(uri);

        Assert.assertTrue(objectNameOptional.isPresent());

        ObjectName objectName = objectNameOptional.get();

        Assert.assertEquals("abbaspour", objectName.owner);
        Assert.assertEquals(ObjectName.Namespace.STOR, objectName.ns);
        Assert.assertEquals("/a/b", objectName.parent);
        Assert.assertEquals("c", objectName.name);
        Assert.assertEquals("/a/b/c", objectName.path);
    }

    @Test
    public void testParseObjectName_subdir_02() {
        final String uri = "/abbaspour/stor/a/b";

        Optional<ObjectName> objectNameOptional = ObjectName.of(uri);

        Assert.assertTrue(objectNameOptional.isPresent());

        ObjectName objectName = objectNameOptional.get();

        Assert.assertEquals("abbaspour", objectName.owner);
        Assert.assertEquals(ObjectName.Namespace.STOR, objectName.ns);
        Assert.assertEquals("/a", objectName.parent);
        Assert.assertEquals("b", objectName.name);
        Assert.assertEquals("/a/b", objectName.path);
    }

    @Test
    public void testParseObjectName_subdir_03() {
        final String uri = "/abbaspour/stor/a/b/";

        Optional<ObjectName> objectNameOptional = ObjectName.of(uri);

        Assert.assertTrue(objectNameOptional.isPresent());

        ObjectName objectName = objectNameOptional.get();

        Assert.assertEquals("abbaspour", objectName.owner);
        Assert.assertEquals(ObjectName.Namespace.STOR, objectName.ns);
        Assert.assertEquals("/a", objectName.parent);
        Assert.assertEquals("b", objectName.name);
        Assert.assertEquals("/a/b", objectName.path);
    }

    @Test
    public void testParseObjectName_wBookmark() {
        final String uri = "/abbaspour/stor/somefile#bookmark";

        Optional<ObjectName> objectNameOptional = ObjectName.of(uri);

        Assert.assertTrue(objectNameOptional.isPresent());

        ObjectName objectName = objectNameOptional.get();

        Assert.assertEquals("abbaspour", objectName.owner);
        Assert.assertEquals(ObjectName.Namespace.STOR, objectName.ns);
        Assert.assertEquals("/", objectName.parent);
        Assert.assertEquals("somefile", objectName.name);
        Assert.assertEquals("/somefile", objectName.path);
    }

    @Test
    public void testParseObjectName_wQuery() {
        final String uri = "/abbaspour/stor/somefile?query";

        Optional<ObjectName> objectNameOptional = ObjectName.of(uri);

        Assert.assertTrue(objectNameOptional.isPresent());

        ObjectName objectName = objectNameOptional.get();

        Assert.assertEquals("abbaspour", objectName.owner);
        Assert.assertEquals(ObjectName.Namespace.STOR, objectName.ns);
        Assert.assertEquals("/", objectName.parent);
        Assert.assertEquals("somefile", objectName.name);
        Assert.assertEquals("/somefile", objectName.path);
    }

    @Test
    public void testParseObjectName_woOwner() {
        final String uri = "/stor/somefile?query";

        Optional<ObjectName> objectNameOptional = ObjectName.of(uri);

        Assert.assertFalse(objectNameOptional.isPresent());
    }

    @Test
    public void testParseObjectName_woNS() {
        final String uri = "/abbaspour//somefile?query";

        Optional<ObjectName> objectNameOptional = ObjectName.of(uri);

        Assert.assertFalse(objectNameOptional.isPresent());
    }

    @Test
    public void testParseObjectName_malformed_01() {
        final String uri = "/abbaspour/somefile?query";

        Optional<ObjectName> objectNameOptional = ObjectName.of(uri);

        Assert.assertFalse(objectNameOptional.isPresent());
    }

    @Test
    public void testObjectName_toString_01() {
        final String uri = "/owner/stor/a/b/c?query";

        Optional<ObjectName> objectNameOptional = ObjectName.of(uri);

        Assert.assertTrue(objectNameOptional.isPresent());
        ObjectName on = objectNameOptional.get();

        Assert.assertEquals("/owner/stor/a/b/c", on.toString());
    }

    @Test
    public void testObjectName_toString_02() {
        final String uri = "/owner/jobs/a/b/c/?query";

        Optional<ObjectName> objectNameOptional = ObjectName.of(uri);

        Assert.assertTrue(objectNameOptional.isPresent());
        ObjectName on = objectNameOptional.get();

        Assert.assertEquals("/owner/jobs/a/b/c", on.toString());
    }

    @Test
    public void testObjectName_rootNS() {
        final String uri = "/owner";

        Optional<ObjectName> objectNameOptional = ObjectName.of(uri);

        Assert.assertTrue(objectNameOptional.isPresent());
        ObjectName on = objectNameOptional.get();

        Assert.assertEquals(ObjectName.Namespace.ROOT, on.ns);
        Assert.assertEquals("/", on.path);
        Assert.assertEquals("", on.name);
    }
}

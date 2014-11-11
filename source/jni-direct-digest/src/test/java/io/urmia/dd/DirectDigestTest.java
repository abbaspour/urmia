package io.urmia.dd;

import org.apache.commons.codec.binary.Base64;
import org.junit.Assert;
import org.junit.Test;

import java.nio.ByteBuffer;
import java.util.Random;

public class DirectDigestTest {

    @Test
    public void test_md5() {
        ByteBuffer bb = ByteBuffer.allocateDirect(3);
        bb.put(new byte[]{'a', 'b', 'c'});

        long ctx = DirectDigest.md5_init();
        DirectDigest.md5_update(ctx, bb);
        byte[] bytes = DirectDigest.md5_final(ctx);

        String base64 = new String(Base64.encodeBase64(bytes));

        Assert.assertEquals("kAFQmDzST7DWlj99KOF/cg==", base64);
    }

    @Test
    public void test_md5_array() {

        ByteBuffer bb = ByteBuffer.allocate(3);
        bb.put(new byte[]{'a', 'b', 'c'});

        long ctx = DirectDigest.md5_init();
        DirectDigest.md5_update(ctx, bb.array());

        byte[] md5 = DirectDigest.md5_final(ctx);

        String base64 = new String(Base64.encodeBase64(md5));
        Assert.assertEquals("kAFQmDzST7DWlj99KOF/cg==", base64);

    }

    @Test
    public void test_md5_1k() {
        Random r = new Random();

        ByteBuffer bb = ByteBuffer.allocateDirect(1024);
        byte[] b = new byte[1024];
        r.nextBytes(b);

        bb.put(b);

        long ctx = DirectDigest.md5_init();
        DirectDigest.md5_update(ctx, bb);
        byte[] md5 = DirectDigest.md5_final(ctx);

        String base64 = new String(Base64.encodeBase64(md5));

        System.out.println("base64 = " + base64);
    }

    @Test
    public void test_md5_empty() {

        ByteBuffer bb = ByteBuffer.allocateDirect(0);

        long ctx = DirectDigest.md5_init();
        DirectDigest.md5_update(ctx, bb);
        byte[] md5 = DirectDigest.md5_final(ctx);

        String base64 = new String(Base64.encodeBase64(md5));
        Assert.assertEquals("1B2M2Y8AsgTpgAmY7PhCfg==", base64);

    }

    @Test
    public void test_md5_1k_load() {
        Random r = new Random();

        for (int i = 0; i < 10; i++) {
            int size = r.nextInt(65536);

            ByteBuffer bb = ByteBuffer.allocateDirect(size);
            byte[] b = new byte[size];
            r.nextBytes(b);

            bb.put(b);

            long ctx = DirectDigest.md5_init();
            DirectDigest.md5_update(ctx, bb);
            byte[] md5 = DirectDigest.md5_final(ctx);

            String base64 = new String(Base64.encodeBase64(md5));
            System.out.println(i + "(" + size + ")\tbase64 = " + base64);
        }
    }
}

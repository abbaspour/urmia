/**
 * Copyright (c) 2013, Joyent, Inc. All rights reserved.
 */
package io.urmia.auth.joyent;

import java.io.IOException;

import io.urmia.auth.CryptoException;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.javanet.NetHttpTransport;

/**
 * @author Yunong Xiao
 */
public class HttpSignerTest {

    private static final String PRIVATE_KEY_PATH_2 = "src/test/resources/id_rsa";
//    private static final String PUBLIC_KEY_PATH_2 = "src/test/resources/id_rsa-stnd.pub";
    private static final String PUBLIC_KEY_PATH_2 = "src/test/resources/id_rsa.pub";

    private static final String PRIVATE_KEY_PATH = "src/test/resources/rsa_private.pem";
    private static final String PUBLIC_KEY_PATH = "src/test/resources/rsa_public.pem";

    private static final String KEY_FINGERPRINT = "04:92:7b:23:bc:08:4f:d7:3b:5a:38:9e:4a:17:2e:df";
    private static final String LOGIN = "yunong";

    private static final HttpRequestFactory REQUEST_FACTORY = new NetHttpTransport().createRequestFactory();

    @BeforeClass
    public static void beforeClass() throws IOException {
    }

    @Test
    public void testSignData() throws IOException, CryptoException {
        HttpSigner signer = HttpSigner.newInstance(PRIVATE_KEY_PATH, KEY_FINGERPRINT, LOGIN);
        HttpVerifier verifier = HttpVerifier.newInstance(PUBLIC_KEY_PATH);

        HttpRequest req = REQUEST_FACTORY.buildGetRequest(new GenericUrl());
        signer.signRequest(req);

        boolean verified = verifier.verifyRequest(req);
        Assert.assertTrue("unable to verify signed authorization header", verified);
    }

    @Test
    public void testSignData2() throws IOException, CryptoException {
        HttpSigner signer = HttpSigner.newInstance(PRIVATE_KEY_PATH_2, KEY_FINGERPRINT, LOGIN);
        HttpVerifier verifier = HttpVerifier.newInstance(PUBLIC_KEY_PATH_2);

        HttpRequest req = REQUEST_FACTORY.buildGetRequest(new GenericUrl());
        signer.signRequest(req);

        boolean verified =  verifier.verifyRequest(req);
        Assert.assertTrue("unable to verify signed authorization header", verified);
    }
}

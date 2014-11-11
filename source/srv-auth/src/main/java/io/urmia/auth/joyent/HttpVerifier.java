/**
 * Copyright (c) 2013, Joyent, Inc. All rights reserved.
 */
package io.urmia.auth.joyent;

import com.google.api.client.http.HttpRequest;
import io.urmia.auth.CryptoException;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.util.encoders.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.*;

/**
 * Joyent HTTP authorization signer. This adheres to the specs of the node-http-signature spec.
 *
 * @author Yunong Xiao
 */
public class HttpVerifier {

    private static final Logger LOG = LoggerFactory.getLogger(HttpVerifier.class);

    private static final String AUTHZ_SIGNING_STRING = "date: %s";
    private static final String AUTHZ_PATTERN = "signature=\"";
    static final String SIGNING_ALGORITHM = "SHA256WithRSAEncryption";

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    /**
     * Returns a new {@link io.urmia.auth.joyent.HttpVerifier} instance that can be used to sign and verify requests according to the
     * joyent-http-signature spec.
     *
     * @see <a href="http://github.com/joyent/node-http-signature/blob/master/http_signing.md">node-http-signature</a>
     * @param keyPath
     *            The path to the rsa key on disk.
     * @return An instance of {@link io.urmia.auth.joyent.HttpVerifier}.
     * @throws java.io.IOException
     *             If the key is invalid.
     */
    public static HttpVerifier newInstance(String keyPath) throws IOException {
        return new HttpVerifier(keyPath);
    }

    private final PublicKey publicKey;

    /**
     * @param keyPath
     * @throws java.io.IOException
     */
    private HttpVerifier(String keyPath) throws IOException {
        LOG.debug("initializing HttpSigner with keypath: {}", keyPath);
        publicKey = readPublicKey(keyPath);
    }

    private PublicKey readPublicKey(String publicKeyPath) throws IOException {

        FileReader fileReader = new FileReader(publicKeyPath);
        PEMParser keyReader = new PEMParser(fileReader);

        JcaPEMKeyConverter converter = new JcaPEMKeyConverter();

        Object keyPair = keyReader.readObject();

        keyReader.close();
        return converter.getPublicKey((SubjectPublicKeyInfo)keyPair);
    }

    public final boolean verifyRequest(HttpRequest request) throws CryptoException {

        LOG.debug("verifying request: " + request.getHeaders());

        String date = request.getHeaders().getDate();
        if (date == null) {
            throw new CryptoException("no date header in request");
        }

        date = String.format(AUTHZ_SIGNING_STRING, date);

        try {
            Signature verify = Signature.getInstance(SIGNING_ALGORITHM);
            verify.initVerify(publicKey);
            String authzHeader = request.getHeaders().getAuthorization();
            int startIndex = authzHeader.indexOf(AUTHZ_PATTERN);
            if (startIndex == -1) {
                throw new CryptoException("invalid authorization header " + authzHeader);
            }
            String encodedSignedDate = authzHeader.substring(startIndex + AUTHZ_PATTERN.length(),
                    authzHeader.length() - 1);
            byte[] signedDate = Base64.decode(encodedSignedDate.getBytes("UTF-8"));
            verify.update(date.getBytes("UTF-8"));
            return verify.verify(signedDate);
        } catch (NoSuchAlgorithmException e) {
            throw new CryptoException("invalid algorithm", e);
        } catch (InvalidKeyException e) {
            throw new CryptoException("invalid key", e);
        } catch (SignatureException e) {
            throw new CryptoException("invalid signature", e);
        } catch (UnsupportedEncodingException e) {
            throw new CryptoException("invalid encoding", e);
        }
    }
}

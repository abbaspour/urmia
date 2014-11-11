/**
 * Copyright (c) 2013, Joyent, Inc. All rights reserved.
 */
package io.urmia.auth.joyent;

import java.io.FileReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import io.urmia.auth.CryptoException;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMDecryptorProvider;
import org.bouncycastle.openssl.PEMEncryptedKeyPair;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.openssl.jcajce.JcePEMDecryptorProviderBuilder;
import org.bouncycastle.util.encoders.Base64;

import com.google.api.client.http.HttpRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Joyent HTTP authorization signer. This adheres to the specs of the node-http-signature spec.
 *
 * @author Yunong Xiao
 */
public class HttpSigner {

    private static final Logger LOG = LoggerFactory.getLogger(HttpSigner.class);

    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("EEE MMM d HH:mm:ss yyyy zzz");
    private static final String AUTHZ_HEADER = "Signature keyId=\"/%s/keys/%s\",algorithm=\"rsa-sha256\",signature=\"%s\"";
    private static final String AUTHZ_SIGNING_STRING = "date: %s";
    private static final String AUTHZ_PATTERN = "signature=\"";
    static final String SIGNING_ALGORITHM = "SHA256WithRSAEncryption";

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    /**
     * Returns a new {@link HttpSigner} instance that can be used to sign and verify requests according to the
     * joyent-http-signature spec.
     *
     * @see <a href="http://github.com/joyent/node-http-signature/blob/master/http_signing.md">node-http-signature</a>
     * @param keyPath
     *            The path to the rsa key on disk.
     * @param fingerPrint
     *            The fingerprint of the rsa key.
     * @param login
     *            The login of the user account.
     * @return An instance of {@link HttpSigner}.
     * @throws IOException
     *             If the key is invalid.
     */
    public static final HttpSigner newInstance(String keyPath, String fingerPrint, String login) throws IOException {
        return new HttpSigner(keyPath, fingerPrint, login);
    }

//    final KeyPair keyPair_;
    private final PrivateKey privateKey;
    private final String login_;

    private final String fingerPrint_;

    /**
     * @param keyPath
     * @throws IOException
     */
    private HttpSigner(String keyPath, String fingerprint, String login) throws IOException {
        LOG.debug(String.format("initializing HttpSigner with keypath: %s, fingerprint: %s, login: %s", keyPath,
                fingerprint, login));
        fingerPrint_ = fingerprint;
        login_ = login;
//        keyPair_ = getKeyPair(keyPath);
        privateKey = readPrivateKey(keyPath, "");
    }

    private PrivateKey readPrivateKey(String privateKeyPath, String keyPassword) throws IOException {

        FileReader fileReader = new FileReader(privateKeyPath);
        PEMParser keyReader = new PEMParser(fileReader);

        JcaPEMKeyConverter converter = new JcaPEMKeyConverter();
        PEMDecryptorProvider decryptionProv = new JcePEMDecryptorProviderBuilder().build(keyPassword.toCharArray());

        Object keyPair = keyReader.readObject();
        PrivateKeyInfo keyInfo;

        if (keyPair instanceof PEMEncryptedKeyPair) {
            PEMKeyPair decryptedKeyPair = ((PEMEncryptedKeyPair) keyPair).decryptKeyPair(decryptionProv);
            keyInfo = decryptedKeyPair.getPrivateKeyInfo();
        } else {
            keyInfo = ((PEMKeyPair) keyPair).getPrivateKeyInfo();
        }

        keyReader.close();
        return converter.getPrivateKey(keyInfo);
    }

    /**
     * @param keyPath
     * @return
     * @throws IOException
     */
/*
    private final KeyPair getKeyPair(String keyPath) throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(keyPath));
        Security.addProvider(new BouncyCastleProvider());
        PemReader pemReader = new PemReader(br);
        KeyPair kp = (KeyPair) pemReader.readObject();
        pemReader.close();
        return kp;
    }
*/

    /**
     * Sign an {@link HttpRequest}.
     *
     * @param request
     *            The {@link HttpRequest} to sign.
     * @throws CryptoException
     *             If unable to sign the request.
     */
    public final void signRequest(HttpRequest request) throws CryptoException {
        LOG.debug("signing request: " + request.getHeaders());
        String date = request.getHeaders().getDate();
        if (date == null) {
            Date now = Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTime();
            date = DATE_FORMAT.format(now);
            LOG.debug("setting date header: " + date);
            request.getHeaders().setDate(date);
        }
        try {
            Signature sig = Signature.getInstance(SIGNING_ALGORITHM);
            sig.initSign(/*keyPair_.getPrivate()*/privateKey);
            String signingString = String.format(AUTHZ_SIGNING_STRING, date);
            sig.update(signingString.getBytes("UTF-8"));
            byte[] signedDate = sig.sign();
            byte[] encodedSignedDate = Base64.encode(signedDate);
            String authzHeader = String.format(AUTHZ_HEADER, login_, fingerPrint_, new String(encodedSignedDate));
            request.getHeaders().setAuthorization(authzHeader);
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

/*
    public final boolean verifyRequest(HttpRequest request) throws CryptoException {

        LOG.debug("verifying request: " + request.getHeaders());

        String date = request.getHeaders().getDate();
        if (date == null) {
            throw new CryptoException("no date header in request");
        }

        date = String.format(AUTHZ_SIGNING_STRING, date);

        try {
            Signature verify = Signature.getInstance(SIGNING_ALGORITHM);
            verify.initVerify(keyPair_.getPublic());
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
*/
}

package io.urmia.auth.joyent;

import javax.security.cert.CertificateException;
import javax.security.cert.X509Certificate;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.security.*;

import org.bouncycastle.openssl.PEMParser;

public class BcPEMReader {
    public static void main(String[] args) throws CertificateException, IOException, NoSuchProviderException, NoSuchAlgorithmException, InvalidKeyException, SignatureException {

//        Reader streamReader = new InputStreamReader(BcPEMReader.class.getResourceAsStream("rsa_private.pem"));
        Reader rsaPrivate = new FileReader("src/test/resources/rsa_private.pem");
        Reader rsaPublic = new FileReader("src/test/resources/rsa_public.pem");

//        PEMParser reader = new PEMParser(streamReader);
        PEMParser privatePem = new PEMParser(rsaPrivate);
        PEMParser publicPem = new PEMParser(rsaPrivate);

        PublicKey publicKey;

        Object pubObject = publicPem.readObject();
        if (pubObject instanceof PublicKey) {
            publicKey = (PublicKey)pubObject;
        } else {
            System.out.println("pubObject = " + pubObject);
            return;
        }

        Object pemObject = privatePem.readObject();
        if (pemObject instanceof X509Certificate) {
            X509Certificate cert = (X509Certificate)pemObject;
            cert.checkValidity(); // to check it's valid in time
            cert.verify(publicKey); // verify the sig. using the issuer's public key
        }
    }
}

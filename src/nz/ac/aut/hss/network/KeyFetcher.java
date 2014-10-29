package nz.ac.aut.hss.network;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

public class KeyFetcher {
	private final String keyStore;
	private final String alias;

	public KeyFetcher(String keyStore, String alias) {
		this.keyStore = keyStore;
		this.alias = alias;
	}

	/**
	 * Loads the keystore. This can then be used to get the Private Key from the key Store. The keystore file e.g
	 * keystoreA for UserA (which is a jks file) should be under the project file
	 * directory for eclipse. E.g. ../workspace/Secure-Multiuser-Network-System/keystoreA
	 * @throws KeyStoreException
	 * @throws IOException
	 * @throws CertificateException
	 * @throws NoSuchAlgorithmException
	 */
	public KeyStore loadKeyStore(String keyStoreName, char[] storepw)
			throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException {
		FileInputStream fis = new FileInputStream(keyStoreName);
		KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());
		keystore.load(fis, storepw);
		fis.close();
		return keystore;
	}

	/**
	 * Method that gets user's own private key from the key store
	 * @throws NoSuchAlgorithmException
	 * @throws KeyStoreException
	 * @throws UnrecoverableKeyException
	 * @throws IOException
	 * @throws CertificateException
	 */
	public Key getPrivateKey(String keyStoreName, char[] storepw, String keyAlias, char[] keypw)
			throws UnrecoverableKeyException, KeyStoreException, NoSuchAlgorithmException, CertificateException,
			IOException {
		KeyStore keystore = loadKeyStore(keyStoreName, storepw);
		return keystore.getKey(keyAlias, keypw);
	}

	/**
	 * Gets the other user's Public Key from the certificate that we have store (currently on file system).
	 * @return
	 * @throws FileNotFoundException
	 * @throws CertificateException
	 */
	public Key getPublicKey(String certificateFileName) throws FileNotFoundException, CertificateException {
		InputStream is = new FileInputStream(certificateFileName); //byte stream of X.509 Cert
		CertificateFactory certFactory = CertificateFactory.getInstance("X509");
		X509Certificate certificate = (X509Certificate) certFactory.generateCertificate(is);
		return certificate.getPublicKey();
	}

	/**
	 * A somewhat more secure version of getPublicKey() in that it gets the public key from a trust store to prevent
	 * possible tampering of the public key, which could happen if the public certificate is store unprotected on the
	 * file system.
	 * This is because on the file system it is not password protected, and also file permissions may allow other users
	 * of
	 * a multiuser OS with write access to that file to modify it and insert a malicious (the attackers own?)public
	 * certificate.
	 * @param trustStoreName The name of the truststore to load.
	 * @return a public key
	 * @throws IOException
	 * @throws CertificateException
	 * @throws NoSuchAlgorithmException
	 * @throws KeyStoreException
	 */
	public Key getPublicKeySecure(String trustStoreName, char[] storepw, String certAlias)
			throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException {
		KeyStore truststore =
				loadKeyStore(trustStoreName, storepw);//Truststore and Keystore implementation is identical in keytool
		X509Certificate cert = (X509Certificate) truststore.getCertificate(certAlias);
		return cert.getPublicKey();
	}
}

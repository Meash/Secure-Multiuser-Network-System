package nz.ac.aut.hss.network;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import sun.misc.BASE64Encoder;
import sun.misc.BASE64Decoder;

public class KeyFetcher {

	/**
	 * @param args
	 * @throws IOException 
	 * @throws CertificateException 
	 * @throws NoSuchAlgorithmException 
	 * @throws KeyStoreException 
	 * @throws UnrecoverableKeyException 
	 */
	/*
	public static void main(String[] args) throws UnrecoverableKeyException, KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException {
		KeyFetcher keyFetch = new KeyFetcher("keystoreA", "aliasA");
		//Test getting private key from key store works
		Key key = keyFetch.getPrivateKey(keyStore, "password".toCharArray(), alias, "password".toCharArray());
		BASE64Encoder encoder = new BASE64Encoder();
		System.out.println(encoder.encode(key.getEncoded()));
		//Test getting public key from file works
		PublicKey publicKey = (PublicKey) keyFetch.getPublicKey("certA.cer");
		System.out.println(publicKey);
	} */

	private static String keyStore;
	private static String alias;
	
	public KeyFetcher(String keyStore, String alias) {
		KeyFetcher.keyStore = keyStore;
		KeyFetcher.alias = alias;
	}
	
	/**
	 * Loads the keystore. This can then be used to get the Private Key from the key Store. The keystore file e.g keystoreA for UserA (which is a jks file) should be under the project file 
	 * directory for eclipse. E.g. ../workspace/Secure-Multiuser-Network-System/keystoreA
	 * @throws KeyStoreException 
	 * @throws IOException 
	 * @throws CertificateException 
	 * @throws NoSuchAlgorithmException 
	 */
	public KeyStore loadKeyStore(String keyStoreName, char[] storepw) throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException {
		FileInputStream fis = new FileInputStream(keyStoreName);
		KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());
		keystore.load(fis,  storepw);
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
			throws UnrecoverableKeyException, KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException {
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
		X509Certificate certificate = (X509Certificate)certFactory.generateCertificate(is);
		return certificate.getPublicKey();
	}
}

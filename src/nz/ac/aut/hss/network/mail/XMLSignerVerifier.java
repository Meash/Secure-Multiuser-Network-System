package nz.ac.aut.hss.network.mail;

/**
A class that demonstrates how XML-Sig can be used to add an
enveloped signature to an XML document and then verify that the
signature is valid
Note this class needs the XML-Sig API, which is included with
version 6 of Java Standard Edition, and available as an optional
API for version 5 by placing the JAR files xmldsig.jar and 
xmlsec.jar in the classpath
@author Andrew Ensor
*/
import java.io.FileNotFoundException;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.PublicKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.Collections;
import java.util.List;
import java.util.Scanner; // Java 1.5 equivalent of cs1.Keyboard
import javax.xml.crypto.AlgorithmMethod;
import javax.xml.crypto.KeySelector;
import javax.xml.crypto.KeySelectorException;
import javax.xml.crypto.KeySelector.Purpose;
import javax.xml.crypto.KeySelectorResult;
import javax.xml.crypto.MarshalException;
import javax.xml.crypto.XMLCryptoContext;
import javax.xml.crypto.XMLStructure;
import javax.xml.crypto.dsig.CanonicalizationMethod;
import javax.xml.crypto.dsig.DigestMethod;
import javax.xml.crypto.dsig.Reference;
import javax.xml.crypto.dsig.SignatureMethod;
import javax.xml.crypto.dsig.SignedInfo;
import javax.xml.crypto.dsig.Transform;
import javax.xml.crypto.dsig.XMLSignature;
import javax.xml.crypto.dsig.XMLSignatureException;
import javax.xml.crypto.dsig.XMLSignatureFactory;
import javax.xml.crypto.dsig.dom.DOMSignContext;
import javax.xml.crypto.dsig.dom.DOMValidateContext;
import javax.xml.crypto.dsig.keyinfo.KeyInfo;
import javax.xml.crypto.dsig.keyinfo.KeyInfoFactory;
import javax.xml.crypto.dsig.keyinfo.KeyValue;
import javax.xml.crypto.dsig.spec.C14NMethodParameterSpec;
import javax.xml.crypto.dsig.spec.TransformParameterSpec;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import nz.ac.aut.hss.network.KeyFetcher;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class XMLSignerVerifier
{
private XMLSignatureFactory sigFactory;
private PrivateKey privateKey;
private PublicKey publicKey;
private DocumentBuilderFactory builderFactory;

public XMLSignerVerifier() throws UnrecoverableKeyException, KeyStoreException, CertificateException, IOException
{  // obtain an instance of an XML-Sig signature factory
   try
   {  sigFactory = XMLSignatureFactory.getInstance("DOM",
         (Provider)Class.forName
         ("org.jcp.xml.dsig.internal.dom.XMLDSigRI").newInstance());
   }
   catch (ClassNotFoundException e)
   {  System.err.println("No XML-Sig provider: " + e);
   }
   catch (InstantiationException e)
   {  System.err.println("Cannot create instance of provider: "+e);
   }
   catch (IllegalAccessException e)
   {  System.err.println("Cannot access XML-Sig provider: " + e);
   }
   // get private and public RSA keys from keystore(of sender) and truststore(of receiver) //example used DSA
   try
   {  
	  KeyFetcher keyFetch = new KeyFetcher("keystoreA", "password".toCharArray());
      privateKey = (PrivateKey) keyFetch.getPrivateKey("aliasA", "password".toCharArray());
      keyFetch = new KeyFetcher("truststoreA", "password".toCharArray());
      publicKey = (PublicKey) keyFetch.getPublicKeySecure("truststoreB", "password".toCharArray(), "aliasA"); //truststoreB should have public cert for A
   }
   catch (NoSuchAlgorithmException e)
   {  System.err.println("Encryption algorithm not available: "+e);
   }
   // create a validating DOM document builder using default parser
   builderFactory = DocumentBuilderFactory.newInstance();
   builderFactory.setNamespaceAware(true); // required for XML-Sec
}

// sign the XML document given in the input stream and put result
// in the output stream
public void sign(InputStream is, OutputStream os)
{  XMLSignature signature = null;
   try
   {  // specify the algorithms that will be used
      DigestMethod digestMethod
         = sigFactory.newDigestMethod(DigestMethod.SHA1, null);
      Transform transform = sigFactory.newTransform
         (Transform.ENVELOPED, (TransformParameterSpec)null);
      Reference ref = sigFactory.newReference("", digestMethod, 
         Collections.singletonList(transform), null, null);
      CanonicalizationMethod canMethod
         = sigFactory.newCanonicalizationMethod
         (CanonicalizationMethod.INCLUSIVE_WITH_COMMENTS,
         (C14NMethodParameterSpec)null);
      SignatureMethod sigMethod = sigFactory.newSignatureMethod
         (SignatureMethod.RSA_SHA1, null); //example was DSA_SHA1
      SignedInfo signedInfo = sigFactory.newSignedInfo(canMethod,
         sigMethod, Collections.singletonList(ref));
      // create a KeyInfo from the public key
      KeyInfoFactory kif = sigFactory.getKeyInfoFactory();
      KeyValue publicKeyValue = kif.newKeyValue(publicKey);
      KeyInfo publicKeyInfo = kif.newKeyInfo
         (Collections.singletonList(publicKeyValue));
      // create the (unsigned) signature
      signature = sigFactory.newXMLSignature
         (signedInfo, publicKeyInfo);
   }
   catch (NoSuchAlgorithmException e)
   {  System.err.println("Digest algorithm not available: " + e);
   }
   catch (InvalidAlgorithmParameterException e)
   {  System.err.println("Invalid parameters for digest: " + e);
   }
   catch (KeyException e)
   {  System.err.println("Exception with public key: " + e);
   }
   // obtain the XML document
   Document document = null;
   try
   {  DocumentBuilder builder = builderFactory.newDocumentBuilder();
      document = builder.parse(is);
   }
   catch (ParserConfigurationException e)
   {  System.err.println("DOM parser exception: " + e);
   }
   catch (SAXException e)
   {  System.err.println("SAX exception while parsing: " + e);
   }
   catch (IOException e)
   {  System.err.println("IO exception with file: " + e);
   }
   // sign the document
   try
   {  DOMSignContext signContext = new DOMSignContext
         (privateKey, document.getDocumentElement());
      signature.sign(signContext);
   }
   catch (MarshalException e)
   {  System.err.println("Marshal exception while signing: " + e);
   }
   catch (XMLSignatureException e)
   {  System.err.println("Signature exception while signing: " + e);
   }
   // send the signed document to the output stream
   try
   {  TransformerFactory tf = TransformerFactory.newInstance();
      Transformer trans = tf.newTransformer();
      trans.transform(new DOMSource(document),new StreamResult(os));
   }
   catch (TransformerConfigurationException e)
   {  System.err.println("Unable to configure transformer: " + e);
   }
   catch (TransformerException e)
   {  System.err.println("Transformer exception: " + e);
   }
}

public boolean validate(InputStream is)
{  // obtain the XML document
   Document document = null;
   try
   {  DocumentBuilder builder = builderFactory.newDocumentBuilder();
      document = builder.parse(is);
   }
   catch (ParserConfigurationException e)
   {  System.err.println("DOM parser exception: " + e);
   }
   catch (SAXException e)
   {  System.err.println("SAX exception while parsing: " + e);
   }
   catch (IOException e)
   {  System.err.println("IO exception with file: " + e);
   }
   // check that the XML document has a Signature node
   NodeList nodeList =  document.getElementsByTagNameNS
      (XMLSignature.XMLNS, "Signature");
   if (nodeList.getLength() == 0)
      System.err.println("No Signature element");
   Node signatureNode = nodeList.item(0);
   // validate the signature using certificate's public key
   DOMValidateContext vc = new DOMValidateContext
      (new KeyValueKeySelector(), signatureNode);
   boolean validated = false;
   try
   {  XMLSignature signature = sigFactory.unmarshalXMLSignature(vc);
      validated = signature.validate(vc);
   }
   catch (MarshalException e)
   {  System.err.println("Marshal exception while signing: " + e);
   }
   catch (XMLSignatureException e)
   {  System.err.println("Signature exception while signing: " + e);
   }
   return validated;
}

public static void main(String[] args) throws UnrecoverableKeyException, KeyStoreException, CertificateException, IOException
{  XMLSignerVerifier xmlSigner = new XMLSignerVerifier();
   Scanner keyboardInput = new Scanner(System.in);
   System.out.print("Please enter name of text file to sign:");
   String inputFilename = keyboardInput.nextLine();
   System.out.print("Please enter name of output text file:");
   String outputFilename = keyboardInput.nextLine();
   try
   {  InputStream is = new FileInputStream(inputFilename);
      OutputStream os = new FileOutputStream(outputFilename);
      xmlSigner.sign(is, os);
      is.close();
      os.flush();
      os.close();
   }
   catch (IOException e)
   {  System.err.println("IO exception with file: " + e);
   }
   // give user a chance to try modifying the output file
   System.out.println("File has been signed,"
      + " press enter to continue");
   keyboardInput.nextLine();
   // now validate the signature in the output file
   try
   {  InputStream is = new FileInputStream(outputFilename);
      System.out.println("Signature validated:"
         + xmlSigner.validate(is));
      is.close();
   }
   catch (IOException e)
   {  System.err.println("IO exception with file: " + e);
   }
}

// inner class that represents a KeySelector for retrieving
// public key from KeyValue element in a certificate
// adapted from Java XML-Sig samples
private class KeyValueKeySelector extends KeySelector
{
   public KeySelectorResult select(KeyInfo keyInfo,
      KeySelector.Purpose purpose, AlgorithmMethod method,
      XMLCryptoContext context) throws KeySelectorException
   {  if (keyInfo == null)
         throw new KeySelectorException("Null KeyInfo object");
      SignatureMethod sm = (SignatureMethod) method;
      List list = keyInfo.getContent();
      for (int i = 0; i < list.size(); i++)
      {  XMLStructure xmlStructure = (XMLStructure) list.get(i);
         if (xmlStructure instanceof KeyValue)
         {  PublicKey pk = null;
            try
            {  pk = ((KeyValue)xmlStructure).getPublicKey();
            }
            catch (KeyException e) 
            {  throw new KeySelectorException(e);
            }
            // make sure algorithm is compatible with method
            String algorithmURI = sm.getAlgorithm();
            String algorithmName = pk.getAlgorithm();
            if ((algorithmName.equalsIgnoreCase("DSA") &&
               algorithmURI.equalsIgnoreCase
               (SignatureMethod.DSA_SHA1)) ||
               (algorithmName.equalsIgnoreCase("RSA") &&
               algorithmURI.equalsIgnoreCase
               (SignatureMethod.RSA_SHA1)))
               return new SimpleKeySelectorResult(pk);
         }
      }
      throw new KeySelectorException("No KeyValue element found");
   }
}

private class SimpleKeySelectorResult
   implements KeySelectorResult
{
   private PublicKey pk;
   
   public SimpleKeySelectorResult(PublicKey pk) 
   {  this.pk = pk;
   }
 
   public Key getKey() 
   {  return pk; 
   }
}
}
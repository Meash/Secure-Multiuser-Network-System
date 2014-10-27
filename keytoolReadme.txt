1. Generated RSA Key Pair using keytool for UserA and UserB:
keytool -genkeypair -storepass password -alias aliasA -keyalg RSA -keystore keystoreA
keytool -genkeypair -storepass password -alias aliasB -keyalg RSA -keystore keystoreB

2.Exported the public certificate for UserA and UserB from the keystore:
keytool -export -storepass password -alias aliasA -file certA.cer -keystore keystoreA
keytool -export -storepass password -alias aliasB -file certB.cer -keystore keystoreB

3. Private key is stored in a self signed certificate in user's keystore. certA.cer is the publc key certificate.

4. Will manually load the public certificate for the other user for now and look into exchanging of this later if needed.
Edit - Andrew advised it it fine to just install them manually so no exchange required.

5. Added method getPublicKeySecure() to use a trust store to store public certificate rather than just retrieve from file system. 
Following keytool commands used to do this:
keytool -import -storepass password -alias aliasB -file certB.cer -keystore truststoreA
keytool -import -storepass password -alias aliasA -file certA.cer -keystore truststoreB



1. Compile source files if any code changes via javac, and rebuild .jars if necessary.
javac -classpath $CLASSPATH:src/:lib/mail-1.4.7.jar LoginClient.java 

2. To build the 2 .jar files:
     jar -cvf Simple.jar simplejaasmodule/SimpleLoginModule.class simplejaasmodule/SimplePrincipal.class
   and:
     jar -cvf JAASExample.jar LoginClient.class LoginClientCallbackHandler.class KeyFetcherTest.class 

3. To Log in to the application and have it run(it currently runs KeyFetcherTest class):To run the program(Security manager and policy file set in KeyFetcher class currently so no need to specify via CLI): 
 java -classpath src/:JAASExample.jar:Simple.jar:lib/mail-1.4.7.jar -Djava.security.auth.login.config=smns.config LoginClient
See the class SimpleLoginModule for valid user name and password. Modify classpath seperator : to ; for windows.

4. If any security manager exceptions, add to smns.policy and rerun 3. and check they no longer occur.

3. The following have been renamed from the JAAS Example Code given by Andrew:
JAASExampleClient renamed to LoginClient
JAASExampleCallbackHandler to LoginClientCallbackHandler
Configuration and Policy files renamed to "smns.config" and "smns.policy".  (smns = Secure Multiuser Network System)

4. Most files are in the default package and in root project directory, since I have had trouble getting JAAS to work in different locations so far.



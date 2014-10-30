/**
   A class that represents a simple JAAS LoginModule which performs
   the authentication of a Subject via a specified CallbackHandler
   and assigns a SimplePrincipal to the Subject if the authentication
   is committed
   @author Andrew Ensor
*/
package simplejaasmodule;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;

public class SimpleLoginModule implements LoginModule
{
   // subject being authenticated
   private Subject subject;
   // handles communication with user in application-specific way
   private CallbackHandler callbackHandler;
   //credential sharing across multiple LoginModule for single sign on
   private Map<String,?> sharedState;
   // configuration options for this LoginModule (allows debug=true)
   private Map<String,?> options;
   private boolean debugEnabled;
   private SimplePrincipal principal;
   private boolean committed = false;
   
   public SimpleLoginModule()
   {  subject = null;
      callbackHandler = null;
      sharedState = null;
      options = null;
      debugEnabled = false;
      principal = null;
      committed = false;
   }
   
   public void initialize(Subject subject,
      CallbackHandler callbackHandler, Map<String,?> sharedState,
      Map<String,?> options)
   {  this.subject = subject;
      this.callbackHandler = callbackHandler;
      this.sharedState = sharedState;
      this.options = options;
      if (options.get("debug").toString().equalsIgnoreCase("true"))
         debugEnabled = true;
   }
   
   public boolean login() throws LoginException
   {  principal = null;
      committed = false;
      if (callbackHandler == null)
         throw new LoginException("No callback handler provided");
      // prepare callbacks to pass to callbackHandler
      NameCallback nameCallback = new NameCallback("user name:");
      PasswordCallback passwordCallback
         = new PasswordCallback("password:", false);
      Callback[] callbacks = {nameCallback, passwordCallback};
      try
      {  callbackHandler.handle(callbacks);
      }
      catch (IOException e)
      {  throw new LoginException(e.toString());
      }
      catch (UnsupportedCallbackException e)
      {  throw new LoginException(e.toString());
      }
      // extract the credentials obtained by callbackHandler
      String userName = nameCallback.getName();
      char[] tempPassword = passwordCallback.getPassword();
      if (tempPassword == null)
         tempPassword = new char[0];
      // copy the password to another array before it gets cleared
      char[] password = new char[tempPassword.length];
      System.arraycopy(tempPassword, 0, password, 0,
         tempPassword.length);
      passwordCallback.clearPassword();
      // output debugging information
      if (debugEnabled)
      {  System.out.print("User entered:" + userName +
            " with password:");
         for (int i=0; i<password.length; i++)
            System.out.print(password[i]);
         System.out.println();
      }
      if (validate(userName, password))
      {  principal = new SimplePrincipal(userName);
         return true;
      }
      else
      {  // don't wait for garbage collection to clear the password
         Arrays.fill(password, ' ');
         return false;
      }
   }
   
   // helper method that tries to validate the user name and password
   // note in practice this would be checked in a secure database
   private boolean validate(String name, char[] password)
   {  String validName = "Jack", validName2 = "Jill";
      char[] validPassword = {'c','h','a','n','g','e','i','t'}, validPassword2 = {'p','a','s','s'};
      boolean validated = false;
      //Jack
      if(name.equals(validName)) {
	      validated =  password.length==validPassword.length;
	      if (validated)
	      {  for (int i=0; i<password.length; i++)
	            if (password[i] != validPassword[i])
	               validated = false;
	      }
      }
      //Jill
      if(name.equals(validName2)) {
	      validated = password.length==validPassword2.length;
	      if (validated)
	      {  for (int i=0; i<password.length; i++)
	            if (password[i] != validPassword2[i])
	               validated = false;
	      }
      }
      // output debugging information
      if (debugEnabled)
         System.out.println("Authenticated user:" + validated);
      return validated;
   }
   
   public boolean abort() throws LoginException
   {  if (principal == null)
         // own login failed so abort login is not performed
         return false;
      if (committed)
         // all LoginModule login succeeded but one's commit failed
         logout();
      else  // another LoginModule login failed
         principal = null;
      return true; // abort was successful
   }
   
   public boolean commit() throws LoginException
   {  if (principal == null)
         // own login failed so can not commit login
         return false;
      if (!subject.getPrincipals().contains(principal))
      {  subject.getPrincipals().add(principal);
         if (debugEnabled)
            System.out.println("SimplePrincipal added to subject");
      }
      principal = null;
      committed = true;
      return true;
   }
   
   public boolean logout() throws LoginException
   {  subject.getPrincipals().remove(principal);
      principal = null;
      committed = false;
      return true;
   }
}

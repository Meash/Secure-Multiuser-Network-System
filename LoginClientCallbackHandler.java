/**
   A class that represents a simple JAAS CallbackHandler that just
   uses System.in and System.out to communicate with the user
   Note a more elaborate CallbackHandler might eg use a GUI
   @see LoginClient.java
*/
import java.io.InputStream;
import java.io.IOException;
import java.io.PushbackInputStream;
import java.util.Arrays;
import java.util.Scanner; // Java 1.5 equivalent of cs1.Keyboard
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.TextOutputCallback;
import javax.security.auth.callback.UnsupportedCallbackException;

public class LoginClientCallbackHandler implements CallbackHandler
{
   private Scanner keyboardInput;
   
   public LoginClientCallbackHandler()
   {  keyboardInput = new Scanner(System.in);
   }
   
   public void handle(Callback[] callbacks)
      throws IOException, UnsupportedCallbackException
   {  if (callbacks == null)
         return; // no callbacks to handle
      for (int i=0; i<callbacks.length; i++)
      {  if (callbacks[i] instanceof TextOutputCallback)
         {  TextOutputCallback callback
               = (TextOutputCallback)callbacks[i];
            switch (callback.getMessageType())
            {  case TextOutputCallback.INFORMATION:
                  System.out.println(callback.getMessage());
                  break;
               case TextOutputCallback.ERROR:
                  System.out.println("ERROR:"+callback.getMessage());
                  break;
               case TextOutputCallback.WARNING:
                  System.out.println("WARNING:"+callback.getMessage());
                  break;
               default:
                  throw new IOException("Unsupported message type: "
                     + callback.getMessageType());
            }
         }
         else if (callbacks[i] instanceof NameCallback)
         {  NameCallback callback = (NameCallback)callbacks[i];
            // prompt user for name and get name using Scanner
            System.out.print(callback.getPrompt());
            callback.setName(keyboardInput.nextLine());
         }
         else if (callbacks[i] instanceof PasswordCallback)
         {  PasswordCallback callback=(PasswordCallback)callbacks[i];
            // prompt user for password and get using util method
            System.out.print(callback.getPrompt());
            callback.setPassword(readPassword(System.in));
         }
         else
            throw new UnsupportedCallbackException(callbacks[i],
               "Unsupported callback");
      }
   }
   
   // utility method that obtains a password from an InputStream
   // without storing the password in any String
   private char[] readPassword(InputStream is) throws IOException
   {  final int INITIAL_CAPACITY = 128;
      char[] buffer = new char[INITIAL_CAPACITY];
      int numChars = 0; // number of characters currently in buffer
      boolean done = false;
      while (!done)
      {  int ch = is.read();
         switch (ch)
         {  case -1:
            case '\n':
               done = true;
               break;
            case '\r':
               int nextChar = is.read();
               if ((nextChar != '\n') && (nextChar != -1))
               {  if (!(is instanceof PushbackInputStream))
                     is = new PushbackInputStream(is);
                  ((PushbackInputStream)is).unread(nextChar);
               }
               else
                  done = true;
               break;
            default:
               if (numChars >= buffer.length)
               {  // buffer full so allocate a new larger buffer
                  char[] newBuffer = new char[buffer.length*2];
                  System.arraycopy(buffer, 0, newBuffer, 0, numChars);
                  Arrays.fill(buffer, ' '); // clear old buffer
                  buffer = newBuffer;
               }
               buffer[numChars++] = (char)ch;
               break;
         }
      }
      if (numChars == 0)
         return null;
      // copy used portion of buffer to new buffer that gets returned
      char[] returnBuffer = new char[numChars];
      System.arraycopy(buffer, 0, returnBuffer, 0, numChars);
      Arrays.fill(buffer, ' '); // clear buffer
      return returnBuffer;
   }
}

/**
   A class that represents a simple JAAS Principal which provides a
   String name as an identity for a Subject
   @see SimpleLoginModule.java
*/
package simplejaasmodule;

import java.security.Principal;

public class SimplePrincipal implements Principal
{
   private String name;
   
   public SimplePrincipal(String name)
   {  if (name == null)
         throw new IllegalArgumentException("No name for subject");
      this.name = name;
   }
   
   public boolean equals(Object another)
   {  if (another==null || !(another instanceof SimplePrincipal))
         return false;
      else
      {  SimplePrincipal principal = (SimplePrincipal)another;
         return name.equals(principal.getName());
      }
   }
   
   public String getName()
   {  return name;
   }
   
   public int hashCode()
   {  return name.hashCode();
   }
   
   public String toString()
   {  return "SimplePrincipal with name " + name;
   }
}

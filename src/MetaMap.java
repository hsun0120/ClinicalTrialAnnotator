import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;

import gov.nih.nlm.nls.skr.GenericObject;

/**
 * 
 * @author Haoran Sun
 *
 */
public class MetaMap {
  private GenericObject submission; //Web API submission
  private String email;
  
  public MetaMap(String username, String password, String email) {
    this.submission = new GenericObject(Integer.MAX_VALUE, username, password);
    this.email = email;
  }
  
  public String config(String source, String version, String incSrc, String
      excSrc, String incType, String excType) {
    if(!this.syntaxCheck(incSrc, excSrc) || !this.syntaxCheck(incType,
        excType))
      return null;
    
    StringBuilder sb = new StringBuilder();
    if(source != null)
      sb.append("-Z " + source + " ");
    if(version != null)
      sb.append("-V " + version + " ");
    if(incSrc != null)
      sb.append("-R " + incSrc + " ");
    if(excSrc != null)
      sb.append("-e " + excSrc + " ");
    if(incType != null)
      sb.append("-J " + incType + " ");
    if(excType != null)
      sb.append("-k " + excType + " ");
    return sb.toString();
  }
  
  public String getResults(String filename, String args) {
    StringBuilder sb = new StringBuilder();
    try(BufferedReader reader = new BufferedReader(new InputStreamReader(new
        FileInputStream(filename)))) {
      String line = null;
      while((line = reader.readLine()) != null) {
        sb.append(line);
      }
    } catch (FileNotFoundException e) {
      e.printStackTrace();
      return null;
    } catch (IOException e) {
      e.printStackTrace();
      return null;
    }
    
    this.submission.setField("Email_Address", email);
    this.submission.setField("APIText", sb.toString());
    this.submission.setField("KSOURCE", "1516");
    String arguments = "--JSONf 2 ";
    if(args != null)
      arguments += args;
    this.submission.setField("COMMAND_ARGS", arguments);
    return this.submission.handleSubmission();
  }
  
  private boolean syntaxCheck(String inclusion, String exclusion) {
    if(inclusion == null || exclusion == null)
      return true;
    String[] inc = inclusion.split(",");
    /* When the list only contains one source */
    if(inc.length == 0) {
      inc = new String[1];
      inc[0] = inclusion;
    }
    String[] exc = exclusion.split(",");
    if(exc.length == 0) {
      exc = new String[1];
      exc[0] = exclusion;
    }
    
    for(int i = 0; i < inc.length; i++)
      for(int j = 0; j < exc.length; j++)
        if(inc[i].equals(exc[j]))
          return false;
    return true;
  }
  public static void main(String[] args) {
    MetaMap req = new MetaMap(args[0], args[1], args[2]);
    String opts = req.config("2017AA", "USAbase", null, null, null, null);
    try
    {
       String results = req.getResults(args[3], opts);
       System.out.print(results);

    } catch (RuntimeException ex) {
       System.err.println("");
       System.err.print("An ERROR has occurred while processing your");
       System.err.println(" request, please review any");
       System.err.print("lines beginning with \"Error:\" above and the");
       System.err.println(" trace below for indications of");
       System.err.println("what may have gone wrong.");
       System.err.println("");
       System.err.println("Trace:");
       ex.printStackTrace();
    }

  }
}
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * A class that build ontology from a ULMS xml file.
 * @author Haoran Sun
 * @since 2017-11-22
 */
public class OntologyBuilder {
  public static void build(String sourceName, String outputName) {
    String XML = OntologyBuilder.loadXML(sourceName);
    
  }
  
  private static String loadXML(String filename) {
    StringBuilder sb = new StringBuilder();
    try(BufferedReader reader = new BufferedReader(new InputStreamReader(new
        FileInputStream(filename)))) {
      String line = null;
      while((line = reader.readLine()) != null)
        sb.append(line);
      return sb.toString();
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }
    return null;
  }
}
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.XML;

/**
 * A class that build ontology from a ULMS xml file.
 * @author Haoran Sun
 * @since 2017-11-22
 */
public class OntologyBuilder {
  public static void build(String sourceName, String outputName) throws
  JSONException, IOException {
    String xmlStr = OntologyBuilder.loadXML(sourceName);
    JSONObject xmlJSONObj = XML.toJSONObject(xmlStr);
    try(BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new 
        FileOutputStream(outputName)))) {
       writer.write(xmlJSONObj.toString());
    }
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
  
  public static void main(String args[]) {
    try {
      OntologyBuilder.build(args[0], args[1]);
    } catch (JSONException | IOException e) {
      e.printStackTrace();
    }
  }
}
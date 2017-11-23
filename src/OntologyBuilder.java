import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.XML;

/**
 * A class that build ontology from a ULMS xml file.
 * @author Haoran Sun
 * @since 2017-11-22
 */
public class OntologyBuilder {
  static final int MAX_LENGTH = 10000;
  
  private MetaMap request;
  
  public OntologyBuilder(String username, String password, String email) {
    this.request = new MetaMap(username, password, email);
  }
  
  public void build(String sourceName, String outputName) throws
  JSONException, IOException {
    String xmlStr = this.loadXML(sourceName).replaceAll("\\s+"," ");
    JSONObject xmlJSONObj = XML.toJSONObject(xmlStr);
    try(BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new 
        FileOutputStream(outputName)))) {
       writer.write(xmlJSONObj.toString());
    }
    /* Replace textblocks and descriptions with MetaMap output */
    this.DFS(xmlJSONObj);
  }
  
  private String loadXML(String filename) {
    StringBuilder sb = new StringBuilder();
    try(BufferedReader reader = new BufferedReader(new InputStreamReader(new
        FileInputStream(filename), StandardCharsets.UTF_8))) {
      String line = null;
      while((line = reader.readLine()) != null) {
        line = line.replace("¨Q", " less than or euqal to ");
        line = line.replace("¨R", " greater than or euqal to ");
        sb.append(line);
      }
      return sb.toString();
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }
    return null;
  }
  
  private void DFS(JSONObject jsonObj) throws JSONException {
    Iterator<?> it = jsonObj.keys();
    while(it.hasNext()) {
      String key = (String) it.next();
      if(key.equals("criteria")) {
        JSONObject criteria = jsonObj.getJSONObject(key);
        String textblock = criteria.getString("textblock");
        int sepIndex = textblock.indexOf("Exclusion Criteria");
        String inclCri = textblock.substring(0, sepIndex);
        criteria.remove("textblock");
        String opts = this.request.config("2017AA", "USAbase", null, null,
            null, null);
        String results = null;
        if(inclCri.length() > MAX_LENGTH) {
          
        } else
          results = this.request.getResults(inclCri, opts, false);
        if(results == null) throw new JSONException("Fail to get MetaMap response");
        
        criteria.put("Inclusion Criteria", new JSONArray(results));
        
        String exclCri = textblock.substring(sepIndex);
        results = null;
        if(exclCri.length() > MAX_LENGTH) {
          
        } else
          results = this.request.getResults(exclCri, opts, false);
        if(results == null) throw new JSONException("Fail to get MetaMap response");
        
        criteria.put("Exclusion Criteria", new JSONArray(results));
      } else if(jsonObj.get(key) instanceof JSONObject)
        this.DFS(jsonObj.getJSONObject(key));
      else
        this.DFS(jsonObj.getJSONArray(key));
    }
  }
  
  private void DFS(JSONArray jsonArray) throws JSONException {
    for(int i = 0; i < jsonArray.length(); i++) {
      if(jsonArray.get(i) instanceof JSONObject)
        this.DFS(jsonArray.getJSONObject(i));
      else return;
    }
  }
  
  public static void main(String args[]) {
    OntologyBuilder builder = new OntologyBuilder(args[0], args[1], args[2]);
    try {
      builder.build(args[3], args[4]);
    } catch (JSONException | IOException e) {
      e.printStackTrace();
    }
  }
}
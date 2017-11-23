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
  private String opts;
  
  public OntologyBuilder(String username, String password, String email) {
    this.request = new MetaMap(username, password, email);
    this.opts = this.request.config("2017AA", "USAbase", null, null, null,
        null);
  }
  
  public void build(String sourceName, String outputName) throws
  JSONException, IOException {
    String xmlStr = this.loadXML(sourceName);
    JSONObject xmlJSONObj = XML.toJSONObject(xmlStr);
    /* Replace textblocks and descriptions with MetaMap output */
    this.DFS(xmlJSONObj);
    try(BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new 
        FileOutputStream(outputName)))) {
       writer.write(xmlJSONObj.toString());
    }
  }
  
  private String loadXML(String filename) {
    StringBuilder sb = new StringBuilder();
    try(BufferedReader reader = new BufferedReader(new InputStreamReader(new
        FileInputStream(filename), StandardCharsets.UTF_8))) {
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
  
  private void DFS(JSONObject jsonObj) throws JSONException {
    Iterator<?> it = jsonObj.keys();
    while(it.hasNext()) {
      String key = (String) it.next();
      if(key.equals("criteria")) {
        JSONObject criteria = jsonObj.getJSONObject(key);
        String textblock = criteria.getString("textblock");
        /* Remove bullet points */
        textblock = textblock.replaceAll("\\s+[1-9]\\.", "");
        textblock = textblock.replaceAll("\\s+", " ");
        textblock = textblock.replace("¨Q", "<=");
        textblock = textblock.replace("¡Ü", "<=");
        textblock = textblock.replace("¨R", ">=");
        textblock = textblock.replace("¡Ý", ">=");
        int sepIndex = textblock.indexOf("Exclusion Criteria");
        if(sepIndex == -1) {
          this.DFS(criteria);
          continue;
        }
        String inclCri = textblock.substring(textblock.indexOf(':') + 2, 
            sepIndex);
        criteria.remove("textblock");
        String results = null;
        if(inclCri.length() > MAX_LENGTH) {
          results = this.overLimitRequest(inclCri);
        } else
          results = this.request.getResults(inclCri, this.opts, false);
        if(results == null) throw new JSONException("Fail to get MetaMap"
            + " response");
        
        criteria.put("Inclusion Criteria", new JSONArray(results));
        
        String exclCri = textblock.substring(sepIndex);
        exclCri = exclCri.substring(exclCri.indexOf(':') + 2);
        results = null;
        if(exclCri.length() > MAX_LENGTH) {
          results = this.overLimitRequest(exclCri);
        } else
          results = this.request.getResults(exclCri, this.opts, false);
        if(results == null) throw new JSONException("Fail to get MetaMap "
            + "response");
        
        criteria.put("Exclusion Criteria", new JSONArray(results));
      } else if (key.equals("textblock") || key.equals("description")) {
        String text = jsonObj.getString(key);
        text = text.replaceAll("\\s+[1-9]\\.", "");
        text = text.replaceAll("\\s+", " ");
        text = text.replace("¨Q", "<=");
        text = text.replace("¡Ü", "<=");
        text = text.replace("¨R", ">=");
        text = text.replace("¡Ý", ">=");
        String results = null;
        if(text.length() > MAX_LENGTH)
          results = this.overLimitRequest(text);
        else
          results = this.request.getResults(text, this.opts, false);
        if(results == null) throw new JSONException("Fail to get MetaMap"
            + " response");
        jsonObj.put(key, new JSONArray(results));
      } else if(jsonObj.get(key) instanceof JSONObject)
        this.DFS(jsonObj.getJSONObject(key));
      else if(jsonObj.get(key) instanceof JSONArray)
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
  
  private String overLimitRequest(String text) {
    StringBuilder response = new StringBuilder();
    StringBuilder sb = new StringBuilder();
    String[] sentences = text.split(". ");
    for(int i = 0; i < sentences.length; i++) {
      int offset = (i == sentences.length - 1 ? 0 : 2);
      String endChar = (offset == 0 ? "" : ". ");
      if(sb.length() + sentences[i].length() + offset <= MAX_LENGTH)
        sb.append(sentences[i] + endChar);
      else {
        String result = this.request.getResults(sb.toString(), this.opts,
            false);
        if(result == null) return null;
        sb = new StringBuilder();
        if(response.length() == 0 && result.length() > 0)
          response.append(result.substring(0, result.length()) + ",");
        else if(result.length() > 0)
          response.append(result.substring(1, result.length()) + ",");
        if(i < sentences.length - 1) sb.append(sentences[i] + ". ");
      }
      
      if(i == sentences.length - 1) {
        String lastResponse = this.request.getResults(sentences[i],
            this.opts, false);
        if(lastResponse == null) return null;
        if(lastResponse.length() > 0)
          response.append(lastResponse.substring(1));
        else
          response.append(']');
      }
    }
    return response.toString();
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
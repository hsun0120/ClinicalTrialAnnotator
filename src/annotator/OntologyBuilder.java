package annotator;

import utils.replace_UTF8;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
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
  static final int BUF_SIZE = 5000;
  
  private MetaMap request;
  private String opts;
  private JSONObject annot = new JSONObject();
  
  private boolean overLimit = false;
  
  public OntologyBuilder(String username, String password, String email) {
    this.request = new MetaMap(username, password, email);
    this.opts = this.request.config("2017AA", "USAbase", null, null, null,
        null);
  }
  
  public void build(String sourceName, String outputName) throws
  JSONException, IOException {
	  String xmlStr = this.loadXML(sourceName).replaceAll("\\s+[1-9]\\.", "");
	  xmlStr = xmlStr.replaceAll("\\s+", " ");
	  JSONObject xmlJSONObj = XML.toJSONObject(xmlStr);

	  this.DFS(xmlJSONObj, null);

	  /* Output converted json file */
	  try(BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new 
			  FileOutputStream(outputName)))) {
		  writer.write(xmlJSONObj.toString());
	  }

	  this.annot.put("nct_id", xmlJSONObj.getJSONObject("clinical_study")
	      .getJSONObject("id_info").getString("nct_id"));
	  /* Output annotated json file */
	  try(BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new 
			  FileOutputStream("annotated_" + outputName)))) {
		  writer.write(this.annot.toString());
	  }
	  
	  if(this.overLimit)
		  System.out.println(xmlJSONObj.getJSONObject("clinical_study")
	      .getJSONObject("id_info").getString("nct_id"));
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
  
  private void DFS(JSONObject jsonObj, String objKey) throws JSONException {
    Iterator<?> it = jsonObj.keys();
    while(it.hasNext()) {
      String key = (String) it.next();
      if(key.equals("brief_summary")) continue;
      if(key.equals("criteria")) {
        JSONObject criteria = jsonObj.getJSONObject(key);
        String textblock = criteria.getString("textblock");
        textblock = this.replaceIllegalChars(textblock);
        int sepIndex = textblock.indexOf("Exclusion Criteria");
        int inclIdx = textblock.indexOf("Inclusion Criteria");
        
        if(inclIdx == -1 && sepIndex == -1) {
          this.DFS(criteria, key);
          continue;
        }
        
        this.annot.put("criteria", new JSONObject());
        
        if(inclIdx != -1 && textblock.indexOf(':') + 2 < textblock.length()) {
          String inclCri = null;
          if(sepIndex == -1)
            inclCri = textblock.substring(textblock.indexOf(':') + 2).trim();
          else
            inclCri = textblock.substring(textblock.indexOf(':') + 2, sepIndex)
            .trim();
        String results = null;
        if(inclCri.length() > MAX_LENGTH) {
          results = this.overLimitRequest(inclCri);
        } else
          results = this.request.getResults(inclCri, this.opts, false);
        if(results == null) throw new JSONException("Fail to get MetaMap"
            + " response");
        
        this.annot.getJSONObject("criteria").put("Inclusion Criteria", new
            JSONArray(results));
        }
        
        if(sepIndex == -1) continue;
        
        String exclCri = textblock.substring(sepIndex);
        if(exclCri.indexOf(':') + 2 > exclCri.length()) continue;
        exclCri = exclCri.substring(exclCri.indexOf(':') + 2).trim();
        String results = null;
        if(exclCri.length() > MAX_LENGTH) {
          results = this.overLimitRequest(exclCri);
        } else
          results = this.request.getResults(exclCri, this.opts, false);
        if(results == null) throw new JSONException("Fail to get MetaMap "
            + "response");
        
        this.annot.getJSONObject("criteria").put("Exclusion Criteria", new
            JSONArray(results));
        
        /* Check other criteria (not a very good solution) */
        int otherIdx = 0;
        if(inclIdx > 0 && sepIndex == -1)
          otherIdx = inclIdx;
        else if(sepIndex > 0 && inclIdx == -1)
          otherIdx = sepIndex;
        else
          otherIdx = inclIdx < sepIndex ? inclIdx : sepIndex;
        
        if(otherIdx != 0) {
          String otherCri = textblock.substring(0, otherIdx).trim();
          String res = null;
          if(otherCri.length() > MAX_LENGTH) {
            res = this.overLimitRequest(otherCri);
          } else
            res = this.request.getResults(otherCri, this.opts, false);
          if(res == null) throw new JSONException("Fail to get MetaMap "
              + "response");
          
          this.annot.getJSONObject("criteria").put("Other Criteria", new
              JSONArray(res));
        }
      } else if (key.equals("textblock") || key.equals("description")) {
        String text = jsonObj.getString(key);
        text = this.replaceIllegalChars(text);
        String results = null;
        if(text.length() > MAX_LENGTH)
          results = this.overLimitRequest(text);
        else
          results = this.request.getResults(text, this.opts, false);
        if(results == null) throw new JSONException("Fail to get MetaMap"
            + " response");
        this.annot.put(objKey, new JSONObject());
        this.annot.getJSONObject(objKey).put(key, new JSONArray(results));
      } else if(jsonObj.get(key) instanceof JSONObject)
        this.DFS(jsonObj.getJSONObject(key), key);
      else if(jsonObj.get(key) instanceof JSONArray)
        this.DFS(jsonObj.getJSONArray(key), key);
    }
  }
  
  private void DFS(JSONArray jsonArray, String arrayKey) throws JSONException {
    for(int i = 0; i < jsonArray.length(); i++) {
      if(jsonArray.get(i) instanceof JSONObject)
        this.DFS(jsonArray.getJSONObject(i), arrayKey);
      else return;
    }
  }
  
  private String overLimitRequest(String text) {
	this.overLimit = true;
    StringBuilder response = new StringBuilder(MAX_LENGTH);
    StringBuilder sb = new StringBuilder();
    String[] sentences = text.split("(?<=\\. )");
    for(int i = 0; i < sentences.length; i++) {
      if(sb.length() + sentences[i].length() <= MAX_LENGTH)
        sb.append(sentences[i]);
      else {
        String result = this.request.getResults(sb.toString(), this.opts,
            false);
        if(result == null) return null;
        sb = new StringBuilder(BUF_SIZE);
        if(response.length() == 0 && result.length() > 0)
          response.append(result.substring(0, result.length() - 1) + ",");
        else if(result.length() > 0)
          response.append(result.substring(1, result.length() - 1) + ",");
        sb.append(sentences[i]);
      }
      
      if(i == sentences.length - 1) {
        String lastResponse = this.request.getResults(sb.toString(),
            this.opts, false);
        if(lastResponse == null) return null;
        if(lastResponse.length() > 0) {
          response.append(lastResponse.substring(1));
        }
        else
          response.append(']');
      }
    }
    return response.toString();
  }
  
  private String replaceIllegalChars (String text) {
	  try {
		  text = replace_UTF8.ReplaceLooklike(text);
	  } catch (IOException e) {
		  e.printStackTrace();
	  }
	  text = text.replace("- ", "\n ");
	  return text.trim();
  }
  
  public static void main(String args[]) {
    OntologyBuilder builder;
    try {
    	File dir = new File("xml");
    	for(final File file : dir.listFiles()) {
    		builder = new OntologyBuilder(args[0], args[1], args[2]);
    		builder.build(file.getPath(), file.getName().substring(0,
    				file.getName().indexOf('.')) + ".json");
    	}
    } catch (JSONException | IOException e) {
      e.printStackTrace();
    }
  }
}
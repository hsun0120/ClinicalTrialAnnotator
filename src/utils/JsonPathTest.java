package utils;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.Iterator;


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;

public class JsonPathTest {

  public String readFile(String filename) {
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
    return sb.toString();
  }

  public void DFS(JSONObject jsonObj, String objKey) throws JSONException {
    Iterator<?> it = jsonObj.keys();
    while(it.hasNext()) {
      String key = (String) it.next();
      if (key.equals("MappingScore") || key.equals("CandidateScore") || key.equals("PhraseLength")){
        int intVal = ((Integer)JSONObject.stringToValue(jsonObj.getString(key))).intValue();
        jsonObj.put(key, intVal);
      }
      else if(jsonObj.get(key) instanceof JSONObject)
        this.DFS(jsonObj.getJSONObject(key), key);
      else if(jsonObj.get(key) instanceof JSONArray)
        this.DFS(jsonObj.getJSONArray(key), key);  
    }
  }

  private void DFS(JSONArray jsonArray, String arrayKey) throws JSONException {
    for(int i = 0; i < jsonArray.length(); i++) {
      if(jsonArray.get(i) instanceof JSONObject)
        this.DFS(jsonArray.getJSONObject(i), arrayKey);
      else if(jsonArray.get(i) instanceof JSONArray)
        this.DFS(jsonArray.getJSONArray(i), arrayKey);
    }
  }
  
  public String filter(JSONArray jsonArr, int threshold) throws JSONException {
    JSONArray ret = new JSONArray();
    for(int index = 0; index < jsonArr.length(); index++) {
      JSONArray arr = jsonArr.getJSONArray(index);
      for(int i = 0; i < arr.length(); i++) {
        JSONObject phrase = arr.getJSONObject(i);
        if(phrase.getInt("PhraseLength") <= 2) continue;
        JSONArray mappings = phrase.getJSONArray("Mappings");
        for(int j = 0; j < mappings.length(); j++) {
          JSONObject mapping = mappings.getJSONObject(j);
          if(mapping.getInt("MappingScore") < threshold) {
            ret.put(phrase.getString("PhraseText"));
            break;
          }
          boolean matched = false;
          JSONArray candidates = mapping.getJSONArray("MappingCandidates");
          for(int k = 0; k < candidates.length(); k++) {
            if(candidates.getJSONObject(k).getInt("CandidateScore") < threshold) {
              matched = true;
              break;
            }
          }
          if(matched) {
            ret.put(phrase.getString("PhraseText"));
            break;
          }
        }
      }
    }
    return ret.toString();
  }

  public static void main(String[] args) throws JSONException {
    JsonPathTest test = new JsonPathTest();
    String json = test.readFile(args[0]);
    JSONObject jsonObj = new JSONObject(json);
    test.DFS(jsonObj, null);
    Object document = Configuration.defaultConfiguration().jsonProvider().parse(jsonObj.toString());
    String res = JsonPath.read(document, "$..Phrases").toString();
    try(BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new 
        FileOutputStream("test.json")))) {
      writer.write(res);
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }
    JSONArray jarr = new JSONArray(res);
    try(BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new 
        FileOutputStream("phrases_" + args[0].substring(10))))) {
      writer.write(test.filter(jarr, -800));
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
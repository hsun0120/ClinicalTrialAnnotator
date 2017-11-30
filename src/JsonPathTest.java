import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.Iterator;

import javax.xml.xpath.XPath;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.XML;

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
	      if(jsonObj.get(key) instanceof JSONObject)
	        this.DFS(jsonObj.getJSONObject(key), key);
	      else if(jsonObj.get(key) instanceof JSONArray)
	        this.DFS(jsonObj.getJSONArray(key), key);
	      else if (key.equals("MappingScore")){
	    	  int intVal = ((Integer)JSONObject.stringToValue(jsonObj.getString(key))).intValue();
	    	  jsonObj.put(key, intVal);
	      }
	    }
	  }
	  
	  private void DFS(JSONArray jsonArray, String arrayKey) throws JSONException {
	    for(int i = 0; i < jsonArray.length(); i++) {
	      if(jsonArray.get(i) instanceof JSONObject)
	        this.DFS(jsonArray.getJSONObject(i), arrayKey);
	      else return;
	    }
	  }
	
	public static void main(String[] args) {
		JsonPathTest test = new JsonPathTest();
		String json = test.readFile(args[0]);
		JSONObject jsonObj = new JSONObject(json);
		test.DFS(jsonObj, null);
		try(BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new 
				FileOutputStream("test.json")))) {
			writer.write(jsonObj.toString());
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		Object document = Configuration.defaultConfiguration().jsonProvider().parse(jsonObj.toString());
		String phrases = JsonPath.read(document, "$..Phrases[*].Mappings").toString();
		
		System.out.println(phrases);
	}
}
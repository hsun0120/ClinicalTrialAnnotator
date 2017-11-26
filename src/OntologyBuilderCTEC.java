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

public class OntologyBuilderCTEC {
  static final String CETC_PATH = "/home/haoran/EliIE-master";
  static final String XML_PATH = "/home/haoran/EliIE-master"
      + "/temp_Parsed.xml";
  static final String NER_PATH = "/home/haoran/EliIE-master"
      + "/temp_NER.xml";
  static final String TEMP_PATH = "/home/haoran/EliIE-master/temp.txt";
  static final String NAME_ENTITY_REC = "python ./NamedEntityRecognition.py"
      + " . temp.txt .";
  static final String RELATION = "python ./Relation.py . temp.txt";
  private JSONObject annot = new JSONObject();
  
  public void build(String sourceName, String outputName) throws
  JSONException, IOException, InterruptedException {
    String xmlStr = this.loadXML(sourceName).replaceAll("\\s+[1-9]\\.", "");
    xmlStr = xmlStr.replaceAll("\\s+", " ");
    JSONObject xmlJSONObj = XML.toJSONObject(xmlStr);

    this.DFS(xmlJSONObj, null);
    try(BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new 
        FileOutputStream(outputName)))) {
       writer.write(xmlJSONObj.toString());
    }
    try(BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new 
        FileOutputStream("annotated_" + outputName)))) {
       writer.write(this.annot.toString());
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
  
  private void writeFile(String content) {
    try(BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new 
        FileOutputStream("/home/haoran/EliIE-master/temp.txt")))) {
       writer.write(content);
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
  
  private void DFS(JSONObject jsonObj, String objKey) throws JSONException,
  IOException, InterruptedException {
    Iterator<?> it = jsonObj.keys();
    while(it.hasNext()) {
      String key = (String) it.next();
      if(key.equals("brief_summary")) continue;
      if(key.equals("criteria")) {
        JSONObject criteria = jsonObj.getJSONObject(key);
        String textblock = criteria.getString("textblock");
        textblock = textblock.replace("≦", "<=");
        textblock = textblock.replace("≤", "<=");
        textblock = textblock.replace("≧", ">=");
        textblock = textblock.replace("≥", ">=");
        textblock = textblock.replace("®", "(R)");
        textblock = textblock.replace(". ", ".\n");
        textblock = textblock.replace("; ", ";\n");
        int sepIndex = textblock.indexOf("Exclusion Criteria");
        if(sepIndex == -1) {
          this.DFS(criteria, key);
          continue;
        }
        String inclCri = textblock.substring(textblock.indexOf(':') + 2, 
            sepIndex);
        this.writeFile(inclCri);
        Process CTEC = Runtime.getRuntime().exec(NAME_ENTITY_REC, null,
            new File(CETC_PATH));
        CTEC.waitFor();
        CTEC = Runtime.getRuntime().exec(RELATION, null,
            new File(CETC_PATH));
        CTEC.waitFor();
        String results = XML.toJSONObject(this.loadXML(XML_PATH)).toString();
        File parsed = new File(XML_PATH);
        parsed.delete();
        File ner = new File(NER_PATH);
        ner.delete();
        File temp = new File(TEMP_PATH);
        temp.delete();
        if(results == null) throw new JSONException("Fail to get MetaMap"
            + " response");
        
        this.annot.put("criteria", new JSONObject());
        this.annot.getJSONObject("criteria").put("Inclusion Criteria", new
            JSONObject(results));
        
        String exclCri = textblock.substring(sepIndex);
        exclCri = exclCri.substring(exclCri.indexOf(':') + 2);
        this.writeFile(exclCri);
        CTEC = Runtime.getRuntime().exec(NAME_ENTITY_REC, null,
            new File(CETC_PATH));
        CTEC.waitFor();
        CTEC = Runtime.getRuntime().exec(RELATION, null,
            new File(CETC_PATH));
        CTEC.waitFor();
        results = XML.toJSONObject(this.loadXML(XML_PATH)).toString();
        parsed = new File(XML_PATH);
        parsed.delete();
        ner = new File(NER_PATH);
        ner.delete();
        temp = new File(TEMP_PATH);
        temp.delete();
        
        if(results == null) throw new JSONException("Fail to get MetaMap "
            + "response");
        
        this.annot.getJSONObject("criteria").put("Exclusion Criteria", new
            JSONObject(results));
      } else if (key.equals("textblock") || key.equals("description")) {
        String text = jsonObj.getString(key);
        text = text.replace("≦", "<=");
        text = text.replace("≤", "<=");
        text = text.replace("≧", ">=");
        text = text.replace("≥", ">=");
        text = text.replace("®", "(R)");
        text = text.replace(". ", ".\n");
        text = text.replace("; ", ";\n");
        this.writeFile(text);
        Process CTEC = Runtime.getRuntime().exec(NAME_ENTITY_REC, null,
            new File(CETC_PATH));
        CTEC.waitFor();
        CTEC = Runtime.getRuntime().exec(RELATION, null,
            new File(CETC_PATH));
        CTEC.waitFor();
        String results = XML.toJSONObject(this.loadXML(XML_PATH)).toString();
        File parsed = new File(XML_PATH);
        parsed.delete();
        File ner = new File(NER_PATH);
        ner.delete();
        File temp = new File(TEMP_PATH);
        temp.delete();
        
        if(results == null) throw new JSONException("Fail to get MetaMap"
            + " response");
        this.annot.put(objKey, new JSONObject());
        this.annot.getJSONObject(objKey).put(key, new JSONObject(results));
      } else if(jsonObj.get(key) instanceof JSONObject)
        this.DFS(jsonObj.getJSONObject(key), key);
      else if(jsonObj.get(key) instanceof JSONArray)
        this.DFS(jsonObj.getJSONArray(key), key);
    }
  }
  
  private void DFS(JSONArray jsonArray, String arrayKey) throws JSONException, IOException, InterruptedException {
    for(int i = 0; i < jsonArray.length(); i++) {
      if(jsonArray.get(i) instanceof JSONObject)
        this.DFS(jsonArray.getJSONObject(i), arrayKey);
      else return;
    }
  }
  
  public static void main(String[] args) {
    OntologyBuilderCTEC builder = new OntologyBuilderCTEC();
    try {
      builder.build(args[0], args[1]);
    } catch (JSONException | IOException | InterruptedException e) {
      e.printStackTrace();
    }
  }
}
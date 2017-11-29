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
import java.util.List;
import java.util.Properties;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.XML;

import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.JSONOutputter;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TreeCoreAnnotations.TreeAnnotation;
import edu.stanford.nlp.util.CoreMap;

public class GraphBuilder {
  private StanfordCoreNLP pipeline;
  
  public GraphBuilder() {
    Properties props = new Properties();
    props.setProperty("annotators", "tokenize, ssplit, pos, lemma, ner, parse");
    this.pipeline = new StanfordCoreNLP(props);
  }
  
  public void build(String sourceName, String outputName) throws
  JSONException, IOException {
    String xmlStr = this.loadXML(sourceName).replaceAll("\\s+[1-9]\\.", "");
    xmlStr = xmlStr.replaceAll("\\s+", " ");
    JSONObject xmlJSONObj = XML.toJSONObject(xmlStr);
    JSONObject annot = new JSONObject();

    this.DFS(xmlJSONObj, null, annot);
    try(BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new 
        FileOutputStream(outputName)))) {
       writer.write(xmlJSONObj.toString());
    }
    try(BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new 
        FileOutputStream("annotated_" + outputName)))) {
       writer.write(annot.toString());
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
  
  private void DFS(JSONObject jsonObj, String objKey, JSONObject annot) throws
  JSONException, IOException {
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
        int sepIndex = textblock.indexOf("Exclusion Criteria");
        if(sepIndex == -1) {
          this.DFS(criteria, key, annot);
          continue;
        }
        String inclCri = textblock.substring(textblock.indexOf(':') + 2, 
            sepIndex);
        Annotation document = new Annotation(inclCri);
        this.pipeline.annotate(document);
        String results = JSONOutputter.jsonPrint(document);
        if(results == null) throw new JSONException("Fail to get CoreNLP"
            + " response");
        
        annot.put("criteria", new JSONObject());
        annot.getJSONObject("criteria").put("Inclusion Criteria", new
            JSONObject(results));
        
        List<CoreMap> sentences = document.get(SentencesAnnotation.class);
        for(CoreMap sentence: sentences) {
          Tree tree = sentence.get(TreeAnnotation.class);
          tree.setSpans();
          Iterator<Tree> iter = tree.iterator();
          while(iter.hasNext()) {
            Tree node = iter.next();
            System.out.println(node.getSpan());
            System.out.println(node.value());
          }
        }
        
        String exclCri = textblock.substring(sepIndex);
        exclCri = exclCri.substring(exclCri.indexOf(':') + 2);
        document = new Annotation(exclCri);
        this.pipeline.annotate(document);
        results = JSONOutputter.jsonPrint(document);
        if(results == null) throw new JSONException("Fail to get CoreNLP "
            + "response");
        
        annot.getJSONObject("criteria").put("Exclusion Criteria", new
            JSONObject(results));
      } else if (key.equals("textblock") || key.equals("description")) {
        String text = jsonObj.getString(key);
        text = text.replace("≦", "<=");
        text = text.replace("≤", "<=");
        text = text.replace("≧", ">=");
        text = text.replace("≥", ">=");
        text = text.replace("®", "(R)");
        Annotation document = new Annotation(text);
        this.pipeline.annotate(document);
        String results = JSONOutputter.jsonPrint(document);
        if(results == null) throw new JSONException("Fail to get CoreNLP"
            + " response");
        annot.put(objKey, new JSONObject());
        annot.getJSONObject(objKey).put(key, new JSONObject(results));
      } else if(jsonObj.get(key) instanceof JSONObject)
        this.DFS(jsonObj.getJSONObject(key), key, annot);
      else if(jsonObj.get(key) instanceof JSONArray)
        this.DFS(jsonObj.getJSONArray(key), key, annot);
    }
  }
  
  private void DFS(JSONArray jsonArray, String arrayKey, JSONObject annot) 
      throws JSONException, IOException {
    for(int i = 0; i < jsonArray.length(); i++) {
      if(jsonArray.get(i) instanceof JSONObject)
        this.DFS(jsonArray.getJSONObject(i), arrayKey, annot);
      else return;
    }
  }
  
  public static void main(String args[]) {
    GraphBuilder builder = new GraphBuilder();
    try {
      builder.build(args[0], args[1]);
    } catch (JSONException | IOException e) {
      e.printStackTrace();
    }
  }
}
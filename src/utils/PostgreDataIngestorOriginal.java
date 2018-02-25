package utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Scanner;
import java.util.logging.Logger;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * A class that imports json data into postgres database.
 * @author Haoran Sun
 * @since 02/05/2018
 */
public class PostgreDataIngestorOriginal {
  static final String DRIVER = "org.postgresql.Driver";
  static final String PREFIX = "jdbc:postgresql://";
  
  private static final Logger logger =
      Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
  
  private HashMap<String, String> cols;
  
  /**
   * Default constructor
   */
  public PostgreDataIngestorOriginal() {
  	this.cols = new HashMap<>();
  }
  
  /**
   * Connect to specified PostgreSQL database
   * @param DBName - database name
   * @param ConnectString - database ip and port
   * @param uid - user name
   * @param password - password
   * @return a valid connection
   */
  public Connection getConn(String ConnectString, String uid,
      String password) {
    Connection conn = null;
    try {
      Class.forName(DRIVER); //Load PostgreSQL JDBC driver
      /* Enter user and password information */
      Properties props = new Properties();
      props.setProperty("user", uid);
      props.setProperty("password", password);
      conn = DriverManager.getConnection(PREFIX + ConnectString , props);
    } catch (ClassNotFoundException | SQLException e) {
      logger.severe("Postgres connection failed!");
    }
    return conn;
  }
  
  /**
   * Send query and obtain result
   * @param conn - connection
   * @param queryString - query expression
   * @return query result set
   */
  public void query(Connection conn, String queryString) {
    try {
      Statement st = conn.createStatement();
      st.executeUpdate(queryString); //Get query result
    } catch (SQLException e) {
      logger.severe("Failed to get resultSet!");
      System.err.println(queryString);
    }
  }
  
  /**
   * Perform type check on all entries present in the set of json files, if the
   * type of fields conflict, make the field type jsonb.
   * @param dir - the directory that contains all json files.
   * @param name - data base name.
   * @return A query string that used to create the table with proper field
   * types.
   */
  public String typeCheck(String dir, String name) {
  	File directory = new File(dir);
		for(final File file : directory.listFiles()) {
			try(Scanner sc = new Scanner(new FileReader(file.getPath()))) {
				JSONObject jsonObj = new JSONObject(sc.nextLine()).
		  			getJSONObject("clinical_study");
				Iterator<String> it = jsonObj.keys();
		  	while(it.hasNext()) {
		  		String key = it.next();
		  		if(jsonObj.get(key) instanceof JSONObject) {
		  			JSONObject field = jsonObj.getJSONObject(key);
		  			if(field.has("textblock") || field.has("description")) {
		  				if(!this.cols.containsKey(key))
		  					this.cols.put(key, "text");
		  			} else
		  				this.cols.put(key, "jsonb");
		  		}
		  		else if(jsonObj.get(key) instanceof JSONArray)
		  			this.cols.put(key, "jsonb");
		  		else if(!this.cols.containsKey(key))
		  			this.cols.put(key, "text");
		  	}
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
		}
		
		StringBuilder sb = new StringBuilder();
		sb.append("CREATE TABLE IF NOT EXISTS ");
		sb.append(name + '(');
		Iterator<Entry<String, String>> it = this.cols.entrySet().iterator();
		while(it.hasNext()) {
			Entry<String, String> entry = it.next();
			sb.append(entry.getKey() + " " + entry.getValue());
			if(it.hasNext())
				sb.append(", ");
			else
				sb.append(")");
		}
		return sb.toString();
  }
  
  /**
   * Split a json text by first level key
   * @param jsonText - a json format string
   * @return a list of split fields
   */
  private ArrayList<JsonNode> jsonSplit(String jsonText) {
  	JSONObject jsonObj = new JSONObject(jsonText).
  			getJSONObject("clinical_study");
  	ArrayList<JsonNode> ret = new ArrayList<>(10);
  	Iterator<String> it = jsonObj.keys();
  	while(it.hasNext()) {
  		String key = it.next();
  		if(jsonObj.get(key) instanceof JSONObject) {
  			JSONObject field = jsonObj.getJSONObject(key);
  			if(field.has("textblock"))
  				ret.add(new JsonNode(key, field.getString("textblock")));
  			else if (field.has("description"))
  				ret.add(new JsonNode(key, field.getString("description")));
  			else
  				ret.add(new JsonNode(key, jsonObj.getJSONObject(key).toString()));
  		}
  		else if(jsonObj.get(key) instanceof JSONArray)
  			ret.add(new JsonNode(key, jsonObj.getJSONArray(key).toString()));
  		else
  			ret.add(new JsonNode(key, jsonObj.getString(key)));
  	}
  	return ret;
  }
  
  /**
   * Import json data to postgres
   * @param conn - connection to postgres database
   * @param jsonStr - json format string
   * @param table - table name
   * @throws IOException 
   */
  public void ingestData(Connection conn, String jsonStr, String table) {
  	ArrayList<JsonNode> columns = this.jsonSplit(jsonStr);
  	String insertion = "INSERT INTO orignctdata (";
  	String values = "VALUES (";
  	for(int i = 0; i < columns.size(); i++) {
  		JsonNode node = columns.get(i);
  		insertion += node.getKey();
  		String tmp = this.replaceIllegalChars(node.getJsonStr());
  		if(this.cols.get(node.getKey()).equals("jsonb") && !tmp.startsWith("[")
  				&& !tmp.startsWith("{")) {
  			tmp = tmp.replace("\"", "\\\"");
  			tmp = tmp.replace("'", "");
  			tmp = "[\"" + tmp + "\"]";
  		}
  		values += ("'" + tmp.replace("'", "''") + "'");
  		if(i == columns.size() - 1) {
  			insertion += ")";
  			values += ");";
  		} else {
  			insertion += ", ";
  			values += ", ";
  		}
  	}
  	this.query(conn, insertion + " " + values);
  }
  
  private String replaceIllegalChars(String text) {
		try {
			text = replace_UTF8.ReplaceLooklike(text);
			text = text.replace("≦", "<=");
		  text = text.replace("≧", ">=");
		  text = text.replace("㎡", "m2");
		  text = text.replace("ï", "i");
		  text = text.replace("˄", "^");
		  text = text.replace("˚", " degrees");
		  text = text.replace("※", "-");
		  text = text.replace("㎲", " microseconds");
		  text = text.replace("́s", "'s");
		  text = text.replace("╳", "x");
		} catch (IOException e) {
			e.printStackTrace();
		}
		return text;
  }
  
  /**
   * A class to store simple json key-value structure.
   * @author Haoran Sun
   *
   */
  protected class JsonNode {
  	private String key;
  	private String jsonStr;
  	
  	public JsonNode(String key, String jsonStr) {
  		if(key.equals("group")) //Handle reserved word in postgres
  			this.key = "\"group\"";
  		else
  			this.key = key;
  		this.jsonStr = jsonStr;
  	}
  	
  	public String getKey() {
  		return this.key;
  	}
  	
  	public String getJsonStr() {
  		return this.jsonStr;
  	}
  }
  
  public static void main(String[] args) {
  	PostgreDataIngestorOriginal pg = new PostgreDataIngestorOriginal();
  	Connection conn = pg.getConn("10.128.37.10:5432/postgres", "postgres", "something");
  	pg.query(conn, pg.typeCheck(args[0], "orignctdata"));
  	File dir = new File(args[0]);
		for(final File file : dir.listFiles()) {
			try(Scanner sc = new Scanner(new FileReader(file.getPath()))) {
				pg.ingestData(conn, sc.nextLine(), "orignctdata");
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
  }
}
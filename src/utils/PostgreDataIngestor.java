package utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Properties;
import java.util.Scanner;
import java.util.logging.Logger;

import org.json.JSONObject;

/**
 * A class that imports json data into postgres database.
 * @author Haoran Sun
 * @since 03/27/2017
 */
public class PostgreDataIngestor {
  static final String DRIVER = "org.postgresql.Driver";
  static final String PREFIX = "jdbc:postgresql://";
  static final int SIZE = 200;
  
  private static final Logger logger =
      Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
  
  private HashSet<String> cols;
  
  /**
   * Default constructor
   */
  public PostgreDataIngestor() {
  	this.cols = new HashSet<>();
  	this.cols.add("nct_id");
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
    }
  }
  
  /**
   * Split a json text by first level key
   * @param jsonText - a json format string
   * @return a list of split fields
   */
  private ArrayList<JsonNode> jsonSplit(String jsonText) {
  	JSONObject jsonObj = new JSONObject(jsonText);
  	ArrayList<JsonNode> ret = new ArrayList<>(10);
  	Iterator<String> it = jsonObj.keys();
  	while(it.hasNext()) {
  		String key = it.next();
  		if(key.equals("nct_id"))
  			ret.add(new JsonNode(key, jsonObj.getString(key)));
  		else if(jsonObj.getJSONObject(key).has("textblock"))
  			ret.add(new JsonNode(key, jsonObj.getJSONObject(key).getJSONArray
  					("textblock").getJSONArray(3).toString()));
  		else if(jsonObj.getJSONObject(key).has("description"))
  			ret.add(new JsonNode(key, jsonObj.getJSONObject(key).getJSONArray
  					("description").getJSONArray(3).toString()));
  		else
  			ret.add(new JsonNode(key, jsonObj.getJSONObject(key).toString()));
  	}
  	return ret;
  }
  
  /**
   * Import json data to postgres
   * @param conn - connection to postgres database
   * @param jsonStr - json format string
   */
  public void ingestData(Connection conn, String jsonStr) {
  	ArrayList<JsonNode> columns = this.jsonSplit(jsonStr);
  	String insertion = "INSERT INTO ctdata (";
  	String values = "VALUES (";
  	for(int i = 0; i < columns.size(); i++) {
  		JsonNode node = columns.get(i);
  		if(this.cols.add(node.getKey())) //Add new column
  			this.query(conn, "ALTER TABLE ctdata ADD COLUMN " + node.getKey() +
  					" jsonb");
  		insertion += node.getKey();
  		values += ("'" + node.getJsonStr().replace("'", "''") + "'");
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
  	PostgreDataIngestor pg = new PostgreDataIngestor();
  	Connection conn = pg.getConn("localhost:5432/postgres", "postgres", "sdsc123");
  	pg.query(conn, "CREATE TABLE IF NOT EXISTS ctdata (nct_id text)");
  	File dir = new File(args[0]);
		for(final File file : dir.listFiles()) {
			try(Scanner sc = new Scanner(new FileReader(file.getPath()))) {
				pg.ingestData(conn, sc.nextLine());
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
		}
  }
}
package utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;
import java.util.Scanner;
import java.util.logging.Logger;

/**
 * A class that handle PostgreSQL query request.
 * @author Haoran Sun
 * @since 03/27/2017
 */
public class PostgreSQLQuery {
  static final String DRIVER = "org.postgresql.Driver";
  static final String PREFIX = "jdbc:postgresql://";
  static final int SIZE = 200;
  
  private static final Logger logger =
      Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
  
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
  
  public static void main(String[] args) {
  	PostgreSQLQuery pg = new PostgreSQLQuery();
  	Connection conn = pg.getConn("localhost:5432/postgres", "postgres", "sdsc123");
  	pg.query(conn, "CREATE TABLE IF NOT EXISTS ctdata (info json)");
  	File dir = new File(args[0]);
		for(final File file : dir.listFiles()) {
			try(Scanner sc = new Scanner(new FileReader(file.getPath()))) {
				pg.query(conn, "INSERT INTO ctdata (info) VALUES('" + sc.nextLine().
						replace("'", "''") + "');");
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
		}
  }
}
import java.sql.*;

public class DBConnection {

    private static DBConnection INSTANCE;
    private Connection connection;

    public Connection getConnection() {
        return connection;
    }

    private DBConnection(){
        String url = "jdbc:sqlite:ClassOrganizer.db";
        try {
            Class.forName("org.sqlite.JDBC");
            this.connection = DriverManager.getConnection(url);
            System.out.println("Database connection successful");
        } catch (Exception e){
            System.err.println( e.getClass().getName() + ": " + e.getMessage());
        }
    }

    public static DBConnection getInstance(){
        if (INSTANCE == null){
            synchronized(DBConnection.class){
                if(INSTANCE == null){
                    INSTANCE = new DBConnection();
                    System.out.println("connection object created.");
                }
            }
        }else{
            System.out.println("Connection object exists!");
        }
        return INSTANCE;
    }
}

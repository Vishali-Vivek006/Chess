
package database;

import java.sql.*;

public class DatabaseConnection {
    private static final String URL = "jdbc:mysql://localhost:3306/chess_db";
    private static final String USERNAME = "root";
    private static final String PASSWORD = "Thoori@28";
    
    private Connection connection;
    
    public DatabaseConnection() {
        this.connection = null;
    }
   
    public boolean connect() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            
            connection = DriverManager.getConnection(URL, USERNAME, PASSWORD);
            System.out.println(" Connected to MySQL database: chess_db");
            return true;
            
        } catch (ClassNotFoundException e) {
            System.out.println(" MySQL JDBC Driver not found");
            return false;
        } catch (SQLException e) {
            System.out.println(" Connection failed: " + e.getMessage());
            return false;
        }
    }
 
    public boolean saveGame(String playerName, String result, String whiteCaptured, String blackCaptured) {
        if (connection == null) {
            System.out.println(" No database connection");
            return false;
        }
        
        String sql = "INSERT INTO chess_games (player_name, moves, result, white_captured, black_captured) VALUES (?, ?, ?, ?, ?)";
        
        try {
           
            PreparedStatement pstmt = connection.prepareStatement(sql);
            
            pstmt.setString(1, playerName);
            
            
            String moves = "Chess game completed at " + java.time.LocalDateTime.now();
            pstmt.setString(2, moves);
            
            pstmt.setString(3, result);
            pstmt.setString(4, whiteCaptured);
            pstmt.setString(5, blackCaptured);
            
            int rowsAffected = pstmt.executeUpdate();
            pstmt.close();
            
            if (rowsAffected > 0) {
                System.out.println(" Game saved to database successfully");
                return true;
            } else {
                System.out.println(" No rows affected - game not saved");
                return false;
            }
            
        } catch (SQLException e) {
            System.out.println(" Save failed: " + e.getMessage());
            return false;
        }
    }
    
    public void displaySavedGames() {
        if (connection == null) {
            System.out.println(" No database connection");
            return;
        }
        
        String sql = "SELECT id, player_name, result, game_date FROM chess_games ORDER BY game_date DESC";
        
        try {
            Statement stmt = connection.createStatement();
            
            ResultSet rs = stmt.executeQuery(sql);
            
            System.out.println("\n=== SAVED CHESS GAMES ===");
            boolean hasGames = false;
            
            while (rs.next()) {
                hasGames = true;
                int id = rs.getInt("id");
                String player = rs.getString("player_name");
                String result = rs.getString("result");
                Timestamp date = rs.getTimestamp("game_date");
                
                System.out.println("ID: " + id + " | Player: " + player + " | Result: " + result + " | Date: " + date);
            }
            
            if (!hasGames) {
                System.out.println("No games found in database");
            }
            
            rs.close();
            stmt.close();
            
        } catch (SQLException e) {
            System.out.println(" Error reading games: " + e.getMessage());
        }
    }
    
    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                System.out.println(" Database connection closed");
            }
        } catch (SQLException e) {
            System.out.println(" Error closing connection: " + e.getMessage());
        }
    }
    
    public boolean isConnected() {
        try {
            return connection != null && !connection.isClosed();
        } catch (SQLException e) {
            return false;
        }
    }
}
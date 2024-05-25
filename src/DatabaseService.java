import java.io.*;
import java.sql.*;
import java.util.ArrayList;

/**
 * The implemented database class.
 */
public class DatabaseService implements Serializable {
    private static final long serialVersionUID = 1L;
    private final Connection conn;

    private static final String DB_HOST = "localhost";
    private static final String DB_USER = "c3358";
    private static final String DB_PASS = "c3358PASS";
    private static final String DB_NAME = "c3358";
    public static final String ONLINE_USERS = "OnlineUsers";
    public static final String USER_INFO = "UserInfo";
    public DatabaseService() {
        try {
            // Load the MySQL JDBC driver
            Class.forName("com.mysql.cj.jdbc.Driver").newInstance();
            // Establish the connection
            String url = "jdbc:mysql://" + DB_HOST + "/?useSSL=true&allowPublicKeyRetrieval=true";
            conn = DriverManager.getConnection(url, DB_USER, DB_PASS);
            // Create the database if it doesn't exist
            createDatabase(DB_NAME);
            useDatabase(DB_NAME);
            // Drop existing tables if they exist
            dropTable(ONLINE_USERS);
            dropTable(USER_INFO);
            // Create the OnlineUsers and UserInfo tables
            createTable(ONLINE_USERS, "(name VARCHAR(255) PRIMARY KEY)");
            createTable(USER_INFO, "(name VARCHAR(255) PRIMARY KEY, password VARCHAR(255), games INT, wins INT, avg FLOAT, ranking INT)");
            // Clear any existing online users
            clearOnlineUsers();
        } catch (SQLException | IllegalAccessException | InstantiationException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Creates a new database under the user.
     * @param name The name of the database.
     * @throws SQLException From SQL related strings.
     */
    public void createDatabase(String name) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            String sql = "CREATE DATABASE IF NOT EXISTS " + name;
            stmt.executeUpdate(sql);
            System.out.println("Database created successfully (if not exists): " + name);
        }
    }

    /**
     * Makes the system use the given database.
     * @param name The name of the database to be used.
     * @throws SQLException From SQL related strings.
     */
    public void useDatabase(String name) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            String sql = "USE " + name;
            stmt.executeUpdate(sql);
            System.out.println("Switched to database: " + name);
        }
    }

    /**
     * Creates a new table in the database.
     * @param name The name of the table.
     * @param fields The columns of the table.
     * @throws SQLException From SQL related strings.
     */
    public void createTable(String name, String fields) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            String sql = "CREATE TABLE IF NOT EXISTS " + name + " " + fields;
            stmt.executeUpdate(sql);
            System.out.println("Table created successfully (if not exists): " + name);
        }
    }

    /**
     * Drops a given table from the database.
     * @param name The table to be dropped.
     * @throws SQLException From SQL related strings.
     */
    public void dropTable(String name) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            String sql = "DROP TABLE IF EXISTS " + name;
            stmt.executeUpdate(sql);
            System.out.println("Table " + name + " dropped successfully (if exists)");
        }
    }
    /**
     * Clears all users from OnlineUsers, used on server start up.
     */
    public void clearOnlineUsers(){
        try {
            Statement stmt = conn.createStatement();
            String sql = "DELETE FROM " + ONLINE_USERS;
            int rowsAffected = stmt.executeUpdate(sql);
            System.out.println("Cleared " + rowsAffected + " rows from " + ONLINE_USERS + " table.");
            this.list(ONLINE_USERS);
            stmt.close(); // Close the statement after use
        } catch (SQLException e) {
            System.err.println("Error clearing online users: " + e.getMessage());
        }
    }

    /**
     * Logs in user by adding their name to OnlineUsers.
     * @param username The username of the user.
     */
    public void loginUser(String username) {
        try {
            String sql = "INSERT INTO " + ONLINE_USERS + " (name) VALUES (?)";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, username);
            int rowsAffected = stmt.executeUpdate();
            System.out.println("Logged in username " + username + ". Rows affected: " + rowsAffected);
            this.list(ONLINE_USERS);
            stmt.close();
        } catch (SQLException | IllegalArgumentException e) {
            System.err.println("Error inserting record: " + e);
        }
    }

    /**
     * Logs out user by removing their name to OnlineUsers.
     * @param username The username of the user.
     */
    public void logoutUser(String username) {
        try {
            String sql = "DELETE FROM " + ONLINE_USERS + " WHERE name = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, username);
            int rowsAffected = stmt.executeUpdate();
            System.out.println("Logged out user: " + username + ". Rows affected: " + rowsAffected);
            this.list(ONLINE_USERS);
            stmt.close();
        } catch (SQLException | IllegalArgumentException e) {
            System.err.println("Error inserting record: " + e);
        }
    }

    /**
     * Returns all relevant data for a user from UserInfo.
     * @param username The username of the user.
     * @return An object of class User.
     */
    public User getUserData(String username) {
        User user = null;
        String sql = "SELECT * FROM " + USER_INFO + " WHERE name = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    user = new User(
                            rs.getString("name"),
                            rs.getInt("games"),
                            rs.getInt("wins"),
                            rs.getFloat("avg"),
                            rs.getInt("ranking")
                    );
                }
            }
        } catch (SQLException e) {
            System.err.println("Error retrieving user: " + e.getMessage());
        }
        return user;
    }

    /**
     * Returns the data of all users from UserInfo.txt.
     * @return An arraylist containing objects of class User.
     */
    public ArrayList<User> getAllUserData() {
        ArrayList<User> allUserData = new ArrayList<>();
        String sql = "SELECT * FROM " + USER_INFO;
        try (PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                User user = new User(
                        rs.getString("name"),
                        rs.getInt("games"),
                        rs.getInt("wins"),
                        rs.getFloat("avg"),
                        rs.getInt("ranking")
                );
                allUserData.add(user);
            }
        } catch (SQLException e) {
            System.err.println("Error retrieving all users: " + e.getMessage());
        }
        return allUserData;
    }

    /**
     * Authenticates a user when logging in, checks if user exists in UserInfo.txt and not in OnlineUsers.txt.
     * Used for old users.
     * @param username The username of the user.
     * @param password The password of the user.
     * @return True or False based on whether the user was authenticated or not.
     */
    public boolean authenticateUser(String username, String password) {
        String sql = "SELECT u.name FROM " + USER_INFO + " u LEFT JOIN " + ONLINE_USERS + " o ON u.name = o.name WHERE u.name = ? AND u.password = ? AND o.name IS NULL";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username);
            stmt.setString(2, password);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String user = rs.getString(1);
                    if (user.equals(username)) {
                        this.loginUser(username);
                        return true; // User exists, password is correct, and not logged in
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Error authenticating user: " + e.getMessage());
        }
        return false; // User does not exist, password is incorrect, or already logged in
    }

    /**
     * Registers the user by adding their details to UserInfo.txt and adds their username to OnlineUsers.txt.
     * Used for new users.
     * @param username The username of the user.
     * @param password The password of the user.
     * @param numberOfGames The total number of games a user has played.
     * @param numberOfWins The total number of wins a user has.
     * @param avgTimeToGame The average time it takes for a user to win a game.
     * @return True or False based on whether the user was registered and logged in or not.
     */
    public boolean registerAndLoginUser(String username, String password, int numberOfGames, int numberOfWins, double avgTimeToGame) {
        try {
            // Check if the user already exists in either OnlineUsers or UserInfo
            String checkSql = "SELECT name FROM " + USER_INFO + " WHERE name = ?";
            try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
                checkStmt.setString(1, username);
                try (ResultSet rs = checkStmt.executeQuery()) {
                    if (!rs.next()) {
                        // User doesn't exist in either table, proceed with registration
                        String insertSql = "INSERT INTO " + USER_INFO + " (name, password, games, wins, avg, ranking) VALUES (?, ?, ?, ?, ?, ?)";
                        try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
                            insertStmt.setString(1, username);
                            insertStmt.setString(2, password);
                            insertStmt.setInt(3, numberOfGames);
                            insertStmt.setInt(4, numberOfWins);
                            insertStmt.setDouble(5, avgTimeToGame);
                            // Calculate the rank based on the number of existing users
                            insertStmt.setInt(6, 0);
                            // Insert user into UserInfo table
                            int rowsAffected = insertStmt.executeUpdate();
                            System.out.println("User '" + username + "' has been added to UserInfo. Rows affected: " + rowsAffected);
                            this.list(USER_INFO);
                            this.updateRanks();
                            // Log in the user
                            this.loginUser(username);
                            return true; // Registration and login successful
                        }
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Error registering and logging in user: " + e.getMessage());
        }
        return false; // User already exists
    }

    /**
     * Updates the rank of all users based on their games won.
     */
    public void updateRanks() {
        try {
            // SQL query to update ranks based on the specified criteria
            String updateSql = "UPDATE " + USER_INFO + " AS u1 " +
                    "JOIN (SELECT name, " + " RANK() OVER (ORDER BY wins DESC, games ASC, avg ASC, name ASC) AS new_rank " + "FROM " + USER_INFO + ") AS u2 " +
                    "ON u1.name = u2.name " +
                    "SET u1.ranking = u2.new_rank";
            try (PreparedStatement stmt = conn.prepareStatement(updateSql)) {
                int rowsAffected = stmt.executeUpdate();
                System.out.println("Ranks updated for " + rowsAffected + " users.");
                this.list(USER_INFO);
            }
        } catch (SQLException e) {
            System.err.println("Error updating ranks: " + e.getMessage());
        }
    }

    /**
     * Displays contents of table for viewing purposes.
     * @param table The name of the table to be displayed.
     */
    private void list(String table) {
        try {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT * FROM " + table);
            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();
            // Print column names
            for (int i = 1; i <= columnCount; i++) {
                System.out.format("%-15s", metaData.getColumnName(i));
            }
            System.out.println();

            // Print rows
            while (rs.next()) {
                for (int i = 1; i <= columnCount; i++) {
                    System.out.format("%-15s", rs.getString(i));
                }
                System.out.println();
            }
            System.out.println();
            rs.close();
            stmt.close();
        } catch (SQLException e) {
            System.err.println("Error listing records: " + e);
        }
    }

    /**
     * Updates the player's statistics.
     * @param username The username of the player.
     * @param playerWon Whether the player won.
     * @param time The time it took if the player won.
     */
    public void updatePlayerStats(String username, boolean playerWon, float time) {
        int winChange = 1;
        if (!playerWon){
            winChange = 0;
            time = 0.0F;
        }
        // SQL query to update player stats
        String sql = "UPDATE UserInfo " +
                "SET games = games + ?, " +
                "    avg = ROUND(((avg * wins + time * v2) / (wins + v2)), 2) " +
                "    wins = wins + ?, " +
                "WHERE name = ?";
        try {
             PreparedStatement pstmt = this.conn.prepareStatement(sql);
            // Set parameters for the SQL query
            pstmt.setInt(1, 1);
            pstmt.setInt(2, winChange);
            pstmt.setFloat(3, time);
            pstmt.setInt(4, winChange);
            pstmt.setInt(5, winChange);
            pstmt.setString(6, username);
            // Execute the SQL update
            pstmt.executeUpdate();
            System.out.println("Player stats updated successfully!");
            this.updateRanks();
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }
}

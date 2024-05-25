import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.ArrayList;

/**
 * The remote interface for the server.
 */
public interface RemoteServerInterface extends Remote {

    /**
     * Returns the data of a user.
     * @param name The username of the user.
     * @return An object of type class.
     * @throws RemoteException
     */
    public User getUserData(String name) throws RemoteException;

    /**
     * Returns the data of all users.
     * @return A list of objects of class Users.
     * @throws RemoteException
     */
    public ArrayList<User> getAllUserData() throws RemoteException;

    /**
     * Authenticates a user when logging in, checks if user exists in UserInfo.txt and not in OnlineUsers.
     * Used for old users.
     * @param username The username of the user.
     * @param password The password of the user.
     * @return True or False based on whether the user was authenticated or not.
     */
    public boolean authenticateUser(String username, String password) throws RemoteException;

    /**
     * Registers the user by adding their details to UserInfo.txt and adds their username to OnlineUsers.
     * Used for new users.
     * @param username The username of the user.
     * @param password The password of the user.
     * @param games The total number of games a user has played.
     * @param wins The total number of wins a user has.
     * @param avg The average time it takes for a user to win a game.
     * @return True or False based on whether the user was registered and logged in or not.
     */
    public boolean registerAndLoginUser(String username, int games, int wins, double avg, String password) throws RemoteException;

    /**
     * Logs out user by removing their name to OnlineUsers.
     * @param name The username of the user.
     */
    public void logoutUser(String name) throws RemoteException;
}

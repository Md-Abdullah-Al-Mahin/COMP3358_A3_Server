import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Objects;

/**
 * The main game server.
 */
public class GameServer extends UnicastRemoteObject implements RemoteServerInterface {
    private static final long serialVersionUID = 1L;
    public DatabaseService db;
    public Game game = new Game(this);
    public JMSQueueClient queue;
    public JMSTopicPublisher topicPublisher;
    GameServer() throws RemoteException {
        super();
        this.db = new DatabaseService();
        this.queue = new JMSQueueClient(this, "localhost");
        this.topicPublisher = new JMSTopicPublisher(this, "localhost");
        this.queue.startListening();
    }

    public static void main(String[] args) {
        try {
            GameServer server = new GameServer();
            Naming.rebind("GameServer", server);
            System.out.println("Game server is running...");
        } catch (RemoteException | MalformedURLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Handler for client requesting game.
     * @param username The username of the requester.
     * @param wins The number of wins of the requester.
     * @param avg The average time to win of the requester.
     */
    public void requestGame(String username, int wins, float avg){
        if (!this.game.started && this.game.playerCount() < 4){
            this.game.addPlayer(username, wins, avg);
            this.queue.sendMessage("GAMEJOINED_" + username);
        } else {
            this.queue.sendMessage("GAMEFULL_" + username);
        }
    }

    @Override
    public User getUserData(String name) throws RemoteException {
        return this.db.getUserData(name);
    }

    @Override
    public ArrayList<User> getAllUserData() throws RemoteException {
        return this.db.getAllUserData();
    }

    @Override
    public boolean  authenticateUser(String username, String password) throws RemoteException {
        return this.db.authenticateUser(username, password);
    }

    @Override
    public boolean registerAndLoginUser(String username, int games, int wins, double avg, String password) {
        return this.db.registerAndLoginUser(username, password, games, wins, avg);
    }

    @Override
    public void logoutUser(String username) throws RemoteException {
        if (username == null) {
            // If name is null, do nothing
            return;
        }
        // Otherwise, perform the logout operation
        this.removePlayer(username);
        this.db.logoutUser(username);
    }

    public void removePlayer(String username){
        this.game.removePlayer(username);
    }
    public void renewGameObj(){
        this.game = new Game(this);
    }
}

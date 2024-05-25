import javax.jms.*;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.util.ArrayList;
import java.util.Objects;

public class JMSTopicPublisher {
    private final GameServer server;
    private final String host;
    private Context jndiContext;
    private ConnectionFactory connectionFactory;
    private Topic topic;
    private Connection connection;
    private Session session;

    /**
     * Publisher for the topic of the game.
     * @param server The game server.
     * @param host The host where the topic is hosted.
     */
    public JMSTopicPublisher(GameServer server, String host) {
        this.server = server;
        this.host = host;
        this.createJNDIContext();
        this.lookupConnectionFactory();
        this.lookupTopic();
        this.createConnection();
    }

    /**
     * Creates a JNDI context.
     */
    private void createJNDIContext() {
        System.setProperty("org.omg.CORBA.ORBInitialHost", host);
        System.setProperty("org.omg.CORBA.ORBInitialPort", "3700");
        try {
            this.jndiContext = new InitialContext();
        } catch (NamingException e) {
            System.err.println("Could not create JNDI API context: " + e);
        }
    }

    /**
     * Looks up the connection factory.
     */
    private void lookupConnectionFactory() {
        try {
            this.connectionFactory = (TopicConnectionFactory) jndiContext.lookup("jms/JPoker24GameConnectionFactory");
        } catch (NamingException e) {
            System.err.println("JNDI API JMS connection factory lookup failed: " + e);
        }
    }

    /**
     * Looks up the topic.
     */
    private void lookupTopic() {
        try {
            this.topic = (Topic) jndiContext.lookup("jms/JPoker24GameTopic");
        } catch (NamingException e) {
            System.err.println("JNDI API JMS topic lookup failed: " + e);
        }
    }

    /**
     * Creates a session with the topic.
     */
    private void createConnection() {
        try {
            this.connection = connectionFactory.createConnection();
            connection.start();
            this.session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE); // Create non-transacted session
        } catch (JMSException e) {
            System.err.println("Failed to create connection to JMS provider: " + e);
        }
    }

    /**
     * Publishes message to the topic.
     * @param messageText The message to be published.
     */
    public void publish(String messageText) {
        try {
            MessageProducer producer = session.createProducer(topic);
            TextMessage message = session.createTextMessage(messageText);
            producer.send(message);
            System.out.println("Message published to topic: " + messageText);
        } catch (JMSException e) {
            System.err.println("Failed to publish message: " + e);
        }
    }

    /**
     * Starts the game.
     * @param numbers The numbers of the cards.
     * @param users The players in the game.
     */
    public void startGame(ArrayList<Integer> numbers, ArrayList<User> users){
        String sentString = "STARTGAME_" + numbers.get(0) + "_" + numbers.get(1) + "_" + numbers.get(2) + "_" + numbers.get(3) + "_" + numbers.get(3) + "_";
        for (int i = 0; i < users.size(); i++){
            sentString += users.get(i).getName() + " " + users.get(i).getGamesWon() + " " + users.get(i).getAvgTimeToGame();
            if (i != users.size() - 1){
                sentString += "|";
            }
        }
        this.publish(sentString);
    }

    /**
     * Ends the game.
     * @param username Username of the winner.
     * @param ansString The answer string of the winner.
     * @param time The time it took for the winner.
     */
    public void endGame(String username, String ansString, float time){
        this.publish("ENDGAME_" + username + "_" + ansString + "_" + time);
    }
}

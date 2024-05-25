import javax.jms.*;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.util.Objects;
import java.util.Properties;


/**
 * Queue client for JMS.
 */
public class JMSQueueClient {
    private GameServer server;
    private String host;
    private Context jndiContext;
    private ConnectionFactory connectionFactory;
    private Queue queue;
    private Connection connection;
    private Session session;
    public boolean waitBreaker = false;

    /**
     * Queue client for JMS.
     * @param server The game server.
     * @param host The host where the queue is hosted.
     */
    public JMSQueueClient(GameServer server, String host){
        this.server = server;
        this.host = host;
        this.createJNDIContext();
        this.lookupConnectionFactory();
        this.lookupQueue();
        this.createConnection();
    }

    /**
     * Creates the JNDI context.
     */
    private void createJNDIContext() {
        System.setProperty("org.omg.CORBA.ORBInitialHost", host);
        System.setProperty("org.omg.CORBA.ORBInitialPort", "3700");
        Properties env = new Properties();
        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.cosnaming.CNCtxFactory");
        env.put(Context.PROVIDER_URL, "iiop://" + host + ":3700");
        try {
            this.jndiContext = new InitialContext(env);
        } catch (NamingException e) {
            System.err.println("Could not create JNDI API context: " + e);
        }
    }

    /**
     * Looks up the connection factory.
     */
    private void lookupConnectionFactory() {
        try {
            this.connectionFactory = (ConnectionFactory) jndiContext.lookup("jms/JPoker24GameConnectionFactory");
        } catch (NamingException e) {
            System.err.println("JNDI API JMS connection factory lookup failed: " + e);
        }
    }

    /**
     * Looks up the queue.
     */
    private void lookupQueue() {
        try {
            this.queue = (Queue) jndiContext.lookup("jms/JPoker24GameQueue");
        } catch (NamingException e) {
            System.err.println("JNDI API JMS queue lookup failed: " + e);
        }
    }

    /**
     * Creates a new session.
     */
    private void createConnection() {
        try {
            this.connection = connectionFactory.createConnection();
            connection.start();
            this.session = connection.createSession(true, Session.CLIENT_ACKNOWLEDGE);
        } catch (JMSException e) {
            System.err.println("Failed to create connection to JMS provider: " + e);
        }
    }

    /**
     * Sends a message to the queue.
     * @param messageText The message to be sent.
     */
    public void sendMessage(String messageText) {
        try {
            MessageProducer producer = session.createProducer(queue);
            TextMessage message = session.createTextMessage(messageText);
            producer.send(message);
            System.out.println("Message sent to the queue: " + messageText);
            producer.close();
        } catch (JMSException e) {
            System.err.println("Failed to send message to queue: " + e);
        }
    }

    /**
     * Starts checking the queue for incoming messages.
     */
    public void startListening() {
        try {
            MessageConsumer consumer = session.createConsumer(queue);
            consumer.setMessageListener(message -> {
                try {
                    if (message instanceof TextMessage) {
                        TextMessage textMessage = (TextMessage) message;
                        String[] msg = textMessage.getText().split("_");
                        if (Objects.equals(msg[0], "REQUESTGAME")) {
                            message.acknowledge();
                            this.server.requestGame(msg[1],Integer.parseInt(msg[2]), Float.parseFloat(msg[3]));
                        } else if (Objects.equals(msg[0], "LEAVEGAME")){
                            message.acknowledge();
                            this.server.removePlayer(msg[1]);
                            this.sendMessage("GAMELEFT_" + msg[1]);
                        } else if (Objects.equals(msg[0], "SUBMITANSWER")){
                            message.acknowledge();
                            String result = this.server.game.checkAns(msg[1]);
                            if (Objects.equals(result, "Right Answer")){
                                this.sendMessage("RIGHTANSWER_" + msg[1]);
                                this.server.game.endGame(msg[1], msg[2], Float.parseFloat(msg[3]));
                            } else {
                                this.sendMessage("WRONGANSWER_" + msg[1] + "_" + result);
                            }
                        }
                    }
                } catch (JMSException e) {
                    System.err.println("Failed to process message from queue: " + e);
                }
            });
            connection.start();
        } catch (JMSException e) {
            System.err.println("Failed to start listening to queue: " + e);
        }
    }
}

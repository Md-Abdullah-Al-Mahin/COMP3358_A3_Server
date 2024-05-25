import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Game {
    /**
     * The class that runs the game.
     * @param server The server class.
     */
    public Game(GameServer server){
        this.server = server;
        this.runTimer();
    }
    private final ArrayList<Integer> numbers = this.generateNumbers(1, 52, 4);
    private final int ans = 24;
    private GameServer server;
    private ArrayList<User> players = new ArrayList<>();
    public boolean started = false;
    public boolean ended = false;
    private boolean timeElapsed = false;
    private ExpressionParser expressionParser;

    /**
     * Returns number of players in game.
     * @return The number of players.
     */
    public int playerCount(){
        return this.players.size();
    }

    /**
     * Runs the timer to check start conditions.
     */
    private void runTimer(){
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.schedule(() -> {
            this.timeElapsed = true;
            if (this.checkGameStartConditions() && !this.started){
                this.playGame();
            }
            scheduler.shutdown();
        }, 10, TimeUnit.SECONDS);
    }

    /**
     * Adds a player to the game.
     * @param name Username of the player.
     * @param wins The number of wins the player has.
     * @param avg The average time to win for the player.
     */
    public synchronized void addPlayer(String name, int wins, float avg){
        this.players.add(new User(name, 0, wins, avg, 0));
        if (this.checkGameStartConditions()){
            this.playGame();
        }
    }

    /**
     * Checks the starting conditions of the game.
     * @return True or False on whether to start the game.
     */
    public boolean checkGameStartConditions(){
        return this.playerCount() == 4 || (this.timeElapsed && this.playerCount() > 1);
    }

    /**
     * Starts the game.
     */
    private void playGame(){
        System.out.println("Starting game");
        this.started = true;
        this.server.topicPublisher.startGame(this.numbers, this.players);
    }

    /**
     * Ends the game.
     * @param username Username of the winner.
     * @param ansString The answer string of the winner.
     * @param time The time it took for the winner.
     */
    public synchronized void endGame(String username, String ansString, float time){
        if (!this.ended){
            this.ended = true;
            this.server.topicPublisher.endGame(username, ansString, time);
            for (User user: this.players){
                this.server.db.updatePlayerStats(user.getName(), Objects.equals(user.getName(), username), time);
            }
            this.server.renewGameObj();
        }
    }

    /**
     * Checks whether the answer is correct.
     * @param ansString The string to be checked.
     * @return The answer is correct or the error.
     */
    public String checkAns(String ansString){
        try {
            double result = this.expressionParser.evaluate(this.expressionParser.parseExpression(ansString));
            if (result == 24){
                return "Right Answer";
            } else {
                return "The answer does not evaluate to 24!";
            }
        } catch (Exception ex){
           return "The expression is not valid!";
        }
    }

    /**
     * Removes a player from the game.
     * @param username The username of the player to be removed.
     */
    public void removePlayer(String username){
        User rem = null;
        for (User user : this.players){
            if (Objects.equals(user.getName(), username)){
                rem = user;
                break;
            }
        }
        this.players.remove(rem);
    }
    private ArrayList<Integer> generateNumbers(int min, int max, int count) {
        ArrayList<Integer> numbers = new ArrayList<>();
        Random random = new Random();
        while (numbers.size() < count) {
            int randomNumber = random.nextInt(max - min + 1) + min;
            if (!numbers.contains(randomNumber)) {
                numbers.add(randomNumber);
            }
        }
        return numbers;
    }

}

package restaurantrush.game;

import java.util.Scanner;
import restaurantrush.cli.ConsoleUI;

public final class Main {
    private Main() {
    }

    public static void main(String[] args) {
        TurnController controller = GameSetup.baseGame();
        boolean finished = new ConsoleUI(new Scanner(System.in), System.out).run(controller);
        if (!finished) {
            System.exit(2);
        }
    }
}

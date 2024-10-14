import java.util.Scanner;
public class Main {
    static frontEnd frontEndOBJ = new frontEnd();
    static backEnd backEndOBJ = new backEnd();

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        System.out.println("1. Front-end\n" + "2. Back-end");
        switch (scanner.nextInt()) {
            case 1 -> frontEndOBJ.startFrontEnd();
            case 2 -> backEndOBJ.startBackEnd();
        }
    }
}
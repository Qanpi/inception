import java.util.Scanner;

public class Sum {
    public static void main(String[] args) {
        try (Scanner sc = new Scanner(System.in)) {
            System.out.println("Please enter two integers separated by a space to get their sum.");
            int x = sc.nextInt();
            int y = sc.nextInt();
            System.out.println(x+y);
        }
    }
}
import java.util.Scanner;

public class New {
    public static void main(String[] args) {
        for (int i=0; i<5; i++) {
            System.out.println(i);
            try (Scanner sc = new Scanner(System.in)) {
                System.out.println(sc.next());
            }
        }                
        System.out.println("test");
    }
}
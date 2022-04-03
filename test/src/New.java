import java.util.Scanner;

public class New {
    public static void main(String[] args) {
        try (Scanner sc = new Scanner(System.in)) {
                System.out.println("scanner" + sc.next());
            }
        for (int i=0; i<5; i++) {
            System.out.println(i);
            
        }                
        System.out.println("test");
    }
}
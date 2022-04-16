import java.util.Scanner;

public class New {
    public static void main(String[] args) {
        
        try (Scanner sc = new Scanner(System.in)) {
               for (int i=0; i<5; i++) {
                   System.out.println("scanner" + sc.next());
               }
        }                
        System.out.println("test");
    }
}
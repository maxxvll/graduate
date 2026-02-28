import cn.dev33.satoken.secure.BCrypt;

public class A {
    public static void main(String[] args) {
        String hashedPassword = BCrypt.hashpw("123456", BCrypt.gensalt());
        System.out.println(hashedPassword);
        System.out.println(BCrypt.checkpw("123456","$2a$10$3uCxrZ7bFl4uR3UUAcYiMudVG7foHiDyTeH9MNaaG/fpRG3hnKl7K"
                ));
    }
}

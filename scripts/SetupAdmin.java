import java.io.Console;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Arrays;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class SetupAdmin {
    public static void main(String[] args) throws Exception {
        if (args.length == 1 && args[0].equals("--hash-stdin")) {
            byte[] bytes = System.in.readAllBytes();
            try {
                if (bytes.length < 8 || bytes.length > 72) throw new IllegalArgumentException("Use 8 to 72 UTF-8 bytes.");
                System.out.print(new BCryptPasswordEncoder(12).encode(StandardCharsets.UTF_8.decode(java.nio.ByteBuffer.wrap(bytes))));
            } finally { Arrays.fill(bytes, (byte) 0); }
            return;
        }
        Console console = System.console();
        if (console == null) {
            throw new IllegalStateException("Run setup from an interactive terminal; password input must not be echoed.");
        }
        Path file = Path.of(".env.admin");
        if (Files.exists(file)) {
            throw new IllegalStateException(".env.admin already exists. Move it to a private backup before changing credentials.");
        }
        String email = console.readLine("Admin email: ").trim();
        if (!email.matches("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}")) {
            throw new IllegalArgumentException("Enter a valid email address.");
        }
        char[] password = console.readPassword("Admin password (hidden): ");
        char[] confirmation = console.readPassword("Confirm password (hidden): ");
        try {
            if (password.length < 8 || StandardCharsets.UTF_8.encode(CharBuffer.wrap(password)).remaining() > 72 || !Arrays.equals(password, confirmation)) {
                throw new IllegalArgumentException("Passwords must match and contain at least 8 characters and at most 72 UTF-8 bytes.");
            }
            String hash = new BCryptPasswordEncoder(12).encode(CharBuffer.wrap(password));
            Files.createFile(file, PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rw-------")));
            Files.writeString(file, "# Local-only Java properties; do not source this file in a shell.\n"
                + "SKYNODE_ADMIN_EMAIL=" + email + "\nSKYNODE_ADMIN_PASSWORD_HASH=" + hash + "\n");
            console.printf("Created private .env.admin with the email and BCrypt hash. No plaintext password was saved.%n");
        } finally {
            Arrays.fill(password, '\0');
            Arrays.fill(confirmation, '\0');
        }
    }
}

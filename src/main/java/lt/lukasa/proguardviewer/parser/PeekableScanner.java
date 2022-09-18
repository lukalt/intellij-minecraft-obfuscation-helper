package lt.lukasa.proguardviewer.parser;

import java.io.PrintWriter;
import java.util.Scanner;

/**
 * @author Lukas Alt
 * @since 12.08.2022
 */
public class PeekableScanner {
    private final Scanner scanner;
    private String next;

    public PeekableScanner(Scanner scanner) {
        this.scanner = scanner;
        this.nextLine();
    }

    public boolean hasNextLine() {
        return this.next != null;
    }

    public String nextLine() {
        String s = next;
        next = scanner.hasNextLine() ? scanner.nextLine() : null;
        return s;
    }

    public String peekNextLine() {
        return next;
    }

}

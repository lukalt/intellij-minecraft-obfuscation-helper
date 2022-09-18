package lt.lukasa.proguardviewer.util;

/**
 * @author Lukas Alt
 * @since 12.08.2022
 */
public class CharBuffer {
    private final char[] buffer;
    private int index;

    public CharBuffer(String input) {
        this.buffer = input.toCharArray();
        this.index = 0;
    }

    public boolean hasNext() {
        return this.index < buffer.length;
    }

    public char take() {
        final char c = this.buffer[this.index];
        this.index++;
        return c;
    }

    public char peek() {
        return this.buffer[index];
    }
}

package lt.lukasa.proguardviewer.ui;

import javax.swing.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * @author Lukas Alt
 * @since 13.08.2022
 */
public class JModelCheckBox extends JCheckBox {
    public JModelCheckBox(String text, Supplier<Boolean> getState, Consumer<Boolean> setState) {
        super(text);
        setModel(new JToggleButton.ToggleButtonModel() {
            @Override
            public boolean isSelected() {
                return getState.get();
            }

            @Override
            public void setSelected(boolean b) {
                setState.accept(b);
            }
        });
    }
}

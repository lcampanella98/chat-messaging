package chat.res.sender;

import java.awt.Color;
import java.io.Serializable;

public abstract class Sender implements Serializable {

    private String name;
    private Color displayColor;

    public Sender(String name) {
        this(name, null);
    }

    public Sender(String name, Color displayColor) {
        this.name = name;
        this.displayColor = displayColor;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Color getDisplayColor() {
        return displayColor;
    }

    public void setDisplayColor(Color displayColor) {
        this.displayColor = displayColor;
    }

    public String toString() {
        return name;
    }
}

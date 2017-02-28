package chat.res;


import java.awt.Color;

public class HelperMethods {

    public static Color hexToRGB(String colorStr) {
        return new Color(Integer.valueOf(colorStr.substring(1, 3), 16),
                Integer.valueOf(colorStr.substring(3, 5), 16), Integer.valueOf(
                colorStr.substring(5, 7), 16));
    }

    public static String colorToHex(Color color) {
        String hex;
        int r = color.getRed();
        int g = color.getGreen();
        int b = color.getBlue();

        hex = String.format("#%02x%02x%02x", r, g, b);

        return hex;
    }

}

package work.ready.core.tools.jcolor;

public abstract class Attribute {

    @Override
    public abstract String toString();

    public static Attribute NONE() {
        return new SimpleAttribute("");
    }

    public static Attribute CLEAR() {
        return new SimpleAttribute("0");
    }

    public static Attribute BOLD() {
        return new SimpleAttribute("1");
    }

    public static Attribute SATURATED() {
        return new SimpleAttribute("1");
    }

    public static Attribute DIM() {
        return new SimpleAttribute("2");
    }

    public static Attribute DESATURATED() {
        return new SimpleAttribute("2");
    }

    public static Attribute ITALIC() {
        return new SimpleAttribute("3");
    }

    public static Attribute UNDERLINE() {
        return new SimpleAttribute("4");
    }

    public static Attribute SLOW_BLINK() {
        return new SimpleAttribute("5");
    }

    public static Attribute RAPID_BLINK() {
        return new SimpleAttribute("6");
    }

    public static Attribute REVERSE() {
        return new SimpleAttribute("7");
    }

    public static Attribute HIDDEN() {
        return new SimpleAttribute("8");
    }

    public static Attribute STRIKETHROUGH() {
        return new SimpleAttribute("9");
    }

    public static Attribute BLACK_TEXT() {
        return new SimpleAttribute("30");
    }

    public static Attribute RED_TEXT() {
        return new SimpleAttribute("31");
    }

    public static Attribute GREEN_TEXT() {
        return new SimpleAttribute("32");
    }

    public static Attribute YELLOW_TEXT() {
        return new SimpleAttribute("33");
    }

    public static Attribute BLUE_TEXT() {
        return new SimpleAttribute("34");
    }

    public static Attribute MAGENTA_TEXT() {
        return new SimpleAttribute("35");
    }

    public static Attribute CYAN_TEXT() {
        return new SimpleAttribute("36");
    }

    public static Attribute WHITE_TEXT() {
        return new SimpleAttribute("37");
    }

    public static Attribute BLACK_BACK() {
        return new SimpleAttribute("40");
    }

    public static Attribute RED_BACK() {
        return new SimpleAttribute("41");
    }

    public static Attribute GREEN_BACK() {
        return new SimpleAttribute("42");
    }

    public static Attribute YELLOW_BACK() {
        return new SimpleAttribute("43");
    }

    public static Attribute BLUE_BACK() {
        return new SimpleAttribute("44");
    }

    public static Attribute MAGENTA_BACK() {
        return new SimpleAttribute("45");
    }

    public static Attribute CYAN_BACK() {
        return new SimpleAttribute("46");
    }

    public static Attribute WHITE_BACK() {
        return new SimpleAttribute("47");
    }

    public static Attribute BRIGHT_BLACK_TEXT() {
        return new SimpleAttribute("90");
    }

    public static Attribute BRIGHT_RED_TEXT() {
        return new SimpleAttribute("91");
    }

    public static Attribute BRIGHT_GREEN_TEXT() {
        return new SimpleAttribute("92");
    }

    public static Attribute BRIGHT_YELLOW_TEXT() {
        return new SimpleAttribute("93");
    }

    public static Attribute BRIGHT_BLUE_TEXT() {
        return new SimpleAttribute("94");
    }

    public static Attribute BRIGHT_MAGENTA_TEXT() {
        return new SimpleAttribute("95");
    }

    public static Attribute BRIGHT_CYAN_TEXT() {
        return new SimpleAttribute("96");
    }

    public static Attribute BRIGHT_WHITE_TEXT() {
        return new SimpleAttribute("97");
    }

    public static Attribute BRIGHT_BLACK_BACK() {
        return new SimpleAttribute("100");
    }

    public static Attribute BRIGHT_RED_BACK() {
        return new SimpleAttribute("101");
    }

    public static Attribute BRIGHT_GREEN_BACK() {
        return new SimpleAttribute("102");
    }

    public static Attribute BRIGHT_YELLOW_BACK() {
        return new SimpleAttribute("103");
    }

    public static Attribute BRIGHT_BLUE_BACK() {
        return new SimpleAttribute("104");
    }

    public static Attribute BRIGHT_MAGENTA_BACK() {
        return new SimpleAttribute("105");
    }

    public static Attribute BRIGHT_CYAN_BACK() {
        return new SimpleAttribute("106");
    }

    public static Attribute BRIGHT_WHITE_BACK() {
        return new SimpleAttribute("107");
    }

    public static Attribute TEXT_COLOR(int colorNumber) {
        return new TextColorAttribute(colorNumber);
    }

    public static Attribute TEXT_COLOR(int r, int g, int b) {
        return new TextColorAttribute(r, g, b);
    }

    public static Attribute BACK_COLOR(int colorNumber) {
        return new BackColorAttribute(colorNumber);
    }

    public static Attribute BACK_COLOR(int r, int g, int b) {
        return new BackColorAttribute(r, g, b);
    }

}


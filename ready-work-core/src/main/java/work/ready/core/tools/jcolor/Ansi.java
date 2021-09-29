package work.ready.core.tools.jcolor;

public class Ansi {

    private static final char ESC = 27; 
    private static final String NEWLINE = System.getProperty("line.separator");

    public static final String PREFIX = ESC + "[";
    
    public static final String SEPARATOR = ";";
    
    public static final String POSTFIX = "m";
    
    public static final String RESET = PREFIX + Attribute.CLEAR() + POSTFIX;

    public static String generateCode(Attribute... attributes) {
        StringBuilder builder = new StringBuilder();

        builder.append(PREFIX);
        for (Object option : attributes) {
            String code = option.toString();
            if (code.equals(""))
                continue;
            builder.append(code);
            builder.append(SEPARATOR);
        }
        builder.append(POSTFIX);

        return builder.toString().replace(SEPARATOR + POSTFIX, POSTFIX);
    }

    public static String generateCode(AnsiFormat attributes) {
        return generateCode(attributes.toArray());
    }

    public static String colorize(String text, String ansiCode) {
        StringBuilder output = new StringBuilder();
        boolean endsWithLine = text.endsWith(NEWLINE);

        String[] lines = text.split(NEWLINE);
        
        for (String line : lines) {
            output.append(ansiCode);
            output.append(line);
            output.append(RESET);
            if (endsWithLine)
                output.append(NEWLINE);
        }
        return output.toString();
    }

    public static String colorize(String text, Attribute... attributes) {
        String ansiCode = generateCode(attributes);
        return colorize(text, ansiCode);
    }

    public static String colorize(String text, AnsiFormat attributes) {
        return colorize(text, attributes.toArray());
    }

    public static String makeItFabulous(String text, Attribute... attributes) {
        return colorize(text, attributes);
    }
}

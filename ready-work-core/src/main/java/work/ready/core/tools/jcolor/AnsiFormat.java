package work.ready.core.tools.jcolor;

import java.util.ArrayList;
import java.util.Arrays;

public class AnsiFormat {

    private final ArrayList<Attribute> _attributes = new ArrayList<>(2);

    public AnsiFormat(Attribute... attributes) {
        _attributes.addAll(Arrays.asList(attributes));
    }

    public String format(String text) {
        return Ansi.colorize(text, this.toArray());
    }

    protected Attribute[] toArray() {
        return _attributes.toArray(new Attribute[0]);
    }
}

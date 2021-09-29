package work.ready.core.tools.jcolor;

class SimpleAttribute extends Attribute {

    private final String _code;

    SimpleAttribute(String code) {
        _code = code;
    }

    @Override
    public String toString() {
        return _code;
    }

}

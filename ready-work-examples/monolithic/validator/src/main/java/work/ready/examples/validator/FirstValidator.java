package work.ready.examples.validator;

import work.ready.core.handler.Controller;
import work.ready.core.handler.validate.Validator;

public class FirstValidator extends Validator {

    @Override
    protected void validate(Controller c) {
        validateInteger("a", "msg", "a is required and it must be an integer");
        validateInteger("b", "msg", "b is required and it must be an integer");
    }

    @Override
    protected void handleError(Controller c) {
        c.renderJson(getError());
    }
}

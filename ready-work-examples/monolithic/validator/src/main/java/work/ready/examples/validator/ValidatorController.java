package work.ready.examples.validator;

import work.ready.core.aop.annotation.Before;
import work.ready.core.handler.Controller;
import work.ready.core.handler.route.RequestMapping;
import work.ready.core.ioc.annotation.Autowired;
import work.ready.core.service.result.Result;
import work.ready.core.service.result.Success;
import work.ready.core.tools.validator.Assert;

@RequestMapping("/")
public class ValidatorController extends Controller {

    @Autowired
    private CalculatorService calculator;

    @RequestMapping
    public Result<Integer> add() {
        int a = Assert.notNull(getParamToInt("a"), "a is required and it must be an integer");
        int b = Assert.notNull(getParamToInt("b"), "b is required and it must be an integer");
        return Success.of(calculator.addFunction(a, b));
    }

    @RequestMapping
    @Before(FirstValidator.class)
    public Result<Integer> subtract() {
        int a = getParamToInt("a");
        int b = getParamToInt("b");
        return Success.of(calculator.subtractFunction(a, b));
    }
}

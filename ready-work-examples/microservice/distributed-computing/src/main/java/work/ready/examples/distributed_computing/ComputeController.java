package work.ready.examples.distributed_computing;

import work.ready.core.handler.Controller;
import work.ready.core.handler.route.RequestMapping;
import work.ready.core.ioc.annotation.Autowired;
import work.ready.core.service.result.Result;
import work.ready.core.service.result.Success;

import java.util.ArrayList;
import java.util.List;

@RequestMapping("/")
public class ComputeController extends Controller {

    @Autowired
    private ComputeService computer;

    @RequestMapping
    public Result<String> displayWords() {
        String text = getParam("text", "Print words on different cloud nodes.");
        return Success.of(computer.displayWords(text));
    }

    @RequestMapping
    public Result<Integer> countWords() {
        String text = getParam("text", "Hello ! Welcome to the distributed computing world.");
        return Success.of(computer.countWords(text));
    }

    @RequestMapping
    public Result<Integer> letterCountTask() {
        String text = getParam("text", "Hello ! Welcome to the distributed computing world.");
        return Success.of(computer.letterCountTask(text));
    }

    @RequestMapping
    public Result<Double> standardDeviation() {
        int number = getParamToInt("number", 1);
        if(number > 50) number = 50;
        List<Double> data = new ArrayList<>();
        for(int i = 0; i < number; i++) { data.addAll(computer.getSampleData()); }
        return Success.of( computer.standardDeviation(data) );
    }

}

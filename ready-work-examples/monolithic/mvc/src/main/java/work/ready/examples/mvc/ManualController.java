package work.ready.examples.mvc;

import work.ready.core.handler.Controller;

public class ManualController extends Controller {

    public void index() {
        renderText("this is a default index action, mapped manually");
    }

    public void test() {
        renderText("this is a test action, mapped manually");
    }
}

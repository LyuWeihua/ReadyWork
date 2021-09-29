package work.ready.examples.websocket;

import work.ready.core.handler.Controller;
import work.ready.core.handler.route.RequestMapping;
import work.ready.core.tools.StrUtil;

@RequestMapping("/")
public class ChatController extends Controller {

    @RequestMapping
    public void index() {
        String name = (String)getSession().getAttribute("name");
        if(name == null) {
            getSession().setAttribute("name", "User_" + StrUtil.randomString(5).toLowerCase());
        }
        render("chat");
    }

}

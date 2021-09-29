package work.ready.examples.security;

import work.ready.core.handler.Controller;
import work.ready.core.handler.route.RequestMapping;
import work.ready.core.server.Ready;
import work.ready.core.service.result.Result;
import work.ready.core.service.result.Success;

import java.io.File;

@RequestMapping("/")
public class SecurityController extends Controller {

    @RequestMapping
    public Result<String> index() {
        return Success.of("hello world !");
    }

    @RequestMapping
    public void downloadFromResource(){
        renderFile(new File(Ready.getClassLoader().getResource("static/files/test.dat").getFile()), "测试文件.dat");
    }

    @RequestMapping
    public void downloadFromPath(){
        renderFile(Ready.root().resolve("static/files/test.dat").toFile(), "测试文件.dat");
    }

}

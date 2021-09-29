package work.ready.examples.mvc;

import work.ready.core.handler.Controller;
import work.ready.core.handler.RequestMethod;
import work.ready.core.handler.action.paramgetter.Param;
import work.ready.core.handler.route.RequestMapping;
import work.ready.core.server.Ready;
import work.ready.core.service.result.Result;
import work.ready.core.service.result.Success;

import java.io.File;
import java.io.IOException;

@RequestMapping(value = "/")
public class MvcController extends Controller {

    @RequestMapping
    public Result<String> index() {
        return Success.of("hello world !");
    }

    // http://127.0.0.1:8080/param/param1/param2/?age=99&name=aaa
    @RequestMapping("/param/{p1}/{p2}/")
    public Result<String> param( @Param("name") String name) {
        String p1 = getPathParam("p1");
        String p2 = getPathParam("p2");
        int age = getParamToInt("age", 100);

        return Success.of("p1=" + p1 + ", p2=" + p2 + ", name=" + name + ", age=" + age);
    }

    @RequestMapping
    public void upload() throws IOException {
        if(getRequest().getMethod().equals(RequestMethod.POST)) {
            String result = "";
            for(String name : getFiles().keySet()){
                result += name + " => " + getFiles().get(name).originalFileName + " => ";
                if(getFiles().get(name).fileItem.isInMemory()) {
                    //getFiles().get(name).fileItem.write(Ready.root().resolve(Ready.getBootstrapConfig().getUploadPath()).resolve("filename"));
                    result += "store in memory temporarily";
                } else {
                    //Files.move(getFiles().get(name).file, Ready.root().resolve(Ready.getBootstrapConfig().getUploadPath()).resolve("filename"));
                    result += "store in file temporarily: " + getFiles().get(name).fileItem.getFile().toString();
                }
                result += " " + getFiles().get(name).fileItem.getFileSize() + " bytes\r\n";
            }
            renderText(result);
        } else {
            render("upload");
        }
    }

    @RequestMapping
    public void QrCode(){
        renderQrCode("LvWeiHua", 200, 200);
    }

    @RequestMapping
    public void downloadFromResource(){
        renderFile(new File(Ready.getClassLoader().getResource("static/files/test.dat").getFile()), "测试文件.dat");
    }

    @RequestMapping
    public void downloadFromPath(){
        renderFile(Ready.root().resolve("static/files/test.dat").toFile(), "测试文件.dat");
    }

    @RequestMapping()
    public void template() {
        render("template");
    }

}

package work.ready.examples.i18n;

import work.ready.core.handler.Controller;
import work.ready.core.handler.route.RequestMapping;
import work.ready.core.server.Ready;
import work.ready.core.service.result.Failure;
import work.ready.core.service.result.Result;
import work.ready.core.service.result.Success;
import work.ready.core.service.status.Status;

@RequestMapping("/")
public class I18nController extends Controller {

    @RequestMapping
    public Result<Status> success() {
        return Success.of(new Status("SUCCESS20200", Ready.now()));
    }

    @RequestMapping
    public Result<Status> error() {
        return Failure.of(new Status("ERROR20750", "abc", "Integer"));
    }

    @RequestMapping(produces = RequestMapping.Produces.Xml)
    public Result<Status> successXml() {
        return Success.of(new Status("SUCCESS20200", Ready.now()));
    }

    @RequestMapping(produces = RequestMapping.Produces.Xml)
    public Result<Status> errorXml() {
        return Failure.of(new Status("ERROR20750", "abc", "Integer"));
    }

    @RequestMapping
    public void text() {
        String message = getI18n().get("welcome");
        String description = getI18n().format("today", Ready.now());
        renderText(message + description);
    }

    @RequestMapping
    public void successTpl() {
        setAttr("status", new Status("SUCCESS20200", Ready.now()).setI18n(getI18n()));
        setAttr("now", Ready.now());
        render("success");
    }

    @RequestMapping
    public void errorTpl() {
        setAttr("status", new Status("ERROR20750", "abc", "Integer").setI18n(getI18n()));
        render("error", 400);
    }
}

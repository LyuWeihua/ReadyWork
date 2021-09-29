package work.ready.examples.ioc_aop;

import work.ready.examples.ioc_aop.component.CodeInjector;
import work.ready.examples.ioc_aop.component.CodeWorker;
import work.ready.examples.ioc_aop.component.MyAop;
import work.ready.examples.ioc_aop.component.MyComponent;
import work.ready.examples.ioc_aop.service.Animal;
import work.ready.core.aop.AopComponent;
import work.ready.core.aop.transformer.TransformerManager;
import work.ready.core.component.proxy.JavaCoder;
import work.ready.core.module.Application;
import work.ready.core.server.ApplicationConfig;
import work.ready.core.server.Ready;

import java.util.List;


public class Main extends Application {

    @Override
    protected void globalConfig(ApplicationConfig config) {
        TransformerManager.getInstance().attach(List.of("work.ready.examples.ioc_aop.interceptor.FourthInterceptor"));
    }

    @Override
    protected void initialize() {
        Ready.proxyManager().addAutoCoder(new JavaCoder()
                .setAnnotation(CodeInjector.class)
                .setAssignableFrom(Animal.class)
                .setGenerator(new CodeWorker()));
        Ready.interceptorManager().addAopComponent(
                new AopComponent().setAnnotation(MyAop.class).setInterceptorClass(MyComponent.class)
        );
    }

    public static void main(String[] args) {
        Ready.For(Main.class).Work(args);
    }

}

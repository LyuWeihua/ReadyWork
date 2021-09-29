package work.ready.examples.ioc_aop.service;

import work.ready.examples.ioc_aop.component.CodeInjector;
import work.ready.examples.ioc_aop.component.MyAop;
import work.ready.core.ioc.annotation.Component;

@Component("cat")
public class Cat implements Animal {

    String type = "cat";

    @Override
    public String type() {
        return type;
    }

    @Override
    public int legs() {
        return 4;
    }

    @Override
    public int height() {
        return 30;
    }

    @Override
    public int weight() {
        return 2;
    }

    @Override
    @MyAop
    public String walk() {
        return "The cat is walking. ";
    }

    @Override
    @CodeInjector(replace = false)
    public String eat() {
        return "The cat is eating. ";
    }

    @Override
    public String sleep() {
        return "The cat is sleeping. ";
    }
}

package work.ready.examples.ioc_aop.service;

import work.ready.core.ioc.annotation.Component;

@Component
public class Pig implements Animal {

    String type = "pig";

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
    public String walk() {
        return "The pig is walking. ";
    }

    @Override
    public String eat() {
        return "The pig is eating. ";
    }

    @Override
    public String sleep() {
        return "The pig is sleeping. ";
    }
}

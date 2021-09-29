package work.ready.examples.ioc_aop.service;

public interface Animal {
    String type();

    int legs();
    int height();
    int weight();

    String walk();
    String eat();
    String sleep();
}

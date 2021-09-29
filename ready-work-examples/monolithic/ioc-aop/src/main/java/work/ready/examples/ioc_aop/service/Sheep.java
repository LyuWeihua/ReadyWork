package work.ready.examples.ioc_aop.service;

public class Sheep implements Animal {

    String type = "sheep";

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
        return 120;
    }

    @Override
    public int weight() {
        return 50;
    }

    @Override
    public String walk() {
        return "The sheep is walking. ";
    }

    @Override
    public String eat() {
        return "The sheep is eating. ";
    }

    @Override
    public String sleep() {
        return "The sheep is sleeping. ";
    }

    public void initMethod() {
        System.err.println("Sheep is initializing. ");
    }

    public void destroyMethod() {
        System.err.println("Sheep is destroying. ");
    }
}

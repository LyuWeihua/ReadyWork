package work.ready.examples.ioc_aop.service;

import work.ready.core.ioc.annotation.Scope;
import work.ready.core.ioc.annotation.ScopeType;
import work.ready.core.ioc.annotation.Service;

@Service("dog")
@Scope(ScopeType.prototype)
public class Dog implements Animal {

    String type = "dog";

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
        return 80;
    }

    @Override
    public int weight() {
        return 18;
    }

    @Override
    public String walk() {
        return "The dog is walking. ";
    }

    @Override
    public String eat() {
        return "The dog is eating. ";
    }

    @Override
    public String sleep() {
        return "The dog is sleeping. ";
    }
}

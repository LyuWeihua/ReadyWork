package work.ready.examples.ioc_aop;

import work.ready.examples.ioc_aop.interceptor.FirstInterceptor;
import work.ready.examples.ioc_aop.service.Animal;
import work.ready.examples.ioc_aop.service.Cat;
import work.ready.examples.ioc_aop.service.Dog;
import work.ready.examples.ioc_aop.service.Pig;
import work.ready.core.aop.annotation.Before;
import work.ready.core.handler.Controller;
import work.ready.core.handler.route.RequestMapping;
import work.ready.core.ioc.annotation.Autowired;
import work.ready.core.ioc.annotation.Inject;
import work.ready.core.ioc.annotation.Qualifier;
import work.ready.core.service.result.Result;
import work.ready.core.service.result.Success;

@RequestMapping(value = "/")
public class IocAopController extends Controller {

    @Qualifier("cat")
    @Autowired
    Animal oneAnimal;

    @Inject
    Cat oneCat;

    @Autowired
    Dog oneDog;

    @Autowired
    Dog anotherDog;

    @Qualifier("sheep")
    @Autowired
    Animal oneSheep;

    @Qualifier("lamb")
    @Autowired
    Animal anotherSheep;

    @Inject(name = "ram")
    Animal ram;

    @Inject(name = "ewe")
    Animal ewe;

    @Inject
    Pig onePig;

    @RequestMapping
    public Result<String> index() {
        String result = "";
        if(oneCat.equals(oneAnimal)) {
            result += "The animal is the cat. ";
        } else {
            result += "The animal is not the cat. ";
        }
        if(oneDog.equals(anotherDog)) {
            result += "The two dogs are the same one. ";
        } else {
            result += "The two dogs are not the same. ";
        }
        if(oneSheep.equals(anotherSheep) && ram.equals(ewe) && oneSheep.equals(ram)) {
            result += "The four sheep are the same one. ";
        } else {
            result += "The four sheep are not the same one. ";
        }
        result += "The type of the pig is " + onePig.type() + ". ";
        return Success.of(result);
    }

    @RequestMapping
    @Before(FirstInterceptor.class)
    public Result<String> interceptor() {
        return Success.of("123456");
    }

    @RequestMapping
    public Result<String> globalInterceptor() {
        return Success.of(oneCat.eat() + "\r\n" + oneDog.eat() + "\r\n" + oneSheep.eat() + "\r\n" + onePig.eat() + "\r\n" + "\r\n" +
                                oneCat.walk() + "\r\n" + oneDog.walk() + "\r\n" + oneSheep.walk() + "\r\n" + onePig.walk() + "\r\n" + "\r\n" +
                                oneCat.sleep() + "\r\n" + oneDog.sleep() + "\r\n" + oneSheep.sleep() + "\r\n" + onePig.sleep());
    }

}

package work.ready.examples.ioc_aop;

import work.ready.examples.ioc_aop.service.Animal;
import work.ready.examples.ioc_aop.service.Pig;
import work.ready.examples.ioc_aop.service.Sheep;
import work.ready.core.ioc.annotation.Bean;
import work.ready.core.ioc.annotation.Configuration;

@Configuration
public class MyConfiguration {

    @Bean(name={"ram","ewe","lamb","sheep"}, initMethod = "initMethod", destroyMethod = "destroyMethod")
    public Animal sheep(){
        return new Sheep();
    }

    @Bean
    public Pig pig(){
        return new AdvancedPig();
    }

    static class AdvancedPig extends Pig {
        String type = "advanced pig";

        @Override
        public String type() {
            return type;
        }
    }
}

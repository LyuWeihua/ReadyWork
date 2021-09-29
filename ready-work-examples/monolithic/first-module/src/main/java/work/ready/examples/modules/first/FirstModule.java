package work.ready.examples.modules.first;


import work.ready.core.log.Log;
import work.ready.core.log.LogFactory;
import work.ready.core.module.AppModule;
import work.ready.core.server.Ready;

import java.util.Map;

public class FirstModule extends AppModule {

    private static final Log logger = LogFactory.getLog(FirstModule.class);

    public static final String CONFIG_NAME = FirstModule.class.getSimpleName();

    public FirstModule(){
        logger.info("FirstModule is enabled.");
    }

    public Map<String, Object> getConfig() {
        return Ready.config().getMapConfig(CONFIG_NAME);
    }

    @Override
    protected void initialize() {
        logger.info("FirstModule is initializing ...");
        //config() can get simple module config from appModule paragraph of application config
        //or use independent config file for complex modules
        //Map config =  Ready.getConfig().getMapConfig(CONFIG_NAME);
        // merge config items from application config and independent config file
        Ready.config().getMapConfig(CONFIG_NAME).putAll(config());
    }

    @Override
    protected void destroy(){
        logger.warn("FirstModule is destroying  ...");
    }

    @Override
    protected boolean verifyInstallation(){
        return true;
    }

    @Override
    protected void install(){
        logger.warn("FirstModule is installing  ...");
    }

    @Override
    protected void uninstall(){
        logger.warn("FirstModule is uninstalling  ...");
    }

    @Override
    protected void upgrade() {
        logger.warn("FirstModule is upgrading  ...");
    }

}

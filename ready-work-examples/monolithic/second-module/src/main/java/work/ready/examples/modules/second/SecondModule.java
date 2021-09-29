package work.ready.examples.modules.second;


import work.ready.core.log.Log;
import work.ready.core.log.LogFactory;
import work.ready.core.module.AppModule;
import work.ready.core.server.Ready;

import java.util.Map;

public class SecondModule extends AppModule {

    private static final Log logger = LogFactory.getLog(SecondModule.class);

    public static final String CONFIG_NAME = SecondModule.class.getSimpleName();

    public SecondModule(){
        logger.info("SecondModule is enabled.");
    }

    public Map<String, Object> getConfig() {
        return Ready.config().getMapConfig(CONFIG_NAME);
    }

    @Override
    protected void initialize() {
        logger.info("SecondModule is initializing ...");
        //config() can get simple module config from appModule paragraph of application config
        //or use independent config file for complex modules
        //Map config =  Ready.getConfig().getMapConfig(CONFIG_NAME);
        // merge config items from application config and independent config file
        Ready.config().getMapConfig(CONFIG_NAME).putAll(config());
    }

    @Override
    protected void destroy(){
        logger.warn("SecondModule is destroying  ...");
    }

    @Override
    protected boolean verifyInstallation(){
        return false;
    }

    @Override
    protected void install(){
        logger.warn("SecondModule is installing  ...");
    }

    @Override
    protected void uninstall(){
        logger.warn("SecondModule is uninstalling  ...");
    }

    @Override
    protected void upgrade() {
        logger.warn("SecondModule is upgrading  ...");
    }

}

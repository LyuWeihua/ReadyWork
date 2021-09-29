package work.ready.examples.logger;

import work.ready.core.handler.Controller;
import work.ready.core.handler.route.RequestMapping;
import work.ready.core.log.Log;
import work.ready.core.log.LogFactory;
import work.ready.core.service.result.Result;
import work.ready.core.service.result.Success;

@RequestMapping(value = "/")
public class LoggerController extends Controller {

    private static final Log logger = LogFactory.getLog(LoggerController.class);

    @RequestMapping
    public Result<String> index() {
        // ... logger.trace/debug/info/warn/error
        testTrace();
        testDebug();
        testInfo();
        testWarn();
        testError();
        return Success.of("hello world !");
    }

    private void testTrace() {
        logger.trace("trace message.");
        logger.trace("trace message with one parameter x = %s.", 1);
        logger.trace("trace message with two parameter y = %s + %s.", 1, 1);
        logger.trace(()->{
            int i = 0;
            int j = 1;
            return "trace message with callback Lambda.";
        });
        try{
            throw new RuntimeException();
        } catch (RuntimeException e) {
            logger.trace(e, "trace message with Exception");
            logger.trace(e, "trace message with Exception and two parameter y = %s + %s.", 1, 1);
            logger.trace(e, ()->{
                int i = 0;
                int j = 1;
                return "trace message with Exception and callback Lambda.";
            });
        }
    }

    private void testDebug() {
        logger.debug("debug message.");
        logger.debug("debug message with one parameter x = %s.", 1);
        logger.debug("debug message with two parameter y = %s + %s.", 1, 1);
        logger.debug(()->{
            int i = 0;
            int j = 1;
            return "debug message with callback Lambda.";
        });
        try{
            throw new RuntimeException();
        } catch (RuntimeException e) {
            logger.debug(e, "debug message with Exception");
            logger.debug(e, "debug message with Exception and two parameter y = %s + %s.", 1, 1);
            logger.debug(e, ()->{
                int i = 0;
                int j = 1;
                return "debug message with Exception and callback Lambda.";
            });
        }
    }

    private void testInfo() {
        logger.info("info message.");
        logger.info("info message with one parameter x = %s.", 1);
        logger.info("info message with two parameter y = %s + %s.", 1, 1);
        logger.info(()->{
            int i = 0;
            int j = 1;
            return "info message with callback Lambda.";
        });
        try{
            throw new RuntimeException();
        } catch (RuntimeException e) {
            logger.info(e, "info message with Exception");
            logger.info(e, "info message with Exception and two parameter y = %s + %s.", 1, 1);
            logger.info(e, ()->{
                int i = 0;
                int j = 1;
                return "info message with Exception and callback Lambda.";
            });
        }
    }

    private void testWarn() {
        logger.warn("warn message.");
        logger.warn("warn message with one parameter x = %s.", 1);
        logger.warn("warn message with two parameter y = %s + %s.", 1, 1);
        logger.warn(()->{
            int i = 0;
            int j = 1;
            return "warn message with callback Lambda.";
        });
        try{
            throw new RuntimeException();
        } catch (RuntimeException e) {
            logger.warn(e, "warn message with Exception");
            logger.warn(e, "warn message with Exception and two parameter y = %s + %s.", 1, 1);
            logger.warn(e, ()->{
                int i = 0;
                int j = 1;
                return "warn message with Exception and callback Lambda.";
            });
        }
    }

    private void testError() {
        logger.error("error message.");
        logger.error("error message with one parameter x = %s.", 1);
        logger.error("error message with two parameter y = %s + %s.", 1, 1);
        logger.error(()->{
            int i = 0;
            int j = 1;
            return "error message with callback Lambda.";
        });
        try{
            throw new RuntimeException();
        } catch (RuntimeException e) {
            logger.error(e, "error message with Exception");
            logger.error(e, "error message with Exception and two parameter y = %s + %s.", 1, 1);
            logger.error(e, ()->{
                int i = 0;
                int j = 1;
                return "error message with Exception and callback Lambda.";
            });
        }
    }
}

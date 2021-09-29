package work.ready.examples.time_uuid;

import work.ready.core.handler.Controller;
import work.ready.core.handler.route.RequestMapping;
import work.ready.core.server.Ready;
import work.ready.core.service.result.Result;
import work.ready.core.service.result.Success;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.Date;

@RequestMapping("/time")
public class TimeController extends Controller {

    @RequestMapping
    public Result<Date> index() {
        return Success.of(Ready.now());
    }

    @RequestMapping
    public Result<Instant> instant() {
        return Success.of(Ready.instant());
    }

    @RequestMapping
    public Result<Long> millis() {
        return Success.of(Ready.currentTimeMillis());
    }

    @RequestMapping
    public Result<LocalDateTime> localDateTime() {
        return Success.of(Ready.localDateTime());
    }

    @RequestMapping
    public Result<ZonedDateTime> zonedDateTime() {
        return Success.of(Ready.zonedDateTime());
    }

    @RequestMapping
    public Result<Boolean> syncTime() {
        return Success.of(Ready.syncTime());
    }

}

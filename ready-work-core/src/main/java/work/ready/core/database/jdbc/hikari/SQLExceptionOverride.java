package work.ready.core.database.jdbc.hikari;

import java.sql.SQLException;

public interface SQLExceptionOverride {
   enum Override {
      CONTINUE_EVICT,
      DO_NOT_EVICT
   }

   default Override adjudicate(final SQLException sqlException)
   {
      return Override.CONTINUE_EVICT;
   }
}

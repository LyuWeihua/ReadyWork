package work.ready.examples.database.event;

import work.ready.core.database.jdbc.common.StatementInformation;
import work.ready.core.database.jdbc.event.SimpleJdbcEventListener;

import java.sql.SQLException;

public class JdbcEventListener extends SimpleJdbcEventListener {

    @Override
    public String onBeforeAnyExecute(StatementInformation statementInformation) throws SQLException
    {
        System.out.println("JdbcEvent: ==> SQL: " + statementInformation.getStatementQuery());
        return statementInformation.getStatementQuery();
    }

    @Override
    public void onAfterAnyExecute(StatementInformation statementInformation, long timeElapsedNanos, SQLException e)
    {
        System.out.println("JdbcEvent: ==> Time elapsed: " + statementInformation.getTotalTimeElapsed());
    }
}

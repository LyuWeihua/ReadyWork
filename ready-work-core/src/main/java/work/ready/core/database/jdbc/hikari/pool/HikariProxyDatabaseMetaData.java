package work.ready.core.database.jdbc.hikari.pool;

import java.lang.Class;
import java.lang.Override;
import java.lang.String;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.RowIdLifetime;
import java.sql.SQLException;
import java.sql.Wrapper;

public final class HikariProxyDatabaseMetaData extends ProxyDatabaseMetaData implements Wrapper, DatabaseMetaData {
    HikariProxyDatabaseMetaData(ProxyConnection connection, DatabaseMetaData metaData) {
        super(connection, metaData);
    }

    @Override
    public boolean isWrapperFor(Class<?> arg0) throws SQLException {
        try { return delegate.isWrapperFor(arg0); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public ResultSet getAttributes(String arg0, String arg1, String arg2, String arg3) throws
            SQLException {
        try { return super.getAttributes(arg0, arg1, arg2, arg3); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public boolean isReadOnly() throws SQLException {
        try { return delegate.isReadOnly(); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public int getResultSetHoldability() throws SQLException {
        try { return delegate.getResultSetHoldability(); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public ResultSet getTables(String arg0, String arg1, String arg2, String[] arg3) throws
            SQLException {
        try { return super.getTables(arg0, arg1, arg2, arg3); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public ResultSet getSchemas(String arg0, String arg1) throws SQLException {
        try { return super.getSchemas(arg0, arg1); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public ResultSet getSchemas() throws SQLException {
        try { return super.getSchemas(); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public boolean allProceduresAreCallable() throws SQLException {
        try { return delegate.allProceduresAreCallable(); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public boolean allTablesAreSelectable() throws SQLException {
        try { return delegate.allTablesAreSelectable(); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public String getUserName() throws SQLException {
        try { return delegate.getUserName(); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public boolean nullsAreSortedHigh() throws SQLException {
        try { return delegate.nullsAreSortedHigh(); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public boolean nullsAreSortedLow() throws SQLException {
        try { return delegate.nullsAreSortedLow(); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public boolean nullsAreSortedAtStart() throws SQLException {
        try { return delegate.nullsAreSortedAtStart(); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public boolean nullsAreSortedAtEnd() throws SQLException {
        try { return delegate.nullsAreSortedAtEnd(); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public String getDatabaseProductName() throws SQLException {
        try { return delegate.getDatabaseProductName(); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public String getDatabaseProductVersion() throws SQLException {
        try { return delegate.getDatabaseProductVersion(); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public String getDriverName() throws SQLException {
        try { return delegate.getDriverName(); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public String getDriverVersion() throws SQLException {
        try { return delegate.getDriverVersion(); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public int getDriverMajorVersion() {
        return ((DatabaseMetaData) delegate).getDriverMajorVersion();
    }

    @Override
    public int getDriverMinorVersion() {
        return ((DatabaseMetaData) delegate).getDriverMinorVersion();
    }

    @Override
    public boolean usesLocalFiles() throws SQLException {
        try { return delegate.usesLocalFiles(); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public boolean usesLocalFilePerTable() throws SQLException {
        try { return delegate.usesLocalFilePerTable(); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public boolean supportsMixedCaseIdentifiers() throws SQLException {
        try { return delegate.supportsMixedCaseIdentifiers(); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public boolean storesUpperCaseIdentifiers() throws SQLException {
        try { return delegate.storesUpperCaseIdentifiers(); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public boolean storesLowerCaseIdentifiers() throws SQLException {
        try { return delegate.storesLowerCaseIdentifiers(); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public boolean storesMixedCaseIdentifiers() throws SQLException {
        try { return delegate.storesMixedCaseIdentifiers(); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public boolean supportsMixedCaseQuotedIdentifiers() throws SQLException {
        try { return delegate.supportsMixedCaseQuotedIdentifiers(); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public boolean storesUpperCaseQuotedIdentifiers() throws SQLException {
        try { return delegate.storesUpperCaseQuotedIdentifiers(); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public boolean storesLowerCaseQuotedIdentifiers() throws SQLException {
        try { return delegate.storesLowerCaseQuotedIdentifiers(); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public boolean storesMixedCaseQuotedIdentifiers() throws SQLException {
        try { return delegate.storesMixedCaseQuotedIdentifiers(); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public String getIdentifierQuoteString() throws SQLException {
        try { return delegate.getIdentifierQuoteString(); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public String getSQLKeywords() throws SQLException {
        try { return delegate.getSQLKeywords(); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public String getNumericFunctions() throws SQLException {
        try { return delegate.getNumericFunctions(); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public String getStringFunctions() throws SQLException {
        try { return delegate.getStringFunctions(); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public String getSystemFunctions() throws SQLException {
        try { return delegate.getSystemFunctions(); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public String getTimeDateFunctions() throws SQLException {
        try { return delegate.getTimeDateFunctions(); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public String getSearchStringEscape() throws SQLException {
        try { return delegate.getSearchStringEscape(); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public String getExtraNameCharacters() throws SQLException {
        try { return delegate.getExtraNameCharacters(); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public boolean supportsAlterTableWithAddColumn() throws SQLException {
        try { return delegate.supportsAlterTableWithAddColumn(); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public boolean supportsAlterTableWithDropColumn() throws SQLException {
        try { return delegate.supportsAlterTableWithDropColumn(); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public boolean supportsColumnAliasing() throws SQLException {
        try { return delegate.supportsColumnAliasing(); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public boolean nullPlusNonNullIsNull() throws SQLException {
        try { return delegate.nullPlusNonNullIsNull(); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public boolean supportsConvert() throws SQLException {
        try { return delegate.supportsConvert(); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public boolean supportsConvert(int arg0, int arg1) throws SQLException {
        try { return delegate.supportsConvert(arg0, arg1); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public boolean supportsTableCorrelationNames() throws SQLException {
        try { return delegate.supportsTableCorrelationNames(); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public boolean supportsDifferentTableCorrelationNames() throws SQLException {
        try { return delegate.supportsDifferentTableCorrelationNames(); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public boolean supportsExpressionsInOrderBy() throws SQLException {
        try { return delegate.supportsExpressionsInOrderBy(); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public boolean supportsOrderByUnrelated() throws SQLException {
        try { return delegate.supportsOrderByUnrelated(); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public boolean supportsGroupBy() throws SQLException {
        try { return delegate.supportsGroupBy(); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public boolean supportsGroupByUnrelated() throws SQLException {
        try { return delegate.supportsGroupByUnrelated(); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public boolean supportsGroupByBeyondSelect() throws SQLException {
        try { return delegate.supportsGroupByBeyondSelect(); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public boolean supportsLikeEscapeClause() throws SQLException {
        try { return delegate.supportsLikeEscapeClause(); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public boolean supportsMultipleResultSets() throws SQLException {
        try { return delegate.supportsMultipleResultSets(); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public boolean supportsMultipleTransactions() throws SQLException {
        try { return delegate.supportsMultipleTransactions(); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public boolean supportsNonNullableColumns() throws SQLException {
        try { return delegate.supportsNonNullableColumns(); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public boolean supportsMinimumSQLGrammar() throws SQLException {
        try { return delegate.supportsMinimumSQLGrammar(); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public boolean supportsCoreSQLGrammar() throws SQLException {
        try { return delegate.supportsCoreSQLGrammar(); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public boolean supportsExtendedSQLGrammar() throws SQLException {
        try { return delegate.supportsExtendedSQLGrammar(); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public boolean supportsANSI92EntryLevelSQL() throws SQLException {
        try { return delegate.supportsANSI92EntryLevelSQL(); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public boolean supportsANSI92IntermediateSQL() throws SQLException {
        try { return delegate.supportsANSI92IntermediateSQL(); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public boolean supportsANSI92FullSQL() throws SQLException {
        try { return delegate.supportsANSI92FullSQL(); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public boolean supportsIntegrityEnhancementFacility() throws SQLException {
        try { return delegate.supportsIntegrityEnhancementFacility(); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public boolean supportsOuterJoins() throws SQLException {
        try { return delegate.supportsOuterJoins(); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public boolean supportsFullOuterJoins() throws SQLException {
        try { return delegate.supportsFullOuterJoins(); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public boolean supportsLimitedOuterJoins() throws SQLException {
        try { return delegate.supportsLimitedOuterJoins(); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public String getSchemaTerm() throws SQLException {
        try { return delegate.getSchemaTerm(); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public String getProcedureTerm() throws SQLException {
        try { return delegate.getProcedureTerm(); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public String getCatalogTerm() throws SQLException {
        try { return delegate.getCatalogTerm(); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public boolean isCatalogAtStart() throws SQLException {
        try { return delegate.isCatalogAtStart(); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public String getCatalogSeparator() throws SQLException {
        try { return delegate.getCatalogSeparator(); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public boolean supportsSchemasInDataManipulation() throws SQLException {
        try { return delegate.supportsSchemasInDataManipulation(); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public boolean supportsSchemasInProcedureCalls() throws SQLException {
        try { return delegate.supportsSchemasInProcedureCalls(); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public boolean supportsSchemasInTableDefinitions() throws SQLException {
        try { return delegate.supportsSchemasInTableDefinitions(); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public boolean supportsSchemasInIndexDefinitions() throws SQLException {
        try { return delegate.supportsSchemasInIndexDefinitions(); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public boolean supportsSchemasInPrivilegeDefinitions() throws SQLException {
        try { return delegate.supportsSchemasInPrivilegeDefinitions(); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public boolean supportsCatalogsInDataManipulation() throws SQLException {
        try { return delegate.supportsCatalogsInDataManipulation(); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public boolean supportsCatalogsInProcedureCalls() throws SQLException {
        try { return delegate.supportsCatalogsInProcedureCalls(); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public boolean supportsCatalogsInTableDefinitions() throws SQLException {
        try { return delegate.supportsCatalogsInTableDefinitions(); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public boolean supportsCatalogsInIndexDefinitions() throws SQLException {
        try { return delegate.supportsCatalogsInIndexDefinitions(); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public boolean supportsCatalogsInPrivilegeDefinitions() throws SQLException {
        try { return delegate.supportsCatalogsInPrivilegeDefinitions(); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public boolean supportsPositionedDelete() throws SQLException {
        try { return delegate.supportsPositionedDelete(); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public boolean supportsPositionedUpdate() throws SQLException {
        try { return delegate.supportsPositionedUpdate(); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public boolean supportsSelectForUpdate() throws SQLException {
        try { return delegate.supportsSelectForUpdate(); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public boolean supportsStoredProcedures() throws SQLException {
        try { return delegate.supportsStoredProcedures(); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public boolean supportsSubqueriesInComparisons() throws SQLException {
        try { return delegate.supportsSubqueriesInComparisons(); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public boolean supportsSubqueriesInExists() throws SQLException {
        try { return delegate.supportsSubqueriesInExists(); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public boolean supportsSubqueriesInIns() throws SQLException {
        try { return delegate.supportsSubqueriesInIns(); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public boolean supportsSubqueriesInQuantifieds() throws SQLException {
        try { return delegate.supportsSubqueriesInQuantifieds(); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public boolean supportsCorrelatedSubqueries() throws SQLException {
        try { return delegate.supportsCorrelatedSubqueries(); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public boolean supportsUnion() throws SQLException {
        try { return delegate.supportsUnion(); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public boolean supportsUnionAll() throws SQLException {
        try { return delegate.supportsUnionAll(); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public boolean supportsOpenCursorsAcrossCommit() throws SQLException {
        try { return delegate.supportsOpenCursorsAcrossCommit(); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public boolean supportsOpenCursorsAcrossRollback() throws SQLException {
        try { return delegate.supportsOpenCursorsAcrossRollback(); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public boolean supportsOpenStatementsAcrossCommit() throws SQLException {
        try { return delegate.supportsOpenStatementsAcrossCommit(); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public boolean supportsOpenStatementsAcrossRollback() throws SQLException {
        try { return delegate.supportsOpenStatementsAcrossRollback(); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public int getMaxBinaryLiteralLength() throws SQLException {
        try { return delegate.getMaxBinaryLiteralLength(); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public int getMaxCharLiteralLength() throws SQLException {
        try { return delegate.getMaxCharLiteralLength(); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public int getMaxColumnNameLength() throws SQLException {
        try { return delegate.getMaxColumnNameLength(); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public int getMaxColumnsInGroupBy() throws SQLException {
        try { return delegate.getMaxColumnsInGroupBy(); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public int getMaxColumnsInIndex() throws SQLException {
        try { return delegate.getMaxColumnsInIndex(); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public int getMaxColumnsInOrderBy() throws SQLException {
        try { return delegate.getMaxColumnsInOrderBy(); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public int getMaxColumnsInSelect() throws SQLException {
        try { return delegate.getMaxColumnsInSelect(); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public int getMaxColumnsInTable() throws SQLException {
        try { return delegate.getMaxColumnsInTable(); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public int getMaxConnections() throws SQLException {
        try { return delegate.getMaxConnections(); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public int getMaxCursorNameLength() throws SQLException {
        try { return delegate.getMaxCursorNameLength(); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public int getMaxIndexLength() throws SQLException {
        try { return delegate.getMaxIndexLength(); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public int getMaxSchemaNameLength() throws SQLException {
        try { return delegate.getMaxSchemaNameLength(); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public int getMaxProcedureNameLength() throws SQLException {
        try { return delegate.getMaxProcedureNameLength(); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public int getMaxCatalogNameLength() throws SQLException {
        try { return delegate.getMaxCatalogNameLength(); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public int getMaxRowSize() throws SQLException {
        try { return delegate.getMaxRowSize(); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public boolean doesMaxRowSizeIncludeBlobs() throws SQLException {
        try { return delegate.doesMaxRowSizeIncludeBlobs(); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public int getMaxStatementLength() throws SQLException {
        try { return delegate.getMaxStatementLength(); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public int getMaxStatements() throws SQLException {
        try { return delegate.getMaxStatements(); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public int getMaxTableNameLength() throws SQLException {
        try { return delegate.getMaxTableNameLength(); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public int getMaxTablesInSelect() throws SQLException {
        try { return delegate.getMaxTablesInSelect(); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public int getMaxUserNameLength() throws SQLException {
        try { return delegate.getMaxUserNameLength(); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public int getDefaultTransactionIsolation() throws SQLException {
        try { return delegate.getDefaultTransactionIsolation(); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public boolean supportsTransactions() throws SQLException {
        try { return delegate.supportsTransactions(); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public boolean supportsTransactionIsolationLevel(int arg0) throws SQLException {
        try { return delegate.supportsTransactionIsolationLevel(arg0); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public boolean supportsDataDefinitionAndDataManipulationTransactions() throws SQLException {
        try { return delegate.supportsDataDefinitionAndDataManipulationTransactions(); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public boolean supportsDataManipulationTransactionsOnly() throws SQLException {
        try { return delegate.supportsDataManipulationTransactionsOnly(); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public boolean dataDefinitionCausesTransactionCommit() throws SQLException {
        try { return delegate.dataDefinitionCausesTransactionCommit(); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public boolean dataDefinitionIgnoredInTransactions() throws SQLException {
        try { return delegate.dataDefinitionIgnoredInTransactions(); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public ResultSet getProcedures(String arg0, String arg1, String arg2) throws SQLException {
        try { return super.getProcedures(arg0, arg1, arg2); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public ResultSet getProcedureColumns(String arg0, String arg1, String arg2, String arg3) throws
            SQLException {
        try { return super.getProcedureColumns(arg0, arg1, arg2, arg3); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public ResultSet getCatalogs() throws SQLException {
        try { return super.getCatalogs(); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public ResultSet getTableTypes() throws SQLException {
        try { return super.getTableTypes(); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public ResultSet getColumns(String arg0, String arg1, String arg2, String arg3) throws
            SQLException {
        try { return super.getColumns(arg0, arg1, arg2, arg3); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public ResultSet getColumnPrivileges(String arg0, String arg1, String arg2, String arg3) throws
            SQLException {
        try { return super.getColumnPrivileges(arg0, arg1, arg2, arg3); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public ResultSet getTablePrivileges(String arg0, String arg1, String arg2) throws SQLException {
        try { return super.getTablePrivileges(arg0, arg1, arg2); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public ResultSet getBestRowIdentifier(String arg0, String arg1, String arg2, int arg3,
            boolean arg4) throws SQLException {
        try { return super.getBestRowIdentifier(arg0, arg1, arg2, arg3, arg4); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public ResultSet getVersionColumns(String arg0, String arg1, String arg2) throws SQLException {
        try { return super.getVersionColumns(arg0, arg1, arg2); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public ResultSet getPrimaryKeys(String arg0, String arg1, String arg2) throws SQLException {
        try { return super.getPrimaryKeys(arg0, arg1, arg2); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public ResultSet getImportedKeys(String arg0, String arg1, String arg2) throws SQLException {
        try { return super.getImportedKeys(arg0, arg1, arg2); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public ResultSet getExportedKeys(String arg0, String arg1, String arg2) throws SQLException {
        try { return super.getExportedKeys(arg0, arg1, arg2); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public ResultSet getCrossReference(String arg0, String arg1, String arg2, String arg3,
            String arg4, String arg5) throws SQLException {
        try { return super.getCrossReference(arg0, arg1, arg2, arg3, arg4, arg5); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public ResultSet getTypeInfo() throws SQLException {
        try { return super.getTypeInfo(); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public ResultSet getIndexInfo(String arg0, String arg1, String arg2, boolean arg3, boolean arg4)
            throws SQLException {
        try { return super.getIndexInfo(arg0, arg1, arg2, arg3, arg4); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public boolean supportsResultSetType(int arg0) throws SQLException {
        try { return delegate.supportsResultSetType(arg0); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public boolean supportsResultSetConcurrency(int arg0, int arg1) throws SQLException {
        try { return delegate.supportsResultSetConcurrency(arg0, arg1); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public boolean ownUpdatesAreVisible(int arg0) throws SQLException {
        try { return delegate.ownUpdatesAreVisible(arg0); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public boolean ownDeletesAreVisible(int arg0) throws SQLException {
        try { return delegate.ownDeletesAreVisible(arg0); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public boolean ownInsertsAreVisible(int arg0) throws SQLException {
        try { return delegate.ownInsertsAreVisible(arg0); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public boolean othersUpdatesAreVisible(int arg0) throws SQLException {
        try { return delegate.othersUpdatesAreVisible(arg0); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public boolean othersDeletesAreVisible(int arg0) throws SQLException {
        try { return delegate.othersDeletesAreVisible(arg0); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public boolean othersInsertsAreVisible(int arg0) throws SQLException {
        try { return delegate.othersInsertsAreVisible(arg0); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public boolean updatesAreDetected(int arg0) throws SQLException {
        try { return delegate.updatesAreDetected(arg0); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public boolean deletesAreDetected(int arg0) throws SQLException {
        try { return delegate.deletesAreDetected(arg0); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public boolean insertsAreDetected(int arg0) throws SQLException {
        try { return delegate.insertsAreDetected(arg0); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public boolean supportsBatchUpdates() throws SQLException {
        try { return delegate.supportsBatchUpdates(); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public ResultSet getUDTs(String arg0, String arg1, String arg2, int[] arg3) throws
            SQLException {
        try { return super.getUDTs(arg0, arg1, arg2, arg3); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public boolean supportsSavepoints() throws SQLException {
        try { return delegate.supportsSavepoints(); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public boolean supportsNamedParameters() throws SQLException {
        try { return delegate.supportsNamedParameters(); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public boolean supportsMultipleOpenResults() throws SQLException {
        try { return delegate.supportsMultipleOpenResults(); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public boolean supportsGetGeneratedKeys() throws SQLException {
        try { return delegate.supportsGetGeneratedKeys(); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public ResultSet getSuperTypes(String arg0, String arg1, String arg2) throws SQLException {
        try { return super.getSuperTypes(arg0, arg1, arg2); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public ResultSet getSuperTables(String arg0, String arg1, String arg2) throws SQLException {
        try { return super.getSuperTables(arg0, arg1, arg2); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public boolean supportsResultSetHoldability(int arg0) throws SQLException {
        try { return delegate.supportsResultSetHoldability(arg0); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public int getDatabaseMajorVersion() throws SQLException {
        try { return delegate.getDatabaseMajorVersion(); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public int getDatabaseMinorVersion() throws SQLException {
        try { return delegate.getDatabaseMinorVersion(); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public int getJDBCMajorVersion() throws SQLException {
        try { return delegate.getJDBCMajorVersion(); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public int getJDBCMinorVersion() throws SQLException {
        try { return delegate.getJDBCMinorVersion(); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public int getSQLStateType() throws SQLException {
        try { return delegate.getSQLStateType(); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public boolean locatorsUpdateCopy() throws SQLException {
        try { return delegate.locatorsUpdateCopy(); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public boolean supportsStatementPooling() throws SQLException {
        try { return delegate.supportsStatementPooling(); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public RowIdLifetime getRowIdLifetime() throws SQLException {
        try { return delegate.getRowIdLifetime(); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public boolean supportsStoredFunctionsUsingCallSyntax() throws SQLException {
        try { return delegate.supportsStoredFunctionsUsingCallSyntax(); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public boolean autoCommitFailureClosesAllResultSets() throws SQLException {
        try { return delegate.autoCommitFailureClosesAllResultSets(); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public ResultSet getClientInfoProperties() throws SQLException {
        try { return super.getClientInfoProperties(); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public ResultSet getFunctions(String arg0, String arg1, String arg2) throws SQLException {
        try { return super.getFunctions(arg0, arg1, arg2); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public ResultSet getFunctionColumns(String arg0, String arg1, String arg2, String arg3) throws
            SQLException {
        try { return super.getFunctionColumns(arg0, arg1, arg2, arg3); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public ResultSet getPseudoColumns(String arg0, String arg1, String arg2, String arg3) throws
            SQLException {
        try { return super.getPseudoColumns(arg0, arg1, arg2, arg3); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public boolean generatedKeyAlwaysReturned() throws SQLException {
        try { return delegate.generatedKeyAlwaysReturned(); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public long getMaxLogicalLobSize() throws SQLException {
        try { return delegate.getMaxLogicalLobSize(); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public boolean supportsRefCursors() throws SQLException {
        try { return delegate.supportsRefCursors(); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public boolean supportsSharding() throws SQLException {
        try { return delegate.supportsSharding(); } catch (SQLException e) { throw checkException(e); }
    }

    @Override
    public String getURL() throws SQLException {
        try { return delegate.getURL(); } catch (SQLException e) { throw checkException(e); }
    }
}

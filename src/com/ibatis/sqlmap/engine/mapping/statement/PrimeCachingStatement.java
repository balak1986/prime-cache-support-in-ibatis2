package com.ibatis.sqlmap.engine.mapping.statement;

import java.lang.reflect.Field;
import java.sql.SQLException;
import java.util.List;

import com.ibatis.common.jdbc.exception.NestedSQLException;
import com.ibatis.sqlmap.engine.cache.CacheKey;
import com.ibatis.sqlmap.engine.cache.CacheModel;
import com.ibatis.sqlmap.engine.scope.ErrorContext;
import com.ibatis.sqlmap.engine.scope.SessionScope;
import com.ibatis.sqlmap.engine.scope.StatementScope;
import com.ibatis.sqlmap.engine.transaction.Transaction;

public class PrimeCachingStatement extends CachingStatement {

    private MappedStatement primedCacheMappedStatement;
    private String keyProperty;

    public PrimeCachingStatement(MappedStatement statement,
	    CacheModel cacheModel, MappedStatement primedCacheMappedStatement,
	    String keyProperty) {
	super(statement, cacheModel);
	this.primedCacheMappedStatement = primedCacheMappedStatement;
	this.keyProperty = keyProperty;
    }

    public void prime(StatementScope statementScope, Object parameterObject,
	    Transaction trans) throws SQLException {
	ErrorContext errorContext = statementScope.getErrorContext();
	// Loading the prime cache for the first time
	SessionScope sessionScopeForPrimeQuery = statementScope.getSession();
	StatementScope statementScopeForPrimeQuery = new StatementScope(
		sessionScopeForPrimeQuery);
	primedCacheMappedStatement.initRequest(statementScopeForPrimeQuery);
	List list = primedCacheMappedStatement
		.executeQueryForList(statementScopeForPrimeQuery, trans,
			parameterObject, 0, -999999);
	// Populating cache for each specific keys
	for (Object obj : list) {
	    try {
		CacheKey specificKey = newSpecificKey(obj, statementScope);
		cacheModel.putObject(specificKey, obj);
	    } catch (Exception exception) {
		errorContext.setCause(exception);
		throw new NestedSQLException(errorContext.toString(), exception);
	    }
	}
    }

    /**
     * Returns a specific key based on a domain object.
     * 
     * @throws Exception
     */
    public CacheKey newSpecificKey(Object domainObject,
	    StatementScope statementScope) throws Exception {
	Class clazz = domainObject.getClass();
	Field field = clazz.getDeclaredField(keyProperty);
	field.setAccessible(true);
	Object parameterObject = field.get(domainObject);
	CacheKey specificKey = getCacheKey(statementScope, parameterObject);
	specificKey.update("executeQueryForObject");
	return specificKey;
    }
}

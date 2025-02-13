package com.dataflow.dataingestionservice.Utils;

import org.hibernate.resource.jdbc.spi.StatementInspector;

public class SqlStatementInspector implements StatementInspector {
    @Override
    public String inspect(String sql) {
        System.out.println("Hibernate SQL: " + sql);
        return sql;
    }
}

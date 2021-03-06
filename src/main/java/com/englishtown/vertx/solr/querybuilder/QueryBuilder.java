package com.englishtown.vertx.solr.querybuilder;

import com.englishtown.vertx.solr.VertxSolrQuery;

/**
 */
public interface QueryBuilder {
    QueryBuilder query(Query query);

    QueryBuilder filterQuery(Term... terms);

    QueryBuilder showDebugInfo();

    QueryBuilder dateBoost(String fieldName, DateOrder dateOrder);

    QueryBuilder dateBoost(String fieldName, DateOrder dateOrder, int multiplier);

    QueryBuilder rows(int numRows);

    VertxSolrQuery build();

    enum DateOrder {
        ASCENDING,
        DESCENDING;
    }
}

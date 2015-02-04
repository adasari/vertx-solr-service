package com.englishtown.vertx.solr.impl;

import com.englishtown.vertx.solr.QueryOptions;
import com.englishtown.vertx.solr.SolrConfigurator;
import com.englishtown.vertx.solr.SolrService;
import com.englishtown.vertx.solr.VertxSolrQuery;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.impl.HttpClientUtil;
import org.apache.solr.client.solrj.request.QueryRequest;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;

import javax.inject.Inject;
import java.util.Date;
import java.util.List;

/**
 * Default implementation of {@link com.englishtown.vertx.solr.SolrService}
 */
public class DefaultSolrService implements SolrService {

    private final SolrConfigurator configurator;
    private SolrServer solrServer;

    @Inject
    public DefaultSolrService(SolrConfigurator configurator) {
        this.configurator = configurator;
    }

    @Override
    public void start() {
        solrServer = configurator.createSolrServer();
    }

    @Override
    public void stop() {
        if (solrServer != null) {
            solrServer.shutdown();
            solrServer = null;
        }
    }

    @Override
    public void query(JsonObject query, QueryOptions options, Handler<AsyncResult<JsonObject>> resultHandler) {
        query(new VertxSolrQuery(query), options, resultHandler);
    }

    @Override
    public void query(VertxSolrQuery query, QueryOptions options, Handler<AsyncResult<JsonObject>> resultHandler) {

        QueryResponse response;

        if (options == null) {
            options = new QueryOptions();
        } else {
            if (options.getBasicAuthUser() != null && options.getBasicAuthPass() != null) {
                query.set(HttpClientUtil.PROP_BASIC_AUTH_USER, options.getBasicAuthUser());
                query.set(HttpClientUtil.PROP_BASIC_AUTH_PASS, options.getBasicAuthPass());
            }
        }

        try {
            QueryRequest request = new QueryRequest(query);
            if (options.getCore() != null) {
                request.setPath("/" + options.getCore() + "/select");
            }
            response = request.process(solrServer);
        } catch (Throwable t) {
            resultHandler.handle(Future.failedFuture(t));
            return;
        }

        SolrDocumentList results = response.getResults();

        JsonArray docs = new JsonArray();
        for (SolrDocument result : results) {
            JsonObject doc = new JsonObject();

            for (String key : result.keySet()) {
                doc.put(key, getJsonValue(result.getFieldValue(key)));
            }

            docs.add(doc);
        }

        JsonObject reply = new JsonObject()
                .put("max_score", results.getMaxScore())
                .put("number_found", results.getNumFound())
                .put("start", results.getStart())
                .put("docs", docs);

        // Solr CursorsMarks are supported as of version 4.7.0
        if (response.getNextCursorMark() != null && !response.getNextCursorMark().isEmpty()) {
            reply.put("next_cursor_mark", response.getNextCursorMark());
        }
        resultHandler.handle(Future.succeededFuture(reply));
    }

    private Object getJsonValue(Object val) {

        if (val instanceof Date) {
            return ((Date) val).getTime();
        }

        if (val instanceof List) {
            JsonArray arr = new JsonArray();
            for (Object v : (Iterable) val) {
                arr.add(getJsonValue(v));
            }
            return arr;
        }

        return val;

    }

}

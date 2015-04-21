package be.proxml.module.sparql;

import org.netkernel.layer0.nkf.*;
import org.netkernel.layer0.representation.IHDSNode;
import org.netkernel.layer0.representation.impl.HDSBuilder;
import org.netkernel.module.standard.endpoint.StandardAccessorImpl;
import java.net.URLEncoder;

/**
 * Created by yyz on 4/14/15.
 */
public class KeywordSearchAccessor extends StandardAccessorImpl{
    public void onSource(INKFRequestContext context) throws Exception
    {
        INKFResponseReadOnly response;
        INKFRequest request;
        boolean isHTTPRequest = true;
        String query;
        if (context.exists("arg:query")) {
            query = context.source("arg:query", String.class);
            isHTTPRequest = false;
        }
        else if (context.exists("httpRequest:/postparam/query"))
            query = context.source("httpRequest:/postparam/query", String.class);
        else if (context.exists("httpRequest:/param/query"))
            query = context.source("httpRequest:/param/query", String.class);
        else throw new NKFException("The request does include the required argument \"query\"");

        if (query.equals("")) throw new NKFException("The required argument \"query\" must not be empty");

        String defaultEndpoint = context.source("sparql:endpoint", String.class);
        String endpoint, limit, accept, acceptEncoding, acceptLang;
        if (isHTTPRequest) {
            endpoint = getArg("httpRequest:/param/endpoint", defaultEndpoint, String.class, context);
            accept = getArg("httpRequest:/header/Accept", "application/sparql-results+xml", String.class, context);
            acceptEncoding = getArg("httpRequest:/header/Accept-Encoding", null, String.class, context);
            acceptLang = getArg("httpRequest:/header/Accept-Lang", null, String.class, context);
            limit = getArg("httpRequest:/param/limit", null, String.class, context);
        }
        else {
            endpoint = getArg("arg:endpoint", defaultEndpoint, String.class, context);
            accept = getArg("arg:accept", "application/sparql-results+xml", String.class, context);
            acceptEncoding = getArg("arg:acceptencoding", null, String.class, context);
            acceptLang = getArg("arg:acceptlang", null, String.class, context);
            limit = getArg("arg:limit", null, String.class, context);
        }

        INKFRequest keywordSearchQueryBuild = context.createRequest("active:freemarker");
        keywordSearchQueryBuild.addArgument("operator", "res:/resources/freemarker/keywordsearch.freemarker");
        keywordSearchQueryBuild.addArgumentByValue("keyword", query);
        if (limit != null) keywordSearchQueryBuild.addArgumentByValue("limit", limit);
        Object keywordSearchQuery = context.issueRequest(keywordSearchQueryBuild);

        request = context.createRequest("active:sparqlQuery");
        request.addArgumentByValue("endpoint", endpoint);
        request.addArgumentByValue("query", keywordSearchQuery);
        request.addArgumentByValue("accept", accept);
        if (acceptEncoding != null) request.addArgumentByValue("acceptencoding", acceptEncoding);
        if (acceptLang != null) request.addArgumentByValue("acceptlang", acceptLang);

        context.logRaw(
                INKFRequestContext.LEVEL_INFO,
                "Received SPARQL Keyword Search Request: " + query + " to endpoint: " + endpoint
        );

        response = context.issueRequestForResponse(request);
        context.createResponseFrom(response);
    }

    private <T> T getArg(String identifier, T defaultValue, Class<T> classType, INKFRequestContext context) throws NKFException{
        T arg;
        try {
            if (context.exists(identifier))
                arg = context.source(identifier, classType);
            else arg = defaultValue;
        } catch (Exception e) {
            arg = defaultValue;
            return  arg;
        }
        return arg;
    }
}

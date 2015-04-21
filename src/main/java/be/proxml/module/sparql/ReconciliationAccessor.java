package be.proxml.module.sparql;

import org.netkernel.layer0.nkf.*;
import org.netkernel.module.standard.endpoint.StandardAccessorImpl;

/**
 * Created by yyz on 4/21/15.
 */
public class ReconciliationAccessor extends StandardAccessorImpl {
    public void onSource(INKFRequestContext context) throws Exception {
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

        String defaultEndpoint = context.source("sparql:endpoint", String.class);
        String endpoint, type, acceptEncoding, acceptLang;
        Object limit;

        if (isHTTPRequest) {
            endpoint = getArg("httpRequest:/param/endpoint", defaultEndpoint, String.class, context);
            acceptEncoding = getArg("httpRequest:/header/Accept-Encoding", null, String.class, context);
            acceptLang = getArg("httpRequest:/header/Accept-Lang", null, String.class, context);
            limit = getArg("httpRequest:/param/limit", "", String.class, context);
            type = getArg("httpRequest:/param/type", null, String.class, context);
        }
        else {
            endpoint = getArg("arg:endpoint", defaultEndpoint, String.class, context);
            acceptEncoding = getArg("arg:acceptencoding", null, String.class, context);
            acceptLang = getArg("arg:acceptlang", null, String.class, context);
            limit = getArg("arg:limit", "", Object.class, context);
            type = getArg("arg:type", null, String.class, context);
        }

        String typeFilter;
        if (type == null) {
            typeFilter = "";
        }
        else typeFilter = "FILTER (xsd:string(?type) = \'" + type +"\'^^xsd:string)";

        INKFRequest reconcileQueryBuild = context.createRequest("active:freemarker");
        reconcileQueryBuild.addArgument("operator", "res:/resources/freemarker/reconcile.freemarker");
        reconcileQueryBuild.addArgumentByValue("typefilter", typeFilter);
        reconcileQueryBuild.addArgumentByValue("keyword", query);
        reconcileQueryBuild.addArgumentByValue("limit", limit);
        reconcileQueryBuild.setRepresentationClass(String.class);
        String reconcileQuery = (String)context.issueRequest(reconcileQueryBuild);

        INKFRequest reconcileRequest = context.createRequest("active:sparqlQuery");
        reconcileRequest.addArgumentByValue("endpoint", endpoint);
        reconcileRequest.addArgumentByValue("query", reconcileQuery);
        reconcileRequest.addArgumentByValue("accept", "application/sparql-results+xml");
        if (acceptEncoding != null) reconcileRequest.addArgumentByValue("acceptencoding", acceptEncoding);
        if (acceptLang != null) reconcileRequest.addArgumentByValue("acceptlang", acceptLang);
        Object queryResult = context.issueRequest(reconcileRequest);

        INKFRequest xsltRequest = context.createRequest("active:xsltc");
        xsltRequest.addArgumentByValue("operand", queryResult);
        xsltRequest.addArgument("operator", "res:/resources/xsl/sparqlresult_to_json.xsl");
        xsltRequest.addArgumentByValue("search", query);
        xsltRequest.setRepresentationClass(String.class);
        String jsonResult = (String)context.issueRequest(xsltRequest);

        String result = "\"result\": [" + jsonResult + "]";
        context.createResponseFrom(result);
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

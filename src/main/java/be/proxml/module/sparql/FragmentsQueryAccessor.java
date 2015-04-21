package be.proxml.module.sparql;

import org.netkernel.layer0.nkf.*;
import org.netkernel.layer0.nkf.impl.NKFEndpointImpl;
import org.netkernel.layer0.representation.IHDSNode;
import org.netkernel.layer0.representation.impl.HDSBuilder;
import org.netkernel.module.standard.endpoint.StandardAccessorImpl;
import org.netkernel.layer0.meta.impl.SourcedArgumentMetaImpl;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by yyz on 4/16/15.
 */
public class FragmentsQueryAccessor extends StandardAccessorImpl {

    public void onSource(INKFRequestContext context) throws Exception {

        INKFResponseReadOnly response;
        INKFRequest fragmentsQueryRequest;
        boolean isHTTPRequest = true;
        String dataset;
        if (context.exists("arg:dataset")) {
            dataset = context.source("arg:dataset", String.class);
            isHTTPRequest = false;
        }
        else if (context.exists("httpRequest:/postparam/dataset"))
            dataset = context.source("httpRequest:/postparam/dataset", String.class);
        else if (context.exists("httpRequest:/param/dataset"))
            dataset = context.source("httpRequest:/param/dataset", String.class);
        else throw new NKFException("The request does include the required argument \"dataset\"");

        String fragmentsRequestPath;
        if (context.exists("arg:url"))
            fragmentsRequestPath = context.source("arg:url", String.class);
        else if (context.exists("sparql:fragmentsRequestPath"))
            fragmentsRequestPath = context.source("sparql:fragmentsRequestPath", String.class);
        else throw new NKFException("SPARQL fragments query request path \"sparql:fragmentsRequestPath\" is not defined in the module definition!");

        String defaultEndpoint = context.source("sparql:endpoint", String.class);
        String endpoint, query, subject, object, predicate, accept, acceptEncoding, acceptLang;
        Object limit, offset;
        long limitLong, offsetLong;

        if (isHTTPRequest) {
            endpoint = getArg("httpRequest:/param/endpoint", defaultEndpoint, String.class, context);
            accept = getArg("httpRequest:/header/Accept", "application/sparql-results+xml", String.class, context);
            acceptEncoding = getArg("httpRequest:/header/Accept-Encoding", null, String.class, context);
            acceptLang = getArg("httpRequest:/header/Accept-Lang", null, String.class, context);
            query = getArg("httpRequest:/query", "", String.class, context);
            subject = getArg("httpRequest:/param/subject", "", String.class, context);
            predicate = getArg("httpRequest:/param/predicate", "", String.class, context);
            object = getArg("httpRequest:/param/object", "", String.class, context);
            limit = getArg("httpRequest:/param/limit", null, String.class, context);
            offset = getArg("httpRequest:/param/offset", null, String.class, context);
        }
        else {
            endpoint = getArg("arg:endpoint", defaultEndpoint, String.class, context);
            accept = getArg("arg:accept", "application/sparql-results+xml", String.class, context);
            acceptEncoding = getArg("arg:acceptencoding", null, String.class, context);
            acceptLang = getArg("arg:acceptlang", null, String.class, context);
            query = getArg("arg:query", "", String.class, context);
            subject = getArg("arg:subject", "", String.class, context);
            predicate = getArg("arg:predicate", "", String.class, context);
            object = getArg("arg:object", "", String.class, context);
            limit = getArg("arg:limit", null, Object.class, context);
            offset = getArg("arg:offset", null, Object.class, context);
        }

        List<Character> allowedLiteralStarts = new ArrayList<Character>();
        allowedLiteralStarts.add('<');
        subject = uriify(subject, "?s", allowedLiteralStarts);
        predicate = uriify(predicate, "?p", allowedLiteralStarts);
        allowedLiteralStarts.add('\'');
        allowedLiteralStarts.add('\"');
        object = uriify(object, "?o", allowedLiteralStarts);

        if (limit == null) limitLong = 100L;
        else limitLong = objToLong(limit);
        if (offset == null) offsetLong = 0L;
        else offsetLong = objToLong(offset);

        INKFRequest fragmentsCountQueryBuild = context.createRequest("active:freemarker");
        fragmentsCountQueryBuild.addArgument("operator", "res:/resources/freemarker/fragmentscount.freemarker");
        fragmentsCountQueryBuild.addArgumentByValue("subject", subject);
        fragmentsCountQueryBuild.addArgumentByValue("predicate", predicate);
        fragmentsCountQueryBuild.addArgumentByValue("object", object);
        String fragmentsCountQuery = (String)context.issueRequest(fragmentsCountQueryBuild);

        INKFRequest fragmentsCountQueryRequest = context.createRequest("active:sparqlQuery");
        fragmentsCountQueryRequest.addArgumentByValue("endpoint", endpoint);
        fragmentsCountQueryRequest.addArgumentByValue("query", fragmentsCountQuery);
        fragmentsCountQueryRequest.addArgumentByValue("httpmethod", "get");
        fragmentsCountQueryRequest.addArgumentByValue("accept", "application/sparql-results+xml");
        Object sparqlCountResult = context.issueRequest(fragmentsCountQueryRequest);

        INKFRequest xsltcRequest = context.createRequest("active:xsltc");
        xsltcRequest.addArgumentByValue("operand", sparqlCountResult);
        xsltcRequest.addArgument("operator", "res:/resources/xsl/sparqlresult_to_count.xsl");
        xsltcRequest.setRepresentationClass(String.class);
        String count = (String)context.issueRequest(xsltcRequest);
        long countLong;
        try {
            countLong = Long.parseLong(count);
        } catch (Exception e) {
            throw new NKFException("An exception occured when parsing fragments count query results, please recheck the query");
        }

        INKFRequest fragmentsQueryBuild = context.createRequest("active:freemarker");
        fragmentsQueryBuild.addArgument("operator", "res:/resources/freemarker/fragments.freemarker");
        fragmentsQueryBuild.addArgumentByValue("dataset", dataset);
        fragmentsQueryBuild.addArgumentByValue("query", (query.equals("")) ? "" : "?" + query);
        fragmentsQueryBuild.addArgumentByValue("url", fragmentsRequestPath);
        fragmentsQueryBuild.addArgumentByValue("subject", subject);
        fragmentsQueryBuild.addArgumentByValue("predicate", predicate);
        fragmentsQueryBuild.addArgumentByValue("object", object);
        fragmentsQueryBuild.addArgumentByValue("offset", offsetLong);
        fragmentsQueryBuild.addArgumentByValue("limit", limitLong);
        fragmentsQueryBuild.addArgumentByValue("count", count);
        long previous = offsetLong - limitLong;
        long next = offsetLong + limitLong;

        String queryWithoutPosition = ("?" + query).replaceAll("(?<=[?&;])offset=.*?($|[&;])", "").replaceAll("(?<=[?&;])limit=.*?($|[&;])", "").replaceAll("&$","");
        if (previous >= 0L) {
            fragmentsQueryBuild.addArgumentByValue("previous", fragmentsRequestPath + queryWithoutPosition + (query.equals("") ? "" : "&") + "offset=" + previous + "&limit=" + limitLong);
        }
        if (next <= countLong) {
            fragmentsQueryBuild.addArgumentByValue("next", fragmentsRequestPath + queryWithoutPosition + (query.equals("") ? "" : "&") + "offset=" + next + "&limit=" + limitLong);
        }
        fragmentsQueryBuild.setRepresentationClass(String.class);
        Object fragmentsQuery = context.issueRequest(fragmentsQueryBuild);

        fragmentsQueryRequest = context.createRequest("active:sparqlQuery");
        fragmentsQueryRequest.addArgumentByValue("endpoint", endpoint);
        fragmentsQueryRequest.addArgumentByValue("query", fragmentsQuery);
        fragmentsQueryRequest.addArgumentByValue("accept", accept);
        if (acceptEncoding != null) fragmentsQueryRequest.addArgumentByValue("acceptencoding", acceptEncoding);
        if (acceptLang != null) fragmentsQueryRequest.addArgumentByValue("acceptlang", acceptLang);

        context.logRaw(
                INKFRequestContext.LEVEL_INFO,
                "Received SPARQL Fragments Query Request with triple pattern \""
                        + subject + " " + predicate + " " + object + "\" to endpoint: " + endpoint
        );

        response = context.issueRequestForResponse(fragmentsQueryRequest);
        context.createResponseFrom(response);
    }

    private <T> T getArg(String identifier, Class<T> classType, INKFRequestContext context) throws NKFException{
        T arg;
        try {
            if (context.exists(identifier))
                arg = context.source(identifier, classType);
            else throw new NKFException("The request does include the required argument: " + identifier);
        } catch (Exception e) {
            throw new NKFException("An exception occurred when getting the required argument: " + identifier);
        }
        return arg;
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

    private String uriify(String literal, String defaultValue, List<Character> patterns) {
        if (literal.equals("")) {
            literal = defaultValue;
        }
        else {
            char first = literal.charAt(0);
            if (!patterns.contains(first)) {
                literal = "<" + literal + ">";
            }
        }
        return literal;
    }

    private long objToLong(Object numObj) throws NKFException{
        long numLong = 0L;
        if (numObj instanceof Long) {
            numLong = (Long)numObj;
        }
        else if (numObj instanceof String) {
            try {
                numLong = Long.parseLong((String)numObj);
            } catch (Exception e) {
                throw new NKFException("An exception occurred when converting " + numObj.toString() + " to Long type!");
            }
        }
        else if (numObj instanceof Integer) {
            numLong = ((Integer) numObj).longValue();
        }
        else throw new NKFException("An exception occurred when converting " + numObj.toString() + " to Long type!");
        return numLong;
    }
}

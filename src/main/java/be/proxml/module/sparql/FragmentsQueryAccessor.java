package be.proxml.module.sparql;

import org.netkernel.layer0.nkf.*;
import org.netkernel.layer0.representation.IHDSNode;
import org.netkernel.layer0.representation.impl.HDSBuilder;
import org.netkernel.module.standard.endpoint.StandardAccessorImpl;
import org.netkernel.layer0.meta.impl.SourcedArgumentMetaImpl;

import java.net.URLEncoder;

/**
 * Created by yyz on 4/16/15.
 */
public class FragmentsQueryAccessor extends StandardAccessorImpl {
    public FragmentsQueryAccessor() {
        this.declareThreadSafe();
        this.declareArgument(new SourcedArgumentMetaImpl("requestpath",null,null,new Class[] {String.class}));
        this.declareArgument(new SourcedArgumentMetaImpl("accept",null,null,new Class[] {String.class}));
        this.declareArgument(new SourcedArgumentMetaImpl("subject",null,null,new Class[] {String.class}));
        this.declareArgument(new SourcedArgumentMetaImpl("predicate",null,null,new Class[] {String.class}));
        this.declareArgument(new SourcedArgumentMetaImpl("object",null,null,new Class[] {String.class}));
        this.declareArgument(new SourcedArgumentMetaImpl("limit",null,null,new Class[] {Long.class}));
        this.declareArgument(new SourcedArgumentMetaImpl("offset",null,null,new Class[] {Long.class}));
    }

    public void onSource(INKFRequestContext context) throws Exception {

        INKFResponseReadOnly response;
        INKFRequest request;
        IHDSNode headers;
        HDSBuilder headerBuilder = new HDSBuilder();
        if (context.exists("httpRequest:/headers")) {
            headers = context.source("httpRequest:/headers", IHDSNode.class).getRoot();
        }
        else if (context.exists("arg:accept")) {
            headerBuilder.addNode("Accept", context.source("arg:accept", String.class));
            headers = headerBuilder.getRoot();
        }
        else {
            headerBuilder.addNode("Accept", "application/sparql-results+xml");
            headers = headerBuilder.getRoot();
        }

        String path;
        if (context.exists("httpRequest:/param/requestpath")) {
            path = context.source("httpRequest:/param/requestpath", String.class);
        }
        else if (context.exists("arg:requestpath")) {
            path = context.source("arg:accept", String.class);
        }
        else {
            IHDSNode connection = context.source("res:/etc/system/DefaultConnection.xml", IHDSNode.class);
            String endpoint = connection.getFirstValue("//endpoint").toString();
            String requestpath = connection.getFirstValue("//requestpath").toString();
            path = endpoint + requestpath;
        }

        // query
        String query = null;
        if (context.exists("httpRequest:/param/query")) {
            query = context.source("httpRequest:/param/query", String.class);
        }
        else if (context.exists("arg:query")) {
            query = context.source("arg:query", String.class);
        }
        else {
            query = "";
        }
        //

        // dataset
        String dataset = null;
        if (context.exists("httpRequest:/param/dataset")) {
            dataset = context.source("httpRequest:/param/dataset", String.class);
        }
        else if (context.exists("arg:dataset")) {
            dataset = context.source("arg:dataset", String.class);
        }
        else {
            throw new Exception("the request does not have a valid - dataset - argument");
        }

        // subject
        String subject = null;
        if (context.exists("httpRequest:/param/subject")) {
            subject = context.source("httpRequest:/param/subject", String.class);
        }
        else if (context.exists("arg:subject")) {
            subject = context.source("arg:subject", String.class);
        }
        else {
            subject = "?s";
        }
        if (subject.equals("")) {
            subject = "?s";
        }
        else {
            if (! subject.startsWith("<")) {
                subject = "<" + subject + ">";
            }
        }

        // predicate
        String predicate = null;
        if (context.exists("httpRequest:/param/predicate")) {
            predicate = context.source("httpRequest:/param/predicate", String.class);
        }
        else if (context.exists("arg:predicate")) {
            predicate = context.source("arg:predicate", String.class);
        }
        else {
            predicate = "?p";
        }
        if (predicate.equals("")) {
            predicate = "?p";
        }
        else {
            if (! predicate.startsWith("<")) {
                predicate = "<" + predicate + ">";
            }
        }

        // object
        String object = null;
        if (context.exists("httpRequest:/param/object")) {
            object = context.source("httpRequest:/param/object", String.class);
        }
        else if (context.exists("arg:object")) {
            object = context.source("arg:object", String.class);
        }
        else {
            object = "?o";
        }
        if (object.equals("")) {
            object = "?o";
        }
        else {
            if ( (! object.startsWith("<")) && (! object.startsWith("'")) && (! object.startsWith("\""))) {
                object = "<" + object + ">";
            }
        }

        // offset
        Long offset = null;
        String offsetStr = null;
        if (context.exists("arg:offset")) {
            try {
                offset = context.source("arg:offset", Long.class);
            } catch (Exception e) {
                offsetStr = context.source("arg:offset", String.class);
            }
        }
        else if (context.exists("httpRequest:/param/offset")) {
            offsetStr = context.source("httpRequest:/param/offset", String.class);
        }
        else offset = 0L;
        if (offsetStr == null || offsetStr.equals("")) {
            offset = 0L;
        }
        else {
            try {
                offset = Long.parseLong(offsetStr);
            } catch (Exception e) {
                throw new Exception("the request does not have a valid - offset - argument");
            }
        }

        // limit
        Long limit = null;
        String limitStr = null;
        if (context.exists("arg:limit")) {
            try {
                limit = context.source("arg:limit", Long.class);
            } catch (Exception e) {
                limitStr = context.source("arg:limit", String.class);
            }
        }
        else if (context.exists("httpRequest:/param/limit")) {
            limitStr = context.source("httpRequest:/param/limit", String.class);
        }
        else limit = 100L;
        if (limitStr == null || limitStr.equals("")) {
            limit = 100L;
        }
        else {
            try {
                limit = Long.parseLong(limitStr);
            } catch (Exception e) {
                throw new Exception("the request does not have a valid - limit - argument");
            }
        }
        //
        INKFRequest fragmentsCountQueryBuild = context.createRequest("active:freemarker");
        fragmentsCountQueryBuild.addArgument("operator", "res:/resources/freemarker/fragmentscount.freemarker");
        fragmentsCountQueryBuild.addArgumentByValue("subject", subject);
        fragmentsCountQueryBuild.addArgumentByValue("predicate", predicate);
        fragmentsCountQueryBuild.addArgumentByValue("object", object);
        String fragmentsCountQuery = (String)context.issueRequest(fragmentsCountQueryBuild);

        INKFRequest fragmentsCountQueryRequest = context.createRequest("active:httpGet");
        fragmentsCountQueryRequest.setVerb(INKFRequestReadOnly.VERB_SOURCE);
        fragmentsCountQueryRequest.addArgument("url", path + "?query=" + URLEncoder.encode(fragmentsCountQuery, "UTF-8"));
        HDSBuilder countQueryHeader = new HDSBuilder();
        countQueryHeader.addNode("Accept", "application/sparql-results+xml");
        fragmentsCountQueryRequest.addArgumentByValue("headers", countQueryHeader.getRoot());
        Object sparqlCountResult = context.issueRequest(fragmentsCountQueryRequest);

        INKFRequest xsltcRequest = context.createRequest("active:xsltc");
        xsltcRequest.addArgumentByValue("operand", sparqlCountResult);
        xsltcRequest.addArgument("operator", "res:/resources/xsl/sparqlresult_to_count.xsl");
        xsltcRequest.setRepresentationClass(String.class);
        String count = (String)context.issueRequest(xsltcRequest);

        Long countLong = Long.parseLong(count);

        INKFRequest fragmentsQueryBuild = context.createRequest("active:freemarker");
        fragmentsQueryBuild.addArgument("operator", "res:/resources/freemarker/fragments.freemarker");
        fragmentsQueryBuild.addArgumentByValue("dataset", dataset);
        fragmentsQueryBuild.addArgumentByValue("query", (query.equals("")) ? "" : "?" + query);
        fragmentsQueryBuild.addArgumentByValue("url", "http://id.vlaanderen.be/fragments");
        fragmentsQueryBuild.addArgumentByValue("subject", subject);
        fragmentsQueryBuild.addArgumentByValue("predicate", predicate);
        fragmentsQueryBuild.addArgumentByValue("object", object);
        fragmentsQueryBuild.addArgumentByValue("offset", offset.toString());
        fragmentsQueryBuild.addArgumentByValue("limit", limit.toString());
        fragmentsQueryBuild.addArgumentByValue("count", count);
        Long previous = offset - limit;
        Long next = offset + limit;
        String queryWithoutPosition = ("?" + query).replaceAll("(?<=[?&;])offset=.*?($|[&;])", "").replaceAll("(?<=[?&;])limit=.*?($|[&;])", "").replaceAll("&$","");
        if (previous >= 0L) {
            fragmentsQueryBuild.addArgumentByValue("previous", path + queryWithoutPosition + (query.equals("") ? "" : "&") + "offset=" + previous.toString() + "&limit=" + limit.toString());
        }
        if (next <= countLong) {
            fragmentsQueryBuild.addArgumentByValue("next", path + queryWithoutPosition + (query.equals("") ? "" : "&") + "offset=" + next.toString() + "&limit=" + limit.toString());
        }
        fragmentsQueryBuild.setRepresentationClass(String.class);
        String fragments = (String)context.issueRequest(fragmentsQueryBuild);

        INKFRequest fragmentsQueryRequest = context.createRequest("active:httpPost");
        fragmentsQueryRequest.setVerb(INKFRequestReadOnly.VERB_SOURCE);
        fragmentsQueryRequest.addArgument("url", path);
        HDSBuilder body = new HDSBuilder();
        body.pushNode("query", fragments);
        fragmentsQueryRequest.addArgumentByValue("nvp", body.getRoot());
        fragmentsQueryRequest.addArgumentByValue("headers", headers);

        Object sparqlResult = context.issueRequestForResponse(fragmentsQueryRequest);

        context.logRaw(
                INKFRequestContext.LEVEL_INFO,
                "Received SPARQL Fragments Search Request to endpoint: " + path
        );
        response = context.createResponseFrom(sparqlResult);
    }
}

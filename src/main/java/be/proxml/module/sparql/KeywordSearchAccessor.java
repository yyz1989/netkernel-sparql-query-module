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
        IHDSNode headers;
        if (context.exists("httpRequest:/headers")) {
            headers = context.source("httpRequest:/headers", IHDSNode.class).getRoot();
        }
        else if (context.exists("arg:accept")) {
            HDSBuilder headerBuilder = new HDSBuilder();
            headerBuilder.addNode("Accept", context.source("arg:accept", String.class));
            headers = headerBuilder.getRoot();
        }
        else {
            HDSBuilder headerBuilder = new HDSBuilder();
            headerBuilder.addNode("Accept", "application/sparql-results+xml");
            headers = headerBuilder.getRoot();
        }
        IHDSNode connection = context.source("res:/etc/system/DefaultConnection.xml", IHDSNode.class);
        String endpoint = connection.getFirstValue("//endpoint").toString();
        String requestpath = connection.getFirstValue("//requestpath").toString();
        String path = endpoint + requestpath;

        String query;
        if (context.exists("httpRequest:/param/query"))
            query = context.source("httpRequest:/param/query", String.class);
        else if (context.exists("httpRequest:/postparam/query"))
            query = context.source("httpRequest:/postparam/query", String.class);
        else if (context.exists("arg:query"))
            query = context.source("arg:query", String.class);
        else throw new NKFException("The request does include the required argument \"query\"");

        String sparqlQuery = "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n" +
                "PREFIX text: <http://jena.apache.org/text#>\n" +
                "PREFIX skos: <http://www.w3.org/2004/02/skos/core#>\n" +
                "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
                "PREFIX dcat: <http://www.w3.org/ns/dcat#>\n" +
                "select distinct ?id ?label where {" +
                "   ?id text:query (rdfs:label \"" + query + "\") ;" +
                "   rdfs:label ?label . " +
                "   {?id a dcat:Dataset} union {?id a dcat:Download} ." +
                "}";

        request = context.createRequest("active:httpGet");
        request.setVerb(INKFRequestReadOnly.VERB_SOURCE);
        request.addArgument("url", path + "?query=" + URLEncoder.encode(sparqlQuery, "UTF-8"));
        request.addArgumentByValue("headers", headers);

        context.logRaw(
                INKFRequestContext.LEVEL_INFO,
                "Received SPARQL Keyword Search Request: " + query + " to endpoint: " + path
        );

        response = context.issueRequestForResponse(request);
        context.createResponseFrom(response);
    }
}

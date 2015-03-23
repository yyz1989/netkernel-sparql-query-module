package resources.endpoints;

import org.netkernel.layer0.nkf.*;
import org.netkernel.layer0.representation.IHDSNode;
import org.netkernel.layer0.representation.impl.HDSBuilder;
import org.netkernel.module.standard.endpoint.StandardAccessorImpl;


public class SparqlQueryExecutor extends StandardAccessorImpl
{

    public void onSource(INKFRequestContext context) throws Exception
    {
        INKFRequest httpRequest;
        Object httpResponse;
        String httpMethod = context.source("httpRequest:/method", String.class);
        IHDSNode acceptHeaders = context.source("httpRequest:/accept", IHDSNode.class);
        String query = "";

        if (httpMethod.equals("GET")) {
            query = context.source("httpRequest:/param/query", String.class);
            httpRequest = context.createRequest("active:httpGet");
            httpRequest.setVerb(INKFRequestReadOnly.VERB_SOURCE);
            httpRequest.addArgument("url", "http://localhost:3030/tdb/query?query=" + query);
            httpRequest.addArgumentByValue("headers", acceptHeaders.getRoot());
        }
        else if (httpMethod.equals("POST")) {
            query = context.source("httpRequest:/postparam/query", String.class);
            HDSBuilder body = new HDSBuilder();
            body.pushNode("query", query);
            httpRequest = context.createRequest("active:httpPost");
            httpRequest.setVerb(INKFRequestReadOnly.VERB_SOURCE);
            httpRequest.addArgument("url", "http://localhost:3030/tdb/query");
            httpRequest.addArgumentByValue("nvp", body.getRoot());
            httpRequest.addArgumentByValue("headers", acceptHeaders.getRoot());
        }
        else {
            httpRequest = context.createRequest("access-blocked");
            query = "Query is invalid";
        }

        context.logRaw(
                INKFRequestContext.LEVEL_INFO,
                "Received SPARQL Query " + httpMethod + " Request :" + query
        );

        httpResponse = context.issueRequest(httpRequest);
        context.createResponseFrom(httpResponse);
    }
}
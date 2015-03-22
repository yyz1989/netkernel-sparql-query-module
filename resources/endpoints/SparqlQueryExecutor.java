package resources.endpoints;

import org.netkernel.layer0.nkf.*;
import org.netkernel.module.standard.endpoint.StandardAccessorImpl;


public class SparqlQueryExecutor extends StandardAccessorImpl
{

    public void onSource(INKFRequestContext context) throws Exception
    {
        INKFRequest httpRequest;
        Object httpResponse;
        String path = context.source("httpRequest:/url", String.class);
        String httpMethod = context.source("httpRequest:/method", String.class);
        String mimeType = context.source("httpRequest:/accept/preferred", String.class);
        String query = context.source("httpRequest:/param/query", String.class);
        context.logRaw(
                INKFRequestContext.LEVEL_INFO,
                "Received SPARQL Query Request " + query
        );

        if (httpMethod.equals("GET")) {
            httpRequest = context.createRequest("active:httpGet");
            httpRequest.setVerb(INKFRequestReadOnly.VERB_SOURCE);
            httpRequest.addArgument("url", "http://localhost:3030/tdb/query?query=" + query);
        }
        else if (httpMethod.equals("POST")) {
            httpRequest = context.createRequest("active:httpPost");
            httpRequest.setVerb(INKFRequestReadOnly.VERB_SINK);
            httpRequest.addArgument("url", "http://localhost:3030/tdb/query");
            httpRequest.addArgument("query", query);
        }
        else {
            httpRequest = context.createRequest("access-blocked");
        }

        httpResponse = context.issueRequest(httpRequest);
        context.createResponseFrom(httpResponse);
    }
}
package resources.endpoints;

import org.netkernel.client.http.representation.HttpClientResponseRepresentation;
import org.netkernel.layer0.nkf.*;
import org.netkernel.layer0.representation.IHDSNode;
import org.netkernel.layer0.representation.impl.HDSBuilder;
import org.netkernel.module.standard.endpoint.StandardAccessorImpl;


public class SparqlQueryExecutor extends StandardAccessorImpl
{

    public void onSource(INKFRequestContext context) throws Exception
    {
        INKFResponseReadOnly response;
        INKFRequest httpRequest;
        String httpMethod = context.source("httpRequest:/method", String.class);
        IHDSNode headers = context.source("httpRequest:/headers", IHDSNode.class);
        IHDSNode connection = context.source("arg:connection", IHDSNode.class);
        String host = connection.getFirstValue("//host").toString();
        String port = connection.getFirstValue("//port").toString();
        String dataset = connection.getFirstValue("//dataset").toString();
        String operation = connection.getFirstValue("//operation").toString();
        String endpoint = host + ":" + port + "/" + dataset + "/" + operation;
        String query;

        if (httpMethod.equals("GET")) {
            query = context.source("httpRequest:/param/query", String.class);
            httpRequest = context.createRequest("active:httpGet");
            httpRequest.setVerb(INKFRequestReadOnly.VERB_SOURCE);
            httpRequest.addArgument("url", endpoint + "?query=" + query);
            httpRequest.addArgumentByValue("headers", headers.getRoot());
        }
        else if (httpMethod.equals("POST")) {
            query = context.source("httpRequest:/postparam/query", String.class);
            HDSBuilder body = new HDSBuilder();
            body.pushNode("query", query);
            HDSBuilder newHeaders = new HDSBuilder();
            newHeaders.addNode("Accept-Encoding", headers.getFirstValue("//Accept-Encoding"));
            newHeaders.addNode("Accept", headers.getFirstValue("//Accept"));
            newHeaders.addNode("Accept-Language", headers.getFirstValue("//Accept-Language"));
            httpRequest = context.createRequest("active:httpPost");
            httpRequest.setVerb(INKFRequestReadOnly.VERB_SOURCE);
            httpRequest.addArgument("url", endpoint);
            httpRequest.addArgumentByValue("nvp", body.getRoot());
            httpRequest.addArgumentByValue("headers", newHeaders.getRoot());
        }
        else {
            httpRequest = context.createRequest("access-blocked");
            query = "Request is not acceptable";
        }

        context.logRaw(
                INKFRequestContext.LEVEL_INFO,
                "Received SPARQL Query " + httpMethod + " Request :" + query
        );

        response = context.issueRequestForResponse(httpRequest);
        context.createResponseFrom(response);
    }
}
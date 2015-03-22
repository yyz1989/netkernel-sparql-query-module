
import org.netkernel.layer0.nkf.INKFRequest;
import org.netkernel.layer0.nkf.INKFRequestContext;
import org.netkernel.layer0.nkf.INKFRequestReadOnly;
import org.netkernel.layer0.nkf.INKFResponse;
import org.netkernel.module.standard.endpoint.StandardAccessorImpl;


public class SparqlQueryExecutor extends StandardAccessorImpl
{

    public void onSource(INKFRequestContext context) throws Exception
    {
        INKFRequest httpRequest;
        INKFResponse response;
        String httpMethod = context.source("httpRequest:/method", String.class);
        String mimeType = context.source("httpRequest:/accept/preferred", String.class);
        String query = context.source("httpRequest:/paramHTMLEncoded/query", String.class);
        context.logRaw(
                INKFRequestContext.LEVEL_INFO,
                "Received SPARQL Query Request " + query
        );

        if (httpMethod.equals("GET")) {
            httpRequest = context.createRequest("active:httpGet");
            httpRequest.setVerb(INKFRequestReadOnly.VERB_SOURCE);
            httpRequest.addArgumentByValue("url", "http://localhost:3030/tdb/query" + query);
        }
        else if (httpMethod.equals("POST")) {
            httpRequest = context.createRequest("active:httpPost");
            httpRequest.setVerb(INKFRequestReadOnly.VERB_SINK);
            httpRequest.addArgumentByValue("url", "http://localhost:3030/tdb/query");
            httpRequest.addArgumentByValue("query", query);
        }
        else {
            httpRequest = context.createRequest("access-blocked");
        }

        context.logRaw(
                INKFRequestContext.LEVEL_INFO,
                "Received SPARQL Query Request"
        );

        response = context.createResponseFrom(httpRequest);
        response.setMimeType(mimeType);
    }
}
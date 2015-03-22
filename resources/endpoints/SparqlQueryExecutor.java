package resources.endpoints;

import org.netkernel.layer0.nkf.INKFRequest;
import org.netkernel.layer0.nkf.INKFRequestContext;
import org.netkernel.layer0.nkf.INKFRequestReadOnly;
import org.netkernel.layer0.nkf.INKFResponse;
import org.netkernel.module.standard.endpoint.StandardAccessorImpl;


public class SparqlQueryExecutor extends StandardAccessorImpl
{

    public void onSource(INKFRequestContext context) throws Exception
    {
        INKFRequestReadOnly outerRequest;
        INKFRequest innerRequest;
        INKFResponse response;

        // Get the outer request and then create an inner request in which the HTTP method
        // directs which ROC verb to use. We also attach the HTTP method as the argument httpMethod.
        outerRequest = context.source("arg:request", INKFRequestReadOnly.class);
        innerRequest = context.createRequest(outerRequest.getIdentifier());

        //Source the HTTP verb from the http request and use a corresponding ROC verb
        String httpMethod = context.source("httpRequest:/method", String.class);
        String mimeType = context.source("httpRequest:/accept/preferred", String.class);
        if (httpMethod.equals("GET")) {
            innerRequest.setVerb(INKFRequestReadOnly.VERB_SOURCE);
        }
        else if (httpMethod.equals("POST")) {
            innerRequest.setVerb(INKFRequestReadOnly.VERB_SINK);
            innerRequest.addPrimaryArgument(context.source("httpRequest:/body"));
        }

        // Return the new, fabricated request to be used by the pluggable overlay as its request to the
        // wrapped space.
        context.logRaw(
                INKFRequestContext.LEVEL_INFO,
                "Received SPARQL Query Request"
        );

        response = context.createResponseFrom(innerRequest);
        response.setMimeType(mimeType);
    }
}
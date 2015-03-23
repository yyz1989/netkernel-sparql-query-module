package resources.endpoints;
/**
 * A SPARQL query client for Netkernel
 * @author Proxml
 */
import org.netkernel.layer0.nkf.*;
import org.netkernel.layer0.representation.IHDSNode;
import org.netkernel.layer0.representation.impl.HDSBuilder;
import org.netkernel.module.standard.endpoint.StandardAccessorImpl;

/**
 * A Netkernel accessor handling the incoming SPARQL query requests
 */
public class SparqlQueryAccessor extends StandardAccessorImpl
{
    /**
     * Reaction of a Netkernel SOURCE request issued from the HTTP frontend
     * @param context the context where the Netkernel request comes from
     * @throws Exception
     */
    public void onSource(INKFRequestContext context) throws Exception
    {
        INKFResponseReadOnly response;
        INKFRequest request;

        // Get query arguments
        String httpMethod = context.source("httpRequest:/method", String.class);
        IHDSNode headers = context.source("httpRequest:/headers", IHDSNode.class);
        IHDSNode connection = context.source("arg:connection", IHDSNode.class);
        String host = connection.getFirstValue("//host").toString();
        String port = connection.getFirstValue("//port").toString();
        String dataset = connection.getFirstValue("//dataset").toString();
        String operation = connection.getFirstValue("//operation").toString();

        // URL of the SPARQL endpoint to be accessed
        String endpoint = host + ":" + port + "/" + dataset + "/" + operation;
        String query;

        if (httpMethod.equals("GET")) {
            query = context.source("httpRequest:/param/query", String.class);
            request = context.createRequest("active:httpGet");
            request.setVerb(INKFRequestReadOnly.VERB_SOURCE);
            request.addArgument("url", endpoint + "?query=" + query);
            request.addArgumentByValue("headers", headers.getRoot());
        }
        else if (httpMethod.equals("POST")) {
            // The body of POST must be URLencoded form data represented in HDS format
            query = context.source("httpRequest:/postparam/query", String.class);
            HDSBuilder body = new HDSBuilder();
            body.pushNode("query", query);

            // The headers of POST must be name-value pairs in HDS format, which must be
            // rebuilt to avoid conflicts generated during relay
            HDSBuilder newHeaders = new HDSBuilder();
            newHeaders.addNode("Accept-Encoding", headers.getFirstValue("//Accept-Encoding"));
            newHeaders.addNode("Accept", headers.getFirstValue("//Accept"));
            newHeaders.addNode("Accept-Language", headers.getFirstValue("//Accept-Language"));

            request = context.createRequest("active:httpPost");
            request.setVerb(INKFRequestReadOnly.VERB_SOURCE);
            request.addArgument("url", endpoint);
            request.addArgumentByValue("nvp", body.getRoot());
            request.addArgumentByValue("headers", newHeaders.getRoot());
        }
        else {
            // Deny services for other type of HTTP requests
            request = context.createRequest("access-blocked");
            query = "Request is not acceptable";
        }

        context.logRaw(
                INKFRequestContext.LEVEL_INFO,
                "Received SPARQL Query " + httpMethod + " Request: " + query
        );

        response = context.issueRequestForResponse(request);
        context.createResponseFrom(response);
    }
}
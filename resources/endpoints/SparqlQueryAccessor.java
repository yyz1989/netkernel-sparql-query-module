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
        String httpMethod;
        IHDSNode headers;
        boolean hasHeadersFromHTTP;
        if (context.exists("httpRequest:/method"))
            httpMethod = context.source("httpRequest:/method", String.class);
        else if (context.exists("arg:httpmethod"))
            httpMethod = context.source("arg:httpmethod", String.class).toUpperCase();
        else throw new NKFException("The equest does include the required argument \"httpmethod\"");

        if (context.exists("httpRequest:/headers")) {
            headers = context.source("httpRequest:/headers", IHDSNode.class).getRoot();
            hasHeadersFromHTTP = true;
        }
        else {
            HDSBuilder headerBuilder = new HDSBuilder();
            if (context.exists("arg:accept"))
                headerBuilder.addNode("Accept",
                        context.source("arg:accept", String.class));
            if (context.exists("arg:acceptencoding"))
                headerBuilder.addNode("Accept-Encoding",
                        context.source("arg:acceptencoding", String.class));
            if (context.exists("arg:acceptlang"))
                headerBuilder.addNode("Accept-Language",
                        context.source("arg:acceptlang", String.class));
            headers = headerBuilder.getRoot();
            hasHeadersFromHTTP = false;
        }
        String endpoint = context.source("arg:endpoint", String.class);
        String dataset = context.source("arg:dataset", String.class);
        String operation = context.source("arg:operation", String.class);

        // URL of the SPARQL endpoint to be accessed
        String path = endpoint + dataset + operation;
        String query;
        if (context.exists("httpRequest:/param/query"))
            query = context.source("httpRequest:/param/query", String.class);
        else if (context.exists("httpRequest:/postparam/query"))
            query = context.source("httpRequest:/postparam/query", String.class);
        else if (context.exists("arg:query"))
            query = context.source("arg:query", String.class);
        else throw new NKFException("The request does include the required argument \"query\"");

        if (httpMethod.equals("GET")) {
            request = context.createRequest("active:httpGet");
            request.setVerb(INKFRequestReadOnly.VERB_SOURCE);
            request.addArgument("url", path + "?" + operation + "=" + query);
            request.addArgumentByValue("headers", headers);
        }
        else if (httpMethod.equals("POST")) {
            // The body of POST must be URLencoded form data represented in HDS format
            HDSBuilder body = new HDSBuilder();
            body.pushNode("query", query);
            request = context.createRequest("active:httpPost");
            request.setVerb(INKFRequestReadOnly.VERB_SOURCE);
            request.addArgument("url", path);
            request.addArgumentByValue("nvp", body.getRoot());
            // The headers of POST must be name-value pairs in HDS format, which must be
            // rebuilt to avoid conflicts generated during relay
            if (hasHeadersFromHTTP) {
                HDSBuilder newHeaders = new HDSBuilder();
                newHeaders.addNode("Accept-Encoding", headers.getFirstValue("//Accept-Encoding"));
                newHeaders.addNode("Accept", headers.getFirstValue("//Accept"));
                newHeaders.addNode("Accept-Language", headers.getFirstValue("//Accept-Language"));
                request.addArgumentByValue("headers", newHeaders.getRoot());
            }
            else request.addArgumentByValue("headers", headers);

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
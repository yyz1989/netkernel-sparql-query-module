package be.proxml.module.sparql;
/**
 * A SPARQL query client for Netkernel
 * @author Proxml
 */
import org.netkernel.layer0.meta.impl.SourcedArgumentMetaImpl;
import org.netkernel.layer0.nkf.*;
import org.netkernel.layer0.representation.IHDSNode;
import org.netkernel.layer0.representation.impl.HDSBuilder;
import org.netkernel.module.standard.endpoint.StandardAccessorImpl;
import java.net.URLEncoder;

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
        boolean isHTTPRequest = true;
        IHDSNode credentials;
        String query;
        if (context.exists("arg:query")) {
            query = context.source("arg:query", String.class);
            isHTTPRequest = false;
        }
        else if (context.exists("httpRequest:/postparam/query"))
            query = context.source("httpRequest:/postparam/query", String.class);
        else if (context.exists("httpRequest:/param/query"))
            query = context.source("httpRequest:/param/query", String.class);
        else throw new NKFException("The request does include the required argument \"query\"");

        if (context.exists("sparql:credentials"))
            credentials = context.source("sparql:credentials", IHDSNode.class);
        else if (context.exists("res:/etc/system/DefaultHttpCredentials.xml"))
            credentials = context.source("res:/etc/system/DefaultHttpCredentials.xml", IHDSNode.class);
        else throw new NKFException("Neither SPARQL endpoint credentials \"sparql:credentials\" nor default credentials config file are defined in the module definition!");

        String defaultEndpoint = context.source("sparql:endpoint", String.class);
        String endpoint, operation, httpMethod, accept, acceptEncoding, acceptLang;
        HDSBuilder headerBuilder = new HDSBuilder();
        if (isHTTPRequest) {
            endpoint = getArg("httpRequest:/param/endpoint", defaultEndpoint, String.class, context);
            operation = getArg("httpRequest:/param/operation", "query", String.class, context);
            httpMethod = getArg("httpRequest:/method", "POST", String.class, context).toUpperCase();
            accept = getArg("httpRequest:/header/Accept", String.class, context);
            acceptEncoding = getArg("httpRequest:/header/Accept-Encoding", null, String.class, context);
            acceptLang = getArg("httpRequest:/header/Accept-Lang", null, String.class, context);
        }
        else {
            endpoint = getArg("arg:endpoint", defaultEndpoint, String.class, context);
            operation = getArg("arg:operation", "query", String.class, context);
            httpMethod = getArg("arg:method", "POST", String.class, context).toUpperCase();
            accept = getArg("arg:accept", String.class, context);
            acceptEncoding = getArg("arg:acceptencoding", null, String.class, context);
            acceptLang = getArg("arg:acceptlang", null, String.class, context);
        }
        headerBuilder.addNode("Accept", accept);
        if (acceptEncoding != null) headerBuilder.addNode("Accept-Encoding", acceptEncoding);
        if (acceptLang != null) headerBuilder.addNode("Accept-Language", acceptLang);

        String encodedQuery = URLEncoder.encode(query, "UTF-8");
        String encodedRequestUrl = endpoint + "?" + operation + "=" + encodedQuery;
        if (httpMethod.equals("GET") && encodedRequestUrl.getBytes("UTF-8").length >= 4000 ) {
            httpMethod = "POST";
            context.logRaw(
                    INKFRequestContext.LEVEL_WARNING,
                    "Warning: Received GET request with a SPARQL Query greater than 4KB! Converted request to POST"
            );
        }

        if (httpMethod.equals("GET")) {
            request = context.createRequest("active:httpGet");
            request.addArgument("url", endpoint + "?" + operation + "=" + query);
            request.addArgumentByValue("headers", headerBuilder.getRoot());
        }
        else if (httpMethod.equals("POST")) {
            HDSBuilder body = new HDSBuilder();
            body.pushNode("query", query);
            request = context.createRequest("active:httpPost");
            request.addArgument("url", endpoint);
            request.addArgumentByValue("nvp", body.getRoot());
            request.addArgumentByValue("headers", headerBuilder.getRoot());
        }
        else {
            // Deny services for other type of HTTP requests
            request = context.createRequest("access-blocked");
            query = "Requested HTTP operation is not acceptable";
        }

        context.logRaw(
                INKFRequestContext.LEVEL_INFO,
                "Received SPARQL Query " + httpMethod + " Request: " + query + " to endpoint: " + endpoint
        );

        response = context.issueRequestForResponse(request);

        int responseCode = (Integer)response.getHeader("HTTP_ACCESSOR_STATUS_CODE_METADATA");
        if (responseCode == 401) {
            context.logRaw(
                    INKFRequestContext.LEVEL_WARNING,
                    "Warning: Request is unauthorized, retrying with credentials"
            );
            INKFRequest authRequest = context.createRequest("active:httpState");
            authRequest.setVerb(INKFRequestReadOnly.VERB_NEW);
            authRequest.addArgumentByValue("credentials", credentials);
            Object auth = context.issueRequest(authRequest);
            request.addArgumentByValue("state", auth);
            response = context.issueRequestForResponse(request);
        }
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
}
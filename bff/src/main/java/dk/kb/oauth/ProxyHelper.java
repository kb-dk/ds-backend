package dk.kb.oauth;

import dk.kb.oauth.config.ServiceConfig;
import dk.kb.util.webservice.exception.InternalServiceException;
import dk.kb.util.webservice.exception.ServiceException;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ProxyHelper {

    public static Response.ResponseBuilder proxy(String method, String api, String path, String query, HttpHeaders headers, String accessToken) {
        URI uri  = getApiUri(api, path, query);
        HttpURLConnection connection = openConnection(method,uri, headers,accessToken);
        try {
            return createResponse(connection);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static Response.ResponseBuilder createResponse(HttpURLConnection connection) throws IOException {
        int status = connection.getResponseCode();
        Response.ResponseBuilder responseBuilder = Response.status(status);

        BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        StringBuilder responseBody = new StringBuilder();
        String line;
        while((line = in.readLine()) != null) {
            responseBody.append(line);
        }

        //copy headers
        responseBuilder.header("content-disposition",connection.getHeaderField("content-disposition"));
        responseBuilder.header("content-type",connection.getHeaderField("content-type"));
        return responseBuilder
            .entity(responseBody.toString());
    }

    private static URI getApiUri(String api, String path,String query) {
        String url = ServiceConfig.getConfig().getString("config.api-base-url")
                +"/"+api+"/"+path;
        if (query != null) {
            url += "?"+query;
        }
        URI uri = URI.create(url);
        return uri;
    }

    private static HttpURLConnection openConnection(String method, URI uri, HttpHeaders headers, String accessToken) {
        HttpURLConnection connection;
        try {
            connection = (HttpURLConnection) uri.toURL().openConnection();
        } catch (IOException e) {
            throw new InternalServiceException("Unable to open connection to "+uri);
        }

        try {
            connection.setRequestMethod(method);
        } catch (ProtocolException e) {
            throw new InternalServiceException("Unable to set request method "+method);
        }

        connection.setRequestProperty("Authorization","Bearer "+accessToken);

        try {
            connection.connect();
        } catch (IOException e) {
            throw new ServiceException("Unable to open connection to "+uri,Response.Status.BAD_GATEWAY);
        }
        return connection;
    }



}

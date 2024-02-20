package dk.kb.oauth;

import dk.kb.oauth.config.ServiceConfig;
import dk.kb.util.webservice.exception.InternalServiceException;
import dk.kb.util.webservice.exception.ServiceException;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ProxyHelper {


    public  static HttpURLConnection openConnection(String method, URI uri, HttpHeaders requestHeaders, String accessToken) {
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



    public static URI getApiUri(String api, String path,String query) {
        String url = ServiceConfig.getConfig().getString("config.api-base-url")
                +"/"+api+"/"+path;
        if (query != null) {
            url += "?"+query;
        }
        URI uri = URI.create(url);
        return uri;
    }

    public static StreamingOutput createStreamingOutput(HttpURLConnection connection) throws IOException {
        InputStream inputStream = connection.getInputStream();
        return outputStream -> {
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            inputStream.close();
            outputStream.close();
        };
    }
}

package dk.kb.oauth;

import dk.kb.oauth.config.ServiceConfig;
import dk.kb.util.webservice.exception.InternalServiceException;
import dk.kb.util.webservice.exception.ServiceException;
import dk.kb.util.yaml.NotFoundException;
import dk.kb.util.yaml.YAML;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import java.io.IOException;
import java.io.InputStream;
import java.net.*;


public class ProxyHelper {

    private static final Logger log = LoggerFactory.getLogger(ProxyHelper.class);

    public static HttpURLConnection openConnection(String method, URI uri, HttpHeaders requestHeaders, String accessToken) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) uri.toURL().openConnection();
        connection.setRequestMethod(method);
        connection.setRequestProperty("Authorization","Bearer "+accessToken);
        connection.connect();
        return connection;
    }


    /**
     * Get the URI for a backend api to proxy
     *
     * @param api the api to proxy
     * @param path the path to proxy
     * @param query query parameters
     * @return the URI to the API call
     */
    public static URI getApiUri(String api, String path,String query) {

        YAML apiConfig;
        try {
            apiConfig = ServiceConfig.getConfig().getSubMap("apis." + api);
        } catch (NotFoundException e) {
            throw new ServiceException(Response.Status.NOT_FOUND);
        }

        String url = apiConfig.get("baseURL")+"/"+path;

        if (query != null) {
            url += "?"+query;
        }
        URI uri = URI.create(url);
        return uri;
    }

    /**
     * Streams the content for a given inputstream
     *
     * @param inputStream
     * @return
     * @throws IOException
     */
    public static StreamingOutput createStreamingOutput(InputStream inputStream) throws IOException {
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

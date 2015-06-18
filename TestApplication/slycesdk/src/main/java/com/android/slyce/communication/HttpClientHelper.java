package com.android.slyce.communication;

import com.android.slyce.utils.Constants;
import java.io.IOException;
import java.net.URISyntaxException;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.auth.DigestScheme;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.util.EntityUtils;

public class HttpClientHelper {

    public static String SEARCH_API = "/v2/search";
    public static String ECHO_API = "/v2/echo";

    private static String IMAGE_URL_PARAM = "image_url";

    // Boilerplate
    private static HttpHost targetHost;
    private static BasicHttpContext context;
    private static DefaultHttpClient client;

    public HttpClientHelper(final String key, final String password){
        init(key, password);
    }

    private void init(final String key, final String password) {
        client = new DefaultHttpClient();
        targetHost = new HttpHost(Constants.MS_HOST, Constants.MS_PORT, Constants.MS_SCHEME);
        AuthScope authScope =
                new AuthScope(targetHost.getHostName(), targetHost.getPort());
        UsernamePasswordCredentials credentials =
                new UsernamePasswordCredentials(key, password);
        client.getCredentialsProvider().setCredentials(authScope, credentials);
        context = new BasicHttpContext();
        context.setAttribute("preemptive-auth", new DigestScheme());
    }

    public void clean() {
        client.getConnectionManager().shutdown();
    }

    public String dispatch(HttpRequest request) throws IOException {
        HttpResponse response = client.execute(targetHost, request, context);
        HttpEntity entity = response.getEntity();
        String responseString = EntityUtils.toString(entity, "UTF-8");

        return responseString;
    }

    public HttpGet createImageUrlSearchRequest(String api, String imageurl) throws IOException, URISyntaxException {

        StringBuilder url = new StringBuilder();
        url.append(api).append("?").append(IMAGE_URL_PARAM).append("=").append(imageurl);

        HttpGet request = new HttpGet(url.toString());

        return request;
    }
}

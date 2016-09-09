package services.impls;

import com.google.api.client.http.HttpBackOffUnsuccessfulResponseHandler;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.util.ExponentialBackOff;

import java.io.IOException;

public class HttpRequestInitializer implements com.google.api.client.http.HttpRequestInitializer {

    @Override
    public void initialize(HttpRequest httpRequest) throws IOException {
        httpRequest.setUnsuccessfulResponseHandler(new HttpBackOffUnsuccessfulResponseHandler(new ExponentialBackOff()));
    }
}

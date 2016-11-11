package services.impls;

import com.google.api.client.http.*;
import com.google.common.util.concurrent.*;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import services.contracts.HttpService;
import services.contracts.ProgressService;

import javax.annotation.Nullable;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;

public class DefaultHttpService implements HttpService {
    private final HttpRequestFactory requestFactory;
    private final RateLimiter rateLimiter;
    private final ListeningExecutorService executorService;
    private final ProgressService progressService;
    private Integer totalRequests = 0;

    enum Method {
        GET,
        POST
    }

    @Inject
    public DefaultHttpService(HttpTransport transport, RateLimiter rateLimiter, @Named("HttpExecutor") ListeningExecutorService listeningExecutorService, ProgressService progressService) {
        this.requestFactory = transport.createRequestFactory(new HttpRequestInitializer());
        this.rateLimiter = rateLimiter;
        this.executorService = listeningExecutorService;
        this.progressService = progressService;
    }

    public ListenableFuture<HttpResponse> get(final String url) {
        ListenableFuture<HttpResponse> responseFuture = executorService.submit(() -> {
            rateLimiter.acquire();
            System.out.println("Request : " + url);
            GenericUrl genericUrl = new GenericUrl(url);
            HttpRequest request = requestFactory.buildGetRequest(genericUrl);
            return request.execute();
        });
        return responseFuture;
    }
}
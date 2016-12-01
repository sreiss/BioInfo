package services.impls;

import com.google.api.client.http.*;
import com.google.common.base.Function;
import com.google.common.util.concurrent.*;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import services.contracts.HttpService;
import services.contracts.ProgramStatsService;
import services.contracts.ProgressService;

import javax.annotation.Nullable;
import java.io.*;
import java.net.SocketTimeoutException;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class DefaultHttpService implements HttpService {
    private final HttpRequestFactory requestFactory;
    private final RateLimiter rateLimiter;
    private final ListeningExecutorService executorService;
    private final ProgramStatsService programStatsService;

    @Inject
    public DefaultHttpService(HttpTransport transport, RateLimiter rateLimiter, @Named("HttpExecutor") ListeningExecutorService listeningExecutorService, ProgramStatsService programStatsService) {
        this.requestFactory = transport.createRequestFactory(new HttpRequestInitializer());
        this.rateLimiter = rateLimiter;
        this.executorService = listeningExecutorService;
        this.programStatsService = programStatsService;
    }

    public ListenableFuture<HttpResponse> get(final String url) {
        ListenableFuture<HttpResponse> responseFuture = executorService.submit(() -> {
            rateLimiter.acquire();
            System.out.println("Request : " + url);
            GenericUrl genericUrl = new GenericUrl(url);
            HttpRequest request = requestFactory.buildGetRequest(genericUrl);
            return request.execute();
        });
        ListenableFuture<HttpResponse> failureTestFuture = Futures.transformAsync(responseFuture, httpResponse -> {
            if (httpResponse == null) {
                return get(url);
            }

            BufferedReader br = new BufferedReader(new InputStreamReader(httpResponse.getContent()));
            br.mark(1);
            int firstByte = br.read();
            if (firstByte == -1) {
                return get(url);
            }

            br.reset();

            return returnHttpResponse(httpResponse);
        }, executorService);

        return failureTestFuture;
    }

    private ListenableFuture<HttpResponse> returnHttpResponse(HttpResponse httpResponse) {
        return executorService.submit(() -> httpResponse);
    }
}
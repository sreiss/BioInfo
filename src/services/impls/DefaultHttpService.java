package services.impls;

import com.google.api.client.http.*;
import com.google.common.base.Function;
import com.google.common.util.concurrent.*;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import services.contracts.ApiStatus;
import services.contracts.HttpService;
import services.contracts.ProgramStatsService;
import services.contracts.ProgressService;

import javax.annotation.Nullable;
import java.awt.*;
import java.io.*;
import java.net.SocketException;
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
    private final ProgressService progressService;

    @Inject
    public DefaultHttpService(HttpTransport transport, RateLimiter rateLimiter, @Named("HttpExecutor") ListeningExecutorService listeningExecutorService, ProgramStatsService programStatsService, ProgressService progressService) {
        this.requestFactory = transport.createRequestFactory(new HttpRequestInitializer());
        this.rateLimiter = rateLimiter;
        this.executorService = listeningExecutorService;
        this.programStatsService = programStatsService;
        this.progressService = progressService;
    }

    public ListenableFuture<HttpResponse> get(final String url) {
        return get(url, null);
    }

    public ListenableFuture<HttpResponse> get(final String url, final String geneId) {
        ListenableFuture<HttpResponse> responseFuture = executorService.submit(() -> {
            rateLimiter.acquire();
            System.out.println("Request : " + url);
            if (geneId != null) {
                progressService.getCurrentDownloadProgress().setDownloading(geneId);
                progressService.invalidateDownloadProgress();
            }
            GenericUrl genericUrl = new GenericUrl(url);
            HttpRequest request = requestFactory.buildGetRequest(genericUrl);
            return request.execute();
        });
        ListenableFuture<HttpResponse> failureCatchingFuture = Futures.catchingAsync(responseFuture, Throwable.class, exception -> {
            if (exception instanceof SocketTimeoutException) {
                progressService.getCurrentApiStatus().setMessage("There seems to be a problem with the API, the last few requests where not answered. The processing might stop for a while.");
                progressService.getCurrentApiStatus().setColor(ApiStatus.OFFLINE_COLOR);
                progressService.invalidateApiStatus();
            }
            return get(url, geneId);
        }, executorService);

        return Futures.transformAsync(failureCatchingFuture, httpResponse -> {
            progressService.getCurrentApiStatus().setMessage("API Online");
            progressService.getCurrentApiStatus().setColor(ApiStatus.ONLINE_COLOR);
            progressService.invalidateApiStatus();
            if (httpResponse == null) {
                return get(url);
            }

            /*
            BufferedReader br = new BufferedReader(new InputStreamReader(httpResponse.getContent()));
            br.mark(1);
            int firstByte = br.read();
            if (firstByte == -1) {
                return get(url);
            }

            br.reset();
            */

            return Futures.immediateFuture(httpResponse);
        }, executorService);
    }
}
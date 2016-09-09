package services.impls;

import com.google.api.client.http.*;
import com.google.common.util.concurrent.RateLimiter;
import com.google.inject.Inject;
import org.jdeferred.*;
import org.jdeferred.impl.DeferredObject;
import org.jdeferred.multiple.MasterProgress;
import org.jdeferred.multiple.MultipleResults;
import org.jdeferred.multiple.OneReject;
import services.contracts.HttpService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Callable;

public class DefaultHttpService implements HttpService {
    private final DeferredManager deferredManager;
    private final HttpRequestFactory requestFactory;
    private final RateLimiter rateLimiter;
    private Integer totalRequests = 0;

    enum Method {
        GET,
        POST
    }

    @Inject
    public DefaultHttpService(DeferredManager deferredManager, HttpTransport transport, RateLimiter rateLimiter) {
        this.deferredManager = deferredManager;
        this.requestFactory = transport.createRequestFactory(new HttpRequestInitializer());
        this.rateLimiter = rateLimiter;
    }

    private Promise<HttpResponse, Throwable, Void> request(Method method, String url) {
        return request(method, url, null);
    }

    /**
     * Returns a promise to handle the HttpResponse.
     */
    private Promise<HttpResponse, Throwable, Void> request(final Method method, final String url, final HttpContent content) {
        return deferredManager.when(new Callable<HttpResponse>() {
            public HttpResponse call() throws IOException {
                rateLimiter.acquire();
                GenericUrl genericUrl = new GenericUrl(url);
                HttpRequest request;
                switch (method) {
                    case GET:
                        request = requestFactory.buildGetRequest(genericUrl);
                        break;
                    case POST:
                        request = requestFactory.buildPostRequest(genericUrl, content);
                        break;
                    default:
                        throw new IllegalArgumentException("Unhandled request type.");
                }
                return request.execute();
            }
        }).then(new DonePipe<HttpResponse, HttpResponse, Throwable, Void>() {
            @Override
            public Promise<HttpResponse, Throwable, Void> pipeDone(HttpResponse response) {
                System.out.println("[" + new Date() + "] Requête terminée : " + url);
                return new DeferredObject<HttpResponse, Throwable, Void>().resolve(response);
            }
        });
    }

    /**
     * Executes a get request and returns a Promise.
     */
    public Promise<HttpResponse, Throwable, Void> get(final String url) {
        return request(Method.GET, url);
    }

    /**
     * Executes a post request and returns a Promise.
     */
    public Promise<HttpResponse, Throwable, Void> post(final String url, HttpContent content) {
        return request(Method.POST, url, content);
    }

    /**
     * Executes multiple get requests and returns a Promise.
     */
    public Promise<MultipleResults, OneReject, MasterProgress> get(final String[] urls) {
        return deferredManager.when(new Callable<List<Promise<HttpResponse, Throwable, Void>>>() {
            @Override
            public List<Promise<HttpResponse, Throwable, Void>> call() throws Exception {
                List<Promise<HttpResponse, Throwable, Void>> promises = new ArrayList<Promise<HttpResponse, Throwable, Void>>();
                for (final String url: urls) {
                    promises.add(get(url));
                }
                return promises;
            }
        }).then(new DonePipe<List<Promise<HttpResponse, Throwable, Void>>, MultipleResults, OneReject, MasterProgress>() {
            @Override
            public Promise<MultipleResults, OneReject, MasterProgress> pipeDone(List<Promise<HttpResponse, Throwable, Void>> promises) {
                return deferredManager.when(promises.toArray(new Promise[promises.size()]));
            }
        }).fail(new FailCallback<OneReject>() {
            @Override
            public void onFail(OneReject oneReject) {
                get(urls);
            }
        });
    }
}
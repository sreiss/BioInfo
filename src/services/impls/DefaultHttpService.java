package services.impls;

import com.google.api.client.http.*;
import com.google.common.util.concurrent.RateLimiter;
import com.google.inject.Inject;
import org.jdeferred.*;
import org.jdeferred.impl.DeferredObject;
import org.jdeferred.multiple.MasterProgress;
import org.jdeferred.multiple.MultipleResults;
import org.jdeferred.multiple.OneReject;
import org.jdeferred.multiple.OneResult;
import services.contracts.HttpService;
import services.contracts.TaskProgress;
import services.contracts.UtilService;

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

    private Promise<HttpResponse, Throwable, Object> request(Method method, String url) {
        return request(method, url, null);
    }

    /**
     * Returns a promise to handle the HttpResponse.
     */
    private Promise<HttpResponse, Throwable, Object> request(final Method method, final String url, final HttpContent content) {
        return deferredManager.when(new DeferredCallable<HttpResponse, Object>() {
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
        }).then(new DonePipe<HttpResponse, HttpResponse, Throwable, Object>() {
            @Override
            public Promise<HttpResponse, Throwable, Object> pipeDone(HttpResponse response) {
                System.out.println("[" + new Date() + "] Requête terminée : " + url);
                return new DeferredObject<HttpResponse, Throwable, Object>().resolve(response);
            }
        });
    }

    /**
     * Executes a get request and returns a Promise.
     */
    public Promise<HttpResponse, Throwable, Object> get(final String url) {
        return request(Method.GET, url);
    }

    /**
     * Executes a post request and returns a Promise.
     */
    public Promise<HttpResponse, Throwable, Object> post(final String url, HttpContent content) {
        return request(Method.POST, url, content);
    }

    /**
     * Executes multiple get requests and returns a Promise.
     */
    public Promise<List<HttpResponse>, Throwable, Object> get(final List<String> urls) {
        return deferredManager.when(new DeferredCallable<Void, Object>() {
            @Override
            public Void call() throws Exception {
                notify(new TaskProgress(urls.size()));
                return null;
            }
        })
                .then(new DonePipe<Void, MultipleResults, OneReject, MasterProgress>() {
                    @Override
                    public Promise<MultipleResults, OneReject, MasterProgress> pipeDone(Void aVoid) {
                        List<Promise<HttpResponse, Throwable, Object>> promises = new ArrayList<Promise<HttpResponse, Throwable, Object>>();
                        for (final String url: urls) {
                            promises.add(get(url));
                        }
                        return deferredManager.when(promises.toArray(new Promise[promises.size()]));
                    }
                })
                .then(new DonePipe<MultipleResults, List<HttpResponse>, Throwable, Object>() {
                    @Override
                    public Promise<List<HttpResponse>, Throwable, Object> pipeDone(MultipleResults oneResults) {
                        List<HttpResponse> responses = new ArrayList<HttpResponse>();
                        for (OneResult oneResult: oneResults) {
                            responses.add((HttpResponse) oneResult.getResult());
                        }
                        return new DeferredObject<List<HttpResponse>, Throwable, Object>().resolve(responses);
                    }
                }, new FailPipe<OneReject, List<HttpResponse>, Throwable, Object>() {
                    @Override
                    public Promise<List<HttpResponse>, Throwable, Object> pipeFail(OneReject oneReject) {
                        return new DeferredObject<List<HttpResponse>, Throwable, Object>().reject((Throwable) oneReject.getReject());
                    }
                })
                .then(new DonePipe<List<HttpResponse>, List<HttpResponse>, Throwable, Object>() {
                    @Override
                    public Promise<List<HttpResponse>, Throwable, Object> pipeDone(List<HttpResponse> responses) {
                        return new DeferredObject<List<HttpResponse>, Throwable, Object>().resolve(responses);
                    }
                }, new FailPipe<Throwable, List<HttpResponse>, Throwable, Object>() {
                    @Override
                    public Promise<List<HttpResponse>, Throwable, Object> pipeFail(Throwable throwable) {
                        // Retry on fail.
                        return get(urls);
                    }
                });
    }
}
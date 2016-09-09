package services.contracts;

import com.google.api.client.http.HttpContent;
import com.google.api.client.http.HttpResponse;
import org.jdeferred.Promise;
import org.jdeferred.multiple.MasterProgress;
import org.jdeferred.multiple.MultipleResults;
import org.jdeferred.multiple.OneReject;

public interface HttpService {
    Promise<HttpResponse, Throwable, Void> get(final String url);
    Promise<HttpResponse, Throwable, Void> post(final String url, HttpContent content);
    Promise<MultipleResults, OneReject, MasterProgress> get(final String[] urls);
}

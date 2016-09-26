package services.impls;

import com.google.api.client.http.HttpResponse;
import com.google.inject.Inject;
import models.Kingdom;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.jdeferred.*;
import org.jdeferred.impl.DeferredObject;
import org.jdeferred.multiple.MasterProgress;
import org.jdeferred.multiple.MultipleResults;
import org.jdeferred.multiple.OneReject;
import services.contracts.*;

import java.util.ArrayList;
import java.util.List;

public class DefaultDataService implements DataService {
    private final HttpService httpService;
    private final ParseService parseService;
    private final FileService fileService;
    private final ConfigService configService;
    private final DeferredManager deferredManager;

    @Inject
    public DefaultDataService(HttpService httpService, ParseService parseService, FileService fileService, ConfigService configService, DeferredManager deferredManager) {
        this.httpService = httpService;
        this.parseService = parseService;
        this.fileService = fileService;
        this.configService = configService;
        this.deferredManager = deferredManager;
    }

    private String generateKingdomGeneListUrl(Kingdom kingdom) {
        return "http://www.ncbi.nlm.nih.gov/genomes/Genome2BE/genome2srv.cgi?action=download&orgn=&report=" + kingdom.getId() + "&status=50|40|30|20|&group=--%20All%20" + kingdom.getLabel() + "%20--&subgroup=--%20All%20" + kingdom.getLabel() + "%20--";
    }

    @Override
    public Promise<Void, Throwable, Object> acquire(Kingdom[] kingdoms) {
        List<String> urls = new ArrayList<String>();
        for (Kingdom kingdom: kingdoms) {
            urls.add(generateKingdomGeneListUrl(kingdom));
        }

        return this.httpService.get(urls)
                .then(new DonePipe<List<HttpResponse>, Void, Throwable, Object>() {
                    @Override
                    public Promise<Void, Throwable, Object> pipeDone(List<HttpResponse> httpResponses) {
                        return saveData(httpResponses);
                    }
                });
    }

    public Promise<Void, Throwable, Object> saveData(List<HttpResponse> responses) {
        return deferredManager.when(configService.getProperty("dataDir"), fileService.createWorkbook())
                .then(new DonePipe<MultipleResults, Void, Throwable, Object>() {
                    @Override
                    public Promise<Void, Throwable, Object> pipeDone(MultipleResults oneResults) {
                        String dataDir = ((String) oneResults.get(0).getResult());
                        XSSFWorkbook workbook = ((XSSFWorkbook) oneResults.get(1).getResult());

                        workbook.createSheet("test");
                        return fileService.writeWorkbook(workbook, dataDir, "test.xlsx");
                    }
                }, new FailPipe<OneReject, Void, Throwable, Object>() {
                    @Override
                    public Promise<Void, Throwable, Object> pipeFail(OneReject oneReject) {
                        return new DeferredObject<Void, Throwable, Object>().reject((Throwable) oneReject.getReject());
                    }
                });
    }
}

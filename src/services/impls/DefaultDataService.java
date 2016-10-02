package services.impls;

import com.google.api.client.http.HttpResponse;
import com.google.inject.Inject;
import com.sun.org.apache.xpath.internal.operations.Or;
import models.Gene;
import models.Kingdom;
import models.Organism;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.jdeferred.*;
import org.jdeferred.impl.DeferredObject;
import org.jdeferred.multiple.MasterProgress;
import org.jdeferred.multiple.MultipleResults;
import org.jdeferred.multiple.OneReject;
import org.jdeferred.multiple.OneResult;
import services.contracts.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class DefaultDataService implements DataService {
    private final HttpService httpService;
    private final ParseService parseService;
    private final FileService fileService;
    private final ConfigService configService;
    private final DeferredManager deferredManager;
    private final KingdomService kingdomService;
    private final OrganismService organismService;

    @Inject
    public DefaultDataService(HttpService httpService, ParseService parseService, FileService fileService, ConfigService configService, DeferredManager deferredManager, KingdomService kingdomService, OrganismService organismService) {
        this.httpService = httpService;
        this.parseService = parseService;
        this.fileService = fileService;
        this.configService = configService;
        this.deferredManager = deferredManager;
        this.kingdomService = kingdomService;
        this.organismService = organismService;
    }

    private String generateKingdomGeneListUrl(Kingdom kingdom) {
        return "http://www.ncbi.nlm.nih.gov/genomes/Genome2BE/genome2srv.cgi?action=download&orgn=&report=" + kingdom.getId() + "&status=50|40|30|20|&group=--%20All%20" + kingdom.getLabel() + "%20--&subgroup=--%20All%20" + kingdom.getLabel() + "%20--";
    }

    @Override
    public Promise<Void, Throwable, Object> acquire(final List<Kingdom> kingdoms) {
        List<String> urls = new ArrayList<String>();
        for (Kingdom kingdom: kingdoms) {
            urls.add(generateKingdomGeneListUrl(kingdom));
        }

        return this.httpService.get(urls)
                .then(new DonePipe<List<HttpResponse>, Void, Throwable, Object>() {
                    @Override
                    public Promise<Void, Throwable, Object> pipeDone(List<HttpResponse> httpResponses) {
                        return saveData(httpResponses, kingdoms);
                    }
                });
    }

    public Promise<Void, Throwable, Object> saveData(final List<HttpResponse> responses, final List<Kingdom> kingdoms) {
        return deferredManager.when(new DeferredCallable<Void, Object>() {
            @Override
            public Void call() throws Exception {
                notify(new TaskProgress(responses.size()));
                return null;
            }
        })
                .then(new DonePipe<Void, MultipleResults, OneReject, MasterProgress>() {
                    @Override
                    public Promise<MultipleResults, OneReject, MasterProgress> pipeDone(Void aVoid) {
                        List<Promise<Kingdom, Throwable, Object>> promises = new ArrayList<Promise<Kingdom, Throwable, Object>>();

                        for (int i = 0; i < responses.size(); i++) {
                            HttpResponse response = responses.get(i);
                            Promise<Kingdom, Throwable, Object> promise = null;
                            try {
                                promise = kingdomService.createKingdomTree(kingdoms.get(i), response.getContent());
                                promises.add(promise);
                            } catch (Exception e) {
                                return new DeferredObject<MultipleResults, OneReject, MasterProgress>().reject(new OneReject(i, promise, e));
                            }
                        }

                        return deferredManager.when(promises.toArray(new Promise[promises.size()]));
                    }
                })
                .then(new DonePipe<MultipleResults, List<Kingdom>, Throwable, Object>() {
                    @Override
                    public Promise<List<Kingdom>, Throwable, Object> pipeDone(MultipleResults oneResults) {
                        DeferredObject<List<Kingdom>, Throwable, Object> deferred = new DeferredObject<List<Kingdom>, Throwable, Object>();
                        List<Kingdom> kingdoms = new ArrayList<Kingdom>();
                        for (OneResult oneResult: oneResults) {
                            kingdoms.add((Kingdom) oneResult.getResult());
                        }
                        deferred.resolve(kingdoms);
                        return deferred.promise();
                    }
                }, new FailPipe<OneReject, List<Kingdom>, Throwable, Object>() {
                    @Override
                    public Promise<List<Kingdom>, Throwable, Object> pipeFail(OneReject oneReject) {
                        return new DeferredObject<List<Kingdom>, Throwable, Object>().reject((Throwable) oneReject.getReject());
                    }
                })
                .then(new DonePipe<List<Kingdom>, Void, Throwable, Object>() {
                    @Override
                    public Promise<Void, Throwable, Object> pipeDone(List<Kingdom> kingdoms) {
                        DeferredObject<Void, Throwable, Object> deferred = new DeferredObject<Void, Throwable, Object>();
                        deferred.resolve(null);
                        return deferred.promise();
                    }
                });

        // retourne le nombre de processeurs logiques que poss�de l'ordi
//                        int cores = Runtime.getRuntime().availableProcessors();
//                        URL url = getUrlForList(kingdoId);
//                        URLConnection urlConn = url.openConnection();
//                        urlConn.connect();
//                        InputStream is = urlConn.getInputStream();
//                        BufferedReader br = new BufferedReader(new InputStreamReader(is));
//
//                        String line = br.readLine();
//
//                        ArrayList<Organism> listOrganism = new ArrayList<Organism>();
//
//                        while ((line = br.readLine()) != null) {
//                            Organism organism = new Organism(line, kingdoId);
//                            Kingdom kingdom = kingdomService.getKingdomById(kingdoId);
//                            String addr="Data/"+kingdom.getLabel()+"/"+organism.group+"/"+organism.subGroup/*+"/"+organism.name*/;
//
//                            //System.out.println(addr);
//
//                            @SuppressWarnings("unused")
//                            boolean success = (new File(addr)).mkdirs();
//
//                            listOrganism.add(organism);
//                        }
//
//                        int listPartSize = listOrganism.size() / cores;
//                        int modPart = listOrganism.size() % cores;
//                        System.out.println("list size =" + listOrganism.size());
//                        int cpt = 1, tmpEnd;
//                        ExecutorService executorService = ExecutorPoolService.getInstance()
//                                .getExecutorService();
//                        for (int i = 0; i < listOrganism.size() - listPartSize; i += listPartSize) {
//
//                            if(i + listPartSize + modPart == listOrganism.size())
//                                tmpEnd = i + listPartSize + modPart;
//                            else
//                                tmpEnd = i + listPartSize;
//
//                            executorService.submit(new ExcelThread(listOrganism, kingdoId, i, tmpEnd));
//                            //ExcelThread t = new ExcelThread("Thread_"+ cpt, listOrganism, index, i, tmpEnd);
//
//                            //t.start();
//
//                            cpt++;
//                        }
//                        System.out.println("END");

//                        workbook.createSheet("test");
//                        return fileService.writeWorkbook(workbook, dataDir, "test.xlsx");


//                    .then(new DonePipe<MultipleResults, Void, Throwable, Object>() {
//                        return deferredManager.when(promises.toArray(new Promise[promises.size()]));
//                    })
//                }, new FailPipe<OneReject, Void, Throwable, Object>() {
//                    @Override
//                    public Promise<Void, Throwable, Object> pipeFail(OneReject oneReject) {
//                        return new DeferredObject<Void, Throwable, Object>().reject((Throwable) oneReject.getReject());
//                    }
//                })
//                .then(new DonePipe<MultipleResults, Void, Throwable, Object>() {
//                    @Override
//                    public Promise<Void, Throwable, Object> pipeDone(MultipleResults oneResults) {
//                        return null;
//                    }
//                });
    }
}

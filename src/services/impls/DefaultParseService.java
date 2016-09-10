package services.impls;

import com.google.inject.Inject;
import models.CodingSequence;
import models.Gene;
import models.Kingdom;
import models.Organism;
import org.jdeferred.DeferredManager;
import org.jdeferred.DonePipe;
import org.jdeferred.Promise;
import org.jdeferred.impl.DeferredObject;
import org.jdeferred.multiple.MasterProgress;
import org.jdeferred.multiple.MultipleResults;
import org.jdeferred.multiple.OneReject;
import org.jdeferred.multiple.OneResult;
import services.contracts.OrganismService;
import services.contracts.ParseService;
import services.contracts.StatisticsService;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DefaultParseService implements ParseService {
    private final DeferredManager deferredManager;
    private final OrganismService organismService;

    @Inject
    public DefaultParseService(DeferredManager deferredManager, OrganismService organismService) {
        this.deferredManager = deferredManager;
        this.organismService = organismService;
    }

    public static boolean checkLocator(String locator) {
        String[] indexes = locator.split("\\.\\.");
        return indexes.length == 2 && Integer.parseInt(indexes[0]) < Integer.parseInt(indexes[1]);
    }

    public static boolean checkSequence(String sequence) {
        return (sequence.length() % 3 == 0)
                && CodingSequence.InitCodon.contains(sequence)
                && CodingSequence.StopCodon.contains(sequence.substring(sequence.length() - 3))
                && !Pattern.compile(CodingSequence.REGEX_ATGC).matcher(sequence).find();
    }

    public Promise<List<String>, Throwable, Void> extractSequences(final BufferedReader reader) {
        return deferredManager.when(new Callable<List<String>>() {
            @Override
            public List<String> call() throws Exception {
                List<String> sequences = new ArrayList<String>();
                String line;
                boolean running;

                while ((line = reader.readLine()) != null) {
                    if (line.startsWith(CodingSequence.START_CDS_INFO)) {
                        Pattern pattern = Pattern.compile(CodingSequence.REGEX_COMPLETE);
                        Matcher matcher = pattern.matcher(line);

                        if (matcher.find()) {
                            String s = matcher.group();
                            pattern = Pattern.compile(CodingSequence.REGEX_LOCATOR);
                            matcher = pattern.matcher(s);
                            boolean locatorsOk = true;
                            while (matcher.find() && locatorsOk) {
                                locatorsOk = checkLocator(matcher.group());
                            }

                            if (locatorsOk) {
                                String sequence = "", line2;

                                running = true;
                                while (running) {
                                    reader.mark(1);
                                    int character = reader.read();
                                    if (character == -1) {
                                        running = false;
                                    } else {
                                        reader.reset();
                                        if (character == CodingSequence.START_CDS_INFO.charAt(0)) {
                                            running = false;
                                        } else {
                                            line2 = reader.readLine();
                                            if (line2 == null) {
                                                running = false;
                                            } else {
                                                sequence += line2;
                                            }
                                        }
                                    }
                                }

                                if (checkSequence(sequence)) {
                                    sequences.add(sequence);
                                }
                            }

                        }
                    }
                }

                return sequences;
            }
        });
    }

    @Override
    public Promise<List<Organism>, Throwable, Void> extractOrganismList(final InputStream inputStream, final String kingdomId) {
        return deferredManager.when(new Callable<List<Promise<Organism, Throwable, Void>>>() {
            @Override
            public List<Promise<Organism, Throwable, Void>> call() throws Exception {
                List<Promise<Organism, Throwable, Void>> promises = new ArrayList<Promise<Organism, Throwable, Void>>();
                DateFormat dateFormat = new SimpleDateFormat("dd/mm/yyyy", Locale.FRENCH);
                String sep = "\t";

                String line;

                InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                while ((line = bufferedReader.readLine()) != null) {
                    String[] data = line.split(sep);

                    String name;
                    String group;
                    String subGroup;
                    Date updateDate = null;
                    String[] geneIds;

                    if (Kingdom.Eukaryota.equals(kingdomId)) {
                        name = data[0];
                        group = data[4];
                        subGroup = data[5];
                        geneIds = extractGeneIds(data[9]);
                        // TODO: udpateDate index: 15
                    } else if (Kingdom.Prokaryotes.equals(kingdomId)) {
                        name = data[0];
                        group = data[5];
                        subGroup = data[6];
                        geneIds = extractGeneIds(data[10]);
                        // TODO: updateDate index: 16
                    } else if (Kingdom.Viruses.equals(kingdomId)) {
                        name = data[0];
                        group = data[2];
                        subGroup = data[3];
                        geneIds = extractGeneIds(data[7]);
                        // TODO: updateDate index: 11
                    } else {
                        name = null;
                        group = null;
                        subGroup = null;
                        geneIds = null;
                    }

                    promises.add(organismService.createOrganism(name, group, subGroup, updateDate, geneIds, kingdomId));
                    /*
                    if (data.length > updatedDateIndex) {
                        if (data[updatedDateIndex].compareTo("-") == 0) {
                            updatedDate = new Date();
                        } else {
                            try {
                                updatedDate = format.parse(data[updatedDateIndex]);
                            } catch (ParseException e) {
                                System.err.println(e.getMessage());
                                e.printStackTrace();
                            }
                        }
                    }
                    */
                }
                inputStream.close();
                inputStreamReader.close();
                bufferedReader.close();

                return promises;
            }
        }).then(new DonePipe<List<Promise<Organism, Throwable, Void>>, MultipleResults, OneReject, MasterProgress>() {
            @Override
            public Promise<MultipleResults, OneReject, MasterProgress> pipeDone(List<Promise<Organism, Throwable, Void>> promises) {
                return deferredManager.when(promises.toArray(new Promise[promises.size()]));
            }
        }).then(new DonePipe<MultipleResults, List<Organism>, Throwable, Void>() {
            @Override
            public Promise<List<Organism>, Throwable, Void> pipeDone(MultipleResults oneResults) {
                List<Organism> organisms = new ArrayList<Organism>();
                for (OneResult oneResult: oneResults) {
                    organisms.add((Organism) oneResult.getResult());
                }
                return new DeferredObject<List<Organism>, Throwable, Void>().resolve(organisms);
            }
        });
    }

    private String[] extractGeneIds(String line)
    {
        String[] res;
        if(line.compareTo("-") != 0)
        {
            res = line.split(";");
            if(res.length>0)
            {
                for(int i=0;i<res.length;i++)
                {
                    res[i] = res[i].split(":")[1].split("/")[0];
                }
            }
            else
            {
                res = null;
            }
        }
        else{
            res = null;
        }
        return res;
    }
}

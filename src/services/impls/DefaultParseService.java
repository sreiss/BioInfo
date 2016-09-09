package services.impls;

import com.google.inject.Inject;
import models.CodingSequence;
import models.Gene;
import org.jdeferred.DeferredManager;
import org.jdeferred.Promise;
import services.contracts.ParseService;
import services.contracts.StatisticsService;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DefaultParseService implements ParseService {
    private final DeferredManager deferredManager;

    @Inject
    public DefaultParseService(DeferredManager deferredManager) {
        this.deferredManager = deferredManager;
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
}

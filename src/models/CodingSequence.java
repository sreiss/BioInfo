package models;

public class CodingSequence {

    public enum Nucleotide {
        A("A"),
        C("C"),
        G("G"),
        T("T");

        private final String letter;

        Nucleotide(String letter) {
            this.letter = letter;
        }

        public static boolean contains(String letter) {
            return valueOf(letter) != null;
        }
    }

    public enum InitCodon {
        ATA("ATA"),
        ATC("ATC"),
        ATG("ATG"),
        ATT("ATT"),
        CTG("CTG"),
        GTG("GTG"),
        TTA("TTA"),
        TTG("TTG");

        private final String codon;

        InitCodon(String codon) {
            this.codon = codon;
        }

        public static boolean contains(String codon) {
            return valueOf(codon) != null;
        }
    }

    public enum StopCodon {
        TAA("TAA"),
        TAG("TAG"),
        TGA("TGA");

        private final String codon;

        StopCodon(String codon) {
            this.codon = codon;
        }

        public static boolean contains(String codon) {
            return valueOf(codon) != null;
        }
    }

    public static String[] nucleobase = { "A", "C", "G", "T" };
    public static String[] codon_init = { "ATA", "ATC", "ATG", "ATT", "CTG", "GTG", "TTA", "TTG" };
    public static String[] codon_stop = { "TAA", "TAG", "TGA" };

    /*Regex*/
    public static final String START_CDS_INFO = ">";
    public static final String REGEX_LOCATION = "\\[location=.*\\]";
    public static final String REGEX_LOCATOR = "\\d+..\\d+";
    public static final String REGEX_JOIN = "join\\("+REGEX_LOCATOR+"(,"+REGEX_LOCATOR+")*\\)";
    public static final String REGEX_COMPLEMENT = "complement\\(("+REGEX_LOCATOR+"|"+REGEX_JOIN+")\\)";
    public static final String REGEX_COMPLETE = "\\[location=("+REGEX_LOCATOR+"|"+REGEX_JOIN+"|"+REGEX_COMPLEMENT+")\\]";
    public static final String REGEX_ATGC = "[^ACGT]";

    public int startIndex;
    public int endIndex;
    public String sequence;

    public CodingSequence(int startIndex, int endIndex) {
        this.startIndex = startIndex;
        this.endIndex = endIndex;
    }

    @Override
    public String toString() {
        return "["+startIndex+","+endIndex+"]";
    }

    public static LinkedHashMap<String, Integer> initLinkedHashMap() {
        LinkedHashMap<String, Integer> hash = new LinkedHashMap<String, Integer>();
        for (String s0 : nucleobase) {
            for (String s1 : nucleobase) {
                for (String s2 : nucleobase) {
                    hash.put(s0+s1+s2, 0);
                }
            }
        }
        return hash;
    }

    public static LinkedHashMap<String, Double> initLinkedHashMapProba() {
        LinkedHashMap<String, Double> hash = new LinkedHashMap<String, Double>();
        for (String s0 : nucleobase) {
            for (String s1 : nucleobase) {
                for (String s2 : nucleobase) {
                    hash.put(s0+s1+s2, 0.0);
                }
            }
        }
        return hash;
    }

    public static LinkedHashMap<String, Integer> initLinkedHashMapDinucleo() {
        LinkedHashMap<String, Integer> hash = new LinkedHashMap<String, Integer>();
        for (String s0 : nucleobase) {
            for (String s1 : nucleobase) {
                hash.put(s0 + s1, 0);
            }
        }
        return hash;
    }

    public static LinkedHashMap<String, Double> initLinkedHashMapDinucleoProba() {
        LinkedHashMap<String, Double> hash = new LinkedHashMap<String, Double>();
        for (String s0 : nucleobase) {
            for (String s1 : nucleobase) {
                hash.put(s0 + s1, 0.0);
            }
        }
        return hash;
    }

    /*public static Gene statsSequenceTrinucleo(ArrayList<String> tab_sequence, Gene g) {
    	String codon0, codon1, codon2;

    	String seq0 = tab_sequence.get(0); // seq0.length() == seq1.length() + 3 == seq2.length() + 3
        String seq1 = tab_sequence.get(1); // seq1.length() == seq2.length()
        String seq2 = tab_sequence.get(2); // seq1.length() == seq2.length()

        int j = 0;

        for (int i = 0; i < seq1.length(); i += 3) {
        	codon0 = seq0.substring(i, i+3);
        	codon1 = seq1.substring(i, i+3);
        	codon2 = seq2.substring(i, i+3);
            g.trinuStatPhase0.put(codon0, g.trinuStatPhase0.get(codon0) + 1);
            g.trinuStatPhase1.put(codon1, g.trinuStatPhase1.get(codon1) + 1);
            g.trinuStatPhase2.put(codon2, g.trinuStatPhase2.get(codon2) + 1);
            j += 1;
        }

        g.totalTrinucleotide = j;

        return g;
    }*/

    /*public static Gene statsSequenceDinucleo(ArrayList<String> tab_sequence, Gene g) {
    	String codon0, codon1;
    	String seq0 = tab_sequence.get(0);
        String seq1 = tab_sequence.get(1);
        int j = 0;
        for (int i = 0; i < seq1.length(); i += 2) {
        	codon0 = seq0.substring(i, i + 2);
        	codon1 = seq1.substring(i, i + 2);
            g.dinuStatPhase0.put(codon0, g.dinuStatPhase0.get(codon0) + 1);
            g.dinuStatPhase1.put(codon1, g.dinuStatPhase1.get(codon1) + 1);
            j += 3;
        }

        g.totalDinucleotide = j;

        return g;
    }*/

    public static Gene statsSequenceDinucleo(String seq, Gene g) {
        String codon0, codon1;
        int j = 0;

        for (int i = 0; i < seq.length()-(3+seq.length()%2)+1; i += 2) {
            codon0 = seq.substring(i, i + 2);
            codon1 = seq.substring(i+1, i + 3);
            g.dinuStatPhase0.put(codon0, g.dinuStatPhase0.get(codon0) + 1);
            g.dinuStatPhase1.put(codon1, g.dinuStatPhase1.get(codon1) + 1);
            j ++;
        }

        g.totalDinucleotide += j;

        return g;
    }

    /**
     * Methode de test pour les regex
     * @param regex regex à chercher
     * @param test string à parser
     */
    public static void RegexTester(String regex, String test) {
        Pattern p = Pattern.compile(regex);
        Matcher m = p.matcher(test);
        if (m.find())
            System.out.println(m.group());
        else
            System.out.println("FAUX");
    }

    public static void main(String[] args) {
        System.out.println(Pattern.compile(REGEX_ATGC).matcher("atcgggtttccca").find());
        RegexTester(REGEX_ATGC, "");
        System.out.println(checkLocator("298..654"));
    }
}

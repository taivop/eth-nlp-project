package annotatorstub.annotator.smaph;

import annotatorstub.annotator.FakeAnnotator;
import annotatorstub.utils.WATRelatednessComputer;
import annotatorstub.utils.bing.BingResult;
import annotatorstub.utils.bing.BingSearchAPI;
import annotatorstub.utils.mention.GreedyMentionIterator;
import annotatorstub.utils.mention.MentionCandidate;
import com.sun.tools.javac.util.Pair;
import it.unipi.di.acube.batframework.data.ScoredAnnotation;
import it.unipi.di.acube.batframework.data.Tag;
import it.unipi.di.acube.batframework.utils.AnnotationException;
import it.unipi.di.acube.batframework.utils.WikipediaApiInterface;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.net.ConnectException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * SMAPH-S annotator from the paper "A Piggyback System for Joint Entity Mention Detection and Linking in Web Queries".
 * @see http://www2016.net/proceedings/proceedings/p567.pdf
 */

public class SmaphSAnnotator extends FakeAnnotator {

    private static BingSearchAPI bingApi;
    private WikipediaApiInterface wikiApi;
    private CandidateEntitiesGenerator candidateEntitiesGenerator;
    private static final int TOP_K_SNIPPETS = 25;
    private final static Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    public SmaphSAnnotator() throws Exception {
        BingSearchAPI.KEY = "crECheFN9wPg0oAJWRZM7nfuJ69ETJhMzxXXjchNMSM";
        bingApi = BingSearchAPI.getInstance();

        this.wikiApi = WikipediaApiInterface.api();

        candidateEntitiesGenerator = new CandidateEntitiesGenerator();

        WATRelatednessComputer.setCache("relatedness.cache");
    }


    private static int minimum(int a, int b, int c) {
        return Math.min(Math.min(a, b), c);
    }

    private static Double average(Collection<Double> collection) {
        Double sum = 0.0;
        for(Double element : collection) {
            sum += element;
        }
        return sum / collection.size();
    }

    /**
     * Calculate Levenshtein edit distance between strings a and b.
     * @see https://en.wikibooks.org/wiki/Algorithm_Implementation/Strings/Levenshtein_distance#Java
     */
    private static int ED(CharSequence a, CharSequence b) {
        int[][] distance = new int[a.length() + 1][b.length() + 1];

        for (int i = 0; i <= a.length(); i++)
            distance[i][0] = i;
        for (int j = 1; j <= b.length(); j++)
            distance[0][j] = j;

        for (int i = 1; i <= a.length(); i++)
            for (int j = 1; j <= b.length(); j++)
                distance[i][j] = minimum(
                        distance[i - 1][j] + 1,
                        distance[i][j - 1] + 1,
                        distance[i - 1][j - 1] + ((a.charAt(i - 1) == b.charAt(j - 1)) ? 0 : 1));

        return distance[a.length()][b.length()];
    }

    /**
     * Calculate the MinED -- a measure of distance -- as described in the paper.
     */
    private static Double minED(String a, String b) {

        String[] termsInA = a.split(" ");
        String[] termsInB = b.split(" ");

        Double minDistancesSum = 0.0;

        for(String termInA : termsInA) {
            String closestInB = "";
            Double currentMinED = Double.POSITIVE_INFINITY;

            for(String termInB : termsInB) {
                Double editDistance = (double) ED(termInA, termInB);
                if(editDistance < currentMinED) {
                    closestInB = termInB;
                    currentMinED = editDistance;
                }
            }

            minDistancesSum += currentMinED;
        }

        return minDistancesSum / termsInA.length;
    }

    /**
     * Remove a trailing parenthetical string, e.g. 'Swiss (nationality)' -> 'Swiss '.
     * @param s string to truncate
     * @return string without trailing stuff in parentheses
     */
    private static String removeFinalParentheticalString(String s) {
        int lastOpeningParenIndex = s.lastIndexOf('(');
        int lastClosingParenIndex = s.lastIndexOf(')');

        if(lastClosingParenIndex > lastOpeningParenIndex) {     // Make sure the opening parenthesis is closed.
            return s.substring(0, lastOpeningParenIndex);
        } else {
            return s;
        }
    }

    /**
     * Check if string is capitalised (positive examples: Taivo, Escher-Wyss Platz; negative: OMG, banana).
     * TODO Not sure if all-caps strings ("OMG") should return true or not -- the article doesn't specify.
     */
    private static boolean isCapitalised(String s) {
        String[] parts = s.split(" |-");
        for(String part : parts) {
            boolean firstLetterUpperCase = Character.isUpperCase(part.charAt(0));
            boolean restLowerCase = part.substring(1) == part.substring(1).toLowerCase();

            if(!firstLetterUpperCase || !restLowerCase) {
                return false;
            }
        }

        return true;
    }


    /**
     * Returns the SMAPH-S features for given entity. See table 4 in article.
     *
     * @param entity the Wikipedia ID of the entity
     * @return vector of per-entity features
     */
    private Vector<Double> getEntityFeatures(Integer entity, String query, BingResult bingResult, CandidateEntities candidateEntities) throws IOException {
        // TODO have a cache for this so if we query the same entity again, we won't recalculate stuff.
        Vector<Double> features = new Vector<>();

        // ====================================================================================
        // region Features drawn from all sources

        Double f1_webTotal = bingResult.getWebTotal().doubleValue();

        //endregion
        // ------------------------------------------------------------------------------------



        // ====================================================================================
        //region Features drawn from sources Epsilon1 and Epsilon2
        Double f4_EDTitle = minED(wikiApi.getTitlebyId(entity), query);

        Double f5_EDTitNP = minED(removeFinalParentheticalString(wikiApi.getTitlebyId(entity)), query);

        Double f6_minEDBolds = Double.POSITIVE_INFINITY;
        Double f7_captBolds = 0.0;
        Double f8_boldTerms = 0.0;

        // TODO boldPortions doesn't need to be calculated again for every entity
        List<String> boldPortions = bingResult.getWebResults().stream().
                map(snippet -> snippet.getHighlightedWords()).
                flatMap(l -> l.stream()).
                collect(Collectors.toList());
        for(String boldPortion : boldPortions) {
            Double currentMinED = minED(boldPortion, query);
            if(currentMinED < f6_minEDBolds) {
                f6_minEDBolds = currentMinED;
            }

            // Check if word is capitalised (first letter uppercase and the rest lowercase)
            if(isCapitalised(boldPortion)) {
                f7_captBolds += 1;
            }

            f8_boldTerms += boldPortion.length();
        }
        f8_boldTerms /= boldPortions.size();
        //endregion
        // ------------------------------------------------------------------------------------




        // ====================================================================================
        //region Features drawn from source Epsilon3
        Double f9_freq = 0.0;
        Double f10_avgRank = 0.0;

        // mentionSnippetPairs: the set X(q) of pairs (mention, snippet) as explained in the paper
        ArrayList<Pair<String, String>> mentionSnippetPairs = new ArrayList<>();

        // TODO bingSnippets doesn't need to be calculated again for every entity
        // bingSnippets: snippets returned by querying bing with the original query
        List<String> bingSnippets = bingResult.getWebResults().stream().
                map(snippet -> snippet.getDescription()).
                collect(Collectors.toList()).
                subList(0, TOP_K_SNIPPETS);

        // snippetEntities: for each snippet, the set of entitities that were found by annotating the snippet with WAT
        List<Set<Integer>> snippetEntities = candidateEntities.getEntitiesQuerySnippetsWATBySnippet();

        // WATSnippetAnnotations: for each snippet, the set of annotations found by annotating the snippet with WAT
        List<Set<ScoredAnnotation>> WATSnippetAnnotations = candidateEntities.getWATSnippetAnnotations();

        int rankCounter = 0;
        for(String snippet : bingSnippets) {
            rankCounter++;

            boolean snippetHasEntity = snippetEntities.get(rankCounter-1).contains(entity); // Do we find our desired entity in the annotations of this snippet?
            Set<ScoredAnnotation> WATannotations = WATSnippetAnnotations.get(rankCounter-1);
            for(ScoredAnnotation scoredAnnotation : WATannotations) {

                String mention = snippet.substring(scoredAnnotation.getPosition(),
                        scoredAnnotation.getPosition() + scoredAnnotation.getLength());
                mentionSnippetPairs.add(new Pair<>(mention, snippet));
            }

            if(snippetHasEntity) {
                f9_freq += 1;
                f10_avgRank += rankCounter;
            } else {
                f10_avgRank += TOP_K_SNIPPETS;
            }
        }
        f9_freq /= bingSnippets.size();
        f10_avgRank /= TOP_K_SNIPPETS;

        Double f11_pageRank = SmaphSMockDataSources.getWikiPageRankScore(entity);


        ArrayList<Double> linkProbabilities = new ArrayList<>();
        ArrayList<Double> commonnesses = new ArrayList<>();
        ArrayList<Double> ambiguities = new ArrayList<>();
        ArrayList<Double> minEDs = new ArrayList<>();
        for(Pair<String, String> mentionSnippetPair : mentionSnippetPairs) {
            String mention = mentionSnippetPair.fst;
            String snippet = mentionSnippetPair.snd;

            linkProbabilities.add(SmaphSMockDataSources.getWikiLinkProbability(mention));
            commonnesses.add(SmaphSMockDataSources.getWikiCommonness(mention, entity));
            ambiguities.add(SmaphSMockDataSources.getWikiAmbiguity(mention));
            minEDs.add(minED(mention, query));
        }

        Double f15_lp_min = Collections.min(linkProbabilities);
        Double f16_lp_max = Collections.max(linkProbabilities);
        Double f17_comm_min = Collections.min(commonnesses);
        Double f18_comm_max = Collections.max(commonnesses);
        Double f19_comm_avg = average(commonnesses);
        Double f20_ambig_min = Collections.min(ambiguities);
        Double f21_ambig_max = Collections.max(ambiguities);
        Double f22_ambig_avg = average(ambiguities);
        Double f23_mentMED_min = Collections.min(minEDs);
        Double f24_mentMED_max = Collections.max(minEDs);

        //endregion
        // ------------------------------------------------------------------------------------




        // ====================================================================================
        //region Combine features into a vector

        features.add(f1_webTotal);
        features.add(f4_EDTitle);
        features.add(f5_EDTitNP);
        features.add(f6_minEDBolds);
        features.add(f7_captBolds);
        features.add(f8_boldTerms);
        features.add(f9_freq);
        features.add(f10_avgRank);
        features.add(f11_pageRank);
        features.add(f15_lp_min);
        features.add(f16_lp_max);
        features.add(f17_comm_min);
        features.add(f18_comm_max);
        features.add(f19_comm_avg);
        features.add(f20_ambig_min);
        features.add(f21_ambig_max);
        features.add(f22_ambig_avg);
        features.add(f23_mentMED_min);
        features.add(f24_mentMED_max);

        //endregion
        // ------------------------------------------------------------------------------------

        return features;
    }

    /**
     * Returns the SMAPH-S features for given mention-entity pair. See table 5 in article.
     *
     * @param mention the mention in query
     * @param entity the Wikipedia ID of the entity
     * @return vector of per-pair features
     */
    private Vector<Double> getMentionEntityFeatures(MentionCandidate mention, Integer entity, String query) {
        // TODO
        Vector<Double> features = new Vector<>();

        features.add(42.0);

        return features;
    }

    public HashSet<ScoredAnnotation> solveSa2W(String query) throws AnnotationException {

        CandidateEntities candidateEntities;
        BingResult bingResult;

        //region Get bing results and create candidate entities set (union of E1, E2 and E3)
        try {
            bingResult = bingApi.query(query);
            candidateEntities =
                    candidateEntitiesGenerator.generateCandidateEntities(bingResult, TOP_K_SNIPPETS, CandidateEntitiesGenerator.QueryMethod.ALL_OVERLAP);
        } catch (ConnectException e){
            logger.warn(e.getMessage());
            return new HashSet<ScoredAnnotation>();
        }
        catch (RuntimeException e){
            if (e.getCause().getCause() instanceof IOException){
                logger.warn(e.getMessage());
                return new HashSet<ScoredAnnotation>();
            }
            else
                throw new AnnotationException(e.getMessage());
        }
        catch (Exception e){
            throw new AnnotationException(e.getMessage());
        }

        HashSet<Tag> entities = new HashSet<Tag>();

        HashSet<Tag> entitiesQuery = (HashSet<Tag>) candidateEntities.getEntitiesQuery().stream().map(s -> new Tag(s)).collect(Collectors.toSet());
        HashSet<Tag> entitiesQueryExtended = (HashSet<Tag>) candidateEntities.getEntitiesQueryExtended().stream().map(s -> new Tag(s)).collect(Collectors.toSet());
        HashSet<Tag> entitiesQuerySnippetsTAGME = (HashSet<Tag>) candidateEntities.getEntitiesQuerySnippetsWAT().stream().map(s -> new Tag(s)).collect(Collectors.toSet());

        entities.addAll(entitiesQuery);
        entities.addAll(entitiesQueryExtended);
        entities.addAll(entitiesQuerySnippetsTAGME);

        //endregion

        // Find all potential mentions
        Set<MentionCandidate> mentions = new HashSet<>();                // Seg(q) in the paper
        GreedyMentionIterator it = new GreedyMentionIterator(query);
        while (it.hasNext()) {
            MentionCandidate mention = it.next();
            mentions.add(mention);
        }

        // Calculate features for all possible pairs of <anchor, candidate entity>
        for(MentionCandidate mention : mentions) {
            for(Tag entity : entities) {

                Integer entityID = entity.getConcept();
                if(entityID == -1) {
                    throw new AnnotationException("Entity ID missing in candidate (received dummy value '-1').");
                }

                // Get both per-entity features and per-pair features.
                Vector<Double> entityFeatures;
                Vector<Double> mentionEntityFeatures;
                try {
                     entityFeatures = getEntityFeatures(entityID, query, bingResult, candidateEntities);
                     mentionEntityFeatures = getMentionEntityFeatures(mention, entityID, query);
                } catch(IOException e) {
                    throw new AnnotationException(e.getMessage());
                }

                Vector<Double> features = new Vector<>();
                features.addAll(entityFeatures);
                features.addAll(mentionEntityFeatures);

                System.out.printf("(%s, %d) features: %s\n", mention.getMention(), entityID, features);
            }
        }


        return new HashSet<>();
    }

    public String getName() {
        return "SMAPH-S annotator";
    }

}

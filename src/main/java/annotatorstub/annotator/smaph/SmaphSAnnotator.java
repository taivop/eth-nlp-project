package annotatorstub.annotator.smaph;

import annotatorstub.annotator.FakeAnnotator;
import annotatorstub.annotator.smaph.CandidateEntitiesGenerator.QueryMethod;
import annotatorstub.utils.EntityToAnchors;
import annotatorstub.utils.StringUtils;
import annotatorstub.utils.WATRelatednessComputer;
import annotatorstub.utils.bing.BingResult;
import annotatorstub.utils.bing.BingSearchAPI;
import annotatorstub.utils.caching.WATRequestCache;
import annotatorstub.utils.mention.GreedyMentionIterator;
import annotatorstub.utils.mention.MentionCandidate;
import annotatorstub.utils.mention.SmaphCandidate;
import it.unipi.di.acube.batframework.data.Mention;
import it.unipi.di.acube.batframework.data.ScoredAnnotation;
import it.unipi.di.acube.batframework.data.Tag;
import it.unipi.di.acube.batframework.problems.Sa2WSystem;
import it.unipi.di.acube.batframework.utils.AnnotationException;
import it.unipi.di.acube.batframework.utils.Pair;
import it.unipi.di.acube.batframework.utils.WikipediaApiInterface;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.net.ConnectException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * SMAPH-S annotator from the paper "A Piggyback System for Joint Entity Mention Detection and Linking in Web Queries".
 * @see <a href="http://www2016.net/proceedings/proceedings/p567.pdf">the original publication</a>.
 */
public class SmaphSAnnotator extends FakeAnnotator {

    private static BingSearchAPI bingApi;
    private static final int DEFAULT_TOP_K_SNIPPETS = 25;

    private WikipediaApiInterface wikiApi;
    private CandidateEntitiesGenerator candidateEntitiesGenerator;
    private final int topKSnippets;
    private final static Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private SmaphSListPruner pruner;
    private CandidateEntitiesGenerator.QueryMethod generatorQueryMethod;
    private EntityToAnchors entityToAnchors;

    public SmaphSAnnotator(SmaphSListPruner pruner, WATRequestCache watRequestCache) {
        this(pruner, QueryMethod.ALL_OVERLAP, DEFAULT_TOP_K_SNIPPETS, watRequestCache);
    }

    /**
     * @param pruner An optional pruner. Passing 'Optional.empty()' disables pruning altogether,
     *               leading to an annotator with (hopefully) very high recall, but horrible
     *               precision.
     *
     * @param generatorQueryMethod How to generate candidates using the WAT annotator. {@see
     * {@link QueryMethod}}
     */
    public SmaphSAnnotator(
        SmaphSListPruner pruner,
        QueryMethod generatorQueryMethod,
        int topKSnippets,
        WATRequestCache watRequestCache
    ) {
        this.pruner = pruner;
        this.generatorQueryMethod = generatorQueryMethod;
        this.topKSnippets = topKSnippets;

        if (!new File(EntityToAnchors.DATASET_FILENAME).exists()) {
            logger.error("Could not find directory {}. You should download and unzip the file at from https://groviera1.di.unipi.it:5001/sharing/HpajtMYjn");
        }
        this.entityToAnchors = EntityToAnchors.e2a();

        BingSearchAPI.KEY = "crECheFN9wPg0oAJWRZM7nfuJ69ETJhMzxXXjchNMSM";
        // This employs Andrei's key.
        BingSearchAPI.KEY = "eQ7iWx2in91LwcKKFKnTaOv+ZKgecyu6FVuBwwi/N7g";
        bingApi = BingSearchAPI.getInstance();

        logger.info("Using top k snippets: {}", topKSnippets);

        this.wikiApi = WikipediaApiInterface.api();
        candidateEntitiesGenerator = new CandidateEntitiesGenerator(watRequestCache);
        try {
            WATRelatednessComputer.setCache("relatedness.cache");
        }
        catch(IOException | ClassNotFoundException ex) {
            throw new RuntimeException("Could not load/set up relatedness cache.", ex);
        }
    }

    public static Double averageIfNotEmpty(Collection<Double> collection) {
        if(collection.isEmpty()) {
            // TODO(andrei): Is this a sensible thing to do?
            return 0.0;
        }

        Double sum = 0.0;
        for(Double element : collection) {
            sum += element;
        }
        return sum / collection.size();
    }

    /**
     * Returns the SMAPH features for given entity. See table 4 in article.
     *
     * @param entity the Wikipedia ID of the entity
     * @return vector of per-entity features
     */
    private List<Double> getEntityFeatures(Integer entity, String query, BingResult bingResult, CandidateEntities candidateEntities) throws IOException {
        // TODO have a cache for this so if we query the same entity again, we won't recalculate stuff.
        ArrayList<Double> features = new ArrayList<>();
        List<HashMap<Mention, HashMap<String, Double>>> watAdditionalAnnotationInfoList =
                candidateEntities.getAdditionalInfoList();
        // TODO use this list to calculate our features

        // ====================================================================================
        // region Features drawn from all sources

        Double f1_webTotal = bingResult.getWebTotal().doubleValue();

        //endregion
        // ------------------------------------------------------------------------------------



        // ====================================================================================
        //region Features drawn from sources Epsilon1 and Epsilon2
        // TODO bingResultTitles doesn't need to be calculated again for every entity
        List<String> bingResultTitles = bingResult.getWebResults().stream().
                map(snippet -> snippet.getTitle()).
                collect(Collectors.toList());

        Double f3_rank;
        Integer firstMatchPosition = 0;
        Integer currentPosition = 0;
        for(String resultTitle : bingResultTitles) {
            currentPosition++;
            String bingPageTitle = StringUtils.extractPageTitleFromBingSnippetTitle(resultTitle);
            boolean urlFoundInResult = false;
            try {
                urlFoundInResult = bingPageTitle.equals(wikiApi.getTitlebyId(entity));
            }
            catch(IOException ex) {
                // TODO(andrei): Keep track and see if this happens often.
                System.err.println("IOException while getting a title ID from Wikipedia API. Entity: " + entity);
                System.err.println("Please check that you're not making bad requests to Wikipedia.");
            }

            if(urlFoundInResult) {
                firstMatchPosition = currentPosition;
                break;
            }
        }
        if(firstMatchPosition == 0) {   // If we didn't find our entity in any result URL
            // TODO this is an arbitrary choice (paper doesn't specify what happens when entity URL isn't in results)
            f3_rank = ((Integer) (topKSnippets * 4)).doubleValue();
        } else {
            f3_rank = firstMatchPosition.doubleValue();
        }

        Double f4_EDTitle = StringUtils.minED(wikiApi.getTitlebyId(entity), query);

        Double f5_EDTitNP = StringUtils.minED(StringUtils.removeFinalParentheticalString(wikiApi.getTitlebyId(entity)), query);

        Double f6_minEDBolds = Double.POSITIVE_INFINITY;
        Double f7_captBolds = 0.0;
        Double f8_boldTerms = 0.0;

        // TODO boldPortions doesn't need to be calculated again for every entity
        List<String> boldPortions = bingResult.getWebResults().stream().
                map(snippet -> snippet.getHighlightedWords()).
                flatMap(l -> l.stream()).
                collect(Collectors.toList());
        for(String boldPortion : boldPortions) {
            Double currentMinED = StringUtils.minED(boldPortion, query);
            if(currentMinED < f6_minEDBolds) {
                f6_minEDBolds = currentMinED;
            }

            // Check if word is capitalised (first letter uppercase and the rest lowercase)
            //System.out.printf("%s is capitalised? %b\n", boldPortion, StringUtils.isCapitalised(boldPortion));
            //System.out.printf("%s capitalisedness: %.2f\n", boldPortion, StringUtils.capitalisedness(boldPortion));
            if(StringUtils.isCapitalised(boldPortion)) {
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

        // mentionSnippetPairs: the set X(q) of pairs (mention, snippetID) as explained in the paper (snippet ID is its rank in the Bing results)
        ArrayList<MentionEntitySnippetTriple> mentionEntitySnippetTriples = new ArrayList<>();

        // TODO bingSnippets doesn't need to be calculated again for every entity
        // bingSnippets: snippets returned by querying bing with the original query
        List<String> bingSnippetsFull = bingResult.getWebResults().stream().
                map(snippet -> snippet.getDescription()).
                collect(Collectors.toList());
        // This ensures that we don't crash when the total number of returned results is smaller
        // than 'topKSnippets'.
        int listEnd = Math.min(topKSnippets, bingSnippetsFull.size());
        List<String> bingSnippets = bingSnippetsFull.subList(0, listEnd);

        // snippetEntities: for each snippet, the set of entitities that were found by annotating the snippet with WAT
        List<Set<Integer>> snippetEntities = candidateEntities.getEntitiesQuerySnippetsWATBySnippet();

        // WATSnippetAnnotations: for each snippet, the set of annotations found by annotating the snippet with WAT
        List<Set<ScoredAnnotation>> WATSnippetAnnotations = candidateEntities.getWATSnippetAnnotations();

        for(int rankCounter=0; rankCounter < bingSnippets.size(); rankCounter++) {

            boolean snippetHasEntity = snippetEntities.get(rankCounter).contains(entity); // Do we find our desired entity in the annotations of this snippet?
            Set<ScoredAnnotation> WATannotations = WATSnippetAnnotations.get(rankCounter);
            for(ScoredAnnotation scoredAnnotation : WATannotations) {

                Mention mention = new Mention(scoredAnnotation.getPosition(), scoredAnnotation.getLength());
                mentionEntitySnippetTriples.add(new MentionEntitySnippetTriple(mention, scoredAnnotation.getConcept(), rankCounter));
            }

            if(snippetHasEntity) {
                f9_freq += 1;
                f10_avgRank += rankCounter;
            } else {
                f10_avgRank += bingSnippets.size();
            }

        }
        f9_freq /= bingSnippets.size();
        f10_avgRank /= bingSnippets.size();

        Double f11_pageRank = SmaphSMockDataSources.getWikiPageRankScore(entity);

        ArrayList<Double> linkProbabilities = new ArrayList<>();
        ArrayList<Double> commonnesses = new ArrayList<>();
        ArrayList<Double> ambiguities = new ArrayList<>();
        ArrayList<Double> rhoScores = new ArrayList<>();
        ArrayList<Double> minEDs = new ArrayList<>();
        for(MentionEntitySnippetTriple mentionEntitySnippetTriple : mentionEntitySnippetTriples) {
            Integer mentionedEntity = mentionEntitySnippetTriple.getEntity();
            //System.out.printf("Scoring entity %d, current entity %d\n", entity, mentionedEntity);

            if(mentionedEntity.equals(entity)) { // Only consider mentions if this is the entity we're calculating features for
                Mention mentionInSnippet = mentionEntitySnippetTriple.getMention();
                Integer snippetRank = mentionEntitySnippetTriple.getSnippetRank();
                String mentionStringInSnippet = bingSnippets.get(snippetRank).substring(
                        mentionInSnippet.getPosition(), mentionInSnippet.getPosition() + mentionInSnippet.getLength()
                );

                HashMap<String, Double> snippetAdditionalInfo =
                        watAdditionalAnnotationInfoList.get(snippetRank).get(mentionInSnippet);

                if(snippetAdditionalInfo == null) {
                    // TODO(andrei): Find out why this happens.
                    logger.error("null snippet info for snippet rank {}, mention in snippet {}",
                        snippetRank, mentionInSnippet);
                    throw new RuntimeException("null snippet info should not occur when the " +
                        "caching is working correctly.");
                }
                else {
                    linkProbabilities.add(snippetAdditionalInfo.get("lp"));
                    commonnesses.add(snippetAdditionalInfo.get("commonness"));
                    ambiguities.add(snippetAdditionalInfo.get("ambiguity"));
                    rhoScores.add(snippetAdditionalInfo.get("rhoScore"));
                }

                minEDs.add(StringUtils.minED(mentionStringInSnippet, query));
            }
        }

        //System.out.printf("|X| = %d, |total triples| = %d\n", count, mentionEntitySnippetTriples.size());

        if(linkProbabilities.isEmpty()) {   // Add dummy elements so that min, max and average methods don't crash
            linkProbabilities.add(0.0);
            commonnesses.add(0.0);
            ambiguities.add(0.0);
            rhoScores.add(0.0);
            minEDs.add(0.0);
        }

        Double f15_lp_min = Collections.min(linkProbabilities);
        Double f16_lp_max = Collections.max(linkProbabilities);
        Double f17_comm_min = Collections.min(commonnesses);
        Double f18_comm_max = Collections.max(commonnesses);
        Double f19_comm_avg = averageIfNotEmpty(commonnesses);
        Double f20_ambig_min = Collections.min(ambiguities);
        Double f21_ambig_max = Collections.max(ambiguities);
        Double f22_ambig_avg = averageIfNotEmpty(ambiguities);
        Double f23_mentMED_min = Collections.min(minEDs);
        Double f24_mentMED_max = Collections.max(minEDs);

        //endregion
        // ------------------------------------------------------------------------------------

        // ====================================================================================
        //region Combine features into a list

        features.add(f1_webTotal);
        features.add(f3_rank);
        features.add(f4_EDTitle);
        features.add(f5_EDTitNP);
        features.add(f6_minEDBolds);
        features.add(f7_captBolds);
        features.add(f8_boldTerms);
        features.add(f9_freq);
        features.add(f10_avgRank);
        // features.add(f11_pageRank);
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
     * @return list of per-pair features
     */
    private List<Double> getMentionEntityFeatures(MentionCandidate mention, Integer entity, String query) throws IOException {
        String mentionString = mention.getMention();
        String entityTitle = wikiApi.getTitlebyId(entity);
        // TODO
        ArrayList<Double> features = new ArrayList<>();

        Double f25_anchorsAvgED;
        Double sum_enumerator = 0.0;
        Double sum_denominator = 0.0;
        if(entityToAnchors.containsId(entity)) {
            for (Pair<String, Integer> anchorAndFreq : entityToAnchors.getAnchors(entity)) {
                String anchor = anchorAndFreq.first;
                Integer freq = anchorAndFreq.second;
                Double sqrt_F = Math.sqrt(freq);
                sum_enumerator += sqrt_F * StringUtils.ED(anchor, mentionString);
                sum_denominator += sqrt_F;
            }
        }

        if(sum_denominator == 0.0) {
            f25_anchorsAvgED = 0.0;
        } else {
            f25_anchorsAvgED = sum_enumerator / sum_denominator;
        }

        Double f26_minEDTitle = StringUtils.minED(mentionString, entityTitle);
        Double f27_EdTitle = (double) StringUtils.ED(mentionString, entityTitle);
        Double f28_commonness;
        Double f29_lp;
        // This is a hack to work around the fact that querying
        // 'http://wikisense.mkapp.it/tag/spot?text=%2F' or something similar leads to a 500 error.
//        if(mentionString.equals("/") || mentionString.equals("&") || mentionString.equals("?")) {
        if(mentionString.length() <= 1) {
            f28_commonness = 0.0;
            f29_lp = 0.0;
        }
        else {
            f28_commonness = WATRelatednessComputer.getCommonness(mentionString, entity);
            f29_lp = WATRelatednessComputer.getLp(mentionString);
        }

        features.add(f25_anchorsAvgED);
        features.add(f26_minEDTitle);
        features.add(f27_EdTitle);
        features.add(f28_commonness);
        features.add(f29_lp);

        return features;
    }

    public List<SmaphCandidate> getCandidatesWithFeatures(String query) {

        CandidateEntities candidateEntities;
        BingResult bingResult;

        //region Get bing results and create candidate entities set (union of E1, E2 and E3)
        try {
            bingResult = bingApi.query(query);
            candidateEntities =
                    candidateEntitiesGenerator.generateCandidateEntities(bingResult, topKSnippets,
                        generatorQueryMethod);
        } catch (ConnectException e){
            logger.warn(e.getMessage());
            return null;
        }
        catch (RuntimeException e){
            if (e.getCause().getCause() instanceof IOException){
                logger.warn(e.getMessage());
                return null;
            }
            else {
                throw new AnnotationException(e.getMessage());
            }
        }
        catch (Exception e){
            throw new AnnotationException(e.getMessage());
        }

        HashSet<Tag> entities = new HashSet<Tag>();

        HashSet<Tag> entitiesQuery = (HashSet<Tag>) candidateEntities.getEntitiesQuery().stream().map(s -> new Tag(s)).collect(Collectors.toSet());
        HashSet<Tag> entitiesQueryExtended = (HashSet<Tag>) candidateEntities.getEntitiesQueryExtended().stream().map(s -> new Tag(s)).collect(
            Collectors.toSet());
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

        List<SmaphCandidate> results = new ArrayList<>();

        // Calculate features for all possible pairs of <anchor, candidate entity>
        for(Tag entity : entities) {
            Integer entityID = entity.getConcept();

            // Get per-entity features
            List<Double> entityFeatures;
            if(entityID == -1) {
                throw new AnnotationException("Entity ID missing in candidate (received dummy value '-1').");
            }
            try {
                entityFeatures = getEntityFeatures(entityID, query, bingResult, candidateEntities);
            }
            catch(IOException e) {
                throw new AnnotationException(e.getMessage());
            }

            for(MentionCandidate mention : mentions) {
                // Get per-pair features.
                List<Double> mentionEntityFeatures;
                try {
                     mentionEntityFeatures = getMentionEntityFeatures(mention, entityID, query);
                } catch(IOException e) {
                    throw new AnnotationException(e.getMessage());
                }

                ArrayList<Double> features = new ArrayList<>();
                features.addAll(entityFeatures);
                features.addAll(mentionEntityFeatures);

//                System.out.printf("('%s', ID %d) features: %s\n", mention.getMention(), entityID, features);
                // Note: we need the inner loop because we also have specific mention-entity
                // features.
                results.add(new SmaphCandidate(entityID, mention, features));
            }
        }

        return results;
    }

    // TODO(andrei): Move this and 'overlapsSet' to their own utility.
    private static class MentionSizeComparator implements Comparator<SmaphCandidate> {
        @Override
        public int compare(SmaphCandidate left, SmaphCandidate right) {
            int leftSize = left.getMentionCandidate().getLength();
            int rightSize = right.getMentionCandidate().getLength();

            //noinspection SuspiciousNameCombination
            return Integer.compare(leftSize, rightSize);
        }
    }

    private static boolean overlapsSet(SmaphCandidate candidate, HashSet<ScoredAnnotation> set) {
        // TODO(andrei): Unify 'MentionCandidate' and Mention, if possible.
        Mention candidateMention = new Mention(
            candidate.getMentionCandidate().getQueryStartPosition(),
            candidate.getMentionCandidate().getLength());

        for (ScoredAnnotation scoredAnnotation : set) {
            if(scoredAnnotation.overlaps(candidateMention)) {
                return true;
            }
        }

        int candidateMentionEnd = candidateMention.getPosition() + candidateMention.getLength();
        System.out.println("No overlap found. Candidate mention: " + candidateMention.getPosition
                () + "-" + candidateMentionEnd + " " +
                candidateMention);
        System.out.println("Full mention: " + candidate.getMentionCandidate().getMention());
        return false;
    }

    /**
     * Takes a list of candidate annotations and greedily picks entities with non-overlapping
     * mentions starting with the entity with the longest mention.
     */
    public static HashSet<ScoredAnnotation> greedyPick(List<SmaphCandidate> candidates) {
        HashSet<ScoredAnnotation> result = new HashSet<>();

        List<SmaphCandidate> sortedCandidates = new ArrayList<>(candidates);
        sortedCandidates.sort(new MentionSizeComparator().reversed());

        for (SmaphCandidate candidate: sortedCandidates) {
            if(! overlapsSet(candidate, result)) {
                result.add(new ScoredAnnotation(
                    candidate.getMentionCandidate().getQueryStartPosition(),
                    candidate.getMentionCandidate().getLength(),
                    candidate.getEntityID(),
                    dummyScore));
            }
        }

        return result;
    }

    /**
     * This is the original mention selection approach, which also allows overlapping mentions
     * and simply picks the first mention for every entity ID it encounters.
     */
    private static HashSet<ScoredAnnotation> naivePick(List<SmaphCandidate> keptCandidates) {
        HashSet<ScoredAnnotation> annotations = new HashSet<>();
        // A simple hack to make SMAPH-1 output an A2W solution: for every entity, select just
        // its first mention.
        Set<Integer> seenEntityIds = new HashSet<>();
        for (SmaphCandidate keptCandidate : keptCandidates) {
            Integer id = keptCandidate.getEntityID();

            // We already saw this entity.
            if(seenEntityIds.contains(id)) {
                continue;
            }

            seenEntityIds.add(id);
            ScoredAnnotation scoredAnnotation = new ScoredAnnotation(
                keptCandidate.getMentionCandidate().getQueryStartPosition(),
                keptCandidate.getMentionCandidate().getLength(),
                keptCandidate.getEntityID(),
                dummyScore);
            annotations.add(scoredAnnotation);
        }

        return annotations;
    }

    // Our SMAPH-{1, S} implementation does no scoring.
    // TODO(andrei): Constantify.
    static float dummyScore = 1.0f;

    // We have to return a HashSet, not just a set, because that's how the interfaces higher up
    // are designed.
    @Override
    public HashSet<ScoredAnnotation> solveSa2W(String query) throws AnnotationException {
        List<SmaphCandidate> candidates = getCandidatesWithFeatures(query);
        List<SmaphCandidate> keptCandidates = pruner.shouldKeep(candidates);
        logger.info("Kept {}/{} candidates.", keptCandidates.size(), candidates.size());

        logger.info("Now processing: {}", query);
        // TODO(andrei): Use flag to switch between these techniques.
//        HashSet<ScoredAnnotation> annotations = greedyPick(keptCandidates);
        HashSet<ScoredAnnotation> annotations = naivePick(keptCandidates);
        logger.info("Found {} final annotations.", annotations.size());
        return annotations;
    }

    public Sa2WSystem getAuxiliaryAnnotator() {
        return candidateEntitiesGenerator.getWAT();
    }

    public String getName() {
        return "SMAPH-S annotator";
    }
}

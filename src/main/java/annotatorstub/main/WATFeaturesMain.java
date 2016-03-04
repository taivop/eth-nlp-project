package annotatorstub.main;

import java.lang.invoke.MethodHandles;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import annotatorstub.utils.WATRelatednessComputer;
import it.unipi.di.acube.batframework.utils.WikipediaApiInterface;

public class WATFeaturesMain {
	private final static Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	public static void main(String[] args) throws Exception {
		WikipediaApiInterface api = WikipediaApiInterface.api();
		WATRelatednessComputer.setCache("relatedness.cache");;

		int obamaId = api.getIdByTitle("Barack Obama");
		int merkelId = api.getIdByTitle("Angela Merkel");
		int germanyId = api.getIdByTitle("Germany");
		
		LOG.info("Wikipedia IDs: Obama:{} Merkel:{} Germany:{}", obamaId, merkelId, germanyId);
		LOG.info("Probability that `Obama' is a link in Wikipedia: {}", WATRelatednessComputer.getLp("Obama"));
		LOG.info("Probability that `Barack Obama' is a link in Wikipedia: {}", WATRelatednessComputer.getLp("Barack Obama"));
		LOG.info("Probability that `Barack' is a link in Wikipedia: {}", WATRelatednessComputer.getLp("Barack"));
		LOG.info("Jaccard relatedness between Germany and Angela Merkel: {}", WATRelatednessComputer.getJaccardRelatedness(germanyId, merkelId));
		LOG.info("Jaccard relatedness between Germany and Barack Obama: {}", WATRelatednessComputer.getJaccardRelatedness(germanyId, obamaId));
		LOG.info("Jaccard relatedness between Angela Merkel and Barack Obama: {}", WATRelatednessComputer.getJaccardRelatedness(merkelId, obamaId));
		LOG.info("MW relatedness between Germany and Angela Merkel: {}", WATRelatednessComputer.getMwRelatedness(germanyId, merkelId));
		LOG.info("MW relatedness between Germany and Barack Obama: {}", WATRelatednessComputer.getMwRelatedness(germanyId, obamaId));
		LOG.info("MW relatedness between Angela Merkel and Barack Obama: {}", WATRelatednessComputer.getMwRelatedness(merkelId, obamaId));

		api.flush();
		WATRelatednessComputer.flush();
	}
}

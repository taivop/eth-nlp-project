package annotatorstub.main;

import java.io.File;
import java.lang.invoke.MethodHandles;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import annotatorstub.utils.EntityToAnchors;
import it.unipi.di.acube.batframework.utils.Pair;

public class E2AMain {
	private final static Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	public static void main(String[] args) {
		
		if (!new File(EntityToAnchors.DATASET_FILENAME).exists())
			LOG.error("Could not find directory {}. You should download and unzip the file at from https://groviera1.di.unipi.it:5001/sharing/HpajtMYjn");

		EntityToAnchors e2a = EntityToAnchors.e2a();
		
		int obamaID = 534366;
		LOG.info("Printing all anchors that refer to entity Barack Obama");
		for (Pair<String, Integer> anchorAndFreq: e2a.getAnchors(obamaID))
			LOG.info("Anchor: {} Frequency: {}", anchorAndFreq.first, anchorAndFreq.second);
		LOG.info("Obama (id {}) has anchors in the database: {}", obamaID, e2a.containsId(obamaID));
		LOG.info("String 'obama' appears {} times as anchors in Wikipedia", e2a.getAnchorGlobalOccurrences("obama"));
		LOG.info("String 'barack obama' appears {} times as anchors in Wikipedia", e2a.getAnchorGlobalOccurrences("barack obama"));
		LOG.info("The commonness between string 'obama' and entity Barack Obama is {}.", e2a.getCommonness("obama", obamaID));
	}

}

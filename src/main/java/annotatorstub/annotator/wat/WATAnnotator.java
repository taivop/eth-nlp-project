package annotatorstub.annotator.wat;

import annotatorstub.annotator.FakeAnnotator;

public class WATAnnotator extends FakeAnnotator {

	private String method = "base";
	private String windowSize = "";
	private String minCommonness = "0.0";
	private String epsilon = "0.0";
	private String sortedBy = "PAGERANK";
	private String relatedness = "jaccard";
	private double pruningThreshold = 0.0;
	
	private WATapi watApi;
	
	public WATAnnotator(){
		watApi = new WATapi(this.method,this.windowSize,this.minCommonness,
				this.epsilon,this.sortedBy,this.relatedness);
	}
	
	public WATAnnotator(String method, String windowSize,
			      String minCommonness, String epsilon,
			      String sortedBy, String relatedness,
			      double pruningThreshold){
		this.method = method;
		this.windowSize = windowSize;
		this.minCommonness = minCommonness;
		this.epsilon = epsilon;
		this.sortedBy = sortedBy;
		this.relatedness = relatedness;
		this.pruningThreshold = pruningThreshold;
		
		watApi = new WATapi(this.method,this.windowSize,this.minCommonness,
							this.epsilon,this.sortedBy,this.relatedness);
	}
	
}

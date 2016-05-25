package annotatorstub.annotator;

import annotatorstub.utils.WATRelatednessComputer;

public class TestMain {
	
	public static void main(String[] args){
		
		int[] links = WATRelatednessComputer.getLinks("obama");
		
		System.out.println("Links found : " + links.length);
		
	}
	
}

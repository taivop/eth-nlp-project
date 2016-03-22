package annotatorstub.utils.mention;

/** a candidate for mention in a query **/
public class MentionCandidate {

	/* start position in original query */
	private int queryStartPosition = -1;
	/* end position in original query */
	private int queryEndPosition = -1;
	/* the mention */
	private String mention;

	// auto generated code:

	public MentionCandidate(int queryStartPosition, int queryEndPosition, String mention) {
		super();
		this.queryStartPosition = queryStartPosition;
		this.queryEndPosition = queryEndPosition;
		this.mention = mention;
	}

	public int getQueryStartPosition() {
		return queryStartPosition;
	}

	public int getQueryEndPosition() {
		return queryEndPosition;
	}

	public String getMention() {
		return mention;
	}
}

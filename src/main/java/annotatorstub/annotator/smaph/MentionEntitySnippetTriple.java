package annotatorstub.annotator.smaph;

import it.unipi.di.acube.batframework.data.Mention;

public class MentionEntitySnippetTriple {
    private Mention mention;
    private Integer entity;
    private Integer snippetRank;

    public MentionEntitySnippetTriple(Mention m, Integer e, Integer s) {
        this.mention = m;
        this.entity = e;
        this.snippetRank = s;
    }

    public Mention getMention() {
        return mention;
    }

    public void setMention(Mention mention) {
        this.mention = mention;
    }

    public Integer getEntity() {
        return entity;
    }

    public void setEntity(Integer entity) {
        this.entity = entity;
    }

    public Integer getSnippetRank() {
        return snippetRank;
    }

    public void setSnippetRank(Integer snippetRank) {
        this.snippetRank = snippetRank;
    }
}

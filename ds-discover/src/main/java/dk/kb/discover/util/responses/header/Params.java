package dk.kb.discover.util.responses.header;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class Params {
    @JsonIgnore
    @JsonProperty("fq")
    List<String> fq;

    @JsonProperty("q")
    String q;

    @JsonProperty("q.op")
    String qOp;

    @JsonProperty("rows")
    String rows;

    @JsonProperty("start")
    String start;

    @JsonProperty("wt")
    String wt;

    @JsonProperty("indent")
    String indent;

    @JsonProperty("facet")
    String facet;

    @JsonProperty("hl")
    String hl;

    @JsonProperty("spellcheck")
    String spellcheck;


    public List<String> getFq() {
        return fq;
    }

    public void setFq(List<String> fq) {
        this.fq = fq;
    }

    public String getQ() {
        return q;
    }

    public void setQ(String q) {
        this.q = q;
    }

    public String getRows() {
        return rows;
    }

    public void setRows(String rows) {
        this.rows = rows;
    }

    public String getStart() {
        return start;
    }

    public void setStart(String start) {
        this.start = start;
    }

    public String getWt() {
        return wt;
    }

    public void setWt(String wt) {
        this.wt = wt;
    }

    public String getIndent() {
        return indent;
    }

    public void setIndent(String indent) {
        this.indent = indent;
    }

    public String getFacet() {
        return facet;
    }

    public void setFacet(String facet) {
        this.facet = facet;
    }

    public String getqOp() {
        return qOp;
    }

    public void setqOp(String qOp) {
        this.qOp = qOp;
    }

    public String getHl() {
        return hl;
    }

    public void setHl(String hl) {
        this.hl = hl;
    }

    public String getSpellcheck() {
        return spellcheck;
    }

    @Override
    public String toString() {
        return "Params{" +
                "fq=" + fq +
                ", q='" + q + '\'' +
                ", qOp='" + qOp + '\'' +
                ", rows='" + rows + '\'' +
                ", start='" + start + '\'' +
                ", wt='" + wt + '\'' +
                ", indent='" + indent + '\'' +
                ", facet='" + facet + '\'' +
                ", hl='" + hl + '\'' +
                ", spellcheck='" + spellcheck + '\'' +
                '}';
    }

    public void setSpellcheck(String spellcheck) {
        this.spellcheck = spellcheck;
    }

}

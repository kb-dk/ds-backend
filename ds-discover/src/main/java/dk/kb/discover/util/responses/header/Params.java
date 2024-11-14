package dk.kb.discover.util.responses.header;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class Params {
    @JsonProperty("indent")
    String indent;

    @JsonProperty("fl")
    String fl;

    @JsonIgnore
    @JsonProperty("fq")
    List<String> fq;

    @JsonProperty("q")
    String q;

    @JsonProperty("q.op")
    String qOp;

    @JsonProperty("rows")
    String rows;

    @JsonProperty("sort")
    String sort;

    @JsonProperty("debug.explain.structure")
    String debugExplainStructure;

    @JsonProperty("queryUUID")
    String queryUUID;

    @JsonProperty("start")
    String start;

    @JsonProperty("wt")
    String wt;

    @JsonProperty("facet")
    String facet;

    @JsonProperty("hl")
    String hl;

    @JsonProperty("spellcheck")
    String spellcheck;

    @JsonProperty("spellcheck.maxCollations")
    String spellcheckMaxCollations;

    @JsonProperty("spellcheck.maxCollationTries")
    String spellcheckMaxCollationTries;

    @JsonProperty("spellcheck.build")
    String spellcheckBuild;

    @JsonProperty("spellcheck.extendedResults")
    String spellcheckExtendedResults;

    @JsonProperty("spellcheck.maxCollationRetries")
    String spellcheckMaxCollationRetries;

    @JsonProperty("spellcheck.accuracy")
    String spellcheckAccuracy;

    @JsonProperty("spellcheck.onlyMorePopular")
    String spellcheckOnlyMorePopular;

    @JsonProperty("spellcheck.count")
    String spellcheckCount;

    @JsonProperty("spellcheck.reload")
    String spellcheckReload;

    @JsonProperty("spellcheck.collate")
    String spellcheckCollate;

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

    public String getFl() {
        return fl;
    }

    public void setFl(String fl) {
        this.fl = fl;
    }

    public String getSort() {
        return sort;
    }

    public void setSort(String sort) {
        this.sort = sort;
    }

    public String getDebugExplainStructure() {
        return debugExplainStructure;
    }

    public void setDebugExplainStructure(String debugExplainStructure) {
        this.debugExplainStructure = debugExplainStructure;
    }

    public String getQueryUUID() {
        return queryUUID;
    }

    public void setQueryUUID(String queryUUID) {
        this.queryUUID = queryUUID;
    }

    public String getSpellcheckMaxCollations() {
        return spellcheckMaxCollations;
    }

    public void setSpellcheckMaxCollations(String spellcheckMaxCollations) {
        this.spellcheckMaxCollations = spellcheckMaxCollations;
    }

    public String getSpellcheckMaxCollationTries() {
        return spellcheckMaxCollationTries;
    }

    public void setSpellcheckMaxCollationTries(String spellcheckMaxCollationTries) {
        this.spellcheckMaxCollationTries = spellcheckMaxCollationTries;
    }

    public String getSpellcheckBuild() {
        return spellcheckBuild;
    }

    public void setSpellcheckBuild(String spellcheckBuild) {
        this.spellcheckBuild = spellcheckBuild;
    }

    public String getSpellcheckExtendedResults() {
        return spellcheckExtendedResults;
    }

    public void setSpellcheckExtendedResults(String spellcheckExtendedResults) {
        this.spellcheckExtendedResults = spellcheckExtendedResults;
    }

    public String getSpellcheckMaxCollationRetries() {
        return spellcheckMaxCollationRetries;
    }

    public void setSpellcheckMaxCollationRetries(String spellcheckMaxCollationRetries) {
        this.spellcheckMaxCollationRetries = spellcheckMaxCollationRetries;
    }

    public String getSpellcheckAccuracy() {
        return spellcheckAccuracy;
    }

    public void setSpellcheckAccuracy(String spellcheckAccuracy) {
        this.spellcheckAccuracy = spellcheckAccuracy;
    }

    public String getSpellcheckOnlyMorePopular() {
        return spellcheckOnlyMorePopular;
    }

    public void setSpellcheckOnlyMorePopular(String spellcheckOnlyMorePopular) {
        this.spellcheckOnlyMorePopular = spellcheckOnlyMorePopular;
    }

    public String getSpellcheckCount() {
        return spellcheckCount;
    }

    public void setSpellcheckCount(String spellcheckCount) {
        this.spellcheckCount = spellcheckCount;
    }

    public String getSpellcheckReload() {
        return spellcheckReload;
    }

    public void setSpellcheckReload(String spellcheckReload) {
        this.spellcheckReload = spellcheckReload;
    }

    public String getSpellcheckCollate() {
        return spellcheckCollate;
    }

    public void setSpellcheckCollate(String spellcheckCollate) {
        this.spellcheckCollate = spellcheckCollate;
    }

    @Override
    public String toString() {
        return "Params{" +
                "indent='" + indent + '\'' +
                ", fl='" + fl + '\'' +
                ", fq=" + fq +
                ", q='" + q + '\'' +
                ", qOp='" + qOp + '\'' +
                ", rows='" + rows + '\'' +
                ", sort='" + sort + '\'' +
                ", debugExplainStructure='" + debugExplainStructure + '\'' +
                ", queryUUID='" + queryUUID + '\'' +
                ", start='" + start + '\'' +
                ", wt='" + wt + '\'' +
                ", facet='" + facet + '\'' +
                ", hl='" + hl + '\'' +
                ", spellcheck='" + spellcheck + '\'' +
                ", spellcheckMaxCollations='" + spellcheckMaxCollations + '\'' +
                ", spellcheckMaxCollationTries='" + spellcheckMaxCollationTries + '\'' +
                ", spellcheckBuild='" + spellcheckBuild + '\'' +
                ", spellcheckExtendedResults='" + spellcheckExtendedResults + '\'' +
                ", spellcheckMaxCollationRetries='" + spellcheckMaxCollationRetries + '\'' +
                ", spellcheckAccuracy='" + spellcheckAccuracy + '\'' +
                ", spellcheckOnlyMorePopular='" + spellcheckOnlyMorePopular + '\'' +
                ", spellcheckCount='" + spellcheckCount + '\'' +
                ", spellcheckReload='" + spellcheckReload + '\'' +
                ", spellcheckCollate='" + spellcheckCollate + '\'' +
                '}';
    }

    public void setSpellcheck(String spellcheck) {
        this.spellcheck = spellcheck;
    }

}

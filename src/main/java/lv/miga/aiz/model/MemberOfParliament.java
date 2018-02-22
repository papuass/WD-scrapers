package lv.miga.aiz.model;

import java.util.List;

public class MemberOfParliament {

    private String qid;
    private String name;
    private String surname;
    private Integer parliament;
    private String replacesDeputy;
    private String fromNote;
    private String toNote;
    private String referenceURL;
    private List<ParliamentaryGroup> parliamentaryGroups;

    public String getQid() {
        return qid;
    }

    public void setQid(String qid) {
        this.qid = qid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSurname() {
        return surname;
    }

    public void setSurname(String surname) {
        this.surname = surname;
    }

    public Integer getParliament() {
        return parliament;
    }

    public void setParliament(Integer parliament) {
        this.parliament = parliament;
    }

    public String getReplacesDeputy() {
        return replacesDeputy;
    }

    public void setReplacesDeputy(String replacesDeputy) {
        this.replacesDeputy = replacesDeputy;
    }

    public String getFromNote() {
        return fromNote;
    }

    public void setFromNote(String fromNote) {
        this.fromNote = fromNote;
    }

    public String getToNote() {
        return toNote;
    }

    public void setToNote(String toNote) {
        this.toNote = toNote;
    }

    public String getReferenceURL() {
        return referenceURL;
    }

    public void setReferenceURL(String referenceURL) {
        this.referenceURL = referenceURL;
    }

    public List<ParliamentaryGroup> getParliamentaryGroups() {
        return parliamentaryGroups;
    }

    public void setParliamentaryGroups(List<ParliamentaryGroup> parliamentaryGroups) {
        this.parliamentaryGroups = parliamentaryGroups;
    }

    @Override
    public String toString() {
        return "MemberOfParliament{" +
                "qid='" + qid + '\'' +
                ", name='" + name + '\'' +
                ", surname='" + surname + '\'' +
                ", parliament=" + parliament +
                ", replacesDeputy='" + replacesDeputy + '\'' +
                ", fromNote='" + fromNote + '\'' +
                ", toNote='" + toNote + '\'' +
                ", referenceURL='" + referenceURL + '\'' +
                ", parliamentaryGroups=" + parliamentaryGroups +
                '}';
    }
}

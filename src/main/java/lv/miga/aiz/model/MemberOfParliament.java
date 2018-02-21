package lv.miga.aiz.model;

import java.util.List;

public class MemberOfParliament {

    private String qid;
    private String name;
    private String surname;
    private String fromNote;
    private String toNote;
    private String referenceURI;
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

    public String getReferenceURI() {
        return referenceURI;
    }

    public void setReferenceURI(String referenceURI) {
        this.referenceURI = referenceURI;
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
                ", fromNote='" + fromNote + '\'' +
                ", toNote='" + toNote + '\'' +
                ", referenceURI='" + referenceURI + '\'' +
                ", parliamentaryGroups=" + parliamentaryGroups +
                '}';
    }
}

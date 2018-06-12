package lv.miga.aiz.model;

import org.immutables.value.Value;

import javax.annotation.Nullable;
import java.util.List;

@Value.Immutable
public interface MemberOfParliament {

    @Nullable
    String getQid();

    String getName();

    String getSurname();

    Integer getParliament();

    @Nullable
    String getReplacesDeputy();

    String getFromNote();

    String getToNote();

    String getReferenceURL();

    List<ParliamentaryGroup> getParliamentaryGroups();

}

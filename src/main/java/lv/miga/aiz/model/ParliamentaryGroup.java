package lv.miga.aiz.model;

import org.immutables.value.Value;

import javax.annotation.Nullable;
import java.util.Date;

@Value.Immutable
public interface ParliamentaryGroup {

    String getGroupName();

    Date getDateFrom();

    @Nullable
    Date getDateTo();

}

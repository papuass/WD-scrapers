package lv.miga.aiz.model;

import org.immutables.value.Value;

import javax.annotation.Nullable;

@Value.Immutable
public interface MemberLink {

    @Nullable
    String getQid();

    String getName();

    String getSurname();

    String getUrl();
}

package lv.miga.aiz.guice;

import com.google.inject.AbstractModule;
import lv.miga.aiz.utils.*;

public class ScraperModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(DateUtils.class).to(DateUtilsImpl.class);
        bind(ExportUtil.class).to(QuickStatementsV1ExportUtilImpl.class);
        bind(TextUtils.class).to(TextUtilsImpl.class);
    }
}

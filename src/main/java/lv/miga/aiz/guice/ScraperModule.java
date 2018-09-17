package lv.miga.aiz.guice;

import com.google.inject.AbstractModule;
import lv.miga.aiz.export.ExportUtil;
import lv.miga.aiz.export.QuickStatementsV1ExportUtilImpl;
import lv.miga.aiz.utils.DateUtils;
import lv.miga.aiz.utils.DateUtilsImpl;
import lv.miga.aiz.utils.TextUtils;
import lv.miga.aiz.utils.TextUtilsImpl;

public class ScraperModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(DateUtils.class).to(DateUtilsImpl.class);
        bind(ExportUtil.class).to(QuickStatementsV1ExportUtilImpl.class);
        bind(TextUtils.class).to(TextUtilsImpl.class);
    }
}

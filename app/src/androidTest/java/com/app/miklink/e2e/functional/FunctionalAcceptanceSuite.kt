package com.app.miklink.e2e.functional

import org.junit.runner.RunWith
import org.junit.runners.Suite

@RunWith(Suite::class)
@Suite.SuiteClasses(
    LaunchNavigationUiTest::class,
    ProbeConfigurationUiTest::class,
    ClientCrudUiTest::class,
    ProfileCrudUiTest::class,
    SettingsUiTest::class,
    ReportSettingsUiTest::class,
    HistoryUiTest::class,
    PdfExportUiTest::class
)
class FunctionalAcceptanceSuite

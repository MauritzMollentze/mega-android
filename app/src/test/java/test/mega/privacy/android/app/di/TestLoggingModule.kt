package test.mega.privacy.android.app.di

import mega.privacy.android.domain.di.LoggingModule as DomainLoggingModule
import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import mega.privacy.android.app.di.LoggingModule
import mega.privacy.android.domain.qualifier.ChatLogger
import mega.privacy.android.domain.qualifier.SdkLogger
import mega.privacy.android.domain.usecase.CreateLogEntry
import mega.privacy.android.domain.usecase.CreateTraceString
import mega.privacy.android.domain.usecase.EnableLogAllToConsole
import mega.privacy.android.domain.usecase.GetCurrentTimeString
import mega.privacy.android.domain.usecase.InitialiseLoggingUseCase
import mega.privacy.android.domain.usecase.ResetSdkLoggerUseCase
import org.mockito.kotlin.mock

@Module
@TestInstallIn(
    replaces = [LoggingModule::class, DomainLoggingModule::class],
    components = [SingletonComponent::class]
)
object TestLoggingModule {

    @Provides
    @SdkLogger
    fun bindCreateSdkLogEntry(): CreateLogEntry = mock()

    @Provides
    @ChatLogger
    fun bindCreateChatLogEntry(): CreateLogEntry = mock()

    @Provides
    fun bindCreateTraceString(): CreateTraceString = mock()

    @Provides
    fun bindGetCurrentTimeString(): GetCurrentTimeString =
        mock()

    @Provides
    fun provideResetSdkLogger(): ResetSdkLoggerUseCase = mock()

    @Provides
    fun provideInitialiseLogging(): InitialiseLoggingUseCase = mock()

    @Provides
    fun provideEnableLogAllToConsole(): EnableLogAllToConsole = mock()
}
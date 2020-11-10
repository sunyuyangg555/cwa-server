package app.coronawarn.server.services.distribution.assembly.appconfig.structure.directory.v2;

import static app.coronawarn.server.services.distribution.common.Helpers.loadApplicationConfiguration;
import static java.io.File.separator;
import static java.lang.String.join;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.File;
import java.io.IOException;
import java.util.Set;
import org.junit.Rule;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.ConfigFileApplicationContextInitializer;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import app.coronawarn.server.common.protocols.internal.v2.ApplicationConfigurationAndroid;
import app.coronawarn.server.common.protocols.internal.v2.ApplicationConfigurationIOS;
import app.coronawarn.server.services.distribution.assembly.appconfig.ApplicationConfigurationV2PublicationConfig;
import app.coronawarn.server.services.distribution.assembly.appconfig.validation.ValidationError;
import app.coronawarn.server.services.distribution.assembly.appconfig.validation.ValidationError.ErrorType;
import app.coronawarn.server.services.distribution.assembly.appconfig.validation.ValidationResult;
import app.coronawarn.server.services.distribution.assembly.appconfig.validation.v2.ApplicationConfigurationAndroidValidator;
import app.coronawarn.server.services.distribution.assembly.appconfig.validation.v2.ApplicationConfigurationIosValidator;
import app.coronawarn.server.services.distribution.assembly.component.CryptoProvider;
import app.coronawarn.server.services.distribution.assembly.structure.Writable;
import app.coronawarn.server.services.distribution.assembly.structure.WritableOnDisk;
import app.coronawarn.server.services.distribution.assembly.structure.directory.Directory;
import app.coronawarn.server.services.distribution.assembly.structure.directory.DirectoryOnDisk;
import app.coronawarn.server.services.distribution.assembly.structure.util.ImmutableStack;
import app.coronawarn.server.services.distribution.common.Helpers;
import app.coronawarn.server.services.distribution.config.DistributionServiceConfig;

@EnableConfigurationProperties(value = DistributionServiceConfig.class)
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {CryptoProvider.class, ApplicationConfigurationV2PublicationConfig.class},
    initializers = ConfigFileApplicationContextInitializer.class)
class AppConfigurationV2StructureProviderTest {

  @Rule
  private TemporaryFolder outputFolder = new TemporaryFolder();

  @Autowired
  private CryptoProvider cryptoProvider;

  @Autowired
  private DistributionServiceConfig distributionServiceConfig;

  @Autowired
  private ApplicationConfigurationAndroid applicationConfigurationAndroid;

  @Autowired
  private ApplicationConfigurationIOS applicationConfigurationIos;

  @Test
  void createsCorrectIosFiles() throws IOException {
    Set<String> expFiles =
        Set.of(join(separator, "app_config_ios"), join(separator, "app_config_ios.checksum"));
    Writable<WritableOnDisk> appConfigs =
        new AppConfigurationV2StructureProvider<ApplicationConfigurationIOS>(
            applicationConfigurationIos, cryptoProvider, distributionServiceConfig,
            distributionServiceConfig.getApi().getAppConfigV2IosFileName(),
            new ApplicationConfigurationIosValidator(applicationConfigurationIos))
                .getConfigurationArchive();

    assertThat(writeDirectoryAndGetFiles(appConfigs)).isEqualTo(expFiles);
  }

  @Test
  void createsCorrectAndroidFiles() throws IOException {
    Set<String> expFiles = Set.of(join(separator, "app_config_android"),
        join(separator, "app_config_android.checksum"));
    Writable<WritableOnDisk> appConfigs =
        new AppConfigurationV2StructureProvider<ApplicationConfigurationAndroid>(
            applicationConfigurationAndroid, cryptoProvider, distributionServiceConfig,
            distributionServiceConfig.getApi().getAppConfigV2AndroidFileName(),
            new ApplicationConfigurationAndroidValidator(applicationConfigurationAndroid))
                .getConfigurationArchive();

    assertThat(writeDirectoryAndGetFiles(appConfigs)).isEqualTo(expFiles);
  }

  @Test
  void shouldReturnNoFilesIfAndroidConfigValidationFails() throws IOException {
    ApplicationConfigurationAndroidValidator validatorMock = mock(ApplicationConfigurationAndroidValidator.class);
    ValidationResult result = new ValidationResult();
    result.add(new ValidationError("test", null, ErrorType.BLANK_LABEL));
    when(validatorMock.validate()).thenReturn(result);

    Writable<WritableOnDisk> appConfigs =
        new AppConfigurationV2StructureProvider<ApplicationConfigurationAndroid>(
            applicationConfigurationAndroid, cryptoProvider, distributionServiceConfig,
            distributionServiceConfig.getApi().getAppConfigV2AndroidFileName(),
            validatorMock)
                .getConfigurationArchive();

    assertThat(appConfigs).isNull();
  }

  @Test
  void shouldReturnNoFilesIfIosConfigValidationFails() throws IOException {
    ApplicationConfigurationIosValidator validatorMock = mock(ApplicationConfigurationIosValidator.class);
    ValidationResult result = new ValidationResult();
    result.add(new ValidationError("test", null, ErrorType.BLANK_LABEL));
    when(validatorMock.validate()).thenReturn(result);

    Writable<WritableOnDisk> appConfigs =
        new AppConfigurationV2StructureProvider<ApplicationConfigurationIOS>(
            applicationConfigurationIos, cryptoProvider, distributionServiceConfig,
            distributionServiceConfig.getApi().getAppConfigV2IosFileName(),
            validatorMock)
                .getConfigurationArchive();

    assertThat(appConfigs).isNull();
  }

  private Set<String> writeDirectoryAndGetFiles(Writable<WritableOnDisk> configFile) throws IOException {
    outputFolder.create();
    File outputFile = outputFolder.newFolder();
    Directory<WritableOnDisk> parentDirectory = new DirectoryOnDisk(outputFile);
    parentDirectory.addWritable(configFile);
    parentDirectory.prepare(new ImmutableStack<>());
    parentDirectory.write();
    return Helpers.getFilePaths(outputFile, outputFile.getAbsolutePath());
  }
}
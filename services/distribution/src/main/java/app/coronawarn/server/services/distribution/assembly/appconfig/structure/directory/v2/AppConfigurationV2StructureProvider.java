package app.coronawarn.server.services.distribution.assembly.appconfig.structure.directory.v2;

import app.coronawarn.server.services.distribution.assembly.appconfig.structure.archive.decorator.signing.AppConfigurationSigningDecorator;
import app.coronawarn.server.services.distribution.assembly.appconfig.validation.ConfigurationValidator;
import app.coronawarn.server.services.distribution.assembly.appconfig.validation.ValidationResult;
import app.coronawarn.server.services.distribution.assembly.component.CryptoProvider;
import app.coronawarn.server.services.distribution.assembly.structure.Writable;
import app.coronawarn.server.services.distribution.assembly.structure.WritableOnDisk;
import app.coronawarn.server.services.distribution.assembly.structure.archive.ArchiveOnDisk;
import app.coronawarn.server.services.distribution.assembly.structure.file.FileOnDisk;
import app.coronawarn.server.services.distribution.config.DistributionServiceConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Validates and provides all configuration files for a specific device which is running the ENF V2.
 */
public class AppConfigurationV2StructureProvider<T extends com.google.protobuf.GeneratedMessageV3> {

  private static final Logger logger = LoggerFactory.getLogger(AppConfigurationV2StructureProvider.class);

  private final T applicationConfiguration;
  private final CryptoProvider cryptoProvider;
  private final DistributionServiceConfig distributionServiceConfig;
  private final ConfigurationValidator appConfigV2Validator;
  private final String appConfigFileName;

  /**
   * Creates an {@link AppConfigurationV2StructureProvider} for the exposure configuration and risk score
   * classification.
   *
   * @param cryptoProvider The {@link CryptoProvider} whose artifacts to use for creating the
   *        signature.
   */
  public AppConfigurationV2StructureProvider(T applicationConfiguration,
      CryptoProvider cryptoProvider,
      DistributionServiceConfig distributionServiceConfig,
      String appConfigFileName,
      ConfigurationValidator appConfigV2Validator) {
    this.applicationConfiguration = applicationConfiguration;
    this.cryptoProvider = cryptoProvider;
    this.distributionServiceConfig = distributionServiceConfig;
    this.appConfigV2Validator = appConfigV2Validator;
    this.appConfigFileName = appConfigFileName;
  }

  /**
   * If validation of the given V2 app config (IOS or Android) succeeds, it is written into a file,
   * put into an archive with the specified name and returned to be included in the CWA file
   * structure.
   */
  public Writable<WritableOnDisk> getConfigurationArchive() {
    ValidationResult validationResult = appConfigV2Validator.validate();

    if (validationResult.hasErrors()) {
      logger.error("App configuration file creation failed. Validation failed for {}, {}",
          appConfigFileName, validationResult);
      return null;
    }

    ArchiveOnDisk appConfigurationFile = new ArchiveOnDisk(appConfigFileName);
    appConfigurationFile
        .addWritable(new FileOnDisk("export.bin", applicationConfiguration.toByteArray()));
    return new AppConfigurationSigningDecorator(appConfigurationFile, cryptoProvider,
        distributionServiceConfig);
  }
}

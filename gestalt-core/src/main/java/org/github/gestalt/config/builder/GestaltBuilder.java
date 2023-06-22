package org.github.gestalt.config.builder;

import org.github.gestalt.config.Gestalt;
import org.github.gestalt.config.GestaltCache;
import org.github.gestalt.config.GestaltCore;
import org.github.gestalt.config.decoder.Decoder;
import org.github.gestalt.config.decoder.DecoderRegistry;
import org.github.gestalt.config.decoder.DecoderService;
import org.github.gestalt.config.entity.GestaltConfig;
import org.github.gestalt.config.entity.GestaltModuleConfig;
import org.github.gestalt.config.exceptions.GestaltConfigurationException;
import org.github.gestalt.config.lexer.PathLexer;
import org.github.gestalt.config.lexer.SentenceLexer;
import org.github.gestalt.config.loader.ConfigLoader;
import org.github.gestalt.config.loader.ConfigLoaderRegistry;
import org.github.gestalt.config.loader.ConfigLoaderService;
import org.github.gestalt.config.node.ConfigNodeManager;
import org.github.gestalt.config.node.ConfigNodeService;
import org.github.gestalt.config.path.mapper.PathMapper;
import org.github.gestalt.config.post.process.PostProcessor;
import org.github.gestalt.config.post.process.PostProcessorConfig;
import org.github.gestalt.config.reload.ConfigReloadStrategy;
import org.github.gestalt.config.reload.CoreReloadListener;
import org.github.gestalt.config.reload.CoreReloadStrategy;
import org.github.gestalt.config.source.ConfigSource;
import org.github.gestalt.config.utils.CollectionUtils;

import java.lang.System.Logger.Level;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import static java.lang.System.Logger.Level.DEBUG;
import static java.lang.System.Logger.Level.WARNING;

/**
 * Builder to setup and create the Gestalt config class.
 *
 * <p>The minimum requirements for building a config is to provide a source.
 * Gestalt gestalt = new GestaltBuilder()
 * .addSource(new FileConfigSource(defaultFile))
 * .build();
 *
 * <p>The builder will automatically add the default config loaders and decoders.
 * You can customise and replace functionality as needed using the appropriate builder methods.
 *
 * <p>If there are any decoders set, it will not add the default decoders. So you will need to add the defaults manually if needed.
 * If there are any config loaders set, it will not add the default config loaders. So you will need to add the defaults manually if needed.
 *
 * <p>The builder can be used to customize and replace any of the functionality of Gestalt.
 *
 * @author <a href="mailto:colin.redmond@outlook.com"> Colin Redmond </a> (c) 2023.
 */
public class GestaltBuilder {
    private static final System.Logger logger = System.getLogger(GestaltBuilder.class.getName());
    private final List<ConfigReloadStrategy> reloadStrategies = new ArrayList<>();
    private final List<CoreReloadListener> coreCoreReloadListeners = new ArrayList<>();
    private final Map<Class, GestaltModuleConfig> modules = new HashMap<>();
    private ConfigLoaderService configLoaderService = new ConfigLoaderRegistry();
    private DecoderService decoderService;
    private SentenceLexer sentenceLexer = new PathLexer();
    private GestaltConfig gestaltConfig = new GestaltConfig();
    private ConfigNodeService configNodeService = new ConfigNodeManager();
    private List<ConfigSource> sources = new ArrayList<>();
    private List<Decoder<?>> decoders = new ArrayList<>();
    private List<ConfigLoader> configLoaders = new ArrayList<>();
    private List<PostProcessor> postProcessors = new ArrayList<>();
    private List<PathMapper> pathMappers = new ArrayList<>();
    private boolean useCacheDecorator = true;

    private Boolean treatWarningsAsErrors = null;
    private Boolean treatMissingArrayIndexAsError = null;
    private Boolean treatMissingValuesAsErrors = null;
    private Boolean treatNullValuesInClassAsErrors = null;

    private Level logLevelForMissingValuesWhenDefaultOrOptional = null;

    private DateTimeFormatter dateDecoderFormat = null;
    private DateTimeFormatter localDateTimeFormat = null;
    private DateTimeFormatter localDateFormat = null;

    // Token that represents the opening of a string substitution.
    private String substitutionOpeningToken = null;

    // Token that represents the closing of a string substitution.
    private String substitutionClosingToken = null;

    // the maximum nested substitution depth.
    private Integer maxSubstitutionNestedDepth = null;

    // the regex used to parse string substitutions.
    // Must have a named capture group transform, key, and default, where the key is required and the transform and default are optional.
    private String substitutionRegex = null;

    /**
     * Adds all default decoders to the builder. Uses the ServiceLoader to find all registered Decoders and adds them
     *
     * @return GestaltBuilder builder
     */
    @SuppressWarnings({"rawtypes"})
    public GestaltBuilder addDefaultDecoders() {
        List<Decoder<?>> decodersSet = new ArrayList<>();
        ServiceLoader<Decoder> loader = ServiceLoader.load(Decoder.class);
        loader.forEach(it -> {
            it.applyConfig(gestaltConfig);
            decodersSet.add(it);
        });
        this.decoders.addAll(decodersSet);
        return this;
    }

    /**
     * Add default config loaders to the builder. Uses the ServiceLoader to find all registered Config Loaders and adds them
     *
     * @return GestaltBuilder builder
     */
    public GestaltBuilder addDefaultConfigLoaders() {
        List<ConfigLoader> configLoaderSet = new ArrayList<>();
        ServiceLoader<ConfigLoader> loader = ServiceLoader.load(ConfigLoader.class);
        loader.forEach(it -> {
            it.applyConfig(gestaltConfig);
            configLoaderSet.add(it);
        });
        configLoaders.addAll(configLoaderSet);
        return this;
    }

    /**
     * Add default post processors to the builder. Uses the ServiceLoader to find all registered post processors and adds them
     *
     * @return GestaltBuilder builder
     */
    public GestaltBuilder addDefaultPostProcessors() {
        List<PostProcessor> postProcessorsSet = new ArrayList<>();
        ServiceLoader<PostProcessor> loader = ServiceLoader.load(PostProcessor.class);
        loader.forEach(it -> {
            PostProcessorConfig config = new PostProcessorConfig(gestaltConfig, configNodeService, sentenceLexer);
            it.applyConfig(config);
            postProcessorsSet.add(it);
        });
        postProcessors.addAll(postProcessorsSet);
        return this;
    }

    /**
     * Add default post processors to the builder. Uses the ServiceLoader to find all registered post processors and adds them
     *
     * @return GestaltBuilder builder
     */
    public GestaltBuilder addDefaultPathMappers() {
        List<PathMapper> pathMappersSet = new ArrayList<>();
        ServiceLoader<PathMapper> loader = ServiceLoader.load(PathMapper.class);
        loader.forEach(it -> {
            it.applyConfig(gestaltConfig);
            pathMappersSet.add(it);
        });
        pathMappers.addAll(pathMappersSet);
        return this;
    }

    /**
     * Sets the list of sources to load. Replaces any sources already set.
     *
     * @param sources list of sources to load.
     * @return GestaltBuilder builder
     * @throws GestaltConfigurationException exception if there are no sources
     */
    public GestaltBuilder setSources(List<ConfigSource> sources) throws GestaltConfigurationException {
        if (sources == null || sources.isEmpty()) {
            throw new GestaltConfigurationException("No sources provided while setting sources");
        }
        this.sources = sources;

        return this;
    }

    /**
     * List of sources to add to the builder.
     *
     * @param sources list of sources to add.
     * @return GestaltBuilder builder
     * @throws GestaltConfigurationException no sources provided
     */
    public GestaltBuilder addSources(List<ConfigSource> sources) throws GestaltConfigurationException {
        if (sources == null || sources.isEmpty()) {
            throw new GestaltConfigurationException("No sources provided while adding sources");
        }
        this.sources.addAll(sources);

        return this;
    }

    /**
     * Add a single source to the builder.
     *
     * @param source add a single sources
     * @return GestaltBuilder builder
     */
    public GestaltBuilder addSource(ConfigSource source) {
        Objects.requireNonNull(source, "Source should not be null");
        this.sources.add(source);
        return this;
    }

    /**
     * Add a config reload strategy to the builder.
     *
     * @param configReloadStrategy add a config reload strategy.
     * @return GestaltBuilder builder
     */
    public GestaltBuilder addReloadStrategy(ConfigReloadStrategy configReloadStrategy) {
        Objects.requireNonNull(configReloadStrategy, "reloadStrategy should not be null");
        this.reloadStrategies.add(configReloadStrategy);
        return this;
    }

    /**
     * Add a list of config reload strategies to the builder.
     *
     * @param reloadStrategies list of config reload strategies.
     * @return GestaltBuilder builder
     */
    public GestaltBuilder addReloadStrategies(List<ConfigReloadStrategy> reloadStrategies) {
        Objects.requireNonNull(reloadStrategies, "reloadStrategies should not be null");
        this.reloadStrategies.addAll(reloadStrategies);
        return this;
    }

    /**
     * Add a Core reload listener to the builder.
     *
     * @param coreReloadListener a Core reload listener
     * @return GestaltBuilder builder
     */
    public GestaltBuilder addCoreReloadListener(CoreReloadListener coreReloadListener) {
        Objects.requireNonNull(coreReloadListener, "coreReloadListener should not be null");
        this.coreCoreReloadListeners.add(coreReloadListener);
        return this;
    }

    /**
     * Add a list of Core reload listener.
     *
     * @param coreCoreReloadListeners a list of Core reload listener
     * @return GestaltBuilder builder
     */
    public GestaltBuilder addCoreReloadListener(List<CoreReloadListener> coreCoreReloadListeners) {
        Objects.requireNonNull(reloadStrategies, "reloadStrategies should not be null");
        this.coreCoreReloadListeners.addAll(coreCoreReloadListeners);
        return this;
    }

    /**
     * Add a config loader service to the builder.
     *
     * @param configLoaderService a config loader service
     * @return GestaltBuilder builder
     */
    public GestaltBuilder setConfigLoaderService(ConfigLoaderService configLoaderService) {
        Objects.requireNonNull(configLoaderService, "ConfigLoaderRegistry should not be null");
        this.configLoaderService = configLoaderService;
        return this;
    }

    /**
     * Sets a list of config loader to the builder. Replaces any currently set.
     * If there are any config loaders set, it will not add the defaults. So you will need to add the defaults manually if needed.
     *
     * @param configLoaders a list of config loader
     * @return GestaltBuilder builder
     * @throws GestaltConfigurationException if there are no config loaders.
     */
    public GestaltBuilder setConfigLoaders(List<ConfigLoader> configLoaders) throws GestaltConfigurationException {
        if (configLoaders == null || configLoaders.isEmpty()) {
            throw new GestaltConfigurationException("No config loader provided while setting config loaders");
        }
        this.configLoaders = configLoaders;
        return this;
    }

    /**
     * Adds a list of config loader to the builder.
     * If there are any config loaders set, it will not add the defaults. So you will need to add the defaults manually if needed.
     *
     * @param configLoaders a list of config loader
     * @return GestaltBuilder builder
     * @throws GestaltConfigurationException if the config loaders are empty
     */
    public GestaltBuilder addConfigLoaders(List<ConfigLoader> configLoaders) throws GestaltConfigurationException {
        if (configLoaders == null || configLoaders.isEmpty()) {
            throw new GestaltConfigurationException("No config loader provided while adding config loaders");
        }
        this.configLoaders.addAll(configLoaders);
        return this;
    }

    /**
     * Add a config loader.
     * If there are any config loaders set, it will not add the defaults. So you will need to add the defaults manually if needed.
     *
     * @param configLoader a config loader
     * @return GestaltBuilder builder
     */
    public GestaltBuilder addConfigLoader(ConfigLoader configLoader) {
        Objects.requireNonNull(configLoader, "ConfigLoader should not be null");
        this.configLoaders.add(configLoader);
        return this;
    }

    /**
     * Sets the list of PostProcessors. Replaces any PostProcessors already set.
     *
     * @param postProcessors list of postProcessors to run.
     * @return GestaltBuilder builder
     * @throws GestaltConfigurationException exception if there are no postProcessors
     */
    public GestaltBuilder setPostProcessors(List<PostProcessor> postProcessors) throws GestaltConfigurationException {
        if (postProcessors == null || postProcessors.isEmpty()) {
            throw new GestaltConfigurationException("No PostProcessors provided while setting");
        }
        this.postProcessors = postProcessors;

        return this;
    }

    /**
     * List of PostProcessor to add to the builder.
     *
     * @param postProcessors list of PostProcessor to add.
     * @return GestaltBuilder builder
     * @throws GestaltConfigurationException no PostProcessor provided
     */
    public GestaltBuilder addPostProcessors(List<PostProcessor> postProcessors) throws GestaltConfigurationException {
        if (postProcessors == null || postProcessors.isEmpty()) {
            throw new GestaltConfigurationException("No PostProcessor provided while adding");
        }
        this.postProcessors.addAll(postProcessors);

        return this;
    }

    /**
     * Add a single PostProcessor to the builder.
     *
     * @param postProcessor add a single PostProcessor
     * @return GestaltBuilder builder
     */
    public GestaltBuilder addPostProcessor(PostProcessor postProcessor) {
        Objects.requireNonNull(postProcessor, "PostProcessor should not be null");
        this.postProcessors.add(postProcessor);
        return this;
    }

    /**
     * Sets the list of PathMappers. Replaces any PathMappers already set.
     *
     * @param pathMappers list of pathMappers to run.
     * @return GestaltBuilder builder
     * @throws GestaltConfigurationException exception if there are no pathMappers
     */
    public GestaltBuilder setPathMappers(List<PathMapper> pathMappers) throws GestaltConfigurationException {
        if (pathMappers == null || pathMappers.isEmpty()) {
            throw new GestaltConfigurationException("No PathMappers provided while setting");
        }
        this.pathMappers = pathMappers;

        return this;
    }

    /**
     * List of PostProcessor to add to the builder.
     *
     * @param pathMappers list of PathMapper to add.
     * @return GestaltBuilder builder
     * @throws GestaltConfigurationException no PathMapper provided
     */
    public GestaltBuilder addPathMapper(List<PathMapper> pathMappers) throws GestaltConfigurationException {
        if (pathMappers == null || pathMappers.isEmpty()) {
            throw new GestaltConfigurationException("No PathMapper provided while adding");
        }
        this.pathMappers.addAll(pathMappers);

        return this;
    }

    /**
     * Add a single PathMapper to the builder.
     *
     * @param pathMapper add a single PathMapper
     * @return GestaltBuilder builder
     */
    public GestaltBuilder addPathMapper(PathMapper pathMapper) {
        Objects.requireNonNull(pathMapper, "PathMapper should not be null");
        this.pathMappers.add(pathMapper);
        return this;
    }

    /**
     * Set the sentence lexer that will be passed through to the DecoderRegistry.
     *
     * @param sentenceLexer for the DecoderRegistry
     * @return GestaltBuilder builder
     */
    public GestaltBuilder setSentenceLexer(SentenceLexer sentenceLexer) {
        Objects.requireNonNull(sentenceLexer, "SentenceLexer should not be null");
        this.sentenceLexer = sentenceLexer;
        return this;
    }

    /**
     * Set the configuration for Gestalt. Will be overridden by any settings specified in the builder
     *
     * @param gestaltConfig configuration for the Gestalt
     * @return GestaltBuilder builder
     */
    public GestaltBuilder setGestaltConfig(GestaltConfig gestaltConfig) {
        Objects.requireNonNull(gestaltConfig, "GestaltConfig should not be null");
        this.gestaltConfig = gestaltConfig;
        return this;
    }

    /**
     * Set the config node service if you want to provide your own. Otherwise a default is provided.
     *
     * @param configNodeService a config node service
     * @return GestaltBuilder builder
     */
    public GestaltBuilder setConfigNodeService(ConfigNodeService configNodeService) {
        Objects.requireNonNull(configNodeService, "ConfigNodeManager should not be null");
        this.configNodeService = configNodeService;
        return this;
    }

    /**
     * Set the decoder service if you want to provide your own. Otherwise a default is provided.
     * If there are any decoders set, it will not add the default decoders. So you will need to add the defaults manually if needed.
     *
     * @param decoderService decoder service
     * @return GestaltBuilder builder
     */
    public GestaltBuilder setDecoderService(DecoderService decoderService) {
        Objects.requireNonNull(decoderService, "DecoderService should not be null");
        this.decoderService = decoderService;
        decoderService.addDecoders(decoders);
        return this;
    }

    /**
     * Set a list of decoders, replaces the existing decoders.
     * If there are any decoders set, it will not add the default decoders. So you will need to add the defaults manually if needed.
     *
     * @param decoders list of decoders
     * @return GestaltBuilder builder
     * @throws GestaltConfigurationException no decoders provided
     */
    public GestaltBuilder setDecoders(List<Decoder<?>> decoders) throws GestaltConfigurationException {
        if (decoders == null || decoders.isEmpty()) {
            throw new GestaltConfigurationException("No decoders provided while setting decoders");
        }
        this.decoders = decoders;
        return this;
    }

    /**
     * Add a list of decoders.
     * If there are any decoders set, it will not add the default decoders. So you will need to add the defaults manually if needed.
     *
     * @param decoders list of decoders
     * @return GestaltBuilder builder
     * @throws GestaltConfigurationException no decoders provided
     */
    public GestaltBuilder addDecoders(List<Decoder<?>> decoders) throws GestaltConfigurationException {
        if (decoders == null || decoders.isEmpty()) {
            throw new GestaltConfigurationException("No decoders provided while adding decoders");
        }
        this.decoders.addAll(decoders);
        return this;
    }

    /**
     * Add a decoder.
     * If there are any decoders set, it will not add the default decoders. So you will need to add the defaults manually if needed.
     *
     * @param decoder add a decoder
     * @return GestaltBuilder builder
     */
    @SuppressWarnings("rawtypes")
    public GestaltBuilder addDecoder(Decoder decoder) {
        Objects.requireNonNull(decoder, "Decoder should not be null");
        this.decoders.add(decoder);
        return this;
    }

    public GestaltBuilder addModuleConfig(GestaltModuleConfig extension) {
        modules.put(extension.getClass(), extension);
        return this;
    }

    /**
     * Treat warnings as errors.
     *
     * @param warningsAsErrors treat warnings as errors.
     * @return GestaltBuilder builder
     */
    public GestaltBuilder setTreatWarningsAsErrors(boolean warningsAsErrors) {
        treatWarningsAsErrors = warningsAsErrors;
        return this;
    }

    /**
     * Treat missing array indexes as errors.
     *
     * @param treatMissingArrayIndexAsError treat missing array indexes as errors.
     * @return GestaltBuilder builder
     */
    public GestaltBuilder setTreatMissingArrayIndexAsError(Boolean treatMissingArrayIndexAsError) {
        this.treatMissingArrayIndexAsError = treatMissingArrayIndexAsError;
        return this;
    }

    /**
     * treat missing object values as errors.
     *
     * @param treatMissingValuesAsErrors treat missing object values as errors
     * @return GestaltBuilder builder
     */
    public GestaltBuilder setTreatMissingValuesAsErrors(Boolean treatMissingValuesAsErrors) {
        this.treatMissingValuesAsErrors = treatMissingValuesAsErrors;
        return this;
    }

    /**
     * Treat null values in classes after decoding as errors.
     *
     * @param treatNullValuesInClassAsErrors treat null values in classes after decoding as errors
     * @return GestaltBuilder builder
     */
    public GestaltBuilder setTreatNullValuesInClassAsErrors(Boolean treatNullValuesInClassAsErrors) {
        this.treatNullValuesInClassAsErrors = treatNullValuesInClassAsErrors;
        return this;
    }

    /**
     * Add a cache layer to gestalt.
     *
     * @param useCacheDecorator use a cache decorator.
     * @return GestaltBuilder builder
     */
    public GestaltBuilder useCacheDecorator(boolean useCacheDecorator) {
        this.useCacheDecorator = useCacheDecorator;
        return this;
    }

    /**
     * Set a date decoder format. Used to decode date times.
     *
     * @param dateDecoderFormat a date decoder format
     * @return GestaltBuilder builder
     */
    public GestaltBuilder setDateDecoderFormat(DateTimeFormatter dateDecoderFormat) {
        this.dateDecoderFormat = dateDecoderFormat;
        return this;
    }

    /**
     * Provide the log level when we log a message when a config is missing, but we provided a default, or it is Optional.
     *
     * @return Log level
     */
    public Level getLogLevelForMissingValuesWhenDefaultOrOptional() {
        return logLevelForMissingValuesWhenDefaultOrOptional;
    }

    /**
     * Provide the log level when we log a message when a config is missing, but we provided a default, or it is Optional.
     *
     * @param logLevelForMissingValuesWhenDefaultOrOptional log level
     * @return GestaltBuilder builder
     */
    public GestaltBuilder setLogLevelForMissingValuesWhenDefaultOrOptional(Level logLevelForMissingValuesWhenDefaultOrOptional) {
        this.logLevelForMissingValuesWhenDefaultOrOptional = logLevelForMissingValuesWhenDefaultOrOptional;
        return this;
    }

    /**
     * Set a local date time format. Used to decode local date times.
     *
     * @param localDateTimeFormat a date decoder format
     * @return GestaltBuilder builder
     */
    public GestaltBuilder setLocalDateTimeFormat(DateTimeFormatter localDateTimeFormat) {
        this.localDateTimeFormat = localDateTimeFormat;
        return this;
    }

    /**
     * Set a local date format. Used to decode local date.
     *
     * @param localDateFormat a local date decoder format
     * @return GestaltBuilder builder
     */
    public GestaltBuilder setLocalDateFormat(DateTimeFormatter localDateFormat) {
        this.localDateFormat = localDateFormat;
        return this;
    }

    /**
     * Set a Token that represents the opening of a string substitution.
     *
     * @param substitutionOpeningToken Token that represents the opening of a string substitution.
     * @return GestaltBuilder builder
     */
    public GestaltBuilder setSubstitutionOpeningToken(String substitutionOpeningToken) {
        this.substitutionOpeningToken = substitutionOpeningToken;
        return this;
    }

    /**
     * Token that represents the closing of a string substitution.
     *
     * @param substitutionClosingToken a token that represents the closing of a string substitution.
     * @return GestaltBuilder builder
     */
    public GestaltBuilder setSubstitutionClosingToken(String substitutionClosingToken) {
        this.substitutionClosingToken = substitutionClosingToken;
        return this;
    }

    /**
     * Get the maximum string substitution nested depth.
     * If you have nested or recursive substitutions that go deeper than this it will fail.
     *
     * @return the maximum string substitution nested depth.
     */
    public Integer getMaxSubstitutionNestedDepth() {
        return maxSubstitutionNestedDepth;
    }

    /**
     * Set the maximum string substitution nested depth.
     * If you have nested or recursive substitutions that go deeper than this it will fail.
     *
     * @param maxSubstitutionNestedDepth the maximum string substitution nested depth.
     * @return GestaltBuilder builder
     */
    public GestaltBuilder setMaxSubstitutionNestedDepth(Integer maxSubstitutionNestedDepth) {
        this.maxSubstitutionNestedDepth = maxSubstitutionNestedDepth;
        return this;
    }

    /**
     * the regex used to parse string substitutions.
     * Must have a named capture group transform, key, and default, where the key is required and the transform and default are optional.
     *
     * @return the string substitution regex
     */
    public String getSubstitutionRegex() {
        return substitutionRegex;
    }

    /**
     * the regex used to parse string substitutions.
     * Must have a named capture group transform, key, and default, where the key is required and the transform and default are optional.
     *
     * @param substitutionRegex the string substitution regex
     * @return GestaltBuilder builder
     */
    public GestaltBuilder setSubstitutionRegex(String substitutionRegex) {
        this.substitutionRegex = substitutionRegex;
        return this;
    }


    /**
     * dedupe decoders and return the deduped list.
     *
     * @return deduped list of decoders.
     */
    protected List<Decoder<?>> dedupeDecoders() {
        Map<String, List<Decoder<?>>> decoderMap = decoders
            .stream()
            .collect(Collectors.groupingBy(Decoder::name))
            .entrySet()
            .stream()
            .filter(it -> it.getValue().size() > 1)
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        if (!decoderMap.isEmpty()) {
            String duplicates = String.join(", ", decoderMap.keySet());
            logger.log(WARNING, "Found duplicate decoders {0}", duplicates);
        }

        return decoders.stream().filter(CollectionUtils.distinctBy(Decoder::name)).collect(Collectors.toList());
    }

    /**
     * Dedupe the list of config loaders and return the deduped list.
     *
     * @return a list of deduped config loaders.
     */
    protected List<ConfigLoader> dedupeConfigLoaders() {
        Map<String, List<ConfigLoader>> configMap = configLoaders
            .stream()
            .collect(Collectors.groupingBy(ConfigLoader::name))
            .entrySet()
            .stream()
            .filter(it -> it.getValue().size() > 1)
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        if (!configMap.isEmpty()) {
            String duplicates = String.join(", ", configMap.keySet());
            logger.log(WARNING, "Found duplicate config loaders {0}", duplicates);
        }

        return configLoaders.stream().filter(CollectionUtils.distinctBy(ConfigLoader::name)).collect(Collectors.toList());
    }

    /**
     * Build Gestalt.
     *
     * @return Gestalt
     * @throws GestaltConfigurationException multiple validations can throw exceptions
     */
    public Gestalt build() throws GestaltConfigurationException {
        if (sources.isEmpty()) {
            throw new GestaltConfigurationException("No sources provided");
        }

        gestaltConfig = rebuildConfig();
        gestaltConfig.registerModuleConfig(modules);

        // setup the decoders, if there are none, add the default ones.
        if (decoders.isEmpty()) {
            logger.log(DEBUG, "No decoders provided, using defaults");
            addDefaultDecoders();
        }

        // setup the default path mappers, if there are none, add the default ones.
        if (pathMappers.isEmpty()) {
            logger.log(DEBUG, "No path mapper provided, using defaults");
            addDefaultPathMappers();
        }

        // if the decoderService does not exist, create it.
        // Otherwise get all the decoders from the decoderService, combine them with the ones in the builder,
        // and update the decoderService
        if (decoderService == null) {
            decoderService = new DecoderRegistry(decoders, configNodeService, sentenceLexer, pathMappers);
        } else {
            decoders.addAll(decoderService.getDecoders());
            List<Decoder<?>> dedupedDecoders = dedupeDecoders();
            decoderService.setDecoders(dedupedDecoders);
        }

        // Setup the config loaders.
        if (configLoaders.isEmpty()) {
            logger.log(DEBUG, "No decoders provided, using defaults");
            addDefaultConfigLoaders();
        }

        if (postProcessors.isEmpty()) {
            logger.log(DEBUG, "No post processors provided, using defaults");
            addDefaultPostProcessors();
        }

        // get all the config loaders from the configLoaderRegistry, combine them with the ones in the builder,
        // and update the configLoaderRegistry
        configLoaders.addAll(configLoaderService.getConfigLoaders());
        List<ConfigLoader> dedupedConfigs = dedupeConfigLoaders();
        configLoaderService.setLoaders(dedupedConfigs);

        // create a new GestaltCoreReloadStrategy to listen for Gestalt Core Reloads.
        CoreReloadStrategy coreReloadStrategy = new CoreReloadStrategy();
        final GestaltCore gestaltCore = new GestaltCore(configLoaderService, sources, decoderService, sentenceLexer, gestaltConfig,
            configNodeService, coreReloadStrategy, postProcessors);

        // register gestaltCore with all the source reload strategies.
        reloadStrategies.forEach(it -> it.registerListener(gestaltCore));
        // Add all listeners for the core update.
        coreCoreReloadListeners.forEach(coreReloadStrategy::registerListener);

        if (useCacheDecorator) {
            GestaltCache gestaltCache = new GestaltCache(gestaltCore);

            // Register the cache with the gestaltCoreReloadStrategy so when the core reloads
            // we can clear the cache.
            coreReloadStrategy.registerListener(gestaltCache);
            return gestaltCache;
        } else {
            return gestaltCore;
        }

    }

    private GestaltConfig rebuildConfig() {
        GestaltConfig newConfig = new GestaltConfig();

        newConfig.setTreatWarningsAsErrors(Objects.requireNonNullElseGet(treatWarningsAsErrors,
            () -> gestaltConfig.isTreatWarningsAsErrors()));

        newConfig.setTreatMissingArrayIndexAsError(Objects.requireNonNullElseGet(treatMissingArrayIndexAsError,
            () -> gestaltConfig.isTreatMissingArrayIndexAsError()));

        newConfig.setTreatMissingValuesAsErrors(Objects.requireNonNullElseGet(treatMissingValuesAsErrors,
            () -> gestaltConfig.isTreatMissingValuesAsErrors()));

        newConfig.setTreatNullValuesInClassAsErrors(Objects.requireNonNullElseGet(treatNullValuesInClassAsErrors,
            () -> gestaltConfig.isTreatNullValuesInClassAsErrors()));

        newConfig.setLogLevelForMissingValuesWhenDefaultOrOptional(
            Objects.requireNonNullElseGet(logLevelForMissingValuesWhenDefaultOrOptional,
                () -> gestaltConfig.getLogLevelForMissingValuesWhenDefaultOrOptional()));

        newConfig.setDateDecoderFormat(Objects.requireNonNullElseGet(dateDecoderFormat,
            () -> gestaltConfig.getDateDecoderFormat()));

        newConfig.setLocalDateTimeFormat(Objects.requireNonNullElseGet(localDateTimeFormat,
            () -> gestaltConfig.getLocalDateTimeFormat()));

        newConfig.setLocalDateFormat(Objects.requireNonNullElseGet(localDateFormat,
            () -> gestaltConfig.getLocalDateFormat()));

        newConfig.setSubstitutionOpeningToken(Objects.requireNonNullElseGet(substitutionOpeningToken,
            () -> gestaltConfig.getSubstitutionOpeningToken()));

        newConfig.setSubstitutionClosingToken(Objects.requireNonNullElseGet(substitutionClosingToken,
            () -> gestaltConfig.getSubstitutionClosingToken()));

        newConfig.setMaxSubstitutionNestedDepth(Objects.requireNonNullElseGet(maxSubstitutionNestedDepth,
            () -> gestaltConfig.getMaxSubstitutionNestedDepth()));

        newConfig.setSubstitutionRegex(Objects.requireNonNullElseGet(substitutionRegex,
            () -> gestaltConfig.getSubstitutionRegex()));

        return newConfig;
    }
}

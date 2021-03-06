package nu.nerd.beastmaster.mobs;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.TreeMap;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EntityType;

import nu.nerd.beastmaster.BeastMaster;

// ----------------------------------------------------------------------------
/**
 * Manages the collection of all known custom {@link MobType}s.
 */
public class MobTypeManager {
    // ------------------------------------------------------------------------
    /**
     * Return the mob type ID corresponding to the specified EntityType.
     * 
     * @param entityType the entity type.
     * @return the corresponding vanilla MobType's ID.
     */
    public static String getMobTypeId(EntityType entityType) {
        return entityType.name().toLowerCase().replace("_", "");
    }

    // ------------------------------------------------------------------------
    /**
     * Constructor.
     * 
     * Registers all of the vanilla mob types.
     */
    public MobTypeManager() {
        addPredefinedTypes();
    }

    // ------------------------------------------------------------------------
    /**
     * Return the {@link MobType} with the specified ID, or null if not found.
     * 
     * @param id the case-insensitive ID.
     * @return the {@link MobType} with the specified ID, or null if not found.
     */
    public MobType getMobType(String id) {
        return id != null ? _idToType.get(id.toLowerCase()) : null;
    }

    // ------------------------------------------------------------------------
    /**
     * Return the MobType corresponding to the specified EntityType.
     * 
     * @param entityType the entity type.
     * @return the corresponding vanilla MobType.
     */
    public MobType getMobType(EntityType entityType) {
        return getMobType(getMobTypeId(entityType));
    }

    // ------------------------------------------------------------------------
    /**
     * Return a collection of all {@link MobType}s.
     * 
     * @return a collection of all {@link MobType}s.
     */
    public Collection<MobType> getAllMobTypes() {
        return _idToType.values();
    }

    // ------------------------------------------------------------------------
    /**
     * Return the set of EntityTypes corresponding to allowed predefined vanilla
     * MobTypes.
     * 
     * @return the set of EntityTypes corresponding to allowed predefined
     *         vanilla MobTypes.
     */
    public Collection<EntityType> getPredefinedEntityTypes() {
        updateAllowedMobEntityTypes();
        return _allowedMobEntityTypes.values();
    }

    // ------------------------------------------------------------------------
    /**
     * Return a sorted ArrayList<> of the predefined {@link MobType}s.
     * 
     * Predefined MobTypes are pre-sorted by ID, so returning them sorted is a
     * zero-cost operation.
     * 
     * NOTE: the current implementation of Collectors.toList() returns an
     * ArrayList. I'm assuming that won't change.
     * 
     * @return a sorted ArrayList<> of the predefined {@link MobType}s.
     */
    public ArrayList<MobType> getPredefinedMobTypes() {
        return (ArrayList<MobType>) getAllMobTypes().stream().filter(MobType::isPredefined).collect(Collectors.toList());
    }

    // ------------------------------------------------------------------------
    /**
     * Return an unsorted ArrayList<> of the custom {@link MobType}s.
     * 
     * NOTE: the current implementation of Collectors.toList() returns an
     * ArrayList. I'm assuming that won't change.
     * 
     * @return an unsorted ArrayList<> of the custom {@link MobType}s.
     */
    public ArrayList<MobType> getCustomMobTypes() {
        return (ArrayList<MobType>) getAllMobTypes().stream().filter(t -> !t.isPredefined()).collect(Collectors.toList());
    }

    // ------------------------------------------------------------------------
    /**
     * Add a new {@link MobType}.
     * 
     * The type should not be previously registered.
     */
    public void addMobType(MobType type) {
        _idToType.put(type.getId().toLowerCase(), type);
    }

    // ------------------------------------------------------------------------
    /**
     * Remove a {@link MobType}.
     * 
     * @param id the ID of the type to remove.
     */
    public void removeMobType(String id) {
        _idToType.remove(id.toLowerCase());
    }

    // ------------------------------------------------------------------------
    /**
     * Remove a {@link MobType}.
     * 
     * @param type the type to remove.
     */
    public void removeMobType(MobType type) {
        removeMobType(type.getId());
    }

    // ------------------------------------------------------------------------
    /**
     * Load all the mob types from the plugin configuration.
     * 
     * @param config the plugin configuration file.
     * @param logger the logger.
     */
    public void load(FileConfiguration config, Logger logger) {
        _allowedMobEntityTypes = null;
        addPredefinedTypes();

        ConfigurationSection mobsSection = config.getConfigurationSection("mobs");
        if (mobsSection == null) {
            mobsSection = config.createSection("mobs");
        }

        for (String id : mobsSection.getKeys(false)) {
            ConfigurationSection section = mobsSection.getConfigurationSection(id);
            MobType mobType = new MobType();
            if (mobType.load(section, logger)) {
                if (_idToType.containsKey(mobType.getId())) {
                    // If mobType already defined, it is a predefined type
                    // added by addPredefinedTypes().
                    mobType._predefined = true;
                }
                addMobType(mobType);
            }
        }
    }

    // ------------------------------------------------------------------------
    /**
     * Save all the mob types in the plugin configuration, except for immutable,
     * predefined mob types.
     * 
     * @param config the plugin configuration file.
     * @param logger the logger.
     */
    public void save(FileConfiguration config, Logger logger) {
        // Create mobs section empty to remove deleted mob types.
        ConfigurationSection mobsSection = config.createSection("mobs");
        for (MobType mobType : _idToType.values()) {
            mobType.save(mobsSection, logger);
        }
    }

    // ------------------------------------------------------------------------
    /**
     * Register the predefined, vanilla mob types.
     * 
     * They are added in case-insensitive alphabetic order so that they can be
     * listed in the same order.
     */
    protected void addPredefinedTypes() {
        updateAllowedMobEntityTypes();

        _idToType.clear();
        for (EntityType entityType : _allowedMobEntityTypes.values()) {
            addMobType(new MobType(getMobTypeId(entityType), entityType, true));
        }
    }

    // ------------------------------------------------------------------------
    /**
     * Update the cached set of EntityTypes corresponding to vanilla MobTypes.
     */
    protected void updateAllowedMobEntityTypes() {
        if (_allowedMobEntityTypes == null) {
            _allowedMobEntityTypes = new TreeMap<>();
            for (EntityType entityType : EntityType.values()) {
                if (entityType.isAlive() &&
                    !BeastMaster.CONFIG.EXCLUDED_ENTITY_TYPES.contains(entityType)) {
                    _allowedMobEntityTypes.put(getMobTypeId(entityType), entityType);
                }
            }
        }
    }

    // ------------------------------------------------------------------------
    /**
     * Map from {@link MobType} lower case ID to instance.
     */
    protected LinkedHashMap<String, MobType> _idToType = new LinkedHashMap<>();

    /**
     * Map from MobType ID to EntityType of the predefined vanilla mob types.
     * 
     * We use a TreeMap<> indexed by mob type ID to ensure sorted order.
     */
    protected TreeMap<String, EntityType> _allowedMobEntityTypes;

} // class MobTypeManager

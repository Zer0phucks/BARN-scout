package com.vpt.scout;

import androidx.annotation.NonNull;
import androidx.room.DatabaseConfiguration;
import androidx.room.InvalidationTracker;
import androidx.room.RoomDatabase;
import androidx.room.RoomOpenHelper;
import androidx.room.migration.AutoMigrationSpec;
import androidx.room.migration.Migration;
import androidx.room.util.DBUtil;
import androidx.room.util.TableInfo;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteOpenHelper;
import com.vpt.scout.data.local.CollectionDao;
import com.vpt.scout.data.local.CollectionDao_Impl;
import com.vpt.scout.data.local.PropertyDao;
import com.vpt.scout.data.local.PropertyDao_Impl;
import com.vpt.scout.data.local.ScoutResultDao;
import com.vpt.scout.data.local.ScoutResultDao_Impl;
import java.lang.Class;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.Generated;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class ScoutDatabase_Impl extends ScoutDatabase {
  private volatile PropertyDao _propertyDao;

  private volatile CollectionDao _collectionDao;

  private volatile ScoutResultDao _scoutResultDao;

  @Override
  @NonNull
  protected SupportSQLiteOpenHelper createOpenHelper(@NonNull final DatabaseConfiguration config) {
    final SupportSQLiteOpenHelper.Callback _openCallback = new RoomOpenHelper(config, new RoomOpenHelper.Delegate(1) {
      @Override
      public void createAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `properties` (`apn` TEXT NOT NULL, `address` TEXT NOT NULL, `latitude` REAL NOT NULL, `longitude` REAL NOT NULL, `hasVpt` INTEGER NOT NULL, `conditionScore` REAL, `city` TEXT, `streetViewImagePath` TEXT, `updatedAt` INTEGER NOT NULL, PRIMARY KEY(`apn`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `collections` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `description` TEXT, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `collection_properties` (`collectionId` INTEGER NOT NULL, `apn` TEXT NOT NULL, `sortOrder` INTEGER NOT NULL, `addedAt` INTEGER NOT NULL, PRIMARY KEY(`collectionId`, `apn`), FOREIGN KEY(`collectionId`) REFERENCES `collections`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_collection_properties_collectionId` ON `collection_properties` (`collectionId`)");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_collection_properties_apn` ON `collection_properties` (`apn`)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `scout_results` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `apn` TEXT NOT NULL, `collectionId` INTEGER, `followUp` INTEGER NOT NULL, `flyered` INTEGER NOT NULL, `notes` TEXT, `scoutedAt` INTEGER NOT NULL, `latitude` REAL, `longitude` REAL, `synced` INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)");
        db.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, '9776eac45c064b1fc748bb93c8587ac4')");
      }

      @Override
      public void dropAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS `properties`");
        db.execSQL("DROP TABLE IF EXISTS `collections`");
        db.execSQL("DROP TABLE IF EXISTS `collection_properties`");
        db.execSQL("DROP TABLE IF EXISTS `scout_results`");
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onDestructiveMigration(db);
          }
        }
      }

      @Override
      public void onCreate(@NonNull final SupportSQLiteDatabase db) {
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onCreate(db);
          }
        }
      }

      @Override
      public void onOpen(@NonNull final SupportSQLiteDatabase db) {
        mDatabase = db;
        db.execSQL("PRAGMA foreign_keys = ON");
        internalInitInvalidationTracker(db);
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onOpen(db);
          }
        }
      }

      @Override
      public void onPreMigrate(@NonNull final SupportSQLiteDatabase db) {
        DBUtil.dropFtsSyncTriggers(db);
      }

      @Override
      public void onPostMigrate(@NonNull final SupportSQLiteDatabase db) {
      }

      @Override
      @NonNull
      public RoomOpenHelper.ValidationResult onValidateSchema(
          @NonNull final SupportSQLiteDatabase db) {
        final HashMap<String, TableInfo.Column> _columnsProperties = new HashMap<String, TableInfo.Column>(9);
        _columnsProperties.put("apn", new TableInfo.Column("apn", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsProperties.put("address", new TableInfo.Column("address", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsProperties.put("latitude", new TableInfo.Column("latitude", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsProperties.put("longitude", new TableInfo.Column("longitude", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsProperties.put("hasVpt", new TableInfo.Column("hasVpt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsProperties.put("conditionScore", new TableInfo.Column("conditionScore", "REAL", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsProperties.put("city", new TableInfo.Column("city", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsProperties.put("streetViewImagePath", new TableInfo.Column("streetViewImagePath", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsProperties.put("updatedAt", new TableInfo.Column("updatedAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysProperties = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesProperties = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoProperties = new TableInfo("properties", _columnsProperties, _foreignKeysProperties, _indicesProperties);
        final TableInfo _existingProperties = TableInfo.read(db, "properties");
        if (!_infoProperties.equals(_existingProperties)) {
          return new RoomOpenHelper.ValidationResult(false, "properties(com.vpt.scout.data.local.PropertyEntity).\n"
                  + " Expected:\n" + _infoProperties + "\n"
                  + " Found:\n" + _existingProperties);
        }
        final HashMap<String, TableInfo.Column> _columnsCollections = new HashMap<String, TableInfo.Column>(5);
        _columnsCollections.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCollections.put("name", new TableInfo.Column("name", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCollections.put("description", new TableInfo.Column("description", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCollections.put("createdAt", new TableInfo.Column("createdAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCollections.put("updatedAt", new TableInfo.Column("updatedAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysCollections = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesCollections = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoCollections = new TableInfo("collections", _columnsCollections, _foreignKeysCollections, _indicesCollections);
        final TableInfo _existingCollections = TableInfo.read(db, "collections");
        if (!_infoCollections.equals(_existingCollections)) {
          return new RoomOpenHelper.ValidationResult(false, "collections(com.vpt.scout.data.local.CollectionEntity).\n"
                  + " Expected:\n" + _infoCollections + "\n"
                  + " Found:\n" + _existingCollections);
        }
        final HashMap<String, TableInfo.Column> _columnsCollectionProperties = new HashMap<String, TableInfo.Column>(4);
        _columnsCollectionProperties.put("collectionId", new TableInfo.Column("collectionId", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCollectionProperties.put("apn", new TableInfo.Column("apn", "TEXT", true, 2, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCollectionProperties.put("sortOrder", new TableInfo.Column("sortOrder", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCollectionProperties.put("addedAt", new TableInfo.Column("addedAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysCollectionProperties = new HashSet<TableInfo.ForeignKey>(1);
        _foreignKeysCollectionProperties.add(new TableInfo.ForeignKey("collections", "CASCADE", "NO ACTION", Arrays.asList("collectionId"), Arrays.asList("id")));
        final HashSet<TableInfo.Index> _indicesCollectionProperties = new HashSet<TableInfo.Index>(2);
        _indicesCollectionProperties.add(new TableInfo.Index("index_collection_properties_collectionId", false, Arrays.asList("collectionId"), Arrays.asList("ASC")));
        _indicesCollectionProperties.add(new TableInfo.Index("index_collection_properties_apn", false, Arrays.asList("apn"), Arrays.asList("ASC")));
        final TableInfo _infoCollectionProperties = new TableInfo("collection_properties", _columnsCollectionProperties, _foreignKeysCollectionProperties, _indicesCollectionProperties);
        final TableInfo _existingCollectionProperties = TableInfo.read(db, "collection_properties");
        if (!_infoCollectionProperties.equals(_existingCollectionProperties)) {
          return new RoomOpenHelper.ValidationResult(false, "collection_properties(com.vpt.scout.data.local.CollectionPropertyEntity).\n"
                  + " Expected:\n" + _infoCollectionProperties + "\n"
                  + " Found:\n" + _existingCollectionProperties);
        }
        final HashMap<String, TableInfo.Column> _columnsScoutResults = new HashMap<String, TableInfo.Column>(10);
        _columnsScoutResults.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsScoutResults.put("apn", new TableInfo.Column("apn", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsScoutResults.put("collectionId", new TableInfo.Column("collectionId", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsScoutResults.put("followUp", new TableInfo.Column("followUp", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsScoutResults.put("flyered", new TableInfo.Column("flyered", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsScoutResults.put("notes", new TableInfo.Column("notes", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsScoutResults.put("scoutedAt", new TableInfo.Column("scoutedAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsScoutResults.put("latitude", new TableInfo.Column("latitude", "REAL", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsScoutResults.put("longitude", new TableInfo.Column("longitude", "REAL", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsScoutResults.put("synced", new TableInfo.Column("synced", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysScoutResults = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesScoutResults = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoScoutResults = new TableInfo("scout_results", _columnsScoutResults, _foreignKeysScoutResults, _indicesScoutResults);
        final TableInfo _existingScoutResults = TableInfo.read(db, "scout_results");
        if (!_infoScoutResults.equals(_existingScoutResults)) {
          return new RoomOpenHelper.ValidationResult(false, "scout_results(com.vpt.scout.data.local.ScoutResultEntity).\n"
                  + " Expected:\n" + _infoScoutResults + "\n"
                  + " Found:\n" + _existingScoutResults);
        }
        return new RoomOpenHelper.ValidationResult(true, null);
      }
    }, "9776eac45c064b1fc748bb93c8587ac4", "7f97ea1bebce5d827f23732bb176f15f");
    final SupportSQLiteOpenHelper.Configuration _sqliteConfig = SupportSQLiteOpenHelper.Configuration.builder(config.context).name(config.name).callback(_openCallback).build();
    final SupportSQLiteOpenHelper _helper = config.sqliteOpenHelperFactory.create(_sqliteConfig);
    return _helper;
  }

  @Override
  @NonNull
  protected InvalidationTracker createInvalidationTracker() {
    final HashMap<String, String> _shadowTablesMap = new HashMap<String, String>(0);
    final HashMap<String, Set<String>> _viewTables = new HashMap<String, Set<String>>(0);
    return new InvalidationTracker(this, _shadowTablesMap, _viewTables, "properties","collections","collection_properties","scout_results");
  }

  @Override
  public void clearAllTables() {
    super.assertNotMainThread();
    final SupportSQLiteDatabase _db = super.getOpenHelper().getWritableDatabase();
    final boolean _supportsDeferForeignKeys = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP;
    try {
      if (!_supportsDeferForeignKeys) {
        _db.execSQL("PRAGMA foreign_keys = FALSE");
      }
      super.beginTransaction();
      if (_supportsDeferForeignKeys) {
        _db.execSQL("PRAGMA defer_foreign_keys = TRUE");
      }
      _db.execSQL("DELETE FROM `properties`");
      _db.execSQL("DELETE FROM `collections`");
      _db.execSQL("DELETE FROM `collection_properties`");
      _db.execSQL("DELETE FROM `scout_results`");
      super.setTransactionSuccessful();
    } finally {
      super.endTransaction();
      if (!_supportsDeferForeignKeys) {
        _db.execSQL("PRAGMA foreign_keys = TRUE");
      }
      _db.query("PRAGMA wal_checkpoint(FULL)").close();
      if (!_db.inTransaction()) {
        _db.execSQL("VACUUM");
      }
    }
  }

  @Override
  @NonNull
  protected Map<Class<?>, List<Class<?>>> getRequiredTypeConverters() {
    final HashMap<Class<?>, List<Class<?>>> _typeConvertersMap = new HashMap<Class<?>, List<Class<?>>>();
    _typeConvertersMap.put(PropertyDao.class, PropertyDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(CollectionDao.class, CollectionDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(ScoutResultDao.class, ScoutResultDao_Impl.getRequiredConverters());
    return _typeConvertersMap;
  }

  @Override
  @NonNull
  public Set<Class<? extends AutoMigrationSpec>> getRequiredAutoMigrationSpecs() {
    final HashSet<Class<? extends AutoMigrationSpec>> _autoMigrationSpecsSet = new HashSet<Class<? extends AutoMigrationSpec>>();
    return _autoMigrationSpecsSet;
  }

  @Override
  @NonNull
  public List<Migration> getAutoMigrations(
      @NonNull final Map<Class<? extends AutoMigrationSpec>, AutoMigrationSpec> autoMigrationSpecs) {
    final List<Migration> _autoMigrations = new ArrayList<Migration>();
    return _autoMigrations;
  }

  @Override
  public PropertyDao propertyDao() {
    if (_propertyDao != null) {
      return _propertyDao;
    } else {
      synchronized(this) {
        if(_propertyDao == null) {
          _propertyDao = new PropertyDao_Impl(this);
        }
        return _propertyDao;
      }
    }
  }

  @Override
  public CollectionDao collectionDao() {
    if (_collectionDao != null) {
      return _collectionDao;
    } else {
      synchronized(this) {
        if(_collectionDao == null) {
          _collectionDao = new CollectionDao_Impl(this);
        }
        return _collectionDao;
      }
    }
  }

  @Override
  public ScoutResultDao scoutResultDao() {
    if (_scoutResultDao != null) {
      return _scoutResultDao;
    } else {
      synchronized(this) {
        if(_scoutResultDao == null) {
          _scoutResultDao = new ScoutResultDao_Impl(this);
        }
        return _scoutResultDao;
      }
    }
  }
}

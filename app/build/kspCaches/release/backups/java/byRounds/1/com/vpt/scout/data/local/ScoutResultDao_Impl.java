package com.vpt.scout.data.local;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import java.lang.Class;
import java.lang.Double;
import java.lang.Exception;
import java.lang.IllegalStateException;
import java.lang.Integer;
import java.lang.Long;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.flow.Flow;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class ScoutResultDao_Impl implements ScoutResultDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<ScoutResultEntity> __insertionAdapterOfScoutResultEntity;

  private final Converters __converters = new Converters();

  private final SharedSQLiteStatement __preparedStmtOfMarkAsSynced;

  public ScoutResultDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfScoutResultEntity = new EntityInsertionAdapter<ScoutResultEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR ABORT INTO `scout_results` (`id`,`apn`,`collectionId`,`followUp`,`flyered`,`notes`,`scoutedAt`,`latitude`,`longitude`,`synced`) VALUES (nullif(?, 0),?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final ScoutResultEntity entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getApn());
        if (entity.getCollectionId() == null) {
          statement.bindNull(3);
        } else {
          statement.bindLong(3, entity.getCollectionId());
        }
        final int _tmp = entity.getFollowUp() ? 1 : 0;
        statement.bindLong(4, _tmp);
        final int _tmp_1 = entity.getFlyered() ? 1 : 0;
        statement.bindLong(5, _tmp_1);
        if (entity.getNotes() == null) {
          statement.bindNull(6);
        } else {
          statement.bindString(6, entity.getNotes());
        }
        final Long _tmp_2 = __converters.fromInstant(entity.getScoutedAt());
        if (_tmp_2 == null) {
          statement.bindNull(7);
        } else {
          statement.bindLong(7, _tmp_2);
        }
        if (entity.getLatitude() == null) {
          statement.bindNull(8);
        } else {
          statement.bindDouble(8, entity.getLatitude());
        }
        if (entity.getLongitude() == null) {
          statement.bindNull(9);
        } else {
          statement.bindDouble(9, entity.getLongitude());
        }
        final int _tmp_3 = entity.getSynced() ? 1 : 0;
        statement.bindLong(10, _tmp_3);
      }
    };
    this.__preparedStmtOfMarkAsSynced = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE scout_results SET synced = 1 WHERE id = ?";
        return _query;
      }
    };
  }

  @Override
  public Object insert(final ScoutResultEntity result,
      final Continuation<? super Long> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Long>() {
      @Override
      @NonNull
      public Long call() throws Exception {
        __db.beginTransaction();
        try {
          final Long _result = __insertionAdapterOfScoutResultEntity.insertAndReturnId(result);
          __db.setTransactionSuccessful();
          return _result;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object markAsSynced(final long id, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfMarkAsSynced.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, id);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfMarkAsSynced.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<ScoutResultEntity>> getAllResults() {
    final String _sql = "SELECT * FROM scout_results ORDER BY scoutedAt DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"scout_results"}, new Callable<List<ScoutResultEntity>>() {
      @Override
      @NonNull
      public List<ScoutResultEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfApn = CursorUtil.getColumnIndexOrThrow(_cursor, "apn");
          final int _cursorIndexOfCollectionId = CursorUtil.getColumnIndexOrThrow(_cursor, "collectionId");
          final int _cursorIndexOfFollowUp = CursorUtil.getColumnIndexOrThrow(_cursor, "followUp");
          final int _cursorIndexOfFlyered = CursorUtil.getColumnIndexOrThrow(_cursor, "flyered");
          final int _cursorIndexOfNotes = CursorUtil.getColumnIndexOrThrow(_cursor, "notes");
          final int _cursorIndexOfScoutedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "scoutedAt");
          final int _cursorIndexOfLatitude = CursorUtil.getColumnIndexOrThrow(_cursor, "latitude");
          final int _cursorIndexOfLongitude = CursorUtil.getColumnIndexOrThrow(_cursor, "longitude");
          final int _cursorIndexOfSynced = CursorUtil.getColumnIndexOrThrow(_cursor, "synced");
          final List<ScoutResultEntity> _result = new ArrayList<ScoutResultEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final ScoutResultEntity _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpApn;
            _tmpApn = _cursor.getString(_cursorIndexOfApn);
            final Long _tmpCollectionId;
            if (_cursor.isNull(_cursorIndexOfCollectionId)) {
              _tmpCollectionId = null;
            } else {
              _tmpCollectionId = _cursor.getLong(_cursorIndexOfCollectionId);
            }
            final boolean _tmpFollowUp;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfFollowUp);
            _tmpFollowUp = _tmp != 0;
            final boolean _tmpFlyered;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfFlyered);
            _tmpFlyered = _tmp_1 != 0;
            final String _tmpNotes;
            if (_cursor.isNull(_cursorIndexOfNotes)) {
              _tmpNotes = null;
            } else {
              _tmpNotes = _cursor.getString(_cursorIndexOfNotes);
            }
            final Instant _tmpScoutedAt;
            final Long _tmp_2;
            if (_cursor.isNull(_cursorIndexOfScoutedAt)) {
              _tmp_2 = null;
            } else {
              _tmp_2 = _cursor.getLong(_cursorIndexOfScoutedAt);
            }
            final Instant _tmp_3 = __converters.toInstant(_tmp_2);
            if (_tmp_3 == null) {
              throw new IllegalStateException("Expected NON-NULL 'java.time.Instant', but it was NULL.");
            } else {
              _tmpScoutedAt = _tmp_3;
            }
            final Double _tmpLatitude;
            if (_cursor.isNull(_cursorIndexOfLatitude)) {
              _tmpLatitude = null;
            } else {
              _tmpLatitude = _cursor.getDouble(_cursorIndexOfLatitude);
            }
            final Double _tmpLongitude;
            if (_cursor.isNull(_cursorIndexOfLongitude)) {
              _tmpLongitude = null;
            } else {
              _tmpLongitude = _cursor.getDouble(_cursorIndexOfLongitude);
            }
            final boolean _tmpSynced;
            final int _tmp_4;
            _tmp_4 = _cursor.getInt(_cursorIndexOfSynced);
            _tmpSynced = _tmp_4 != 0;
            _item = new ScoutResultEntity(_tmpId,_tmpApn,_tmpCollectionId,_tmpFollowUp,_tmpFlyered,_tmpNotes,_tmpScoutedAt,_tmpLatitude,_tmpLongitude,_tmpSynced);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Flow<List<ScoutResultEntity>> getResultsForCollection(final long collectionId) {
    final String _sql = "SELECT * FROM scout_results WHERE collectionId = ? ORDER BY scoutedAt DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, collectionId);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"scout_results"}, new Callable<List<ScoutResultEntity>>() {
      @Override
      @NonNull
      public List<ScoutResultEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfApn = CursorUtil.getColumnIndexOrThrow(_cursor, "apn");
          final int _cursorIndexOfCollectionId = CursorUtil.getColumnIndexOrThrow(_cursor, "collectionId");
          final int _cursorIndexOfFollowUp = CursorUtil.getColumnIndexOrThrow(_cursor, "followUp");
          final int _cursorIndexOfFlyered = CursorUtil.getColumnIndexOrThrow(_cursor, "flyered");
          final int _cursorIndexOfNotes = CursorUtil.getColumnIndexOrThrow(_cursor, "notes");
          final int _cursorIndexOfScoutedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "scoutedAt");
          final int _cursorIndexOfLatitude = CursorUtil.getColumnIndexOrThrow(_cursor, "latitude");
          final int _cursorIndexOfLongitude = CursorUtil.getColumnIndexOrThrow(_cursor, "longitude");
          final int _cursorIndexOfSynced = CursorUtil.getColumnIndexOrThrow(_cursor, "synced");
          final List<ScoutResultEntity> _result = new ArrayList<ScoutResultEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final ScoutResultEntity _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpApn;
            _tmpApn = _cursor.getString(_cursorIndexOfApn);
            final Long _tmpCollectionId;
            if (_cursor.isNull(_cursorIndexOfCollectionId)) {
              _tmpCollectionId = null;
            } else {
              _tmpCollectionId = _cursor.getLong(_cursorIndexOfCollectionId);
            }
            final boolean _tmpFollowUp;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfFollowUp);
            _tmpFollowUp = _tmp != 0;
            final boolean _tmpFlyered;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfFlyered);
            _tmpFlyered = _tmp_1 != 0;
            final String _tmpNotes;
            if (_cursor.isNull(_cursorIndexOfNotes)) {
              _tmpNotes = null;
            } else {
              _tmpNotes = _cursor.getString(_cursorIndexOfNotes);
            }
            final Instant _tmpScoutedAt;
            final Long _tmp_2;
            if (_cursor.isNull(_cursorIndexOfScoutedAt)) {
              _tmp_2 = null;
            } else {
              _tmp_2 = _cursor.getLong(_cursorIndexOfScoutedAt);
            }
            final Instant _tmp_3 = __converters.toInstant(_tmp_2);
            if (_tmp_3 == null) {
              throw new IllegalStateException("Expected NON-NULL 'java.time.Instant', but it was NULL.");
            } else {
              _tmpScoutedAt = _tmp_3;
            }
            final Double _tmpLatitude;
            if (_cursor.isNull(_cursorIndexOfLatitude)) {
              _tmpLatitude = null;
            } else {
              _tmpLatitude = _cursor.getDouble(_cursorIndexOfLatitude);
            }
            final Double _tmpLongitude;
            if (_cursor.isNull(_cursorIndexOfLongitude)) {
              _tmpLongitude = null;
            } else {
              _tmpLongitude = _cursor.getDouble(_cursorIndexOfLongitude);
            }
            final boolean _tmpSynced;
            final int _tmp_4;
            _tmp_4 = _cursor.getInt(_cursorIndexOfSynced);
            _tmpSynced = _tmp_4 != 0;
            _item = new ScoutResultEntity(_tmpId,_tmpApn,_tmpCollectionId,_tmpFollowUp,_tmpFlyered,_tmpNotes,_tmpScoutedAt,_tmpLatitude,_tmpLongitude,_tmpSynced);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Object getLatestResultForProperty(final String apn,
      final Continuation<? super ScoutResultEntity> $completion) {
    final String _sql = "SELECT * FROM scout_results WHERE apn = ? ORDER BY scoutedAt DESC LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, apn);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<ScoutResultEntity>() {
      @Override
      @Nullable
      public ScoutResultEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfApn = CursorUtil.getColumnIndexOrThrow(_cursor, "apn");
          final int _cursorIndexOfCollectionId = CursorUtil.getColumnIndexOrThrow(_cursor, "collectionId");
          final int _cursorIndexOfFollowUp = CursorUtil.getColumnIndexOrThrow(_cursor, "followUp");
          final int _cursorIndexOfFlyered = CursorUtil.getColumnIndexOrThrow(_cursor, "flyered");
          final int _cursorIndexOfNotes = CursorUtil.getColumnIndexOrThrow(_cursor, "notes");
          final int _cursorIndexOfScoutedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "scoutedAt");
          final int _cursorIndexOfLatitude = CursorUtil.getColumnIndexOrThrow(_cursor, "latitude");
          final int _cursorIndexOfLongitude = CursorUtil.getColumnIndexOrThrow(_cursor, "longitude");
          final int _cursorIndexOfSynced = CursorUtil.getColumnIndexOrThrow(_cursor, "synced");
          final ScoutResultEntity _result;
          if (_cursor.moveToFirst()) {
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpApn;
            _tmpApn = _cursor.getString(_cursorIndexOfApn);
            final Long _tmpCollectionId;
            if (_cursor.isNull(_cursorIndexOfCollectionId)) {
              _tmpCollectionId = null;
            } else {
              _tmpCollectionId = _cursor.getLong(_cursorIndexOfCollectionId);
            }
            final boolean _tmpFollowUp;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfFollowUp);
            _tmpFollowUp = _tmp != 0;
            final boolean _tmpFlyered;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfFlyered);
            _tmpFlyered = _tmp_1 != 0;
            final String _tmpNotes;
            if (_cursor.isNull(_cursorIndexOfNotes)) {
              _tmpNotes = null;
            } else {
              _tmpNotes = _cursor.getString(_cursorIndexOfNotes);
            }
            final Instant _tmpScoutedAt;
            final Long _tmp_2;
            if (_cursor.isNull(_cursorIndexOfScoutedAt)) {
              _tmp_2 = null;
            } else {
              _tmp_2 = _cursor.getLong(_cursorIndexOfScoutedAt);
            }
            final Instant _tmp_3 = __converters.toInstant(_tmp_2);
            if (_tmp_3 == null) {
              throw new IllegalStateException("Expected NON-NULL 'java.time.Instant', but it was NULL.");
            } else {
              _tmpScoutedAt = _tmp_3;
            }
            final Double _tmpLatitude;
            if (_cursor.isNull(_cursorIndexOfLatitude)) {
              _tmpLatitude = null;
            } else {
              _tmpLatitude = _cursor.getDouble(_cursorIndexOfLatitude);
            }
            final Double _tmpLongitude;
            if (_cursor.isNull(_cursorIndexOfLongitude)) {
              _tmpLongitude = null;
            } else {
              _tmpLongitude = _cursor.getDouble(_cursorIndexOfLongitude);
            }
            final boolean _tmpSynced;
            final int _tmp_4;
            _tmp_4 = _cursor.getInt(_cursorIndexOfSynced);
            _tmpSynced = _tmp_4 != 0;
            _result = new ScoutResultEntity(_tmpId,_tmpApn,_tmpCollectionId,_tmpFollowUp,_tmpFlyered,_tmpNotes,_tmpScoutedAt,_tmpLatitude,_tmpLongitude,_tmpSynced);
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object getScoutedApnsInCollection(final long collectionId,
      final Continuation<? super List<String>> $completion) {
    final String _sql = "SELECT apn FROM scout_results WHERE collectionId = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, collectionId);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<String>>() {
      @Override
      @NonNull
      public List<String> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final List<String> _result = new ArrayList<String>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final String _item;
            _item = _cursor.getString(0);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object getUnsyncedResults(
      final Continuation<? super List<ScoutResultEntity>> $completion) {
    final String _sql = "SELECT * FROM scout_results WHERE synced = 0";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<ScoutResultEntity>>() {
      @Override
      @NonNull
      public List<ScoutResultEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfApn = CursorUtil.getColumnIndexOrThrow(_cursor, "apn");
          final int _cursorIndexOfCollectionId = CursorUtil.getColumnIndexOrThrow(_cursor, "collectionId");
          final int _cursorIndexOfFollowUp = CursorUtil.getColumnIndexOrThrow(_cursor, "followUp");
          final int _cursorIndexOfFlyered = CursorUtil.getColumnIndexOrThrow(_cursor, "flyered");
          final int _cursorIndexOfNotes = CursorUtil.getColumnIndexOrThrow(_cursor, "notes");
          final int _cursorIndexOfScoutedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "scoutedAt");
          final int _cursorIndexOfLatitude = CursorUtil.getColumnIndexOrThrow(_cursor, "latitude");
          final int _cursorIndexOfLongitude = CursorUtil.getColumnIndexOrThrow(_cursor, "longitude");
          final int _cursorIndexOfSynced = CursorUtil.getColumnIndexOrThrow(_cursor, "synced");
          final List<ScoutResultEntity> _result = new ArrayList<ScoutResultEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final ScoutResultEntity _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpApn;
            _tmpApn = _cursor.getString(_cursorIndexOfApn);
            final Long _tmpCollectionId;
            if (_cursor.isNull(_cursorIndexOfCollectionId)) {
              _tmpCollectionId = null;
            } else {
              _tmpCollectionId = _cursor.getLong(_cursorIndexOfCollectionId);
            }
            final boolean _tmpFollowUp;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfFollowUp);
            _tmpFollowUp = _tmp != 0;
            final boolean _tmpFlyered;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfFlyered);
            _tmpFlyered = _tmp_1 != 0;
            final String _tmpNotes;
            if (_cursor.isNull(_cursorIndexOfNotes)) {
              _tmpNotes = null;
            } else {
              _tmpNotes = _cursor.getString(_cursorIndexOfNotes);
            }
            final Instant _tmpScoutedAt;
            final Long _tmp_2;
            if (_cursor.isNull(_cursorIndexOfScoutedAt)) {
              _tmp_2 = null;
            } else {
              _tmp_2 = _cursor.getLong(_cursorIndexOfScoutedAt);
            }
            final Instant _tmp_3 = __converters.toInstant(_tmp_2);
            if (_tmp_3 == null) {
              throw new IllegalStateException("Expected NON-NULL 'java.time.Instant', but it was NULL.");
            } else {
              _tmpScoutedAt = _tmp_3;
            }
            final Double _tmpLatitude;
            if (_cursor.isNull(_cursorIndexOfLatitude)) {
              _tmpLatitude = null;
            } else {
              _tmpLatitude = _cursor.getDouble(_cursorIndexOfLatitude);
            }
            final Double _tmpLongitude;
            if (_cursor.isNull(_cursorIndexOfLongitude)) {
              _tmpLongitude = null;
            } else {
              _tmpLongitude = _cursor.getDouble(_cursorIndexOfLongitude);
            }
            final boolean _tmpSynced;
            final int _tmp_4;
            _tmp_4 = _cursor.getInt(_cursorIndexOfSynced);
            _tmpSynced = _tmp_4 != 0;
            _item = new ScoutResultEntity(_tmpId,_tmpApn,_tmpCollectionId,_tmpFollowUp,_tmpFlyered,_tmpNotes,_tmpScoutedAt,_tmpLatitude,_tmpLongitude,_tmpSynced);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object getScoutedCountForCollection(final long collectionId,
      final Continuation<? super Integer> $completion) {
    final String _sql = "SELECT COUNT(*) FROM scout_results WHERE collectionId = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, collectionId);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<Integer>() {
      @Override
      @NonNull
      public Integer call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final Integer _result;
          if (_cursor.moveToFirst()) {
            final int _tmp;
            _tmp = _cursor.getInt(0);
            _result = _tmp;
          } else {
            _result = 0;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}

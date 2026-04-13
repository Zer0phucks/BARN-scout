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
import java.lang.Exception;
import java.lang.Float;
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
public final class PropertyDao_Impl implements PropertyDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<PropertyEntity> __insertionAdapterOfPropertyEntity;

  private final Converters __converters = new Converters();

  private final SharedSQLiteStatement __preparedStmtOfDeleteAll;

  public PropertyDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfPropertyEntity = new EntityInsertionAdapter<PropertyEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `properties` (`apn`,`address`,`latitude`,`longitude`,`hasVpt`,`conditionScore`,`city`,`streetViewImagePath`,`updatedAt`) VALUES (?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final PropertyEntity entity) {
        statement.bindString(1, entity.getApn());
        statement.bindString(2, entity.getAddress());
        statement.bindDouble(3, entity.getLatitude());
        statement.bindDouble(4, entity.getLongitude());
        final int _tmp = entity.getHasVpt() ? 1 : 0;
        statement.bindLong(5, _tmp);
        if (entity.getConditionScore() == null) {
          statement.bindNull(6);
        } else {
          statement.bindDouble(6, entity.getConditionScore());
        }
        if (entity.getCity() == null) {
          statement.bindNull(7);
        } else {
          statement.bindString(7, entity.getCity());
        }
        if (entity.getStreetViewImagePath() == null) {
          statement.bindNull(8);
        } else {
          statement.bindString(8, entity.getStreetViewImagePath());
        }
        final Long _tmp_1 = __converters.fromInstant(entity.getUpdatedAt());
        if (_tmp_1 == null) {
          statement.bindNull(9);
        } else {
          statement.bindLong(9, _tmp_1);
        }
      }
    };
    this.__preparedStmtOfDeleteAll = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM properties";
        return _query;
      }
    };
  }

  @Override
  public Object insertAll(final List<PropertyEntity> properties,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfPropertyEntity.insert(properties);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object insert(final PropertyEntity property,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfPropertyEntity.insert(property);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteAll(final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteAll.acquire();
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
          __preparedStmtOfDeleteAll.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<PropertyEntity>> getAllProperties() {
    final String _sql = "SELECT * FROM properties ORDER BY city, address";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"properties"}, new Callable<List<PropertyEntity>>() {
      @Override
      @NonNull
      public List<PropertyEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfApn = CursorUtil.getColumnIndexOrThrow(_cursor, "apn");
          final int _cursorIndexOfAddress = CursorUtil.getColumnIndexOrThrow(_cursor, "address");
          final int _cursorIndexOfLatitude = CursorUtil.getColumnIndexOrThrow(_cursor, "latitude");
          final int _cursorIndexOfLongitude = CursorUtil.getColumnIndexOrThrow(_cursor, "longitude");
          final int _cursorIndexOfHasVpt = CursorUtil.getColumnIndexOrThrow(_cursor, "hasVpt");
          final int _cursorIndexOfConditionScore = CursorUtil.getColumnIndexOrThrow(_cursor, "conditionScore");
          final int _cursorIndexOfCity = CursorUtil.getColumnIndexOrThrow(_cursor, "city");
          final int _cursorIndexOfStreetViewImagePath = CursorUtil.getColumnIndexOrThrow(_cursor, "streetViewImagePath");
          final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updatedAt");
          final List<PropertyEntity> _result = new ArrayList<PropertyEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final PropertyEntity _item;
            final String _tmpApn;
            _tmpApn = _cursor.getString(_cursorIndexOfApn);
            final String _tmpAddress;
            _tmpAddress = _cursor.getString(_cursorIndexOfAddress);
            final double _tmpLatitude;
            _tmpLatitude = _cursor.getDouble(_cursorIndexOfLatitude);
            final double _tmpLongitude;
            _tmpLongitude = _cursor.getDouble(_cursorIndexOfLongitude);
            final boolean _tmpHasVpt;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfHasVpt);
            _tmpHasVpt = _tmp != 0;
            final Float _tmpConditionScore;
            if (_cursor.isNull(_cursorIndexOfConditionScore)) {
              _tmpConditionScore = null;
            } else {
              _tmpConditionScore = _cursor.getFloat(_cursorIndexOfConditionScore);
            }
            final String _tmpCity;
            if (_cursor.isNull(_cursorIndexOfCity)) {
              _tmpCity = null;
            } else {
              _tmpCity = _cursor.getString(_cursorIndexOfCity);
            }
            final String _tmpStreetViewImagePath;
            if (_cursor.isNull(_cursorIndexOfStreetViewImagePath)) {
              _tmpStreetViewImagePath = null;
            } else {
              _tmpStreetViewImagePath = _cursor.getString(_cursorIndexOfStreetViewImagePath);
            }
            final Instant _tmpUpdatedAt;
            final Long _tmp_1;
            if (_cursor.isNull(_cursorIndexOfUpdatedAt)) {
              _tmp_1 = null;
            } else {
              _tmp_1 = _cursor.getLong(_cursorIndexOfUpdatedAt);
            }
            final Instant _tmp_2 = __converters.toInstant(_tmp_1);
            if (_tmp_2 == null) {
              throw new IllegalStateException("Expected NON-NULL 'java.time.Instant', but it was NULL.");
            } else {
              _tmpUpdatedAt = _tmp_2;
            }
            _item = new PropertyEntity(_tmpApn,_tmpAddress,_tmpLatitude,_tmpLongitude,_tmpHasVpt,_tmpConditionScore,_tmpCity,_tmpStreetViewImagePath,_tmpUpdatedAt);
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
  public Object getPropertyByApn(final String apn,
      final Continuation<? super PropertyEntity> $completion) {
    final String _sql = "SELECT * FROM properties WHERE apn = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, apn);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<PropertyEntity>() {
      @Override
      @Nullable
      public PropertyEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfApn = CursorUtil.getColumnIndexOrThrow(_cursor, "apn");
          final int _cursorIndexOfAddress = CursorUtil.getColumnIndexOrThrow(_cursor, "address");
          final int _cursorIndexOfLatitude = CursorUtil.getColumnIndexOrThrow(_cursor, "latitude");
          final int _cursorIndexOfLongitude = CursorUtil.getColumnIndexOrThrow(_cursor, "longitude");
          final int _cursorIndexOfHasVpt = CursorUtil.getColumnIndexOrThrow(_cursor, "hasVpt");
          final int _cursorIndexOfConditionScore = CursorUtil.getColumnIndexOrThrow(_cursor, "conditionScore");
          final int _cursorIndexOfCity = CursorUtil.getColumnIndexOrThrow(_cursor, "city");
          final int _cursorIndexOfStreetViewImagePath = CursorUtil.getColumnIndexOrThrow(_cursor, "streetViewImagePath");
          final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updatedAt");
          final PropertyEntity _result;
          if (_cursor.moveToFirst()) {
            final String _tmpApn;
            _tmpApn = _cursor.getString(_cursorIndexOfApn);
            final String _tmpAddress;
            _tmpAddress = _cursor.getString(_cursorIndexOfAddress);
            final double _tmpLatitude;
            _tmpLatitude = _cursor.getDouble(_cursorIndexOfLatitude);
            final double _tmpLongitude;
            _tmpLongitude = _cursor.getDouble(_cursorIndexOfLongitude);
            final boolean _tmpHasVpt;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfHasVpt);
            _tmpHasVpt = _tmp != 0;
            final Float _tmpConditionScore;
            if (_cursor.isNull(_cursorIndexOfConditionScore)) {
              _tmpConditionScore = null;
            } else {
              _tmpConditionScore = _cursor.getFloat(_cursorIndexOfConditionScore);
            }
            final String _tmpCity;
            if (_cursor.isNull(_cursorIndexOfCity)) {
              _tmpCity = null;
            } else {
              _tmpCity = _cursor.getString(_cursorIndexOfCity);
            }
            final String _tmpStreetViewImagePath;
            if (_cursor.isNull(_cursorIndexOfStreetViewImagePath)) {
              _tmpStreetViewImagePath = null;
            } else {
              _tmpStreetViewImagePath = _cursor.getString(_cursorIndexOfStreetViewImagePath);
            }
            final Instant _tmpUpdatedAt;
            final Long _tmp_1;
            if (_cursor.isNull(_cursorIndexOfUpdatedAt)) {
              _tmp_1 = null;
            } else {
              _tmp_1 = _cursor.getLong(_cursorIndexOfUpdatedAt);
            }
            final Instant _tmp_2 = __converters.toInstant(_tmp_1);
            if (_tmp_2 == null) {
              throw new IllegalStateException("Expected NON-NULL 'java.time.Instant', but it was NULL.");
            } else {
              _tmpUpdatedAt = _tmp_2;
            }
            _result = new PropertyEntity(_tmpApn,_tmpAddress,_tmpLatitude,_tmpLongitude,_tmpHasVpt,_tmpConditionScore,_tmpCity,_tmpStreetViewImagePath,_tmpUpdatedAt);
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
  public Flow<List<PropertyEntity>> getVptProperties() {
    final String _sql = "SELECT * FROM properties WHERE hasVpt = 1 ORDER BY city, address";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"properties"}, new Callable<List<PropertyEntity>>() {
      @Override
      @NonNull
      public List<PropertyEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfApn = CursorUtil.getColumnIndexOrThrow(_cursor, "apn");
          final int _cursorIndexOfAddress = CursorUtil.getColumnIndexOrThrow(_cursor, "address");
          final int _cursorIndexOfLatitude = CursorUtil.getColumnIndexOrThrow(_cursor, "latitude");
          final int _cursorIndexOfLongitude = CursorUtil.getColumnIndexOrThrow(_cursor, "longitude");
          final int _cursorIndexOfHasVpt = CursorUtil.getColumnIndexOrThrow(_cursor, "hasVpt");
          final int _cursorIndexOfConditionScore = CursorUtil.getColumnIndexOrThrow(_cursor, "conditionScore");
          final int _cursorIndexOfCity = CursorUtil.getColumnIndexOrThrow(_cursor, "city");
          final int _cursorIndexOfStreetViewImagePath = CursorUtil.getColumnIndexOrThrow(_cursor, "streetViewImagePath");
          final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updatedAt");
          final List<PropertyEntity> _result = new ArrayList<PropertyEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final PropertyEntity _item;
            final String _tmpApn;
            _tmpApn = _cursor.getString(_cursorIndexOfApn);
            final String _tmpAddress;
            _tmpAddress = _cursor.getString(_cursorIndexOfAddress);
            final double _tmpLatitude;
            _tmpLatitude = _cursor.getDouble(_cursorIndexOfLatitude);
            final double _tmpLongitude;
            _tmpLongitude = _cursor.getDouble(_cursorIndexOfLongitude);
            final boolean _tmpHasVpt;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfHasVpt);
            _tmpHasVpt = _tmp != 0;
            final Float _tmpConditionScore;
            if (_cursor.isNull(_cursorIndexOfConditionScore)) {
              _tmpConditionScore = null;
            } else {
              _tmpConditionScore = _cursor.getFloat(_cursorIndexOfConditionScore);
            }
            final String _tmpCity;
            if (_cursor.isNull(_cursorIndexOfCity)) {
              _tmpCity = null;
            } else {
              _tmpCity = _cursor.getString(_cursorIndexOfCity);
            }
            final String _tmpStreetViewImagePath;
            if (_cursor.isNull(_cursorIndexOfStreetViewImagePath)) {
              _tmpStreetViewImagePath = null;
            } else {
              _tmpStreetViewImagePath = _cursor.getString(_cursorIndexOfStreetViewImagePath);
            }
            final Instant _tmpUpdatedAt;
            final Long _tmp_1;
            if (_cursor.isNull(_cursorIndexOfUpdatedAt)) {
              _tmp_1 = null;
            } else {
              _tmp_1 = _cursor.getLong(_cursorIndexOfUpdatedAt);
            }
            final Instant _tmp_2 = __converters.toInstant(_tmp_1);
            if (_tmp_2 == null) {
              throw new IllegalStateException("Expected NON-NULL 'java.time.Instant', but it was NULL.");
            } else {
              _tmpUpdatedAt = _tmp_2;
            }
            _item = new PropertyEntity(_tmpApn,_tmpAddress,_tmpLatitude,_tmpLongitude,_tmpHasVpt,_tmpConditionScore,_tmpCity,_tmpStreetViewImagePath,_tmpUpdatedAt);
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
  public Flow<List<PropertyEntity>> getPropertiesByCity(final String city) {
    final String _sql = "SELECT * FROM properties WHERE city = ? ORDER BY address";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, city);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"properties"}, new Callable<List<PropertyEntity>>() {
      @Override
      @NonNull
      public List<PropertyEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfApn = CursorUtil.getColumnIndexOrThrow(_cursor, "apn");
          final int _cursorIndexOfAddress = CursorUtil.getColumnIndexOrThrow(_cursor, "address");
          final int _cursorIndexOfLatitude = CursorUtil.getColumnIndexOrThrow(_cursor, "latitude");
          final int _cursorIndexOfLongitude = CursorUtil.getColumnIndexOrThrow(_cursor, "longitude");
          final int _cursorIndexOfHasVpt = CursorUtil.getColumnIndexOrThrow(_cursor, "hasVpt");
          final int _cursorIndexOfConditionScore = CursorUtil.getColumnIndexOrThrow(_cursor, "conditionScore");
          final int _cursorIndexOfCity = CursorUtil.getColumnIndexOrThrow(_cursor, "city");
          final int _cursorIndexOfStreetViewImagePath = CursorUtil.getColumnIndexOrThrow(_cursor, "streetViewImagePath");
          final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updatedAt");
          final List<PropertyEntity> _result = new ArrayList<PropertyEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final PropertyEntity _item;
            final String _tmpApn;
            _tmpApn = _cursor.getString(_cursorIndexOfApn);
            final String _tmpAddress;
            _tmpAddress = _cursor.getString(_cursorIndexOfAddress);
            final double _tmpLatitude;
            _tmpLatitude = _cursor.getDouble(_cursorIndexOfLatitude);
            final double _tmpLongitude;
            _tmpLongitude = _cursor.getDouble(_cursorIndexOfLongitude);
            final boolean _tmpHasVpt;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfHasVpt);
            _tmpHasVpt = _tmp != 0;
            final Float _tmpConditionScore;
            if (_cursor.isNull(_cursorIndexOfConditionScore)) {
              _tmpConditionScore = null;
            } else {
              _tmpConditionScore = _cursor.getFloat(_cursorIndexOfConditionScore);
            }
            final String _tmpCity;
            if (_cursor.isNull(_cursorIndexOfCity)) {
              _tmpCity = null;
            } else {
              _tmpCity = _cursor.getString(_cursorIndexOfCity);
            }
            final String _tmpStreetViewImagePath;
            if (_cursor.isNull(_cursorIndexOfStreetViewImagePath)) {
              _tmpStreetViewImagePath = null;
            } else {
              _tmpStreetViewImagePath = _cursor.getString(_cursorIndexOfStreetViewImagePath);
            }
            final Instant _tmpUpdatedAt;
            final Long _tmp_1;
            if (_cursor.isNull(_cursorIndexOfUpdatedAt)) {
              _tmp_1 = null;
            } else {
              _tmp_1 = _cursor.getLong(_cursorIndexOfUpdatedAt);
            }
            final Instant _tmp_2 = __converters.toInstant(_tmp_1);
            if (_tmp_2 == null) {
              throw new IllegalStateException("Expected NON-NULL 'java.time.Instant', but it was NULL.");
            } else {
              _tmpUpdatedAt = _tmp_2;
            }
            _item = new PropertyEntity(_tmpApn,_tmpAddress,_tmpLatitude,_tmpLongitude,_tmpHasVpt,_tmpConditionScore,_tmpCity,_tmpStreetViewImagePath,_tmpUpdatedAt);
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
  public Object getCount(final Continuation<? super Integer> $completion) {
    final String _sql = "SELECT COUNT(*) FROM properties";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
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

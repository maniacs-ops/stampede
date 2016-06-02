/*
 *     This file is part of ToroDB.
 *
 *     ToroDB is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Affero General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     ToroDB is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Affero General Public License for more details.
 *
 *     You should have received a copy of the GNU Affero General Public License
 *     along with ToroDB. If not, see <http://www.gnu.org/licenses/>.
 *
 *     Copyright (c) 2014, 8Kdata Technology
 *     
 */

package com.torodb.backend.tables;

import org.jooq.Identity;
import org.jooq.TableField;
import org.jooq.impl.AbstractKeys;
import org.jooq.impl.SQLDataType;

import com.torodb.backend.DatabaseInterface;
import com.torodb.backend.meta.DatabaseSchema;
import com.torodb.backend.tables.records.RootDocPartTableRecord;
import com.torodb.core.TableRef;
import com.torodb.core.transaction.metainf.MetaDocPart;

/**
 *
 */
public class RootDocPartTable extends AbstractDocPartTable<RootDocPartTableRecord> {
    
    private static final long serialVersionUID = 2532216259252881711L;

    private final TableField<RootDocPartTableRecord, Integer> didField
            = createField(DID_COLUMN_NAME, SQLDataType.INTEGER.nullable(false), this, "");
    
    public RootDocPartTable(
            String database,
            String collection,
            TableRef tableRef,
            DatabaseSchema schema,
            String tableName,
            MetaDocPart metaDocPart,
            DatabaseInterface databaseInterface
    ) {
        super(database, collection, tableRef,
                schema, tableName, 
                metaDocPart, databaseInterface);
    }

    public TableField<RootDocPartTableRecord, Integer> getDidColumn() {
        return didField;
    }

    /**
     * The class holding records for this type
     * <p>
     * @return
     */
    @Override
    public Class<RootDocPartTableRecord> getRecordType() {
        return RootDocPartTableRecord.class;
    }

    /**
     * {@inheritDoc}
     * <p>
     * @return
     */
    @Override
    public Identity<RootDocPartTableRecord, Integer> getIdentity() {
        if (identityRoot == null) {
            synchronized (this) {
                identityRoot = IdentityFactory.createIdentity(this);
            }
        }
        return identityRoot;
    }

    /**
     * {@inheritDoc}
     * <p>
     * @param alias
     * @return
     */
    @Override
    public RootDocPartTable as(String alias) {
        return new RootDocPartTable(database, collection, tableRef, getSchema(), 
                alias, metaDocPart, databaseInterface);
    }

    /**
     * Rename this table
     * <p>
     * @param name
     * @return
     */
    public RootDocPartTable rename(String name) {
        return new RootDocPartTable(database, collection, tableRef, getSchema(), 
                name, metaDocPart, databaseInterface);
    }

    private static class IdentityFactory extends AbstractKeys {
        public static Identity<RootDocPartTableRecord, Integer> createIdentity(RootDocPartTable table) {
            return createIdentity(table, table.didField);
        }
    }
}

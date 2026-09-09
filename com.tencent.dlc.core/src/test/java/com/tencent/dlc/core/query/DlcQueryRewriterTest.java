/*
 * Tencent DLC Core
 * Copyright (C) 2026 Bing Tong and contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.tencent.dlc.core.query;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class DlcQueryRewriterTest {

    @Test
    void describeTableWithSchemaIsRewritten() {
        String sql = "DESCRIBE TABLE gold_transaction.transaction_fact";
        String rewritten = DlcQueryRewriter.rewrite(sql);
        assertEquals(
            "SELECT column_name AS col_name, column_type AS data_type, column_comment AS comment "
                + "FROM information_schema.columns "
                + "WHERE schema_name = 'gold_transaction' AND table_name = 'transaction_fact' "
                + "ORDER BY column_position",
            rewritten
        );
    }

    @Test
    void descWithoutTableKeywordIsRewritten() {
        String sql = "desc gold_transaction.transaction_fact";
        String rewritten = DlcQueryRewriter.rewrite(sql);
        assertEquals(
            "SELECT column_name AS col_name, column_type AS data_type, column_comment AS comment "
                + "FROM information_schema.columns "
                + "WHERE schema_name = 'gold_transaction' AND table_name = 'transaction_fact' "
                + "ORDER BY column_position",
            rewritten
        );
    }

    @Test
    void describeExtendedIsRewrittenIgnoringModifier() {
        String sql = "DESCRIBE EXTENDED gold_transaction.transaction_fact";
        String rewritten = DlcQueryRewriter.rewrite(sql);
        assertEquals(
            "SELECT column_name AS col_name, column_type AS data_type, column_comment AS comment "
                + "FROM information_schema.columns "
                + "WHERE schema_name = 'gold_transaction' AND table_name = 'transaction_fact' "
                + "ORDER BY column_position",
            rewritten
        );
    }

    @Test
    void describeWithBackticksIsRewritten() {
        String sql = "DESCRIBE TABLE `gold_transaction`.`transaction_fact`";
        String rewritten = DlcQueryRewriter.rewrite(sql);
        assertEquals(
            "SELECT column_name AS col_name, column_type AS data_type, column_comment AS comment "
                + "FROM information_schema.columns "
                + "WHERE schema_name = 'gold_transaction' AND table_name = 'transaction_fact' "
                + "ORDER BY column_position",
            rewritten
        );
    }

    @Test
    void unqualifiedTableUsesCurrentDatabase() {
        String sql = "DESCRIBE transaction_fact";
        String rewritten = DlcQueryRewriter.rewrite(sql);
        assertEquals(
            "SELECT column_name AS col_name, column_type AS data_type, column_comment AS comment "
                + "FROM information_schema.columns "
                + "WHERE schema_name = current_database() AND table_name = 'transaction_fact' "
                + "ORDER BY column_position",
            rewritten
        );
    }

    @Test
    void describeWithCatalogIsRewritten() {
        String sql = "DESCRIBE DataLakeCatalog.gold_transaction.transaction_fact";
        String rewritten = DlcQueryRewriter.rewrite(sql);
        assertEquals(
            "SELECT column_name AS col_name, column_type AS data_type, column_comment AS comment "
                + "FROM information_schema.columns "
                + "WHERE catalog_name = 'DataLakeCatalog' AND schema_name = 'gold_transaction' "
                + "AND table_name = 'transaction_fact' "
                + "ORDER BY column_position",
            rewritten
        );
    }

    @Test
    void describeWithBacktickedCatalogIsRewritten() {
        String sql = "DESCRIBE `DataLakeCatalog`.`gold_transaction`.`transaction_fact`";
        String rewritten = DlcQueryRewriter.rewrite(sql);
        assertEquals(
            "SELECT column_name AS col_name, column_type AS data_type, column_comment AS comment "
                + "FROM information_schema.columns "
                + "WHERE catalog_name = 'DataLakeCatalog' AND schema_name = 'gold_transaction' "
                + "AND table_name = 'transaction_fact' "
                + "ORDER BY column_position",
            rewritten
        );
    }

    @Test
    void nonDescribeQueryIsPassedThrough() {
        String sql = "SELECT * FROM gold_transaction.transaction_fact LIMIT 10";
        String rewritten = DlcQueryRewriter.rewrite(sql);
        assertEquals(sql, rewritten);
    }

    @Test
    void nullInputReturnsNull() {
        assertNull(DlcQueryRewriter.rewrite(null));
    }
}

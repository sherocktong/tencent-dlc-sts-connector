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

import com.tencent.dlc.core.query.DlcQueryRewriter.DescribeTarget;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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

    @Test
    void twoPartNameUsesDefaultCatalog() {
        String sql = "DESCRIBE TABLE makro.dsm_daily_sales_metrics_tmp_nodisc";
        String rewritten = DlcQueryRewriter.rewrite(sql, "DataLakeCatalog");
        assertEquals(
            "SELECT column_name AS col_name, column_type AS data_type, column_comment AS comment "
                + "FROM information_schema.columns "
                + "WHERE catalog_name = 'DataLakeCatalog' AND schema_name = 'makro' "
                + "AND table_name = 'dsm_daily_sales_metrics_tmp_nodisc' "
                + "ORDER BY column_position",
            rewritten
        );
    }

    @Test
    void unqualifiedNameUsesDefaultCatalog() {
        String sql = "DESCRIBE transaction_fact";
        String rewritten = DlcQueryRewriter.rewrite(sql, "DataLakeCatalog");
        assertEquals(
            "SELECT column_name AS col_name, column_type AS data_type, column_comment AS comment "
                + "FROM information_schema.columns "
                + "WHERE catalog_name = 'DataLakeCatalog' AND schema_name = current_database() "
                + "AND table_name = 'transaction_fact' "
                + "ORDER BY column_position",
            rewritten
        );
    }

    @Test
    void explicitCatalogWinsOverDefault() {
        String sql = "DESCRIBE spark_catalog.gold_transaction.transaction_fact";
        String rewritten = DlcQueryRewriter.rewrite(sql, "DataLakeCatalog");
        assertEquals(
            "SELECT column_name AS col_name, column_type AS data_type, column_comment AS comment "
                + "FROM information_schema.columns "
                + "WHERE catalog_name = 'spark_catalog' AND schema_name = 'gold_transaction' "
                + "AND table_name = 'transaction_fact' "
                + "ORDER BY column_position",
            rewritten
        );
    }

    @Test
    void blankDefaultCatalogIsIgnored() {
        String sql = "DESCRIBE gold_transaction.transaction_fact";
        String expected =
            "SELECT column_name AS col_name, column_type AS data_type, column_comment AS comment "
                + "FROM information_schema.columns "
                + "WHERE schema_name = 'gold_transaction' AND table_name = 'transaction_fact' "
                + "ORDER BY column_position";
        assertEquals(expected, DlcQueryRewriter.rewrite(sql, null));
        assertEquals(expected, DlcQueryRewriter.rewrite(sql, ""));
    }

    @Test
    void nonDescribeQueryIsPassedThroughWithDefaultCatalog() {
        String sql = "SELECT * FROM gold_transaction.transaction_fact LIMIT 10";
        assertEquals(sql, DlcQueryRewriter.rewrite(sql, "DataLakeCatalog"));
    }

    @Test
    void parseDescribeExtractsCatalogSchemaTable() {
        DescribeTarget target = DlcQueryRewriter.parseDescribe(
            "describe table makro_shared_biz_catalog.cr_bi.prinn_component_sales_percentage");
        assertNotNull(target);
        assertEquals("makro_shared_biz_catalog", target.getCatalog());
        assertEquals("cr_bi", target.getSchema());
        assertEquals("prinn_component_sales_percentage", target.getTable());
        assertEquals("`makro_shared_biz_catalog`.`cr_bi`.`prinn_component_sales_percentage`",
            target.qualifiedName());
    }

    @Test
    void parseDescribeTreatsTwoPartNameAsSchema() {
        DescribeTarget target = DlcQueryRewriter.parseDescribe("DESC gold_transaction.transaction_fact");
        assertNotNull(target);
        assertNull(target.getCatalog());
        assertEquals("gold_transaction", target.getSchema());
        assertEquals("transaction_fact", target.getTable());
        assertEquals("`gold_transaction`.`transaction_fact`", target.qualifiedName());
    }

    @Test
    void parseDescribeHandlesUnqualifiedAndBacktickedNames() {
        DescribeTarget unqualified = DlcQueryRewriter.parseDescribe("DESCRIBE transaction_fact");
        assertNotNull(unqualified);
        assertNull(unqualified.getCatalog());
        assertNull(unqualified.getSchema());
        assertEquals("transaction_fact", unqualified.getTable());
        assertEquals("`transaction_fact`", unqualified.qualifiedName());

        DescribeTarget backticked = DlcQueryRewriter.parseDescribe("DESCRIBE `cat`.`sch`.`tab`");
        assertNotNull(backticked);
        assertEquals("cat", backticked.getCatalog());
        assertEquals("sch", backticked.getSchema());
        assertEquals("tab", backticked.getTable());
    }

    @Test
    void parseDescribeIgnoresExtendedAndFormatted() {
        DescribeTarget target = DlcQueryRewriter.parseDescribe("DESCRIBE FORMATTED cat.sch.tab");
        assertNotNull(target);
        assertEquals("cat", target.getCatalog());
        assertEquals("sch", target.getSchema());
        assertEquals("tab", target.getTable());
    }

    @Test
    void parseDescribeReturnsNullForNonDescribeSql() {
        assertNull(DlcQueryRewriter.parseDescribe("SELECT * FROM cat.sch.tab"));
        assertNull(DlcQueryRewriter.parseDescribe("SHOW TABLES IN cat.sch"));
        assertNull(DlcQueryRewriter.parseDescribe(null));
    }

    @Test
    void describeProbeSqlSelectsZeroRows() {
        DescribeTarget target = DlcQueryRewriter.parseDescribe("DESCRIBE cat.sch.tab");
        assertEquals("SELECT * FROM `cat`.`sch`.`tab` LIMIT 0", DlcQueryRewriter.describeProbeSql(target));

        DescribeTarget unqualified = DlcQueryRewriter.parseDescribe("DESCRIBE tab");
        assertEquals("SELECT * FROM `tab` LIMIT 0", DlcQueryRewriter.describeProbeSql(unqualified));
    }
}

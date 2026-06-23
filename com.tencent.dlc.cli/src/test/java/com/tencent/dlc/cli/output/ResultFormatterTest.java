/*
 * Tencent DLC CLI
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
package com.tencent.dlc.cli.output;

import com.tencent.dlc.core.result.DlcResultSetHandler;
import org.junit.jupiter.api.Test;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ResultFormatterTest {

    @Test
    void tableFormatterPrintsHeadersAndRows() throws SQLException {
        ResultSet resultSet = createSampleResultSet();
        TableFormatter formatter = new TableFormatter();
        StringWriter output = new StringWriter();

        formatter.format(resultSet, new PrintWriter(output), -1);

        String text = output.toString();
        assertTrue(text.contains("id"));
        assertTrue(text.contains("name"));
        assertTrue(text.contains("Alice"));
        assertTrue(text.contains("Bob"));
        assertTrue(text.contains("2 row(s)"));
    }

    @Test
    void csvFormatterPrintsCommaSeparatedValues() throws SQLException {
        ResultSet resultSet = createSampleResultSet();
        CsvFormatter formatter = new CsvFormatter();
        StringWriter output = new StringWriter();

        formatter.format(resultSet, new PrintWriter(output), -1);

        String text = output.toString();
        assertTrue(text.contains("id,name"));
        assertTrue(text.contains("1,Alice"));
        assertTrue(text.contains("2,Bob"));
    }

    @Test
    void jsonFormatterPrintsArrayOfObjects() throws SQLException {
        ResultSet resultSet = createSampleResultSet();
        JsonFormatter formatter = new JsonFormatter();
        StringWriter output = new StringWriter();

        formatter.format(resultSet, new PrintWriter(output), -1);

        String text = output.toString();
        assertTrue(text.contains("["));
        assertTrue(text.contains("\"id\": \"1\""));
        assertTrue(text.contains("\"name\": \"Alice\""));
        assertTrue(text.contains("]"));
    }

    @Test
    void yamlFormatterPrintsListOfMaps() throws SQLException {
        ResultSet resultSet = createSampleResultSet();
        YamlFormatter formatter = new YamlFormatter();
        StringWriter output = new StringWriter();

        formatter.format(resultSet, new PrintWriter(output), -1);

        String text = output.toString();
        assertTrue(text.contains("-"));
        assertTrue(text.contains("id: 1"));
        assertTrue(text.contains("name: Alice"));
        assertTrue(text.contains("# 2 row(s)"));
    }

    @Test
    void beautifiedTableFormatterPrintsSingleLineRows() throws SQLException {
        ResultSet resultSet = createSampleResultSet();
        BeautifiedTableFormatter formatter = new BeautifiedTableFormatter();
        StringWriter output = new StringWriter();

        formatter.format(resultSet, new PrintWriter(output), -1);

        String text = output.toString();
        assertTrue(text.contains("id | name"));
        assertTrue(text.contains("1  | Alice"));
        assertTrue(text.contains("2  | Bob"));
        assertTrue(text.contains("2 row(s)"));
    }

    @Test
    void beautifiedTableFormatterRemovesNewlinesInCells() throws SQLException {
        ResultSetMetaData metaData = mock(ResultSetMetaData.class);
        when(metaData.getColumnCount()).thenReturn(1);
        when(metaData.getColumnLabel(1)).thenReturn("value");

        ResultSet resultSet = mock(ResultSet.class);
        when(resultSet.getMetaData()).thenReturn(metaData);
        when(resultSet.next()).thenReturn(true, false);
        when(resultSet.getObject(1)).thenReturn("line1\nline2\rline3");

        BeautifiedTableFormatter formatter = new BeautifiedTableFormatter();
        StringWriter output = new StringWriter();

        formatter.format(resultSet, new PrintWriter(output), -1);

        String text = output.toString();
        assertTrue(text.contains("line1 line2 line3"));
        assertTrue(!text.contains("\nline2"));
    }

    @Test
    void sharedHandlerProvidesSafeMetadataWhenDriverReturnsNull() throws SQLException {
        ResultSet rawResultSet = mock(ResultSet.class);
        when(rawResultSet.getMetaData()).thenReturn(null);
        when(rawResultSet.next()).thenReturn(true, true, false);
        when(rawResultSet.getObject(1)).thenReturn("first", "second");

        ResultSet wrapped = DlcResultSetHandler.safeMetaData(rawResultSet);
        ResultSetMetaData metaData = wrapped.getMetaData();
        assertTrue(metaData != null);
        assertTrue(metaData.getColumnCount() == 1);
        assertTrue("result".equals(metaData.getColumnLabel(1)));

        BeautifiedTableFormatter formatter = new BeautifiedTableFormatter();
        StringWriter output = new StringWriter();
        formatter.format(wrapped, new PrintWriter(output), -1);

        String text = output.toString();
        assertTrue(text.contains("first"));
        assertTrue(text.contains("second"));
        assertTrue(text.contains("2 row(s)"));
    }

    private ResultSet createSampleResultSet() throws SQLException {
        ResultSetMetaData metaData = mock(ResultSetMetaData.class);
        when(metaData.getColumnCount()).thenReturn(2);
        when(metaData.getColumnLabel(1)).thenReturn("id");
        when(metaData.getColumnLabel(2)).thenReturn("name");

        ResultSet resultSet = mock(ResultSet.class);
        when(resultSet.getMetaData()).thenReturn(metaData);
        when(resultSet.next()).thenReturn(true, true, false);
        when(resultSet.getObject(1)).thenReturn("1", "2");
        when(resultSet.getObject(2)).thenReturn("Alice", "Bob");

        return resultSet;
    }
}

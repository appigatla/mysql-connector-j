/*
   Copyright (C) 2002 MySQL AB
     This program is free software; you can redistribute it and/or modify
     it under the terms of the GNU General Public License as published by
     the Free Software Foundation; either version 2 of the License, or
     (at your option) any later version.
     This program is distributed in the hope that it will be useful,
     but WITHOUT ANY WARRANTY; without even the implied warranty of
     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
     GNU General Public License for more details.
     You should have received a copy of the GNU General Public License
     along with this program; if not, write to the Free Software
     Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
     
 */
package testsuite.simple;

import com.mysql.jdbc.NotImplemented;

import java.sql.*;

import junit.framework.Test;

import testsuite.BaseTestCase;


public class StatementsTest
    extends BaseTestCase {

    //~ Constructors ..........................................................

    /**
     * Creates a new StatementsTest object.
     * 
     * @param name DOCUMENT ME!
     */
    public StatementsTest(String name) {
        super(name);
    }

    //~ Methods ...............................................................

    /**
     * DOCUMENT ME!
     * 
     * @param args DOCUMENT ME!
     * @throws Exception DOCUMENT ME!
     */
    public static void main(String[] args)
                     throws Exception {
        new StatementsTest("testStubbed").run();
        new StatementsTest("testInsert").run();
        new StatementsTest("testAutoIncrement").run();
        new StatementsTest("testPreparedStatement").run();
        new StatementsTest("testPreparedStatementBatch").run();
        new StatementsTest("testClose").run();
    }

    /**
     * DOCUMENT ME!
     * 
     * @throws Exception DOCUMENT ME!
     */
    public void setUp()
               throws Exception {
        super.setUp();

        try {
            stmt.executeUpdate("DROP TABLE statement_test");
        } /* ignore */ catch (SQLException sqlEx) {
        }

        stmt.executeUpdate(
                "CREATE TABLE statement_test (id int not null primary key auto_increment, strdata1 varchar(255) not null, strdata2 varchar(255))");

        // explicitly set the catalog to exercise code in execute(), executeQuery() and
        // executeUpdate()
        // FIXME: Only works on Windows!
        //conn.setCatalog(conn.getCatalog().toUpperCase());
    }

    /**
     * DOCUMENT ME!
     * 
     * @throws Exception DOCUMENT ME!
     */
    public void tearDown()
                  throws Exception {
        stmt.executeUpdate("DROP TABLE statement_test");
        super.tearDown();
    }

    /**
     * DOCUMENT ME!
     * 
     * @throws SQLException DOCUMENT ME!
     */
    public void testAccessorsAndMutators()
                                  throws SQLException {
        assertTrue("Connection can not be null, and must be same connection", 
                   stmt.getConnection() == conn);

        // Set max rows, to exercise code in execute(), executeQuery() and executeUpdate()
        Statement accessorStmt = null;

        try {
            accessorStmt = conn.createStatement();
            accessorStmt.setMaxRows(1);
            accessorStmt.setMaxRows(0); // FIXME, test that this actually affects rows returned
            accessorStmt.setMaxFieldSize(255);
            assertTrue("Max field size should match what was set", accessorStmt.getMaxFieldSize() == 255);
            
            try {
            	accessorStmt.setMaxFieldSize(Integer.MAX_VALUE);
            	fail("Should not be able to set max field size > max_packet_size");
            } catch (SQLException sqlEx) { /* ignore */ }
           
            accessorStmt.setCursorName("undef");
            accessorStmt.setEscapeProcessing(true);
            accessorStmt.setFetchDirection(java.sql.ResultSet.FETCH_FORWARD);

            int fetchDirection = accessorStmt.getFetchDirection();
            assertTrue("Set fetch direction != get fetch direction", 
                       fetchDirection == java.sql.ResultSet.FETCH_FORWARD);

            try {
                accessorStmt.setFetchDirection(Integer.MAX_VALUE);
                fail("Should not be able to set fetch direction to invalid value");
            } catch (SQLException sqlEx) { /* ignore */
            }

            try {
                accessorStmt.setMaxRows(50000000 + 10);
                fail("Should not be able to set max rows > 50000000");
            } catch (SQLException sqlEx) { /* ignore */
            }

            try {
                accessorStmt.setMaxRows(Integer.MIN_VALUE);
                fail("Should not be able to set max rows < 0");
            } catch (SQLException sqlEx) { /* ignore */
            }
            
            int fetchSize = stmt.getFetchSize();
            
            try {
            	accessorStmt.setFetchSize(Integer.MAX_VALUE);
            	fail("Should not be able to set FetchSize > max rows");
            } catch (SQLException sqlEx) { /* ignore */ }
            
            try {
            	accessorStmt.setFetchSize(-2);
            	fail("Should not be able to set FetchSize < 0");
            } catch (SQLException sqlEx) { /* ignore */ }
            
            assertTrue("Fetch size before invalid setFetchSize() calls should match fetch size now",
            	fetchSize == stmt.getFetchSize());
            
            
        } finally {

            if (accessorStmt != null) {

                try {
                    accessorStmt.close();
                } catch (SQLException sqlEx) { /* ignore */
                }

                accessorStmt = null;
            }
        }
    }

    /**
     * DOCUMENT ME!
     * 
     * @throws SQLException DOCUMENT ME!
     */
    public void testAutoIncrement()
                           throws SQLException {
        try
        {
        	stmt = conn.createStatement(java.sql.ResultSet.TYPE_FORWARD_ONLY, java.sql.ResultSet.CONCUR_READ_ONLY);
        	stmt.setFetchSize(Integer.MIN_VALUE);
        	
        stmt.executeUpdate(
                "INSERT INTO statement_test (strdata1) values ('blah')");

        int autoIncKeyFromApi = -1;
        rs = stmt.getGeneratedKeys();

        if (rs.next()) {
            autoIncKeyFromApi = rs.getInt(1);
        } else {
            fail("Failed to retrieve AUTO_INCREMENT using Statement.getGeneratedKeys()");
        }
        
        rs.close();

        int autoIncKeyFromFunc = -1;
        rs = stmt.executeQuery("SELECT LAST_INSERT_ID()");

        if (rs.next()) {
            autoIncKeyFromFunc = rs.getInt(1);
        } else {
            fail("Failed to retrieve AUTO_INCREMENT using LAST_INSERT_ID()");
        }

        if (autoIncKeyFromApi != -1 && autoIncKeyFromFunc != -1) {
            assertTrue("Key retrieved from API (" + autoIncKeyFromApi + 
                       ") does not match key retrieved from LAST_INSERT_ID() " + 
                       autoIncKeyFromFunc + ") function", 
                       autoIncKeyFromApi == autoIncKeyFromFunc);
        } else {
            fail("AutoIncrement keys were '0'");
        }
        }
        finally {
        	if (rs != null) {
        		try {
        			rs.close();
        		}
        		catch (Exception ex) { /* ignore */ }
        	}
        	
        	rs = null;
        }
    }

    /**
     * DOCUMENT ME!
     * 
     * @throws SQLException DOCUMENT ME!
     */
    public void testClose()
                   throws SQLException {

        Statement closeStmt            = null;
        boolean   exceptionAfterClosed = false;

        try {
            closeStmt = conn.createStatement();
            closeStmt.close();

            try {
                closeStmt.executeQuery("SELECT 1");
            } catch (SQLException sqlEx) {
                exceptionAfterClosed = true;
            }
        } finally {

            if (closeStmt != null) {

                try {
                    closeStmt.close();
                } catch (SQLException sqlEx) {

                    /* ignore */
                }
            }

            closeStmt = null;
        }

        assertTrue("Operations not allowed on Statement after .close() is called!", 
                   exceptionAfterClosed);
    }

    /**
     * DOCUMENT ME!
     * 
     * @throws SQLException DOCUMENT ME!
     */
    public void testInsert()
                    throws SQLException {

		try 
		{
        boolean autoCommit = conn.getAutoCommit();

        // Test running a query for an update. It should fail.
        try {
            conn.setAutoCommit(false);
            stmt.executeUpdate("SELECT * FROM statement_test");
        } catch (SQLException sqlEx) {
            assertTrue("Exception thrown for unknown reason", 
                       sqlEx.getSQLState().equalsIgnoreCase("01S03"));
        } finally {
            conn.setAutoCommit(autoCommit);
        }

        // Test running a update for an query. It should fail.
        try {
            conn.setAutoCommit(false);
            stmt.executeQuery(
                    "UPDATE statement_test SET strdata1='blah' WHERE 1=0");
        } catch (SQLException sqlEx) {
            assertTrue("Exception thrown for unknown reason", 
                       sqlEx.getSQLState().equalsIgnoreCase("S1009"));
        } finally {
        	
            conn.setAutoCommit(autoCommit);
        }

        for (int i = 0; i < 10; i++) {

            int updateCount = stmt.executeUpdate(
                                      "INSERT INTO statement_test (strdata1,strdata2) values ('abcdefg', 'poi')");
            assertTrue("Update count must be '1', was '" + updateCount + 
                       "'", (updateCount == 1));
        }
         
        }
        finally {
        	if (rs != null) {
        		try {
        			rs.close();
        		}
        		catch (Exception ex) { /* ignore */ }
        	}
        	
        	rs = null;
        }
    }

    /**
     * DOCUMENT ME!
     * 
     * @throws SQLException DOCUMENT ME!
     */
    public void testPreparedStatement()
                               throws SQLException {
        stmt.executeUpdate(
                "INSERT INTO statement_test (id, strdata1,strdata2) values (999,'abcdefg', 'poi')");
        pstmt = conn.prepareStatement(
                        "UPDATE statement_test SET strdata1=?, strdata2=? where id=?");
        pstmt.setString(1, "iop");
        pstmt.setString(2, "higjklmn");
        pstmt.setInt(3, 999);

        int updateCount = pstmt.executeUpdate();
        assertTrue("Update count must be '1', was '" + updateCount + "'", 
                   (updateCount == 1));
    }

    /**
     * DOCUMENT ME!
     * 
     * @throws SQLException DOCUMENT ME!
     */
    public void testPreparedStatementBatch()
                                    throws SQLException {
        pstmt = conn.prepareStatement(
                        "INSERT INTO " + 
                        "statement_test (strdata1, strdata2) VALUES (?,?)");

        for (int i = 0; i < 10; i++) {
            pstmt.setString(1, "batch_" + i);
            pstmt.setString(2, "batch_" + i);
            pstmt.addBatch();
        }

        int[] updateCounts = pstmt.executeBatch();

        for (int i = 0; i < updateCounts.length; i++) {
            assertTrue("Update count must be '1', was '" + updateCounts[i] + 
                       "'", (updateCounts[i] == 1));
        }
    }

    /**
     * DOCUMENT ME!
     * 
     * @throws SQLException DOCUMENT ME!
     */
    public void testStubbed()
                     throws SQLException {
   
        try {
            stmt.getResultSetHoldability();
        } catch (NotImplemented notImplEx) { /* ignore */
        }
    }
}
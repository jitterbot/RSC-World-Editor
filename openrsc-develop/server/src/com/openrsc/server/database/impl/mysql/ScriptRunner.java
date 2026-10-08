package com.openrsc.server.database.impl.mysql;

  
                                                                             
                                                                            
                    
                                                                                                              
   

/*
 *  Copyright 2004 Clinton Begin
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

import java.io.*;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

   
                               
   
public class ScriptRunner {

    private static final String DEFAULT_DELIMITER = ";";
    private static final Pattern SOURCE_COMMAND = Pattern.compile("^\\s*SOURCE\\s+(.*?)\\s*$", Pattern.CASE_INSENSITIVE);

       
                                 
                                                                         
       
    public static final Pattern delimP = Pattern.compile("^\\s*(--)?\\s*delimiter\\s*=?\\s*([^\\s]+)+\\s*.*$", Pattern.CASE_INSENSITIVE);

    private final Connection connection;

    private final boolean stopOnError;
    private final boolean autoCommit;

    @SuppressWarnings("UseOfSystemOutOrSystemErr")
    private PrintWriter logWriter = null;
    @SuppressWarnings("UseOfSystemOutOrSystemErr")
    private PrintWriter errorLogWriter = null;

    private String delimiter = DEFAULT_DELIMITER;
    private boolean fullLineDelimiter = false;

    private String userDirectory = System.getProperty("user.dir");

       
                          
       
    public ScriptRunner(Connection connection, boolean autoCommit,
                        boolean stopOnError) {
        this.connection = connection;
        this.autoCommit = autoCommit;
        this.stopOnError = stopOnError;
        File logFile = new File("create_db.log");
        File errorLogFile = new File("create_db_error.log");
        try {
            if (logFile.exists()) {
                logWriter = new PrintWriter(new FileWriter(logFile, true));
            } else {
                logWriter = new PrintWriter(new FileWriter(logFile, false));
            }
        } catch(IOException e){
            System.err.println("Unable to access or create the db_create log");
        }
        try {
            if (errorLogFile.exists()) {
                errorLogWriter = new PrintWriter(new FileWriter(errorLogFile, true));
            } else {
                errorLogWriter = new PrintWriter(new FileWriter(errorLogFile, false));
            }
        } catch(IOException e){
            System.err.println("Unable to access or create the db_create error log");
        }
        String timeStamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date());
        println("\n-------\n" + timeStamp + "\n-------\n");
        printlnError("\n-------\n" + timeStamp + "\n-------\n");
    }

    public void setDelimiter(String delimiter, boolean fullLineDelimiter) {
        this.delimiter = delimiter;
        this.fullLineDelimiter = fullLineDelimiter;
    }

       
                                    
      
                                                                 
       
    public void setLogWriter(PrintWriter logWriter) {
        this.logWriter = logWriter;
    }

       
                                         
      
                                                                           
       
    public void setErrorLogWriter(PrintWriter errorLogWriter) {
        this.errorLogWriter = errorLogWriter;
    }

       
                                                                                    
       
    public void setUserDirectory(String userDirectory) {
        this.userDirectory = userDirectory;
    }

       
                                                              
      
                                                                                                 
       
    public void runScript(String filepath) throws IOException, SQLException {
        File file = new File(userDirectory, filepath);
        this.runScript(new BufferedReader(new FileReader(file)));
    }

       
                                                              
      
                                               
       
    public void runScript(Reader reader) throws IOException, SQLException {
        try {
            boolean originalAutoCommit = connection.getAutoCommit();
            try {
                if (originalAutoCommit != this.autoCommit) {
                    connection.setAutoCommit(this.autoCommit);
                }
                runScript(connection, reader);
            } finally {
                connection.setAutoCommit(originalAutoCommit);
            }
        } catch (IOException | SQLException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Error running script.  Cause: " + e, e);
        }
    }

       
                                                                        
                           
      
                                                         
                                               
                                                   
                                                                       
       
    private void runScript(Connection conn, Reader reader) throws IOException,
            SQLException {
        StringBuffer command = null;
        try {
            LineNumberReader lineReader = new LineNumberReader(reader);
            String line;
            while ((line = lineReader.readLine()) != null) {
                if (command == null) {
                    command = new StringBuffer();
                }
                String trimmedLine = line.trim();
                final Matcher delimMatch = delimP.matcher(trimmedLine);
                if (trimmedLine.length() < 1
                        || trimmedLine.startsWith("//")) {
                                 
                } else if (delimMatch.matches()) {
                    setDelimiter(delimMatch.group(2), false);
                } else if (trimmedLine.startsWith("--")) {
                    println(trimmedLine);
                } else if (trimmedLine.length() < 1
                        || trimmedLine.startsWith("--")) {
                                 
                } else if (!fullLineDelimiter
                        && trimmedLine.endsWith(getDelimiter())
                        || fullLineDelimiter
                        && trimmedLine.equals(getDelimiter())) {
                    command.append(line.substring(0, line
                            .lastIndexOf(getDelimiter())));
                    command.append(" ");
                    this.execCommand(conn, command, lineReader);
                    command = null;
                } else {
                    command.append(line);
                    command.append("\n");
                }
            }
            if (command != null) {
                this.execCommand(conn, command, lineReader);
            }
            if (!autoCommit) {
                conn.commit();
            }
        }
        catch (IOException e) {
            throw new IOException(String.format("Error executing '%s': %s", command, e.getMessage()), e);
        } finally {
            conn.rollback();
            flush();
        }
    }

    private void execCommand(Connection conn, StringBuffer command,
                             LineNumberReader lineReader) throws IOException, SQLException {

        if (command.length() == 0) {
            return;
        }

        Matcher sourceCommandMatcher = SOURCE_COMMAND.matcher(command);
        if (sourceCommandMatcher.matches()) {
            this.runScriptFile(conn, sourceCommandMatcher.group(1));
            return;
        }

        this.execSqlCommand(conn, command, lineReader);
    }

    private void runScriptFile(Connection conn, String filepath) throws IOException, SQLException {
        File file = new File(userDirectory, filepath);
        this.runScript(conn, new BufferedReader(new FileReader(file)));
    }

    private void execSqlCommand(Connection conn, StringBuffer command,
                                LineNumberReader lineReader) throws SQLException {

        Statement statement = conn.createStatement();

        println(command);

        boolean hasResults = false;
        try {
            hasResults = statement.execute(command.toString());
        } catch (SQLException e) {
            final String errText = String.format("Error executing '%s' (line %d): %s",
                    command, lineReader.getLineNumber(), e.getMessage());
            printlnError(errText);
            System.err.println(errText);
            if (stopOnError) {
                throw new SQLException(errText, e);
            }
        }

        if (autoCommit && !conn.getAutoCommit()) {
            conn.commit();
        }

        ResultSet rs = statement.getResultSet();
        if (hasResults && rs != null) {
            ResultSetMetaData md = rs.getMetaData();
            int cols = md.getColumnCount();
            for (int i = 1; i <= cols; i++) {
                String name = md.getColumnLabel(i);
                print(name + "\t");
            }
            println("");
            while (rs.next()) {
                for (int i = 1; i <= cols; i++) {
                    String value = rs.getString(i);
                    print(value + "\t");
                }
                println("");
            }
        }

        try {
            statement.close();
        } catch (Exception e) {
                                                         
        }
    }

    private String getDelimiter() {
        return delimiter;
    }

    @SuppressWarnings("UseOfSystemOutOrSystemErr")

    private void print(Object o) {
        if (logWriter != null) {
            logWriter.print(o);
        }
    }

    private void println(Object o) {
        if (logWriter != null) {
            logWriter.println(o);
        }
    }

    private void printlnError(Object o) {
        if (errorLogWriter != null) {
            errorLogWriter.println(o);
        }
    }

    private void flush() {
        if (logWriter != null) {
            logWriter.flush();
        }
        if (errorLogWriter != null) {
            errorLogWriter.flush();
        }
    }
}
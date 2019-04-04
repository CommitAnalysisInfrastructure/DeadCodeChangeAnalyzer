/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements. See the NOTICE
 * file distributed with this work for additional information regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package net.ssehub.comani.analysis.deadcodechange.diff;

/**
 * This abstract class represents a general file diff. It implements common attributes and methods for the following
 * sub-classes:<br>
 * <ul>
 * <li>{@link VariabilityModelFileDiff}</li>
 * <li>{@link BuildFileDiff}</li>
 * <li>{@link CodeFileDiff}</li>
 * </ul>
 * 
 * @author Christian Kroeher
 *
 */
public abstract class FileDiff {
    
    /**
     * Enumeration for defining the type of file diff for a specific object. This eases the identification of the type
     * of file diff during the identification of relevant changes of a given commit.
     * 
     * @author Christian Kroeher
     *
     */
    public enum FileType { CODE, BUILD, VARIABILITY_MODEL, OTHER };
    
    /**
     * String identifying an added line in a diff text, if found at the start of the line.
     */
    protected static final String LINE_ADDED_MARKER = "+";
    
    /**
     * String identifying a deleted line in a diff text, if found at the start of the line.
     */
    protected static final String LINE_DELETED_MARKER = "-";
    
    /**
     * The {@link FileType} of this file diff.
     */
    protected FileType fileType;
    
    /**
     * The full, line-wise diff description of a commit.
     */
    protected String[] diffLines;
    
    /**
     * Definition of whether the {@link CodeFileDiff} should consider all preprocessor blocks (<code>true</code>) or
     * only those blocks with references to configuration options (<code>false</code>).
     */
    protected boolean considerAllBlocks;
    
    /**
     * The result of the analysis in terms of whether the given commit changes relevant information (<code>true</code>)
     * or not (<code>false</code>).
     */
    protected boolean changeRelevant;
    
    /**
     * Construct a new {@link FileDiff}.<br><br>
     * 
     * This constructor will start a line-wise analysis of the given diff lines calling the abstract method 
     * {@link #isRelevantChange(String, int)}. This method is implemented in the specific file diff classes to detect
     * changes to the variability information available in the specific type of file diff.
     * 
     * @param fileType the {@link FileType} of this file diff
     * @param diffLines the full, line-wise diff description of a commit
     */
    protected FileDiff(FileType fileType, String[] diffLines) {
        this.fileType = fileType;
        this.diffLines = diffLines;
        analyzeDiff();
    }
    
    /**
     * Construct a new {@link FileDiff}.<br><br>
     * 
     * This constructor will start a line-wise analysis of the given diff lines calling the abstract method 
     * {@link #isRelevantChange(String, int)}. This method is implemented in the specific file diff classes to detect
     * changes to the variability information available in the specific type of file diff.<br><br>
     * 
     * This constructor is only used for {@link CodeFileDiff}s to set the {@link #considerAllBlocks} attribute before
     * the analysis is startet.
     * 
     * @param fileType the {@link FileType} of this file diff
     * @param diffLines the full, line-wise diff description of a commit
     * @param considerAllBlocks <code>true</code> if the diff analysis should consider all preprocessor blocks or
     *        <code>false</code> if it should only consider blocks with references to configuration options
     */
    protected FileDiff(FileType fileType, String[] diffLines, boolean considerAllBlocks) {
        this.fileType = fileType;
        this.diffLines = diffLines;
        this.considerAllBlocks = considerAllBlocks;
        analyzeDiff();
    }
    
    /**
     * Analyze the given {@link #diffLines} for relevant changes that force a new dead code analysis.
     */
    private void analyzeDiff() {
        String diffLine = null;
        String diffLineNoMarker = null;
        int lineCounter = 0;
        while (!changeRelevant && lineCounter < diffLines.length) {
            diffLine = diffLines[lineCounter];
            if (diffLine.startsWith(LINE_ADDED_MARKER) || diffLine.startsWith(LINE_DELETED_MARKER)) {
                // Up-front check if the changed line is an empty line
                diffLineNoMarker = diffLine.substring(1, diffLine.length());
                if (!diffLineNoMarker.trim().isEmpty()) {
                    // Now, actually delete the marker and additional comments from that line
                    diffLine = normalize(diffLine, lineCounter);
                    if (!diffLine.trim().isEmpty()) {
                        changeRelevant = isRelevantChange(diffLine, lineCounter);
                    }
                }
            }
            lineCounter++;
        }
    }
    
    /**
     * Normalize the given diff line, e.g. remove leading "+" or "-", check if the line is a general comment or
     * contains general comments as part of the line.
     * 
     * @param diffLine the line of a diff description to be normalized
     * @param diffLinePosition the index of the given diff line in {@link #diffLines} used, e.g., for backtracking
     * @return the normalized diff line without leading "+" or "-"; in case of comments, only the non-comment part
     * will be return, which may lead to an empty string
     */
    protected abstract String normalize(String diffLine, int diffLinePosition);
    
    /**
     * Check if the given diff line (without leading "+" or "-") describes a change to variability information, which
     * forces another dead code analysis.
     *  
     * @param cleanDiffLine the line of a diff description to be checked for variability information without leading "+"
     *        or "-"
     * @param cleanDiffLinePosition the index of the given diff line in {@link #diffLines} used, e.g., for backtracking 
     * @return <code>true</code> if the given diff line contains variability information, <code>false</code> otherwise
     */
    protected abstract boolean isRelevantChange(String cleanDiffLine, int cleanDiffLinePosition);
    
    /**
     * Return the {@link FileType} of this file diff.
     * 
     * @return the {@link FileType} of this file diff
     */
    public FileType getFileType() {
        return this.fileType;
    }
    
    /**
     * Returns the result of this file diff.
     * 
     * @return <code>true</code> if the given commit changes relevant information; <code>false</code> otherwise
     */
    public boolean getResult() {
        return this.changeRelevant;
    }
    
    /**
     * Return the full, line-wise diff description analyzed by this file diff.
     * 
     * @return the full, line-wise diff description analyzed by this file diff
     */
    public String[] getDiffText() {
        return this.diffLines;
    }

}

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

import java.util.regex.Pattern;

/**
 * This class implements analysis methods and required attributes for identifying relevant changes to code artifacts.
 * 
 * @author Christian Kroeher
 *
 */
public class CodeFileDiff extends FileDiff {
    
    /**
     * String identifying the start of a single line comment in a code file.<br><br>
     * 
     * Value: {@value #CODE_SL_COMMENT_MARKER};
     */
    private static final String CODE_SL_COMMENT_MARKER = "//";
    
    /**
     * String identifying the start of a multi line comment in a code file.<br><br>
     * 
     * Value: {@value #CODE_ML_COMMENT_START_MARKER};
     */
    private static final String CODE_ML_COMMENT_START_MARKER = "/*";
    
    /**
     * Regex identifying the start of a multi line comment in a code file.<br><br>
     * 
     * Value: {@value #CODE_ML_COMMENT_START_PATTERN};
     */
    private static final String CODE_ML_COMMENT_START_PATTERN = "/\\*";
    
    /**
     * String identifying the end of a multi line comment in a code file.<br><br>
     * 
     * Value: {@value #CODE_ML_COMMENT_END_MARKER};
     */
    private static final String CODE_ML_COMMENT_END_MARKER = "*/";
    
    /**
     * Regex identifying the end of a multi line comment in a code file.<br><br>
     * 
     * Value: {@value #CODE_ML_COMMENT_END_PATTERN};
     */
    private static final String CODE_ML_COMMENT_END_PATTERN = "\\*/";
    
    /**
     * Regex identifying lines that contain a reference to a configuration option "CONFIG_" in a code file.<br><br>
     * 
     * Value: {@value #CODE_VAR_PATTERN};
     */
    private static final String CODE_VAR_PATTERN = ".*(\\s+|\\(|\\[|\\{|\\<|\\)|\\]|\\}|\\>)\\!?CONFIG_.*";
    
    /**
     * Regex identifying a preprocessor "ifdef"-block.<br><br>
     * 
     * Value: {@value #CODE_IFDEF_PATTERN};
     */
    private static final String CODE_IFDEF_PATTERN = "#\\s*(ifdef|ifndef)";
    
    /**
     * Regex identifying lines that contain "#ifdef" or "#ifndef" statements in a code file and refer to a configuration
     * option.<br><br>
     * 
     * Value: {@value #CODE_VAR_IFDEF_PATTERN};
     */
    private static final String CODE_VAR_IFDEF_PATTERN = CODE_IFDEF_PATTERN + CODE_VAR_PATTERN;
    
    /**
     * Regex identifying lines that contain help methods (macros) in a code file.<br><br>
     * 
     * Value: {@value #CODE_VAR_HELP_METHODS_PATTERN};
     */
    private static final String CODE_VAR_HELP_METHODS_PATTERN = "\\!?(defined|IS_BUILTIN|"
            + "IS_MODULE|IS_REACHABLE|IS_ENABLED)\\(";
    
    /**
     * Regex identifying lines that contain conditions with help methods (macros) in a code file and refer to a
     * configuration option.<br><br>
     * 
     * Value: {@value #CODE_VAR_IF_CONDITION_PATTERN};
     */
    private static final String CODE_VAR_IF_CONDITION_PATTERN = "\\!?.*(" + CODE_VAR_HELP_METHODS_PATTERN + ")?"
            + CODE_VAR_PATTERN;
    
    /**
     * Regex identifying a preprocessor "if"- or "elif"-block.<br><br>
     * 
     * Value: {@value #CODE_IF_PATTERN};
     */
    private static final String CODE_IF_PATTERN = "#\\s*(if|elif).*";
    
    /**
     * Regex identifying lines that contain "#if" or "#elif" statements in a code file and refer to help methods
     * (macros), which in turn refer to a configuration option.<br><br>
     * 
     * Value: {@value #CODE_VAR_IF_PATTERN};
     */
    private static final String CODE_VAR_IF_PATTERN = CODE_IF_PATTERN + CODE_VAR_IF_CONDITION_PATTERN;
    
    /**
     * Regex identifying lines that contain variability information in a code file and indicate the start of a
     * conditional block.<br><br>
     * 
     * Value: {@value #CODE_VAR_IF_START_PATTERN};
     */
    private static final String CODE_VAR_IF_START_PATTERN = ".*((" + CODE_VAR_IFDEF_PATTERN + ")"
            + "|(" + CODE_VAR_IF_PATTERN + "))";
    
    /**
     * Regex identifying a preprocessor "endif".<br><br>
     * 
     * Value: {@value #CODE_END_IF_PATTERN};
     */
    private static final String CODE_END_IF_PATTERN = ".*#\\s*endif.*";
    
    /**
     * Regex identifying a preprocessor "else".<br><br>
     * 
     * Value: {@value #CODE_ELSE_PATTERN};
     */
    private static final String CODE_ELSE_PATTERN = ".*#\\s*else.*";
    
    /**
     * Regex identifying lines that indicate the end of a conditional block in a code file. If these lines can be
     * interpreted as variability related depends on the corresponding start of the block (the condition).<br><br>
     * 
     * Value: {@value #CODE_IF_END_PATTERN};
     */
    private static final String CODE_IF_END_PATTERN = ".*#\\s*(else|endif).*";
    
    /**
     * Construct a new {@link CodeFileDiff}.<br><br>
     * 
     * This constructor will call the super constructor of {@link FileDiff}, which will start a line-wise analysis of
     * the given diff lines calling the inherited methods {@link #normalize(String, int)} and
     * {@link #isRelevantChange(String, int)} defined in this class.
     * 
     * @param diffLines the lines of a source code diff
     * @param considerAllBlocks <code>true</code> if the diff analysis should consider all preprocessor blocks or
     *        <code>false</code> if it should only consider blocks with references to configuration options
     */
    public CodeFileDiff(String[] diffLines, boolean considerAllBlocks) {
        super(FileType.CODE, diffLines, considerAllBlocks);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected String normalize(String diffLine, int diffLinePosition) {
        // 1. Remove "+" or "-"
        String normalizedDiffLine = diffLine;
        /*
         * This method is also used by backtrackCondition-method and
         * may receive unchanged diff lines (no leading "+" or "-") from
         * there. Thus, check before removing the first character although
         * this is not needed if called from parent-class. 
         */
        if (diffLine.startsWith(LINE_ADDED_MARKER) || diffLine.startsWith(LINE_DELETED_MARKER)) {
            normalizedDiffLine = diffLine.substring(1, diffLine.length());
        }
        // 2. Split around comment-token
        if (normalizedDiffLine.contains(CODE_SL_COMMENT_MARKER)) {
            // Single line comment included, thus only use the part before the comment.
            String[] normalizedDiffLineParts = normalizedDiffLine.split(CODE_SL_COMMENT_MARKER);
            if (normalizedDiffLineParts.length > 0) {
                normalizedDiffLine = normalizedDiffLineParts[0];
            } else {
                normalizedDiffLine = "";
            }
        } else if (normalizedDiffLine.contains(CODE_ML_COMMENT_START_MARKER)) {
            if (normalizedDiffLine.contains(CODE_ML_COMMENT_END_MARKER)) {
                /*
                 * Multi line comment start and end in single line, thus the part before the start and the part after
                 * the end of the comment should be used for further analysis.
                 */
                String beforeCommentStart = "";
                String afterCommentEnd = "";
                String[] normalizedDiffLineParts = normalizedDiffLine.split(CODE_ML_COMMENT_START_PATTERN);
                if (normalizedDiffLineParts.length > 0) {
                    beforeCommentStart = normalizedDiffLineParts[0];
                }
                normalizedDiffLineParts = normalizedDiffLine.split(CODE_ML_COMMENT_END_PATTERN);
                if (normalizedDiffLineParts.length > 1) {
                    afterCommentEnd = normalizedDiffLineParts[1];
                }
                normalizedDiffLine = beforeCommentStart + " " + afterCommentEnd;
            } else {
                // Multi line comment start only in this line, thus only use the part before the start.
                String[] normalizedDiffLineParts = normalizedDiffLine.split(CODE_ML_COMMENT_START_PATTERN);
                if (normalizedDiffLineParts.length > 0) {
                    normalizedDiffLine = normalizedDiffLineParts[0];
                } else {
                    normalizedDiffLine = "";
                }
            }
        } else if (normalizedDiffLine.contains(CODE_ML_COMMENT_END_MARKER)) {
            // Multi line comment end included, thus only use the part after the end of the comment (if available)
            String[] normalizedDiffLineParts = normalizedDiffLine.split(CODE_ML_COMMENT_END_PATTERN);
            if (normalizedDiffLineParts.length > 1) {
                normalizedDiffLine = normalizedDiffLineParts[1];
            } else {
                normalizedDiffLine = "";
            }
        }
        // 3. Check if is part of comment
        if (!normalizedDiffLine.trim().isEmpty() && isPartOfComment(diffLinePosition)) {
            normalizedDiffLine = "";
        }
        // 4. Return part of the diff line without comments
        return normalizedDiffLine;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean isRelevantChange(String cleanDiffLine, int cleanDiffLinePosition) {
        /*
         * Clean diff line means, that there is no leading "+" or "-" anymore and
         * comments that were part of this line are removed. In case of a multi line
         * comment, the inner part if this comment needs additional checks here.
         * See isPartOfComment-method.
         */
        boolean isRelevantChange = false;
        if (!isPartOfComment(cleanDiffLinePosition)) {
            int startIndex = indexOfContinuationStart(cleanDiffLinePosition);
            String startDiffLine = diffLines[startIndex];
            if (considerAllBlocks) {
                // Consider all preprocessor blocks
                isRelevantChange = isBlock(startDiffLine);
            } else {
                // Consider only those preprocessor blocks that reference a configuration option CONFIG_*
                isRelevantChange = checkForConfigBlocks(startDiffLine, startIndex);
            }
        }
        return isRelevantChange;
    }
    
    /**
     * Checks the line before the given position for continuation and, if this is found, determines the index of the 
     * start of the statement the continuation belongs to. This is the first line with a continuation.
     *  
     * @param cleanDiffLinePosition the index of a particular diff line
     * @return either the given index above, if no continuation was found, or the index of the line containing the start
     *         of the statement the continuation belongs to
     */
    private int indexOfContinuationStart(int cleanDiffLinePosition) {
        int startIndex = cleanDiffLinePosition;
        int diffLineCounter = cleanDiffLinePosition - 1;
        while (diffLineCounter >= 0 && diffLines[diffLineCounter].trim().endsWith("\\")) {
            startIndex = diffLineCounter;
            diffLineCounter--;
        }
        return startIndex;
    }
    
    /**
     * Checks the given line for containing preprocessor statements defining a code block. This is the case, if the line
     * matches one of the following patterns:
     * <ul>
     * <li>{@link #CODE_IFDEF_PATTERN}</li>
     * <li>{@link #CODE_IF_PATTERN}</li>
     * <li>{@link #CODE_IF_END_PATTERN}</li>
     * </ul>
     * @param cleanDiffLine the diff line to check
     * @return <code>true</code> if the given line matches one of the patterns above; <code>false</code> otherwise
     */
    private boolean isBlock(String cleanDiffLine) {
        boolean isRelevantChange = false;
        /*
         * Simply check, if line contains block information: if it does, return true without further checks.
         * No backtracking, etc. necessary here as we check for all blocks.
         */
        if (Pattern.matches(CODE_IFDEF_PATTERN, cleanDiffLine) 
                || Pattern.matches(CODE_IF_PATTERN, cleanDiffLine) 
                || Pattern.matches(CODE_IF_END_PATTERN, cleanDiffLine)) {
            isRelevantChange = true;
        }
        return isRelevantChange;
    }
    
    /**
     * Checks the given line for containing preprocessor statements defining a code block, which references a 
     * configuration option in its condition or contains nested blocks with such references.
     * 
     * @param cleanDiffLine the diff line to check
     * @param cleanDiffLinePosition the index of the given diff line in the set of all diff lines
     * @return <code>true</code> if the given line contains preprocessor statements defining a code block, which
     *         references a configuration option in its condition or contains nested blocks with such references;
     *         <code>true</code> otherwise
     */
    private boolean checkForConfigBlocks(String cleanDiffLine, int cleanDiffLinePosition) {
        boolean isRelevantChange = false;
        if (isConfigBlock(cleanDiffLine, cleanDiffLinePosition)) {
            // A block referencing a configuration option (CONFIG_*) is changed directly
            isRelevantChange = true;
        } else if (isBlock(cleanDiffLine)) {
            /*
             * A general block is changed: check if it contains other blocks referencing a configuration option
             * (CONFIG_*), which are affected by that change indirectly.
             * 
             * For that, simply search for CONFIG_* and, if found, check if it is part of an #if* or #elif condition
             */
            isRelevantChange = containsConfigBlocks(cleanDiffLine, cleanDiffLinePosition,
                    Pattern.matches(CODE_END_IF_PATTERN, cleanDiffLine));
        }
        return isRelevantChange;
    }
    
    /**
     * Checks the lines between the given diff line and its counter part (either opening or closing preprocessor block
     * statement) for other blocks referencing a configuration option.
     * 
     * @param cleanDiffLine the diff line constituting the opening or closing preprocessor statement in which nested
     *        blocks should be analyzed
     * @param cleanDiffLinePosition the index of the given diff line in the set of all diff lines
     * @param backwardSearch <code>true</code> if the given diff line contains a closing statement and this method must
     *        search in all previous lines, or <code>false</code> if the given diff line contains a opening statement
     *        such that this method must search all following lines
     * @return <code>true</code> if the block represented by the given line contains nested blocks referencing a
     *         configuration option; <code>true</code> otherwise
     */
    private boolean containsConfigBlocks(String cleanDiffLine, int cleanDiffLinePosition, boolean backwardSearch) {
        boolean containsConfigBlocks = false;
        boolean counterpartFound = false;
        int nestedBlocksCounter = 0;
        String diffLine;
        if (backwardSearch) {
            // Given diff line is #endif: search all previous diff lines until the corresponding #if*, #elif, or #else
            int diffLineCounter = cleanDiffLinePosition - 1;
            while (!containsConfigBlocks && !counterpartFound && diffLineCounter >= 0) {
                diffLine = normalize(diffLines[diffLineCounter], diffLineCounter);
                if (Pattern.matches(CODE_END_IF_PATTERN, cleanDiffLine)) {
                    nestedBlocksCounter++;
                } else if (Pattern.matches(CODE_IFDEF_PATTERN, diffLine) || Pattern.matches(CODE_IF_PATTERN, diffLine)
                        || Pattern.matches(CODE_ELSE_PATTERN, diffLine)) {
                    if (nestedBlocksCounter == 0) {
                        counterpartFound = true;
                        // If the statement contains a reference to a configuration option, we are done
                        containsConfigBlocks = Pattern.matches(CODE_VAR_PATTERN, diffLine);
                    } else {
                        nestedBlocksCounter--;
                    }
                } else {
                    // The line contains neither a closing nor an opening preprocessor statement...
                    if (Pattern.matches(CODE_VAR_PATTERN, diffLine)) {
                        // ...but a reference to a configuration option: check if this line is part of a continuation
                        int indexOfContinuationStart = indexOfContinuationStart(diffLineCounter);
                        if (indexOfContinuationStart != diffLineCounter 
                                && (Pattern.matches(CODE_IFDEF_PATTERN, diffLines[indexOfContinuationStart])
                                || Pattern.matches(CODE_IF_PATTERN, diffLines[indexOfContinuationStart])
                                || Pattern.matches(CODE_ELSE_PATTERN, diffLines[indexOfContinuationStart]))) {
                            containsConfigBlocks = true;
                        }
                    }
                }
                diffLineCounter--;
            }
        } else {
            // Given diff line is #if*, #elif, or #else: search all following diff lines until the corresponding #endif
            int diffLineCounter = cleanDiffLinePosition + 1;
            while (!containsConfigBlocks && !counterpartFound && diffLineCounter < diffLines.length) {
                diffLine = normalize(diffLines[diffLineCounter], diffLineCounter);
                if (Pattern.matches(CODE_IFDEF_PATTERN, diffLine) || Pattern.matches(CODE_IF_PATTERN, diffLine)
                        || Pattern.matches(CODE_ELSE_PATTERN, diffLine)) {
                    nestedBlocksCounter++;
                    // If the statement contains a reference to a configuration option, we are done
                    containsConfigBlocks = Pattern.matches(CODE_VAR_PATTERN, diffLine);
                } else if (Pattern.matches(CODE_END_IF_PATTERN, cleanDiffLine)) {
                    if (nestedBlocksCounter == 0) {
                        counterpartFound = true;
                    } else {
                        nestedBlocksCounter--;
                    }
                } else {
                    // The line contains neither a closing nor an opening preprocessor statement...
                    if (Pattern.matches(CODE_VAR_PATTERN, diffLine)) {
                        // ...but a reference to a configuration option: check if this line is part of a continuation
                        int indexOfContinuationStart = indexOfContinuationStart(diffLineCounter);
                        if (indexOfContinuationStart != diffLineCounter 
                                && (Pattern.matches(CODE_IFDEF_PATTERN, diffLines[indexOfContinuationStart])
                                || Pattern.matches(CODE_IF_PATTERN, diffLines[indexOfContinuationStart])
                                || Pattern.matches(CODE_ELSE_PATTERN, diffLines[indexOfContinuationStart]))) {
                            containsConfigBlocks = true;
                        }
                    }
                }
                diffLineCounter++;
            }
        }
        return containsConfigBlocks;
    }
    
    /**
     * Checks the given line for containing preprocessor statements, which define a block referencing a configuration
     * option in its condition.
     * 
     * @param cleanDiffLine cleanDiffLine the diff line to check
     * @param cleanDiffLinePosition the index of the given diff line in the set of all diff lines
     * @return <code>true</code> if the block represented by the given line references a configuration option in its
     *         condition; <code>true</code> otherwise
     */
    private boolean isConfigBlock(String cleanDiffLine, int cleanDiffLinePosition) {
        boolean isConfigBlock = false;
        if (Pattern.matches(CODE_VAR_IFDEF_PATTERN, cleanDiffLine) 
                || Pattern.matches(CODE_VAR_IF_PATTERN, cleanDiffLine)) {
            // Found #if* with reference to configuration option in the same line
            isConfigBlock = true;
        } else if (cleanDiffLine.trim().endsWith("\\")) {
            // No reference to configuration options found, but continuation detected: check all continuing lines
            boolean isContinuing = true;
            int diffLineCounter = cleanDiffLinePosition + 1;
            String continuingLine;
            while (!isConfigBlock && isContinuing && diffLineCounter < diffLines.length) {
                continuingLine = normalize(diffLines[diffLineCounter], diffLineCounter);
                if (Pattern.matches(CODE_VAR_PATTERN, continuingLine)) {
                    isConfigBlock = true;
                } else if (!continuingLine.trim().endsWith("\\")) {
                    isContinuing = false;
                }
                diffLineCounter++;
            }
        } else if (Pattern.matches(CODE_IF_END_PATTERN, cleanDiffLine)
                && backtrackPreprocessorCondition(cleanDiffLinePosition)) {
            // Found #else or #endif and the related condition references a configuration option
            isConfigBlock = true;
        }
        return isConfigBlock;
    }
    
    /**
     * Check if the diff line at the given position is part of a multi line comment.
     * 
     * @param diffLinePosition the index of the diff line which should be checked for being
     * part of a multi line comment
     * @return <code>true</code> if the diff line at the given index is part of a multi line
     * comment, <code>false</code> otherwise
     */
    private boolean isPartOfComment(int diffLinePosition) {
        boolean isPartOfComment = false;
        int diffLineCounter = diffLinePosition - 1;
        boolean commentMarkerFound = false;
        String previousDiffLine = null;
        while (diffLineCounter >= 0 && !commentMarkerFound) {
            previousDiffLine = diffLines[diffLineCounter];
            if (previousDiffLine.contains(CODE_ML_COMMENT_END_MARKER)) {
                /*
                 * Closing multi line found, thus the diff line at the given position
                 * cannot be part of a multi line comment.
                 */
                commentMarkerFound = true;
            } else if (previousDiffLine.contains(CODE_ML_COMMENT_START_MARKER)) {
                /*
                 * Opening multi line found, thus the diff line at the given position
                 * must be part of a multi line comment. 
                 */
                commentMarkerFound = true;
                isPartOfComment = true;
            }
            diffLineCounter--;
        }
        return isPartOfComment;
    }
    
    /**
     * Find the condition for the "#endif" or "#else" statement at the given index of the diff
     * lines and return <code>true</code> if this condition is variability related, which should
     * lead to an increment of the respective variability lines counter (added or deleted).
     *  
     * @param blockEndIndex the index of the diff line where the "#endif" or "#else" statement was found
     * @return <code>true</code> if the found condition is variability related, <code>false</code> otherwise
     */
    private boolean backtrackPreprocessorCondition(int blockEndIndex) {
        /*
         * Determine the change type of the block end ('+' or '-') and the inverted type for checking
         * whether possible previous block end statements are part of a nested block or if the given block end
         * substitutes the previous block end statement, e.g. like in this case:
         *
         * #if !CONFIG_X
         *     ...
         * #else
         *     ...
         * -#endif
         *     ...
         * +#endif
         */
        String blockEndLine = diffLines[blockEndIndex];
        char blockEndChangeType = blockEndLine.charAt(0);
        char invertedblockEndChangeType = invertChangeType(blockEndChangeType);
        boolean conditionIsVariabilityRelated = false;
        boolean conditionFound = false;
        int nestedEndifCounter = 0; // Counts the nested #endif-statements
        int diffLinesCounter = blockEndIndex - 1;
        String diffLine = null;
        while (!conditionFound && diffLinesCounter >= 0) {
            diffLine = diffLines[diffLinesCounter]; // normalize(diffLines[diffLinesCounter])
            if (nestedEndifCounter == 0 && !Pattern.matches(CODE_IF_END_PATTERN, diffLine)
                    && Pattern.matches(".*#if.*", diffLine)) {
                // No nested blocks and not an #endif or #else and line indicates block start
                conditionFound = true;
                if (Pattern.matches(CODE_VAR_IF_START_PATTERN, diffLine)) {
                    // Current diff line contains variability information
                    conditionIsVariabilityRelated = true;
                } else if (diffLine.trim().endsWith("\\")) {
                    /*
                     * Current diffLine contains #if-statement but does not include a CONFIG_ symbol.
                     * If this line ends with continuation ("\"), we have to check the following lines until
                     * there is no continuation anymore for CONFIG_ symbols.
                     */
                    int blockLinesCounter = diffLinesCounter + 1;
                    String blockLine = "";
                    do {
                        blockLine = normalize(diffLines[blockLinesCounter], blockLinesCounter);
                        if (Pattern.matches(CODE_VAR_PATTERN, blockLine)) {
                            conditionIsVariabilityRelated = true;
                        }
                        blockLinesCounter++;
                    } while (blockLinesCounter < blockEndIndex && blockLine.trim().endsWith("\\"));
                }
            } else {
                if (!diffLine.isEmpty() && diffLine.charAt(0) != invertedblockEndChangeType
                        && Pattern.matches(".*#endif.*", diffLine)) {
                    // Nested block end found
                    nestedEndifCounter++;
                } else if (nestedEndifCounter > 0 && Pattern.matches(".*#if.*", diffLine)) {
                    // Nested block start found
                    nestedEndifCounter--;
                }
            }
            diffLinesCounter--;
        }
        return conditionIsVariabilityRelated;
    }
    
    /**
     * Invert the given change type ('+' or '-').
     * 
     * @param changeType the character defining the change type ('+' or '-')
     * @return if the given change type is '+' then '-'; '+' in all other cases
     */
    private char invertChangeType(char changeType) {
        char invertedChangeType = '+';
        if (changeType == '+') {
            invertedChangeType = '-';
        }
        return invertedChangeType;
    }
    
}

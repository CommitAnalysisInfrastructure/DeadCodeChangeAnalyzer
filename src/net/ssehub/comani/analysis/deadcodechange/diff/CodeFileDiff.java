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

import net.ssehub.comani.core.Logger;
import net.ssehub.comani.core.Logger.MessageType;

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
     * String identifying the end of a multi line comment in a code file.<br><br>
     * 
     * Value: {@value #CODE_ML_COMMENT_END_MARKER};
     */
    private static final String CODE_ML_COMMENT_END_MARKER = "*/";
    
    /**
     * Regex identifying the start of a multi line comment in a code file.<br><br>
     * 
     * Value: {@value #CODE_ML_COMMENT_START_PATTERN};
     */
    private static final String CODE_ML_COMMENT_START_PATTERN = "/\\*";
    
    /**
     * Regex identifying the end of a multi line comment in a code file.<br><br>
     * 
     * Value: {@value #CODE_ML_COMMENT_END_PATTERN};
     */
    private static final String CODE_ML_COMMENT_END_PATTERN = "\\*/";
    
    /**
     * Regex identifying the start of a C-preprocessor condition. This pattern summarizes the following variants:
     * <ul>
     * <li><code>#if ...</code></li>
     * <li><code>#ifdef ...</code></li>
     * <li><code>#ifndef ...</code></li>
     * </ul>
     * 
     * Value: {@value #CPP_IF_PATTERN};
     */
    private static final Pattern CPP_IF_PATTERN = Pattern.compile(".*#\\s*if.*");
    
    /**
     * Regex identifying the start of a C-preprocessor alternative for a related condition.<br><br>
     * 
     * Value: {@value #CPP_ELSE_PATTERN};
     */
    private static final Pattern CPP_ELSE_PATTERN = Pattern.compile(".*#\\s*else.*");
    
    /**
     * Regex identifying the start of a C-preprocessor conditional alternative for a related condition.<br><br>
     * 
     * Value: {@value #CPP_ELIF_PATTERN};
     */
    private static final Pattern CPP_ELIF_PATTERN = Pattern.compile(".*#\\s*elif.*");
    
    /**
     * Regex identifying the end of a C-preprocessor condition.<br><br>
     * 
     * Value: {@value #CPP_ENDIF_PATTERN};
     */
    private static final Pattern CPP_ENDIF_PATTERN = Pattern.compile(".*#\\s*endif.*");
    
    /**
     * Construct a new {@link CodeFileDiff}.<br><br>
     * 
     * This constructor will call the super constructor of {@link FileDiff}, which will start a line-wise analysis of
     * the given diff lines calling the inherited methods {@link #normalize(String, int)} and
     * {@link #isRelevantChange(String, int)} defined in this class.
     * 
     * @param diffLines the lines of a source code diff
     */
    public CodeFileDiff(String[] diffLines) {
        super(FileType.CODE, diffLines);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected String normalize(String diffLine, int diffLinePosition) {
        // 1. Remove "+" or "-", if they exist
        String normalizedDiffLine = diffLine;
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
         * Clean diff line means, that there is no leading "+" or "-" anymore and comments that were part of this line
         * are removed. In case of a multi line comment, the inner part if this comment needs additional checks here.
         * See isPartOfComment-method.
         */
        boolean isRelevantChange = false;
        if (!isPartOfComment(cleanDiffLinePosition)) {
            if (isBlockRelatedChange(cleanDiffLine, cleanDiffLinePosition)) {
                isRelevantChange = true;
                Logger.getInstance().log("CodeFileDiff", "Relevant change detected", cleanDiffLine, MessageType.DEBUG);
            }
        }
        return isRelevantChange;
    }
    
    /**
     * Checks whether the given diff line indicates a block-related change. This is the case, if it:
     * <ul>
     * <li>contains a block statement:
     *     <ul>
     *     <li><code>#if*</code></li>
     *     <li><code>#else</code></li>
     *     <li><code>#elif</code></li>
     *     <li>or <code>#endif</code></li>
     *     </ul>
     * </li>
     * <li>is a continuation of a <code>#if*</code> or <code>#elif</code> condition</li>
     * </ul> 
     * @param cleanDiffLine the <i>normalized</i> diff line to check
     * @param cleanDiffLinePosition the position of the diff line in the set of all diff lines
     * @return <code>true</code> if the diff line indicates a block-related change; <code>false</code> otherwise
     */
    private boolean isBlockRelatedChange(String cleanDiffLine, int cleanDiffLinePosition) {
        boolean isBlockRelatedChange = false;
        if (containsBlockStatement(cleanDiffLine) || isBlockConditionContinuation(cleanDiffLinePosition)) {
            isBlockRelatedChange = true;
        }
        return isBlockRelatedChange;
    }
    
    /**
     * Checks whether the given diff line contains a block statement. This is the case, if it matches one of the
     * following patterns:
     * <ul>
     * <li>{@link #CPP_IF_PATTERN}</li>
     * <li>{@link #CPP_ELIF_PATTERN}</li>
     * <li>{@link #CPP_ELSE_PATTERN}</li>
     * <li>{@link #CPP_ENDIF_PATTERN}</li>
     * </ul>
     * @param cleanDiffLine the <i>normalized</i> diff line to check
     * @return <code>true</code> if the diff line matches one of the patterns above; <code>false</code> otherwise
     */
    private boolean containsBlockStatement(String cleanDiffLine) {
        boolean containsBlockStatement = false;
        if (CPP_IF_PATTERN.matcher(cleanDiffLine).matches()
                || CPP_ELIF_PATTERN.matcher(cleanDiffLine).matches()
                || CPP_ELSE_PATTERN.matcher(cleanDiffLine).matches()
                || CPP_ENDIF_PATTERN.matcher(cleanDiffLine).matches()) {
            containsBlockStatement = true;
        }
        return containsBlockStatement;
    }
    
    /**
     * Checks whether the diff line indicated by the given diff line position in the set of all diff lines is a
     * continuation of a blocks condition (condition definition over multiple lines using "<code>\</code>").
     * 
     * @param diffLinePosition the position of a diff line in the set of all diff lines to check
     * @return <code>true</code> if the diff line indicated by the given diff line position is a continuation;
     *         <code>false</code> otherwise
     */
    private boolean isBlockConditionContinuation(int diffLinePosition) {
        boolean isBlockConditionContinuation = false;
        boolean continuationFound = true;
        int diffLineCounter = diffLinePosition - 1;
        String normalizedDiffLine;
        while (!isBlockConditionContinuation && continuationFound && diffLineCounter >= 0) {
            normalizedDiffLine = normalize(diffLines[diffLineCounter], diffLineCounter).trim(); 
            if (!normalizedDiffLine.endsWith("\\")) {
                continuationFound = false;
            } else {
                if (containsBlockStatement(normalizedDiffLine)) {
                    isBlockConditionContinuation = true;
                }
            }
            diffLineCounter--;
        }
        return isBlockConditionContinuation;
    }
    
    /**
     * Checks whether the diff line at the given diff line position is part of a multi line comment.
     * 
     * @param diffLinePosition the position of a diff line in the set of all diff lines to check
     * @return <code>true</code> if the diff line at the given index is part of a multi line comment; <code>false</code>
     *         otherwise
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
    
}

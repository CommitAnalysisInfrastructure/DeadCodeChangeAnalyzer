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

import java.util.List;
import java.util.regex.Pattern;

import net.ssehub.comani.data.ChangedArtifact;
import net.ssehub.comani.data.Commit;

/**
 * This class represents a general diff analyzer, which checks the changes to different types of artifacts with respect
 * to their impact on the dead code analysis. Hence, if the change may influence earlier results, the current commit
 * is marked accordingly.
 * 
 * @author Christian Kroeher
 *
 */
public class DiffAnalyzer {
    
    /**
     * This array contains file extensions (without the ".") for identifying files that should not be analyzed.<br><br>
     * 
     * Although regular expressions for identifying files for analysis exist, there are certain combinations that lead
     * to wrong results, e.g. "Config.lb" (found in coreboot), where the name of the file seems to define a 
     * Kconfig-file, but the content is not.
     */
    private static final String[] FILE_EXTENSION_BLACKLIST = {"lb"};
    
    /**
     * Regex identifying directories containing documentation.<br><br>
     * 
     * Value: {@value #DOC_DIR_PATTERN};
     */
    private static final String DOC_DIR_PATTERN = "[dD]ocumentation(s)?";
    
    /**
     * Regex identifying directories containing scripts.<br><br>
     * 
     * Value: {@value #SCRIPT_DIR_PATTERN};
     */
    private static final String SCRIPT_DIR_PATTERN = "[sS]cript(s)?";
    
    /**
     * Regex identifying files to be excluded from analysis, in particular documentation files or scripts.<br><br>
     * 
     * Value: {@value #FILE_EXCLUDE_PATTERN};<br>
     * 
     * See {@link #DOC_DIR_PATTERN} and {@link #SCRIPT_DIR_PATTERN}
     */
    private static final String FILE_EXCLUDE_PATTERN = "(.*/((" + DOC_DIR_PATTERN + ")|(" + SCRIPT_DIR_PATTERN 
            + "))/.*)|(.*\\.txt)";
    
    /**
     * The string denoting the Java regular expression for identifying variability model files.
     */
    private String vmFilePattern;
    
    /**
     * The string denoting the Java regular expression for identifying code files.
     */
    private String codeFilePattern;
    
    /**
     * The string denoting the Java regular expression for identifying build files.
     */
    private String buildFilePattern;
    
    /**
     * The {@link Commit} to analyze given via the constructor of this class.
     * 
     * @see #DiffAnalyzer(Commit)
     */
    private Commit commit;
    
    /**
     * The {@link AnalysisResult} of analyzing the {@link #commit}.
     */
    private AnalysisResult analysisResult;
    
    /**
     * Construct a new {@link DiffAnalyzer}.
     * 
     * @param vmFilesRegex the regular expression identifying variability model files
     * @param codeFilesRegex the regular expression identifying code files
     * @param buildFilesRegex the regular expression identifying build files
     * @param commit the {@link Commit} containing diff information
     */
    public DiffAnalyzer(String vmFilesRegex, String codeFilesRegex, String buildFilesRegex, Commit commit) {
        this.vmFilePattern = vmFilesRegex;
        this.codeFilePattern = codeFilesRegex;
        this.buildFilePattern = buildFilesRegex;
        this.commit = commit;
    }
    
    /**
     * Analyze the artifacts changed by the given commit.
     * 
     * @return <code>true</code> if the analysis of the given commit (changed artifacts) was successful,
     *         <code>false</code> otherwise
     */
    public boolean analyze() {
        boolean analyzedSuccessful = false;
        if (!commit.getId().isEmpty()) {
            List<ChangedArtifact> changedArtifactList = commit.getChangedArtifacts();
            analysisResult = new AnalysisResult();
            analysisResult.setCommitId(commit.getId());
            FileDiff fileDiff = null;
            for (ChangedArtifact changedArtifact : changedArtifactList) {
                if (!changedArtifact.getArtifactPath().isEmpty() 
                        && !Pattern.matches(FILE_EXCLUDE_PATTERN, changedArtifact.getArtifactPath()) 
                        && !isBlacklisted(changedArtifact.getArtifactPath())) {
                    if (Pattern.matches(codeFilePattern, changedArtifact.getArtifactPath())) {
                        // Diff affects source code file
                        String[] diffLines = changedArtifact.getContent().toArray(new String[0]);
                        fileDiff = new CodeFileDiff(diffLines);
                        if (fileDiff.getResult()) {
                            analysisResult.addRelevantCodeChanges(changedArtifact.getArtifactPath());
                        }
                    } else if (!analysisResult.getRelevantBuildChanges() 
                            && Pattern.matches(buildFilePattern, changedArtifact.getArtifactPath())) {
                        // Diff affects build file
                        String[] diffLines = changedArtifact.getContent().toArray(new String[0]);
                        fileDiff = new BuildFileDiff(diffLines);
                        analysisResult.setRelevantBuildChanges(fileDiff.getResult());
                    } else if (!analysisResult.getRelevantVariabilityModelChanges() 
                            && Pattern.matches(vmFilePattern, changedArtifact.getArtifactPath())) {
                        // Diff affects variability model file
                        String[] diffLines = changedArtifact.getContent().toArray(new String[0]);
                        fileDiff = new VariabilityModelFileDiff(diffLines);
                        analysisResult.setRelevantVariabilityModelChanges(fileDiff.getResult());
                    }
                }
            }
            analyzedSuccessful = true;
        }
        return analyzedSuccessful;
    }
    
    /**
     * Returns the result of analyzing the given commit.
     * 
     * @return the {@link #analysisResult}; may be <code>null</code>
     */
    public AnalysisResult getResult() {
        return analysisResult; 
    }
    
    /**
     * Check the name of the changed file defined in the given changed file description line
     * against the blacklisted file extensions defined in {@link #FILE_EXTENSION_BLACKLIST}.
     *  
     * @param changedFileDescriptionLine the first line of a diff containing the path and the
     * name of the changed file, e.g. "diff --git a/include/libbb.h b/include/libbb.h"
     * @return <code>true</code> if the extension of the file in the given changed file description
     * line matches on of the blacklisted file extensions, <code>false</code> otherwise
     */
    private boolean isBlacklisted(String changedFileDescriptionLine) {
        boolean isBlacklisted = false;
        int blacklistCounter = 0;
        while (blacklistCounter < FILE_EXTENSION_BLACKLIST.length && !isBlacklisted) {
            /*
             * The given line always contains a string similar to "diff --git a/include/libbb.h b/include/libbb.h".
             * Thus, remove leading and trailing whitespace and check if one of the blacklist entries prepended by
             * a "." matched the end of the given line. 
             */
            String fileExtension = "." + FILE_EXTENSION_BLACKLIST[blacklistCounter];
            if (changedFileDescriptionLine.trim().endsWith(fileExtension)) {
                isBlacklisted = true;
            }
            blacklistCounter++;
        }
        return isBlacklisted;
    }
    
}

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
package net.ssehub.comani.analysis.deadcodechange.core;

import java.util.Properties;

import net.ssehub.comani.analysis.AbstractCommitAnalyzer;
import net.ssehub.comani.analysis.AnalysisSetupException;
import net.ssehub.comani.data.IAnalysisQueue;

/**
 * This class represents the main class of this analyzer.
 * 
 * @author Christian Kroeher
 *
 */
public class DeadCodeChangeAnalyzer extends AbstractCommitAnalyzer {

    /**
     * Create a new instance of this analyzer.
     *  
     * @param analysisProperties the properties of the properties file defining the analysis process and the
     *        configuration of the analyzer in use; all properties, which start with the prefix "<tt>analysis.</tt>" 
     * @param commitQueue the {@link IAnalysisQueue} for transferring commits from an extractor to an analyzer
     * @throws AnalysisSetupException if the analyzer is not supporting the current operating or version control
     *         system as well as preparing this analyzer fails
     */
    public DeadCodeChangeAnalyzer(Properties analysisProperties, IAnalysisQueue commitQueue)
            throws AnalysisSetupException {
        super(analysisProperties, commitQueue);
        // TODO Auto-generated constructor stub
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean analyze() {
        // TODO Auto-generated method stub
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean operatingSystemSupported(String operatingSystem) {
        // TODO Auto-generated method stub
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean versionControlSystemSupported(String versionControlSystem) {
        // TODO Auto-generated method stub
        return false;
    }

}

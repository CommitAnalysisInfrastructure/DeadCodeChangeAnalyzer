# DeadCodeChangeAnalyzer
This [ComAnI](https://github.com/CommitAnalysisInfrastructure/ComAnI) plug-in realizes an analyzer for identifying changes to code, build, and variability model artifacts in KBuild-based Software Product Lines, like [Linux kernel](https://git.kernel.org/pub/scm/linux/kernel/git/torvalds/linux.git) or the [Coreboot firmware](https://www.coreboot.org/downloads.html), which affect the results of the dead code analysis [1]. It therefore adapts the algorithms of the [VariabilityChangeAnalyzer]( https://github.com/CommitAnalysisInfrastructure/VariabilityChangeAnalyzer) to consider changes that affect the variability information in build and variability model artifacts as well as code blocks.

*Main class name:* `net.ssehub.comani.analysis.deadcodechange.core.DeadCodeChangeAnalyzer`

*Support:*
- Operating system: all
- Version control system: “git” or “svn”

For more information on how to use ComAnI and its plug-ins, we recommend reading the [ComAnI Guide](https://github.com/CommitAnalysisInfrastructure/ComAnI/blob/master/guide/ComAnI_Guide.pdf).

## Installation
Download the [DeadCodeChangeAnalyzer.jar](/release/DeadCodeChangeAnalyzer.jar) file from the release directory and save it to the ComAnI plug-ins directory on your machine. This directory is the one specified as `core.plugins_dir` in the configuration file of a particular ComAnI instance.

*Requirements:*
- The [ComAnI infrastructure](https://github.com/CommitAnalysisInfrastructure/ComAnI) has to be installed to execute this plug-in as the analyzer of a particular ComAnI instance

## Execution
This plug-in is not a standalone tool, but only executable as the analyzer of a particular ComAnI instance. Therefore, it has to be defined in the configuration file via its fully qualified main class name as follows:

`analysis.analyzer = net.ssehub.comani.analysis.deadcodechange.core.DeadCodeChangeAnalyzer`

*Plug-in-specific configuration parameter(s):*

The regular expressions for identifying variability model, code, and build files. The definition of all three properties is mandatory and must follow the definition of [Java regular expressions](https://docs.oracle.com/javase/7/docs/api/java/util/regex/Pattern.html). The absence of these definitions causes an `AnalysisSetupException`. An example is given below for each of the parameters.
```Properties
Type: mandatory
Default value: none
Related parameters: none
analysis.variability_change_analyzer.vm_files_regex = <Java-Regex>
```
Example: `analysis.variability_change_analyzer.vm_files_regex = .*/Kconfig((\\.|\\-|\\_|\\+|\\~).*)?`
```Properties
Type: mandatory
Default value: none
Related parameters: none
analysis.variability_change_analyzer.code_files_regex = <Java-Regex>
```
Example: `analysis.variability_change_analyzer.code_files_regex = .*/.*\\.[hcS]((\\.|\\-|\\_|\\+|\\~).*)?`
```Properties
Type: mandatory
Default value: none
Related parameters: none
analysis.variability_change_analyzer.build_files_regex = <Java-Regex>
```
Example: `analysis.variability_change_analyzer.build_files_regex = .*/(Makefile|Kbuild)((\\.|\\-|\\_|\\+|\\~).*)?`

## License
This project is licensed under the [Apache License 2.0](https://www.apache.org/licenses/LICENSE-2.0.html).

## Acknowledgments
This work is partially supported by the ITEA3 project [REVaMP²](http://www.revamp2-project.eu/), funded by the [BMBF (German Ministry of Research and Education)](https://www.bmbf.de/) under grant 01IS16042H.

A special thanks goes to the developers of [KernelHaven](https://github.com/KernelHaven/): Adam Krafczyk, Sascha El-Sharkawy, Moritz Flöter, Alice Schwarz, Kevin Stahr, Johannes Ude, Manuel Nedde, Malek Boukhari, and Marvin Forstreuter. Their architecture and core concepts significantly inspired the development of this project. In particular, the mechanisms for file-based configuration of the infrastructure and the plug-ins as well as loading and executing individual plug-ins are adopted in this work.

## References
[1] Reinhard Tartler, Daniel Lohmann, Julio Sincero and Wolfgang Schröder-Preikschat. [Feature Consistency in Compile-time-configurable System Software: Facing the Linux 10,000 Feature Problem]( https://dl.acm.org/citation.cfm?id=1966451). In Proceedings of the 6th Conference on Computer Systems, pp. 47-60, ACM, 2011.



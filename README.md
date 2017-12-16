# ClinicalTrialAnnotator

#### Build Instruction
1. Import as maven project.
2. Download the [Stanford CoreNLP models](http://nlp.stanford.edu/software/stanford-corenlp-models-current.jar) and add to the build path.

#### Usage
1. Modify main method of GraphBuilder, provide required neo4j authentication information.
2. Create a directory named xml and place all xml files that need to be annoteated there.
3. Run the program, converted json files are going to be created under the build path.

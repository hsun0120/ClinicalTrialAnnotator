# ClinicalTrialAnnotator

## Build Instruction
1. Import as maven project.
2. Download the [MetaMap Web API](https://ii.nlm.nih.gov/Web_API/index.shtml) and add to the build path.
2. Download the [Stanford CoreNLP models](http://nlp.stanford.edu/software/stanford-corenlp-models-current.jar) and add to the build path.
Note: a [UMLS Terminlogy Service (UTS)](https://uts.nlm.nih.gov/home.html) account is required to use MetaMap and OntologyBuilder.

## Usage
### MetaMap
This program takes 4 command line arguments: UTS account name, password, email, and file name (the file must be in plain text format).

### OntologyBuilder
This program takes 5 command line arguments: UTS account name, password, email, input xml file (from clinical trials), and output json file name.

### GraphBuilder
1. Modify main method of GraphBuilder, provide required neo4j authentication information.
2. Create a directory named xml and place all xml files that need to be annoteated there.
3. Run the program, converted json files are going to be created under the build path.

## Typical Issues
Since MetaMap does not support non-ASCII characters, you will get an error if the input file contains any non-ASCII characters that are not yet handled by the program (see Issues tab for details).
GraphBuilder will not crash in this case, but you will get a untokenizable warning from Stanford NLP.

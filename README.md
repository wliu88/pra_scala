## PRA (Path Ranking Algorithm) and SFE (Subgraph Feature Extraction)

This repo is forked from Matt Gardner's [pra code](https://github.com/matt-gardner/pra). The original is no longer
maintained. Modifications have been made to ensure the code runs smoothly. Support for new datasets have also been 
added. Below is a brief description of the code from the original repo:

PRA and SFE are algorithms that extract feature matrices from graphs, and use those feature
matrices to do link prediction in that graph.  This repository contains implementations of PRA and
SFE, as used in the following papers (among others):

* Efficient and Expressive Knowledge Base Completion Using Subgraph Feature Extraction.  Matt
  Gardner and Tom Mitchell.  EMNLP 2015. ([website](http://rtw.ml.cmu.edu/emnlp2015_sfe))
* Incorporating Vector Space Similarity in Random Walk Inference over Knowledge Bases.  Matt
  Gardner, Partha Talukdar, Jayant Krishnamurthy, and Tom Mitchell.  EMNLP 2014.
([website](http://rtw.ml.cmu.edu/emnlp2014_vector_space_pra))
* Improving Learning and Inference in a Large Knowledge-base using Latent Syntactic Cues.  Matt
  Gardner, Partha Talukdar, Bryan Kisiel, and Tom Mitchell.  EMNLP 2013.
([website](http://rtw.ml.cmu.edu/emnlp2013_pra))

To reproduce the experiments in those papers, see the corresponding website.  Note that the EMNLP
2015 paper has the most detailed instructions, and the older papers use versions of the code that
aren't compatible with the current repository.

See [the github.io page](http://matt-gardner.github.io/pra/) for code documentation.

## Dependencies:
This repo is written in Scala. Scala uses sbt as its main build tool. You can download sbt 
[here](https://www.scala-sbt.org/download.html). Once you have sbt installed and in your shell’s PATH, you can run 
the following commands to clone the repository and run the tests, verifying that things are working correctly:
```bash
git clone https://github.com/matt-gardner/pra
cd pra
sbt test
```
If the end output is something like the following, you can be confident that everything is set up properly:
```bash
/home/mg1/clone/pra$ sbt test
[info] Loading global plugins from /usr0/home/mg1/.sbt/0.13/plugins
[info] Loading project definition from /usr0/home/mg1/clone/pra/project
[info] Set current project to pra (in build file:/usr0/home/mg1/clone/pra/)
[... a lot of output from the tests ...]
[info] Passed: Total 65, Failed 0, Errors 0, Passed 65
[success] Total time: 2 s, completed Sep 29, 2014 5:11:42 PM
```


## Memory Requirement

This code takes quite a bit of memory. On NELL graphs, the code can easily use upwards of **40GB**. 
To set the maximum heap size for running the code, change `javaOptions in run ++= Seq("-Xmx40g")` 
in `build.sbt`.

## Quick Start
To quickly reproduce the best result from the EMNLP 2015 paper, run the following commands: 
```bash
git clone https://github.com/matt-gardner/pra.git
cd pra/examples
wget http://rtw.ml.cmu.edu/emnlp2015_sfe/graph.tgz
mkdir -p graphs/nell/
tar -xzf graph.tgz
mv kb_svo/ graphs/nell/
wget http://rtw.ml.cmu.edu/emnlp2015_sfe/split.tgz
mkdir splits/
tar -xzf split.tgz
mv final_nell_split_with_negatives/ splits/
wget http://rtw.ml.cmu.edu/emnlp2015_sfe/nell_metadata.tgz
mkdir relation_metadata/
tar -xzf nell_metadata.tgz
mv nell/ relation_metadata/
cd ..
sbt "run ./examples/ sfe_bfs_pra_anyrel"
```
You will need to run the last command twice, incidentally - the first time, you should select the `ExperimentRunner` 
option, and the second time you should select `ExperimentScorer`, to see the results.

## Using your own data
1. Four components are necessary for running this code: relation metadata, a graph, a training/testing split, and an
experiment specification file. Details can be accessed [here](http://matt-gardner.github.io/pra/input/experiment_spec.html).
Even though you can specify all four components completely by yourself, I am adopting an easier approach
in which I only need to provide relation metadata and an experiment specification. I will use the code to automatically
generate the graph and the training/testing split. 

2. Read Issue Page of the original repo. 

2. To specify relation metadata, you need to make a new folder in the `relation_metadata` folder. In the new folder,
generate `category_instances/`, `domains.tsv`, `ranges.tsv`, `labeled_edges.tsv`, `relations/`, and `inverses.tsv` based
on your data according to specifications [here](http://matt-gardner.github.io/pra/input/relation_metadata.html).
Usually, data for knowledge completion task is made of triplets, each contains a subject, a relation, and an object. A
list of triplets is enough to generate all the files mentioned above.

3. To automatically generate the graph and split from the relation metadata, create an experiment specification file 
`your_experiment_specs` according the example `robot_create_graph_and_split.json`. 
Then run `sbt "run ./examples/ your_experiment_specs"`. After running the command, two new folders will be created in
`splits` and `graphs`.

## Run SFE with random walk path extractor
1. Add two lines in the `create_graph_and_split` experiment spec to create GraphChi files
```$json
"graph": {
  ...
  "shard plain text graph": true,
  "output plain text file": true,
}
```
  GraphChi is the graph object that random walk uses.

2. Remove `graphs/your_dataset/edge.dat`, otherwise the code will look for this file using a wrong name, causing a
runtime error. 

3. Use `pra` experiment spec to run PRA and use `extract_pra` to get features extracted from random walks. 



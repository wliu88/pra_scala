## PRA (Path Ranking Algorithm) and SFE (Subgraph Feature Extraction)

This repo is forked from Matt Gardner's [pra code](https://github.com/matt-gardner/pra). The original is no longer
maintained. Modifications have been made to ensure the code runs smoothly. Support for new datasets have also been 
added. See `README_Original.md` for more details about the original repo. 

## Installation
This repo is written in Scala. Scala uses sbt as its main build tool. You can download sbt 
[here](https://www.scala-sbt.org/download.html). Once you have sbt installed and in your shell’s PATH, you can run 
the following commands to clone the repository and run the tests, verifying that things are working correctly:
```bash
git clone git@github.com:wliu88/pra_scala.git
cd pra_scala
sbt test
```
If the end output is something like the following, you can be confident that everything is set up properly:
```bash
.../pra_scala$ sbt test
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
To run an experiment using the code, four types of data are necessary: relation metadata, a knowledge graph, 
a training/testing split, and an experiment specification file.  All data for an experiment need to be stored in the
`/examples` folder. To get started with the code, you can run experiments either with the WN18RR or FB15k-237 
dataset by moving one of the pre-specified `examples` folders stored in `templates/` to the root of the repo.

Now I will discuss folders in the `examples` folder:
* `experiment_specs`: this folder stores `json` files where each fully specifies an experiment you can run with the code.
Details can be accessed [here](http://matt-gardner.github.io/pra/input/experiment_spec.html).
* `param_files`: this folder stores `json` files which serve as bases to build up different experiments. To use these
base specs, you can use the `load` command in an experiment spec file. 
* `graphs`: this folder stores all data that specify a knowledge graph.
* `relation_metadata`: this folder stores relation instances and meta-data about them, such as relation domains and entity types.
* `splits`: this folder stores data splits.
* `results`: this folder stores experiment results.

Even though you can specify the four types of data mentioned above completely by yourself, an easier approach is to 
only provide relation metadata and experiment specifications. Then you can use the code to automatically generate 
the graph and the training/testing split. Because of this, the `examples` folders I provide as templates only include 
`experiment_specs`, `param_files`, and `relation_metadata`. Now I will walk through some of the template experiment specs.

* `[dataset name prefix]_create_graph_and_split.json`: this experiment spec will help create a knowledge graph, sample 
negative examples, and split the data into training and testing. All the remaining folders described above that are 
initially missing will be populated after running this experiment spec. ***Note*** that you need to remove the `edges.dat`
file in the `graphs` folder after you finish running this experiment spec. This file will cause other experiments to fail. 
* `[dataset name prefix]_pra.json`: this experiment spec helps running the pra method.
* `[dataset name prefix]_sfe.json`: this experiment spec helps running the sfe method.

Finally, to run an experiment spec, you just need to run `sbt "run ./examples/ [the name of your experiment spec]"` in
your shell while you are in the root of the repo. You will need to run the command twice (except for the 
`[dataset name prefix]_create_graph_and_split.json`), incidentally - the first time, you should select the `ExperimentRunner` 
option, and the second time you should select `ExperimentScorer`, to see the results. Note that the log info sometimes 
may be printed out as [error]. It's not actually error.


## Matt's Quick Start
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
option, and the second time you should select `ExperimentScorer`, to see the results. Note that the log info sometimes 
may be printed out as [error]. It's not actually error.

## Using your own data
1. To specify relation metadata, you need to make a new folder in the `relation_metadata` folder. In the new folder,
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



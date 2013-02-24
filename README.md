## Instalation

1. git clone https://github.com/cos/iterace-workspace.git
2. cd iterace-workspace
3. git submodule init
4. git submodule update
5. edit IteRace/src/main/resources/local.conf `wala.jre-lib-path` to point to your jre classes jar
6. have a look at /project/subjects and add the projects you want to run the benchmarks over to the appropriate location.
   - many of the projects have forks https://github.com/cos
   - afterwards, edit /project/Build.scala according to your selection
6. sbt
7. Within the sbt console: > compile

Post an issue on the workspace's github page if you encounter any problems during instalation.

## Running the evaluation

Within the SBT console:

* Use `bench` for a single run, e.g. `bench subject=em3d two-threads`. Try tabing for autocomplete help.
 
* Use `bench-all` to run all configurations/scenarios that match a particular constraint, e.g., `bench-all two-threads` runs `bench` for all projects under all configurations that have `two-threads` activated. 

* use `merge-all` to gather all results in a single `.json` file within `project/target/all.json`

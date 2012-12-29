
## Instalation

1. git clone https://github.com/cos/iterace-workspace.git
2. cd iterace-workspace
3. git update

## Running the evaluation

Use `sbt` within the iterace-workspace folder to enter the SBT console.

Within the SBT console:

* Use `bench` for a single run, e.g. `bench project=em3d two-threads`. Try tabing for autocomplete help.
 
* Use `bench-all` to run all configurations/scenarios that match a particular constraint, e.g., `bench-all two-threads` runs `bench` for all projects under all configurations that have `two-threads` activated. 

* use `merge-all` to gather all results in a single `.json` file within `project/target/all.json`

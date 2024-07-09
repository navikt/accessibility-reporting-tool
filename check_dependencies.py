# importing the module
import json
import sys
import subprocess
import re
import sys
from datetime import date

json_file_name = 'build/dependencyUpdates/dependencies.json'
dependency_definition_file = "buildSrc/src/main/kotlin/dependencies/Groups.kt"


def run_checks():
    if len(sys.argv) > 1 and str(sys.argv[1]) == "--runTask":
        subprocess.call(["./gradlew", "dependencyUpdates"])
    updates = list(get_avaiable_updates())
    if len(updates) != 0:
        updates_summary = [map_dependency(dep) for dep in updates if is_major_version(dep)]
        write_as_comment(updates_summary)
        print(f'Found {len(updates_summary)} outdated dependencies, see see {dependency_definition_file} for details')
        sys.exit(len(updates_summary))
    else:
        sys.exit(0)


def get_avaiable_updates():
    with open(json_file_name, 'r') as json_file:
        data = json.load(json_file)
        available_updates = data["outdated"]["dependencies"]
        json_file.close()
        return available_updates


def is_major_version(version):
    return bool(re.search("^[0-9.]*$", version["available"]["milestone"]))


def map_dependency(dep):
    return f'{dep["group"]}:  {dep["version"]} -> {dep["available"]["milestone"]}'


def write_as_comment(pending_updates):
    dependency_file = open(dependency_definition_file, 'a')
    dependency_file.write("\n/*\n")
    dependency_file.write(f'{date.today()}: {len(pending_updates)} outdated dependencies')
    for pending in pending_updates:
        dependency_file.write(f'\n{pending}')
    dependency_file.write("\n*/")
    dependency_file.close()


run_checks()

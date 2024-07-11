# importing the module
import json
import subprocess
import re
import sys
from datetime import datetime
import os

json_file_name = 'app/build/dependencyUpdates/dependencies.json'
date_str = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
dependency_log_file = f'dependency_updates/{datetime.now().strftime("%Y-%m")}.txt'
dependency_definition_file = "buildSrc/src/main/kotlin/dependencies/Groups.kt"


def run_checks():
    if len(sys.argv) > 1 and str(sys.argv[1]) == "--runTask":
        subprocess.call(["./gradlew", "app:dependencyUpdates"])
    updates = list(get_avaiable_updates())
    if len(updates) != 0:
        updates_summary = [map_dependency(dep) for dep in updates if is_major_version(dep)]
        write_findings_to_file(updates_summary)
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


def write_findings_to_file(pending_updates):
    if os.path.exists(dependency_log_file):
        log_file = open(dependency_log_file, 'a')
        log_file.write("\n -----Dependency check ------\n")
        write_to_log(log_file, pending_updates)
    else:
        write_to_log(open(dependency_log_file, 'w'), pending_updates)

    dependency_file = open(dependency_definition_file, 'a')
    dependency_file_header = f'\n/*\n{date_str}: {len(pending_updates)} outdated dependencies'
    write_dependency_summary(dependency_file, dependency_file_header, pending_updates)


def write_to_log(dependency_log, pending_updates):
    dependency_log_header = f'{date_str}: {len(pending_updates)} outdated dependencies found'
    write_dependency_summary(dependency_log, dependency_log_header, pending_updates)
    dependency_log.close()


def write_dependency_summary(file, header, pending_updates):
    file.write(header)
    for pending in pending_updates:
        file.write(f'\n{pending}')


run_checks()

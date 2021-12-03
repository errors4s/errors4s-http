#!/usr/bin/env bash

set -e

source sbt-shell-release/src/bash/functions.sh

export SBT_TASKS=';docs/mdoc --check;++2.13.6;scalafixAll --check;scalafmtSbtCheck;scalafmtCheckAll;+test;+test:doc'
readonly SBT_TASKS

sbt_release

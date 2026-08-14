#!/bin/bash
set -euo pipefail

SCRIPT_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
PROJECT_ROOT=$(cd "$SCRIPT_DIR/../.." && pwd)
source "$SCRIPT_DIR/sdd-run-log.sh"
cd "$PROJECT_ROOT"

FEATURE=${1:-}
ACTION=${2:-implement}
DELTA_FILE=""
TASK_ID=""

if [[ "${3:-}" =~ ^T[0-9]{3}$ ]]; then
  TASK_ID=${3:-}
else
  DELTA_FILE=${3:-}
  TASK_ID=${4:-}
fi

usage() {
  cat <<EOF
Usage: $0 <feature> [action] [delta-file] [task-id]

Actions:
  implement   Implement feature from SDD spec (default)
  test        Generate tests from SDD spec
  refactor    Refactor existing code using SDD context

Examples:
  $0 product
  $0 product test
  $0 product refactor
  $0 product implement T003
  $0 customer implement docs/specs/customer.delta.md
  $0 customer implement docs/specs/customer.delta.md T003
EOF
}

if [[ "$FEATURE" == "-h" || "$FEATURE" == "--help" ]]; then
  usage
  exit 0
fi

if [[ -z "$FEATURE" ]]; then
  usage
  exit 1
fi

if [[ -n "$TASK_ID" && ! "$TASK_ID" =~ ^T[0-9]{3}$ ]]; then
  echo "Task id must use Txxx format, for example T003: $TASK_ID" >&2
  exit 1
fi

case "$ACTION" in
  implement|implementation|spec)
    ACTION_CANONICAL="implement"
    PROMPT_FILE="ai/prompts/implement-from-spec.md"
    AGENT_FILE="ai/agents/implementation-engineer.md"
    ACTION_DESCRIPTION="Implement the feature described by the SDD files."
    ;;
  test|tests|unit-tests|unit)
    ACTION_CANONICAL="test"
    PROMPT_FILE="ai/prompts/test-generator.md"
    AGENT_FILE="ai/agents/test-engineer.md"
    ACTION_DESCRIPTION="Generate comprehensive tests for the feature described by the SDD files."
    ;;
  refactor|refactoring)
    ACTION_CANONICAL="refactor"
    PROMPT_FILE="ai/prompts/refactor.md"
    AGENT_FILE=""
    ACTION_DESCRIPTION="Refactor the feature implementation using the SDD files as context."
    ;;
  *)
    echo "Unknown action: $ACTION" >&2
    usage
    exit 1
    ;;
esac

CONSTITUTION_FILE="docs/sdd/constitution.md"
WORKFLOW_FILE="docs/sdd/workflow.md"
CONTEXT_MAP_FILE="docs/sdd/context-map.md"
SKILLS_FILE="docs/sdd/skills.md"
CONTEXT_FILE="ai/context/mobile.md"
FEATURE_DIR="docs/features/${FEATURE}"
FEATURE_SPEC_FILE="${FEATURE_DIR}/spec.md"
FEATURE_PLAN_FILE="${FEATURE_DIR}/plan.md"
FEATURE_TASKS_FILE="${FEATURE_DIR}/tasks.md"
DOMAIN_SPEC_FILE="docs/specs/${FEATURE}.spec.md"
DOMAIN_TASKS_FILE="docs/tasks/${FEATURE}.tasks.md"

for file in "$PROMPT_FILE" "$CONSTITUTION_FILE" "$WORKFLOW_FILE" "$CONTEXT_MAP_FILE" "$SKILLS_FILE"; do
  if [[ ! -f "$file" ]]; then
    echo "Required file not found: $file" >&2
    exit 1
  fi
done

if [[ -n "$AGENT_FILE" && ! -f "$AGENT_FILE" ]]; then
  echo "Required agent file not found: $AGENT_FILE" >&2
  exit 1
fi

if [[ -d "$FEATURE_DIR" ]]; then
  if [[ -f "ai/prompts/sdd-execute.md" ]]; then
    PROMPT_FILE="ai/prompts/sdd-execute.md"
  fi

  for file in "$FEATURE_SPEC_FILE" "$FEATURE_PLAN_FILE" "$FEATURE_TASKS_FILE"; do
    if [[ ! -f "$file" ]]; then
      echo "Required feature file not found: $file" >&2
      exit 1
    fi
  done

  if [[ -n "$TASK_ID" ]]; then
    if ! grep -Eq "\b${TASK_ID}\b" "$FEATURE_TASKS_FILE"; then
      echo "Task id not found in $FEATURE_TASKS_FILE: $TASK_ID" >&2
      exit 1
    fi
  fi

  FEATURE_CONTEXT=$(cat <<EOF
## Feature Specification
$(cat "$FEATURE_SPEC_FILE")

## Feature Plan
$(cat "$FEATURE_PLAN_FILE")

## Feature Tasks
$(cat "$FEATURE_TASKS_FILE")
EOF
)
else
  if [[ -n "$TASK_ID" ]]; then
    echo "Task-focused execution requires docs/features/${FEATURE}/tasks.md" >&2
    exit 1
  fi

  for file in "$DOMAIN_SPEC_FILE" "$DOMAIN_TASKS_FILE" "$CONTEXT_FILE"; do
    if [[ ! -f "$file" ]]; then
      echo "Required domain file not found: $file" >&2
      exit 1
    fi
  done

  FEATURE_CONTEXT=$(cat <<EOF
## Domain Specification
$(cat "$DOMAIN_SPEC_FILE")

## Domain Tasks
$(cat "$DOMAIN_TASKS_FILE")

## Mobile Context
$(cat "$CONTEXT_FILE")
EOF
)
fi

TASK_SECTION=""
if [[ -n "$TASK_ID" ]]; then
  ACTION_DESCRIPTION="Execute only SDD task ${TASK_ID}. Do not implement unrelated pending tasks."
  TASK_SECTION=$(cat <<EOF

## SDD Task Focus
Task id: ${TASK_ID}

Implement only this task and its declared dependencies. Keep changes scoped to the files and verification listed for this task. Mark only this task complete when verification passes.
EOF
)
fi

DELTA_SECTION=""
if [[ -n "$DELTA_FILE" ]]; then
  if [[ ! -f "$DELTA_FILE" ]]; then
    echo "Delta file not found: $DELTA_FILE" >&2
    exit 1
  fi

  DELTA_SECTION=$(cat <<EOF

## SDD Delta
Apply only the changes described in this delta file. Use the main specification as baseline context and avoid unrelated refactors.

$(cat "$DELTA_FILE")
EOF
)
fi

INPUT=$(cat <<EOF
$ACTION_DESCRIPTION

## SDD Constitution
$(cat "$CONSTITUTION_FILE")

## SDD Workflow
$(cat "$WORKFLOW_FILE")

## SDD Context Map
$(cat "$CONTEXT_MAP_FILE")

## SDD Skills Guide
$(cat "$SKILLS_FILE")

## AI Prompt
$(cat "$PROMPT_FILE")

$(if [[ -n "$AGENT_FILE" ]]; then printf '## SDD Agent\n%s\n\n' "$(cat "$AGENT_FILE")"; fi)
$FEATURE_CONTEXT
$TASK_SECTION
$DELTA_SECTION
EOF
)

run_sdd_logged "codex" \
  codex exec \
  --json \
  -C "$(pwd)" \
  -s workspace-write \
  "$INPUT"

#!/bin/bash

sdd_now_ms() {
  local now
  now=$(date +%s%3N)
  if [[ "$now" == *N ]]; then
    echo "$(($(date +%s) * 1000))"
  else
    echo "$now"
  fi
}

sdd_count_chars() {
  printf '%s' "${1:-}" | wc -c | tr -d ' '
}

sdd_count_lines() {
  if [[ -z "${1:-}" ]]; then
    echo 0
  else
    printf '%s\n' "$1" | wc -l | tr -d ' '
  fi
}

sdd_estimate_tokens_from_chars() {
  local chars=${1:-0}
  echo $(((chars + 3) / 4))
}

sdd_sum_codex_usage() {
  local jsonl_file=${1:-}
  if [[ -z "$jsonl_file" || ! -s "$jsonl_file" ]]; then
    return 1
  fi

  node - "$jsonl_file" <<'NODE'
const fs = require('fs');

const file = process.argv[2];
const totals = {
  input_tokens: 0,
  cached_input_tokens: 0,
  output_tokens: 0,
  reasoning_output_tokens: 0,
};
let turns = 0;

for (const line of fs.readFileSync(file, 'utf8').split(/\r?\n/)) {
  if (!line.trim()) continue;

  let event;
  try {
    event = JSON.parse(line);
  } catch {
    continue;
  }

  if (event.type !== 'turn.completed' || !event.usage) continue;
  turns += 1;

  for (const key of Object.keys(totals)) {
    const value = Number(event.usage[key] || 0);
    if (Number.isFinite(value)) {
      totals[key] += value;
    }
  }
}

if (turns === 0) {
  process.exit(1);
}

console.log(JSON.stringify({ turns, ...totals }));
NODE
}

sdd_json_field() {
  local json=${1:-}
  local field=${2:-}
  node -e "const data = JSON.parse(process.argv[1]); console.log(data[process.argv[2]] ?? 0)" "$json" "$field"
}

sdd_print_progress() {
  local pid=$1
  local start_ms=$2
  local interval=${SDD_PROGRESS_INTERVAL_SECONDS:-2}

  printf 'SDD executando'
  while kill -0 "$pid" 2>/dev/null; do
    local now_ms elapsed_seconds
    now_ms=$(sdd_now_ms)
    elapsed_seconds=$(((now_ms - start_ms) / 1000))
    printf '\rSDD executando... %ss' "$elapsed_seconds"
    sleep "$interval"
  done
  printf '\r'
}

sdd_build_execution_log() {
  local provider=$1
  local status=$2
  local started_at=$3
  local duration_ms=$4

  local input_chars input_lines input_tokens
  input_chars=$(sdd_count_chars "${INPUT:-}")
  input_lines=$(sdd_count_lines "${INPUT:-}")
  input_tokens=$(sdd_estimate_tokens_from_chars "$input_chars")

  local usage_section
  if [[ -n "${SDD_CODEX_USAGE_JSON:-}" ]]; then
    local turns input_real cached_real output_real reasoning_real billable_input_real
    turns=$(sdd_json_field "$SDD_CODEX_USAGE_JSON" "turns")
    input_real=$(sdd_json_field "$SDD_CODEX_USAGE_JSON" "input_tokens")
    cached_real=$(sdd_json_field "$SDD_CODEX_USAGE_JSON" "cached_input_tokens")
    output_real=$(sdd_json_field "$SDD_CODEX_USAGE_JSON" "output_tokens")
    reasoning_real=$(sdd_json_field "$SDD_CODEX_USAGE_JSON" "reasoning_output_tokens")
    billable_input_real=$((input_real - cached_real))
    if [[ "$billable_input_real" -lt 0 ]]; then
      billable_input_real=0
    fi
    usage_section=$(cat <<EOF
- Uso Codex: ${turns} turn(s), ${input_real} input tokens, ${cached_real} cached input tokens, ${billable_input_real} input tokens nao-cacheados, ${output_real} output tokens, ${reasoning_real} reasoning output tokens
EOF
)
  else
    usage_section="- Uso Codex: indisponivel nesta execucao; rode o adapter Codex com JSON habilitado para capturar o uso real"
  fi

  cat <<EOF

## Resumo da execucao SDD
- Projeto: ${PROJECT_ROOT:-$(pwd)}
- Provider: $provider
- Feature: ${FEATURE:-}
- Acao: ${ACTION_CANONICAL:-${ACTION:-}}
- Tarefa: ${TASK_ID:-none}
- Status: $status
- Inicio UTC: $started_at
- Duracao: ${duration_ms}ms
- Prompt: ${PROMPT_FILE:-}
- Delta: ${DELTA_FILE:-none}
- Entrada: ${input_chars} chars, ${input_lines} linhas, ~${input_tokens} tokens
- Uso local estimado: ~${input_tokens} tokens de entrada por chars/4, apenas para comparacao historica
$usage_section
- Saida: nao coletada para evitar logs grandes e estimativas enganosas
- Observacao: uso Codex vem do evento local turn.completed.usage emitido por codex exec --json; nao e estimado pelo modelo.
EOF
}

sdd_execution_log_path() {
  if [[ -z "${FEATURE:-}" ]]; then
    return 1
  fi

  local feature_dir runs_dir stamp action
  feature_dir="${PROJECT_ROOT:-$(pwd)}/docs/features/${FEATURE}"
  if [[ ! -d "$feature_dir" ]]; then
    return 1
  fi

  runs_dir="$feature_dir/runs"
  mkdir -p "$runs_dir"
  stamp=$(date -u +"%Y-%m-%dT%H-%M-%SZ")
  action="${ACTION_CANONICAL:-${ACTION:-run}}"
  echo "${runs_dir}/${stamp}-${action}-${1}-execution-log.md"
}

run_sdd_logged() {
  local provider=$1
  shift

  local started_at start_ms end_ms duration_ms status execution_log log_path output_mode usage_file stderr_file
  output_mode=${SDD_EXEC_OUTPUT:-quiet}
  usage_file=$(mktemp "${TMPDIR:-/tmp}/sdd-codex-usage.XXXXXX.jsonl")
  stderr_file=$(mktemp "${TMPDIR:-/tmp}/sdd-codex-stderr.XXXXXX.log")
  started_at=$(date -u +"%Y-%m-%dT%H:%M:%SZ")
  start_ms=$(sdd_now_ms)
  SDD_CODEX_USAGE_JSON=""

  set +e
  if [[ "$output_mode" == "full" ]]; then
    "$@" > >(tee "$usage_file") 2> >(tee "$stderr_file" >&2)
    status=$?
  else
    "$@" >"$usage_file" 2>"$stderr_file" &
    local command_pid=$!
    sdd_print_progress "$command_pid" "$start_ms" 2>/dev/null &
    local progress_pid=$!
    wait "$command_pid"
    status=$?
    kill "$progress_pid" 2>/dev/null || true
    wait "$progress_pid" 2>/dev/null || true
    printf '\nSDD finalizado. Preparando resumo...\n'
  fi
  set -e

  end_ms=$(sdd_now_ms)
  duration_ms=$((end_ms - start_ms))
  if [[ "$provider" == "codex" ]]; then
    SDD_CODEX_USAGE_JSON=$(sdd_sum_codex_usage "$usage_file" 2>/dev/null || true)
  fi
  execution_log=$(sdd_build_execution_log "$provider" "$status" "$started_at" "$duration_ms")

  printf 'SDD resumo: provider=%s feature=%s acao=%s status=%s duracao=%sms\n' \
    "$provider" "${FEATURE:-}" "${ACTION_CANONICAL:-${ACTION:-}}" "$status" "$duration_ms"

  if [[ -n "$SDD_CODEX_USAGE_JSON" ]]; then
    printf 'SDD uso Codex: %s\n' "$SDD_CODEX_USAGE_JSON"
  fi

  if [[ "$status" -ne 0 && "$output_mode" != "full" ]]; then
    printf 'SDD erro: saida suprimida. Rode com SDD_EXEC_OUTPUT=full para diagnosticar.\n'
    if [[ -s "$stderr_file" ]]; then
      printf 'SDD erro stderr:\n'
      tail -n 40 "$stderr_file"
    fi
  fi

  if log_path=$(sdd_execution_log_path "$provider"); then
    printf '%s\n' "$execution_log" > "$log_path"
    printf 'SDD log: %s\n' "${log_path#${PROJECT_ROOT:-$(pwd)}/}"
  fi

  rm -f "$usage_file" "$stderr_file"
  return "$status"
}

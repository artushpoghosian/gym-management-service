#!/usr/bin/env bash
set -euo pipefail

MAIN=http://localhost:8080
WORKLOAD=http://localhost:8082

jqget() { python3 -c "import sys,json;print(json.load(sys.stdin)['$1'])"; }

echo "==> 1. Register a trainer (CARDIO) and a trainee"
TRAINER=$(curl -s -X POST $MAIN/trainers -H 'Content-Type: application/json' \
  -d '{"firstName":"Nora","lastName":"Fit","specialization":"CARDIO"}')
TRAINEE=$(curl -s -X POST $MAIN/trainees -H 'Content-Type: application/json' \
  -d '{"firstName":"Ken","lastName":"Gym"}')
TR_U=$(echo "$TRAINER" | jqget username)
TR_P=$(echo "$TRAINER" | jqget password)
TE_U=$(echo "$TRAINEE" | jqget username)
echo "    trainer=$TR_U  trainee=$TE_U"

echo "==> 2. Log in as the trainer (get JWT)"
TOKEN=$(curl -s -X POST $MAIN/auth/login -H 'Content-Type: application/json' \
  -d "{\"username\":\"$TR_U\",\"password\":\"$TR_P\"}" | jqget token)
echo "    token acquired"

create_training() {
  local name=$1 date=$2 mins=$3
  curl -s -o /dev/null -w "    create $name ($mins min, $date) -> HTTP %{http_code}\n" \
    -X POST $MAIN/api/trainings \
    -H "Authorization: Bearer $TOKEN" -H "username: $TR_U" -H "password: $TR_P" \
    -H 'Content-Type: application/json' \
    -d "{\"traineeUsername\":\"$TE_U\",\"trainerUsername\":\"$TR_U\",\"trainingName\":\"$name\",\"trainingDate\":\"$date\",\"trainingDuration\":$mins}"
}

echo "==> 3. Create three trainings"
create_training "Cardio A" "2026-07-15" 60
create_training "Cardio B" "2026-07-20" 30
create_training "Cardio C" "2026-08-05" 45

echo "==> 4. Read the trainer's monthly summary from the WORKLOAD service"
echo "    (expect July=90, August=45)"
curl -s $WORKLOAD/api/trainer-workload/$TR_U -H "Authorization: Bearer $TOKEN" | python3 -m json.tool

echo "==> Done."

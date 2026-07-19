#!/usr/bin/env sh

for i in $(seq 1 100); do
   printf "Round %d\n" "$i"

   curl -s -XPOST localhost:8080/api/bets -H 'Content-Type: application/json' -d '{"userId":"u1","jackpotId":"JP-VARIABLE","amount":500}'
   printf "\n"

   curl -s -XPOST localhost:8080/api/bets -H 'Content-Type: application/json' -d '{"userId":"u2","jackpotId":"JP-FIXED","amount":333}'
   printf "\n"
done

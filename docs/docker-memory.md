# Demo VPS memory settings

`docker-compose.yml` sets small, reversible resource budgets for a 1–3 person demo. It does not change database volumes or application APIs. Use `docker compose config -q` to validate environment interpolation before starting the stack.

| Service | JVM initial / maximum heap | Container limit |
| --- | ---: | ---: |
| api-gateway | 64 / 160 MiB | 384 MiB |
| identity-service | 64 / 256 MiB | 576 MiB |
| nutrition-service | 64 / 224 MiB | 512 MiB |
| blog-service | 64 / 256 MiB | 576 MiB |
| recipe-service | 64 / 256 MiB | 576 MiB |
| location-service | 64 / 192 MiB | 448 MiB |
| ai-service | 64 / 320 MiB | 768 MiB |
| payment-service | 64 / 192 MiB | 448 MiB |
| MySQL 8.4 | 128 MiB InnoDB buffer pool | 640 MiB |

The gap above each heap is for metaspace, JIT code, thread stacks, native allocations and, for ai-service, FFmpeg/ffprobe child processes. Each Java service sees two active processors, limiting idle JVM thread and GC overhead. Database services use a Hikari maximum pool size of four and minimum idle of one. Seven pools can therefore hold at most 28 connections, below MySQL's 60 connection limit. MySQL's Performance Schema is disabled for this small demo; enable it again if its monitoring tables are needed. AI Video accepts one processing worker and two queued jobs; additional requests follow its existing queue rejection behavior. This may increase wait time for concurrent uploads.

These are limits, not a guarantee that a 5 GiB host is sufficient. Check host free memory and swap as well as `docker stats --no-stream`; container memory totals omit the host OS, Docker and filesystem cache. FFmpeg memory depends on input codec, resolution and duration. The 768 MiB ai-service limit covers both its JVM and child process; an unusually demanding video may hit the limit even when idle readings are low. Test a representative upload on the deployment host before a live demo, without calling paid AI endpoints unless authorized.

On the local Docker Desktop host (15.49 GiB available to Docker), the nine application/database containers measured 3,641 MiB before and 2,481 MiB after startup, rising to 2,559 MiB after a synthetic video workload; all nine stayed healthy. The 20-second 720p FFmpeg run reached 386 MiB in ai-service; ffprobe and audio extraction succeeded, and its cgroup reported zero swap. This is not a measurement on a 5 GiB Ubuntu VPS. The sum of the nine hard container limits is 4,928 MiB, so simultaneous peaks would leave too little memory for the OS. Monitor the actual VPS during a representative demo and reduce limits or add RAM if it swaps. The local ngrok container restarted because its endpoint was already online elsewhere; it is outside the nine-container total.

## Verification

Run `docker compose config -q`, `docker compose build`, and `docker compose up -d --no-build`. Check `docker compose ps`, `docker stats --no-stream`, and `docker inspect` for OOM kills and restart counts. On Ubuntu, also check `free -h`, `vmstat 1`, and `docker stats` during an upload to detect host swapping or peak memory pressure. Health checks cover application startup and MySQL connectivity, but do not prove peak video or payment webhook performance.

## Rollback

Review `git diff -- docker-compose.yml`, then restore its previous version with `git restore -- docker-compose.yml` if these are the only edits in that file. Run `docker compose config -q` and `docker compose up -d --no-build` to recreate containers with the previous settings. The `mysql_data` volume stays mounted throughout; never use `docker compose down -v` for this rollback. If only one service needs more headroom, raise its `-Xmx` and `mem_limit` together, leaving enough space for native memory.
